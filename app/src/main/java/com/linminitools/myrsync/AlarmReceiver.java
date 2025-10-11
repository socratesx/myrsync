package com.linminitools.myrsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.TimePicker;

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

        Log.i(TAG, "Received broadcast action=" + i.getAction() + " configId=" + config_id_to_run + " schedulerId=" + scheduler_id);

        String Debug_log_path = ctx.getApplicationInfo().dataDir + "/debug.log";
        File debug_log = new File(Debug_log_path);
        Locale current_locale = ctx.getResources().getConfiguration().locale;
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);


        if ((Objects.requireNonNull(i.getAction())).equals("android.intent.action.BOOT_COMPLETED")) {
            Log.i(TAG, "Handling boot completed event");
            try {
                appendDebugLog(debug_log, formatter, "--------DEVICE REBOOTED--------");

                SharedPreferences sched_prefs = ctx.getSharedPreferences("schedulers", MODE_PRIVATE);

                Map<String, ?> sched_keys = sched_prefs.getAll();
                for (Map.Entry<String, ?> entry : sched_keys.entrySet()) {
                    if (entry.getKey().startsWith("id_")) {
                        String id = String.valueOf(entry.getValue());

                        TimePicker tp = new TimePicker(ctx);

                        tp.setIs24HourView(true);
                        tp.setCurrentHour(sched_prefs.getInt("hour_" + id, 0));
                        tp.setCurrentMinute(sched_prefs.getInt("min_" + id, 0));
                        String days = sched_prefs.getString("days_" + id, "");
                        String name = sched_prefs.getString("name_" + id, "");
                        int config_id = sched_prefs.getInt("config_id_" + id, 0);

                        Scheduler sched = new Scheduler(days, tp, (Integer) entry.getValue());
                        sched.name = name;
                        sched.addedOn = sched_prefs.getLong("addedon", 0);
                        sched.config_id = config_id;
                        sched.setAlarm(ctx);

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
            if (config_id_to_run > -1) {

                SharedPreferences config_prefs = ctx.getSharedPreferences("configs", MODE_PRIVATE);
                Map<String, ?> config_keys = config_prefs.getAll();
                boolean configFound = false;

                for (Map.Entry<String, ?> entry : config_keys.entrySet()) {
                    if (entry.getKey().contains("rs_id_")) {
                        String id = String.valueOf(entry.getValue());

                        if (Integer.parseInt(id) == config_id_to_run) {
                            configFound = true;
                            RS_Configuration config = new RS_Configuration((Integer) entry.getValue());
                            config.rs_user = config_prefs.getString("rs_user_" + id, "");
                            config.rs_ip = config_prefs.getString("rs_ip_" + id, "");
                            config.rs_port = config_prefs.getString("rs_port_" + id, "");
                            config.rs_options = config_prefs.getString("rs_options_" + id, "");
                            config.rs_module = config_prefs.getString("rs_module_" + id, "");
                            config.local_path = config_prefs.getString("local_path_" + id, "");
                            config.name = config_prefs.getString("rs_name_" + id, "");
                            config.rs_mode = config_prefs.getString("rs_mode_" + id, "push");
                            config.addedOn = config_prefs.getLong("rs_addedon_" + id, 0);
                            String startMessage = "CONFIGURATION RUN " + config.name + " TRIGGERED FROM SCHEDULER " + scheduler_id;
                            appendDebugLog(debug_log, formatter, startMessage);
                            try {
                                Log.i(TAG, "Executing configuration '" + config.name + "' (id=" + config.id + ") from scheduler " + scheduler_id);
                                config.executeConfig(ctx, scheduler_id);
                            } catch (Exception executionError) {
                                String errorMessage = executionError.getMessage() == null ? executionError.toString() : executionError.getMessage();
                                Log.e(TAG, "Failed to execute configuration '" + config.name + "'", executionError);
                                appendDebugLog(debug_log, formatter, "CONFIGURATION RUN " + config.name + " FAILED TO START FROM ALARM: " + errorMessage);
                            }
                            break;
                        }

                    }
                }

                if (!configFound) {
                    String message = "WARNING: JOB FIRED BUT NO RSYNC CONFIGURATION FOUND (config_id=" + config_id_to_run + ")";
                    Log.w(TAG, message);
                    appendDebugLog(debug_log, formatter, message);
                }

            }
            else{
                String warning = "WARNING: JOB FIRED BUT NO RSYNC CONFIGURATION EXECUTED (config_id=-1)";
                Log.w(TAG, warning);
                appendDebugLog(debug_log, formatter, warning);
            }
        }

    }

    private void appendDebugLog(File debugLog, SimpleDateFormat formatter, String message) {
        try {
            FileWriter debug_writer = new FileWriter(debugLog, true);
            CharSequence logMessage = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + message;
            debug_writer.append(logMessage);
            debug_writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to write to debug log", e);
        }
    }
}




