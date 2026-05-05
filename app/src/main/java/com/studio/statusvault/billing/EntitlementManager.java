package com.studio.statusvault.billing;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Persists whether the user has purchased premium (no ads) via the one-time in-app product.
 * Updated by {@link PlayBillingController} after Play Billing queries.
 */
public final class EntitlementManager {

    private static final String PREFS = "billing_entitlement_prefs";
    private static final String KEY_PREMIUM = "has_active_premium";

    private EntitlementManager() {
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isPremium(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_PREMIUM, false);
    }

    public static void setPremium(@NonNull Context context, boolean premium) {
        prefs(context).edit().putBoolean(KEY_PREMIUM, premium).apply();
    }
}
