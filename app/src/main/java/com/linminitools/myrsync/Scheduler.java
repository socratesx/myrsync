package com.linminitools.myrsync;


import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.widget.TimePicker;
import android.widget.Toast;

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
import java.util.Objects;

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
    }

    public void setAlarm(Context ctx){

        if (!Alarm_Times.isEmpty()) Alarm_Times.clear();

        Calendar cal = Calendar.getInstance();
        Calendar saved_cal = new GregorianCalendar();
        cal.setFirstDayOfWeek(Calendar.MONDAY);

        cal.set(Calendar.HOUR_OF_DAY,d.getCurrentHour());
        cal.set(Calendar.MINUTE,d.getCurrentMinute());
        long scheduler_time = cal.getTimeInMillis();
        final long time_selected = scheduler_time;

        String[] all_days={"SUNDAY","MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY"};
        String[] days_list;
        int today_number=saved_cal.get(DAY_OF_WEEK);
        int day_of_month=saved_cal.get(Calendar.DAY_OF_MONTH);

        try {
            days_list = this.days.substring(1, this.days.length() - 1).toUpperCase().split("[.]");
            Map<String, Integer> weekdays = new HashMap<>();

            for (int i = 0; i <= 6; i++) weekdays.put(all_days[i], i + 1);


            int count=0;
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
            int jobcount=id*10;
            for (long t :Alarm_Times){
                jobcount=jobcount+1;
                setWorker(ctx,t,jobcount);

            }



        }
        catch (StringIndexOutOfBoundsException e){
            CharSequence text = "No repeat Days have been selected. Scheduler will never run!";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(appContext, text, duration);
            toast.show();
        }
    }


    private void setWorker(Context ctx, long start, int jobid){

        // 86400000;          1 day in milliseconds
        // 60000;          // 1 minute in milliseconds
        long next_week_in_millis= 604800000;

        long delay_till_work_start = start - getInstance().getTimeInMillis();
        long next_time= start+next_week_in_millis;

        PersistableBundle perbun=new PersistableBundle();
        perbun.putInt("config_id",config_id);
        perbun.putInt("jobid",jobid);
        perbun.putInt("sched_id",this.id);
        perbun.putLong("next_time",next_time);

        long seconds = delay_till_work_start / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        String remaining_time=days + "days  " + hours % 24 + "h  " + minutes % 60 + "m  " + seconds % 60 +"s";


        try {


            String Debug_log_path= ctx.getApplicationInfo().dataDir + "/debug.log";
            debug_log = new java.io.File(Debug_log_path);


            FileWriter debug_writer = new FileWriter(debug_log,true);
            Locale current_locale = ctx.getResources().getConfiguration().locale;
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);
            CharSequence message= "\n\n[ "+ formatter.format(Calendar.getInstance().getTime()) +" ] "+"setWorker { "+"\nJOBID = "+String.valueOf(jobid) +
                    "\nSCHEDULER ID = "+String.valueOf(this.id)+
                    "\nSTART = " + formatter.format(start)+
                    "\nDELAY TILL WORK START = "+remaining_time+
                    "\nNEXT TIME (WEEK) = "+formatter.format(next_time)+ " }";
            debug_writer.append(message);
            debug_writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


        ComponentName serviceComponent = new ComponentName(ctx, rsyncJobScheduler.class);

        JobInfo.Builder builder= new JobInfo.Builder(jobid, serviceComponent)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(delay_till_work_start)
                .setExtras(perbun)
                .setOverrideDeadline(delay_till_work_start+30000)
                .setPersisted(true);

        JobInfo rsyncJob= builder.build();
        JobScheduler jobScheduler = (JobScheduler)ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        Objects.requireNonNull(jobScheduler).schedule(rsyncJob);
    }


    public void cancelAlarm(Context ctx) {
        JobScheduler js = (JobScheduler) ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        for (JobInfo ji : Objects.requireNonNull(js).getAllPendingJobs()) {
            if (String.valueOf(this.id).equals(String.valueOf(ji.getId()).substring(0, 1))) Objects.requireNonNull(js).cancel(ji.getId());
        }
    }

    public String getNextAlarm(Context ctx){
        JobScheduler js = (JobScheduler) ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        //noinspection unchecked
        ArrayList<Long> next_jobs= new ArrayList();
        for (JobInfo ji : Objects.requireNonNull(js).getAllPendingJobs()) {
            if (String.valueOf(this.id).equals(String.valueOf(ji.getId()).substring(0, 1)))
                next_jobs.add(ji.getMinLatencyMillis());
        }
        //this.update();
        Collections.sort(next_jobs);

        long next_alarm=next_jobs.get(0);
        long saved_on=ctx.getSharedPreferences("schedulers", MODE_PRIVATE).getLong("last_save_"+String.valueOf(this.id),0);
        long first_run_on=saved_on+next_alarm;
        long next_time=first_run_on-Calendar.getInstance().getTimeInMillis();


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
