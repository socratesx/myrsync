package com.linminitools.myrsync;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Shared paywall / upgrade dialogs. */
public final class ProUpgradeUi {

    private ProUpgradeUi() {
    }

    public static void showUpgradeDialog(@NonNull Activity activity, @NonNull String message) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.pro_upgrade_title)
                .setMessage(message)
                .setPositiveButton(R.string.pro_upgrade_buy, (d, w) -> launchPurchase(activity))
                .setNeutralButton(R.string.pro_restore, (d, w) -> restore(activity))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static void showFeatureLocked(@NonNull Activity activity, int messageRes) {
        showUpgradeDialog(activity, activity.getString(messageRes));
    }

    public static void launchPurchase(@NonNull Activity activity) {
        BillingManager billing = billing(activity);
        if (billing != null) {
            billing.launchPurchaseFlow(activity);
        }
    }

    public static void restore(@NonNull Activity activity) {
        BillingManager billing = billing(activity);
        if (billing != null) {
            billing.restorePurchases(activity);
        }
    }

    @Nullable
    private static BillingManager billing(@NonNull Context context) {
        Context app = context.getApplicationContext();
        if (app instanceof myRsyncApplication) {
            return ((myRsyncApplication) app).getBillingManager();
        }
        return null;
    }
}
