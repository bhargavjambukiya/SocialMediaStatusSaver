package com.studio.statusvault.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.gowtham.library.utils.TrimVideo;
import com.studio.statusvault.Constant;
import com.studio.statusvault.R;
import com.studio.statusvault.ads.AdsPolicy;
import com.studio.statusvault.data.FilesData;
import com.studio.statusvault.data.repository.StatusMediaItem;
import com.studio.statusvault.filesoperations.FileOperations;
import com.studio.statusvault.utils.AudioExtractor;
import com.studio.statusvault.utils.MediaLibraryEvents;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class VideoViewerActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = "VideoViewerActivity";

    private enum VideoToolAction {
        NONE,
        TRIM,
        EXTRACT_AUDIO,
        DOWNLOAD,
        SHARE
    }

    private final ActivityResultLauncher<Intent> videoTrimLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                    return;
                }

                String trimmedPath = TrimVideo.getTrimmedVideoPath(result.getData());
                if (trimmedPath == null) {
                    return;
                }

                try {
                    File destDir = FilesData.getAppConvertedVideoDir();
                    FileUtils.forceMkdir(destDir);
                    File src = new File(trimmedPath);
                    File dest = new File(destDir, src.getName());
                    FileUtils.copyFile(src, dest);
                    MediaLibraryEvents.notifySavedLibraryChanged();
                    showSnackbarOpenConverted();
                } catch (IOException e) {
                    Toast.makeText(VideoViewerActivity.this, getString(R.string.video_save_failed), Toast.LENGTH_SHORT).show();
                }
            });

    private RewardedAd rewardedAd;

    private int position;
    private ArrayList<StatusMediaItem> recentVideos;
    private ArrayList<File> normalVideos;

    private ImageView imageViewSaveORDelete, imageViewShareFile, imageViewBack, imageViewMp3Converter, imageViewVideoSplitter;
    private View toolTrimContainer, toolExtractContainer, rowShareAction, rowSaveAction;
    private TextView textLabelSave;
    private VideoView vv;
    private char contentType;
    private boolean rewardEarnedForCurrentAd;
    private VideoToolAction lastOfferedAction = VideoToolAction.NONE;
    @Nullable
    private VideoToolAction actionAwaitingAdLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_viewer);

        boolean recentViewerFolderOnly = false;
        try {
            Bundle extras = getIntent().getExtras();
            contentType = extras.getChar("contentType");
            position = extras.getInt("position");
            recentViewerFolderOnly =
                    extras.getBoolean(Constant.EXTRA_RECENT_VIEWER_STATUS_FOLDER_ONLY, false);
        } catch (NullPointerException e) {
            Toast.makeText(this, getString(R.string.imageviewer_error), Toast.LENGTH_SHORT).show();
        }

        String mode = FilesData.getRecentOrSaved();
        if ("recent".equals(mode)) {
            recentVideos = recentViewerFolderOnly
                    ? FilesData.copyFilteredToWhatsAppStatusFolderOnly(
                            FilesData.getWhatsAppFilesVideos(), this)
                    : FilesData.getWhatsAppFilesVideos();
            if (recentVideos != null && !recentVideos.isEmpty()) {
                position = Math.min(position, recentVideos.size() - 1);
            }
        } else if ("videoSplitter".equals(mode)) {
            normalVideos = FilesData.getSplittedFilesVideos();
        } else {
            normalVideos = FilesData.getSavedFilesVideos();
        }

        initializeComponents();
        setUpPlayer();

        if (AdsPolicy.shouldShowAds(this)) {
            loadRewardedAd();
        } else {
            applyRewardAdUiState(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AdsPolicy.shouldShowAds(this)) {
            rewardedAd = null;
            applyRewardAdUiState(true);
        }
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, getString(R.string.rewarded_video), adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedAd = null;
                        Log.d("VideoViewer Ad", "Ad failed to load: " + loadAdError.getMessage());
                        applyRewardAdUiState(false);
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        Log.d("VideoViewer Ad", "Ad loaded");
                        applyRewardAdUiState(true);
                        if (actionAwaitingAdLoad != null) {
                            VideoToolAction awaiting = actionAwaitingAdLoad;
                            actionAwaitingAdLoad = null;
                            setActionLoading(awaiting, false);
                            vv.pause();
                            lastOfferedAction = awaiting;
                            showRewardedAd();
                        }
                    }
                });
    }

    private void showRewardedAd() {
        if (rewardedAd == null) {
            loadRewardedAd();
            return;
        }

        rewardEarnedForCurrentAd = false;
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                VideoToolAction offered = lastOfferedAction;
                boolean earned = rewardEarnedForCurrentAd;

                rewardedAd = null;
                loadRewardedAd();

                // Run trim/audio only after the rewarded ad UI is fully closed (e.g. user taps X on
                // "Reward granted"), not when the reward callback fires while the ad is still visible.
                if (earned) {
                    if (offered == VideoToolAction.TRIM) {
                        launchTrimEditor();
                    } else if (offered == VideoToolAction.EXTRACT_AUDIO) {
                        runAudioExtraction();
                    } else if (offered == VideoToolAction.DOWNLOAD) {
                        performSaveOrDeleteAction();
                    } else if (offered == VideoToolAction.SHARE) {
                        performShareAction();
                    }
                } else if (offered != VideoToolAction.NONE) {
                    int msg;
                    if (offered == VideoToolAction.TRIM) {
                        msg = R.string.video_tool_ad_cancelled_trim;
                    } else if (offered == VideoToolAction.EXTRACT_AUDIO) {
                        msg = R.string.video_tool_ad_cancelled_extract;
                    } else if (offered == VideoToolAction.DOWNLOAD) {
                        msg = R.string.video_tool_ad_cancelled_download;
                    } else {
                        msg = R.string.video_tool_ad_cancelled_share;
                    }
                    Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                }

                lastOfferedAction = VideoToolAction.NONE;
                clearActionLoading();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                rewardedAd = null;
                loadRewardedAd();
                Toast.makeText(VideoViewerActivity.this, R.string.video_tool_ad_failed, Toast.LENGTH_SHORT).show();
                lastOfferedAction = VideoToolAction.NONE;
                clearActionLoading();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d("VideoViewer Ad", "Ad showed.");
            }
        });

        rewardedAd.show(this, rewardItem -> {
            rewardEarnedForCurrentAd = true;
            Log.d("VideoViewer Ad", "Ad triggered reward; tool opens after dismiss.");
        });
    }

    private void launchTrimEditor() {
        if ("recent".equals(FilesData.getRecentOrSaved())) {
            StatusMediaItem item = recentVideos.get(position);
            if (item.isFromSaf() && item.getUri() != null) {
                TrimVideo.activity(String.valueOf(item.getUri()))
                        .disableCompression()
                        .start(VideoViewerActivity.this, videoTrimLauncher);
            } else if (item.getFile() != null) {
                TrimVideo.activity(String.valueOf(Uri.fromFile(item.getFile())))
                        .disableCompression()
                        .start(VideoViewerActivity.this, videoTrimLauncher);
            }
        } else {
            File path = normalVideos.get(position);
            TrimVideo.activity(String.valueOf(Uri.fromFile(path)))
                    .disableCompression()
                    .start(VideoViewerActivity.this, videoTrimLauncher);
        }
    }

    private void runAudioExtraction() {
        try {
            showLoader();

            if ("recent".equals(FilesData.getRecentOrSaved())) {
                StatusMediaItem item = recentVideos.get(position);

                if (item.isFromSaf() && item.getUri() != null) {
                    File tempVideo = FileOperations.copyUriToCacheFile(
                            VideoViewerActivity.this,
                            item.getUri(),
                            item.getName()
                    );
                    new AudioExtractor(VideoViewerActivity.this).genVideoUsingMuxer(
                            tempVideo, 0, true, false, mProgressDialog
                    );
                } else if (item.getFile() != null) {
                    new AudioExtractor(VideoViewerActivity.this).genVideoUsingMuxer(
                            item.getFile(), 0, true, false, mProgressDialog
                    );
                }
            } else {
                new AudioExtractor(VideoViewerActivity.this).genVideoUsingMuxer(
                        normalVideos.get(position), 0, true, false, mProgressDialog
                );
            }

            FilesData.audioSavedFiles();
            MediaLibraryEvents.notifySavedLibraryChanged();
            hideLoader();
            showSnackbarOpenAudio();
        } catch (IOException e) {
            hideLoader();
            e.printStackTrace();
            Toast.makeText(VideoViewerActivity.this, R.string.audio_extract_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void showSnackbarOpenConverted() {
        View anchor = findViewById(android.R.id.content);
        Snackbar.make(anchor, R.string.video_trim_saved_snackbar, Snackbar.LENGTH_LONG)
                .setAction(R.string.snackbar_open_converted, v -> {
                    FilesData.setRecentOrSaved("videoSplitter");
                    startActivity(new Intent(VideoViewerActivity.this, VideoSplitterActivity.class));
                })
                .show();
    }

    private void showSnackbarOpenAudio() {
        View anchor = findViewById(android.R.id.content);
        Snackbar.make(anchor, R.string.audio_extract_saved_snackbar, Snackbar.LENGTH_LONG)
                .setAction(R.string.snackbar_open_audio, v -> {
                    FilesData.setRecentOrSaved("audio");
                    startActivity(new Intent(VideoViewerActivity.this, AudioActivity.class));
                })
                .show();
    }

    private void applyRewardAdUiState(boolean adReady) {
        toolTrimContainer.setEnabled(true);
        toolExtractContainer.setEnabled(true);
        imageViewMp3Converter.setImageResource(R.drawable.ic_audio);
        imageViewVideoSplitter.setImageResource(R.drawable.ic_video);
        if ("recent".equals(FilesData.getRecentOrSaved())) {
            imageViewSaveORDelete.setImageResource(R.drawable.ic_direct_download);
        }
        imageViewShareFile.setImageResource(R.drawable.ic_share);
        imageViewMp3Converter.setEnabled(true);
        imageViewVideoSplitter.setEnabled(true);
        imageViewSaveORDelete.setEnabled(true);
        imageViewShareFile.setEnabled(true);
        rowSaveAction.setEnabled(true);
        rowShareAction.setEnabled(true);
        if (!adReady) {
            Log.d("VideoViewer Ad", "Rewarded ad not ready; tools still tappable to retry load.");
        }
    }

    private void setActionLoading(VideoToolAction action, boolean loading) {
        if (action == VideoToolAction.TRIM) {
            if (loading) {
                Glide.with(this).load(R.drawable.loading).into(imageViewVideoSplitter);
            } else {
                imageViewVideoSplitter.setImageResource(R.drawable.ic_video);
            }
            imageViewVideoSplitter.setEnabled(!loading);
            toolTrimContainer.setEnabled(!loading);
        } else if (action == VideoToolAction.EXTRACT_AUDIO) {
            if (loading) {
                Glide.with(this).load(R.drawable.loading).into(imageViewMp3Converter);
            } else {
                imageViewMp3Converter.setImageResource(R.drawable.ic_audio);
            }
            imageViewMp3Converter.setEnabled(!loading);
            toolExtractContainer.setEnabled(!loading);
        } else if (action == VideoToolAction.DOWNLOAD) {
            if (loading) {
                Glide.with(this).load(R.drawable.loading).into(imageViewSaveORDelete);
            } else if ("recent".equals(FilesData.getRecentOrSaved())) {
                imageViewSaveORDelete.setImageResource(R.drawable.ic_direct_download);
            }
            imageViewSaveORDelete.setEnabled(!loading);
            rowSaveAction.setEnabled(!loading);
        } else if (action == VideoToolAction.SHARE) {
            if (loading) {
                Glide.with(this).load(R.drawable.loading).into(imageViewShareFile);
            } else {
                imageViewShareFile.setImageResource(R.drawable.ic_share);
            }
            imageViewShareFile.setEnabled(!loading);
            rowShareAction.setEnabled(!loading);
        }
    }

    private void clearActionLoading() {
        applyRewardAdUiState(rewardedAd != null);
    }

    private void showToolSheet(VideoToolAction action) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_video_tool, null, false);
        TextView title = v.findViewById(R.id.sheet_title);
        TextView body = v.findViewById(R.id.sheet_body);
        if (action == VideoToolAction.TRIM) {
            title.setText(R.string.video_tool_sheet_trim_title);
            body.setText(R.string.video_tool_sheet_trim_body);
        } else {
            title.setText(R.string.video_tool_sheet_extract_title);
            body.setText(R.string.video_tool_sheet_extract_body);
        }
        v.findViewById(R.id.sheet_continue).setOnClickListener(btn -> {
            dialog.dismiss();
            startRewardFlow(action);
        });
        v.findViewById(R.id.sheet_cancel).setOnClickListener(btn -> dialog.dismiss());
        dialog.setContentView(v);
        dialog.show();
    }

    private void startRewardFlow(VideoToolAction action) {
        if (!isNetworkConnected()) {
            Toast.makeText(this, R.string.video_tool_need_network, Toast.LENGTH_SHORT).show();
            return;
        }
        lastOfferedAction = action;
        if (!AdsPolicy.shouldShowAds(this)) {
            vv.pause();
            if (action == VideoToolAction.TRIM) {
                launchTrimEditor();
            } else if (action == VideoToolAction.EXTRACT_AUDIO) {
                runAudioExtraction();
            } else if (action == VideoToolAction.DOWNLOAD) {
                performSaveOrDeleteAction();
            } else if (action == VideoToolAction.SHARE) {
                performShareAction();
            }
            lastOfferedAction = VideoToolAction.NONE;
            return;
        }
        if (rewardedAd != null) {
            vv.pause();
            showRewardedAd();
            return;
        }
        actionAwaitingAdLoad = action;
        setActionLoading(action, true);
        loadRewardedAd();
    }

    private void setUpPlayer() {
        if ("recent".equals(FilesData.getRecentOrSaved())) {
            StatusMediaItem item = recentVideos.get(position);

            if (item.isFromSaf() && item.getUri() != null) {
                vv.setVideoURI(item.getUri());
            } else if (item.getFile() != null) {
                vv.setVideoPath(item.getFile().getPath());
            }
        } else {
            vv.setVideoPath(normalVideos.get(position).getPath());
        }

        MediaController mc = new MediaController(this, false);
        vv.setMediaController(mc);
        vv.setFitsSystemWindows(true);
        mc.show(0);
        mc.setVisibility(View.VISIBLE);
        mc.setAnchorView(vv);
        vv.start();

        mc.setPrevNextListeners(v -> {
            if ("recent".equals(FilesData.getRecentOrSaved())) {
                if ((recentVideos.size() - 1) == position) {
                    position = 0;
                } else {
                    position++;
                }

                StatusMediaItem item = recentVideos.get(position);
                if (item.isFromSaf() && item.getUri() != null) {
                    vv.setVideoURI(item.getUri());
                } else if (item.getFile() != null) {
                    vv.setVideoPath(item.getFile().getPath());
                }
                vv.start();
            } else {
                if ((normalVideos.size() - 1) == position) {
                    position = 0;
                } else {
                    position++;
                }

                vv.setVideoPath(normalVideos.get(position).getPath());
                vv.start();
            }
        }, v -> {
            if ("recent".equals(FilesData.getRecentOrSaved())) {
                if (position == 0) {
                    position = recentVideos.size() - 1;
                } else {
                    position--;
                }

                StatusMediaItem item = recentVideos.get(position);
                if (item.isFromSaf() && item.getUri() != null) {
                    vv.setVideoURI(item.getUri());
                } else if (item.getFile() != null) {
                    vv.setVideoPath(item.getFile().getPath());
                }
                vv.start();
            } else {
                if (position == 0) {
                    position = normalVideos.size() - 1;
                } else {
                    position--;
                }

                vv.setVideoPath(normalVideos.get(position).getPath());
                vv.start();
            }
        });
    }

    private void initializeComponents() {
        vv = findViewById(R.id.videoView);
        imageViewVideoSplitter = findViewById(R.id.imageViewVideoSplitter);
        imageViewSaveORDelete = findViewById(R.id.imageViewSaveORDelete);
        imageViewShareFile = findViewById(R.id.imageViewShareFile);
        imageViewBack = findViewById(R.id.imageViewBack);
        imageViewMp3Converter = findViewById(R.id.imageViewMp3Converter);
        toolTrimContainer = findViewById(R.id.tool_trim_container);
        toolExtractContainer = findViewById(R.id.tool_extract_container);
        textLabelSave = findViewById(R.id.text_label_save);

        toolTrimContainer.setOnClickListener(this);
        toolExtractContainer.setOnClickListener(this);
        rowShareAction = findViewById(R.id.row_share_action);
        rowSaveAction = findViewById(R.id.row_save_action);
        rowShareAction.setOnClickListener(this);
        rowSaveAction.setOnClickListener(this);
        imageViewBack.setOnClickListener(this);

        if (!"recent".equals(FilesData.getRecentOrSaved())) {
            imageViewSaveORDelete.setImageResource(R.drawable.ic_delete);
            textLabelSave.setText(R.string.video_preview_action_delete);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.row_save_action) {
            if ("recent".equals(FilesData.getRecentOrSaved())) {
                startRewardFlow(VideoToolAction.DOWNLOAD);
            } else {
                performSaveOrDeleteAction();
            }
        } else if (id == R.id.row_share_action) {
            startRewardFlow(VideoToolAction.SHARE);
        } else if (id == R.id.imageViewBack) {
            finish();
        } else if (id == R.id.tool_extract_container) {
            showToolSheet(VideoToolAction.EXTRACT_AUDIO);
        } else if (id == R.id.tool_trim_container) {
            showToolSheet(VideoToolAction.TRIM);
        }
    }

    private void performSaveOrDeleteAction() {
        if ("recent".equals(FilesData.getRecentOrSaved())) {
            StatusMediaItem item = null;
            try {
                if (recentVideos == null || recentVideos.isEmpty()) {
                    Log.e(TAG, "performSaveOrDeleteAction: no recent videos available");
                    Toast.makeText(VideoViewerActivity.this, R.string.video_save_failed_source_missing, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (position < 0 || position >= recentVideos.size()) {
                    Log.e(TAG, "performSaveOrDeleteAction: invalid position=" + position + ", size=" + recentVideos.size());
                    Toast.makeText(VideoViewerActivity.this, R.string.video_save_failed_source_missing, Toast.LENGTH_SHORT).show();
                    return;
                }

                item = recentVideos.get(position);
                if (item == null) {
                    Log.e(TAG, "performSaveOrDeleteAction: item is null at position=" + position);
                    Toast.makeText(VideoViewerActivity.this, R.string.video_save_failed_source_missing, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (item.isFromSaf() && item.getUri() != null) {
                    FileOperations.saveAndRefreshFilesFromUri(
                            VideoViewerActivity.this,
                            item.getUri(),
                            item.getName()
                    );
                } else if (item.getFile() != null) {
                        FileOperations.saveAndRefreshFiles(VideoViewerActivity.this, item.getFile());
                } else {
                    Log.e(TAG, "performSaveOrDeleteAction: both uri and file are null for item name=" + item.getName());
                    Toast.makeText(VideoViewerActivity.this, R.string.video_save_failed_source_missing, Toast.LENGTH_SHORT).show();
                    return;
                }

                showSavedStatusSnackbar(R.string.video_saved_success_message);
            } catch (SecurityException e) {
                Log.e(TAG, "performSaveOrDeleteAction: permission error, position=" + position
                        + ", uri=" + (item != null ? item.getUri() : "null"), e);
                Toast.makeText(VideoViewerActivity.this, R.string.video_save_failed_permission, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.e(TAG, "performSaveOrDeleteAction: IO error, position=" + position
                        + ", uri=" + (item != null ? item.getUri() : "null")
                        + ", file=" + (item != null ? item.getFile() : "null"), e);
                Toast.makeText(VideoViewerActivity.this, R.string.video_save_failed_io, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "performSaveOrDeleteAction: unexpected error, position=" + position, e);
                Toast.makeText(VideoViewerActivity.this, getString(R.string.video_save_failed), Toast.LENGTH_SHORT).show();
            }
        } else {
            File target;
            if ("videoSplitter".equals(FilesData.getRecentOrSaved())) {
                target = FilesData.getSplittedFilesVideos().get(position);
            } else {
                target = FilesData.getSavedFilesVideos().get(position);
            }

            FileOperations.deleteAndRefreshFiles(target);
            MediaLibraryEvents.notifySavedLibraryChanged();
            Toast.makeText(VideoViewerActivity.this, getString(R.string.video_deleted), Toast.LENGTH_SHORT).show();
        }
    }

    private void showSavedStatusSnackbar(int messageRes) {
        View anchor = findViewById(android.R.id.content);
        Snackbar.make(anchor, messageRes, Snackbar.LENGTH_LONG)
                .setAction(R.string.saved_status_view_action, v -> {
                    FilesData.setRecentOrSaved("saved");
                    startActivity(new Intent(VideoViewerActivity.this, StoriesActivity.class));
                })
                .show();
    }

    private void performShareAction() {
        if ("recent".equals(FilesData.getRecentOrSaved())) {
            StatusMediaItem item = recentVideos.get(position);

            if (item.isFromSaf() && item.getUri() != null) {
                FileOperations.shareUri(VideoViewerActivity.this, item.getUri(), contentType);
            } else if (item.getFile() != null) {
                FileOperations.shareFile(item.getFile(), VideoViewerActivity.this, contentType);
            }
        } else {
            FileOperations.shareFile(normalVideos.get(position), VideoViewerActivity.this, contentType);
        }
    }
}
