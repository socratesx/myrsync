package com.linminitools.mysync;



import android.content.SharedPreferences;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.linminitools.mysync.MainActivity.appContext;

public class Scheduler {

    ArrayList<String> days;
    TimePicker d;
    String name;
    int id, hour, min, config_id;

    Scheduler (ArrayList<String> days, TimePicker d, int id){
        this.days=days;
        this.d=d;
        this.id=id;
        this.hour=d.getCurrentHour();
        this.min=d.getCurrentMinute();
    }


    protected void saveToDisk(){

        SharedPreferences prefs = appContext.getSharedPreferences("schedulers", MODE_PRIVATE);
        SharedPreferences.Editor prefseditor = prefs.edit();
        Set<String> set = new HashSet<String>(this.days);

        prefseditor.putInt("hour_"+String.valueOf(this.id),this.hour);
        prefseditor.putInt("min_"+String.valueOf(this.id),this.min);
        prefseditor.putStringSet("days_"+String.valueOf(this.id),set);
        prefseditor.putString("name_"+String.valueOf(this.id),this.name);
        prefseditor.apply();

    }

    protected void deleteFromDisk(){
        SharedPreferences prefs = appContext.getSharedPreferences("schedulers", MODE_PRIVATE);
        SharedPreferences.Editor prefseditor = prefs.edit();

        prefseditor.remove("hour_"+String.valueOf(this.id));
        prefseditor.remove("min_"+String.valueOf(this.id));
        prefseditor.remove("days_"+String.valueOf(this.id));
        prefseditor.remove("name_"+String.valueOf(this.id));
        prefseditor.apply();
    }


}
