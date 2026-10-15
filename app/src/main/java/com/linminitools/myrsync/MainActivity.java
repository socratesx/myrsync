package com.linminitools.myrsync;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.linminitools.myrsync.rsync.RsyncBridge;
import com.linminitools.myrsync.util.UiSafety;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.linminitools.myrsync.myRsyncApplication.schedulers;
import static com.linminitools.myrsync.myRsyncApplication.configs;

public class MainActivity extends AppCompatActivity {
    public static final String SELECTED_TAB = "selected_tab";

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2;
    public static Context appContext;
    static File log_file;
    static File debug_log;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UiSafety.stripDecorViewDrawables(getWindow().getDecorView());
        appContext = getApplicationContext();

        Locale current_locale = appContext.getResources().getConfiguration().locale;
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);

        String log_path = appContext.getApplicationInfo().dataDir + "/logfile.log";
        log_file = new File(log_path);

        String Debug_log_path = appContext.getApplicationInfo().dataDir + "/debug.log";
        debug_log = new File(Debug_log_path);

        ensureLogFileExists(log_file, "MainActivity", "Unable to create rsync log file");
        ensureLogFileExists(debug_log, "MainActivity", "Unable to create debug log file");

        UiSafety.runOnUiThreadIfAlive(this, () -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                        && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("notifications", true)) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_REQUEST_CODE);
                }
            } else if (Build.VERSION.SDK_INT >= 23 && !checkPermission()) {
                requestPermission();
            }

            ViewPager viewPager = findViewById(R.id.pager);
            ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
            adapter.refresh_adapter();

            viewPager.setAdapter(adapter);

            TabLayout tabLayout = findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(viewPager);
            TabLayout.Tab tab = tabLayout.getTabAt(getIntent().getIntExtra(SELECTED_TAB, 0));
            if (tab != null) {
                tab.select();
            }
        });

        Boolean is_first_run = getSharedPreferences("Install", MODE_PRIVATE).getBoolean("first_run", true);
        if (prepareRsyncExecutable(is_first_run, formatter)) {
            getSharedPreferences("Install", MODE_PRIVATE).edit().putBoolean("first_run", false).apply();
        }
    }

    private static void ensureLogFileExists(@NonNull File file, @NonNull String tag, @NonNull String errorMessage) {
        if (file.exists()) {
            return;
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            Log.w(tag, errorMessage + ": unable to create parent directories for " + parent.getAbsolutePath());
        }

        try {
            if (!file.exists() && !file.createNewFile()) {
                Log.w(tag, errorMessage);
            }
        } catch (IOException e) {
            Log.e(tag, errorMessage, e);
        }
    }

    private boolean prepareRsyncExecutable(boolean forceCopy, SimpleDateFormat formatter) {
        return ensureRsyncExecutable(this, forceCopy, formatter);
    }

    static boolean ensureRsyncExecutable(@NonNull Context context, @SuppressWarnings("unused") boolean forceCopy, SimpleDateFormat formatter) {
        Context appContext = context.getApplicationContext();

        if (log_file == null) {
            log_file = new File(appContext.getApplicationInfo().dataDir + "/logfile.log");
        }
        if (debug_log == null) {
            debug_log = new File(appContext.getApplicationInfo().dataDir + "/debug.log");
        }

        ensureLogFileExists(log_file, "MainActivity", "Unable to create rsync log file");
        ensureLogFileExists(debug_log, "MainActivity", "Unable to create debug log file");

        try {
            RsyncBridge.ensureExecutable(appContext);
            appendDebugLog(formatter, "RSYNC EXECUTABLE AVAILABLE");
            return true;
        } catch (IOException | SecurityException error) {
            String message = "FAILED TO PREPARE RSYNC EXECUTABLE: " + error.getMessage();
            appendDebugLog(formatter, message);
            Log.e("MainActivity", message, error);
            return false;
        }
    }

    protected static void appendDebugLog(SimpleDateFormat formatter, String message) {
        try {
            FileWriter debug_writer = new FileWriter(debug_log, true);
            CharSequence logMessage = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + message;
            debug_writer.append(logMessage);
            debug_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static void appendRsyncLog(SimpleDateFormat formatter, String message) {
        try {
            FileWriter logWriter = new FileWriter(log_file, true);
            CharSequence logMessage = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + message;
            logWriter.append(logMessage);
            logWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (UiSafety.isUiDead(this)) {
            return false;
        }
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.app_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (UiSafety.isUiDead(this)) {
            return false;
        }

        int id = item.getItemId();

        if (id == R.id.me_exit) {
            UiSafety.runOnUiThreadIfAlive(this, this::finishAffinity);
            return true;
        }

        if (id == R.id.me_about) {
            String versionName = "";
            try {
                PackageInfo pInfo = appContext.getPackageManager().getPackageInfo(getPackageName(), 0);
                versionName = pInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            final SpannableString aboutMessage = new SpannableString(getResources().getString(R.string.about) + "\n\n myRsync Version: Beta-" + versionName);
            Linkify.addLinks(aboutMessage, Linkify.ALL);

            UiSafety.runOnUiThreadIfAlive(this, () -> {
                if (UiSafety.isUiDead(this)) {
                    return;
                }
                AlertDialog alertDialog = new AlertDialog.Builder(this)
                        .setTitle("About")
                        .setMessage(aboutMessage)
                        .setPositiveButton("Close", (dialog, which) -> {
                        })
                        .show();
                TextView textView = alertDialog.findViewById(android.R.id.message);
                if (textView != null) {
                    textView.setTextSize(12);
                    textView.setMovementMethod(LinkMovementMethod.getInstance());
                }
            });
            return true;
        }

        if (id == R.id.me_help) {
            final CharSequence helpMessage = getResources().getString(R.string.help);
            UiSafety.runOnUiThreadIfAlive(this, () -> {
                if (UiSafety.isUiDead(this)) {
                    return;
                }
                new AlertDialog.Builder(this)
                        .setTitle("Help")
                        .setMessage(helpMessage)
                        .setPositiveButton("Close", (dialog, which) -> {
                        })
                        .show();
            });
            return true;
        }

        if (id == R.id.me_settings) {
            UiSafety.runOnUiThreadIfAlive(this, () -> {
                if (UiSafety.isUiDead(this)) {
                    return;
                }
                Intent i = new Intent(this, Settings.class);
                startActivity(i);
            });
            return true;
        }


        return super.onOptionsItemSelected(item);
    }



    public void createConfig(View v){
        if (UiSafety.isUiDead(this)) {
            return;
        }
        UiSafety.runOnUiThreadIfAlive(this, () -> {
            if (UiSafety.isUiDead(this)) {
                return;
            }
            Intent i = new Intent(this,addConfig.class);
            startActivityForResult(i,1);
        });
    }

    public void createScheduler(View v){
        if (UiSafety.isUiDead(this)) {
            return;
        }
        UiSafety.runOnUiThreadIfAlive(this, () -> {
            if (UiSafety.isUiDead(this)) {
                return;
            }
            Intent i = new Intent(this,addScheduler.class);
            startActivityForResult(i,2);
        });

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final Intent resultData = data;

        UiSafety.runOnUiThreadIfAlive(this, () -> {
            if (UiSafety.isUiDead(this)) {
                return;
            }

            if ((requestCode == 41 || requestCode == 42) && resultData != null) {
                try {
                    Uri pathUri = resultData.getData();
                    if (pathUri == null) {
                        return;
                    }
                    Uri dirUri = DocumentsContract.buildDocumentUriUsingTree(pathUri, DocumentsContract.getTreeDocumentId(pathUri));

                    DocumentFile pickedDir = DocumentFile.fromTreeUri(appContext, dirUri);

                    File selectedLogFile = log_file;
                    if (requestCode == 42) {
                        selectedLogFile = debug_log;
                    }
                    DocumentFile previousLog = pickedDir != null ? pickedDir.findFile(selectedLogFile.getName()) : null;
                    if (previousLog != null) {
                        previousLog.delete();
                    }

                    DocumentFile exportedLog = pickedDir != null ? pickedDir.createFile("log", selectedLogFile.getName()) : null;

                    try (FileInputStream inputStream = new FileInputStream(selectedLogFile);
                         OutputStream outputStream = exportedLog != null ? getContentResolver().openOutputStream(exportedLog.getUri()) : null) {
                        if (outputStream == null) {
                            return;
                        }
                        byte[] buffer = new byte[inputStream.available()];
                        int read = inputStream.read(buffer);
                        if (read > 0) {
                            outputStream.write(buffer, 0, read);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }

            ViewPager viewPager = findViewById(R.id.pager);
            if (viewPager == null) {
                return;
            }
            ViewPagerAdapter v = (ViewPagerAdapter) viewPager.getAdapter();
            if (v != null) {
                v.refresh_adapter();
            }

            viewPager.setAdapter(v);
            viewPager.setCurrentItem(requestCode);

            TabLayout tabLayout = findViewById(R.id.tabs);
            if (tabLayout != null) {
                tabLayout.setupWithViewPager(viewPager);
            }
        });
    }

    public static String getPath(@NonNull final Context context, @NonNull final Uri uri) {

        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                StringBuilder path;
                if ("primary".equalsIgnoreCase(type)) {
                    path = new StringBuilder(Environment.getExternalStorageDirectory().toString());

                    for (int i=0;i<split.length;i++){
                        if (i>0) path.append("/").append(split[i]);
                    }
                    //return Environment.getExternalStorageDirectory() + "/" + split[1];
                    return path.toString();
                }

                else {

                    path = new StringBuilder("/storage");
                    for (String aSplit : split) {
                        path.append("/").append(aSplit);
                    }
                    return path.toString();
                }

            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
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
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, Objects.requireNonNull(contentUri), selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private static String getDataColumn(Context context, @NonNull Uri uri, String selection,
                                        String[] selectionArgs) {

        final String column = "_data";
        final String[] projection = {
                column
        };

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        UiSafety.runOnUiThreadIfAlive(this, () -> {
            if (UiSafety.isUiDead(this)) {
                return;
            }
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(MainActivity.this, "Write External Storage permission allows to export logfile to your external storage. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("value", "Permission Granted, Now you can use local drive .");
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
        }
    }

    public void config_clickHandler(@NonNull View v){

        if (UiSafety.isUiDead(this)) {
            return;
        }

        if (v.getTag(R.id.bt_edit)!=null) {
            final int pos = (int) v.getTag(R.id.bt_edit);
            UiSafety.runOnUiThreadIfAlive(this, () -> {
                if (UiSafety.isUiDead(this)) {
                    return;
                }
                Intent intent = new Intent(this,editConfig.class);
                intent.putExtra("pos",pos);
                this.startActivityForResult(intent,1);
            });
        }
        else if (v.getTag(R.id.bt_delete)!=null) {
            final int pos = (int) v.getTag(R.id.bt_delete);

            UiSafety.runOnUiThreadIfAlive(this, () -> {
                if (UiSafety.isUiDead(this)) {
                    return;
                }
                new AlertDialog.Builder(this)
                        .setTitle("Delete Configuration")
                        .setMessage("This action will delete this configuration and all its schedulers. Are you sure?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            if (UiSafety.isUiDead(this)) {
                                return;
                            }
                            RS_Configuration deleted_config=configs.get(pos);
                            deleted_config.deleteFromDisk();
                            configs.remove(pos);

                            ArrayList<Scheduler> connected_schedulers_to_deleted_configuration=new ArrayList<>();
                            for (Scheduler s : schedulers){
                                if (deleted_config.id==s.config_id){
                                    connected_schedulers_to_deleted_configuration.add(s);
                                    s.deleteFromDisk();
                                }
                            }
                            schedulers.removeAll(connected_schedulers_to_deleted_configuration);
                            onActivityResult(1,RESULT_OK,null);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                        .show();
            });


        }


    }

    public void sched_clickHandler(@NonNull View v){

        if (UiSafety.isUiDead(this)) {
            return;
        }

        if (v.getTag(R.id.bt_edit_sched)!=null) {
            final int pos = (int) v.getTag(R.id.bt_edit_sched);
            UiSafety.runOnUiThreadIfAlive(this, () -> {
                if (UiSafety.isUiDead(this)) {
                    return;
                }
                Intent intent = new Intent(this,editSched.class);
                intent.putExtra("pos",pos);
                startActivityForResult(intent,2);
            });

        }
        else if (v.getTag(R.id.bt_delete_sched)!=null) {
            final int pos = (int) v.getTag(R.id.bt_delete_sched);

            UiSafety.runOnUiThreadIfAlive(this, () -> {
                if (UiSafety.isUiDead(this)) {
                    return;
                }
                new AlertDialog.Builder(this)
                        .setTitle("Delete Scheduler")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            if (UiSafety.isUiDead(this)) {
                                return;
                            }
                            schedulers.get(pos).deleteFromDisk();
                            schedulers.get(pos).cancelAlarm(appContext);
                            schedulers.remove(pos);
                            onActivityResult(2,RESULT_OK,null);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                        .show();
            });


        }

    }

    @Override
    protected void onDestroy() {
        final View decorView = getWindow() != null ? getWindow().getDecorView() : null;
        super.onDestroy();
        // The decor view is only safe to tear down once the Activity is fully destroyed.
        UiSafety.safelyTearDownViewTree(decorView);
    }


}
