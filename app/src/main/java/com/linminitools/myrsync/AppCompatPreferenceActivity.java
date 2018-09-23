package com.linminitools.myrsync;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.PrintWriter;
import java.util.prefs.Preferences;

/**
 * A {@link android.preference.PreferenceActivity} which implements and proxies the necessary calls
 * to be used with AppCompat.
 */
public abstract class AppCompatPreferenceActivity extends PreferenceActivity {

    private AppCompatDelegate mDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    @NonNull
    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();

    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);

    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Log.d("ONDESTROY",String.valueOf(settings.getBoolean("clear_log",false)));
        if (settings.getBoolean("clear_log",false)){

            SharedPreferences prefs = getSharedPreferences("Rsync_Command_build", MODE_PRIVATE);
            String log = prefs.getString("log", getApplicationInfo().dataDir + "/logfile.log");
            File log_file = new File(log);
            try{
                PrintWriter writer = new PrintWriter(log_file);
                writer.print("cleared");
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }
}
