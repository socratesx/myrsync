package com.linminitools.myrsync;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Google Play Billing for the one-time {@link BuildConfig#PRO_PRODUCT_ID} unlock.
 */
public final class BillingManager implements PurchasesUpdatedListener {

    public interface Listener {
        void onProStatusChanged(boolean isPro);
    }

    private static final String TAG = "BillingManager";

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new ArrayList<>();

    private BillingClient billingClient;
    @Nullable
    private ProductDetails proProductDetails;
    private boolean connecting;

    public BillingManager(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void addListener(@NonNull Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    public void start() {
        if (billingClient != null) {
            queryPurchases();
            return;
        }
        billingClient = BillingClient.newBuilder(appContext)
                .setListener(this)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();
        startConnection();
    }

    private void startConnection() {
        if (billingClient == null || connecting || billingClient.isReady()) {
            if (billingClient != null && billingClient.isReady()) {
                queryProductDetails();
                queryPurchases();
            }
            return;
        }
        connecting = true;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                connecting = false;
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails();
                    queryPurchases();
                } else {
                    Log.w(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                connecting = false;
                Log.w(TAG, "Billing service disconnected");
            }
        });
    }

    public void launchPurchaseFlow(@NonNull Activity activity) {
        if (billingClient == null || !billingClient.isReady()) {
            start();
            toast(activity, R.string.pro_billing_unavailable);
            return;
        }
        if (proProductDetails == null) {
            queryProductDetails();
            toast(activity, R.string.pro_product_unavailable);
            return;
        }
        BillingFlowParams.ProductDetailsParams productParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(proProductDetails)
                        .build();
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(productParams))
                .build();
        BillingResult result = billingClient.launchBillingFlow(activity, flowParams);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "launchBillingFlow failed: " + result.getDebugMessage());
            toast(activity, R.string.pro_billing_unavailable);
        }
    }

    public void restorePurchases(@Nullable Activity activity) {
        if (billingClient == null || !billingClient.isReady()) {
            start();
            if (activity != null) {
                toast(activity, R.string.pro_billing_unavailable);
            }
            return;
        }
        queryPurchases(activity, true);
    }

    private void queryProductDetails() {
        if (billingClient == null || !billingClient.isReady()) {
            return;
        }
        List<QueryProductDetailsParams.Product> products = Collections.singletonList(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BuildConfig.PRO_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build());
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build();
        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "queryProductDetails failed: " + billingResult.getDebugMessage());
                return;
            }
            proProductDetails = null;
            if (productDetailsList != null) {
                for (ProductDetails details : productDetailsList) {
                    if (BuildConfig.PRO_PRODUCT_ID.equals(details.getProductId())) {
                        proProductDetails = details;
                        break;
                    }
                }
            }
            Log.i(TAG, "Pro product details loaded=" + (proProductDetails != null));
        });
    }

    public void queryPurchases() {
        queryPurchases(null, false);
    }

    private void queryPurchases(@Nullable Activity activity, boolean fromRestoreUi) {
        if (billingClient == null || !billingClient.isReady()) {
            return;
        }
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "queryPurchases failed: " + billingResult.getDebugMessage());
                if (fromRestoreUi && activity != null) {
                    toast(activity, R.string.pro_restore_failed);
                }
                return;
            }
            boolean owned = processPurchases(purchases);
            if (fromRestoreUi && activity != null) {
                toast(activity, owned ? R.string.pro_restore_success : R.string.pro_restore_none);
            }
        });
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult,
                                   @Nullable List<Purchase> purchases) {
        int code = billingResult.getResponseCode();
        if (code == BillingClient.BillingResponseCode.OK) {
            processPurchases(purchases);
        } else if (code == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(TAG, "Purchase canceled");
        } else if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            queryPurchases();
        } else {
            Log.w(TAG, "onPurchasesUpdated: " + billingResult.getDebugMessage());
        }
    }

    private boolean processPurchases(@Nullable List<Purchase> purchases) {
        boolean owned = false;
        if (purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
                    continue;
                }
                if (purchase.getProducts().contains(BuildConfig.PRO_PRODUCT_ID)) {
                    owned = true;
                    acknowledgeIfNeeded(purchase);
                }
            }
        }
        applyEntitlement(owned);
        return owned;
    }

    private void acknowledgeIfNeeded(@NonNull Purchase purchase) {
        if (purchase.isAcknowledged() || billingClient == null) {
            return;
        }
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        billingClient.acknowledgePurchase(params, result -> {
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "acknowledge failed: " + result.getDebugMessage());
            }
        });
    }

    private void applyEntitlement(boolean owned) {
        boolean wasPro = ProEntitlement.isPro(appContext);
        if (owned) {
            ProEntitlement.setPro(appContext, true);
        } else if (!BuildConfig.DEBUG) {
            // In release, trust Play as source of truth (refunds revoke Pro).
            // Keep grandfathered unlock if never purchased and migration set Pro — only revoke
            // when Play explicitly says not owned AND we previously had a Play purchase flag.
            // Simpler policy: if Play reports no ownership, do not force-revoke grandfathered Pro.
            // Only set true from Play; revocation requires an owned->not-owned transition after purchase.
            if (wasPro && prefsHadPlayPurchase()) {
                ProEntitlement.setPro(appContext, false);
            }
        }
        // Always notify; Settings UI refreshes from isPro().
        boolean nowPro = ProEntitlement.isPro(appContext);
        if (owned) {
            markPlayPurchase(true);
        }
        notifyListeners(nowPro);
        if (owned && !wasPro) {
            mainHandler.post(() ->
                    Toast.makeText(appContext, R.string.pro_unlocked_toast, Toast.LENGTH_LONG).show());
        }
    }

    private static final String KEY_HAD_PLAY_PURCHASE = "had_play_purchase";

    private boolean prefsHadPlayPurchase() {
        return appContext.getSharedPreferences("pro", Context.MODE_PRIVATE)
                .getBoolean(KEY_HAD_PLAY_PURCHASE, false);
    }

    private void markPlayPurchase(boolean had) {
        appContext.getSharedPreferences("pro", Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_HAD_PLAY_PURCHASE, had)
                .apply();
    }

    private void notifyListeners(boolean isPro) {
        mainHandler.post(() -> {
            for (Listener listener : new ArrayList<>(listeners)) {
                listener.onProStatusChanged(isPro);
            }
        });
    }

    private void toast(@NonNull Activity activity, int resId) {
        mainHandler.post(() -> Toast.makeText(activity, resId, Toast.LENGTH_LONG).show());
    }
}
