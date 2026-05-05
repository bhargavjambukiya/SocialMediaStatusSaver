package com.studio.statusvault.ui.activity;

import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.studio.statusvault.MyApplication;
import com.studio.statusvault.ads.AdsPolicy;
import com.studio.statusvault.ads.NativeAdLoader;
import com.studio.statusvault.billing.EntitlementManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.gms.tasks.Task;
import com.studio.statusvault.Constant;
import com.studio.statusvault.R;
import com.studio.statusvault.data.FilesData;
import com.studio.statusvault.locale.AppLocaleHelper;

import android.net.Uri;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.studio.statusvault.ui.StatusPickerHintOverlay;
import com.studio.statusvault.utils.StatusOnboardingHelper;
import com.studio.statusvault.utils.WhatsAppAccessHelper;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQ_CODE_VERSION_UPDATE = 530;
    private AppUpdateManager appUpdateManager;
    private InstallStateUpdatedListener installStateUpdatedListener;
    private ConstraintLayout constraintLayoutImages, constraintLayoutVideos, constraintLayoutConvertedAudio,
            constraintLayoutMain, constraintLayoutSaved, constraintLayoutSettings, constraintLayoutVideoSplitter,
            constraintLayoutAllStatuses, constraintLayoutRemoveAds;
    private AdView mAdView;
    private NativeAd homeNativeAd;
    private AlertDialog statusFolderOnboardingDialog;
    private boolean statusOnboardingTreeFlow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppLocaleHelper.hasUserSelectedLanguage(this)) {
            startActivity(new Intent(this, LanguageActivity.class)
                    .putExtra(LanguageActivity.EXTRA_FIRST_RUN, true));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        initializeComponents();
        checkForAppUpdate();

       /* Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "1");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "name");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
        MyApplication.mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);*/

        // Interstitial on back from other screens is disabled; avoid preloading until re-enabled in BaseActivity#finishAfterInterstitial.
        // if (AdsPolicy.shouldShowAds(this)) {
        //     InterstitialHelper.preload(this);
        // }
    }

    /**
     * First-run WhatsApp “.Statuses” folder access (SAF), shown as a dialog over the home screen.
     */
    private void showStatusFolderOnboardingDialogIfNeeded() {
        if (!StatusOnboardingHelper.shouldShowStatusFolderOnboarding(this)) {
            return;
        }
        if (statusFolderOnboardingDialog != null && statusFolderOnboardingDialog.isShowing()) {
            return;
        }
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_status_folder_onboarding, null, false);
        TextView close = content.findViewById(R.id.buttonStatusIntroClose);
        MaterialButton grant = content.findViewById(R.id.buttonGrantPermission);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setView(content)
                .setCancelable(false);
        AlertDialog dialog = builder.create();
        statusFolderOnboardingDialog = dialog;

        close.setOnClickListener(v -> dialog.dismiss());
        grant.setOnClickListener(v -> {
            statusOnboardingTreeFlow = true;
            openDocumentTreeInternal(true);
        });
        dialog.setOnDismissListener(di -> statusFolderOnboardingDialog = null);
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                d.dismiss();
                return true;
            }
            return false;
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.dimAmount = 0f;
            window.setAttributes(lp);
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            int w = (int) (metrics.widthPixels * 0.92f);
            window.setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void openDocumentTreeInternal(boolean showGuidelineHint) {
        Intent intent = WhatsAppAccessHelper.newOpenDocumentTreeIntentForWhatsappStatuses(this);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        whatsappFolderPickerLauncher.launch(intent);
        if (showGuidelineHint && StatusPickerHintOverlay.canShow(this)) {
            // Give a moment to start navigating; user can tap “Hide” if the hint is in the way.
            StatusPickerHintOverlay.showAfterDelay(this, 2000L);
        }
    }

    private void openSelectedScreenAfterFolderAccess() {
        if ("audio".equals(FilesData.getRecentOrSaved())) {
            startActivity(new Intent(MainActivity.this, AudioActivity.class));
        } else if ("videoSplitter".equals(FilesData.getRecentOrSaved())) {
            startActivity(new Intent(MainActivity.this, VideoSplitterActivity.class));
        } else {
            startActivity(new Intent(MainActivity.this, StoriesActivity.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkNewAppVersionState();
        ((MyApplication) getApplication()).getPlayBillingController().refreshEntitlements(
                () -> runOnUiThread(this::applyPremiumStateToHome));
        showStatusFolderOnboardingDialogIfNeeded();
    }

    private void applyPremiumStateToHome() {
        updateRemoveAdsPromoVisibility();
        refreshPremiumBannerState(R.id.banner_ad_container);
        if (!AdsPolicy.shouldShowAds(this)) {
            mAdView = null;
            destroyHomeNativeAd();
        }
    }

    private void updateRemoveAdsPromoVisibility() {
        if (constraintLayoutRemoveAds == null) {
            return;
        }
        constraintLayoutRemoveAds.setVisibility(
                EntitlementManager.isPremium(this) ? View.GONE : View.VISIBLE);
    }

    private void destroyHomeNativeAd() {
        FrameLayout nativeContainer = findViewById(R.id.native_ad_container);
        if (nativeContainer != null) {
            nativeContainer.removeAllViews();
            nativeContainer.setVisibility(View.GONE);
        }
        if (homeNativeAd != null) {
            homeNativeAd.destroy();
            homeNativeAd = null;
        }
    }

    private void initializeComponents() {
        constraintLayoutVideoSplitter = findViewById(R.id.constraintLayoutVideoSplitter);
        constraintLayoutSettings = findViewById(R.id.constraintLayoutSettings);
        FrameLayout bannerContainer = findViewById(R.id.banner_ad_container);
        bannerContainer.setVisibility(View.GONE);
        bannerContainer.removeAllViews();
        mAdView = null;
        bindBannerAdView(null);
        FrameLayout nativeAdContainer = findViewById(R.id.native_ad_container);
        NativeAdLoader.loadInto(
                nativeAdContainer,
                this,
                getString(R.string.native_ad),
                ad -> homeNativeAd = ad);
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
        constraintLayoutAllStatuses = findViewById(R.id.constraintLayoutAllStatuses);
        constraintLayoutAllStatuses.setOnClickListener(this);
        constraintLayoutRemoveAds = findViewById(R.id.constraintLayoutRemoveAds);
        constraintLayoutRemoveAds.setOnClickListener(this);
        updateRemoveAdsPromoVisibility();
    }

    private final ActivityResultLauncher<Intent> whatsappFolderPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                StatusPickerHintOverlay.dismiss();
                if (statusOnboardingTreeFlow) {
                    statusOnboardingTreeFlow = false;
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri treeUri = result.getData().getData();
                        if (treeUri != null) {
                            int takeFlags = result.getData().getFlags();
                            WhatsAppAccessHelper.saveTreeUri(MainActivity.this, treeUri, takeFlags);
                        }
                    }
                    if (statusFolderOnboardingDialog != null && statusFolderOnboardingDialog.isShowing()) {
                        statusFolderOnboardingDialog.dismiss();
                    }
                    return;
                }
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri treeUri = result.getData().getData();
                    if (treeUri != null) {
                        int takeFlags = result.getData().getFlags();
                        WhatsAppAccessHelper.saveTreeUri(MainActivity.this, treeUri, takeFlags);
                        openSelectedScreenAfterFolderAccess();
                    }
                }
            });

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.constraintLayoutImages) {
            Constant.statusTabSelection = 0;
            FilesData.setRecentOrSaved("recent");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!WhatsAppAccessHelper.hasTreeUri(this)) {
                    showStatusFolderOnboardingDialogIfNeeded();
                    return;
                }
            }

            startActivity(new Intent(MainActivity.this, StoriesActivity.class));
        } else if (id == R.id.constraintLayoutVideos) {
            Constant.statusTabSelection = 1;
            FilesData.setRecentOrSaved("recent");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!WhatsAppAccessHelper.hasTreeUri(this)) {
                    showStatusFolderOnboardingDialogIfNeeded();
                    return;
                }
            }

            startActivity(new Intent(MainActivity.this, StoriesActivity.class));
        } else if (id == R.id.constraintLayoutAllStatuses) {
            Constant.statusTabSelection = 0;
            FilesData.setRecentOrSaved("recent");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!WhatsAppAccessHelper.hasTreeUri(this)) {
                    showStatusFolderOnboardingDialogIfNeeded();
                    return;
                }
            }

            Intent allStories = new Intent(MainActivity.this, StoriesActivity.class);
            allStories.putExtra(StoriesActivity.EXTRA_COMBINED_RECENT_TABS, true);
            startActivity(allStories);
        } else if (id == R.id.constraintLayoutConvertedAudio) {
            FilesData.setRecentOrSaved("audio");
            // Extracted audio lives in app-specific storage; no WhatsApp SAF needed.
            startActivity(new Intent(MainActivity.this, AudioActivity.class));
        } else if (id == R.id.constraintLayoutVideoSplitter) {
            Constant.statusTabSelection = 0;
            FilesData.setRecentOrSaved("videoSplitter");
            // Converted trim output lives in app-specific storage; no WhatsApp SAF needed.
            startActivity(new Intent(MainActivity.this, VideoSplitterActivity.class));
        } else if (id == R.id.constraintLayoutSettings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        } else if (id == R.id.constraintLayoutRemoveAds) {
            Intent premiumSettings = new Intent(MainActivity.this, SettingsActivity.class);
            premiumSettings.putExtra(SettingsActivity.EXTRA_SCROLL_TO_PREMIUM, true);
            startActivity(premiumSettings);
        } else if (id == R.id.constraintLayoutSaved) {
            Constant.statusTabSelection = 0;
            FilesData.setRecentOrSaved("offline");
            startActivity(new Intent(MainActivity.this, StoriesActivity.class));
        }
    }

    @Override
    protected void onDestroy() {
        StatusPickerHintOverlay.dismiss();
        if (statusFolderOnboardingDialog != null && statusFolderOnboardingDialog.isShowing()) {
            statusFolderOnboardingDialog.dismiss();
        }
        unregisterInstallStateUpdListener();
        destroyHomeNativeAd();
        super.onDestroy();
    }

    private void checkForAppUpdate() {
        // Creates instance of the manager.
        appUpdateManager = AppUpdateManagerFactory.create(MainActivity.this);

        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        // Create a listener to track request state updates.
        installStateUpdatedListener = new InstallStateUpdatedListener() {
            @Override
            public void onStateUpdate(InstallState installState) {
                // Show module progress, log state, or install the update.
                if (installState.installStatus() == InstallStatus.DOWNLOADED)
                    // After the update is downloaded, show a notification
                    // and request user confirmation to restart the app.
                    popupSnackbarForCompleteUpdateAndUnregister();
            }
        };

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // Request the update.
                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {

                    // Before starting an update, register a listener for updates.
                    appUpdateManager.registerListener(installStateUpdatedListener);
                    // Start an update.
                    startAppUpdateFlexible(appUpdateInfo);
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    // Start an update.
                    startAppUpdateImmediate(appUpdateInfo);
                }
            }
        });
    }

    private void startAppUpdateImmediate(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    // The current activity making the update request.
                    this,
                    // Include a request code to later monitor this update request.
                    MainActivity.REQ_CODE_VERSION_UPDATE);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }

    private void startAppUpdateFlexible(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    // The current activity making the update request.
                    this,
                    // Include a request code to later monitor this update request.
                    MainActivity.REQ_CODE_VERSION_UPDATE);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
            unregisterInstallStateUpdListener();
        }
    }

    /**
     * Displays the snackbar notification and call to action.
     * Needed only for Flexible app update
     */
    private void popupSnackbarForCompleteUpdateAndUnregister() {
        Snackbar snackbar =
                Snackbar.make(constraintLayoutMain, getString(R.string.update_downloaded), Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.restart, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appUpdateManager.completeUpdate();
            }
        });
        snackbar.setActionTextColor(getResources().getColor(R.color.white));
        snackbar.show();

        unregisterInstallStateUpdListener();
    }

    /**
     * Checks that the update is not stalled during 'onResume()'.
     * However, you should execute this check at all app entry points.
     */
    private void checkNewAppVersionState() {
        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(
                        appUpdateInfo -> {
                            //FLEXIBLE:
                            // If the update is downloaded but not installed,
                            // notify the user to complete the update.
                            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                                popupSnackbarForCompleteUpdateAndUnregister();
                            }

                            //IMMEDIATE:
                            /*if (appUpdateInfo.updateAvailability()
                                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                                // If an in-app update is already running, resume the update.
                                startAppUpdateImmediate(appUpdateInfo);
                            }*/
                        });

    }

    /**
     * Needed only for FLEXIBLE update
     */
    private void unregisterInstallStateUpdListener() {
        if (appUpdateManager != null && installStateUpdatedListener != null)
            appUpdateManager.unregisterListener(installStateUpdatedListener);
    }

    @Override
    public void onActivityResult(int requestCode, final int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {

            case REQ_CODE_VERSION_UPDATE:
                if (resultCode != RESULT_OK) { //RESULT_OK / RESULT_CANCELED / RESULT_IN_APP_UPDATE_FAILED
                    Log.d("Update flow failed!", "Result code: " + resultCode);
                    // If the update is cancelled or fails,
                    // you can request to start the update again.
                    unregisterInstallStateUpdListener();
                }

                break;
        }
    }

}