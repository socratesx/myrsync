package com.linminitools.mysync;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.linminitools.mysync.MainActivity.appContext;
import static com.linminitools.mysync.MainActivity.configs;
import static com.linminitools.mysync.MainActivity.schedulers;

public class addScheduler extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_scheduler);

        TimePicker tp = findViewById(R.id.timePicker);
        tp.setIs24HourView(true);

        Spinner sp_configs= findViewById(R.id.sp_configs);
        List<String> listLoadToSpinner = new ArrayList<String>();

        for (RS_Configuration c : configs){
            listLoadToSpinner.add(c.name);
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                appContext,
                android.R.layout.simple_spinner_dropdown_item,
                listLoadToSpinner);

        sp_configs.setAdapter(spinnerAdapter);

    }

    public void saveScheduler(View v){

        TextView tv = findViewById(R.id.ed_name);
        TimePicker tp = findViewById(R.id.timePicker);
        tp.setIs24HourView(true);
        List<String> day_string = Arrays.asList("sunday","monday","tuesday","wednesday","thursday","friday","saturday");

        //java.util.Calendar cal = java.util.Calendar.getInstance();

        //cal.set(java.util.Calendar.HOUR,tp.getCurrentHour());
        //cal.set(java.util.Calendar.MINUTE,tp.getCurrentMinute());
        //long scheduler_time = cal.getTimeInMillis();

        String name = String.valueOf(tv.getText());
        Spinner sp = findViewById(R.id.sp_configs);
        int config_pos= sp.getSelectedItemPosition();


        //Intent futureIntent = new Intent("android.media.action.DISPLAY_NOTIFICATION");
        //futureIntent.addCategory("com.linminitools.mysync");
        //futureIntent.putExtra("Time",scheduler_time);
       // futureIntent.putExtra("config",config_pos);

        long interval= 604800000;        // 1 week in milliseconds

        //AlarmManager al = (AlarmManager) getSystemService(ALARM_SERVICE);

        String repeat2=".";

        for (String d : day_string){
            int resID = getResources().getIdentifier("tb_" + d, "id", getPackageName());
            ToggleButton tb = findViewById(resID);
            Log.d("BUTTONS ACTIVATED",String.valueOf(resID)+String.valueOf(tb.isChecked()));
            if (tb.isChecked()){
                repeat2=repeat2.concat(d+".");
                //PendingIntent broadcast= PendingIntent.getBroadcast(this,day_string.indexOf(d),futureIntent,PendingIntent.FLAG_UPDATE_CURRENT);
                //al.setRepeating(AlarmManager.RTC_WAKEUP,scheduler_time,interval,broadcast);
                //al.set(AlarmManager.RTC_WAKEUP,scheduler_time,broadcast);
            }
        }


        int id=schedulers.size()+1;

        Scheduler sched = new Scheduler(repeat2,tp,id);
        sched.name = name;
        sched.config_pos = config_pos;
        sched.saveToDisk();
        sched.setAlarm(appContext);
        schedulers.add(sched);

        this.finish();
    }
}
