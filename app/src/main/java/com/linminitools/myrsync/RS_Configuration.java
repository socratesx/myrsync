package com.linminitools.myrsync;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import androidx.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;


public class RS_Configuration implements Comparable<RS_Configuration>{

    /** SSH auth: private key file (default). */
    public static final String SSH_AUTH_KEY = "key";
    /** SSH auth: password via Dropbear DROPBEAR_PASSWORD. */
    public static final String SSH_AUTH_PASSWORD = "password";

    String rs_ip, rs_port, rs_user, rs_module, rs_options, local_path, name, rs_mode, adv_options;
    String rs_transport_type, rs_ssh_private_key_path, rs_ssh_remote_path;
    String rs_ssh_auth_type = SSH_AUTH_KEY;
    String rs_ssh_password = "";

    Long addedOn;
    final int id;

    boolean isSshPasswordAuth() {
        return "SSH".equals(rs_transport_type) && SSH_AUTH_PASSWORD.equals(rs_ssh_auth_type);
    }


    public RS_Configuration(){
        this.id=0;
    }


    RS_Configuration(int id){
        this.id=id;
        this.name="Config "+String.valueOf(id);
    }

    void saveToDisk(){
        
        SharedPreferences prefs = myRsyncApplication.appContext.getSharedPreferences("configs", Context.MODE_PRIVATE);
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
        // Only initialize run history for a brand-new configuration; edits / schedule changes must keep it.
        if (!prefs.contains("rs_id_" + String.valueOf(this.id))) {
            prefseditor.putString("last_result_" + String.valueOf(this.id), "Never Run");
            prefseditor.putString("last_run_" + String.valueOf(this.id), "Never Run");
        }
        prefseditor.putString("rs_adv_options_"+String.valueOf(this.id),this.adv_options);
        prefseditor.putLong("rs_addedon_"+String.valueOf(this.id),addedOn);
        prefseditor.putString("rs_transport_type_"+String.valueOf(this.id), rs_transport_type);
        prefseditor.putString("rs_ssh_private_key_path_"+String.valueOf(this.id), rs_ssh_private_key_path);
        prefseditor.putString("rs_ssh_remote_path_"+String.valueOf(this.id), rs_ssh_remote_path);
        prefseditor.putString("rs_ssh_auth_type_"+String.valueOf(this.id),
                rs_ssh_auth_type != null ? rs_ssh_auth_type : SSH_AUTH_KEY);
        // Never persist SSH passwords in plaintext configs.xml.
        prefseditor.remove("rs_ssh_password_"+String.valueOf(this.id));

        prefseditor.apply();

        if (isSshPasswordAuth() && rs_ssh_password != null && !rs_ssh_password.isEmpty()) {
            SshSecretStore.putPassword(myRsyncApplication.appContext, this.id, rs_ssh_password);
        } else {
            SshSecretStore.removePassword(myRsyncApplication.appContext, this.id);
            secureDeleteSshTempFiles(myRsyncApplication.appContext, this.id);
        }

        Locale current_locale = AppLocales.of(myRsyncApplication.appContext);
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);
        try {
            File debugLogFile = new File(myRsyncApplication.appContext.getApplicationInfo().dataDir + "/debug.log");
            FileWriter debug_writer = new FileWriter(debugLogFile, true);
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
        SharedPreferences prefs = myRsyncApplication.appContext.getSharedPreferences("configs", Context.MODE_PRIVATE);
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
        prefseditor.remove("rs_adv_options_"+String.valueOf(this.id));
        prefseditor.remove("rs_transport_type_"+String.valueOf(this.id));
        prefseditor.remove("rs_ssh_private_key_path_"+String.valueOf(this.id));
        prefseditor.remove("rs_ssh_remote_path_"+String.valueOf(this.id));
        prefseditor.remove("rs_ssh_auth_type_"+String.valueOf(this.id));
        prefseditor.remove("rs_ssh_password_"+String.valueOf(this.id));
        prefseditor.apply();

        SshSecretStore.removePassword(myRsyncApplication.appContext, this.id);
        secureDeleteSshTempFiles(myRsyncApplication.appContext, this.id);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) myRsyncApplication.appContext.deleteSharedPreferences("CMD_"+String.valueOf(id));
        else (new File(myRsyncApplication.appContext.getApplicationInfo().dataDir+"/shared_prefs/CMD_"+String.valueOf(id)+".xml")).delete();

        Locale current_locale = AppLocales.of(myRsyncApplication.appContext);
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);
        try {
            File debugLogFile = new File(myRsyncApplication.appContext.getApplicationInfo().dataDir + "/debug.log");
            FileWriter debug_writer = new FileWriter(debugLogFile, true);
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

    /**
     * Starts an rsync run on a background thread.
     *
     * @return the worker thread, or {@code null} if the run was aborted before starting
     *         (e.g. no usable network).
     */
    Thread executeConfig(final Context context, final Integer scheduler_id){

        SharedPreferences sched_prefs = context.getSharedPreferences("schedulers", Context.MODE_PRIVATE);
        int sched;

        if (scheduler_id == null) sched = -1;
        else sched = scheduler_id;

        if (!shouldRunNow(context, sched_prefs, scheduler_id)) {
            return null;
        }

        send_notification(context, 3);
            final SharedPreferences prefs =  context.getSharedPreferences("Rsync_Command_build", Context.MODE_PRIVATE);
            final int id = this.id;

            final String options = this.rs_options != null ? this.rs_options : "";
            final String log = prefs.getString("log",context.getApplicationInfo().dataDir+"/logfile.log");
            final String local_path = this.local_path != null ? this.local_path.trim() : "";
            final String remote_path = this.rs_ssh_remote_path != null ? this.rs_ssh_remote_path.trim() : "";
            final String module = this.rs_module != null ? this.rs_module.trim() : "";

            String Debug_log_path = context.getApplicationInfo().dataDir + "/debug.log";
            final File debugLogFile = new File(Debug_log_path);
            final String mode= this.rs_mode;


            Thread t = new Thread(){
                @SuppressLint("ApplySharedPref")
                @Override
                public void run() {
                    try {
                        SharedPreferences pref = context.getSharedPreferences("CMD_"+String.valueOf(id),Context.MODE_PRIVATE);
                        SharedPreferences.Editor pref_Edit= pref.edit();
                        SharedPreferences sched_prefs = context.getSharedPreferences("schedulers", Context.MODE_PRIVATE);
                        Locale current_locale = AppLocales.of(context);
                        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm",current_locale);

                        if (scheduler_id!=null) sched_prefs.edit().putBoolean("is_running_" + String.valueOf(scheduler_id), true).commit();


                        pref_Edit.putBoolean("is_running",true);
                        pref_Edit.commit();

                        try {
                            FileWriter debug_writer = new FileWriter(debugLogFile, true);
                            CharSequence message = "\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + "CONFIGURATION RUN "+name+ " STARTED";
                            debug_writer.append(message);
                            debug_writer.close();
                        }
                        catch (IOException e){
                            e.printStackTrace();
                        }
                        String app_folder = context.getApplicationInfo().nativeLibraryDir;
                        ProcessBuilder p = new ProcessBuilder();
                        Log.d("External Cache dir", context.getExternalCacheDirs()[0].getAbsolutePath());
                        String rsync_bin = app_folder+"/librsync.so";
                        String ssh_bin = app_folder+"/libdbclient.so";
                        String convert_bin = app_folder+"/libdbconvert.so";

                        ArrayList<String> cmdList = new ArrayList<>();

                        boolean isRoot = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("root_access", false);

                        String destination;
                        String transport_options = "";
                        String workingKeyPath = "";
                        String sshRemoteCmd = "";
                        boolean passwordAuth = isSshPasswordAuth();
                        String sshPassword = rs_ssh_password != null ? rs_ssh_password : "";
                        File sshPassFile = null;
                        File sshWrapFile = null;

                        if ("SSH".equals(rs_transport_type)) {
                            destination = rs_user + "@" + rs_ip + ":" + remote_path;

                            if (passwordAuth) {
                                // rsync does not reliably inherit ProcessBuilder env into the -e child.
                                // Use a small wrapper that exports DROPBEAR_PASSWORD then execs dbclient.
                                try {
                                    File[] wrapFiles = prepareSshPasswordWrapper(context, ssh_bin, sshPassword);
                                    sshPassFile = wrapFiles[0];
                                    sshWrapFile = wrapFiles[1];
                                } catch (Exception e) {
                                    Log.e("SSH_PASS", "Failed to prepare password wrapper", e);
                                    secureDeleteSshTempFiles(context, id);
                                    sshWrapFile = null;
                                }
                                if (sshWrapFile == null) {
                                    try {
                                        FileWriter debug_writer = new FileWriter(debugLogFile, true);
                                        debug_writer.append("\nSSH password auth aborted: could not prepare wrapper.");
                                        debug_writer.close();
                                    } catch (IOException ignored) {}
                                    if (scheduler_id != null) {
                                        sched_prefs.edit().putBoolean("is_running_" + scheduler_id, false).commit();
                                    }
                                    update_result(context, "Error", scheduler_id != null ? scheduler_id : -1);
                                    return;
                                }
                                sshRemoteCmd = "/system/bin/sh " + sshWrapFile.getAbsolutePath()
                                        + " -p " + rs_port + " -y -y";
                                transport_options = "-e '" + sshRemoteCmd + "'";
                            } else {
                                File keyFile = new File(rs_ssh_private_key_path);
                                workingKeyPath = rs_ssh_private_key_path;

                                // SSH keys must have 600 permissions. On Android, files on SD card usually have 777 or similar
                                // and chmod doesn't work. We copy it to the app's internal storage and chmod it there.
                                File internalKey = new File(context.getFilesDir(), "ssh_key_" + id);
                                try {
                                    copyFile(keyFile, internalKey);
                                    Runtime.getRuntime().exec("chmod 600 " + internalKey.getAbsolutePath()).waitFor();
                                    workingKeyPath = internalKey.getAbsolutePath();

                                    // Try to convert key if it's OpenSSH format
                                    File convertedKey = new File(context.getFilesDir(), "ssh_key_db_" + id);
                                    String[] convertCmd = {convert_bin, "openssh", "dropbear", internalKey.getAbsolutePath(), convertedKey.getAbsolutePath()};
                                    try {
                                        Process convertProc = Runtime.getRuntime().exec(convertCmd);
                                        int exitCode = convertProc.waitFor();
                                        if (exitCode == 0 && convertedKey.exists() && convertedKey.length() > 0) {
                                            workingKeyPath = convertedKey.getAbsolutePath();
                                            Runtime.getRuntime().exec("chmod 600 " + workingKeyPath).waitFor();
                                        } else {
                                            Log.w("SSH_KEY", "Key conversion skipped or failed (exit code: " + exitCode + ")");
                                        }
                                    } catch (Exception e) {
                                        Log.w("SSH_KEY", "Key conversion exception", e);
                                    }
                                } catch (IOException | InterruptedException e) {
                                    Log.e("SSH_KEY", "Failed to prepare key file", e);
                                }

                                sshRemoteCmd = ssh_bin + " -p " + rs_port + " -i " + workingKeyPath
                                        + " -y -y";
                                transport_options = "-e '" + sshRemoteCmd + "'";
                            }
                        } else {
                            destination = "rsync://" + rs_user + "@" + rs_ip + ":" + rs_port + "/" + module;
                            sshRemoteCmd = "";
                        }

                        String result = "Error";
                        try {
                            if (isRoot) {
                                StringBuilder rootCmd = new StringBuilder(rsync_bin);

                                String cleanOptions = options.strip();
                                if (!cleanOptions.isEmpty()) {
                                    rootCmd.append(" ").append(cleanOptions);
                                }

                                if (!transport_options.isEmpty()) {
                                    rootCmd.append(" ").append(transport_options);
                                }

                                rootCmd.append(" --log-file=").append(log);

                                if (mode.equals("Push")) {
                                    rootCmd.append(" ").append(local_path).append(" ").append(destination);
                                } else {
                                    rootCmd.append(" ").append(destination).append(" ").append(local_path);
                                }

                                cmdList.add("su");
                                cmdList.add("-c");
                                cmdList.add(rootCmd.toString());

                            } else {
                                cmdList.add(rsync_bin);

                                String cleanOptions = options.strip();
                                if (!cleanOptions.isEmpty()) {
                                    String[] splitOptions = cleanOptions.split("\\s+");
                                    cmdList.addAll(Arrays.asList(splitOptions));
                                }

                                if ("SSH".equals(rs_transport_type)) {
                                    cmdList.add("-e");
                                    cmdList.add(sshRemoteCmd);
                                }

                                cmdList.add("--log-file");
                                cmdList.add(log);

                                if (mode.equals("Push")) {
                                    cmdList.add(local_path);
                                    cmdList.add(destination);
                                } else {
                                    cmdList.add(destination);
                                    cmdList.add(local_path);
                                }
                            }

                            // Logging the command to debug.log (never include the password).
                            try {
                                FileWriter debug_writer = new FileWriter(debugLogFile, true);
                                String cmdLog = "\nRSYNC_COMMAND=" + redactSecrets(cmdList.toString(), sshPassword);
                                debug_writer.append(cmdLog);
                                debug_writer.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            Log.d("CMD_STRING", redactSecrets(cmdList.toString(), sshPassword));

                            p.command(cmdList);

                            Log.d("FILES_FOLDER", app_folder);
                            p.environment().put("PATH", app_folder + ":/su/bin:/sbin:/vendor/bin:/system/sbin:/system/bin:/su/xbin:/system/xbin");
                            p.redirectErrorStream(true);
                            Process process = p.start();

                            BufferedReader std_out = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            StringBuilder builder = new StringBuilder();
                            String line;

                            while ((line = std_out.readLine()) != null) {
                                builder.append(line);
                                builder.append(System.getProperty("line.separator"));
                            }
                            String stdout = builder.toString();

                            String filteredOut = filterSshClientNoise(
                                    redactSecrets(stdout, sshPassword));
                            if (!filteredOut.isEmpty()) {
                                try {
                                    FileWriter debug_writer = new FileWriter(debugLogFile, true);
                                    CharSequence message = "\n\n[ "
                                            + formatter.format(Calendar.getInstance().getTime())
                                            + " ] OUT_STREAM = "
                                            + filteredOut;
                                    debug_writer.append(message);
                                    debug_writer.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            result = process.waitFor() == 0 ? "OK" : "Warning! Check Log!";
                        } finally {
                            secureDeleteSshTempFiles(context, id);
                            sshPassFile = null;
                            sshWrapFile = null;
                        }

                        if (scheduler_id != null) {
                            sched_prefs.edit().putBoolean("is_running_"+String.valueOf(scheduler_id),false).commit();
                        }
                        update_result(context, result, scheduler_id != null ? scheduler_id : -1);
                        try {
                            FileWriter debug_writer = new FileWriter(debugLogFile, true);
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
                            FileWriter debug_writer = new FileWriter(debugLogFile, true);
                            String message = "CONFIGURATION RUN EXCEPTION CAUGHT: \n" + e.getMessage();
                            debug_writer.append(message);
                            debug_writer.close();
                            e.printStackTrace();
                            if (scheduler_id != null) {
                                sched_prefs.edit().putBoolean("is_running_" + String.valueOf(scheduler_id), false).commit();
                            }
                            update_result(context, "Error", scheduler_id != null ? scheduler_id : -1);
                        }
                        catch (IOException ioe){
                            ioe.printStackTrace();
                        }

                    }
                    }
                };

                t.start();
                return t;
    }

    /** Manual / Tasker: any network. Scheduled: optional Wi‑Fi-only via {@code wifi_switch_}. */
    private boolean shouldRunNow(Context context, SharedPreferences sched_prefs, Integer scheduler_id) {
        int sched = scheduler_id == null ? -1 : scheduler_id;
        boolean requireWifi = false;
        if (scheduler_id != null) {
            requireWifi = sched_prefs.getBoolean("wifi_switch_" + scheduler_id, false);
        }
        boolean ok = requireWifi ? isConnectedToWifi(context) : isNetworkAvailable(context);
        if (!ok) {
            send_notification(context, requireWifi ? 1 : 0);
            update_result(context, "Aborted", sched);
        }
        return ok;
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;
        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
        if (caps == null) return false;
        return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                || caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                || caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET);
    }

    private void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        try (FileChannel source = new FileInputStream(sourceFile).getChannel();
             FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());
        }
    }

    private void update_result(Context context, String result, int scheduler_id){

        Calendar cal = Calendar.getInstance();
        long time = cal.getTimeInMillis();

        SharedPreferences cmdpref = context.getSharedPreferences("CMD_"+String.valueOf(id),Context.MODE_PRIVATE);
        SharedPreferences.Editor cmd_pref_Edit= cmdpref.edit();
        SharedPreferences config_prefs =  context.getSharedPreferences("configs", Context.MODE_PRIVATE);
        SharedPreferences.Editor configs_prefseditor = config_prefs.edit();
        SharedPreferences sched_prefs = context.getSharedPreferences("schedulers", Context.MODE_PRIVATE);

        Locale current_locale = AppLocales.of(context);
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm",current_locale);

        cmd_pref_Edit.putBoolean("is_running",false);
        cmd_pref_Edit.commit();

        configs_prefseditor.putString("last_result_"+String.valueOf(this.id),result);
        if (scheduler_id >= 0) {
            sched_prefs.edit().putLong("last_run_"+String.valueOf(scheduler_id),time).commit();
            Scheduler.refreshInMemoryAlarmCache(scheduler_id);
        }
        configs_prefseditor.putString("last_run_"+String.valueOf(id),formatter.format(time));
        configs_prefseditor.commit();

    }

//    private String get_current_ssid(Context context) {
//        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//
//        // 1. Check if we have an active network
//        android.net.Network network = cm.getActiveNetwork();
//        if (network == null) return "none";
//
//        // 2. Check if it's WiFi
//        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(network);
//        if (caps == null || !caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
//            return "none";
//        }
//
//        // 3. Modern way (Android 10+)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            // The SSID is often embedded in the TransportInfo
//            android.net.TransportInfo transportInfo = caps.getTransportInfo();
//            if (transportInfo instanceof WifiInfo) {
//                WifiInfo info = (WifiInfo) transportInfo;
//                String ssid = info.getSSID();
//                if (ssid != null && !ssid.equals("<unknown ssid>")) {
//                    return ssid.replaceAll("^\"|\"$", "");
//                }
//            }
//        }
//
//        // 4. Fallback for older versions or if TransportInfo fails
//        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        WifiInfo connectionInfo = wifiManager.getConnectionInfo();
//        if (connectionInfo != null) {
//            String ssid = connectionInfo.getSSID();
//            if (ssid != null && !ssid.equals("<unknown ssid>")) {
//                return ssid.replaceAll("^\"|\"$", "");
//            }
//        }
//
//        return "none";
//    }

    private boolean isConnectedToWifi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;
        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
        return caps != null && caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI);
    }

    private void send_notification(Context ctx, int type) {

        SharedPreferences set_prefs = getDefaultSharedPreferences(ctx);
        Locale current_locale = AppLocales.of(ctx);
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
            int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent, pendingFlags);

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

    private static final String SSH_SECURE_DIR = "ssh_secure";

    @NonNull
    private static File sshSecureDir(@NonNull Context context) {
        File dir = new File(context.getFilesDir(), SSH_SECURE_DIR);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return dir;
    }

    @NonNull
    private static File sshPassFileFor(@NonNull Context context, int configId) {
        return new File(sshSecureDir(context), "pass_" + configId + ".tmp");
    }

    @NonNull
    private static File sshWrapFileFor(@NonNull Context context, int configId) {
        return new File(sshSecureDir(context), "wrap_" + configId + ".sh");
    }

    @NonNull
    private static File sshAskPassFileFor(@NonNull Context context, int configId) {
        return new File(sshSecureDir(context), "askpass_" + configId + ".sh");
    }

    /** Overwrite then delete password/wrapper temp files for a config. */
    static void secureDeleteSshTempFiles(@NonNull Context context, int configId) {
        secureWipeDelete(sshPassFileFor(context, configId));
        secureWipeDelete(sshWrapFileFor(context, configId));
        secureWipeDelete(sshAskPassFileFor(context, configId));
        secureWipeDelete(new File(sshSecureDir(context), "auth_probe_" + configId + ".txt"));
        // Legacy paths from the first wrapper revision (before ssh_secure/).
        secureWipeDelete(new File(context.getFilesDir(), "ssh_pass_" + configId + ".tmp"));
        secureWipeDelete(new File(context.getFilesDir(), "ssh_wrap_" + configId + ".sh"));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void secureWipeDelete(@Nullable File file) {
        if (file == null || !file.exists()) {
            return;
        }
        try {
            long len = file.length();
            if (len > 0 && len < 1_000_000L) {
                char[] wipe = new char[(int) len];
                java.util.Arrays.fill(wipe, '\0');
                try (FileWriter fw = new FileWriter(file, false)) {
                    fw.write(wipe);
                }
            }
        } catch (IOException ignored) {
        }
        file.delete();
    }

    /**
     * Creates a password file + wrapper so Dropbear can authenticate when spawned by rsync {@code -e}.
     * <p>
     * The password is never interpolated into the script text (avoids mangling special characters).
     * The wrapper reads the file into {@code DROPBEAR_PASSWORD} via assignment (literal bytes) and
     * execs dbclient. Invoked as {@code /system/bin/sh wrap.sh ...} so the script need not be +x.
     *
     * @return [passwordFile, wrapperScript]
     */
    @NonNull
    private File[] prepareSshPasswordWrapper(@NonNull Context context,
                                             @NonNull String sshBin,
                                             @NonNull String password)
            throws IOException {
        File passFile = sshPassFileFor(context, id);
        File wrapFile = sshWrapFileFor(context, id);
        try {
            // UTF-8 bytes, no trailing newline added (EditText content as-is).
            byte[] passBytes = password.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            try (FileOutputStream fos = new FileOutputStream(passFile, false)) {
                fos.write(passBytes);
            }
            //noinspection ResultOfMethodCallIgnored
            passFile.setReadable(true, true);
            //noinspection ResultOfMethodCallIgnored
            passFile.setWritable(true, true);

            String passPath = shellSingleQuote(passFile.getAbsolutePath());
            String sshPath = shellSingleQuote(sshBin);

            // Assignment keeps password bytes literal (spaces, $, !, @, quotes, etc.).
            // Trailing newlines are stripped by $(...); we never write a trailing newline.
            // Use absolute /system/bin/* — rsync's -e child may inherit a minimal PATH.
            String wrapScript = "#!/system/bin/sh\n"
                    + "PASS_FILE=" + passPath + "\n"
                    + "SSH_BIN=" + sshPath + "\n"
                    + "DROPBEAR_PASSWORD=$(/system/bin/cat \"$PASS_FILE\" 2>/dev/null)\n"
                    + "export DROPBEAR_PASSWORD\n"
                    + "exec \"$SSH_BIN\" \"$@\"\n";
            try (FileWriter fw = new FileWriter(wrapFile, false)) {
                fw.write(wrapScript);
            }
            //noinspection ResultOfMethodCallIgnored
            wrapFile.setReadable(true, true);
            //noinspection ResultOfMethodCallIgnored
            wrapFile.setWritable(true, true);

            return new File[]{passFile, wrapFile};
        } catch (IOException e) {
            secureDeleteSshTempFiles(context, id);
            throw e;
        }
    }

    /** Single-quote a value for embedding in a shell script. */
    private static String shellSingleQuote(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }

    /** Remove a password from log lines if present. */
    private static String redactSecrets(String text, String password) {
        if (text == null) {
            return "";
        }
        if (password == null || password.isEmpty()) {
            return text;
        }
        return text.replace(password, "********");
    }

    /** Strip routine Dropbear/rsync chatter that adds no diagnostic value. */
    @NonNull
    private static String filterSshClientNoise(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.contains("Caution, skipping hostkey check")) {
                continue;
            }
            if (trimmed.contains("skipping hostkey check for")) {
                continue;
            }
            if (trimmed.contains("Ignoring unknown configuration option")) {
                continue;
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(line);
        }
        return out.toString();
    }

    }
