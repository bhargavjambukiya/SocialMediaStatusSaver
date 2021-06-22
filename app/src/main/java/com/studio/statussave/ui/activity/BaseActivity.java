package com.studio.statussave.ui.activity;

import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.RequestConfiguration;
import com.studio.statussave.BuildConfig;

import java.util.ArrayList;
import java.util.List;

public class BaseActivity extends AppCompatActivity {
    public Dialog mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProgressDialog = new CustomProgressDialog(this);

    }

    public RequestConfiguration adMobConfiguration() {
        List<String> testDevices = new ArrayList<>();
        if (BuildConfig.DEBUG) {
            testDevices.add(AdRequest.DEVICE_ID_EMULATOR);
            testDevices.add("29D243536ED87F9BC93D71893D30047C");
        }

        RequestConfiguration requestConfiguration
                = new RequestConfiguration.Builder()
                .setTestDeviceIds(testDevices)
                .build();
        return requestConfiguration;
    }

    /**
     * Show Loader
     */
    public void showLoader() {
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    /**
     * Hide Loader
     */
    public void hideLoader() {
        if (mProgressDialog != null)
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
    }
}
