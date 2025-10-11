package com.linminitools.myrsync;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.ExternalStorageStats;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.os.EnvironmentCompat;
import androidx.documentfile.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.StringTokenizer;
import com.linminitools.myrsync.rsync.RsyncBridge;
import com.linminitools.myrsync.rsync.RsyncResult;

import static android.content.Context.MODE_PRIVATE;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;


public class RS_Configuration extends MainActivity implements Comparable<RS_Configuration>{

    String rs_ip, rs_user, rs_module, rs_options, local_path, name, rs_mode, adv_options;
    Long addedOn;
    String rs_port="873";
    final int id;


    public RS_Configuration(){
        this.id=0;
    }


    RS_Configuration(int id){
        this.id=id;
        this.name="Config "+String.valueOf(id);
    }
    
    void saveToDisk(){
        
        SharedPreferences prefs = appContext.getSharedPreferences("configs", MODE_PRIVATE);
        SharedPreferences.Editor prefseditor = prefs.edit();
        this.addedOn= Calendar.getInstance().getTimeInMillis();

        prefseditor.putInt("rs_id_"+String.valueOf(this.id),this.id);
        prefseditor.putString("rs_name_"+String.valueOf(this.id),name);
        prefseditor.putString("rs_options_"+String.valueOf(this.id),rs_options);
        prefseditor.putString("rs_user_"+String.valueOf(this.id),rs_user);
        prefseditor.putString("rs_module_"+String.valueOf(this.id),rs_module);
        prefseditor.putString("rs_ip_"+String.valueOf(this.id),rs_ip);
        prefseditor.putString("rs_port_"+String.valueOf(this.id),rs_port);
        prefseditor.putString("rs_mode_"+String.valueOf(this.id),rs_mode);
        prefseditor.putString("local_path_"+String.valueOf(this.id),local_path);
        prefseditor.putString("last_result_"+String.valueOf(this.id),"Never Run");
        prefseditor.putString("last_run_"+String.valueOf(this.id),"Never Run");
        //prefseditor.putString("path_uri_"+String.valueOf(this.id),this.path_uri);
        prefseditor.putString("rs_adv_options_"+String.valueOf(this.id),this.adv_options);
        prefseditor.putLong("rs_addedon_"+String.valueOf(this.id),addedOn);

        prefseditor.apply();
        Locale current_locale = appContext.getResources().getConfiguration().locale;
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);
        try {
            FileWriter debug_writer = new FileWriter(debug_log, true);
            CharSequence message = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + "CONFIGURATION SAVED: " +this.name;
            debug_writer.append(message);
            debug_writer.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }

        
    }
    

    @SuppressWarnings("ResultOfMethodCallIgnored")
    void deleteFromDisk(){
        SharedPreferences prefs = appContext.getSharedPreferences("configs", MODE_PRIVATE);
        SharedPreferences.Editor prefseditor = prefs.edit();

        prefseditor.remove("rs_name_"+String.valueOf(this.id));
        prefseditor.remove("rs_id_"+String.valueOf(this.id));
        prefseditor.remove("rs_options_"+String.valueOf(this.id));
        prefseditor.remove("rs_user_"+String.valueOf(this.id));
        prefseditor.remove("rs_module_"+String.valueOf(this.id));
        prefseditor.remove("rs_ip_"+String.valueOf(this.id));
        prefseditor.remove("rs_port_"+String.valueOf(this.id));
        prefseditor.remove("rs_mode_"+String.valueOf(this.id));
        prefseditor.remove("local_path_"+String.valueOf(this.id));
        prefseditor.remove("last_result_"+String.valueOf(this.id));
        prefseditor.remove("last_run_"+String.valueOf(this.id));
        prefseditor.remove("rs_addedon_"+String.valueOf(this.id));
        //prefseditor.remove("path_uri_"+String.valueOf(this.id));
        prefseditor.remove("rs_adv_options_"+String.valueOf(this.id));
        prefseditor.apply();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appContext.deleteSharedPreferences("CMD_"+String.valueOf(id));
        else (new File(appContext.getApplicationInfo().dataDir+"/shared_prefs/CMD_"+String.valueOf(id)+".xml")).delete();

        Locale current_locale = appContext.getResources().getConfiguration().locale;
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);
        try {
            FileWriter debug_writer = new FileWriter(debug_log, true);
            CharSequence message = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + "CONFIGURATION DELETED: " +this.name;
            debug_writer.append(message);
            debug_writer.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

    public int compareTo(@NonNull RS_Configuration c){
        return Long.compare(this.addedOn,c.addedOn);
    }

    void executeConfig(final Context context, final Integer scheduler_id){

        SharedPreferences sched_prefs = context.getSharedPreferences("schedulers", MODE_PRIVATE);
        boolean sw_wifi = sched_prefs.getBoolean("wifi_switch_"+ String.valueOf(scheduler_id),false);
        String wifi_ssid = sched_prefs.getString("ssid_"+ String.valueOf(scheduler_id),"");
        int sched;
        String ssid = get_current_ssid(context);
        if (scheduler_id == null) sched =-1;
        else sched = scheduler_id;
        //Log.d("WIFI_SSID",ssid);
        //Log.d("WIFI_SET",wifi_ssid);
        //Log.d("WIFI_SW",String.valueOf(sw_wifi));

        if (ssid == null) {
            send_notification(context,0);
            update_result(context,"Aborted",sched);
        }
        else {
            if (sw_wifi && ssid.equals("none")){
                send_notification(context,1);
                update_result(context,"Aborted",sched);
            }
            else if (sw_wifi && !ssid.equals(wifi_ssid) && !wifi_ssid.isEmpty()){
                send_notification(context,2);
                update_result(context,"Aborted",sched);
            }
            else {
                send_notification(context,3);
                final SharedPreferences prefs =  context.getSharedPreferences("Rsync_Command_build", MODE_PRIVATE);
                final SharedPreferences prefs2 =  context.getSharedPreferences("configs", MODE_PRIVATE);
                final int id = this.id;

                final String options = this.rs_options;
                final String log = prefs.getString("log",context.getApplicationInfo().dataDir+"/logfile.log");
                final String local_path=this.local_path;

                String rsync_user_string=this.rs_user+"@";
                if(this.rs_user.isEmpty()) rsync_user_string="";

                final String cmd = "rsync://"+rsync_user_string+this.rs_ip+":"+this.rs_port+"/"+this.rs_module;
                String Debug_log_path = context.getApplicationInfo().dataDir + "/debug.log";
                debug_log = new File(Debug_log_path);
                final String mode= this.rs_mode;

                //File selected_dir = new File(this.local_path);
                //selected_dir.setWritable(true);


                final Uri dirUri= Uri.parse(prefs2.getString("path_uri_"+this.id,""));
                final File f = new File(local_path);
                final DocumentFile df = DocumentFile.fromFile(f);


                File[] ext_dir = context.getExternalFilesDirs(Environment.DIRECTORY_DOCUMENTS);

                Log.d( "EXTERNAL_SD_STATE",Arrays.toString(ext_dir));


                Thread t = new Thread(){
                    @SuppressLint("ApplySharedPref")
                    @Override
                    public void run() {
                        Locale current_locale = context.getResources().getConfiguration().locale;
                        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm",current_locale);

                        SharedPreferences pref = context.getSharedPreferences("CMD_"+String.valueOf(id),MODE_PRIVATE);
                        SharedPreferences.Editor pref_Edit= pref.edit();
                        SharedPreferences sched_prefs = context.getSharedPreferences("schedulers", MODE_PRIVATE);
                        try {

                            if (scheduler_id!=null) sched_prefs.edit().putBoolean("is_running_" + String.valueOf(scheduler_id), true).commit();


                            pref_Edit.putBoolean("is_running",true);
                            pref_Edit.commit();

                            appendDebugLog(formatter, "CONFIGURATION RUN " + name + " STARTED");
                            appendRsyncLog(formatter, "CONFIGURATION RUN " + name + " STARTED");
                            if (!MainActivity.ensureRsyncExecutable(context, false, formatter)) {
                                String message = "CONFIGURATION RUN " + name + " FAILED: unable to prepare rsync executable";
                                appendDebugLog(formatter, message);
                                appendRsyncLog(formatter, message);
                                if (pref_Edit != null) {
                                    pref_Edit.putBoolean("is_running", false);
                                    pref_Edit.commit();
                                }
                                if (scheduler_id != null) {
                                    sched_prefs.edit().putBoolean("is_running_" + String.valueOf(scheduler_id), false).commit();
                                }
                                int schedulerKey = scheduler_id == null ? -1 : scheduler_id;
                                update_result(context, "Failed to prepare rsync executable", schedulerKey);
                                return;
                            }
                            Log.d("External Cache dir", context.getExternalCacheDirs()[0].getAbsolutePath());
                            String commandLine;
                            if (mode.equals("Push")) {
                                commandLine = "rsync " + options + " --log-file " + log + " " + local_path + " " + cmd;
                            } else {
                                boolean wantsRoot = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("root_access", false);
                                if (wantsRoot) {
                                    String rootWarning = "ROOT EXECUTION REQUESTED BUT NOT SUPPORTED IN SHARED LIBRARY MODE";
                                    MainActivity.appendDebugLog(formatter, rootWarning);
                                    MainActivity.appendRsyncLog(formatter, rootWarning);
                                }
                                commandLine = "rsync " + options + " --log-file " + log + " " + cmd + " " + local_path;
                            }

                            MainActivity.appendDebugLog(formatter, "RSYNC COMMAND LINE: " + commandLine);
                            String[] argv = tokenizeCommand(commandLine);

                            RsyncResult rsyncResult = RsyncBridge.runRsync(argv);
                            String stdout = rsyncResult.getStdout();
                            String stderr = rsyncResult.getStderr();

                            if (!stdout.isEmpty()) {
                                MainActivity.appendDebugLog(formatter, "RSYNC STDOUT =\n" + stdout);
                                MainActivity.appendRsyncLog(formatter, "RSYNC STDOUT\n" + stdout);
                            }
                            if (!stderr.isEmpty()) {
                                MainActivity.appendDebugLog(formatter, "RSYNC STDERR =\n" + stderr);
                                MainActivity.appendRsyncLog(formatter, "RSYNC STDERR\n" + stderr);
                            }

                            MainActivity.appendDebugLog(formatter, "RSYNC EXIT CODE = " + rsyncResult.getExitCode());

                            String result;
                            if (rsyncResult.getExitCode() == 0) {
                                result = "OK";
                            } else {
                                result = "Warning! Check Log!";
                            }


                            if (scheduler_id!=null) {
                                sched_prefs.edit().putBoolean("is_running_"+String.valueOf(scheduler_id),false).commit();
                                update_result(context,result,scheduler_id);
                            }
                            MainActivity.appendDebugLog(formatter, "CONFIGURATION RUN " + name + " FINISHED");
                            MainActivity.appendRsyncLog(formatter, "CONFIGURATION RUN " + name + " FINISHED WITH RESULT: " + result);
                        }
                        catch (Exception e) {
                            String message = "CONFIGURATION RUN EXCEPTION CAUGHT: \n" + (e.getMessage() == null ? e.toString() : e.getMessage());
                            MainActivity.appendDebugLog(formatter, message);
                            MainActivity.appendRsyncLog(formatter, message);
                            Log.e("RS_Configuration", "Unable to execute rsync", e);
                            if (pref_Edit != null) {
                                pref_Edit.putBoolean("is_running", false);
                                pref_Edit.commit();
                            }
                            if (scheduler_id != null && sched_prefs != null) {
                                sched_prefs.edit().putBoolean("is_running_" + String.valueOf(scheduler_id), false).commit();
                                update_result(context, "Failed", scheduler_id);
                            } else {
                                update_result(context, "Failed", -1);
                            }
                        }
                    }
                };

                t.start();

            }
        }
    }

    @NonNull
    private static String[] tokenizeCommand(@NonNull String command) {
        StringTokenizer tokenizer = new StringTokenizer(command);
        String[] tokens = new String[tokenizer.countTokens()];
        int index = 0;
        while (tokenizer.hasMoreTokens()) {
            tokens[index++] = tokenizer.nextToken();
        }
        return tokens;
    }

    private void update_result(Context context, String result, int scheduler_id){

        Calendar cal = Calendar.getInstance();
        long time = cal.getTimeInMillis();

        SharedPreferences cmdpref = context.getSharedPreferences("CMD_"+String.valueOf(id),MODE_PRIVATE);
        SharedPreferences.Editor cmd_pref_Edit= cmdpref.edit();
        SharedPreferences config_prefs =  context.getSharedPreferences("configs", MODE_PRIVATE);
        SharedPreferences.Editor configs_prefseditor = config_prefs.edit();
        SharedPreferences sched_prefs = context.getSharedPreferences("schedulers", MODE_PRIVATE);

        Locale current_locale = context.getResources().getConfiguration().locale;
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm",current_locale);

        cmd_pref_Edit.putBoolean("is_running",false);
        cmd_pref_Edit.commit();

        configs_prefseditor.putString("last_result_"+String.valueOf(this.id),result);
        if (scheduler_id >= 0) sched_prefs.edit().putLong("last_run_"+String.valueOf(scheduler_id),time).commit();
        configs_prefseditor.putString("last_run_"+String.valueOf(id),formatter.format(time));
        configs_prefseditor.commit();

    }

    private String get_current_ssid(Context context){

        String ssid = "";
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null) {
            return null;
        }

        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            Log.d("WIFI_INFO",connectionInfo.getSupplicantState().toString());
            if (connectionInfo.getSupplicantState().toString().equals("COMPLETED") && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                ssid = connectionInfo.getSSID();
            }
            else return "none";
        }

        return ssid.replaceAll("^\"|\"$", "");

    }

    private void send_notification(Context ctx, int type) {

        SharedPreferences set_prefs = getDefaultSharedPreferences(ctx);
        Locale current_locale = ctx.getResources().getConfiguration().locale;
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);

        String message = "Rsync configuration " + this.name + " started on " + formatter.format(Calendar.getInstance().getTime());
        if (type==0) message = "Network is not connected. The job is aborted";
        else if (type==1) message = "You are not connected to WiFi. The job is aborted.";
        else if (type==2) message = "You are not connected to the network you specified. The job is aborted.";

        boolean Notifications_enabled = set_prefs.getBoolean("notifications", true);
        boolean Vibration_enabled = set_prefs.getBoolean("vibrate", false);
        Uri ring_path = Uri.parse(set_prefs.getString("ringtone", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).getPath()));


        if (Notifications_enabled) {
            String new_id="";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) new_id= createNotificationChannel(ctx);

            Intent intent = new Intent(ctx, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent, pendingIntentFlags);

            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder not = new NotificationCompat.Builder(ctx, new_id);

            not.setContentTitle("myRSync Job Status")
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setContentText(message)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setSound(ring_path)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);
            if (Vibration_enabled) not.setVibrate(new long[]{300, 600});

            Objects.requireNonNull(nm).notify(0, not.build());

        }
    }

        @TargetApi(Build.VERSION_CODES.O)
        private String createNotificationChannel(Context ctx){

            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            SharedPreferences set_prefs = getDefaultSharedPreferences(ctx);

            Boolean Vibration_enabled = set_prefs.getBoolean("vibrate",false);
            Uri ring_path=Uri.parse(set_prefs.getString("ringtone", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).getPath()));


            if (nm != null) {
                for (NotificationChannel nc : nm.getNotificationChannels())
                    nm.deleteNotificationChannel(nc.getId());


                String new_id = String.valueOf(new Random().nextLong());


                AudioAttributes aa = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION).build();

                NotificationChannel channel = new NotificationChannel(new_id, "Notification Channel", NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("Notify Job Start");
                channel.setSound(ring_path, aa);
                channel.enableLights(true);
                channel.setLightColor(Color.WHITE);

                if (Vibration_enabled) {
                    channel.enableVibration(true);
                    channel.setVibrationPattern(new long[]{300, 600});
                } else {
                    channel.enableVibration(false);
                }

                nm.createNotificationChannel(channel);

                return new_id;
            }
            return "";
        }

    }
