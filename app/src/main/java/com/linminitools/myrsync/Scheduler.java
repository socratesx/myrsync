package com.linminitools.myrsync;


import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

    //ArrayList<String> days;
    TimePicker d;
    String days,name;
    private int id;
    int hour;
    int min;
    int config_pos;
    //private List<PendingIntent> Alarm_List;
    private List<Long> Alarm_Times;
    private List<Integer> JobsIds;

    public Scheduler(){
        this.name="Scheduler";
    }

    public Scheduler (String days, TimePicker d, int id){

        this.days=days;
        this.d=d;
        this.id=id;
        this.hour=d.getCurrentHour();
        this.min=d.getCurrentMinute();
        //this.Alarm_List= new ArrayList<>();
        this.Alarm_Times = new ArrayList<>();
        this.JobsIds = new ArrayList<>();

    }


    public int compareTo(Scheduler s){
        return Integer.compare(this.id,s.id);
    }

    void update(){
        this.hour=d.getCurrentHour();
        this.min=d.getCurrentMinute();
    }


    @SuppressLint("ApplySharedPref")
    void saveToDisk(){

        SharedPreferences prefs = appContext.getSharedPreferences("schedulers", MODE_PRIVATE);
        SharedPreferences.Editor prefseditor = prefs.edit();

        prefseditor.putInt("id_"+String.valueOf(this.id),this.id);
        prefseditor.putInt("hour_"+String.valueOf(this.id),this.hour);
        prefseditor.putInt("min_"+String.valueOf(this.id),this.min);
        prefseditor.putString("days_"+String.valueOf(this.id),this.days);
        prefseditor.putString("name_"+String.valueOf(this.id),this.name);
        prefseditor.putInt("config_pos_"+String.valueOf(this.id),this.config_pos);
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
        prefseditor.remove("config_pos_"+String.valueOf(this.id));
        prefseditor.commit();
    }

    public void setAlarm(Context ctx){
        //if (!Alarm_List.isEmpty()) Alarm_List.clear();
        if (!Alarm_Times.isEmpty()) Alarm_Times.clear();

        Calendar cal = Calendar.getInstance();
        Calendar saved_cal = new GregorianCalendar();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        //cal.get(Calendar.DATE);
        long week_interval= 604800000;        // 1 week in milliseconds
        long day_interval= 86400000;         // 1 day in milliseconds
        //AlarmManager al = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);




        cal.set(Calendar.HOUR_OF_DAY,d.getCurrentHour());
        cal.set(Calendar.MINUTE,d.getCurrentMinute());
        long scheduler_time = cal.getTimeInMillis();
        final long time_selected = scheduler_time;

        //ComponentName comp = new ComponentName(getPackageName(),getLocalClassName());
        /*
        Intent futureIntent = new Intent(ctx,AlarmReceiver.class);
        futureIntent.setAction(ACTION_RUN);
        futureIntent.addCategory("com.linminitools.myrsync");
        futureIntent.putExtra("Time",scheduler_time);
        futureIntent.putExtra("config",config_pos);
        */
        String[] all_days={"SUNDAY","MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY"};
        String[] days_list;
        int today_number=saved_cal.get(DAY_OF_WEEK);
        int day_of_month=saved_cal.get(Calendar.DAY_OF_MONTH);

        try {
            days_list = this.days.substring(1, this.days.length() - 1).toUpperCase().split("[.]");
            //Log.d("DAY_", days_list[0-days_list.length]);
            Map<String, Integer> weekdays = new HashMap<>();

            for (int i = 0; i <= 6; i++) {
                weekdays.put(all_days[i], i + 1);
            }
            int count=0;
            for (String d : days_list) {
                int delta = today_number - weekdays.get(d);
                cal = getInstance();

                int request = (100 * this.id) + weekdays.get(d);
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

                /*PendingIntent broadcast = PendingIntent.getBroadcast(ctx, request, futureIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                Objects.requireNonNull(al).setRepeating(AlarmManager.RTC_WAKEUP, scheduler_time, week_interval, broadcast);
                Alarm_List.add(broadcast);  */
                Alarm_Times.add(scheduler_time);
            }
            Collections.sort(Alarm_Times);
            int jobcount=id*10;
            for (long t :Alarm_Times){
                jobcount=jobcount+1;
                Log.d("AL_TIME",String.valueOf(t));
                JobsIds.add(jobcount);
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

        long delay_till_work_start = start - getInstance().getTimeInMillis();

        if (delay_till_work_start<60000) {
            if (delay_till_work_start < 0) {
                delay_till_work_start=86400000+delay_till_work_start;
            } else {
                delay_till_work_start = 60000L;
            }
        }



        PersistableBundle perbun=new PersistableBundle();
        perbun.putInt("config_pos",config_pos);
        perbun.putInt("jobid",jobid);

        Log.d("DELAY",String.valueOf(delay_till_work_start));

        ComponentName serviceComponent = new ComponentName(ctx, rsyncJobScheduler.class);

        JobInfo.Builder builder= new JobInfo.Builder(jobid, serviceComponent)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(delay_till_work_start)//Long.valueOf(delay_till_work_start))
                .setExtras(perbun)
                .setOverrideDeadline(delay_till_work_start+30000)
                .setPersisted(true);
        JobInfo rsyncJob= builder.build();

        JobScheduler jobScheduler = (JobScheduler)ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        Objects.requireNonNull(jobScheduler).schedule(rsyncJob);
        //Log.d("JOB_SCHEDULER_RESULT",String.valueOf(result));
        //Log.d("JOB_SCHEDULER_PENDING",String.valueOf(jobScheduler.getPendingJob(jobid).getId()) +" - "+ String.valueOf(jobScheduler.getPendingJob(jobid).getMinLatencyMillis()));

    }


    public void cancelAlarm(Context ctx) {
       /* AlarmManager al = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
        for (PendingIntent pi : Alarm_List){
            Objects.requireNonNull(al).cancel(pi);
        }
        Alarm_List.clear();
            */
        JobScheduler jobScheduler1 = (JobScheduler) ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        for (JobInfo j : jobScheduler1.getAllPendingJobs()) {
            if (String.valueOf(this.id).equals(String.valueOf(j.getId()).substring(0, 1))) {
                Objects.requireNonNull(jobScheduler1).cancel(j.getId());
            }
            /*
            for (Integer i : JobsIds) {
                JobScheduler jobScheduler = (JobScheduler) ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                Objects.requireNonNull(jobScheduler).cancel(i);
                Log.d("JOB_ID", String.valueOf(i) + " is cancelled");
            }
            */

        }
    }

    public String getNextAlarm(){
        Calendar now = Calendar.getInstance();
        long time = now.getTimeInMillis();

        int today = now.get(Calendar.DAY_OF_WEEK);

        @SuppressWarnings("unchecked") Map<Integer,String> all_days= new HashMap();
        all_days.put(2,"MONDAY");
        all_days.put(3,"TUESDAY");
        all_days.put(4,"WEDNESDAY");
        all_days.put(5,"THURSDAY");
        all_days.put(6,"FRIDAY");
        all_days.put(7,"SATURDAY");
        all_days.put(1,"SUNDAY");


        String today_string=all_days.get(today);
        try {
            String[] days_list = this.days.substring(1, this.days.length() - 1).toUpperCase().split("[.]");
            List<String> sched_days = new ArrayList<>(Arrays.asList(days_list));
            long next_time;
            int count = 0;
            Calendar scheduler_time = Calendar.getInstance();
            scheduler_time.set(Calendar.HOUR_OF_DAY,this.hour);
            scheduler_time.set(Calendar.MINUTE,this.min);

            long delta = scheduler_time.getTimeInMillis() - time;

            if(delta>0) {
                while (!sched_days.contains(all_days.get(today))) {
                    count = count + 1;
                    today = today + 1;
                    if (today > 7) today = 1;
                }
            }
            else{
                today = today + 1;
                count=1;
                if (today > 7) today = 1;
                while (!sched_days.contains(all_days.get(today))) {
                    count = count + 1;
                    today = today + 1;
                    if (today > 7) today = 1;
                }

            }
                next_time = count * 86400000 + delta;


            time = time + next_time;
            if(next_time<0) time=time+604800000;
            Locale current_locale = appContext.getResources().getConfiguration().locale;
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm",current_locale);

            return formatter.format(time);
        }
        catch (StringIndexOutOfBoundsException e){
            return "Never";
        }

    }


}
