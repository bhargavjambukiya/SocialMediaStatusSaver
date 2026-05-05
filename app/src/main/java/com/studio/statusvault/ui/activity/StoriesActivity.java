package com.studio.statusvault.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.ads.AdView;
import com.studio.statusvault.ads.BannerAdLoader;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.studio.statusvault.Constant;
import com.studio.statusvault.R;
import com.studio.statusvault.ads.AdsPolicy;
import com.studio.statusvault.data.FilesData;
import com.studio.statusvault.ui.fragment.CombinedRecentFragment;
import com.studio.statusvault.ui.fragment.ImageFragment;
import com.studio.statusvault.ui.fragment.VideoFragment;
import com.studio.statusvault.ui.viewmodel.StoriesViewModel;
import com.studio.statusvault.utils.MediaLibraryEvents;
import com.studio.statusvault.utils.WhatsAppAccessHelper;

public class StoriesActivity extends BaseActivity {

    /** When true, show All | Images | Videos tabs; All merges recent images and videos (last 7 days). */
    public static final String EXTRA_COMBINED_RECENT_TABS =
            "com.studio.statusvault.StoriesActivity.EXTRA_COMBINED_RECENT_TABS";

    private AdView mAdView;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private StoriesViewModel storiesViewModel;
    private SwipeRefreshLayout swipeRefreshLayout;
    private final Runnable savedLibraryListener = () -> {
        if (storiesViewModel != null) {
            storiesViewModel.loadStoriesLists();
        }
    };

    private final ActivityResultLauncher<Intent> whatsappFolderPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                Uri treeUri = result.getData().getData();
                if (treeUri != null) {
                    int takeFlags = result.getData().getFlags();
                    WhatsAppAccessHelper.saveTreeUri(StoriesActivity.this, treeUri, takeFlags);
                    storiesViewModel.loadStoriesLists();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.st_activity_saved_stories);

        storiesViewModel = new ViewModelProvider(this).get(StoriesViewModel.class);

        setUpToolbar();
        initializeComponents();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAfterInterstitial();
            }
        });

        boolean combinedRecentTabs =
                getIntent().getBooleanExtra(EXTRA_COMBINED_RECENT_TABS, false)
                        && FilesData.getRecentOrSaved().equals("recent");
        StoriesPagerAdapter adapter = new StoriesPagerAdapter(this, combinedRecentTabs);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(combinedRecentTabs ? 0 : Constant.statusTabSelection, false);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (combinedRecentTabs) {
                if (position == 0) {
                    tab.setText(R.string.tab_all);
                } else if (position == 1) {
                    tab.setText(R.string.tab_image);
                } else {
                    tab.setText(R.string.tab_video);
                }
            } else {
                tab.setText(position == 0 ? getString(R.string.tab_image) : getString(R.string.tab_video));
            }
        }).attach();

        storiesViewModel.getLoading().observe(this, loading -> {
            if (!Boolean.TRUE.equals(loading) && swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
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
        storiesViewModel.loadStoriesLists();
    }

    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (FilesData.getRecentOrSaved().equals("recent")) {
            toolbar.setTitle(getString(R.string.title_activity_recent_stories));
        } else {
            toolbar.setTitle(getString(R.string.title_activity_saved_stories));
        }
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initializeComponents() {
        FrameLayout bannerContainer = findViewById(R.id.banner_ad_container);
        mAdView = BannerAdLoader.attachAnchoredAdaptiveBanner(bannerContainer, this);
        bindBannerAdView(mAdView);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        viewPager = findViewById(R.id.container);
        tabLayout = findViewById(R.id.tabs);
        swipeRefreshLayout.setOnRefreshListener(() -> storiesViewModel.loadStoriesLists());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finishAfterInterstitial();
        return true;
    }

    /**
     * Shows the WhatsApp folder instruction dialog and SAF picker (Android 11+).
     */
    public void requestWhatsAppFolderAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showWhatsAppFolderInstructionDialog();
        }
    }

    private void showWhatsAppFolderInstructionDialog() {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_whatsapp_folder_guide, null, false);
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_whatsapp_folder_title)
                .setView(content)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_continue, (dialog, which) -> openWhatsAppFolderPicker())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openWhatsAppFolderPicker() {
        Intent intent = WhatsAppAccessHelper.newOpenDocumentTreeIntentForWhatsappStatuses(this);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        whatsappFolderPickerLauncher.launch(intent);
    }

    private static class StoriesPagerAdapter extends FragmentStateAdapter {

        private final boolean combinedRecentTabs;

        StoriesPagerAdapter(@NonNull FragmentActivity fragmentActivity, boolean combinedRecentTabs) {
            super(fragmentActivity);
            this.combinedRecentTabs = combinedRecentTabs;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (combinedRecentTabs) {
                if (position == 0) {
                    return new CombinedRecentFragment();
                }
                if (position == 1) {
                    return new ImageFragment();
                }
                return new VideoFragment();
            }
            if (position == 0) {
                return new ImageFragment();
            }
            return new VideoFragment();
        }

        @Override
        public int getItemCount() {
            return combinedRecentTabs ? 3 : 2;
        }
    }
}
