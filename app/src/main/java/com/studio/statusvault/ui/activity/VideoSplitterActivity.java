package com.studio.statusvault.ui.activity;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.ads.AdView;
import com.studio.statusvault.ads.BannerAdLoader;
import com.studio.statusvault.Constant;
import com.studio.statusvault.R;
import com.studio.statusvault.ads.AdsPolicy;
import com.studio.statusvault.ui.fragment.VideoFragment;
import com.studio.statusvault.ui.viewmodel.StoriesViewModel;
import com.studio.statusvault.utils.MediaLibraryEvents;

import java.util.ArrayList;
import java.util.List;

public class VideoSplitterActivity extends BaseActivity {
    private AdView mAdView;
    private StoriesViewModel storiesViewModel;
    private final Runnable savedLibraryListener = () -> {
        if (storiesViewModel != null) {
            storiesViewModel.loadStoriesLists();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_splitter);

        storiesViewModel = new ViewModelProvider(this).get(StoriesViewModel.class);

        setUpToolbar();
        initializeComponents();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAfterInterstitial();
            }
        });
    }

    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.title_activity_video_splitter));
        toolbar.setSubtitle(R.string.subtitle_video_splitter);
        setSupportActionBar(toolbar);
        //back button on toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initializeComponents() {
        FrameLayout bannerContainer = findViewById(R.id.banner_ad_container);
        mAdView = BannerAdLoader.attachAnchoredAdaptiveBanner(bannerContainer, this);
        bindBannerAdView(mAdView);
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        ViewPager mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setCurrentItem(Constant.statusTabSelection);
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

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> fragments = new ArrayList<>();
        private final List<String> fragmentNames = new ArrayList<>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            fragments.clear();
         /*   fragments.add(new ImageFragment());
            fragmentNames.add(getString(R.string.tab_image));*/
            fragments.add(new VideoFragment());
            fragmentNames.add(getString(R.string.tab_video));
        }

        @Override
        public Fragment getItem(int position) {

            return fragments.get(position);
        }

        @Override
        public int getCount() {

            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentNames.get(position);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finishAfterInterstitial();
        return true;
    }
}