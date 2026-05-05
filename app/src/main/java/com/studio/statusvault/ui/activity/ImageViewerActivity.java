package com.studio.statusvault.ui.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.snackbar.Snackbar;
import com.studio.statusvault.Constant;
import com.studio.statusvault.adapters.AdapterImagesViewer;
import com.studio.statusvault.ads.AdsPolicy;
import com.studio.statusvault.data.FilesData;
import com.studio.statusvault.data.repository.StatusMediaItem;
import com.studio.statusvault.filesoperations.FileOperations;
import com.studio.statusvault.utils.MediaLibraryEvents;
import com.studio.statusvault.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ImageViewerActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = "ImageViewerActivity";
    private enum ImageAction {
        DOWNLOAD,
        SHARE
    }

    String path;
    int position;
    char contentType;
    PhotoView mPhotoView;
    private ImageView imageViewSaveORDelete, imageViewShareFile;
    private ViewPager viewPagerImages;
    private RewardedAd rewardedAd;
    private boolean rewardEarnedForCurrentAd;
    private boolean awaitingActionAdLoad;
    @Nullable
    private ImageAction pendingAction;
    /** Recent mode: items backing the pager (merged 7-day list vs WhatsApp folder only). */
    @Nullable
    private ArrayList<StatusMediaItem> recentWhatsAppImagesSource;
    private boolean recentViewerFolderOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.i_activity_image_viewer);

        setUpToolbar();
        try {
            android.os.Bundle extras = getIntent().getExtras();
            position = extras.getInt("position");
            contentType = extras.getChar("contentType");
            recentViewerFolderOnly =
                    extras.getBoolean(Constant.EXTRA_RECENT_VIEWER_STATUS_FOLDER_ONLY, false);
        } catch (NullPointerException e) {
            Toast.makeText(this, getString(R.string.imageviewer_error), Toast.LENGTH_SHORT).show();
        }

        initializeComponents();
        if (AdsPolicy.shouldShowAds(this)) {
            loadRewardedAd();
        }

        if (!"recent".equals(FilesData.getRecentOrSaved())) {
            imageViewSaveORDelete.setImageResource(R.drawable.ic_delete);
            ArrayList<File> savedImages = FilesData.getSavedFilesImages();
            if (savedImages != null && !savedImages.isEmpty()) {
                viewPagerImages.setAdapter(AdapterImagesViewer.fromSavedFiles(ImageViewerActivity.this, savedImages));
                viewPagerImages.setCurrentItem(Math.min(position, savedImages.size() - 1));
                path = savedImages.get(viewPagerImages.getCurrentItem()).getAbsolutePath();
            }
        } else {
            recentWhatsAppImagesSource = recentViewerFolderOnly
                    ? FilesData.copyFilteredToWhatsAppStatusFolderOnly(
                            FilesData.getWhatsAppFilesImages(), this)
                    : new ArrayList<>(FilesData.getWhatsAppFilesImages());
            AdapterImagesViewer myCustomPagerAdapter =
                    new AdapterImagesViewer(ImageViewerActivity.this, recentWhatsAppImagesSource);
            viewPagerImages.setAdapter(myCustomPagerAdapter);
            if (recentWhatsAppImagesSource != null && !recentWhatsAppImagesSource.isEmpty()) {
                position = Math.min(position, recentWhatsAppImagesSource.size() - 1);
            }
            viewPagerImages.setCurrentItem(position);
        }

        viewPagerImages.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int pos) {
                if (!"recent".equals(FilesData.getRecentOrSaved())) {
                    imageViewSaveORDelete.setImageResource(R.drawable.ic_delete);
                    path = FilesData.getSavedFilesImages().get(pos).getAbsolutePath();
                } else {
                    StatusMediaItem item = recentWhatsAppImagesSource.get(pos);
                    if (item.isFromSaf() && item.getUri() != null) {
                        path = item.getUri().toString();
                    } else if (item.getFile() != null) {
                        path = item.getFile().getAbsolutePath();
                    }
                }
                position = pos;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AdsPolicy.shouldShowAds(this)) {
            rewardedAd = null;
            setActionLoading(ImageAction.DOWNLOAD, false);
            setActionLoading(ImageAction.SHARE, false);
        }
    }

    private void initializeComponents() {
        mPhotoView = findViewById(R.id.imageviewer);
        imageViewSaveORDelete = findViewById(R.id.imageViewSaveORDelete);
        imageViewShareFile = findViewById(R.id.imageViewShareFile);
        viewPagerImages = findViewById(R.id.viewPagerImages);

        imageViewShareFile.setOnClickListener(this);
        imageViewSaveORDelete.setOnClickListener(this);
    }

    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolimageviewer);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, getString(R.string.rewarded_video), adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedAd = null;
                        Log.d("ImageViewer Ad", "Ad failed to load: " + loadAdError.getMessage());
                        if (awaitingActionAdLoad && pendingAction != null) {
                            setActionLoading(pendingAction, false);
                            awaitingActionAdLoad = false;
                        }
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        Log.d("ImageViewer Ad", "Ad loaded");
                        if (awaitingActionAdLoad && pendingAction != null) {
                            setActionLoading(pendingAction, false);
                            awaitingActionAdLoad = false;
                            showRewardedAdForAction();
                        }
                    }
                });
    }

    private void showRewardedAdForAction() {
        if (rewardedAd == null) {
            loadRewardedAd();
            return;
        }

        rewardEarnedForCurrentAd = false;
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                boolean earned = rewardEarnedForCurrentAd;
                ImageAction action = pendingAction;
                pendingAction = null;
                rewardedAd = null;
                loadRewardedAd();
                if (earned && action != null) {
                    if (action == ImageAction.DOWNLOAD) {
                        saveCurrentImage();
                    } else {
                        shareCurrentImage();
                    }
                } else if (action == ImageAction.DOWNLOAD) {
                    Toast.makeText(ImageViewerActivity.this, R.string.image_tool_ad_cancelled_download, Toast.LENGTH_SHORT).show();
                } else if (action == ImageAction.SHARE) {
                    Toast.makeText(ImageViewerActivity.this, R.string.video_tool_ad_cancelled_share, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                ImageAction action = pendingAction;
                pendingAction = null;
                rewardedAd = null;
                loadRewardedAd();
                if (action != null) {
                    setActionLoading(action, false);
                }
                Toast.makeText(ImageViewerActivity.this, R.string.video_tool_ad_failed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d("ImageViewer Ad", "Ad showed.");
            }
        });

        rewardedAd.show(this, rewardItem -> rewardEarnedForCurrentAd = true);
    }

    private void setActionLoading(ImageAction action, boolean loading) {
        if (!"recent".equals(FilesData.getRecentOrSaved())) {
            return;
        }
        if (action == ImageAction.DOWNLOAD) {
            if (loading) {
                Glide.with(this).load(R.drawable.loading).into(imageViewSaveORDelete);
            } else {
                imageViewSaveORDelete.setImageResource(R.drawable.ic_direct_download);
            }
            imageViewSaveORDelete.setEnabled(!loading);
        } else {
            if (loading) {
                Glide.with(this).load(R.drawable.loading).into(imageViewShareFile);
            } else {
                imageViewShareFile.setImageResource(R.drawable.ic_share);
            }
            imageViewShareFile.setEnabled(!loading);
        }
    }

    private void startRewardedActionFlow(ImageAction action) {
        if (!isNetworkConnected()) {
            Toast.makeText(this, R.string.video_tool_need_network, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!AdsPolicy.shouldShowAds(this)) {
            if (action == ImageAction.DOWNLOAD) {
                saveCurrentImage();
            } else {
                shareCurrentImage();
            }
            return;
        }
        pendingAction = action;
        if (rewardedAd != null) {
            showRewardedAdForAction();
            return;
        }
        awaitingActionAdLoad = true;
        setActionLoading(action, true);
        loadRewardedAd();
    }

    private void shareCurrentImage() {
        if ("recent".equals(FilesData.getRecentOrSaved())) {
            StatusMediaItem item = recentWhatsAppImagesSource.get(position);

            if (item.isFromSaf() && item.getUri() != null) {
                FileOperations.shareUri(ImageViewerActivity.this, item.getUri(), contentType);
            } else if (item.getFile() != null) {
                FileOperations.shareFile(item.getFile(), ImageViewerActivity.this, contentType);
            }
        } else {
            FileOperations.shareFile(FilesData.getSavedFilesImages().get(position), ImageViewerActivity.this, contentType);
        }
    }

    private void saveCurrentImage() {
        StatusMediaItem item = null;
        try {
            ArrayList<StatusMediaItem> images = recentWhatsAppImagesSource;
            if (images == null || images.isEmpty()) {
                Log.e(TAG, "saveCurrentImage: no recent images available");
                Toast.makeText(ImageViewerActivity.this, R.string.image_save_failed_source_missing, Toast.LENGTH_SHORT).show();
                return;
            }
            if (position < 0 || position >= images.size()) {
                Log.e(TAG, "saveCurrentImage: invalid position=" + position + ", size=" + images.size());
                Toast.makeText(ImageViewerActivity.this, R.string.image_save_failed_source_missing, Toast.LENGTH_SHORT).show();
                return;
            }

            item = images.get(position);
            if (item == null) {
                Log.e(TAG, "saveCurrentImage: item is null at position=" + position);
                Toast.makeText(ImageViewerActivity.this, R.string.image_save_failed_source_missing, Toast.LENGTH_SHORT).show();
                return;
            }

            if (item.isFromSaf() && item.getUri() != null) {
                FileOperations.saveAndRefreshFilesFromUri(
                        ImageViewerActivity.this,
                        item.getUri(),
                        item.getName()
                );
            } else if (item.getFile() != null) {
                FileOperations.saveAndRefreshFiles(ImageViewerActivity.this, item.getFile());
            } else {
                Log.e(TAG, "saveCurrentImage: both uri and file are null for item name=" + item.getName());
                Toast.makeText(ImageViewerActivity.this, R.string.image_save_failed_source_missing, Toast.LENGTH_SHORT).show();
                return;
            }

            showSavedStatusSnackbar(R.string.image_saved_success_message);
        } catch (SecurityException e) {
            Log.e(TAG, "saveCurrentImage: permission error, position=" + position
                    + ", uri=" + (item != null ? item.getUri() : "null"), e);
            Toast.makeText(ImageViewerActivity.this, R.string.image_save_failed_permission, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "saveCurrentImage: IO error, position=" + position
                    + ", uri=" + (item != null ? item.getUri() : "null")
                    + ", file=" + (item != null ? item.getFile() : "null"), e);
            Toast.makeText(ImageViewerActivity.this, R.string.image_save_failed_io, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "saveCurrentImage: unexpected error, position=" + position, e);
            Toast.makeText(ImageViewerActivity.this, getString(R.string.image_save_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void showSavedStatusSnackbar(int messageRes) {
        View anchor = findViewById(android.R.id.content);
        Snackbar.make(anchor, messageRes, Snackbar.LENGTH_LONG)
                .setAction(R.string.saved_status_view_action, v -> {
                    FilesData.setRecentOrSaved("saved");
                    startActivity(new android.content.Intent(ImageViewerActivity.this, StoriesActivity.class));
                })
                .show();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.imageViewSaveORDelete) {
            if ("recent".equals(FilesData.getRecentOrSaved())) {
                startRewardedActionFlow(ImageAction.DOWNLOAD);
            } else {
                FileOperations.deleteAndRefreshFiles(FilesData.getSavedFilesImages().get(position));
                MediaLibraryEvents.notifySavedLibraryChanged();
                Toast.makeText(ImageViewerActivity.this, getString(R.string.image_deleted), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.imageViewShareFile) {
            startRewardedActionFlow(ImageAction.SHARE);
        }
    }
}