package com.linminitools.mysync;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.support.v4.app.AlarmManagerCompat;
import android.support.v7.util.SortedList;
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
import java.util.Map;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.linminitools.mysync.MainActivity.appContext;
import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.getInstance;

public class Scheduler extends MainActivity{

    //ArrayList<String> days;
    TimePicker d;
    String days,name;
    int id, hour, min, config_pos;
    private String today;
    private long triggerTime;
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

    protected void update(){
        this.hour=d.getCurrentHour();
        this.min=d.getCurrentMinute();
    }


    protected void saveToDisk(){

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

    protected void deleteFromDisk(){
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


        Log.d("DATE", String.valueOf(cal.get(DAY_OF_WEEK)));
        Log.d("HOUR_SELECTED", String.valueOf(scheduler_time));

        Intent futureIntent = new Intent();
        futureIntent.setAction("android.media.action.DISPLAY_NOTIFICATION");
        futureIntent.addCategory("com.linminitools.mysync");
        futureIntent.putExtra("Time",scheduler_time);
        futureIntent.putExtra("config",config_pos);

        String[] all_days={"SUNDAY","MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY"};
        String[] days_list={""};
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
                Log.d("DELTA_",String.valueOf(cal.get(DAY_OF_WEEK))+" "+String.valueOf(weekdays.get(d)));
                int request = (100 * this.id) + weekdays.get(d);
                cal.set(Calendar.HOUR_OF_DAY, this.d.getCurrentHour());
                cal.set(Calendar.MINUTE, this.d.getCurrentMinute());
                if (delta>0) {
                    cal.set(Calendar.DAY_OF_MONTH,day_of_month-Math.abs(delta)+7);
                    Log.d("HOUR_0", "DELTA>0 " +String.valueOf(delta) + " " +String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
                    Log.d("COUNTER",String.valueOf(count));
                }
                else if (delta==0){
                    Calendar now = Calendar.getInstance();
                    long time_now=now.getTimeInMillis();
                    long delta2 = time_selected - time_now;
                    if(delta2<0)  cal.set(Calendar.DAY_OF_MONTH,day_of_month+7);
                    Log.d("COUNTER",String.valueOf(count));
                    Log.d("HOUR_0", "DELTA=0 " +String.valueOf(delta) + " "+String.valueOf(cal.get(Calendar.DAY_OF_MONTH)) );
                }
                else{
                    cal.set(Calendar.DAY_OF_MONTH,day_of_month+Math.abs(delta));
                    Log.d("HOUR_0", "DELTA<0 "+ String.valueOf(delta) + " " +String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
                    Log.d("COUNTER",String.valueOf(count));
                }
                count=count+1;
                scheduler_time = cal.getTimeInMillis();
                Log.d("HOUR_1", String.valueOf(scheduler_time));

                PendingIntent broadcast = PendingIntent.getBroadcast(ctx, request, futureIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                al.setRepeating(AlarmManager.RTC_WAKEUP, scheduler_time, week_interval, broadcast);
                Alarm_List.add(broadcast);
                Alarm_Times.add(scheduler_time);

            }

            Collections.sort(Alarm_Times);
            Log.d("SORTED_LIST", Alarm_Times.toString());

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
            al.cancel(pi);
        }
        Alarm_List.clear();
    }

    public List<PendingIntent> getAlarmList(){
        return Alarm_List;
    }

    public String getNextAlarm(){
        Calendar now = Calendar.getInstance();
        long time = now.getTimeInMillis();

        Log.d("COMPARE___",String.valueOf(now.get(Calendar.DAY_OF_WEEK)));
        int today = now.get(Calendar.DAY_OF_WEEK);

        Map<Integer,String> all_days= new HashMap();
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
            List<String> sched_days = new ArrayList<String>(Arrays.asList(days_list));
            long next_time = 0;
            int count = 0;
            Calendar scheduler_time = Calendar.getInstance();
            scheduler_time.set(Calendar.HOUR_OF_DAY,this.hour);
            scheduler_time.set(Calendar.MINUTE,this.min);

            long delta = scheduler_time.getTimeInMillis() - time;
            Log.d("DELTA", String.valueOf(delta));
            if(delta>0) {
                while (!sched_days.contains(all_days.get(today))) {
                    Log.d("DELTA", "WENT INTO THE WHILE"+today);
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
                Log.d("NEXT_TIME", String.valueOf(next_time));

            time = time + next_time;
            if(next_time<0) time=time+604800000;

            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm");

            return formatter.format(time);
        }
        catch (StringIndexOutOfBoundsException e){
            return "Never";
        }

    }


}
