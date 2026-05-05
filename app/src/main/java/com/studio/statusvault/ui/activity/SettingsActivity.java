package com.studio.statusvault.ui.activity;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Paint;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.studio.statusvault.BuildConfig;
import com.studio.statusvault.MyApplication;
import com.studio.statusvault.R;
import com.studio.statusvault.billing.EntitlementManager;
import com.studio.statusvault.billing.PlayBillingController;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    /** When true, scroll the premium card into view (e.g. opened from home “Remove ads”). */
    public static final String EXTRA_SCROLL_TO_PREMIUM =
            "com.studio.statusvault.SettingsActivity.EXTRA_SCROLL_TO_PREMIUM";
    MaterialCardView cardPremium;
    MaterialButton buttonPremiumUpgrade;
    View rowSettingsLanguage, rowSettingsPrivacy, rowSettingsRate, rowSettingsShare, rowSettingsSupport;
    TextView textViewRestorePurchase;
    TextView textViewPremiumPrice;
    TextView textViewPremiumSubline;
    TextView textViewPremiumDisclaimer;
    TextView textViewPremiumBadge;
    private boolean purchaseInProgress;
    private boolean restoreInProgress;

    private final ActivityResultLauncher<Intent> languageScreen = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    recreate();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_settings);

        setUpToolbar();
        initializeComponents();
        ((MyApplication) getApplication()).getPlayBillingController().start();
        bindPremiumUi();
        loadPremiumPriceDetails();
        maybeScrollToPremiumFromIntent();
    }

    private void maybeScrollToPremiumFromIntent() {
        if (!getIntent().getBooleanExtra(EXTRA_SCROLL_TO_PREMIUM, false)) {
            return;
        }
        ScrollView scroll = findViewById(R.id.settingsScrollView);
        if (scroll == null || cardPremium == null) {
            return;
        }
        scroll.post(() -> scroll.smoothScrollTo(0, cardPremium.getTop()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlayBillingController billing = ((MyApplication) getApplication()).getPlayBillingController();
        billing.refreshEntitlements(this::bindPremiumUi);
    }

    private void initializeComponents() {
        cardPremium = findViewById(R.id.cardPremium);
        buttonPremiumUpgrade = findViewById(R.id.buttonPremiumUpgrade);
        textViewRestorePurchase = findViewById(R.id.textViewRestorePurchase);
        textViewRestorePurchase.getPaint().setFlags(
                textViewRestorePurchase.getPaint().getFlags() | Paint.UNDERLINE_TEXT_FLAG);
        textViewRestorePurchase.setOnClickListener(this);
        cardPremium.setOnClickListener(this);
        buttonPremiumUpgrade.setOnClickListener(this);
        textViewPremiumPrice = findViewById(R.id.textViewPremiumPrice);
        textViewPremiumSubline = findViewById(R.id.textViewPremiumSubline);
        textViewPremiumDisclaimer = findViewById(R.id.textViewPremiumDisclaimer);
        textViewPremiumBadge = findViewById(R.id.textViewPremiumBadge);

        rowSettingsPrivacy = findViewById(R.id.rowSettingsPrivacy);
        rowSettingsRate = findViewById(R.id.rowSettingsRate);
        rowSettingsShare = findViewById(R.id.rowSettingsShare);
        rowSettingsSupport = findViewById(R.id.rowSettingsSupport);
        rowSettingsLanguage = findViewById(R.id.rowSettingsLanguage);
        rowSettingsLanguage.setOnClickListener(this);
        rowSettingsPrivacy.setOnClickListener(this);
        rowSettingsRate.setOnClickListener(this);
        rowSettingsShare.setOnClickListener(this);
        rowSettingsSupport.setOnClickListener(this);

    }

    private void bindPremiumUi() {
        boolean premium = EntitlementManager.isPremium(this);
        if (premium) {
            textViewPremiumPrice.setText(getString(R.string.settings_premium_status_active));
        } else if (textViewPremiumPrice.getText() == null || textViewPremiumPrice.getText().length() == 0) {
            textViewPremiumPrice.setText(getString(R.string.settings_premium_price));
        }
        textViewPremiumSubline.setText(premium
                ? getString(R.string.settings_premium_subline_active)
                : getString(R.string.settings_premium_subline));
        textViewPremiumDisclaimer.setVisibility(premium ? View.GONE : View.VISIBLE);
        textViewPremiumBadge.setVisibility(premium ? View.GONE : View.VISIBLE);
        buttonPremiumUpgrade.setVisibility(premium ? View.GONE : View.VISIBLE);
        textViewRestorePurchase.setVisibility(premium ? View.GONE : View.VISIBLE);
        syncPurchaseControls();
    }

    private void syncPurchaseControls() {
        boolean busy = purchaseInProgress || restoreInProgress;
        buttonPremiumUpgrade.setEnabled(!busy);
        textViewRestorePurchase.setEnabled(!busy);
    }

    private void loadPremiumPriceDetails() {
        if (EntitlementManager.isPremium(this)) {
            return;
        }
        PlayBillingController billing = ((MyApplication) getApplication()).getPlayBillingController();
        if (!billing.isReady()) {
            billing.start();
            return;
        }
        billing.queryPremiumProductDetails(new PlayBillingController.ProductDetailsCallback() {
            @Override
            public void onLoaded(@NonNull com.android.billingclient.api.ProductDetails productDetails) {
                com.android.billingclient.api.ProductDetails.OneTimePurchaseOfferDetails offer =
                        productDetails.getOneTimePurchaseOfferDetails();
                if (offer == null) {
                    return;
                }
                String formattedPrice = offer.getFormattedPrice();
                textViewPremiumPrice.setText(getString(R.string.settings_premium_price_dynamic, formattedPrice));
            }

            @Override
            public void onError(@NonNull String message) {
                // Keep fallback text from strings.xml
            }
        });
    }

    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.title_settings));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.cardPremium || id == R.id.buttonPremiumUpgrade) {
            if (EntitlementManager.isPremium(this)) {
                return;
            }
            PlayBillingController billing = ((MyApplication) getApplication()).getPlayBillingController();
            if (!billing.isReady()) {
                billing.start();
                Toast.makeText(this, R.string.billing_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }
            purchaseInProgress = true;
            syncPurchaseControls();
            billing.launchPremiumPurchase(this, new PlayBillingController.PurchaseFlowCallback() {
                @Override
                public void onPurchasePending() {
                    purchaseInProgress = false;
                    syncPurchaseControls();
                    bindPremiumUi();
                    Toast.makeText(SettingsActivity.this, R.string.billing_purchase_pending, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onPurchaseCompleted() {
                    purchaseInProgress = false;
                    syncPurchaseControls();
                    bindPremiumUi();
                    Toast.makeText(SettingsActivity.this, R.string.billing_purchase_success, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onUserCancelled() {
                    purchaseInProgress = false;
                    syncPurchaseControls();
                    Toast.makeText(SettingsActivity.this, R.string.billing_purchase_cancelled, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onPurchaseError(@NonNull String message) {
                    purchaseInProgress = false;
                    syncPurchaseControls();
                    Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        } else if (id == R.id.textViewRestorePurchase) {
            // Silent INAPP reconciliation only — queryPurchasesAsync; never launch billing sheet.
            PlayBillingController billing = ((MyApplication) getApplication()).getPlayBillingController();
            billing.start();
            restoreInProgress = true;
            syncPurchaseControls();
            billing.refreshEntitlements(() -> {
                restoreInProgress = false;
                syncPurchaseControls();
                bindPremiumUi();
                if (EntitlementManager.isPremium(SettingsActivity.this)) {
                    Toast.makeText(SettingsActivity.this, R.string.billing_restore_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this, R.string.billing_restore_not_found, Toast.LENGTH_LONG).show();
                }
            });
        } else if (id == R.id.rowSettingsPrivacy) {
            String url = "https://sites.google.com/view/social-media-saver/home";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } else if (id == R.id.rowSettingsRate) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID)));
        } else if (id == R.id.rowSettingsShare) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    "Hey, download this app! at: https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        } else if (id == R.id.rowSettingsSupport) {
            startActivity(new Intent(this, SupportActivity.class));
        } else if (id == R.id.rowSettingsLanguage) {
            Intent language = new Intent(this, LanguageActivity.class);
            language.putExtra(LanguageActivity.EXTRA_FROM_SETTINGS, true);
            languageScreen.launch(language);
        }
    }
}
