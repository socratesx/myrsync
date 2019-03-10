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
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.os.EnvironmentCompat;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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

import static android.content.Context.MODE_PRIVATE;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;


public class RS_Configuration extends MainActivity implements Comparable<RS_Configuration>{

    String rs_ip, rs_user, rs_module, rs_options, local_path, path_uri, name, rs_mode, adv_options;
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
        prefseditor.putString("path_uri_"+String.valueOf(this.id),this.path_uri);
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
        prefseditor.remove("path_uri_"+String.valueOf(this.id));
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

        send_notification(context);
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
                    try {
                        SharedPreferences pref = context.getSharedPreferences("CMD_"+String.valueOf(id),MODE_PRIVATE);
                        SharedPreferences.Editor pref_Edit= pref.edit();
                        SharedPreferences sched_prefs = context.getSharedPreferences("schedulers", MODE_PRIVATE);
                        Locale current_locale = context.getResources().getConfiguration().locale;
                        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm",current_locale);

                        if (scheduler_id!=null) sched_prefs.edit().putBoolean("is_running_" + String.valueOf(scheduler_id), true).commit();


                        pref_Edit.putBoolean("is_running",true);
                        pref_Edit.commit();

                        try {
                            FileWriter debug_writer = new FileWriter(debug_log, true);
                            CharSequence message = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + "CONFIGURATION RUN "+name+ " STARTED";
                            debug_writer.append(message);
                            debug_writer.close();
                        }
                        catch (IOException e){
                            e.printStackTrace();
                        }

                        String rsync_bin= context.getSharedPreferences("Install",MODE_PRIVATE).getString("rsync_binary",".");
                        ProcessBuilder p = new ProcessBuilder();
                        Log.d("External Cache dir", context.getExternalCacheDirs()[0].getAbsolutePath());
                        String cmd_string="";
                        if (mode.equals("Push")) {
                            cmd_string = rsync_bin + " " + options + " --log-file " + log + " " + local_path + " " + cmd;
                            //p = new ProcessBuilder(rsync_bin,options,"--debug","ALL","--log-file",log,local_path,cmd);
                        }
                        else {
                            if ((PreferenceManager.getDefaultSharedPreferences(context).getBoolean("root_access",false))){
                                //p=new ProcessBuilder("su","-c",rsync_bin,options,"--log-file",log,"--debug","ALL",cmd,local_path);
                                cmd_string = "su -c "+ rsync_bin +" "+options+ " --log-file "+log+" "+cmd +" "+local_path+" ";

                            }
                            else {
                                //p=new ProcessBuilder(rsync_bin,options,"--log-file",log,"--debug","ALL",cmd,local_path);
                                cmd_string = rsync_bin +" "+options+ " --log-file "+log+" "+cmd +" "+local_path+" ";
                            }

                        }
                        Log.d("CMD_STRING",Arrays.toString(cmd_string.split(" ")));
                        p.command(cmd_string.split(" "));
                        

                        Map<String, String> env = p.environment();
                        env.put("PATH", "/su/bin:/sbin:/vendor/bin:/system/sbin:/system/bin:/su/xbin:/system/xbin");
                        //p.directory(new File(context.getApplicationInfo().dataDir));
                        p.redirectErrorStream(true);
                        Process process=p.start();

                        BufferedReader std_out = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        StringBuilder builder = new StringBuilder();
                        String line;

                        while ( (line = std_out.readLine()) != null) {
                            builder.append(line);
                            builder.append(System.getProperty("line.separator"));
                        }

                        String result = builder.toString();

                        try {
                            FileWriter debug_writer = new FileWriter(debug_log, true);
                            CharSequence message = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + "OUT_STREAM = "+result;
                            debug_writer.append(message);
                            debug_writer.close();
                        }
                        catch (IOException e){
                            e.printStackTrace();
                        }


                        if (result.equals("")) result="OK";
                        else result="Warning! Check Log!";

                        SharedPreferences prefs =  context.getSharedPreferences("configs", MODE_PRIVATE);
                        SharedPreferences.Editor prefseditor = prefs.edit();
                        prefseditor.putString("last_result_"+String.valueOf(id),result);


                        Calendar cal = Calendar.getInstance();
                        long time = cal.getTimeInMillis();

                        sched_prefs.edit().putLong("last_run_"+String.valueOf(scheduler_id),time).commit();

                        prefseditor.putString("last_run_"+String.valueOf(id),formatter.format(time));
                        prefseditor.commit();
                        pref_Edit.putBoolean("is_running",false);
                        pref_Edit.commit();
                        if (scheduler_id!=null) sched_prefs.edit().putBoolean("is_running_"+String.valueOf(scheduler_id),false).commit();
                        try {
                            FileWriter debug_writer = new FileWriter(debug_log, true);
                            CharSequence message = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + "CONFIGURATION RUN "+name+ " FINISHED";
                            debug_writer.append(message);
                            debug_writer.close();
                        }
                        catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                    catch (Exception e) {
                        try {
                            FileWriter debug_writer = new FileWriter(debug_log, true);
                            String message = "CONFIGURATION RUN EXCEPTION CAUGHT: \n" + e.getMessage();
                            debug_writer.append(message);
                            debug_writer.close();
                            e.printStackTrace();
                        }
                        catch (IOException ioe){
                            ioe.printStackTrace();
                        }

                    }
                }
            };

            t.start();



    }

    private void send_notification(Context ctx) {

        SharedPreferences set_prefs = getDefaultSharedPreferences(ctx);
        Locale current_locale = ctx.getResources().getConfiguration().locale;
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);

        String message = "Rsync configuration " + this.name + " started on " + formatter.format(Calendar.getInstance().getTime());

        Boolean Notifications_enabled = set_prefs.getBoolean("notifications", true);
        Boolean Vibration_enabled = set_prefs.getBoolean("vibrate", false);
        Uri ring_path = Uri.parse(set_prefs.getString("ringtone", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).getPath()));


        if (Notifications_enabled) {
            String new_id="";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) new_id= createNotificationChannel(ctx);

            Intent intent = new Intent(ctx, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent, 0);

            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder not = new NotificationCompat.Builder(ctx, new_id);

            not.setContentTitle("myRSync Job Started")
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

