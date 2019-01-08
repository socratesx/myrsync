package com.linminitools.myrsync;

import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;
import com.linminitools.myrsync.PluginBundleValues;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

public final class TaskerPluginReceiver extends AbstractPluginSettingReceiver {

    @Override
    protected boolean isBundleValid(@NonNull final Bundle bundle) {
        return PluginBundleValues.isBundleValid(bundle);
    }

    @Override
    protected boolean isAsync() {
        return true;
    }

    @Override
    protected void firePluginSetting(@NonNull final Context context, @NonNull final Bundle bundle) {
        int configId = PluginBundleValues.getConfigId(bundle);

        String Debug_log_path = context.getApplicationInfo().dataDir + "/debug.log";
        File debug_log = new File(Debug_log_path);
        Locale current_locale = context.getResources().getConfiguration().locale;
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);
        FileWriter debug_writer = null;

        try {
            boolean configFound = false;
            SharedPreferences config_prefs = context.getSharedPreferences("configs", MODE_PRIVATE);
            Map<String, ?> config_keys = config_prefs.getAll();

            for (Map.Entry<String, ?> entry : config_keys.entrySet()) {
                if (entry.getKey().contains("rs_id_")) {
                    String id = String.valueOf(entry.getValue());

                    if (Integer.parseInt(id) == configId) {
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

                        debug_writer = new FileWriter(debug_log, true);

                        CharSequence message = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] CONFIGURATION RUN " + config.name + " TRIGGERED FROM TASKER PLUGIN";
                        debug_writer.append(message);
                        debug_writer.close();

                        config.executeConfig(context, null);
                        break;
                    }
                }
            }

            if (!configFound) {
                debug_writer = new FileWriter(debug_log, true);

                CharSequence message = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] CONFIGURATION RUN TRIGGERED FROM TASKER PLUGIN BUT NO CONFIG FOUND WITH ID " + configId;
                debug_writer.append(message);
                debug_writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}