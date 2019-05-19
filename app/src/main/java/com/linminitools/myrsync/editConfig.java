package com.linminitools.myrsync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Map;
import java.util.Objects;

import static android.os.Environment.DIRECTORY_ALARMS;
import static android.os.Environment.DIRECTORY_DCIM;
import static android.os.Environment.DIRECTORY_DOCUMENTS;
import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.DIRECTORY_MOVIES;
import static android.os.Environment.DIRECTORY_MUSIC;
import static android.os.Environment.DIRECTORY_NOTIFICATIONS;
import static android.os.Environment.DIRECTORY_PICTURES;
import static android.os.Environment.DIRECTORY_PODCASTS;
import static android.os.Environment.DIRECTORY_RINGTONES;
import static com.linminitools.myrsync.MainActivity.appContext;
import static com.linminitools.myrsync.myRsyncApplication.configs;

public class editConfig extends addConfig {


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_config);

        Button save = findViewById(R.id.bt_save);
        Button view = findViewById(R.id.bt_view);
        Button bt_add = findViewById(R.id.bt_add);
        Button bt_exec = findViewById(R.id.bt_execute);

        ImageButton info = findViewById(R.id.ib_info_daemon);
        info.setImageResource(R.drawable.ic_info_black_24dp);

        save.setEnabled(true);
        view.setEnabled(true);
        bt_add.setText(R.string.change_path);
        bt_exec.setEnabled(true);

        Intent i = getIntent();
        final int p = i.getIntExtra("pos", 0);
        RS_Configuration config = configs.get(p);

        EditText ed_rs_name = findViewById(R.id.et_rsync_alias);
        ed_rs_name.setText(config.name);

        EditText ed_srv_ip = findViewById(R.id.ed_srv_ip);
        ed_srv_ip.setText(config.rs_ip);

        EditText ed_rsync_user = findViewById(R.id.ed_rsync_user);
        ed_rsync_user.setText(config.rs_user);

        EditText ed_srv_port = findViewById(R.id.ed_srv_port);
        ed_srv_port.setText(config.rs_port);

        EditText ed_rsync_mod = findViewById(R.id.ed_rsync_mod);
        ed_rsync_mod.setText(config.rs_module);

        RadioGroup rg_mode = findViewById(R.id.rg_mode);
        RadioButton rb_pull = findViewById(R.id.rb_pull);
        RadioButton rb_push = findViewById(R.id.rb_push);



        if (rb_pull.getText().equals(config.rs_mode)) rb_pull.setChecked(true);

        else rb_push.setChecked(true);

        TextView tv_path = findViewById(R.id.tv_path);
        tv_path.setVisibility(View.VISIBLE);

        String local_path = getSharedPreferences("configs", MODE_PRIVATE).getString("local_path_"+String.valueOf(config.id),"");
        tv_path.setText(local_path);

        String rs_options = config.rs_options;

        if (Objects.equals(rs_options, "-")) {
            rs_options = "";
        }

        if (!rs_options.isEmpty()) {
            String options = (rs_options.substring(1)).split(" ")[0].trim();
            Log.d("OPTIONS:",options);
            for (char x : options.toCharArray()) {
                Log.d("OPTIONS:",String.valueOf(x));
                int resID = getResources().getIdentifier("cb_" + String.valueOf(x), "id", getPackageName());
                if (resID!=0) {
                    CheckBox cb = findViewById(resID);

                    cb.setChecked(true);
                }
            }
        }
        EditText ed_adv_options = findViewById(R.id.ed_advanced_options);
        ed_adv_options.setText(config.adv_options);



        final int id = config.id;

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionConfig(id, 1);
            }
        });

        bt_exec.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                actionConfig(id, 2);
            }
        });

        rg_mode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                group.check(checkedId);
            }
        });

    }

    private void actionConfig(int id, int request){
        Map<String,String> configMap=processForm();

        if (!configMap.get("rs_ip").isEmpty() &&
                !configMap.get("rs_module").isEmpty() && !configMap.get("local_path").isEmpty()) {

            for (RS_Configuration c : configs){

                if (c.id==id){
                    c.rs_ip = configMap.get("rs_ip");
                    c.rs_user = configMap.get("rs_user");
                    c.rs_port = configMap.get("rs_port");
                    c.rs_options = configMap.get("rs_options");
                    c.rs_module = configMap.get("rs_module");
                    c.local_path = configMap.get("local_path");
                    c.name = configMap.get("rs_name");
                    c.rs_mode = configMap.get("rs_mode");
                    c.path_uri = configMap.get("path_uri");
                    c.adv_options = configMap.get("adv_options");
                    if (request==1) {
                        c.saveToDisk();

                        CharSequence text = "Configuration saved";
                        int duration = Toast.LENGTH_SHORT;
                        Toast toast = Toast.makeText(appContext, text, duration);
                        toast.show();
                    }
                    else if (request==2){
                        c.executeConfig(getApplication().getBaseContext(),null);

                        CharSequence text = "Rsync Job Started";
                        int duration = Toast.LENGTH_SHORT;
                        Toast toast = Toast.makeText(appContext, text, duration);
                        toast.show();
                    }
                    break;
                }
            }





        }
        else{


            CharSequence text = "Configuration is not Complete!";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(appContext, text, duration);
            toast.show();
        }

        this.finish();
    }




}
