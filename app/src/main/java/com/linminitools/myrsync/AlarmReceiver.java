package com.linminitools.myrsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.TimePicker;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;
import static com.linminitools.myrsync.MainActivity.configs;
import static com.linminitools.myrsync.MainActivity.schedulers;


public class AlarmReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context ctx, Intent i) {

        SharedPreferences sched_prefs = ctx.getSharedPreferences("schedulers", MODE_PRIVATE);
        SharedPreferences config_prefs = ctx.getSharedPreferences("configs", MODE_PRIVATE);
        //SharedPreferences settings_prefs = ctx.getSharedPreferences("settings",MODE_PRIVATE);
        Log.d("ALARM_MANAGER_MYSYNC","CALLED");
        //SharedPreferences set_prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        configs.clear();
        schedulers.clear();
        //settings.put("Notifications",settings_prefs.getBoolean("Notifications",true));

        for(int c=1; c<100;c++){
            if (config_prefs.getString("rs_ip_"+String.valueOf(c),"").isEmpty()){
                break;
            }
            else {
                RS_Configuration config = new RS_Configuration(c);
                config.rs_user=config_prefs.getString("rs_user_"+String.valueOf(c),"");
                config.rs_ip = config_prefs.getString("rs_ip_"+String.valueOf(c), "");
                config.rs_port = config_prefs.getString("rs_port_"+String.valueOf(c), "");
                config.rs_options = config_prefs.getString("rs_options_"+String.valueOf(c),"");
                config.rs_module = config_prefs.getString("rs_module_"+String.valueOf(c), "");
                config.local_path = config_prefs.getString("local_path_"+String.valueOf(c), "");
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
                int config_pos=sched_prefs.getInt("config_pos_"+id,0);
                Scheduler sched = new Scheduler(days,tp,(Integer) entry.getValue());
                sched.name=name;
                sched.config_pos=config_pos;
                schedulers.add(sched);
                Log.d("map_", id);
            }
        }
        Collections.sort(schedulers);


        if ((Objects.requireNonNull(i.getAction())).equals("android.intent.action.BOOT_COMPLETED" )) {
            for (Scheduler s : schedulers){
                s.setAlarm(ctx);
                }
        }

    }

}



