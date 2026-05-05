package com.studio.statusvault.ads;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.AdChoicesView;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.studio.statusvault.R;

public final class NativeAdLoader {

    private static final String TAG = "StatusVaultAds";

    public interface Callback {
        void onNativeAd(@Nullable NativeAd nativeAd);
    }

    private NativeAdLoader() {
    }

    /**
     * Loads a native ad into {@code container}. The caller must {@link NativeAd#destroy()} when done
     * (e.g. in {@code onDestroy}) and hide the slot when the user is premium.
     */
    public static void loadInto(
            @NonNull FrameLayout container,
            @NonNull Activity activity,
            @NonNull String adUnitId,
            @Nullable Callback callback) {
        if (!AdsPolicy.shouldShowAds(activity)) {
            container.setVisibility(View.GONE);
            container.removeAllViews();
            if (callback != null) {
                callback.onNativeAd(null);
            }
            return;
        }
        container.setVisibility(View.VISIBLE);
        container.removeAllViews();
        NativeAdView adView = (NativeAdView) activity.getLayoutInflater()
                .inflate(R.layout.native_ad_template, container, false);

        AdLoader adLoader = new AdLoader.Builder(activity, adUnitId)
                .forNativeAd(nativeAd -> {
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        nativeAd.destroy();
                        if (callback != null) {
                            callback.onNativeAd(null);
                        }
                        return;
                    }
                    populate(nativeAd, adView);
                    container.removeAllViews();
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
                    lp.gravity = Gravity.BOTTOM;
                    container.addView(adView, lp);
                    if (callback != null) {
                        callback.onNativeAd(nativeAd);
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.w(TAG, "Native failed: code=" + loadAdError.getCode()
                                + " domain=" + loadAdError.getDomain()
                                + " msg=" + loadAdError.getMessage());
                        if (activity.isFinishing() || activity.isDestroyed()) {
                            return;
                        }
                        container.setVisibility(View.GONE);
                        container.removeAllViews();
                        if (callback != null) {
                            callback.onNativeAd(null);
                        }
                    }
                })
                .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private static void populate(NativeAd nativeAd, NativeAdView adView) {
        MediaView mediaView = adView.findViewById(R.id.native_ad_media);
        TextView headline = adView.findViewById(R.id.native_ad_headline);
        TextView body = adView.findViewById(R.id.native_ad_body);
        TextView cta = adView.findViewById(R.id.native_ad_call_to_action);
        ImageView icon = adView.findViewById(R.id.native_ad_icon);
        TextView advertiser = adView.findViewById(R.id.native_ad_advertiser);

        AdChoicesView adChoices = adView.findViewById(R.id.native_ad_choices);
        adView.setAdChoicesView(adChoices);

        adView.setMediaView(mediaView);
        adView.setHeadlineView(headline);
        adView.setBodyView(body);
        adView.setCallToActionView(cta);
        adView.setIconView(icon);
        adView.setAdvertiserView(advertiser);

        if (nativeAd.getMediaContent() == null) {
            mediaView.setVisibility(View.GONE);
        } else {
            mediaView.setVisibility(View.VISIBLE);
            mediaView.setMediaContent(nativeAd.getMediaContent());
        }

        if (nativeAd.getHeadline() == null) {
            headline.setVisibility(View.GONE);
        } else {
            headline.setVisibility(View.VISIBLE);
            headline.setText(nativeAd.getHeadline());
        }

        if (nativeAd.getBody() == null) {
            body.setVisibility(View.GONE);
        } else {
            body.setVisibility(View.VISIBLE);
            body.setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            cta.setVisibility(View.GONE);
        } else {
            cta.setVisibility(View.VISIBLE);
            cta.setText(nativeAd.getCallToAction());
        }

        NativeAd.Image iconImage = nativeAd.getIcon();
        if (iconImage == null || iconImage.getDrawable() == null) {
            icon.setVisibility(View.GONE);
        } else {
            icon.setVisibility(View.VISIBLE);
            icon.setImageDrawable(iconImage.getDrawable());
        }

        if (nativeAd.getAdvertiser() == null) {
            advertiser.setVisibility(View.GONE);
        } else {
            advertiser.setVisibility(View.VISIBLE);
            advertiser.setText(nativeAd.getAdvertiser());
        }

        adView.setNativeAd(nativeAd);
    }
}
