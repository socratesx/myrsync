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
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

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

        final EditText ed_ssid = findViewById(R.id.ed_wifi_ssid);
        final ImageButton get_ssid = findViewById(R.id.ib_get_ssid);
        get_ssid.setEnabled(ed_ssid.isEnabled());

        Switch sw_wifi = findViewById(R.id.sw_wifi_switch);
        sw_wifi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ed_ssid.setEnabled(isChecked);
                get_ssid.setEnabled(isChecked);
            }
        });

        get_ssid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ssid=get_current_ssid(appContext);
                ed_ssid.setText(ssid);
            }
        });


    }

    protected String get_current_ssid(Context context){

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

    public void saveScheduler(View v){

        TextView tv = findViewById(R.id.ed_name);
        TimePicker tp = findViewById(R.id.timePicker);
        tp.setIs24HourView(true);
        List<String> day_string = Arrays.asList("sunday","monday","tuesday","wednesday","thursday","friday","saturday");

        EditText wifi_ssid = findViewById(R.id.ed_wifi_ssid);
        Switch wifi_sw = findViewById(R.id.sw_wifi_switch);

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

            if (repeat2.equals(".")) {
                String Message = "This scheduler has no repeat days and will not be saved. Please select at least one day otherwise there is no need for a scheduler. ";
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(appContext, Message, duration);
                toast.show();
            }
            else {
                Scheduler sched = new Scheduler(repeat2, tp, id);
                sched.name = name;
                sched.config_id = config_id;
                sched.wifi_sw = wifi_sw.isChecked();
                sched.wifi_ssid = String.valueOf(wifi_ssid.getText());
                sched.saveToDisk();
                sched.setAlarm(appContext);
                schedulers.add(sched);

                this.finish();
            }
        }
        else{
            String Message = "This scheduler has no configuration to attach to and it will not be saved. Create at least one configuration before creating a scheduler. ";
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
