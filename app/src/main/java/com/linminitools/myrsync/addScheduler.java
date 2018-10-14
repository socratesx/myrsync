package com.linminitools.myrsync;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.linminitools.myrsync.MainActivity.appContext;
import static com.linminitools.myrsync.MainActivity.configs;
import static com.linminitools.myrsync.MainActivity.schedulers;

public class addScheduler extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_scheduler);

        TimePicker tp = findViewById(R.id.timePicker);
        tp.setIs24HourView(true);

        Spinner sp_configs= findViewById(R.id.sp_configs);
        List<String> listLoadToSpinner = new ArrayList<>();

        for (RS_Configuration c : configs){
            listLoadToSpinner.add(c.name);
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
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

        String name = String.valueOf(tv.getText());
        Spinner sp = findViewById(R.id.sp_configs);
        int config_id=-1;
        String config_name=(String)sp.getSelectedItem();
        for (RS_Configuration c : configs) if((c.name).equals(config_name)) config_id = c.id;


        if (config_id>-1) {
            // 604800000;        1 week in milliseconds

            String repeat2 = ".";

            for (String d : day_string) {
                int resID = getResources().getIdentifier("tb_" + d, "id", getPackageName());
                ToggleButton tb = findViewById(resID);
                if (tb.isChecked()) {
                    repeat2 = repeat2.concat(d + ".");
                }
            }

            int id = 1;

            SharedPreferences sched_prefs = getSharedPreferences("schedulers",MODE_PRIVATE);

            while (sched_prefs.getInt("id_"+String.valueOf(id),-1)>0){
                id=id+1;
            }

            //Log.d("ID=",String.valueOf(id));

            Scheduler sched = new Scheduler(repeat2, tp, id);
            sched.name = name;
            sched.config_id = config_id;
            sched.saveToDisk();
            sched.setAlarm(appContext);
            schedulers.add(sched);

            this.finish();
        }
        else{
            String Message = "This scheduler has no configuration to attach and it will not be saved. Create at least one configuration before creating a scheduler. ";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(appContext, Message, duration);
            toast.show();
        }
    }

    public void scheduler_info(View v){
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Rsync Options Help")
                        .setMessage(getResources().getString(R.string.scheduler_info))
                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                ;
        alertDialogBuilder.show();
    }
}
