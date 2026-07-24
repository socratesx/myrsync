package com.linminitools.myrsync;

import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import androidx.annotation.NonNull;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.viewpager2.widget.ViewPager2;
import androidx.appcompat.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

import static com.linminitools.myrsync.myRsyncApplication.schedulers;
import static com.linminitools.myrsync.myRsyncApplication.configs;

public class MainActivity extends AppCompatActivity {
    public static final String SELECTED_TAB = "selected_tab";

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int MANAGE_STORAGE_REQUEST_CODE = 100;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;
    public static Context appContext;
    static File log_file;
    static File debug_log;

    private ViewPager2 viewPager;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appContext = getApplicationContext();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        requestRequiredPermissions();

        Locale current_locale = appContext.getResources().getConfiguration().locale;
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);

        String log_path = appContext.getApplicationInfo().dataDir + "/logfile.log";
        log_file = new File(log_path);

        String Debug_log_path = appContext.getApplicationInfo().dataDir + "/debug.log";
        debug_log = new File(Debug_log_path);

        if (!debug_log.exists()) {
            try {
                debug_log.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        viewPager = findViewById(R.id.pager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        TabLayout tabLayout = findViewById(R.id.tabs);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(ViewPagerAdapter.getTabTitle(position))
        ).attach();

        int selectedTab = getIntent().getIntExtra(SELECTED_TAB, 0);
        if (selectedTab > 0) {
            viewPager.setCurrentItem(selectedTab, false);
        }

        // Re-arm alarms on launch (covers upgrades from inexact setRepeating).
        for (Scheduler s : schedulers) {
            try {
                s.setAlarm(appContext);
            } catch (Exception e) {
                Log.e("MainActivity", "Failed to re-arm scheduler id=" + s.id, e);
            }
        }
        maybePromptExactAlarmPermission();

        String rsyncBinaryPath = getApplicationInfo().nativeLibraryDir + "/librsync.so";
        getSharedPreferences("Install", MODE_PRIVATE).edit()
                .putString("rsync_binary", rsyncBinaryPath)
                .apply();

        boolean is_first_run = getSharedPreferences("Install", MODE_PRIVATE).getBoolean("first_run", true);
        if (is_first_run) {
            getSharedPreferences("Install", MODE_PRIVATE).edit().putBoolean("first_run", false).apply();
            try {
                FileWriter debug_writer = new FileWriter(debug_log, true);
                CharSequence message = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + "FIRST RUN INITIALIZATION {\n"+
                        "RSYNC BINARY: " + rsyncBinaryPath + "\n" +
                        "SUPPORTED ABIS: "+ Arrays.toString(Build.SUPPORTED_ABIS)+"\nSDK_Version: "+ Build.VERSION.SDK_INT+"\n}";
                debug_writer.append(message);
                debug_writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.me_exit) {
            this.finishAffinity();
            return true;
        }

        if (id == R.id.me_about) {
            SpannableString s = new SpannableString(getResources().getString(R.string.about));
            String version="";
            try {
                PackageInfo pInfo = appContext.getPackageManager().getPackageInfo(getPackageName(), 0);
                version = pInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            s = SpannableString.valueOf(s+"\n\n myRsync Version: Beta-"+version);
            Linkify.addLinks(s, Linkify.ALL);

            new MaterialAlertDialogBuilder(this)
                    .setTitle("About")
                    .setMessage(s)
                    .setPositiveButton("Close", (dialog, which) -> {})
                    .show();
        }

        if (id == R.id.me_help) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Help")
                    .setMessage(getResources().getString(R.string.help))
                    .setPositiveButton("Close", (dialog, which) -> {})
                    .show();
        }

        if (id == R.id.me_settings) {
            // Fully qualified: android.provider.Settings is also imported in this file.
            Intent i = new Intent(this, com.linminitools.myrsync.Settings.class);
            startActivity(i);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Context app = getApplicationContext();
        if (app instanceof myRsyncApplication) {
            ((myRsyncApplication) app).getBillingManager().queryPurchases();
        }
    }

    public void createConfig(View v){
        if (!ProEntitlement.canCreateConfig(this)) {
            ProUpgradeUi.showFeatureLocked(this, R.string.pro_limit_configs);
            return;
        }
        Intent i = new Intent(this, addConfig.class);
        startActivityForResult(i, 1);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 41 || requestCode == 42) {
            try {
                Uri pathUri = data.getData();
                Uri dirUri = DocumentsContract.buildDocumentUriUsingTree(pathUri, DocumentsContract.getTreeDocumentId(pathUri));

                DocumentFile pickedDir = DocumentFile.fromTreeUri(appContext, dirUri);

                File selected_logfile = log_file;
                if (requestCode == 42) selected_logfile = debug_log;
                DocumentFile previous_log = Objects.requireNonNull(pickedDir).findFile(selected_logfile.getName());
                if (previous_log != null) previous_log.delete();

                DocumentFile exported_log = pickedDir.createFile("log", selected_logfile.getName());

                FileInputStream inputStream = new FileInputStream(selected_logfile);
                OutputStream outputStream = getContentResolver().openOutputStream(Objects.requireNonNull(exported_log).getUri());

                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                inputStream.close();

                Objects.requireNonNull(outputStream).write(buffer);
                outputStream.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ViewPagerAdapter adapter = new ViewPagerAdapter(this);
            viewPager.setAdapter(adapter);

            TabLayout tabLayout = findViewById(R.id.tabs);
            new TabLayoutMediator(tabLayout, viewPager,
                    (tab, position) -> tab.setText(ViewPagerAdapter.getTabTitle(position))
            ).attach();

            if (requestCode == 1) {
                viewPager.setCurrentItem(1, false);
            }
        }
    }

    public static String getPath(@NonNull final Context context, @NonNull final Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                StringBuilder path;
                if ("primary".equalsIgnoreCase(type)) {
                    path = new StringBuilder(Environment.getExternalStorageDirectory().toString());
                    for (int i = 0; i < split.length; i++) {
                        if (i > 0) path.append("/").append(split[i]);
                    }
                    return path.toString();
                } else {
                    path = new StringBuilder("/storage");
                    for (String aSplit : split) {
                        path.append("/").append(aSplit);
                    }
                    return path.toString();
                }
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                switch (type) {
                    case "image":
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        break;
                    case "video":
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        break;
                    case "audio":
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        break;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, Objects.requireNonNull(contentUri), selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private static String getDataColumn(Context context, @NonNull Uri uri, String selection,
                                        String[] selectionArgs) {
        final String column = "_data";
        final String[] projection = {column};

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private void requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.storage_access_title)
                        .setMessage(R.string.storage_access_message)
                        .setPositiveButton(R.string.storage_access_continue, (dialog, which) -> {
                            Intent intent = new Intent(
                                    android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
                        })
                        .setNegativeButton(R.string.storage_access_not_now, null)
                        .show();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("Permissions", "Permission granted for request code: " + requestCode);
        } else {
            Log.d("Permissions", "Permission denied for request code: " + requestCode);
        }
    }

    public void config_clickHandler(@NonNull View v) {
        if (v.getTag(R.id.bt_edit) != null) {
            int pos = (int) v.getTag(R.id.bt_edit);
            Intent intent = new Intent(this, editConfig.class);
            intent.putExtra("pos", pos);
            this.startActivityForResult(intent, 1);
        } else if (v.getTag(R.id.bt_delete) != null) {
            final int pos = (int) v.getTag(R.id.bt_delete);

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Configuration")
                    .setMessage("This action will delete this configuration and all its schedulers. Are you sure?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        RS_Configuration deleted_config = configs.get(pos);
                        deleted_config.deleteFromDisk();
                        configs.remove(pos);

                        ArrayList<Scheduler> connected = new ArrayList<>();
                        for (Scheduler s : schedulers) {
                            if (deleted_config.id == s.config_id) {
                                connected.add(s);
                                s.deleteFromDisk();
                            }
                        }
                        schedulers.removeAll(connected);
                        onActivityResult(1, RESULT_OK, null);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                    .show();
        } else if (v.getTag(R.id.bt_run) != null) {
            final int pos = (int) v.getTag(R.id.bt_run);
            configs.get(pos).executeConfig(this,null);
        }
        else if (v.getTag(R.id.bt_clone) != null) {
            if (!ProEntitlement.canCreateConfig(this)) {
                ProUpgradeUi.showFeatureLocked(this, R.string.pro_limit_configs);
                return;
            }
            final int pos = (int) v.getTag(R.id.bt_clone);
            RS_Configuration original = configs.get(pos);

            int newId = 1;
            SharedPreferences config_prefs = getSharedPreferences("configs", MODE_PRIVATE);
            while (config_prefs.getInt("rs_id_" + newId, -1) > 0) {
                newId++;
            }
            RS_Configuration clone = new RS_Configuration(newId);
            clone.name = original.name + " (copy)";
            clone.rs_ip = original.rs_ip;
            clone.rs_user = original.rs_user;
            clone.rs_port = original.rs_port;
            clone.rs_module = original.rs_module;
            clone.rs_options = original.rs_options;
            clone.rs_mode = original.rs_mode;
            clone.local_path = original.local_path;
            clone.adv_options = original.adv_options;
            clone.rs_transport_type = original.rs_transport_type;
            clone.rs_ssh_private_key_path = original.rs_ssh_private_key_path;
            clone.rs_ssh_remote_path = original.rs_ssh_remote_path;
            clone.saveToDisk();
            SharedPreferences cfg = getSharedPreferences("configs", MODE_PRIVATE);
            cfg.edit().putString("path_uri_" + clone.id, cfg.getString("path_uri_" + original.id, "")).apply();

            if (ProEntitlement.canUseSchedules(this)) {
                ArrayList<Scheduler> toCopy = Scheduler.schedulersForConfig(original.id);
                int slot = 0;
                for (Scheduler oldS : toCopy) {
                    TimePicker tp = new TimePicker(this);
                    tp.setIs24HourView(true);
                    tp.setHour(oldS.hour);
                    tp.setMinute(oldS.min);
                    int sid = SchedulerIds.makeId(clone.id, slot);
                    Scheduler neu = new Scheduler(oldS.days, tp, sid);
                    neu.name = SchedulerIds.internalLabel(clone.id, slot);
                    neu.config_id = clone.id;
                    neu.wifi_sw = oldS.wifi_sw;
                    neu.wifi_ssid = oldS.wifi_ssid != null ? oldS.wifi_ssid : "";
                    neu.saveToDisk();
                    neu.setAlarm(appContext);
                    schedulers.add(neu);
                    slot++;
                }
                Collections.sort(schedulers);
            }

            configs.add(clone);
            onActivityResult(1, RESULT_OK, null);
        }
    }

    /**
     * On Android 12+, exact alarms require a user-grantable app-op. Without it, schedules
     * fall back to inexact delivery and can miss the configured minute.
     */
    private void maybePromptExactAlarmPermission() {
        if (schedulers.isEmpty()) {
            return;
        }
        if (Scheduler.canScheduleExactAlarms(this)) {
            return;
        }
        SharedPreferences prefs = getSharedPreferences("Install", MODE_PRIVATE);
        if (prefs.getBoolean("exact_alarm_prompt_shown", false)) {
            return;
        }
        prefs.edit().putBoolean("exact_alarm_prompt_shown", true).apply();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Allow exact alarms")
                .setMessage("myRSync needs the \"Alarms & reminders\" permission so scheduled "
                        + "syncs run at the time you set. Without it, Android may delay them.")
                .setPositiveButton("Open settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e("MainActivity", "Could not open exact-alarm settings", e);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
