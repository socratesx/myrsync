package com.linminitools.myrsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;



public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";


    @Override
    public void onReceive(Context ctx, Intent i) {

        int config_id_to_run = i.getIntExtra("config_id", -1);
        int scheduler_id = i.getIntExtra("sched_id", -1);

        String Debug_log_path = ctx.getApplicationInfo().dataDir + "/debug.log";
        File debug_log = new File(Debug_log_path);
        Locale current_locale = ctx.getResources().getConfiguration().locale;
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);

        Log.i(TAG, "onReceive action=" + i.getAction()
                + " config_id=" + config_id_to_run
                + " sched_id=" + scheduler_id);

        // Scheduled fires require Pro. Boot re-arm path still runs below and will no-op if Free.
        String action = i.getAction();
        if (action != null
                && action.equals("android.intent.action.RUN")
                && !ProEntitlement.canUseSchedules(ctx)) {
            Log.w(TAG, "Ignoring scheduled run: Pro required");
            try {
                FileWriter debug_writer = new FileWriter(debug_log, true);
                debug_writer.append("\n[ ")
                        .append(formatter.format(Calendar.getInstance().getTime()))
                        .append(" ] ALARM IGNORED: Pro required");
                debug_writer.close();
            } catch (IOException ignored) {
            }
            ProEntitlement.clearSchedules(ctx);
            return;
        }

        if ((Objects.requireNonNull(i.getAction())).equals("android.intent.action.BOOT_COMPLETED")) {
            try {
                FileWriter debug_writer = new FileWriter(debug_log, true);
                CharSequence message = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + "--------DEVICE REBOOTED--------";
                debug_writer.append(message);
                debug_writer.close();

                SharedPreferences sched_prefs = ctx.getSharedPreferences("schedulers", MODE_PRIVATE);

                Map<String, ?> sched_keys = sched_prefs.getAll();
                for (Map.Entry<String, ?> entry : sched_keys.entrySet()) {
                    if (entry.getKey().startsWith("id_")) {
                        String id = String.valueOf(entry.getValue());

                        // TimePicker must be created on the main thread.
                        final int schedId = (Integer) entry.getValue();
                        final Context appCtx = ctx.getApplicationContext();
                        new Handler(Looper.getMainLooper()).post(() -> {
                            try {
                                Scheduler sched = Scheduler.fromPrefs(appCtx, schedId);
                                sched.setAlarm(appCtx);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to restore scheduler " + schedId, e);
                            }
                        });

                    }
                }

            } catch (Exception e) {
                try {
                    FileWriter debug_writer = new FileWriter(debug_log, true);
                    e.printStackTrace();
                    debug_writer.append(e.getMessage());
                    debug_writer.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        } else {
            try {
                FileWriter debug_writer = new FileWriter(debug_log, true);
                CharSequence message = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] "
                        + "ALARM FIRED sched_id=" + scheduler_id + " config_id=" + config_id_to_run;
                debug_writer.append(message);
                debug_writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (config_id_to_run > -1) {
                RS_Configuration config = myRsyncApplication.loadConfigById(ctx, config_id_to_run);
                if (config != null) {
                    config.executeConfig(ctx, scheduler_id > -1 ? scheduler_id : null);
                } else {
                    Log.e(TAG, "No config found for id=" + config_id_to_run);
                    try {
                        FileWriter debug_writer = new FileWriter(debug_log, true);
                        CharSequence message = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] "
                                + "WARNING: JOB FIRED BUT CONFIG NOT FOUND (config_id=" + config_id_to_run + ")";
                        debug_writer.append(message);
                        debug_writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Exact one-shot alarms: re-arm the next weekly occurrence(s).
                if (scheduler_id > -1) {
                    final int schedId = scheduler_id;
                    final Context appCtx = ctx.getApplicationContext();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            SharedPreferences sched_prefs =
                                    appCtx.getSharedPreferences("schedulers", MODE_PRIVATE);
                            if (sched_prefs.getInt("id_" + schedId, -1) != schedId) {
                                return;
                            }
                            Scheduler sched = Scheduler.fromPrefs(appCtx, schedId);
                            sched.setAlarm(appCtx);
                            // Overview uses in-memory Scheduler objects; clear their stale cache
                            // so Next Run jumps to the following occurrence instead of
                            // "About to start" for the slot that just fired.
                            Scheduler.refreshInMemoryAlarmCache(schedId);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to reschedule scheduler " + schedId, e);
                        }
                    });
                }

            } else {
                try {
                    FileWriter debug_writer = new FileWriter(debug_log, true);
                    CharSequence message = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + "WARNING: JOB FIRED BUT NO RSYNC CONFIGURATION EXECUTED (config_id=-1)";
                    debug_writer.append(message);
                    debug_writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
