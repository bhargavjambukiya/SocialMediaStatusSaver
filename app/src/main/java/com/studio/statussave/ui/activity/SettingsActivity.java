package com.studio.statussave.ui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.studio.statussave.BuildConfig;
import com.studio.statussave.R;
import com.studio.statussave.data.FilesData;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {
    TextView textViewPrivacyPolicy, textViewRateUs, textViewShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setUpToolbar();
        initializeComponents();

    }

    private void initializeComponents() {
        textViewPrivacyPolicy = findViewById(R.id.textViewPrivacyPolicy);
        textViewRateUs = findViewById(R.id.textViewRateUs);
        textViewShare = findViewById(R.id.textViewShare);

        textViewPrivacyPolicy.setOnClickListener(this);
        textViewRateUs.setOnClickListener(this);
        textViewShare.setOnClickListener(this);
    }

    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.title_settings));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.textViewPrivacyPolicy:
                String url = "https://sites.google.com/view/social-media-saver/home";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
                break;
            case R.id.textViewRateUs:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID)));
                break;
            case R.id.textViewShare:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        "Hey, download this app! at: https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
                break;
        }
    }
}