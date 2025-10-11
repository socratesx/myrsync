package com.linminitools.myrsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linminitools.myrsync.tasker.TaskerPluginContract;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.Context.MODE_PRIVATE;

public final class TaskerPluginReceiver extends BroadcastReceiver {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final String TAG = "TaskerPluginReceiver";

    @Override
    public void onReceive(@NonNull final Context context, @Nullable final Intent intent) {
        final Bundle bundle = TaskerPluginContract.extractBundle(intent);
        if (bundle == null || !PluginBundleValues.isBundleValid(bundle)) {
            return;
        }

        final PendingResult pendingResult = goAsync();
        EXECUTOR.execute(() -> {
            try {
                firePluginSetting(context.getApplicationContext(), bundle);
            } finally {
                pendingResult.finish();
            }
        });
    }

    private void firePluginSetting(@NonNull final Context context, @NonNull final Bundle bundle) {
        final int configId = PluginBundleValues.getConfigId(bundle);

        final String debugLogPath = context.getApplicationInfo().dataDir + "/debug.log";
        final File debugLog = new File(debugLogPath);
        final Locale currentLocale = context.getResources().getConfiguration().locale;
        final SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", currentLocale);

        Log.i(TAG, "Received Tasker request for configId=" + configId);

        try {
            boolean configFound = false;
            final SharedPreferences configPrefs = context.getSharedPreferences("configs", MODE_PRIVATE);
            final Map<String, ?> configKeys = configPrefs.getAll();

            for (final Map.Entry<String, ?> entry : configKeys.entrySet()) {
                if (entry.getKey().contains("rs_id_")) {
                    final String id = String.valueOf(entry.getValue());

                    if (Integer.parseInt(id) == configId) {
                        configFound = true;

                        final RS_Configuration config = new RS_Configuration((Integer) entry.getValue());
                        config.rs_user = configPrefs.getString("rs_user_" + id, "");
                        config.rs_ip = configPrefs.getString("rs_ip_" + id, "");
                        config.rs_port = configPrefs.getString("rs_port_" + id, "");
                        config.rs_options = configPrefs.getString("rs_options_" + id, "");
                        config.rs_module = configPrefs.getString("rs_module_" + id, "");
                        config.local_path = configPrefs.getString("local_path_" + id, "");
                        config.name = configPrefs.getString("rs_name_" + id, "");
                        config.rs_mode = configPrefs.getString("rs_mode_" + id, "push");
                        config.addedOn = configPrefs.getLong("rs_addedon_" + id, 0);

                        appendDebugLog(debugLog, formatter,
                                "CONFIGURATION RUN " + config.name + " TRIGGERED FROM TASKER PLUGIN");
                        try {
                            Log.i(TAG, "Executing configuration '" + config.name + "' (id=" + config.id + ") from Tasker plugin");
                            config.executeConfig(context, null);
                        } catch (Exception executionError) {
                            final String errorMessage = executionError.getMessage() == null
                                    ? executionError.toString()
                                    : executionError.getMessage();
                            Log.e(TAG, "Failed to execute configuration '" + config.name + "' from Tasker plugin", executionError);
                            appendDebugLog(debugLog, formatter,
                                    "CONFIGURATION RUN " + config.name + " FAILED TO START FROM TASKER PLUGIN: " + errorMessage);
                            MAIN_HANDLER.post(() -> Toast.makeText(context,
                                    "Failed to start requested configuration.", Toast.LENGTH_SHORT).show());
                        }
                        break;
                    }
                }
            }

            if (!configFound) {
                appendDebugLog(debugLog, formatter,
                        "CONFIGURATION RUN TRIGGERED FROM TASKER PLUGIN BUT NO CONFIG FOUND WITH ID " + configId);
                MAIN_HANDLER.post(() -> Toast.makeText(context,
                        "No configuration found for Tasker request.", Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            final String errorMessage = e.getMessage() == null ? e.toString() : e.getMessage();
            Log.e(TAG, "Unable to process Tasker plugin request", e);
            appendDebugLog(debugLog, formatter,
                    "CONFIGURATION RUN FAILED TO START FROM TASKER PLUGIN: " + errorMessage);
            MAIN_HANDLER.post(() -> Toast.makeText(context,
                    "Failed to process Tasker request.", Toast.LENGTH_SHORT).show());
        }
    }

    private static void appendDebugLog(@NonNull File debugLog, @NonNull SimpleDateFormat formatter, @NonNull String message) {
        try (FileWriter debugWriter = new FileWriter(debugLog, true)) {
            CharSequence logMessage = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + message;
            debugWriter.append(logMessage);
        } catch (IOException e) {
            Log.e(TAG, "Unable to write to debug log", e);
        }
    }

}
