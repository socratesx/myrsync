package com.linminitools.myrsync;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.widget.TimePicker;

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

    @Override
    public void onCreate() {
        super.onCreate();

        appContext = getApplicationContext();

        // initialise configs and schedulers so they can be accessed by all activities independently
        configs.clear();
        schedulers.clear();
        populateArrays(appContext);
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

        Map<String,?> config_keys = config_prefs.getAll();
        for(Map.Entry<String,?> entry : config_keys.entrySet()){
            if (entry.getKey().contains("rs_id_")) {
                String id=String.valueOf(entry.getValue());

                RS_Configuration config = new RS_Configuration((Integer)entry.getValue());
                config.rs_user = config_prefs.getString("rs_user_"+id,"");
                config.rs_ip = config_prefs.getString("rs_ip_"+id, "");
                config.rs_port = config_prefs.getString("rs_port_"+id, "");
                config.rs_options = config_prefs.getString("rs_options_"+id,"");
                config.rs_module = config_prefs.getString("rs_module_"+id, "");
                config.local_path = config_prefs.getString("local_path_"+id, "");
                config.name = config_prefs.getString("rs_name_"+id, "");
                config.addedOn = config_prefs.getLong("rs_addedon_"+id,0);
                config.rs_mode = config_prefs.getString("rs_mode_"+id,"Push");
                configs.add(config);
            }
        }

        Map<String,?> sched_keys = sched_prefs.getAll();
        for(Map.Entry<String,?> entry : sched_keys.entrySet()){
            if (entry.getKey().startsWith("id_")) {
                String id=String.valueOf(entry.getValue());

                TimePicker tp=new TimePicker(this);

                tp.setIs24HourView(true);
                tp.setCurrentHour(sched_prefs.getInt("hour_"+id,0));
                tp.setCurrentMinute(sched_prefs.getInt("min_"+id,0));
                String days = sched_prefs.getString("days_"+id,"");
                String name = sched_prefs.getString("name_"+id,"");
                int config_id=sched_prefs.getInt("config_id_"+id,0);

                Scheduler sched = new Scheduler(days,tp,(Integer) entry.getValue());
                sched.name=name;
                sched.addedOn = sched_prefs.getLong("addedon",0);
                sched.config_id=config_id;
                schedulers.add(sched);
                //Log.d("SCHEDULER_NAME", sched.name);
            }
        }
        Collections.sort(schedulers);
        Collections.sort(configs);
    }
}
