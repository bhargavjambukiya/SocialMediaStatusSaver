package com.studio.statusvault.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class WalkthroughHelper {

    private static final String PREFS = "walkthrough_prefs";
    private static final String KEY_SHOWN = "walkthrough_shown";

    private WalkthroughHelper() {
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean shouldShow(@NonNull Context context) {
        return !prefs(context).getBoolean(KEY_SHOWN, false);
    }

    public static void markShown(@NonNull Context context) {
        prefs(context).edit().putBoolean(KEY_SHOWN, true).apply();
    }
}
