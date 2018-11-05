package com.linminitools.myrsync;


import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.getInstance;

public class Scheduler extends MainActivity implements Comparable<Scheduler>{


    TimePicker d;
    String days,name;
    int id;
    int hour;
    int min;
    int config_id;
    private List<Long> Alarm_Times;
    Long addedOn;


    public Scheduler(){
        this.name="Scheduler";
    }

    public Scheduler (String days, TimePicker d, int id){

        this.days=days;
        this.d=d;
        this.id=id;
        this.hour=d.getCurrentHour();
        this.min=d.getCurrentMinute();
        this.Alarm_Times = new ArrayList<>();

    }


    public int compareTo(@NonNull Scheduler s){
        return Long.compare(this.addedOn,s.addedOn);
    }

    void update(){
        this.hour=d.getCurrentHour();
        this.min=d.getCurrentMinute();
    }


    @SuppressLint("ApplySharedPref")
    void saveToDisk(){

        SharedPreferences prefs = appContext.getSharedPreferences("schedulers", MODE_PRIVATE);
        SharedPreferences.Editor prefseditor = prefs.edit();

        this.addedOn=prefs.getLong("addedon_"+String.valueOf(this.id),0);
        if (this.addedOn==0) this.addedOn=Calendar.getInstance().getTimeInMillis();

        prefseditor.putInt("id_"+String.valueOf(this.id),this.id);
        prefseditor.putInt("hour_"+String.valueOf(this.id),this.hour);
        prefseditor.putInt("min_"+String.valueOf(this.id),this.min);
        prefseditor.putString("days_"+String.valueOf(this.id),this.days);
        prefseditor.putString("name_"+String.valueOf(this.id),this.name);
        prefseditor.putInt("config_id_"+String.valueOf(this.id),this.config_id);
        prefseditor.putLong("addedon_"+String.valueOf(this.id),this.addedOn);
        prefseditor.putLong("last_run_"+String.valueOf(this.id),-1);
        prefseditor.putLong("last_save_"+String.valueOf(this.id),Calendar.getInstance().getTimeInMillis());
        prefseditor.putBoolean("is_running_"+String.valueOf(this.id),false);
        prefseditor.commit();

        try {
            FileWriter debug_writer = new FileWriter(debug_log, true);
            Locale current_locale = appContext.getResources().getConfiguration().locale;
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);
            CharSequence message = "\n\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + "SCHEDULER SAVED: "+this.name +"{"+
                    "\nID: "+this.id+"\nTIME: "+this.hour+":"+this.min+"\nDAYS: "+this.days +"\n}";
            debug_writer.append(message);
            debug_writer.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

    @SuppressLint("ApplySharedPref")
    void deleteFromDisk(){
        SharedPreferences prefs = appContext.getSharedPreferences("schedulers", MODE_PRIVATE);
        SharedPreferences.Editor prefseditor = prefs.edit();

        prefseditor.remove("id_"+String.valueOf(this.id));
        prefseditor.remove("hour_"+String.valueOf(this.id));
        prefseditor.remove("min_"+String.valueOf(this.id));
        prefseditor.remove("days_"+String.valueOf(this.id));
        prefseditor.remove("name_"+String.valueOf(this.id));
        prefseditor.remove("config_id_"+String.valueOf(this.id));
        prefseditor.remove("addedon_"+String.valueOf(this.id));
        prefseditor.remove("last_run_"+String.valueOf(this.id));
        prefseditor.remove("last_save_"+String.valueOf(this.id));
        prefseditor.remove("is_running_"+String.valueOf(this.id));

        prefseditor.commit();

        try {
            FileWriter debug_writer = new FileWriter(debug_log, true);
            Locale current_locale = appContext.getResources().getConfiguration().locale;
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);
            CharSequence message = "\n\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + "SCHEDULER DELETED: "+this.name;
            debug_writer.append(message);
            debug_writer.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

    private List<Long> calculate_alarm_times(Context ctx) {

        if (!Alarm_Times.isEmpty()) Alarm_Times.clear();

        Calendar cal = Calendar.getInstance();
        Calendar saved_cal = new GregorianCalendar();
        cal.setFirstDayOfWeek(Calendar.MONDAY);

        cal.set(Calendar.HOUR_OF_DAY, d.getCurrentHour());
        cal.set(Calendar.MINUTE, d.getCurrentMinute());
        long scheduler_time = cal.getTimeInMillis();
        final long time_selected = scheduler_time;

        String[] all_days = {"SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
        String[] days_list;
        int today_number = saved_cal.get(DAY_OF_WEEK);
        int day_of_month = saved_cal.get(Calendar.DAY_OF_MONTH);

        try {
            days_list = this.days.substring(1, this.days.length() - 1).toUpperCase().split("[.]");
            Map<String, Integer> weekdays = new HashMap<>();

            for (int i = 0; i <= 6; i++) weekdays.put(all_days[i], i + 1);


            int count = 0;
            for (String d : days_list) {
                int delta = today_number - weekdays.get(d);
                cal = getInstance();

                cal.set(Calendar.HOUR_OF_DAY, this.d.getCurrentHour());
                cal.set(Calendar.MINUTE, this.d.getCurrentMinute());
                if (delta > 0) {
                    cal.set(Calendar.DAY_OF_MONTH, day_of_month - Math.abs(delta) + 7);
                } else if (delta == 0) {
                    Calendar now = Calendar.getInstance();
                    long time_now = now.getTimeInMillis();
                    long delta2 = time_selected - time_now;
                    if (delta2 < 0) cal.set(Calendar.DAY_OF_MONTH, day_of_month + 7);
                } else {
                    cal.set(Calendar.DAY_OF_MONTH, day_of_month + Math.abs(delta));
                }
                count = count + 1;
                scheduler_time = cal.getTimeInMillis();

                Alarm_Times.add(scheduler_time);
            }
            Collections.sort(Alarm_Times);
            return Alarm_Times;

        } catch (StringIndexOutOfBoundsException e) {
            CharSequence text = "No repeat Days have been selected. Scheduler will never run!";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(appContext, text, duration);
            toast.show();
            return null;
        }
    }

    public void setAlarm(Context ctx){

        ArrayList<Long> alarms = (ArrayList<Long>)this.calculate_alarm_times(ctx);
        if (alarms!=null) {
            int jobcount = this.id * 10;
            AlarmManager alarmMgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

            Intent i = new Intent(ctx, AlarmReceiver.class);
            i.setAction(Intent.ACTION_RUN);
            i.putExtra("config_id", this.config_id);
            i.putExtra("sched_id", this.id);

            String Debug_log_path= ctx.getApplicationInfo().dataDir + "/debug.log";
            debug_log = new File(Debug_log_path);

            try {
                FileWriter debug_writer = new FileWriter(debug_log, true);
                Locale current_locale = ctx.getResources().getConfiguration().locale;
                SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);
                CharSequence message = "\n\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + "Scheduler::setAlarm { ";
                debug_writer.append(message);

            for (long t : alarms) {
                jobcount = jobcount + 1;
                PendingIntent pi = PendingIntent.getBroadcast(ctx, jobcount, i, PendingIntent.FLAG_UPDATE_CURRENT);
                alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, t, 604800000, pi);
                message = "\nAlarm Time Set :"+formatter.format(t);
                debug_writer.append(message);
            }
                message = "\n} ";
                debug_writer.append(message);
                debug_writer.close();

            } catch (IOException e) {
                    e.printStackTrace();
            }
        }
    }

    public void cancelAlarm(Context ctx) {

        ArrayList<Long> alarms = (ArrayList<Long>)this.calculate_alarm_times(ctx);
        if (alarms!=null) {

            int jobcount = this.id * 10;
            AlarmManager alarmMgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

            Intent i = new Intent(ctx, AlarmReceiver.class);
            i.setAction(Intent.ACTION_RUN);
            i.putExtra("config_id", this.config_id);
            i.putExtra("sched_id", this.id);

            for (long t : alarms) {
                jobcount=jobcount+1;
                PendingIntent pi = PendingIntent.getBroadcast(ctx, jobcount, i, PendingIntent.FLAG_UPDATE_CURRENT);
                alarmMgr.cancel(pi);
            }
        }
    }

    public String getNextAlarm(Context ctx){

        if (Alarm_Times.isEmpty()) Alarm_Times=this.calculate_alarm_times(ctx);

        SharedPreferences prefs = ctx.getSharedPreferences("schedulers", MODE_PRIVATE);
        if(Alarm_Times.get(0)-prefs.getLong("last_run_"+String.valueOf(this.id),-1)<0) {
            Alarm_Times.clear();
            Alarm_Times=this.calculate_alarm_times(ctx);
        }

        long next_time=Alarm_Times.get(0)-Calendar.getInstance().getTimeInMillis();
        long seconds = next_time / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        String remaining_time;
        if (next_time>0) remaining_time = days + "days  " + hours % 24 + "h  " + minutes % 60 + "m  " + seconds % 60 +"s";
        else remaining_time="About to start";
        return String.valueOf(remaining_time);

    }
}
