package com.studio.statusvault.ads;

import android.content.Context;
import android.text.TextUtils;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.RequestConfiguration;
import com.studio.statusvault.BuildConfig;
import com.studio.statusvault.R;

import java.util.ArrayList;
import java.util.List;

public final class AdMobConfig {

    private AdMobConfig() {
    }

    /**
     * Registers debug test devices so AdMob serves Google test creatives on physical phones.
     * Add your device hash from Logcat ({@code admob_optional_test_device_id}) — it changes per install/device.
     */
    public static RequestConfiguration requestConfiguration(Context context) {
        List<String> testDeviceIds = new ArrayList<>();
        if (BuildConfig.DEBUG) {
            testDeviceIds.add(AdRequest.DEVICE_ID_EMULATOR);
            testDeviceIds.add("29D243536ED87F9BC93D71893D30047C");
            String extra = context.getString(R.string.admob_optional_test_device_id);
            if (!TextUtils.isEmpty(extra)) {
                testDeviceIds.add(extra.trim());
            }
        }
        return new RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build();
    }
}
