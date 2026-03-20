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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
            Intent i = new Intent(this, Settings.class);
            startActivity(i);
        }

        return super.onOptionsItemSelected(item);
    }

    public void createConfig(View v){
        Intent i = new Intent(this, addConfig.class);
        startActivityForResult(i, 1);
    }

    public void createScheduler(View v){
        Intent i = new Intent(this, addScheduler.class);
        startActivityForResult(i, 2);
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

            viewPager.setCurrentItem(requestCode, false);
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
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
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
            clone.saveToDisk();
            configs.add(clone);
            onActivityResult(1, RESULT_OK, null);
        }
    }

    public void sched_clickHandler(@NonNull View v) {
        if (v.getTag(R.id.bt_edit_sched) != null) {
            int pos = (int) v.getTag(R.id.bt_edit_sched);
            Intent intent = new Intent(this, editSched.class);
            intent.putExtra("pos", pos);
            startActivityForResult(intent, 2);
        } else if (v.getTag(R.id.bt_delete_sched) != null) {
            final int pos = (int) v.getTag(R.id.bt_delete_sched);

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Scheduler")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        schedulers.get(pos).deleteFromDisk();
                        schedulers.get(pos).cancelAlarm(appContext);
                        schedulers.remove(pos);
                        onActivityResult(2, RESULT_OK, null);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                    .show();
        }
    }
}
