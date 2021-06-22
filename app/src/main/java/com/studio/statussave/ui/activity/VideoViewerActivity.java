package com.studio.statussave.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.gowtham.library.utils.TrimVideo;
import com.studio.statussave.BuildConfig;
import com.studio.statussave.R;
import com.studio.statussave.adapters.RecyclerInstances;
import com.studio.statussave.data.FilesData;
import com.studio.statussave.filesoperations.FileOperations;
import com.studio.statussave.utils.AudioExtractor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class VideoViewerActivity extends BaseActivity implements View.OnClickListener, RewardedVideoAdListener {
    private RewardedVideoAd mRewardedVideoAd;

    int position;
    private static ArrayList<File> videos;
    private ImageView imageViewSaveORDelete, imageViewShareFile, imageViewBack, imageViewMp3Converter, imageViewVideoSplitter;
    private VideoView vv;
    char contentType;
    boolean isRewarded, isVideoSplitter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_viewer);


        initializeAd();

        if (FilesData.getRecentOrSaved().equals("recent")) {
            videos = FilesData.getWhatsAppFilesVideos();
        } else if (FilesData.getRecentOrSaved().equals("videoSplitter")) {
            videos = FilesData.getSplittedFilesVideos();
        } else {
            videos = FilesData.getSavedFilesVideos();
        }


        try {
            contentType = getIntent().getExtras().getChar("contentType");
            position = getIntent().getExtras().getInt("position");
        } catch (NullPointerException e) {
            Toast.makeText(this, getString(R.string.imageviewer_error), Toast.LENGTH_SHORT).show();
        }
        initializeComponents();
        setUpPlayer();
        if (isNetworkConnected()) {
            adLoading(false);
        }
    }

    private void initializeAd() {
        MobileAds.setRequestConfiguration(adMobConfiguration());
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedVideoAd.setRewardedVideoAdListener(this);
        mRewardedVideoAd.loadAd(getString(R.string.rewarded_video), new AdRequest.Builder().build());
    }

    private void adLoading(boolean loading) {
        imageViewMp3Converter.setEnabled(loading);
        imageViewVideoSplitter.setEnabled(loading);
        if (!loading) {
            Glide.with(this).load(R.drawable.loading).into(imageViewMp3Converter);
            Glide.with(this).load(R.drawable.loading).into(imageViewVideoSplitter);
        } else {
            imageViewMp3Converter.setImageResource(R.drawable.convert_mp3);
            imageViewVideoSplitter.setImageResource(R.drawable.ic_video_spliter);
        }

    }

    private void setUpPlayer() {
        vv.setVideoPath(videos.get(position).getPath());
        MediaController mc = new MediaController(this, false);
        vv.setMediaController(mc);
        vv.setFitsSystemWindows(true);
        mc.show(0);
        mc.setVisibility(View.VISIBLE);
        mc.setAnchorView(vv);
        vv.start();

        mc.setPrevNextListeners(v -> {
            // next button clicked
            if ((videos.size() - 1) == position) {
                position = 0;
                vv.setVideoPath(videos.get(position).getPath());
                vv.start();

            } else {
                vv.setVideoPath(videos.get(++position).getPath());
                vv.start();
            }

        }, v -> {
            if (position == 0) {
                position = videos.size() - 1;
                vv.setVideoPath(videos.get(position).getPath());
                vv.start();
            } else {

                vv.setVideoPath(videos.get(--position).getPath());
                vv.start();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TrimVideo.VIDEO_TRIMMER_REQ_CODE && data != null) {
            Toast.makeText(this, "Video saved successfully", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeComponents() {
        vv = findViewById(R.id.videoView);
        imageViewVideoSplitter = findViewById(R.id.imageViewVideoSplitter);
        imageViewSaveORDelete = findViewById(R.id.imageViewSaveORDelete);
        imageViewShareFile = findViewById(R.id.imageViewShareFile);
        imageViewBack = findViewById(R.id.imageViewBack);
        imageViewMp3Converter = findViewById(R.id.imageViewMp3Converter);

        imageViewVideoSplitter.setOnClickListener(this);
        imageViewShareFile.setOnClickListener(this);
        imageViewSaveORDelete.setOnClickListener(this);
        imageViewBack.setOnClickListener(this);
        imageViewMp3Converter.setOnClickListener(this);

        if (!FilesData.getRecentOrSaved().equals("recent")) {
            imageViewSaveORDelete.setImageResource(R.drawable.ic_delete);
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imageViewSaveORDelete:
                if (FilesData.getRecentOrSaved().equals("recent")) {
                    try {
                        FileOperations.saveAndRefreshFiles(FilesData.getWhatsAppFilesVideos().get(position));
                        Toast.makeText(VideoViewerActivity.this, getString(R.string.video_saved), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(VideoViewerActivity.this, getString(R.string.video_save_failed), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    FileOperations.deleteAndRefreshFiles(FilesData.getSavedFilesVideos().get(position));
                    RecyclerInstances.savedVideoAdapter.notifyDataSetChanged();
                    RecyclerInstances.savedVideoRecyclerview.setAdapter(RecyclerInstances.savedVideoAdapter);
                    Toast.makeText(VideoViewerActivity.this, getString(R.string.video_deleted), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.imageViewShareFile:
                FileOperations.shareFile(FilesData.getWhatsAppFilesVideos().get(position), VideoViewerActivity.this, contentType);
                break;
            case R.id.imageViewBack:
                finish();
                break;
            case R.id.imageViewMp3Converter:
                isVideoSplitter = false;
                if (isNetworkConnected()) {
                    if (!mRewardedVideoAd.isLoaded()) {
                        initializeAd();
                        adLoading(false);
                    }
                    if (mRewardedVideoAd.isLoaded()) {
                        vv.pause();
                        mRewardedVideoAd.show();
                    }
                } else {
                    Toast.makeText(this, "For convert audio internet connection is required.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.imageViewVideoSplitter:
                isVideoSplitter = true;
                if (isNetworkConnected()) {
                    if (!mRewardedVideoAd.isLoaded()) {
                        initializeAd();
                        adLoading(false);
                    }
                    if (mRewardedVideoAd.isLoaded()) {
                        vv.pause();
                        mRewardedVideoAd.show();
                    }
                } else {
                    Toast.makeText(this, "For split video internet connection is required.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mRewardedVideoAd.pause(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRewardedVideoAd.destroy(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mRewardedVideoAd.resume(this);
    }

    @Override
    public void onRewardedVideoAdLoaded() {
        Log.d("VideoViewer Ad", "Ad loaded");
        adLoading(true);
        // Toast.makeText(getBaseContext(), "Ad loaded.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewardedVideoAdOpened() {
        Log.d("VideoViewer Ad", "Ad opened.");
        //Toast.makeText(getBaseContext(), "Ad opened.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewardedVideoStarted() {
        Log.d("VideoViewer Ad", "Ad started.");
        //Toast.makeText(getBaseContext(), "Ad started.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewardedVideoAdClosed() {
        adLoading(false);
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedVideoAd.setRewardedVideoAdListener(this);
        mRewardedVideoAd.loadAd(getString(R.string.rewarded_video), new AdRequest.Builder().build());
        Log.d("VideoViewer Ad", "Ad closed.");
        if (!isRewarded) {
            Toast.makeText(getBaseContext(), "Failed to convert audio.", Toast.LENGTH_SHORT).show();
        }
        //Toast.makeText(getBaseContext(), "Ad closed.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewarded(RewardItem rewardItem) {
        isRewarded = true;
        Log.d("VideoViewer Ad", "Ad triggered reward.");
        // Toast.makeText(getBaseContext(), "Ad triggered reward.", Toast.LENGTH_SHORT).show();
        if (isVideoSplitter) {
            File path = videos.get(position);
            TrimVideo.activity(String.valueOf(Uri.fromFile(path)))
                    .setDestination(Environment.getExternalStorageDirectory().toString() + FilesData.SAVED_FILES_SPLIT_VIDEO)  //default output path /storage/emulated/0/DOWNLOADS
                    .start(this);
        } else {
            try {
                showLoader();
                new AudioExtractor(this).genVideoUsingMuxer(videos.get(position),
                        0, true, false, mProgressDialog);
                FilesData.audioSavedFiles();
                hideLoader();
                Toast.makeText(this, "File converted successfully", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRewardedVideoAdLeftApplication() {
        Log.d("VideoViewer Ad", "Ad left application.");
        //Toast.makeText(getBaseContext(), "Ad left application.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int i) {
        Log.d("VideoViewer Ad", "Ad failed to load.");
        //Toast.makeText(getBaseContext(), "Ad failed to load.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewardedVideoCompleted() {

    }
}
