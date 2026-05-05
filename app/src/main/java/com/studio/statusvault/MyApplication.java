package com.studio.statusvault;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.MobileAds;
import com.studio.statusvault.ads.AdMobConfig;
import com.studio.statusvault.billing.PlayBillingController;
import com.studio.statusvault.data.FilesData;
import com.studio.statusvault.locale.AppLocaleHelper;

public class MyApplication extends Application {
    //public static FirebaseAnalytics mFirebaseAnalytics;

    @Nullable
    private PlayBillingController playBillingController;

    @NonNull
    public PlayBillingController getPlayBillingController() {
        if (playBillingController == null) {
            playBillingController = new PlayBillingController(this);
        }
        return playBillingController;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLocaleHelper.reconcileLanguageStateAfterPossibleRestore(this);
        AppLocaleHelper.applyPersistedLocale(this);
        FilesData.init(this);
        getPlayBillingController().start();
        MobileAds.setRequestConfiguration(AdMobConfig.requestConfiguration(this));
        MobileAds.initialize(this, initializationStatus -> { });
        // Obtain the FirebaseAnalytics instance.
        // mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }
}
