package com.linminitools.myrsync;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import androidx.documentfile.provider.DocumentFile;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.linminitools.myrsync.MainActivity.appContext;
import static com.linminitools.myrsync.myRsyncApplication.configs;
import static com.linminitools.myrsync.myRsyncApplication.schedulers;
import static com.linminitools.myrsync.MainActivity.getPath;

public class addConfig extends AppCompatActivity {
    boolean path_is_not_writable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemBars.setup(this);
        setContentView(R.layout.add_config);
        SystemBars.applyTopInsets(findViewById(R.id.config_app_bar));

        MaterialToolbar toolbar = findViewById(R.id.config_toolbar);
        toolbar.setTitle(getTitle());
        toolbar.setNavigationOnClickListener(v -> finish());

        final MaterialButton save = findViewById(R.id.bt_save);
        final MaterialButton view = findViewById(R.id.bt_view);
        save.setEnabled(false);
        view.setEnabled(false);

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
        ((TextInputEditText)findViewById(R.id.ed_ssh_password)).addTextChangedListener(commonWatcher);
        ((TextInputEditText)findViewById(R.id.ed_srv_ip)).addTextChangedListener(commonWatcher);

        RadioGroup transport_type = findViewById(R.id.rg_transport_type);
        transport_type.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_daemon) {
                    findViewById(R.id.lt_ssh_options).setVisibility(View.GONE);
                    findViewById(R.id.lt_rsync_srv_mod).setVisibility(View.VISIBLE);
                    TextInputLayout lt_rsync_user = findViewById(R.id.lt_rsync_user);
                    lt_rsync_user.setHint(getString(R.string.rsync_user));

                } else if (checkedId == R.id.rb_ssh) {
                    findViewById(R.id.lt_ssh_options).setVisibility(View.VISIBLE);
                    findViewById(R.id.lt_rsync_srv_mod).setVisibility(View.GONE);

                    TextInputLayout lt_rsync_user = findViewById(R.id.lt_rsync_user);
                    lt_rsync_user.setHint(getString(R.string.ssh_user));

                }
                updateSshAuthUi();
                updateButtons();
            }
        });

        initSshAuthControls();
        initScheduleEditorControls();
    }

    protected void initSshAuthControls() {
        RadioGroup authGroup = findViewById(R.id.rg_ssh_auth);
        if (authGroup == null) {
            return;
        }
        authGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateSshAuthUi();
            updateButtons();
        });
        updateSshAuthUi();
    }

    protected boolean isSshPasswordAuthSelected() {
        RadioGroup authGroup = findViewById(R.id.rg_ssh_auth);
        return authGroup != null && authGroup.getCheckedRadioButtonId() == R.id.rb_ssh_auth_password;
    }

    protected void updateSshAuthUi() {
        View keyRow = findViewById(R.id.lt_ssh_key_row);
        View passwordLayout = findViewById(R.id.lt_ssh_password);
        View passwordHint = findViewById(R.id.tv_ssh_password_hint);
        if (keyRow == null || passwordLayout == null) {
            return;
        }
        boolean password = isSshPasswordAuthSelected();
        keyRow.setVisibility(password ? View.GONE : View.VISIBLE);
        passwordLayout.setVisibility(password ? View.VISIBLE : View.GONE);
        if (passwordHint != null) {
            passwordHint.setVisibility(password ? View.VISIBLE : View.GONE);
        }
    }

    /** Call after {@code setContentView} whenever the hierarchy is (re)inflated (e.g. edit screen). */
    protected void initScheduleEditorControls() {
        MaterialButton addSched = findViewById(R.id.bt_add_schedule_slot);
        View schedulesBlock = findViewById(R.id.schedules_section_block);
        TextView schedulesLocked = findViewById(R.id.tv_schedules_pro_locked);

        boolean proSchedules = ProEntitlement.canUseSchedules(this);
        if (schedulesLocked != null) {
            schedulesLocked.setVisibility(proSchedules ? View.GONE : View.VISIBLE);
            if (!proSchedules) {
                schedulesLocked.setOnClickListener(v ->
                        ProUpgradeUi.showFeatureLocked(this, R.string.pro_limit_schedules));
            }
        }
        if (addSched != null) {
            addSched.setVisibility(proSchedules ? View.VISIBLE : View.GONE);
            addSched.setOnClickListener(v -> {
                if (!ProEntitlement.canUseSchedules(this)) {
                    ProUpgradeUi.showFeatureLocked(this, R.string.pro_limit_schedules);
                    return;
                }
                addSchedulerEditorRow(null);
            });
        }
        LinearLayout container = findViewById(R.id.schedulers_editor_container);
        if (container != null) {
            container.setVisibility(proSchedules ? View.VISIBLE : View.GONE);
            if (!proSchedules) {
                container.removeAllViews();
            }
        }
        if (schedulesBlock != null) {
            schedulesBlock.setAlpha(proSchedules ? 1f : 0.85f);
        }
    }

    protected void updateButtons() {
        MaterialButton save = findViewById(R.id.bt_save);
        MaterialButton view = findViewById(R.id.bt_view);
        TextInputEditText local_path = findViewById(R.id.ed_path);
        TextInputEditText remote_path = findViewById(R.id.ed_remote_path);
        TextInputEditText rsync_mod = findViewById(R.id.ed_rsync_mod);
        TextInputEditText ssh_key_path = findViewById(R.id.ed_ssh_key_path);
        TextInputEditText srv_ip = findViewById(R.id.ed_srv_ip);
        RadioGroup transport_type = findViewById(R.id.rg_transport_type);

        boolean hasIp = srv_ip.getText() != null && srv_ip.getText().length() > 0;
        boolean hasLocalPath = local_path.getText() != null && local_path.getText().length() > 0;
        boolean hasRemote = false;

        if (transport_type.getCheckedRadioButtonId() == R.id.rb_ssh) {
            boolean hasRemotePath = remote_path.getText() != null && remote_path.getText().length() > 0;
            TextInputEditText ssh_password = findViewById(R.id.ed_ssh_password);
            if (isSshPasswordAuthSelected()) {
                boolean hasPassword = ssh_password.getText() != null && ssh_password.getText().length() > 0;
                hasRemote = hasRemotePath && hasPassword;
            } else {
                boolean hasKeyPath = ssh_key_path.getText() != null && ssh_key_path.getText().length() > 0;
                hasRemote = hasRemotePath && hasKeyPath;
            }
        } else {
            hasRemote = rsync_mod.getText() != null && rsync_mod.getText().length() > 0;
        }

        boolean enabled = hasIp && hasLocalPath && hasRemote;
        save.setEnabled(enabled);
        view.setEnabled(enabled);
    }

    public void addPath(View v) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        i.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        i.setFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        Intent chooser = Intent.createChooser(i, "Choose Directory");
        startActivityForResult(chooser, 1);
    }

    public void addSshKeyPath(View v) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri pathUri = data.getData();

            if (requestCode == 1) {
                Uri dirUri = DocumentsContract.buildDocumentUriUsingTree(pathUri, DocumentsContract.getTreeDocumentId(pathUri));
                String path = getPath(appContext, dirUri);
                File dir = new File(path);
                boolean has_root_access = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("root_access", false);
                RadioGroup rg = findViewById(R.id.rg_mode);
                RadioButton checked = findViewById(rg.getCheckedRadioButtonId());

                if (checked.getText().equals("Pull") && (!has_root_access) && (!dir.canWrite())) {
                    path_is_not_writable = true;
                } else {
                    path_is_not_writable = false;
                    File f = new File(path);
                    f.setWritable(true, false);

                    SharedPreferences path_prefs = appContext.getSharedPreferences("Rsync_Config_path", MODE_PRIVATE);
                    SharedPreferences.Editor path_prefseditor = path_prefs.edit();
                    path_prefseditor.putString("local_path", path);
                    path_prefseditor.putString("path_uri", dirUri.toString());
                    path_prefseditor.commit();

                    TextInputEditText tv_path = findViewById(R.id.ed_path);
                    if (!Objects.requireNonNull(path).isEmpty()) {
                        tv_path.setText(path);
                        MaterialButton tv_addpath = findViewById(R.id.bt_add);
                        tv_addpath.setText(R.string.change_path);
                    }
                }
            } else if (requestCode == 2) {
                String path = getPath(appContext, pathUri);
                TextInputEditText et_ssh_key_path = findViewById(R.id.ed_ssh_key_path);
                if (path != null && !path.isEmpty()) {
                    et_ssh_key_path.setText(path);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (path_is_not_writable) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Selected Folder is not Writable!")
                    .setMessage(getResources().getString(R.string.select_folder))
                    .setPositiveButton("Close", (dialog, which) -> {})
                    .show();
            path_is_not_writable = false;
        }
    }

    Map<String, String> processForm() {
        Map<String, String> configMap = new HashMap<>();

        int checked_rb_transport_type = ((RadioGroup) findViewById(R.id.rg_transport_type)).getCheckedRadioButtonId();
        RadioButton rb_transport_type = findViewById(checked_rb_transport_type);

        TextInputEditText et_srv_ip = findViewById(R.id.ed_srv_ip);
        TextInputEditText et_srv_port = findViewById(R.id.ed_srv_port);
        TextInputEditText et_rs_user = findViewById(R.id.ed_rsync_user);
        TextInputEditText et_rs_mod = findViewById(R.id.ed_rsync_mod);
        TextInputEditText et_config_name = findViewById(R.id.et_rsync_alias);

        int checked_rb_mode = ((RadioGroup) findViewById(R.id.rg_mode)).getCheckedRadioButtonId();
        RadioButton rb_mode = findViewById(checked_rb_mode);

        String options = "-";
        String[] availableOptions = {"a", "r", "z", "v", "n", "p", "t", "O", "q", "m", "u", "g"};

        for (String check_box : availableOptions) {
            int resID = getResources().getIdentifier("cb_" + check_box, "id", getPackageName());
            CheckBox cb = findViewById(resID);
            if (cb.isChecked()) {
                options = options.concat(check_box);
            }
        }

        if (Objects.equals(options, "-")) {
            options = "";
        }

        TextInputEditText ed_adv_opts = findViewById(R.id.ed_advanced_options);
        String adv_options = ed_adv_opts.getText().toString();
        options = options.concat(" " + adv_options);

        TextInputEditText et_path = findViewById(R.id.ed_path);
        TextInputEditText et_remote_path = findViewById(R.id.ed_remote_path);
        TextInputEditText et_ssh_key_path = findViewById(R.id.ed_ssh_key_path);
        TextInputEditText et_ssh_password = findViewById(R.id.ed_ssh_password);

        String rs_transport_type = String.valueOf(rb_transport_type.getText()).trim();
        String rs_user = String.valueOf(et_rs_user.getText()).trim();
        String rs_ip = String.valueOf(et_srv_ip.getText()).trim();
        String rs_port = String.valueOf(et_srv_port.getText()).trim();
        String rs_module = String.valueOf(et_rs_mod.getText()).trim();
        String rs_name = String.valueOf(et_config_name.getText()).trim();
        String rs_mode = String.valueOf(rb_mode.getText()).trim();
        String local_path = String.valueOf(et_path.getText());
        String ssh_remote_path = String.valueOf(et_remote_path.getText());
        String ssh_private_key_path = String.valueOf(et_ssh_key_path.getText());
        String ssh_password = et_ssh_password.getText() != null
                ? et_ssh_password.getText().toString() : "";
        boolean passwordAuth = "SSH".equals(rs_transport_type) && isSshPasswordAuthSelected();
        String ssh_auth_type = passwordAuth
                ? RS_Configuration.SSH_AUTH_PASSWORD : RS_Configuration.SSH_AUTH_KEY;
        if (passwordAuth) {
            ssh_private_key_path = "";
        } else {
            ssh_password = "";
        }

        String path_uri = appContext.getSharedPreferences("Rsync_Config_path", MODE_PRIVATE).getString("path_uri", "");
        if (rs_port.isEmpty()) {
            rs_port = rs_transport_type.equals("SSH") ? "22" : "873";
        }

        configMap.put("rs_ip", rs_ip);
        configMap.put("rs_user", rs_user);
        configMap.put("rs_port", rs_port);
        configMap.put("rs_module", rs_module);
        configMap.put("local_path", local_path);
        configMap.put("rs_options", options);
        configMap.put("rs_name", rs_name);
        configMap.put("rs_mode", rs_mode);
        configMap.put("path_uri", path_uri);
        configMap.put("adv_options", adv_options);
        configMap.put("rs_ssh_remote_path", ssh_remote_path);
        configMap.put("rs_ssh_private_key_path", ssh_private_key_path);
        configMap.put("rs_ssh_auth_type", ssh_auth_type);
        configMap.put("rs_ssh_password", ssh_password);
        configMap.put("rs_transport_type", rs_transport_type);

        return configMap;
    }

    public void viewCommand(View v) {
        Map<String, String> configMap = processForm();

        String rs_transport_type = configMap.get("rs_transport_type");
        String options = configMap.get("rs_options");
        String rs_user = configMap.get("rs_user");
        String rs_ip = configMap.get("rs_ip");
        String rs_port = configMap.get("rs_port");
        String rs_module = configMap.get("rs_module");
        String local_path = configMap.get("local_path");
        String rs_mode = configMap.get("rs_mode");
        String rs_ssh_private_key_path = configMap.get("rs_ssh_private_key_path");
        String rs_ssh_remote_path = configMap.get("rs_ssh_remote_path");

        TextView tv = findViewById(R.id.tv_rs_cmd_View);

        String Rsync_command;
        String ssh_destination = rs_user + "@" + rs_ip + ":" + rs_ssh_remote_path;
        String daemon_destination = "rsync://" + rs_user + "@" + rs_ip + ":" + rs_port + "/" + rs_module;

        if (rs_transport_type.equals("SSH")) {
            String authNote = RS_Configuration.SSH_AUTH_PASSWORD.equals(configMap.get("rs_ssh_auth_type"))
                    ? " (" + getString(R.string.ssh_view_password_auth) + ")"
                    : " (-i key)";
            Rsync_command = rs_mode.equals("Push") ?
                    "rsync " + options + " " + local_path + " " + ssh_destination + authNote :
                    "rsync " + options + " " + ssh_destination + " " + local_path + authNote;
        }
        else {
            Rsync_command = rs_mode.equals("Push") ?
                    "rsync " + options + " " + local_path + " " + daemon_destination :
                    "rsync " + options + " " + daemon_destination + " " + local_path;
        }

        tv.setText(Rsync_command);
    }

    public void saveConfig(View v) {
        Map<String, String> configMap = processForm();

        String rs_options = configMap.get("rs_options");
        String rs_user = configMap.get("rs_user");
        String rs_ip = configMap.get("rs_ip");
        String rs_port = configMap.get("rs_port");
        String rs_module = configMap.get("rs_module");
        String local_path = configMap.get("local_path");
        String rs_name = configMap.get("rs_name");
        String rs_mode = configMap.get("rs_mode");
        String path_uri = configMap.get("path_uri");
        String adv_options = configMap.get("adv_options");
        String rs_transport_type = configMap.get("rs_transport_type");
        String rs_ssh_private_key_path = configMap.get("rs_ssh_private_key_path");
        String rs_ssh_remote_path = configMap.get("rs_ssh_remote_path");
        String rs_ssh_auth_type = configMap.get("rs_ssh_auth_type");
        String rs_ssh_password = configMap.get("rs_ssh_password");

        boolean is_ssh = rs_transport_type.equals("SSH");
        boolean remote_complete = false;
        if (is_ssh) {
            boolean authOk = RS_Configuration.SSH_AUTH_PASSWORD.equals(rs_ssh_auth_type)
                    ? (rs_ssh_password != null && !rs_ssh_password.isEmpty())
                    : (rs_ssh_private_key_path != null && !rs_ssh_private_key_path.isEmpty());
            remote_complete = rs_ssh_remote_path != null && !rs_ssh_remote_path.isEmpty() && authOk;
        } else {
            remote_complete = !rs_module.isEmpty();
        }

        if (!rs_ip.isEmpty() && remote_complete && !local_path.isEmpty()) {
            if (!ProEntitlement.canCreateConfig(this)) {
                ProUpgradeUi.showFeatureLocked(this, R.string.pro_limit_configs);
                return;
            }
            if (!validateScheduleRows()) {
                return;
            }

            int id = 1;
            SharedPreferences config_prefs = getSharedPreferences("configs", MODE_PRIVATE);

            while (config_prefs.getInt("rs_id_" + String.valueOf(id), -1) > 0) {
                id = id + 1;
            }

            RS_Configuration config = new RS_Configuration(id);
            config.rs_ip = rs_ip;
            config.rs_user = rs_user;
            config.rs_port = rs_port;
            config.rs_options = rs_options;
            config.rs_module = rs_module;
            config.local_path = local_path;
            config.name = rs_name;
            config.rs_mode = rs_mode;
            config.adv_options = adv_options;
            config.rs_transport_type = rs_transport_type;
            config.rs_ssh_private_key_path = rs_ssh_private_key_path;
            config.rs_ssh_remote_path = rs_ssh_remote_path;
            config.rs_ssh_auth_type = rs_ssh_auth_type;
            config.rs_ssh_password = rs_ssh_password;

            config.saveToDisk();
            config_prefs.edit().putString("path_uri_" + config.id, path_uri).apply();
            configs.add(config);
            syncSchedulersFromContainer(config.id);
            this.finish();
        } else {
            Toast.makeText(getApplicationContext(), "Configuration is not Complete!", Toast.LENGTH_SHORT).show();
        }
    }

    public void rsync_help(View v) {
        new MaterialAlertDialogBuilder(v.getContext())
                .setTitle("Rsync Options Help")
                .setMessage(getResources().getString(R.string.rsync_options))
                .setPositiveButton("Close", (dialog, which) -> {})
                .show();
    }

    public void rsync_adv_opts_help(View v) {
        new MaterialAlertDialogBuilder(v.getContext())
                .setTitle("Rsync Advanced Options Help")
                .setMessage(getResources().getString(R.string.rsync_adv_options))
                .setPositiveButton("Close", (dialog, which) -> {})
                .show();
    }

    public void select_path_help(View v) {
        new MaterialAlertDialogBuilder(v.getContext())
                .setTitle("Pull Configuration Folder Select")
                .setMessage(getResources().getString(R.string.select_folder))
                .setPositiveButton("Close", (dialog, which) -> {})
                .show();
    }

    public void rsync_daemon_info(View v) {
        SpannableString s = new SpannableString(getResources().getString(R.string.rsync_daemon_info));
        Pattern p = Pattern.compile("https://download.samba.org/pub/rsync/rsyncd.conf.html");
        Linkify.addLinks(s, p, null);

        new MaterialAlertDialogBuilder(v.getContext())
                .setTitle("Build Rsync Configuration")
                .setMessage(s)
                .setPositiveButton("Close", (dialog, which) -> {})
                .show();
    }

    private static final int[] SCHED_DAY_BTN_IDS = {
            R.id.tb_monday, R.id.tb_tuesday, R.id.tb_wednesday, R.id.tb_thursday,
            R.id.tb_friday, R.id.tb_saturday, R.id.tb_sunday
    };
    private static final String[] SCHED_DAY_NAMES = {
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"
    };

    protected static class ScheduleRowData {
        final String days;
        final int hour, min;
        final boolean wifiSw;

        ScheduleRowData(String days, int hour, int min, boolean wifiSw) {
            this.days = days;
            this.hour = hour;
            this.min = min;
            this.wifiSw = wifiSw;
        }
    }

    protected void addSchedulerEditorRow(Scheduler existing) {
        LinearLayout container = findViewById(R.id.schedulers_editor_container);
        View row = LayoutInflater.from(this).inflate(R.layout.item_scheduler_editor, container, false);
        container.addView(row);

        TimePicker tp = row.findViewById(R.id.sched_time_picker);
        tp.setIs24HourView(true);

        MaterialSwitch sw = row.findViewById(R.id.sched_sw_wifi);

        row.findViewById(R.id.bt_remove_schedule_slot).setOnClickListener(v -> {
            container.removeView(row);
            refreshScheduleSlotTitles();
        });

        if (existing != null) {
            tp.setHour(existing.hour);
            tp.setMinute(existing.min);
            String[] active = existing.days.split("[.]");
            for (String a : active) {
                if (a.isEmpty()) continue;
                int rid = getResources().getIdentifier("tb_" + a, "id", getPackageName());
                if (rid != 0) {
                    MaterialButton btn = row.findViewById(rid);
                    if (btn != null) btn.setChecked(true);
                }
            }
            sw.setChecked(Boolean.TRUE.equals(existing.wifi_sw));
        }

        refreshScheduleSlotTitles();
    }

    protected void refreshScheduleSlotTitles() {
        LinearLayout c = findViewById(R.id.schedulers_editor_container);
        for (int i = 0; i < c.getChildCount(); i++) {
            View row = c.getChildAt(i);
            TextView title = row.findViewById(R.id.tv_schedule_slot_title);
            title.setText(getString(R.string.schedule_number, i + 1));
        }
    }

    protected void loadSchedulerEditorsForConfig(int configId) {
        LinearLayout c = findViewById(R.id.schedulers_editor_container);
        if (c == null || !ProEntitlement.canUseSchedules(this)) {
            return;
        }
        c.removeAllViews();
        ArrayList<Scheduler> list = Scheduler.schedulersForConfig(configId);
        for (Scheduler s : list) {
            addSchedulerEditorRow(s);
        }
    }

    protected ScheduleRowData readScheduleRow(View row) {
        TimePicker tp = row.findViewById(R.id.sched_time_picker);
        StringBuilder repeat = new StringBuilder(".");
        for (int i = 0; i < SCHED_DAY_BTN_IDS.length; i++) {
            MaterialButton btn = row.findViewById(SCHED_DAY_BTN_IDS[i]);
            if (btn != null && btn.isChecked()) {
                repeat.append(SCHED_DAY_NAMES[i]).append(".");
            }
        }
        MaterialSwitch sw = row.findViewById(R.id.sched_sw_wifi);
        return new ScheduleRowData(repeat.toString(), tp.getHour(), tp.getMinute(), sw.isChecked());
    }

    protected boolean validateScheduleRows() {
        if (!ProEntitlement.canUseSchedules(this)) {
            return true;
        }
        LinearLayout c = findViewById(R.id.schedulers_editor_container);
        if (c == null) {
            return true;
        }
        for (int i = 0; i < c.getChildCount(); i++) {
            ScheduleRowData rd = readScheduleRow(c.getChildAt(i));
            if (rd.days.equals(".")) {
                Toast.makeText(this, R.string.schedule_needs_day, Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    protected void syncSchedulersFromContainer(int configId) {
        LinearLayout container = findViewById(R.id.schedulers_editor_container);
        ArrayList<ScheduleRowData> pending = new ArrayList<>();
        if (ProEntitlement.canUseSchedules(this) && container != null) {
            for (int i = 0; i < container.getChildCount(); i++) {
                pending.add(readScheduleRow(container.getChildAt(i)));
            }
        }
        for (Scheduler s : new ArrayList<>(schedulers)) {
            if (s.config_id == configId) {
                s.cancelAlarm(this);
                s.deleteFromDisk();
                schedulers.remove(s);
            }
        }
        for (int i = 0; i < pending.size(); i++) {
            ScheduleRowData rd = pending.get(i);
            TimePicker tp = new TimePicker(this);
            tp.setIs24HourView(true);
            tp.setHour(rd.hour);
            tp.setMinute(rd.min);
            int sid = SchedulerIds.makeId(configId, i);
            Scheduler sched = new Scheduler(rd.days, tp, sid);
            sched.name = SchedulerIds.internalLabel(configId, i);
            sched.config_id = configId;
            sched.wifi_sw = rd.wifiSw;
            sched.wifi_ssid = "";
            sched.saveToDisk();
            sched.setAlarm(appContext);
            schedulers.add(sched);
        }
        Collections.sort(schedulers);
    }

}
