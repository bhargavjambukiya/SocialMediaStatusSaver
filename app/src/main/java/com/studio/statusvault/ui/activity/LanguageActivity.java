package com.studio.statusvault.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.nativead.NativeAd;
import com.studio.statusvault.R;
import com.studio.statusvault.ads.AdsPolicy;
import com.studio.statusvault.ads.NativeAdLoader;
import com.studio.statusvault.locale.AppLocaleHelper;
import com.studio.statusvault.ui.adapter.LanguageListAdapter;

public class LanguageActivity extends BaseActivity {

    public static final String EXTRA_FIRST_RUN = "extra_first_run";
    public static final String EXTRA_FROM_SETTINGS = "extra_from_settings";

    @Nullable
    private View toolbarContinueAction;
    private TextView textSubtitle;
    private LanguageListAdapter adapter;
    private boolean isFirstRun;
    private boolean fromSettings;
    private NativeAd languageNativeAd;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        Intent in = getIntent();
        isFirstRun = in.getBooleanExtra(EXTRA_FIRST_RUN, false);
        fromSettings = in.getBooleanExtra(EXTRA_FROM_SETTINGS, false);

        Toolbar toolbar = findViewById(R.id.toolbarLanguage);
        toolbar.setTitle(R.string.lang_picker_title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            if (isFirstRun) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            } else {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        textSubtitle = findViewById(R.id.textLanguageSubtitle);
        textSubtitle.setText(R.string.lang_picker_subtitle);

        String current = AppLocaleHelper.getSavedLanguageTag(this);
        if (current.isEmpty()) {
            current = "en";
        }
        int preselect = indexOfTag(current);

        adapter = new LanguageListAdapter(this, preselect, pos -> {
            if (toolbarContinueAction != null) {
                toolbarContinueAction.setEnabled(true);
            }
        });
        RecyclerView rv = findViewById(R.id.recyclerLanguages);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isFirstRun) {
                    finishAffinity();
                } else {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        });

        FrameLayout nativeAdContainer = findViewById(R.id.native_ad_container);
        NativeAdLoader.loadInto(
                nativeAdContainer,
                this,
                getString(R.string.native_ad),
                ad -> languageNativeAd = ad);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_language_continue, menu);
        MenuItem item = menu.findItem(R.id.action_language_continue);
        View action = item.getActionView();
        toolbarContinueAction = action;
        if (action != null) {
            action.setEnabled(adapter != null && adapter.getSelectedPosition() >= 0);
            action.setOnClickListener(v -> confirm());
        }
        return true;
    }

    private void destroyLanguageNativeAd() {
        FrameLayout c = findViewById(R.id.native_ad_container);
        if (c != null) {
            c.removeAllViews();
            c.setVisibility(View.GONE);
        }
        if (languageNativeAd != null) {
            languageNativeAd.destroy();
            languageNativeAd = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AdsPolicy.shouldShowAds(this)) {
            destroyLanguageNativeAd();
        }
    }

    @Override
    protected void onDestroy() {
        destroyLanguageNativeAd();
        super.onDestroy();
    }

    private int indexOfTag(String tag) {
        if (tag == null || tag.isEmpty()) {
            return 0;
        }
        String base = tag.contains("-") ? tag.split("-", 2)[0] : tag;
        String[] tags = getResources().getStringArray(R.array.app_language_tags);
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].equalsIgnoreCase(base)) {
                return i;
            }
        }
        return 0;
    }

    private void confirm() {
        int i = adapter.getSelectedPosition();
        if (i < 0) {
            return;
        }
        String tag = getResources().getStringArray(R.array.app_language_tags)[i];
        AppLocaleHelper.saveLanguage(this, tag);
        if (fromSettings) {
            setResult(RESULT_OK);
        } else {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (isFirstRun) {
            return true;
        }
        setResult(RESULT_CANCELED);
        finish();
        return true;
    }
}
