package com.linminitools.mysync;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.AlreadyBoundException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentManager;
import android.support.design.widget.TabLayout;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    public static ArrayList<RS_Configuration> configs = new ArrayList<RS_Configuration>();
    public static ArrayList<Scheduler> schedulers = new ArrayList<>();
    public static Context appContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appContext=getApplicationContext();
        configs.clear();
        schedulers.clear();

        SharedPreferences config_prefs = appContext.getSharedPreferences("configs", MODE_PRIVATE);
        SharedPreferences sched_prefs = appContext.getSharedPreferences("schedulers", MODE_PRIVATE);

        for(int i=1; i<100;i++){
            if (config_prefs.getString("rs_ip_"+String.valueOf(i),"").isEmpty()){
                break;
            }
            else {
                RS_Configuration config = new RS_Configuration(i);
                config.rs_user=config_prefs.getString("rs_user_"+String.valueOf(i),"");
                config.rs_ip = config_prefs.getString("rs_ip_"+String.valueOf(i), "");
                config.rs_port = config_prefs.getString("rs_port_"+String.valueOf(i), "");
                config.rs_options = config_prefs.getString("rs_options_"+String.valueOf(i),"");
                config.rs_module = config_prefs.getString("rs_module_"+String.valueOf(i), "");
                config.local_path = config_prefs.getString("local_path_"+String.valueOf(i), "");
                configs.add(config);
            }
        }
        for(int i=1; i<100;i++) {
            if (sched_prefs.getInt("id", -1)<0) {
                break;
            } else {
                TimePicker tp=new TimePicker(this);
                tp.setCurrentHour(sched_prefs.getInt("hour",0));
                tp.setCurrentMinute(sched_prefs.getInt("min",0));
                ArrayList<String> days = (ArrayList<String>) sched_prefs.getStringSet("days",null);

                Scheduler sched = new Scheduler(days,tp,i);
                schedulers.add(sched);
            }
        }

        if (Build.VERSION.SDK_INT >= 23)
        {
            if (checkPermission())
            {
                ViewPager viewPager = findViewById(R.id.pager);
                ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

                // Add Fragments to adapter one by one

                adapter.addFragment(new tab1(), "About");
                adapter.addFragment(new tab3(), "Configurations");
                adapter.addFragment(new tab2(), "Schedulers");


                viewPager.setAdapter(adapter);

                TabLayout tabLayout =  findViewById(R.id.tabs);
                tabLayout.setupWithViewPager(viewPager);
            } else {
                requestPermission(); // Code for permission
                ViewPager viewPager = findViewById(R.id.pager);
                ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());


                // Add Fragments to adapter one by one
                adapter.addFragment(new tab1(), "About");
                adapter.addFragment(new tab3(), "Configurations");
                adapter.addFragment(new tab2(), "Schedulers");

                viewPager.setAdapter(adapter);

                TabLayout tabLayout =  findViewById(R.id.tabs);
                tabLayout.setupWithViewPager(viewPager);
            }
        }
        else
            {

            ViewPager viewPager = findViewById(R.id.pager);
            ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

            // Add Fragments to adapter one by one
            adapter.addFragment(new tab1(), "About");
            adapter.addFragment(new tab3(), "Configurations");
            adapter.addFragment(new tab2(), "Schedulers");

            viewPager.setAdapter(adapter);

            TabLayout tabLayout =  findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(viewPager);


            }

    }


    public void createConfig(View v){

        Intent i = new Intent(this,addConfig.class);
        startActivityForResult(i,1);

        }

    public void createScheduler(View v){

        Intent i = new Intent(this,addScheduler.class);
        startActivityForResult(i,2);

    }



    protected void onActivityResult(int requestCode, int resultCode, Intent data){




        ViewPager viewPager = findViewById(R.id.pager);

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        // Add Fragments to adapter one by one
        adapter.addFragment(new tab1(), "About");
        adapter.addFragment(new tab3(), "Configurations");
        adapter.addFragment(new tab2(), "Schedulers");
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(requestCode);
        TabLayout tabLayout =  findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);


    }

    //@TargetApi(Build.VERSION_CODES.O)



    // Adapter for the viewpager using FragmentPagerAdapter
    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new java.util.ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                String path="";
                if ("primary".equalsIgnoreCase(type)) {
                    path=Environment.getExternalStorageDirectory().toString();

                    for (int i=0;i<split.length;i++){
                        if (i>0) path=path+"/"+split[i];
                    }
                    //return Environment.getExternalStorageDirectory() + "/" + split[1];
                    return path;
                }

                else {

                    path="/storage";
                    for (int i=0;i<split.length;i++) {
                        path = path + "/" + split[i];
                    }
                    return path;
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
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
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

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(MainActivity.this, "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
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


    public void config_clickHandler(View v){

        if (v.getTag(R.id.bt_edit)!=null) {
            int pos = (int) v.getTag(R.id.bt_edit);
            Intent intent = new Intent(this,editConfig.class);
            intent.putExtra("pos",pos);
            startActivity(intent);
        }
        else if (v.getTag(R.id.bt_delete)!=null) {
            final int pos = (int) v.getTag(R.id.bt_delete);

            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(this)
                            .setTitle("Delete Configuration")
                            .setMessage("Are you sure?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    configs.get(pos).deleteFromDisk();
                                    configs.remove(pos);
                                    onActivityResult(1,RESULT_OK,null);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog alertDialog = alertDialogBuilder.show();


        }


    }

    public void sched_clickHandler(View v){

        if (v.getTag(R.id.bt_edit)!=null) {
            int pos = (int) v.getTag(R.id.bt_edit);
            Intent intent = new Intent(this,editConfig.class);
            intent.putExtra("pos",pos);
            startActivity(intent);
        }
        else if (v.getTag(R.id.bt_delete)!=null) {
            final int pos = (int) v.getTag(R.id.bt_delete);

            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(this)
                            .setTitle("Delete Configuration")
                            .setMessage("Are you sure?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    configs.get(pos).deleteFromDisk();
                                    configs.remove(pos);
                                    onActivityResult(1,RESULT_OK,null);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog alertDialog = alertDialogBuilder.show();


        }


    }





}
