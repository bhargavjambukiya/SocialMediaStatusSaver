package com.studio.statusvault.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdView;
import com.studio.statusvault.ads.BannerAdLoader;
import com.studio.statusvault.R;
import com.studio.statusvault.ads.AdsPolicy;
import com.studio.statusvault.adapters.AudioFileListAdapter;
import com.studio.statusvault.data.FilesData;
import com.studio.statusvault.utils.MediaLibraryEvents;

import java.io.File;
import java.util.ArrayList;

public class AudioActivity extends BaseActivity {
    public static LinearLayout layoutNoRecordFound;
    private AdView mAdView;
    private AudioFileListAdapter audioAdapter;
    private RecyclerView audioRecyclerView;
    private final Runnable savedLibraryListener = () -> runOnUiThread(() -> {
        if (!isFinishing()) {
            refreshAudioList();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_activity);

        setUpToolbar();

        initializeComponents();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAfterInterstitial();
            }
        });
    }

    private void initializeComponents() {
        layoutNoRecordFound = findViewById(R.id.layoutNoRecordFound);
        FrameLayout bannerContainer = findViewById(R.id.banner_ad_container);
        mAdView = BannerAdLoader.attachAnchoredAdaptiveBanner(bannerContainer, this);
        bindBannerAdView(mAdView);

        if (!FilesData.getRecentOrSaved().equals("audio")) {
            return;
        }

        audioRecyclerView = findViewById(R.id.audioRecyclerView);
        audioRecyclerView.setHasFixedSize(true);
        audioRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        audioAdapter = new AudioFileListAdapter(this, () -> {
            if (layoutNoRecordFound != null) {
                layoutNoRecordFound.setVisibility(View.VISIBLE);
            }
        });
        audioRecyclerView.setAdapter(audioAdapter);
        refreshAudioList();
    }

    private void refreshAudioList() {
        if (!FilesData.getRecentOrSaved().equals("audio") || audioAdapter == null) {
            return;
        }
        FilesData.audioSavedFiles();
        ArrayList<File> files = new ArrayList<>(FilesData.getSavedAudioFiles());
        audioAdapter.submitList(files);
        if (files.isEmpty()) {
            layoutNoRecordFound.setVisibility(View.VISIBLE);
        } else {
            layoutNoRecordFound.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        MediaLibraryEvents.addListener(savedLibraryListener);
    }

    @Override
    protected void onStop() {
        MediaLibraryEvents.removeListener(savedLibraryListener);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPremiumBannerState(R.id.banner_ad_container);
        if (!AdsPolicy.shouldShowAds(this)) {
            mAdView = null;
        }
        refreshAudioList();
    }

    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (FilesData.getRecentOrSaved().equals("audio")) {
            toolbar.setTitle(getString(R.string.title_activity_saved_audio));
        }
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(R.string.subtitle_saved_audio);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finishAfterInterstitial();
        return true;
    }
}
