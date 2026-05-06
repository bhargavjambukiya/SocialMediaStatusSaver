package com.studio.statusvault.billing;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.studio.statusvault.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Google Play Billing for a one-time in-app premium unlock (no subscription).
 * Product id and price are configured in Play Console.
 */
public final class PlayBillingController implements PurchasesUpdatedListener {
    private static final String TAG = "PlayBillingController";

    public interface PurchaseFlowCallback {
        void onPurchasePending();

        void onPurchaseCompleted();

        void onUserCancelled();

        void onPurchaseError(@NonNull String message);
    }

    public interface ProductDetailsCallback {
        void onLoaded(@NonNull ProductDetails productDetails);

        void onError(@NonNull String message);
    }

    private final Application app;
    @Nullable
    private BillingClient billingClient;
    private boolean connecting;
    private final Object pendingLock = new Object();
    /** Run after the next completed purchase query (e.g. UI refresh, restore toast). */
    private final ArrayList<Runnable> pendingEntitlementCallbacks = new ArrayList<>();

    @Nullable
    private PurchaseFlowCallback purchaseFlowCallback;

    public PlayBillingController(@NonNull Application application) {
        this.app = application;
    }

    public void start() {
        if (billingClient != null && billingClient.isReady()) {
            queryPurchasesAndDrainCallbacks();
            return;
        }
        if (connecting) {
            return;
        }
        connecting = true;
        billingClient = BillingClient.newBuilder(app)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                connecting = false;
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryPurchasesAndDrainCallbacks();
                } else {
                    Log.e(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                    runPendingEntitlementCallbacks();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected");
                connecting = false;
                billingClient = null;
            }
        });
    }

    public boolean isReady() {
        return billingClient != null && billingClient.isReady();
    }

    /**
     * Queries existing one-time ({@link BillingClient.ProductType#INAPP}) purchases via
     * {@code BillingClient#queryPurchasesAsync} with {@link BillingClient.ProductType#INAPP},
     * updates {@link EntitlementManager}, then runs queued finishers on the main thread.
     * <p>Use the same path for Restore purchase and when the app resumes — do not launch a billing sheet.
     * If the client is not ready yet, the callback runs after setup + query completes (or setup failure drain).
     */
    public void refreshEntitlements(@Nullable Runnable onFinished) {
        if (onFinished != null) {
            synchronized (pendingLock) {
                pendingEntitlementCallbacks.add(onFinished);
            }
        }
        if (billingClient == null || !billingClient.isReady()) {
            start();
            return;
        }
        queryPurchasesAndDrainCallbacks();
    }

    private void queryPurchasesAndDrainCallbacks() {
        if (billingClient == null || !billingClient.isReady()) {
            return;
        }
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                (billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        processPurchases(purchases);
                    } else {
                        Log.w(TAG, "queryPurchasesAsync failed: " + billingResult.getDebugMessage());
                    }
                    runPendingEntitlementCallbacks();
                });
    }

    private void runPendingEntitlementCallbacks() {
        ArrayList<Runnable> batch;
        synchronized (pendingLock) {
            batch = new ArrayList<>(pendingEntitlementCallbacks);
            pendingEntitlementCallbacks.clear();
        }
        if (batch.isEmpty()) {
            return;
        }
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> {
            for (Runnable r : batch) {
                try {
                    r.run();
                } catch (RuntimeException e) {
                    Log.e(TAG, "pending entitlement callback crashed", e);
                }
            }
        });
    }

    public void launchPremiumPurchase(
            @NonNull Activity activity,
            @NonNull PurchaseFlowCallback callback) {
        if (billingClient == null || !billingClient.isReady()) {
            callback.onPurchaseError(app.getString(R.string.billing_not_ready));
            return;
        }
        String productId = activity.getString(R.string.one_time_premium_product_id);
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build());
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();
        billingClient.queryProductDetailsAsync(params, (br, detailsList) -> {
            if (br.getResponseCode() != BillingClient.BillingResponseCode.OK
                    || detailsList == null
                    || detailsList.isEmpty()) {
                callback.onPurchaseError(app.getString(R.string.billing_product_unavailable));
                return;
            }
            ProductDetails productDetails = detailsList.get(0);
            ProductDetails.OneTimePurchaseOfferDetails offer = productDetails.getOneTimePurchaseOfferDetails();
            if (offer == null) {
                callback.onPurchaseError(app.getString(R.string.billing_no_offer));
                return;
            }
            BillingFlowParams.ProductDetailsParams.Builder paramsBuilder =
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails);
            // Billing Library 6+ exposes getOfferToken(); 7.1.x may not — reflection keeps one APK compatible.
            String offerToken = oneTimeOfferToken(offer);
            if (offerToken != null) {
                paramsBuilder.setOfferToken(offerToken);
            }
            BillingFlowParams.ProductDetailsParams productDetailsParams = paramsBuilder.build();
            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(Collections.singletonList(productDetailsParams))
                    .build();
            BillingResult launchResult = billingClient.launchBillingFlow(activity, flowParams);
            if (launchResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                String msg = launchResult.getDebugMessage();
                if (msg == null || msg.isEmpty()) {
                    msg = app.getString(R.string.billing_launch_failed);
                }
                callback.onPurchaseError(msg);
                purchaseFlowCallback = null;
            } else {
                purchaseFlowCallback = callback;
            }
        });
    }

    /**
     * Loads {@link ProductDetails} for the one-time premium product (e.g. formatted price for UI).
     */
    public void queryPremiumProductDetails(@NonNull ProductDetailsCallback callback) {
        if (billingClient == null || !billingClient.isReady()) {
            callback.onError(app.getString(R.string.billing_not_ready));
            return;
        }
        String productId = app.getString(R.string.one_time_premium_product_id);
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build());
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();
        billingClient.queryProductDetailsAsync(params, (br, detailsList) -> {
            if (br.getResponseCode() != BillingClient.BillingResponseCode.OK
                    || detailsList == null
                    || detailsList.isEmpty()) {
                callback.onError(app.getString(R.string.billing_product_unavailable));
                return;
            }
            callback.onLoaded(detailsList.get(0));
        });
    }

    /**
     * Returns the one-time offer token when available (needed for multi-offer products on newer Play Billing).
     */
    @Nullable
    private static String oneTimeOfferToken(@NonNull ProductDetails.OneTimePurchaseOfferDetails offer) {
        try {
            java.lang.reflect.Method m = offer.getClass().getMethod("getOfferToken");
            Object token = m.invoke(offer);
            return token instanceof String ? (String) token : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /**
     * Play’s {@link Purchase#getOriginalJson()} {@code purchaseState} is authoritative for
     * canceled/refunded one-time purchases: {@code 1} = canceled (includes refund), {@code 0} = purchased,
     * {@code 2} = pending. The Billing client can still report {@link Purchase.PurchaseState#PURCHASED}
     * briefly after a refund, so we must not grant premium from those rows.
     */
    private static boolean isCanceledOrRefundedInPurchaseJson(@NonNull Purchase purchase) {
        try {
            JSONObject o = new JSONObject(purchase.getOriginalJson());
            int ps = o.optInt("purchaseState", -1);
            return ps == 1;
        } catch (JSONException e) {
            return false;
        }
    }

    private void processPurchases(@Nullable List<Purchase> purchases) {
        String productId = app.getString(R.string.one_time_premium_product_id);
        boolean active = false;
        boolean pending = false;
        if (purchases != null) {
            for (Purchase purchase : purchases) {
                if (!purchase.getProducts().contains(productId)) {
                    continue;
                }
                if (isCanceledOrRefundedInPurchaseJson(purchase)) {
                    continue;
                }
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                    pending = true;
                    continue;
                }
                if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
                    continue;
                }
                active = true;
                if (!purchase.isAcknowledged() && billingClient != null) {
                    AcknowledgePurchaseParams acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();
                    billingClient.acknowledgePurchase(acknowledgeParams, billingResult -> { });
                }
                break;
            }
        }
        EntitlementManager.setPremium(app, active);
        if (purchaseFlowCallback != null) {
            if (active) {
                purchaseFlowCallback.onPurchaseCompleted();
                purchaseFlowCallback = null;
            } else if (pending) {
                purchaseFlowCallback.onPurchasePending();
            }
        }
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            processPurchases(purchases != null ? purchases : Collections.emptyList());
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            if (purchaseFlowCallback != null) {
                purchaseFlowCallback.onUserCancelled();
                purchaseFlowCallback = null;
            }
        } else {
            if (purchaseFlowCallback != null) {
                String msg = billingResult.getDebugMessage();
                if (msg == null || msg.isEmpty()) {
                    msg = app.getString(R.string.billing_launch_failed);
                }
                purchaseFlowCallback.onPurchaseError(msg);
                purchaseFlowCallback = null;
            }
            refreshEntitlements(null);
        }
    }
}
