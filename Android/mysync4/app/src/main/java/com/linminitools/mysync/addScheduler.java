package com.linminitools.mysync;

import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.linminitools.mysync.MainActivity.appContext;
import static com.linminitools.mysync.MainActivity.configs;
import static com.linminitools.mysync.MainActivity.schedulers;

public class addScheduler extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_scheduler);

        Spinner sp_configs= (Spinner) findViewById(R.id.sp_configs);
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
        List<String> day_string = Arrays.asList("monday","tuesday","wednesday","thursday","friday","saturday","sunday");

        ArrayList<String> repeat = new ArrayList<>();

        for (String d : day_string){
            int resID = getResources().getIdentifier("tb_" + String.valueOf(d), "id", getPackageName());
            ToggleButton tb = findViewById(resID);
            if (tb.isActivated()){
                repeat.add(d);
            }

        }

        String name = String.valueOf(tv.getText());
        Spinner sp = findViewById(R.id.sp_configs);
        int config_id= configs.get(sp.getSelectedItemPosition()).id;

        int id=schedulers.size()+1;

        Scheduler sched = new Scheduler(repeat,tp,id);
        sched.name = name;
        sched.config_id = config_id;
        sched.saveToDisk();
        schedulers.add(sched);

        this.finish();
    }
}
