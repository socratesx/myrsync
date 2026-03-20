package com.linminitools.myrsync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.linminitools.myrsync.MainActivity.appContext;
import static com.linminitools.myrsync.myRsyncApplication.configs;
import static com.linminitools.myrsync.myRsyncApplication.schedulers;

public class editSched extends addScheduler {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_scheduler);

        MaterialToolbar toolbar = findViewById(R.id.scheduler_toolbar);
        toolbar.setTitle("Edit Scheduler Settings");
        toolbar.setNavigationOnClickListener(v -> finish());

        Intent i = getIntent();
        final int pos = i.getIntExtra("pos", 0);

        Scheduler sched = schedulers.get(pos);
        TextInputEditText ed_name = findViewById(R.id.ed_name);
        ed_name.setText(sched.name);

        TimePicker tp = findViewById(R.id.timePicker);
        tp.setIs24HourView(true);
        tp.setHour(sched.hour);
        tp.setMinute(sched.min);

        int config_id = sched.config_id;
        Spinner sp = findViewById(R.id.sp_configs);

        List<String> listLoadToSpinner = new ArrayList<>();
        int selected_config = 0;

        for (RS_Configuration c : configs) {
            listLoadToSpinner.add(c.name);
            if (c.id == config_id) selected_config = listLoadToSpinner.indexOf(c.name);
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, listLoadToSpinner);

        sp.setAdapter(spinnerAdapter);
        sp.setSelection(selected_config);

        MaterialButtonToggleGroup toggleDays = findViewById(R.id.toggle_days);
        int[] dayBtnIds = {R.id.tb_monday, R.id.tb_tuesday, R.id.tb_wednesday,
                R.id.tb_thursday, R.id.tb_friday, R.id.tb_saturday, R.id.tb_sunday};

        for (int dayId : dayBtnIds) {
            toggleDays.uncheck(dayId);
        }

        String days = sched.days;
        String[] active_days = days.split("[.]");
        for (String a : active_days) {
            int rid = getResources().getIdentifier("tb_" + a, "id", getPackageName());
            if (rid != 0) {
                toggleDays.check(rid);
            }
        }

        com.google.android.material.button.MaterialButton bt_save = findViewById(R.id.bt_save_sched);
        bt_save.setOnClickListener(v -> saveAction(pos));

        MaterialSwitch sw_wifi = findViewById(R.id.sw_wifi_switch);
        sw_wifi.setChecked(sched.wifi_sw);
    }

    private void saveAction(int pos) {
        TextInputEditText tv = findViewById(R.id.ed_name);
        TimePicker tp = findViewById(R.id.timePicker);

        List<String> day_string = Arrays.asList("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");

        String repeat2 = ".";

        int[] dayBtnIds = {R.id.tb_monday, R.id.tb_tuesday, R.id.tb_wednesday,
                R.id.tb_thursday, R.id.tb_friday, R.id.tb_saturday, R.id.tb_sunday};

        for (int j = 0; j < day_string.size(); j++) {
            MaterialButton btn = findViewById(dayBtnIds[j]);
            if (btn.isChecked()) {
                repeat2 = repeat2.concat(day_string.get(j) + ".");
            }
        }

        String name = String.valueOf(tv.getText());
        Spinner sp = findViewById(R.id.sp_configs);

        MaterialSwitch sw_wifi = findViewById(R.id.sw_wifi_switch);
        boolean wifi_sw = sw_wifi.isChecked();

        String selected_config_name = (String) sp.getSelectedItem();
        int config_id = 0;
        for (RS_Configuration c : configs) if ((c.name).equals(selected_config_name)) config_id = c.id;

        if (repeat2.equals(".")) {
            Toast.makeText(appContext,
                    "This scheduler has no repeat days and will not be saved. Please select at least one day.",
                    Toast.LENGTH_LONG).show();
        } else {
            Scheduler sched = schedulers.get(pos);
            sched.cancelAlarm(appContext);
            sched.days = repeat2;
            sched.name = name;
            sched.config_id = config_id;
            sched.d = tp;
            sched.wifi_sw = wifi_sw;
            sched.update();
            sched.saveToDisk();
            sched.setAlarm(appContext);
            this.finish();
        }
    }
}
