package com.linminitools.myrsync;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Tasker / Locale plug-in bundle extras for firing a saved configuration.
 */
public final class PluginBundleValues {

    private static final String TAG = "PluginBundleValues";

    @NonNull
    public static final String BUNDLE_EXTRA_INT_CONFIG
            = "com.linminitools.myrsync.INT_CONFIG";

    @NonNull
    public static final String BUNDLE_EXTRA_INT_VERSION_CODE
            = "com.linminitools.myrsync.INT_VERSION_CODE";

    public static boolean isBundleValid(@Nullable final Bundle bundle) {
        if (bundle == null || !bundle.containsKey(BUNDLE_EXTRA_INT_CONFIG)) {
            return false;
        }
        // getInt returns 0 for a missing key; key presence was checked above.
        // Wrong types (e.g. String) typically yield 0 / default, which we reject.
        int id = bundle.getInt(BUNDLE_EXTRA_INT_CONFIG, -1);
        if (id < 0) {
            Log.e(TAG, "Bundle has invalid " + BUNDLE_EXTRA_INT_CONFIG);
            return false;
        }
        return true;
    }

    @NonNull
    public static Bundle generateBundle(@NonNull final Context context, int configId) {
        final Bundle result = new Bundle();
        result.putInt(BUNDLE_EXTRA_INT_VERSION_CODE, readVersionCode(context));
        result.putInt(BUNDLE_EXTRA_INT_CONFIG, configId);
        return result;
    }

    public static int getConfigId(@NonNull final Bundle bundle) {
        return bundle.getInt(BUNDLE_EXTRA_INT_CONFIG);
    }

    @SuppressWarnings("deprecation")
    private static int readVersionCode(@NonNull Context context) {
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                return (int) context.getPackageManager()
                        .getPackageInfo(context.getPackageName(), 0)
                        .getLongVersionCode();
            }
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private PluginBundleValues() {
        throw new UnsupportedOperationException("This class is non-instantiable");
    }
}
