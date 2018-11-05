package com.linminitools.myrsync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.linminitools.myrsync.MainActivity.appContext;
import static com.linminitools.myrsync.MainActivity.configs;
import static com.linminitools.myrsync.MainActivity.schedulers;


public class editSched extends addScheduler {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_scheduler);

        Intent i = getIntent();
        final int pos = i.getIntExtra("pos", 0);

        Scheduler sched = schedulers.get(pos);
        EditText ed_name = findViewById(R.id.ed_name);
        ed_name.setText(sched.name);

        TimePicker tp = findViewById(R.id.timePicker);

        tp.setIs24HourView(true);

        tp.setCurrentHour(sched.hour);
        tp.setCurrentMinute(sched.min);

        int config_id = sched.config_id;

        Spinner sp = findViewById(R.id.sp_configs);

        List<String> listLoadToSpinner = new ArrayList<>();

        int selected_config=0;

        for (RS_Configuration c : configs) {
            listLoadToSpinner.add(c.name);
            if (c.id==config_id) selected_config=listLoadToSpinner.indexOf(c.name);
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(appContext, android.R.layout.simple_spinner_dropdown_item, listLoadToSpinner);


        sp.setAdapter(spinnerAdapter);
        sp.setSelection(selected_config);





        String days = sched.days;
        String[] active_days = days.split("[.]");

        List<String> day_string = Arrays.asList("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");


        for (String d : day_string) {
            int resID = getResources().getIdentifier("tb_" + d, "id", getPackageName());
            ToggleButton tb = findViewById(resID);
            tb.setSelected(true);
            tb.toggle();
        }

        for (String a : active_days) {
            int rid = getResources().getIdentifier("tb_" + a, "id", getPackageName());
            if (rid != 0) {
                ToggleButton bt = findViewById(rid);
                bt.setSelected(true);
                bt.toggle();

            }
        }

        Button bt_save = findViewById(R.id.bt_save_sched);
        bt_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAction(pos);
            }
        });
    }






        private void saveAction(int pos) {

            TextView tv = findViewById(R.id.ed_name);
            TimePicker tp = findViewById(R.id.timePicker);

            List<String> day_string = Arrays.asList("monday","tuesday","wednesday","thursday","friday","saturday","sunday");


            String repeat2=".";

            for (String d : day_string){
                int resID = getResources().getIdentifier("tb_" + d, "id", getPackageName());
                ToggleButton tb = findViewById(resID);

                if (tb.isChecked()){
                    repeat2=repeat2.concat(d+".");

                }
            }

            String name = String.valueOf(tv.getText());
            Spinner sp = findViewById(R.id.sp_configs);

            List<String> listLoadToSpinner = new ArrayList<>();

            for (RS_Configuration c : configs){
                listLoadToSpinner.add(c.name);
            }

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(appContext, android.R.layout.simple_spinner_dropdown_item, listLoadToSpinner);


            String selected_config_name= (String)sp.getSelectedItem();
            int config_id=0;
            for (RS_Configuration c : configs) if((c.name).equals(selected_config_name)) config_id = c.id;

            sp.setAdapter(spinnerAdapter);

            Scheduler sched = schedulers.get(pos);
            sched.cancelAlarm(appContext);
            sched.days=repeat2;
            sched.name = name;
            sched.config_id = config_id;
            sched.d=tp;
            sched.update();
            sched.saveToDisk();
            sched.setAlarm(appContext);

            this.finish();

        }

}
