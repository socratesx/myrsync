package com.linminitools.myrsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Handles Tasker / Locale {@code FIRE_SETTING} broadcasts.
 */
public final class TaskerPluginReceiver extends BroadcastReceiver {

    private static final String TAG = "TaskerPluginFire";
    private static final String EXTRA_BUNDLE =
            "com.twofortyfouram.locale.intent.extra.BUNDLE";
    private static final String ACTION_FIRE =
            "com.twofortyfouram.locale.intent.action.FIRE_SETTING";
    private static final long WAKELOCK_TIMEOUT_MS = 15L * 60L * 1000L;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.i(TAG, "onReceive action=" + (intent != null ? intent.getAction() : "null")
                + " component=" + (intent != null ? intent.getComponent() : "null"));

        if (intent == null || !ACTION_FIRE.equals(intent.getAction())) {
            Log.w(TAG, "Ignoring non-FIRE_SETTING intent");
            return;
        }

        if (!ProEntitlement.canUseTasker(context)) {
            Log.w(TAG, "Ignoring fire: Pro required");
            appendDebug(context, "TASKER FIRE IGNORED: Pro required");
            return;
        }

        Bundle bundle = intent.getBundleExtra(EXTRA_BUNDLE);
        // Allow a flat config-id extra for adb/device testing when EXTRA_BUNDLE is absent.
        if (bundle == null && intent.hasExtra(PluginBundleValues.BUNDLE_EXTRA_INT_CONFIG)) {
            bundle = new Bundle();
            bundle.putInt(PluginBundleValues.BUNDLE_EXTRA_INT_CONFIG,
                    intent.getIntExtra(PluginBundleValues.BUNDLE_EXTRA_INT_CONFIG, -1));
            bundle.putInt(PluginBundleValues.BUNDLE_EXTRA_INT_VERSION_CODE, 0);
        }
        if (bundle == null) {
            Log.e(TAG, "Missing EXTRA_BUNDLE");
            appendDebug(context, "TASKER FIRE IGNORED: missing EXTRA_BUNDLE");
            return;
        }

        Log.i(TAG, "bundle keys=" + bundle.keySet()
                + " configId=" + bundle.getInt(PluginBundleValues.BUNDLE_EXTRA_INT_CONFIG, -1));

        if (!PluginBundleValues.isBundleValid(bundle)) {
            Log.e(TAG, "Bundle failed validation");
            appendDebug(context, "TASKER FIRE IGNORED: invalid bundle keys=" + bundle.keySet());
            return;
        }

        final int configId = PluginBundleValues.getConfigId(bundle);
        final PendingResult pendingResult = goAsync();
        final Context appContext = context.getApplicationContext();

        final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wakeLock = pm != null
                ? pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myrsync:TaskerPluginFire")
                : null;
        if (wakeLock != null) {
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire(WAKELOCK_TIMEOUT_MS);
        }

        // Safe on a background thread now that RS_Configuration is a plain model class.
        new Thread(() -> {
            try {
                runConfig(appContext, configId);
            } catch (Throwable t) {
                Log.e(TAG, "Fire failed for configId=" + configId, t);
                appendDebug(appContext, "TASKER FIRE FAILED: " + t.getMessage());
            } finally {
                try {
                    if (wakeLock != null && wakeLock.isHeld()) {
                        wakeLock.release();
                    }
                } catch (RuntimeException ignored) {
                    // May already be released by timeout.
                }
                if (pendingResult != null) {
                    pendingResult.finish();
                }
            }
        }, "TaskerPluginFire").start();
    }

    private static void runConfig(Context context, int configId) {
        RS_Configuration config = myRsyncApplication.loadConfigById(context, configId);
        if (config == null) {
            Log.e(TAG, "No config for id=" + configId);
            appendDebug(context,
                    "CONFIGURATION RUN TRIGGERED FROM TASKER PLUGIN BUT NO CONFIG FOUND WITH ID "
                            + configId);
            return;
        }

        Log.i(TAG, "Starting config id=" + configId + " name=" + config.name
                + " thread=" + Thread.currentThread().getName());
        appendDebug(context, "CONFIGURATION RUN " + config.name + " TRIGGERED FROM TASKER PLUGIN");

        Thread rsyncThread = config.executeConfig(context, null);
        if (rsyncThread == null) {
            Log.w(TAG, "executeConfig returned null (likely aborted by network gate)");
            appendDebug(context, "TASKER FIRE: executeConfig aborted before start for " + config.name);
            return;
        }

        try {
            rsyncThread.join(WAKELOCK_TIMEOUT_MS - 5_000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "Interrupted while waiting for config run", e);
        }
        Log.i(TAG, "Finished waiting for config id=" + configId);
    }

    private static void appendDebug(Context context, String message) {
        File debugLog = new File(context.getApplicationInfo().dataDir + "/debug.log");
        Locale currentLocale = AppLocales.of(context);
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", currentLocale);
        try (FileWriter debugWriter = new FileWriter(debugLog, true)) {
            debugWriter.append("\n\n[ ")
                    .append(formatter.format(Calendar.getInstance().getTime()))
                    .append(" ] ")
                    .append(message);
        } catch (IOException e) {
            Log.e(TAG, "Failed writing debug.log", e);
        }
    }
}
