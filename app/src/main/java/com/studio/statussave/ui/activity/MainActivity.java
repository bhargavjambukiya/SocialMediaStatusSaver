package com.studio.statussave.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
import com.studio.statussave.Constant;
import com.studio.statussave.R;
import com.studio.statussave.data.FilesData;
import com.studio.statussave.utils.Common;
import com.studio.statussave.utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    // Permission
    private static final int REQUEST_PERMISSIONS = 1234;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private Context context;
    @SuppressLint("InlinedApi")
    private static final String[] NOTIFICATION_PERMISSION = {
            Manifest.permission.POST_NOTIFICATIONS
    };
    private static final int NOTIFICATION_REQUEST_PERMISSIONS = 4;

    private ConstraintLayout constraintLayoutImages, constraintLayoutVideos, constraintLayoutConvertedAudio,
            constraintLayoutMain, constraintLayoutSaved, constraintLayoutSettings, constraintLayoutVideoSplitter;
    private AdView mAdView;


    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {

                    Intent data = result.getData();

                    assert data != null;

                    Log.d("HEY: ", data.getData().toString());

                    context.getContentResolver().takePersistableUriPermission(
                            data.getData(),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show();

                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        initializeComponents();


       /* if (!arePermissionDenied()) {
            next();
            return;
        }*/

        //CheckPermission();


        // MobileAds.setRequestConfiguration(adMobConfiguration());
        //mAdView.loadAd(new AdRequest.Builder().build());
    }

    private void CheckPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionDenied()) {
            // If Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermissionQ();
                return;
            }
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(NOTIFICATION_PERMISSION,
                    NOTIFICATION_REQUEST_PERMISSIONS);
        }

        if (Common.APP_DIR == null || Common.APP_DIR.isEmpty()) {
            Common.APP_DIR = getExternalFilesDir("StatusDownloader").getPath();
            Log.d("App Path", Common.APP_DIR);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (arePermissionDenied()) {
            CheckPermission();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void requestPermissionQ() {
        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);

        Intent intent = sm.getPrimaryStorageVolume().createOpenDocumentTreeIntent();
        String startDir = "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses";

        Uri uri = intent.getParcelableExtra("android.provider.extra.INITIAL_URI");

        String scheme = uri.toString();
        scheme = scheme.replace("/root/", "/document/");
        scheme += "%3A" + startDir;

        uri = Uri.parse(scheme);

        Log.d("URI", uri.toString());

        intent.putExtra("android.provider.extra.INITIAL_URI", uri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);


        activityResultLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS && grantResults.length > 0) {
            if (arePermissionDenied()) {
                // Clear Data of Application, So that it can request for permissions again
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            } else {
                // next();
            }
        }
    }

    private boolean arePermissionDenied() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return getContentResolver().getPersistedUriPermissions().size() <= 0;
        }

        for (String permissions : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), permissions) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }


/*    private void next() {

        handler.postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 1000);

    }*/

    private void initializeComponents() {
        constraintLayoutVideoSplitter = findViewById(R.id.constraintLayoutVideoSplitter);
        constraintLayoutSettings = findViewById(R.id.constraintLayoutSettings);
        mAdView = findViewById(R.id.adView);
        constraintLayoutMain = findViewById(R.id.constraintLayoutMain);
        constraintLayoutImages = findViewById(R.id.constraintLayoutImages);
        constraintLayoutVideos = findViewById(R.id.constraintLayoutVideos);
        constraintLayoutConvertedAudio = findViewById(R.id.constraintLayoutConvertedAudio);
        constraintLayoutSaved = findViewById(R.id.constraintLayoutSaved);
        constraintLayoutImages.setOnClickListener(this);
        constraintLayoutVideos.setOnClickListener(this);
        constraintLayoutConvertedAudio.setOnClickListener(this);
        constraintLayoutSaved.setOnClickListener(this);
        constraintLayoutVideoSplitter.setOnClickListener(this);
        constraintLayoutSettings.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.constraintLayoutImages:
                Constant.statusTabSelection = 0;
                FilesData.scrapWhatsAppFiles();
                FilesData.setRecentOrSaved("recent");
                startActivity(new Intent(MainActivity.this, StoriesActivity.class));
                break;
            case R.id.constraintLayoutVideos:
                Constant.statusTabSelection = 1;
                FilesData.scrapWhatsAppFiles();
                FilesData.setRecentOrSaved("recent");
                startActivity(new Intent(MainActivity.this, StoriesActivity.class));
                break;
            case R.id.constraintLayoutConvertedAudio:
                FilesData.scrapWhatsAppFiles();
                FilesData.setRecentOrSaved("audio");
                startActivity(new Intent(MainActivity.this, AudioActivity.class));
                break;
            case R.id.constraintLayoutSaved:
                Constant.statusTabSelection = 0;
                FilesData.scrapWhatsAppFiles();
                FilesData.setRecentOrSaved("offline");
                startActivity(new Intent(MainActivity.this, StoriesActivity.class));
                break;
            case R.id.constraintLayoutSettings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            case R.id.constraintLayoutVideoSplitter:
                Constant.statusTabSelection = 0;
                FilesData.scrapWhatsAppFiles();
                FilesData.setRecentOrSaved("videoSplitter");
                startActivity(new Intent(MainActivity.this, VideoSplitterActivity.class));
                break;
        }
    }


}