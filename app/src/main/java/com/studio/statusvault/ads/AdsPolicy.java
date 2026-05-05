package com.studio.statusvault.ads;

import android.content.Context;

import androidx.annotation.NonNull;

import com.studio.statusvault.BuildConfig;
import com.studio.statusvault.billing.EntitlementManager;

/**
 * Central gate for monetization. Premium purchasers see no ads.
 * {@link BuildConfig#SCREENSHOT_BUILD} can be enabled via Gradle (-PSCREENSHOT_BUILD=true)
 * so Play Store screenshots show no banners, native ads, or rewarded flows.
 * Optional future: integrate UMP consent before {@link #shouldShowAds(Context)} for EEA/UK.
 */
public final class AdsPolicy {

    private AdsPolicy() {
    }

    public static boolean shouldShowAds(@NonNull Context context) {
        if (BuildConfig.SCREENSHOT_BUILD) {
            return false;
        }
        return !EntitlementManager.isPremium(context);
    }
}
