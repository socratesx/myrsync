package com.linminitools.myrsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import static com.linminitools.myrsync.MainActivity.configs;
import static com.linminitools.myrsync.MainActivity.schedulers;


public class AlarmReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context ctx, Intent i) {
        String Debug_log_path= ctx.getApplicationInfo().dataDir + "/debug.log";
        File debug_log = new File(Debug_log_path);
        if ((Objects.requireNonNull(i.getAction())).equals("android.intent.action.BOOT_COMPLETED" )) {
            try {
                FileWriter debug_writer = new FileWriter(debug_log,true);
                Locale current_locale = ctx.getResources().getConfiguration().locale;
                SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);
                CharSequence message= "\n\n[ "+ formatter.format(Calendar.getInstance().getTime()) +" ] "+"--------DEVICE REBOOTED--------";
                debug_writer.append(message);
                debug_writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            }

        /*
        SharedPreferences sched_prefs = ctx.getSharedPreferences("schedulers", MODE_PRIVATE);
        SharedPreferences config_prefs = ctx.getSharedPreferences("configs", MODE_PRIVATE);

        configs.clear();
        schedulers.clear();

        Map<String,?> config_keys = config_prefs.getAll();
        for(Map.Entry<String,?> entry : config_keys.entrySet()){
            if (entry.getKey().contains("rs_id_")) {
                String id=String.valueOf(entry.getValue());

                RS_Configuration config = new RS_Configuration((Integer)entry.getValue());
                config.rs_user=config_prefs.getString("rs_user_"+id,"");
                config.rs_ip = config_prefs.getString("rs_ip_"+id, "");
                config.rs_port = config_prefs.getString("rs_port_"+id, "");
                config.rs_options = config_prefs.getString("rs_options_"+id,"");
                config.rs_module = config_prefs.getString("rs_module_"+id, "");
                config.local_path = config_prefs.getString("local_path_"+id, "");
                config.name = config_prefs.getString("rs_name_"+id, "");
                config.addedOn = config_prefs.getLong("rs_addedon_"+id,0);
                configs.add(config);
            }
        }


        Map<String,?> sched_keys = sched_prefs.getAll();

        for(Map.Entry<String,?> entry : sched_keys.entrySet()){
            if (entry.getKey().contains("id_")) {
                String id=String.valueOf(entry.getValue());

                TimePicker tp=new TimePicker(ctx);
                tp.setIs24HourView(true);

                tp.setCurrentHour(sched_prefs.getInt("hour_"+id,0));
                tp.setCurrentMinute(sched_prefs.getInt("min_"+id,0));
                String days = sched_prefs.getString("days_"+id,"");
                String name = sched_prefs.getString("name_"+id,"");
                int config_id=sched_prefs.getInt("config_id_"+id,0);
                Scheduler sched = new Scheduler(days,tp,(Integer) entry.getValue());
                sched.name=name;
                sched.config_id=config_id;
                schedulers.add(sched);
            }
        }



        if ((Objects.requireNonNull(i.getAction())).equals("android.intent.action.BOOT_COMPLETED" )) {
            for (Scheduler s : schedulers){
                s.setAlarm(ctx);
                }
        }
        */
    }

}



