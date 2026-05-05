package com.studio.statusvault.ads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.studio.statusvault.R;

public final class InterstitialHelper {

    private static final long MIN_INTERVAL_MS = 90_000;

    @Nullable
    private static InterstitialAd loaded;
    private static boolean loadInFlight;
    private static long lastDismissTimeMs;

    private InterstitialHelper() {
    }

    public static void preload(Context context) {
        if (!AdsPolicy.shouldShowAds(context)) {
            loaded = null;
            loadInFlight = false;
            return;
        }
        if (loaded != null || loadInFlight) {
            return;
        }
        loadInFlight = true;
        Context app = context.getApplicationContext();
        InterstitialAd.load(
                app,
                app.getString(R.string.interstitial_main),
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        loadInFlight = false;
                    }

                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        loadInFlight = false;
                        loaded = interstitialAd;
                    }
                });
    }

    /**
     * Shows a loaded interstitial if available and outside the cooldown window; otherwise runs {@code onFinished}.
     */
    public static void showIfAvailable(@NonNull Activity activity, @NonNull Runnable onFinished) {
        if (!AdsPolicy.shouldShowAds(activity)) {
            loaded = null;
            onFinished.run();
            return;
        }
        if (loaded == null) {
            preload(activity);
            onFinished.run();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastDismissTimeMs < MIN_INTERVAL_MS) {
            onFinished.run();
            return;
        }

        InterstitialAd ad = loaded;
        loaded = null;

        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                lastDismissTimeMs = System.currentTimeMillis();
                ad.setFullScreenContentCallback(null);
                preload(activity.getApplicationContext());
                onFinished.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                lastDismissTimeMs = System.currentTimeMillis();
                ad.setFullScreenContentCallback(null);
                preload(activity.getApplicationContext());
                onFinished.run();
            }

            @Override
            public void onAdShowedFullScreenContent() {
            }
        });

        ad.show(activity);
    }
}
