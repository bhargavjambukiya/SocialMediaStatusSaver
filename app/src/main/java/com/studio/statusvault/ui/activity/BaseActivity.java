package com.studio.statusvault.ui.activity;

import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.android.gms.ads.AdView;
import com.studio.statusvault.ads.AdsPolicy;

public class BaseActivity extends AppCompatActivity {
    public Dialog mProgressDialog;

    private AdView bannerAdView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        mProgressDialog = new CustomProgressDialog(this);

    }

    protected void bindBannerAdView(@Nullable AdView adView) {
        bannerAdView = adView;
    }

    /**
     * Hides the banner container and destroys the ad after the user becomes premium (e.g. from Settings).
     */
    protected void refreshPremiumBannerState(@IdRes int bannerContainerId) {
        if (AdsPolicy.shouldShowAds(this)) {
            return;
        }
        View c = findViewById(bannerContainerId);
        if (c instanceof FrameLayout) {
            ((FrameLayout) c).removeAllViews();
            c.setVisibility(View.GONE);
        }
        if (bannerAdView != null) {
            bannerAdView.destroy();
            bannerAdView = null;
        }
    }

    /**
     * Interstitial on leaving child screens (back to MainActivity) is disabled for now; finish immediately.
     * Re-enable: call {@link com.studio.statusvault.ads.InterstitialHelper#showIfAvailable} when user is not premium.
     */
    protected void finishAfterInterstitial() {
        finish();
        // if (!AdsPolicy.shouldShowAds(this)) { finish(); return; }
        // InterstitialHelper.showIfAvailable(this, this::finish);
    }

    @Override
    protected void onPause() {
        if (bannerAdView != null) {
            bannerAdView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bannerAdView != null) {
            bannerAdView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (bannerAdView != null) {
            bannerAdView.destroy();
            bannerAdView = null;
        }
        super.onDestroy();
    }

    /**
     * Show Loader
     */
    public void showLoader() {
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    /**
     * Hide Loader
     */
    public void hideLoader() {
        if (mProgressDialog != null)
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
    }
}
