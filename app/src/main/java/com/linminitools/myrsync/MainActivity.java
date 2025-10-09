package com.linminitools.myrsync;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static com.linminitools.myrsync.myRsyncApplication.schedulers;
import static com.linminitools.myrsync.myRsyncApplication.configs;

public class MainActivity extends AppCompatActivity {
    public static final String SELECTED_TAB = "selected_tab";

    private static final int PERMISSION_REQUEST_CODE = 1;
    public static Context appContext;
    static File log_file;
    static File debug_log;

    private static final String DEFAULT_RSYNC_ASSET = "rsync_binary/x86_64/rsync";
    private static final Map<String, String> RSYNC_ASSET_PATHS;

    static {
        Map<String, String> assetPaths = new HashMap<>();
        assetPaths.put("armeabi-v7a", "rsync_binary/armv7/rsync");
        assetPaths.put("armeabi", "rsync_binary/armv7/rsync");
        assetPaths.put("armv7", "rsync_binary/armv7/rsync");
        assetPaths.put("x86", DEFAULT_RSYNC_ASSET);
        assetPaths.put("x86_64", DEFAULT_RSYNC_ASSET);
        RSYNC_ASSET_PATHS = Collections.unmodifiableMap(assetPaths);
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appContext = getApplicationContext();

        if (Build.VERSION.SDK_INT >= 23)
            if (!checkPermission()) requestPermission(); // Code for permission

        Locale current_locale = appContext.getResources().getConfiguration().locale;
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);

        String log_path = appContext.getApplicationInfo().dataDir + "/logfile.log";
        log_file = new File(log_path);

        String Debug_log_path = appContext.getApplicationInfo().dataDir + "/debug.log";
        debug_log = new File(Debug_log_path);

        ensureLogFileExists(log_file, "MainActivity", "Unable to create rsync log file");
        ensureLogFileExists(debug_log, "MainActivity", "Unable to create debug log file");


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

    static boolean ensureRsyncExecutable(@NonNull Context context, boolean forceCopy, SimpleDateFormat formatter) {
        Context appContext = context.getApplicationContext();

        if (log_file == null) {
            log_file = new File(appContext.getApplicationInfo().dataDir + "/logfile.log");
        }
        if (debug_log == null) {
            debug_log = new File(appContext.getApplicationInfo().dataDir + "/debug.log");
        }

        ensureLogFileExists(log_file, "MainActivity", "Unable to create rsync log file");
        ensureLogFileExists(debug_log, "MainActivity", "Unable to create debug log file");

        String appFileDirectory = appContext.getFilesDir().getPath();
        File rsyncExecutable = new File(appFileDirectory, "rsync");

        File parentDirectory = rsyncExecutable.getParentFile();
        if (parentDirectory == null) {
            appendDebugLog(formatter, "UNABLE TO RESOLVE DIRECTORY FOR rsync EXECUTABLE");
            return false;
        }
        if (!parentDirectory.exists() && !parentDirectory.mkdirs()) {
            appendDebugLog(formatter, "FAILED TO CREATE DIRECTORY " + parentDirectory.getAbsolutePath());
            return false;
        }
        if (!parentDirectory.canWrite()) {
            appendDebugLog(formatter, "DIRECTORY " + parentDirectory.getAbsolutePath() + " IS NOT WRITABLE");
            return false;
        }

        boolean shouldCopy = forceCopy || !rsyncExecutable.exists();
        if (!shouldCopy && !rsyncExecutable.canExecute()) {
            shouldCopy = true;
        }

        if (shouldCopy) {
            if (rsyncExecutable.exists() && !rsyncExecutable.delete()) {
                Log.w("MainActivity", "Unable to delete existing rsync binary before refresh.");
            }

            String binPath = resolveRsyncBinaryAsset();

            Log.d("BINARY", binPath);
            appendDebugLog(formatter, "SELECTED RSYNC BINARY " + binPath + " FOR ABIS " + Arrays.toString(Build.SUPPORTED_ABIS));

            try (InputStream in = appContext.getAssets().open(binPath, AssetManager.ACCESS_BUFFER);
                 FileOutputStream fos = new FileOutputStream(rsyncExecutable)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }

                appendDebugLog(formatter, "RSYNC EXECUTABLE COPIED {\nSUPPORTED ABIS: " + Arrays.toString(Build.SUPPORTED_ABIS)
                        + "\nSDK_Version: " + Build.VERSION.SDK_INT + "\n}");
            } catch (IOException e) {
                appendDebugLog(formatter, "EXCEPTION CAUGHT: \n" + (e.getMessage() == null ? e.toString() : e.getMessage()));
                return false;
            }
        }

        if (!ensureExecutablePermissions(rsyncExecutable, formatter)) {
            return false;
        }

        appContext.getSharedPreferences("Install", MODE_PRIVATE).edit().putString("rsync_binary", rsyncExecutable.getAbsolutePath()).apply();
        return true;
    }

    private static boolean ensureExecutablePermissions(File executable, SimpleDateFormat formatter) {
        boolean readable = executable.setReadable(true, false);
        boolean executableSet = executable.setExecutable(true, false);
        boolean writable = executable.setWritable(true, true);

        if (!readable || !executableSet || !writable) {
            appendDebugLog(formatter, "EXECUTABLE PERMISSION PRIMING FAILED FOR rsync");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Os.chmod(executable.getAbsolutePath(), 0755);
            } catch (ErrnoException e) {
                Log.w("MainActivity", "Unable to chmod rsync binary", e);
                appendDebugLog(formatter, "EXCEPTION CAUGHT: \n" + (e.getMessage() == null ? e.toString() : e.getMessage()));
            }
        }

        if (!executable.canExecute()) {
            try {
                Process process = new ProcessBuilder("/system/bin/chmod", "755", executable.getAbsolutePath()).start();
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    appendDebugLog(formatter, "EXECUTABLE PERMISSION CHMOD FALLBACK FAILED WITH CODE " + exitCode);
                }
            } catch (Exception e) {
                Log.w("MainActivity", "chmod fallback failed", e);
                appendDebugLog(formatter, "EXCEPTION CAUGHT: \n" + (e.getMessage() == null ? e.toString() : e.getMessage()));
            }
        }

        if (!executable.canExecute()) {
            appendDebugLog(formatter, "EXECUTABLE PERMISSION CHECK FAILED FOR rsync");
            return false;
        }

        return true;
    }

    @NonNull
    private static String resolveRsyncBinaryAsset() {
        for (String abi : Build.SUPPORTED_ABIS) {
            String normalizedAbi = abi;
            if ("arm64-v8a".equals(abi)) {
                normalizedAbi = "armeabi-v7a";
            }

            String path = RSYNC_ASSET_PATHS.get(normalizedAbi);
            if (path != null) {
                return path;
            }
        }

        return DEFAULT_RSYNC_ASSET;
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.app_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
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
                int verCode = pInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            s = SpannableString.valueOf(s+"\n\n myRsync Version: Beta-"+version);

            Linkify.addLinks(s,Linkify.ALL);
            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(this)
                            .setTitle("About")
                            .setMessage(s)
                            .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            ;
            AlertDialog alertDialog = alertDialogBuilder.show();
            TextView textView = alertDialog.findViewById(android.R.id.message);

            textView.setTextSize(12);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }

        if (id == R.id.me_help) {
            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(this)
                            .setTitle("Help")
                            .setMessage(getResources().getString(R.string.help))
                            .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                    ;
            alertDialogBuilder.show();
        }

        if (id == R.id.me_settings) {

            Intent i = new Intent(this,Settings.class);

            startActivity(i);

        }


        return super.onOptionsItemSelected(item);
    }



    public void createConfig(View v){

        Intent i = new Intent(this,addConfig.class);
        startActivityForResult(i,1);

        }

    public void createScheduler(View v){

        Intent i = new Intent(this,addScheduler.class);
        startActivityForResult(i,2);

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == 41 || requestCode==42) && data != null) {

            try {

                Uri pathUri = data.getData();
                if (pathUri == null) {
                    return;
                }
                Uri dirUri = DocumentsContract.buildDocumentUriUsingTree(pathUri, DocumentsContract.getTreeDocumentId(pathUri));

                DocumentFile pickedDir= DocumentFile.fromTreeUri(appContext,dirUri);

                File selected_logfile = log_file;
                if (requestCode==42) selected_logfile = debug_log;
                DocumentFile previous_log = Objects.requireNonNull(pickedDir).findFile(selected_logfile.getName());
                if (previous_log != null) previous_log.delete();

                DocumentFile exported_log = pickedDir.createFile("log", selected_logfile.getName());

                FileInputStream inputStream = new FileInputStream(selected_logfile);
                OutputStream outputStream = getContentResolver().openOutputStream(Objects.requireNonNull(exported_log).getUri());

                byte[] buffer = new byte[inputStream.available()];
                //noinspection ResultOfMethodCallIgnored
                inputStream.read(buffer);
                inputStream.close();

                Objects.requireNonNull(outputStream).write(buffer);
                outputStream.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {

            ViewPager viewPager = findViewById(R.id.pager);
            ViewPagerAdapter v = (ViewPagerAdapter) viewPager.getAdapter();
            Objects.requireNonNull(v).refresh_adapter();


            viewPager.setAdapter(v);
            viewPager.setCurrentItem(requestCode);

            TabLayout tabLayout = findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(viewPager);

        }
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

        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(MainActivity.this, "Write External Storage permission allows to export logfile to your external storage. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
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

        if (v.getTag(R.id.bt_edit)!=null) {
            int pos = (int) v.getTag(R.id.bt_edit);
            Intent intent = new Intent(this,editConfig.class);
            intent.putExtra("pos",pos);
            this.startActivityForResult(intent,1);
        }
        else if (v.getTag(R.id.bt_delete)!=null) {
            final int pos = (int) v.getTag(R.id.bt_delete);

            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(this)
                            .setTitle("Delete Configuration")
                            .setMessage("This action will delete this configuration and all its schedulers. Are you sure?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
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
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(@NonNull DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
            alertDialogBuilder.show();


        }


    }

    public void sched_clickHandler(@NonNull View v){

        if (v.getTag(R.id.bt_edit_sched)!=null) {
            int pos = (int) v.getTag(R.id.bt_edit_sched);
            Intent intent = new Intent(this,editSched.class);
            intent.putExtra("pos",pos);
            startActivityForResult(intent,2);

        }
        else if (v.getTag(R.id.bt_delete_sched)!=null) {
            final int pos = (int) v.getTag(R.id.bt_delete_sched);

            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(this)
                            .setTitle("Delete Scheduler")
                            .setMessage("Are you sure?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    schedulers.get(pos).deleteFromDisk();
                                    schedulers.get(pos).cancelAlarm(appContext);
                                    schedulers.remove(pos);
                                    onActivityResult(2,RESULT_OK,null);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(@NonNull DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
            alertDialogBuilder.show();


        }

    }


}
