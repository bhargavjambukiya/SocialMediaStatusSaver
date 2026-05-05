package com.studio.statusvault.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.google.android.material.button.MaterialButton;
import com.studio.statusvault.R;

import androidx.annotation.Nullable;

/**
 * Full-screen hint on top of the system document tree (and other windows) so we can
 * point users to the system "Use this folder" action. Requires
 * {@link Settings#canDrawOverlays}.
 */
public final class StatusPickerHintOverlay {

    @Nullable
    private static View root;

    private StatusPickerHintOverlay() {
    }

    public static boolean canShow(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }
        return Settings.canDrawOverlays(context.getApplicationContext());
    }

    public static void showAfterDelay(Context activityContext, long delayMs) {
        if (!canShow(activityContext)) {
            return;
        }
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> {
                    if (activityContext instanceof Activity) {
                        Activity a = (Activity) activityContext;
                        if (a.isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && a.isDestroyed())) {
                            return;
                        }
                    }
                    show(activityContext);
                },
                Math.max(0, delayMs));
    }

    public static void show(Context context) {
        if (!canShow(context)) {
            return;
        }
        if (context instanceof Activity) {
            Activity a = (Activity) context;
            if (a.isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && a.isDestroyed())) {
                return;
            }
        }
        dismiss();
        Context app = context.getApplicationContext();
        WindowManager wm = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return;
        }
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        // Leave the bottom ~12% of the screen for the system “Use this folder” action.
        int overlayH = (int) (dm.heightPixels * 0.88f);
        @SuppressWarnings("deprecation")
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayH,
                type,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = 0;
        p.y = 0;

        LayoutInflater inflater = LayoutInflater.from(context);
        root = inflater.inflate(R.layout.overlay_status_picker_hint, null, false);
        View dismissBtn = root.findViewById(R.id.textDismissHint);
        if (dismissBtn != null) {
            dismissBtn.setOnClickListener(v -> dismiss());
        }
        MaterialButton mock = root.findViewById(R.id.textHintUseFolderLabel);
        if (mock != null) {
            mock.setText(R.string.status_picker_hint_button_label);
        }
        try {
            wm.addView(root, p);
        } catch (WindowManager.BadTokenException | SecurityException e) {
            root = null;
        }
    }

    public static void dismiss() {
        if (root == null) {
            return;
        }
        try {
            Context c = root.getContext();
            WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                wm.removeView(root);
            }
        } catch (Exception ignored) {
        }
        root = null;
    }
}
