package com.studio.statusvault.data;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * Recent WhatsApp statuses are always limited to the last 7 days (no user setting).
 */
public final class RecentStatusRangePreference {

    public enum Range {
        ALL,
        LAST_7_DAYS
    }

    private RecentStatusRangePreference() {
    }

    @NonNull
    public static Range getRange(@NonNull Context context) {
        return Range.LAST_7_DAYS;
    }

    public static void setRange(@NonNull Context context, @NonNull Range range) {
        // Fixed policy: always last 7 days.
    }

    public static boolean isLast7DaysOnly(@NonNull Context context) {
        return true;
    }
}
