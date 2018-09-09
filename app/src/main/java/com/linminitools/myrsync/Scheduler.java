package com.linminitools.myrsync;


import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class Scheduler extends MainActivity{

    //ArrayList<String> days;
    TimePicker d;
    String days,name;
    private int id;
    int hour;
    int min;
    int config_pos;
    private List<PendingIntent> Alarm_List;
    private List<Long> Alarm_Times;

    public Scheduler(){
        this.name="Scheduler";
    }

    public Scheduler (String days, TimePicker d, int id){

        this.days=days;
        this.d=d;
        this.id=id;
        this.hour=d.getCurrentHour();
        this.min=d.getCurrentMinute();
        this.Alarm_List= new ArrayList<>();
        this.Alarm_Times = new ArrayList<>();

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
        prefseditor.commit();
    }

    public void setAlarm(Context ctx){
        if (!Alarm_List.isEmpty()) Alarm_List.clear();

        Calendar cal = Calendar.getInstance();
        Calendar saved_cal = new GregorianCalendar();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        //cal.get(Calendar.DATE);
        long week_interval= 604800000;        // 1 week in milliseconds
        long day_interval= 86400000;         // 1 day in milliseconds
        AlarmManager al = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);




        cal.set(Calendar.HOUR_OF_DAY,d.getCurrentHour());
        cal.set(Calendar.MINUTE,d.getCurrentMinute());
        long scheduler_time = cal.getTimeInMillis();
        final long time_selected = scheduler_time;

        Intent futureIntent = new Intent();
        futureIntent.setAction("android.media.action.DISPLAY_NOTIFICATION");
        futureIntent.addCategory("com.linminitools.myrsync");
        futureIntent.putExtra("Time",scheduler_time);
        futureIntent.putExtra("config",config_pos);

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
                if (delta>0) {
                    cal.set(Calendar.DAY_OF_MONTH,day_of_month-Math.abs(delta)+7);

                }
                else if (delta==0){
                    Calendar now = Calendar.getInstance();
                    long time_now=now.getTimeInMillis();
                    long delta2 = time_selected - time_now;
                    if(delta2<0)  cal.set(Calendar.DAY_OF_MONTH,day_of_month+7);
                }
                else{
                    cal.set(Calendar.DAY_OF_MONTH,day_of_month+Math.abs(delta));
                }
                count=count+1;
                scheduler_time = cal.getTimeInMillis();

                PendingIntent broadcast = PendingIntent.getBroadcast(ctx, request, futureIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                Objects.requireNonNull(al).setRepeating(AlarmManager.RTC_WAKEUP, scheduler_time, week_interval, broadcast);
                Alarm_List.add(broadcast);
                Alarm_Times.add(scheduler_time);
                for (long t :Alarm_Times){
                    Log.d("AL_TIME",String.valueOf(t));
                }

            }

            Collections.sort(Alarm_Times);

        }
        catch (StringIndexOutOfBoundsException e){
            CharSequence text = "No repeat Days have been selected. Scheduler will never run!";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(appContext, text, duration);
            toast.show();
        }
    }

    public void cancelAlarm(Context ctx){
        AlarmManager al = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
        for (PendingIntent pi : Alarm_List){
            Objects.requireNonNull(al).cancel(pi);
        }
        Alarm_List.clear();
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
