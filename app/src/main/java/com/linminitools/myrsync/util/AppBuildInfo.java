package com.linminitools.myrsync.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;

public final class AppBuildInfo {

    private AppBuildInfo() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }

    public static int getVersionCode(@NonNull final Context context) {
        try {
            final PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return (int) packageInfo.getLongVersionCode();
            }
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new AssertionError("Unable to determine version code", e); //$NON-NLS-1$
        }
    }
}
