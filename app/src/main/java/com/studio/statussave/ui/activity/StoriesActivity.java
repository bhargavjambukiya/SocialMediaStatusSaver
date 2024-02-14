package com.studio.statussave.ui.activity;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.studio.statussave.Constant;
import com.studio.statussave.R;
import com.studio.statussave.data.FilesData;
import com.studio.statussave.ui.fragment.ImageFragment;
import com.studio.statussave.ui.fragment.VideoFragment;
import com.studio.statussave.ui.fragment.VideoFragmentOld;

import java.util.ArrayList;
import java.util.List;

public class StoriesActivity extends BaseActivity {
    //private AdView mAdView;
    ViewPager mViewPager;
    TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.st_activity_saved_stories);

        setUpToolbar();

        initializeComponents();

        // MobileAds.setRequestConfiguration(adMobConfiguration());
        // mAdView.loadAd(new AdRequest.Builder().build());

        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
        mViewPager.setCurrentItem(Constant.statusTabSelection);
    }

    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (FilesData.getRecentOrSaved().equals("recent")) {
            toolbar.setTitle(getString(R.string.title_activity_recent_stories));
        } else {
            toolbar.setTitle(getString(R.string.title_activity_saved_stories));
        }
        setSupportActionBar(toolbar);
        //back button on toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initializeComponents() {
        // mAdView = findViewById(R.id.adView);
        mViewPager = findViewById(R.id.container);
        tabLayout = findViewById(R.id.tabs);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> fragments = new ArrayList<>();
        private final List<String> fragmentNames = new ArrayList<>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            fragments.clear();
            fragments.add(new ImageFragment());
            fragmentNames.add(getString(R.string.tab_image));
            fragments.add(new VideoFragment());
            fragmentNames.add(getString(R.string.tab_video));
        }

        @Override
        public Fragment getItem(int position) {

            return fragments.get(position);
        }

        @Override
        public int getCount() {

            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentNames.get(position);
        }
    }
    /**/
}
