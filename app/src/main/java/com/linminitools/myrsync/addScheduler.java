package com.linminitools.myrsync;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.linminitools.myrsync.MainActivity.appContext;
import static com.linminitools.myrsync.myRsyncApplication.configs;
import static com.linminitools.myrsync.myRsyncApplication.schedulers;

public class addScheduler extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_scheduler);

        MaterialToolbar toolbar = findViewById(R.id.scheduler_toolbar);
        toolbar.setTitle(getTitle());
        toolbar.setNavigationOnClickListener(v -> finish());

        TimePicker tp = findViewById(R.id.timePicker);
        tp.setIs24HourView(true);

        Spinner sp_configs = findViewById(R.id.sp_configs);
        List<String> listLoadToSpinner = new ArrayList<>();

        for (RS_Configuration c : configs) {
            listLoadToSpinner.add(c.name);
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                listLoadToSpinner);

        sp_configs.setAdapter(spinnerAdapter);

        MaterialButtonToggleGroup toggleDays = findViewById(R.id.toggle_days);
        int[] dayBtnIds = {R.id.tb_monday, R.id.tb_tuesday, R.id.tb_wednesday,
                R.id.tb_thursday, R.id.tb_friday, R.id.tb_saturday, R.id.tb_sunday};
        for (int id : dayBtnIds) {
            toggleDays.check(id);
        }

        MaterialSwitch sw_wifi = findViewById(R.id.sw_wifi_switch);
        sw_wifi.setOnCheckedChangeListener((buttonView, isChecked) -> {});
    }

    protected String get_current_ssid(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) {
            Toast.makeText(context, "You are not connected!", Toast.LENGTH_SHORT).show();
            return "";
        }

        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
        if (caps == null || !caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
            Toast.makeText(context, "You are not connected to a WiFi Network!", Toast.LENGTH_SHORT).show();
            return "";
        }

        String ssid = "";
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        if (connectionInfo != null && !android.text.TextUtils.isEmpty(connectionInfo.getSSID())
                && !connectionInfo.getSSID().equals("<unknown ssid>")) {
            ssid = connectionInfo.getSSID();
        } else {
            Toast.makeText(context, "Could not determine WiFi SSID.", Toast.LENGTH_SHORT).show();
            return "";
        }

        return ssid.replaceAll("^\"|\"$", "");
    }

    public void saveScheduler(View v) {
        TextInputEditText tv = findViewById(R.id.ed_name);
        TimePicker tp = findViewById(R.id.timePicker);
        tp.setIs24HourView(true);
        List<String> day_string = Arrays.asList("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday");

        MaterialSwitch wifi_sw = findViewById(R.id.sw_wifi_switch);

        String name = String.valueOf(tv.getText());
        Spinner sp = findViewById(R.id.sp_configs);
        int config_id = -1;
        String config_name = (String) sp.getSelectedItem();
        for (RS_Configuration c : configs) if ((c.name).equals(config_name)) config_id = c.id;

        if (config_id > -1) {
            String repeat2 = ".";

            int[] dayBtnIds = {R.id.tb_sunday, R.id.tb_monday, R.id.tb_tuesday,
                    R.id.tb_wednesday, R.id.tb_thursday, R.id.tb_friday, R.id.tb_saturday};

            for (int i = 0; i < day_string.size(); i++) {
                MaterialButton btn = findViewById(dayBtnIds[i]);
                if (btn.isChecked()) {
                    repeat2 = repeat2.concat(day_string.get(i) + ".");
                }
            }

            int id = 1;
            SharedPreferences sched_prefs = getSharedPreferences("schedulers", MODE_PRIVATE);

            while (sched_prefs.getInt("id_" + String.valueOf(id), -1) > 0) {
                id = id + 1;
            }

            if (repeat2.equals(".")) {
                Toast.makeText(appContext,
                        "This scheduler has no repeat days and will not be saved. Please select at least one day.",
                        Toast.LENGTH_LONG).show();
            } else {
                Scheduler sched = new Scheduler(repeat2, tp, id);
                sched.name = name;
                sched.config_id = config_id;
                sched.wifi_sw = wifi_sw.isChecked();
                sched.saveToDisk();
                sched.setAlarm(appContext);
                schedulers.add(sched);
                this.finish();
            }
        } else {
            Toast.makeText(appContext,
                    "This scheduler has no configuration to attach to. Create at least one configuration first.",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void scheduler_info(View v) {
        new MaterialAlertDialogBuilder(v.getContext())
                .setTitle("Scheduler Info")
                .setMessage(getResources().getString(R.string.scheduler_info))
                .setPositiveButton("Close", (dialog, which) -> {})
                .show();
    }
}
