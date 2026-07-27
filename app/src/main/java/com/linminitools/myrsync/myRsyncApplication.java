package com.linminitools.myrsync;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.NonNull;
import android.widget.TimePicker;
import androidx.annotation.Nullable;
import android.preference.PreferenceManager;
import com.linminitools.myrsync.RS_Configuration;
import com.linminitools.myrsync.Scheduler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class myRsyncApplication extends Application {
    @NonNull
    public static final ArrayList<RS_Configuration> configs = new ArrayList<>();
    @NonNull
    public static final ArrayList<Scheduler> schedulers = new ArrayList<>();
    public static Context appContext;

    private BillingManager billingManager;

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        ThemeModeHelper.migrateLegacyThemePreference(defaultPrefs);
        ThemeModeHelper.applyFromPreferenceString(
                defaultPrefs.getString("theme_mode", "follow_system"));

        appContext = getApplicationContext();

        // Move any plaintext SSH passwords out of configs.xml before loading configs.
        SshSecretStore.migrateAllLegacy(appContext);

        // initialise configs and schedulers so they can be accessed by all activities independently
        configs.clear();
        schedulers.clear();
        populateArrays(appContext);

        ProEntitlement.migrateIfNeeded(this);
        billingManager = new BillingManager(this);
        billingManager.start();
    }

    @NonNull
    public BillingManager getBillingManager() {
        if (billingManager == null) {
            billingManager = new BillingManager(this);
            billingManager.start();
        }
        return billingManager;
    }

    /** Reload in-memory configs from disk (e.g. Tasker plugin editor after app was already running). */
    public static void reloadConfigsFromDisk(@NonNull Context ctx) {
        SshSecretStore.migrateAllLegacy(ctx);
        configs.clear();
        loadConfigsFromDisk(ctx, configs);
        Collections.sort(configs);
    }

    /** Load configs into a caller-owned list without touching global in-memory state. */
    public static void loadConfigsInto(@NonNull Context ctx,
                                       @NonNull ArrayList<RS_Configuration> target) {
        target.clear();
        loadConfigsFromDisk(ctx, target);
        Collections.sort(target);
    }

    /** Load one configuration by id, or null if it does not exist on disk. */
    @Nullable
    public static RS_Configuration loadConfigById(@NonNull Context ctx, int configId) {
        SharedPreferences config_prefs = ctx.getSharedPreferences("configs", MODE_PRIVATE);
        String id = String.valueOf(configId);
        if (config_prefs.getInt("rs_id_" + id, -1) != configId) {
            return null;
        }
        RS_Configuration config = new RS_Configuration(configId);
        config.rs_user = config_prefs.getString("rs_user_" + id, "");
        config.rs_ip = config_prefs.getString("rs_ip_" + id, "");
        config.rs_port = config_prefs.getString("rs_port_" + id, "");
        config.rs_options = config_prefs.getString("rs_options_" + id, "");
        config.rs_module = config_prefs.getString("rs_module_" + id, "");
        config.local_path = config_prefs.getString("local_path_" + id, "");
        config.name = config_prefs.getString("rs_name_" + id, "");
        config.addedOn = config_prefs.getLong("rs_addedon_" + id, 0);
        config.rs_mode = config_prefs.getString("rs_mode_" + id, "Push");
        config.adv_options = config_prefs.getString("rs_adv_options_" + id, "");
        config.rs_transport_type = config_prefs.getString("rs_transport_type_" + id, "Rsync Daemon");
        config.rs_ssh_private_key_path = config_prefs.getString("rs_ssh_private_key_path_" + id, "");
        config.rs_ssh_remote_path = config_prefs.getString("rs_ssh_remote_path_" + id, "");
        config.rs_ssh_auth_type = config_prefs.getString("rs_ssh_auth_type_" + id, RS_Configuration.SSH_AUTH_KEY);
        config.rs_ssh_password = SshSecretStore.getPassword(ctx, configId);
        return config;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void populateArrays(Context ctx){

        SharedPreferences config_prefs = ctx.getSharedPreferences("configs", MODE_PRIVATE);
        SharedPreferences sched_prefs = ctx.getSharedPreferences("schedulers", MODE_PRIVATE);
        SharedPreferences prefs = ctx.getSharedPreferences("Rsync_Command_build", MODE_PRIVATE);
        String log_path = prefs.getString("log", ctx.getApplicationInfo().dataDir + "/logfile.log");

        File log_file = new File(log_path);
        try {
            log_file.createNewFile();
        }
        catch (IOException e){
            e.printStackTrace();
        }

        loadConfigsFromDisk(ctx, configs);

        Map<String,?> sched_keys = sched_prefs.getAll();
        for(Map.Entry<String,?> entry : sched_keys.entrySet()){
            if (entry.getKey().startsWith("id_")) {
                String id=String.valueOf(entry.getValue());

                TimePicker tp=new TimePicker(this);

                tp.setIs24HourView(true);
                tp.setHour(sched_prefs.getInt("hour_"+id,0));
                tp.setMinute(sched_prefs.getInt("min_"+id,0));
                String days = sched_prefs.getString("days_"+id,"");
                String name = sched_prefs.getString("name_"+id,"");
                int config_id=sched_prefs.getInt("config_id_"+id,0);

                Scheduler sched = new Scheduler(days,tp,(Integer) entry.getValue());
                sched.name=name;
                sched.addedOn = sched_prefs.getLong("addedon",0);
                sched.config_id=config_id;
                sched.wifi_sw = sched_prefs.getBoolean("wifi_switch_"+id,false);
                sched.wifi_ssid = sched_prefs.getString("ssid_"+id,"");
                schedulers.add(sched);
                //Log.d("SCHEDULER_NAME", sched.name);
            }
        }
        Collections.sort(schedulers);
        Collections.sort(configs);
    }

    private static void loadConfigsFromDisk(@NonNull Context ctx, @NonNull ArrayList<RS_Configuration> target) {
        SharedPreferences config_prefs = ctx.getSharedPreferences("configs", MODE_PRIVATE);
        Map<String, ?> config_keys = config_prefs.getAll();
        for (Map.Entry<String, ?> entry : config_keys.entrySet()) {
            if (!entry.getKey().contains("rs_id_")) {
                continue;
            }
            Object rawId = entry.getValue();
            if (!(rawId instanceof Integer)) {
                continue;
            }
            int configId = (Integer) rawId;
            RS_Configuration config = loadConfigById(ctx, configId);
            if (config != null) {
                target.add(config);
            }
        }
    }
}
