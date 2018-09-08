package com.linminitools.myrsync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

import static com.linminitools.myrsync.MainActivity.appContext;
import static com.linminitools.myrsync.MainActivity.configs;

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
        bt_add.setText("Change Path");
        bt_exec.setEnabled(true);

        Intent i = getIntent();
        final int p = i.getIntExtra("pos", 0);
        RS_Configuration config = configs.get(p);

        EditText ed_srv_ip = findViewById(R.id.ed_srv_ip);
        ed_srv_ip.setText(config.rs_ip);

        EditText ed_rsync_user = findViewById(R.id.ed_rsync_user);
        ed_rsync_user.setText(config.rs_user);

        EditText ed_srv_port = findViewById(R.id.ed_srv_port);
        ed_srv_port.setText(config.rs_port);

        EditText ed_rsync_mod = findViewById(R.id.ed_rsync_mod);
        ed_rsync_mod.setText(config.rs_module);

        TextView tv_path = findViewById(R.id.tv_path);
        tv_path.setVisibility(View.VISIBLE);
        tv_path.setText(config.local_path);

        String rs_options = config.rs_options;

        if (rs_options == "-") {
            rs_options = "";
        }

        if (!rs_options.isEmpty()) {
            String options = rs_options.substring(1);
            for (char x : options.toCharArray()) {
                int resID = getResources().getIdentifier("cb_" + String.valueOf(x), "id", getPackageName());
                CheckBox cb = findViewById(resID);
                cb.setChecked(true);
            }
        }

        final int id = config.id;

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionConfig(v,id,p,1);
            }
        });

        bt_exec.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                actionConfig(v, id, p,2);
            }
        });

    }

    public void actionConfig(View v, int id, int position, int request){
        Map<String,String> configMap=processForm(v);

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
                    if (request==1) {
                        c.saveToDisk();

                        CharSequence text = "Configuration saved";
                        int duration = Toast.LENGTH_SHORT;
                        Toast toast = Toast.makeText(appContext, text, duration);
                        toast.show();
                    }
                    else if (request==2){
                        c.executeConfig(getApplication().getBaseContext());

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
