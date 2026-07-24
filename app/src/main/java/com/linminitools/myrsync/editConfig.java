package com.linminitools.myrsync;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.content.SharedPreferences;

import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static com.linminitools.myrsync.MainActivity.appContext;
import static com.linminitools.myrsync.myRsyncApplication.configs;

public class editConfig extends addConfig {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_config);

        MaterialToolbar toolbar = findViewById(R.id.config_toolbar);
        toolbar.setTitle("Edit Configuration");
        toolbar.setNavigationOnClickListener(v -> finish());

        MaterialButton save = findViewById(R.id.bt_save);
        MaterialButton view = findViewById(R.id.bt_view);
        MaterialButton bt_add = findViewById(R.id.bt_add);
        MaterialButton bt_exec = findViewById(R.id.bt_execute);

        save.setEnabled(true);
        view.setEnabled(true);
        bt_add.setText(R.string.change_path);
        bt_exec.setEnabled(true);

        Intent i = getIntent();
        final int p = i.getIntExtra("pos", 0);
        RS_Configuration config = configs.get(p);

        TextInputEditText ed_rs_name = findViewById(R.id.et_rsync_alias);
        ed_rs_name.setText(config.name);

        TextInputEditText ed_srv_ip = findViewById(R.id.ed_srv_ip);
        ed_srv_ip.setText(config.rs_ip);

        TextInputEditText ed_rsync_user = findViewById(R.id.ed_rsync_user);
        ed_rsync_user.setText(config.rs_user);

        TextInputEditText ed_srv_port = findViewById(R.id.ed_srv_port);
        ed_srv_port.setText(config.rs_port);

        TextInputEditText ed_rsync_mod = findViewById(R.id.ed_rsync_mod);
        ed_rsync_mod.setText(config.rs_module);

        RadioGroup transport_type = findViewById(R.id.rg_transport_type);
        if ("SSH".equals(config.rs_transport_type)) {
            transport_type.check(R.id.rb_ssh);
            findViewById(R.id.lt_ssh_options).setVisibility(View.VISIBLE);
            findViewById(R.id.lt_rsync_srv_mod).setVisibility(View.GONE);
            ((TextInputLayout)findViewById(R.id.lt_rsync_user)).setHint(getString(R.string.ssh_user));
        } else {
            transport_type.check(R.id.rb_daemon);
            findViewById(R.id.lt_ssh_options).setVisibility(View.GONE);
            findViewById(R.id.lt_rsync_srv_mod).setVisibility(View.VISIBLE);
            ((TextInputLayout)findViewById(R.id.lt_rsync_user)).setHint(getString(R.string.rsync_user));
        }

        transport_type.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_daemon) {
                findViewById(R.id.lt_ssh_options).setVisibility(View.GONE);
                findViewById(R.id.lt_rsync_srv_mod).setVisibility(View.VISIBLE);
                ((TextInputLayout)findViewById(R.id.lt_rsync_user)).setHint(getString(R.string.rsync_user));
            } else if (checkedId == R.id.rb_ssh) {
                findViewById(R.id.lt_ssh_options).setVisibility(View.VISIBLE);
                findViewById(R.id.lt_rsync_srv_mod).setVisibility(View.GONE);
                ((TextInputLayout)findViewById(R.id.lt_rsync_user)).setHint(getString(R.string.ssh_user));
            }
            updateButtons();
        });

        RadioGroup rg_mode = findViewById(R.id.rg_mode);
        if ("Pull".equals(config.rs_mode)) rg_mode.check(R.id.rb_pull);
        else rg_mode.check(R.id.rb_push);

        TextInputEditText tv_local_path = findViewById(R.id.ed_path);
        tv_local_path.setText(config.local_path);

        TextInputEditText tv_remote_path = findViewById(R.id.ed_remote_path);
        tv_remote_path.setText(config.rs_ssh_remote_path);

        TextInputEditText tv_ssh_key_path = findViewById(R.id.ed_ssh_key_path);
        tv_ssh_key_path.setText(config.rs_ssh_private_key_path);

        TextWatcher commonWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateButtons();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        ((TextInputEditText)findViewById(R.id.ed_path)).addTextChangedListener(commonWatcher);
        ((TextInputEditText)findViewById(R.id.ed_remote_path)).addTextChangedListener(commonWatcher);
        ((TextInputEditText)findViewById(R.id.ed_rsync_mod)).addTextChangedListener(commonWatcher);
        ((TextInputEditText)findViewById(R.id.ed_ssh_key_path)).addTextChangedListener(commonWatcher);
        ((TextInputEditText)findViewById(R.id.ed_srv_ip)).addTextChangedListener(commonWatcher);

        String rs_options = config.rs_options;
        if (rs_options != null && !rs_options.isEmpty() && rs_options.startsWith("-")) {
            String options = rs_options.split(" ")[0].substring(1);
            for (char x : options.toCharArray()) {
                int resID = getResources().getIdentifier("cb_" + x, "id", getPackageName());
                if (resID != 0) {
                    CheckBox cb = findViewById(resID);
                    if (cb != null) cb.setChecked(true);
                }
            }
        }

        TextInputEditText ed_adv_options = findViewById(R.id.ed_advanced_options);
        ed_adv_options.setText(config.adv_options);

        final int id = config.id;
        save.setOnClickListener(v -> actionConfig(id, 1));
        bt_exec.setOnClickListener(v -> actionConfig(id, 2));

        initScheduleEditorControls();
        loadSchedulerEditorsForConfig(id);
    }

    private void actionConfig(int id, int request) {
        Map<String, String> configMap = processForm();
        boolean is_ssh = "SSH".equals(configMap.get("rs_transport_type"));
        boolean remote_complete = is_ssh ? 
            (!configMap.get("rs_ssh_remote_path").isEmpty() && !configMap.get("rs_ssh_private_key_path").isEmpty()) : 
            !configMap.get("rs_module").isEmpty();

        if (!configMap.get("rs_ip").isEmpty() && remote_complete && !configMap.get("local_path").isEmpty()) {
            if (request == 1 && !validateScheduleRows()) {
                return;
            }
            for (RS_Configuration c : configs) {
                if (c.id == id) {
                    c.rs_ip = configMap.get("rs_ip");
                    c.rs_user = configMap.get("rs_user");
                    c.rs_port = configMap.get("rs_port");
                    c.rs_options = configMap.get("rs_options");
                    c.rs_module = configMap.get("rs_module");
                    c.local_path = configMap.get("local_path");
                    c.name = configMap.get("rs_name");
                    c.rs_mode = configMap.get("rs_mode");
                    c.adv_options = configMap.get("adv_options");
                    c.rs_transport_type = configMap.get("rs_transport_type");
                    c.rs_ssh_private_key_path = configMap.get("rs_ssh_private_key_path");
                    c.rs_ssh_remote_path = configMap.get("rs_ssh_remote_path");

                    if (request == 1) {
                        c.saveToDisk();
                        getSharedPreferences("configs", MODE_PRIVATE).edit()
                                .putString("path_uri_" + c.id, configMap.get("path_uri"))
                                .apply();
                        syncSchedulersFromContainer(id);
                        Toast.makeText(appContext, "Configuration saved", Toast.LENGTH_SHORT).show();
                    } else if (request == 2) {
                        c.executeConfig(getApplication().getBaseContext(), null);
                        Toast.makeText(appContext, "Rsync Job Started", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
            }
            this.finish();
        } else {
            Toast.makeText(appContext, "Configuration is not Complete!", Toast.LENGTH_SHORT).show();
        }
    }
}
