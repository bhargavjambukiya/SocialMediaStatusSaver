package com.studio.statusvault.utils;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

/**
 * Whether the WhatsApp status-folder (SAF) prompt should be shown on Android 11+ when tree access is missing.
 * There is no separate “already dismissed” flag: closing without granting means the prompt can appear again
 * (e.g. on next {@link com.studio.statusvault.ui.activity.MainActivity} resume or when opening status images/videos).
 */
public final class StatusOnboardingHelper {

    private StatusOnboardingHelper() {
    }

    /**
     * @return true when the app should offer folder access (no persisted tree URI yet).
     */
    public static boolean shouldShowStatusFolderOnboarding(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return false;
        }
        return !WhatsAppAccessHelper.hasTreeUri(context);
    }
}
