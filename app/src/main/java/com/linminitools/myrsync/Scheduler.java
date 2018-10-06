package com.linminitools.myrsync;


import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
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
    //private List<Integer> JobsIds;
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
        if (this.addedOn==0) Calendar.getInstance().getTimeInMillis();

        prefseditor.putInt("id_"+String.valueOf(this.id),this.id);
        prefseditor.putInt("hour_"+String.valueOf(this.id),this.hour);
        prefseditor.putInt("min_"+String.valueOf(this.id),this.min);
        prefseditor.putString("days_"+String.valueOf(this.id),this.days);
        prefseditor.putString("name_"+String.valueOf(this.id),this.name);
        prefseditor.putInt("config_pos_"+String.valueOf(this.id),this.config_pos);
        prefseditor.putLong("addedon_"+String.valueOf(this.id),this.addedOn);

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
        prefseditor.remove("addedon_"+String.valueOf(this.id));
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
                Log.d("AL_TIME",String.valueOf(t));
                //JobsIds.add(jobcount);
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

        long one_day = 86400000;          // 1 day in milliseconds
        long one_minute = 60000;          // 1 minute in milliseconds

        long delay_till_work_start = start - getInstance().getTimeInMillis();

        if (delay_till_work_start < one_minute) {

            if (delay_till_work_start < 0) delay_till_work_start=one_day+delay_till_work_start;
            else delay_till_work_start = one_minute;

        }

        PersistableBundle perbun=new PersistableBundle();
        perbun.putInt("config_pos",config_pos);
        perbun.putInt("jobid",jobid);

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
        ArrayList<Long> next_jobs= new ArrayList();
        for (JobInfo ji : Objects.requireNonNull(js).getAllPendingJobs()) {
            if (String.valueOf(this.id).equals(String.valueOf(ji.getId()).substring(0, 1)))
                next_jobs.add(ji.getMinLatencyMillis());
        }

        Collections.sort(next_jobs);
        long next_alarm;
        float Days, Hours, Minutes, Seconds;
        next_alarm=next_jobs.get(0);

        Days = next_alarm/86400000;
        Hours = (next_alarm-((int)Days)*next_alarm)/3600000;
        Minutes = Hours/60000;
        Seconds = Minutes/1000;

        String remaining_time= String.valueOf((int)Days) +" Days " + String.valueOf((int)Hours)+" Hours "
                + String.valueOf((int)Minutes)+ " Minutes "+ String.valueOf((int)Seconds)+ "Seconds";

        return String.valueOf(remaining_time);

    }
    /*
    public String getNextAlarm(){
        Calendar now = Calendar.getInstance();
        long time = now.getTimeInMillis();
        long one_day = 86400000;
        long one_week =604800000;

        int today = now.get(Calendar.DAY_OF_WEEK);

        @SuppressWarnings("unchecked") Map<Integer,String> all_days= new HashMap();
        all_days.put(2,"MONDAY");
        all_days.put(3,"TUESDAY");
        all_days.put(4,"WEDNESDAY");
        all_days.put(5,"THURSDAY");
        all_days.put(6,"FRIDAY");
        all_days.put(7,"SATURDAY");
        all_days.put(1,"SUNDAY");

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
                next_time = count * one_day + delta;


            time = time + next_time;
            if(next_time<0) time=time+one_week;
            Locale current_locale = appContext.getResources().getConfiguration().locale;
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm",current_locale);

            return formatter.format(time);
        }
        catch (StringIndexOutOfBoundsException e){
            return "Never";
        }

    }
*/

}
