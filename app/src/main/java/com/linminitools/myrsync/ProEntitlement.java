package com.linminitools.myrsync;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import static com.linminitools.myrsync.myRsyncApplication.configs;
import static com.linminitools.myrsync.myRsyncApplication.schedulers;

/**
 * Free vs Pro entitlements. Free: one configuration, no schedules, no Tasker.
 * Pro is unlocked via Play Billing product {@link BuildConfig#PRO_PRODUCT_ID}.
 */
public final class ProEntitlement {

    private static final String TAG = "ProEntitlement";
    private static final String PREFS = "pro";
    private static final String KEY_UNLOCKED = "pro_unlocked";
    private static final String KEY_MIGRATED = "freemium_migrated";

    private ProEntitlement() {
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isPro(@NonNull Context ctx) {
        return prefs(ctx).getBoolean(KEY_UNLOCKED, false);
    }

    public static void setPro(@NonNull Context ctx, boolean unlocked) {
        boolean wasPro = isPro(ctx);
        prefs(ctx).edit().putBoolean(KEY_UNLOCKED, unlocked).apply();
        applyTaskerComponents(ctx, unlocked);
        if (wasPro && !unlocked) {
            clearSchedules(ctx);
        }
        if (!wasPro && unlocked) {
            Log.i(TAG, "Pro unlocked");
        }
    }

    /**
     * Existing installs that already have configs are grandfathered to Pro once so beta users
     * are not locked out. Fresh installs stay on Free until purchase.
     */
    public static void migrateIfNeeded(@NonNull Context ctx) {
        SharedPreferences p = prefs(ctx);
        if (p.getBoolean(KEY_MIGRATED, false)) {
            applyTaskerComponents(ctx, isPro(ctx));
            if (!isPro(ctx)) {
                clearSchedules(ctx);
            }
            return;
        }
        boolean hasConfigs = !configs.isEmpty()
                || !ctx.getSharedPreferences("configs", Context.MODE_PRIVATE).getAll().isEmpty();
        p.edit()
                .putBoolean(KEY_MIGRATED, true)
                .putBoolean(KEY_UNLOCKED, hasConfigs)
                .apply();
        applyTaskerComponents(ctx, hasConfigs);
        if (!hasConfigs) {
            clearSchedules(ctx);
        }
        Log.i(TAG, "Freemium migration: pro=" + hasConfigs);
    }

    public static boolean canCreateConfig(@NonNull Context ctx) {
        return isPro(ctx) || configs.size() < 1;
    }

    public static boolean canUseSchedules(@NonNull Context ctx) {
        return isPro(ctx);
    }

    public static boolean canUseTasker(@NonNull Context ctx) {
        return isPro(ctx);
    }

    static void applyTaskerComponents(@NonNull Context ctx, boolean enabled) {
        PackageManager pm = ctx.getPackageManager();
        int state = enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        ComponentName[] components = new ComponentName[]{
                new ComponentName(ctx, TaskerPluginEditActivity.class),
                new ComponentName(ctx, TaskerPluginReceiver.class)
        };
        for (ComponentName component : components) {
            try {
                pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP);
            } catch (Exception e) {
                Log.w(TAG, "Unable to update Tasker component " + component.getClassName(), e);
            }
        }
    }

    /** Remove all schedules when Free (schedules are a Pro feature). */
    static void clearSchedules(@NonNull Context ctx) {
        if (schedulers.isEmpty()) {
            return;
        }
        for (Scheduler s : new ArrayList<>(schedulers)) {
            s.cancelAlarm(ctx);
            s.deleteFromDisk();
        }
        schedulers.clear();
        Log.i(TAG, "Cleared schedules for Free tier");
    }
}
