package com.studio.statussave.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.studio.statussave.R;
import com.studio.statussave.adapters.RecyclerAdapter;
import com.studio.statussave.adapters.RecyclerInstances;
import com.studio.statussave.data.FilesData;

public class AudioActivity extends BaseActivity {
    public static LinearLayout layoutNoRecordFound;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_activity);

        setUpToolbar();

        initializeComponents();

        MobileAds.setRequestConfiguration(adMobConfiguration());
        mAdView.loadAd(new AdRequest.Builder().build());
    }

    private void initializeComponents() {
        layoutNoRecordFound = findViewById(R.id.layoutNoRecordFound);
        mAdView = findViewById(R.id.adView);

        if (FilesData.getRecentOrSaved().equals("audio")) {
            RecyclerInstances.audioRecyclerView = findViewById(R.id.audioRecyclerView);
            RecyclerInstances.audioRecyclerView.setHasFixedSize(true);

            LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
            RecyclerInstances.audioRecyclerView.setLayoutManager(mLayoutManager);

            if (FilesData.getSavedAudioFiles().isEmpty()) {
                FilesData.audioSavedFiles();
            }
            if (FilesData.getSavedAudioFiles().size() > 0) {
                RecyclerInstances.audioAdapter = new RecyclerAdapter(FilesData.getSavedAudioFiles(), this, 'a');
                RecyclerInstances.audioRecyclerView.setAdapter(RecyclerInstances.audioAdapter);
            } else {
                layoutNoRecordFound.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (FilesData.getRecentOrSaved().equals("audio")) {
            toolbar.setTitle(getString(R.string.title_activity_saved_audio));
        }
        setSupportActionBar(toolbar);

        //back button on toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}







