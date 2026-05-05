package com.studio.statusvault.ads;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.studio.statusvault.R;

public final class BannerAdLoader {

    private static final String TAG = "StatusVaultAds";

    private BannerAdLoader() {
    }

    /**
     * Creates an anchored adaptive banner, adds it to {@code container}, and loads an ad.
     * Programmatic creation avoids XML {@code adSize} conflicts (size may only be set once).
     *
     * @return {@code null} when the user has premium (container is hidden).
     */
    public static AdView attachAnchoredAdaptiveBanner(FrameLayout container, Activity activity) {
        if (!AdsPolicy.shouldShowAds(activity)) {
            container.setVisibility(View.GONE);
            container.removeAllViews();
            return null;
        }
        container.setVisibility(View.VISIBLE);
        container.removeAllViews();
        AdView adView = new AdView(activity);
        adView.setAdUnitId(activity.getString(R.string.banner_home_footer));
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int widthDp = (int) (dm.widthPixels / dm.density);
        AdSize adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp);
        adView.setAdSize(adSize);
        container.addView(
                adView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT));
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // 0=internal, 1=invalid request, 2=network, 3=no fill (inventory), etc.
                Log.w(TAG, "Banner failed: code=" + loadAdError.getCode()
                        + " domain=" + loadAdError.getDomain()
                        + " msg=" + loadAdError.getMessage());
            }
        });
        adView.loadAd(new AdRequest.Builder().build());
        return adView;
    }
}
