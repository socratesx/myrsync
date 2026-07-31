package com.linminitools.myrsync;


import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.getInstance;

/**
 * Schedule model + AlarmManager wiring. Plain class (not an Activity) so it is safe to
 * construct from receivers / background threads.
 */
public class Scheduler implements Comparable<Scheduler> {

    TimePicker d;
    String days, name, wifi_ssid;
    int id;
    int hour;
    int min;
    int config_id;
    private List<Long> Alarm_Times;
    Long addedOn;
    Boolean wifi_sw;


    public Scheduler() {
        this.name = "Scheduler";
    }

    public Scheduler(String days, TimePicker d, int id) {
        this.days = days;
        this.d = d;
        this.id = id;
        this.hour = d.getHour();
        this.min = d.getMinute();
        this.Alarm_Times = new ArrayList<>();
    }


    public int compareTo(@NonNull Scheduler s) {
        return Long.compare(this.addedOn, s.addedOn);
    }

    /** Schedulers for a configuration, sorted by id (stable slot order). */
    @NonNull
    public static ArrayList<Scheduler> schedulersForConfig(int configId) {
        ArrayList<Scheduler> out = new ArrayList<>();
        for (Scheduler s : myRsyncApplication.schedulers) {
            if (s.config_id == configId) out.add(s);
        }
        Collections.sort(out, (a, b) -> Integer.compare(a.id, b.id));
        return out;
    }

    /**
     * Countdown to the soonest fire among schedulers for the Overview.
     * Uses the same {@link #getNextAlarmFireMillis} basis as {@link #getNextAlarm(Context)} —
     * cached {@code Alarm_Times} with past-time pruning — so the target instant stays stable
     * while only "now" changes each tick.
     */
    public static String earliestNextRunLabel(@NonNull Context ctx, @NonNull List<Scheduler> list) {
        if (list.isEmpty()) return ctx.getString(R.string.no_schedule);
        long bestT = Long.MAX_VALUE;
        int bestId = Integer.MAX_VALUE;
        for (Scheduler s : list) {
            long t = s.getNextAlarmFireMillis(ctx);
            if (t < bestT || (t == bestT && s.id < bestId)) {
                bestT = t;
                bestId = s.id;
            }
        }
        if (bestT == Long.MAX_VALUE) return ctx.getString(R.string.no_schedule);
        long nextTime = bestT - Calendar.getInstance().getTimeInMillis();
        return formatRemainingCountdown(ctx, nextTime);
    }

    /** Same formatting as legacy {@link #getNextAlarm(Context)} (incl. hours % 24 / minutes % 60). */
    static String formatRemainingCountdown(@NonNull Context ctx, long nextTimeMs) {
        // Only the last minute before a future fire shows "About to start".
        if (nextTimeMs > 60_000L) {
            long seconds = nextTimeMs / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;
            return days + "days  " + hours % 24 + "h  " + minutes % 60 + "m  " + seconds % 60 + "s";
        }
        if (nextTimeMs > 0) {
            return ctx.getString(R.string.about_to_start);
        }
        // Past/zero should not be returned by {@link #getNextAlarmFireMillis}; treat as no schedule.
        return ctx.getString(R.string.about_to_start);
    }

    /** Drop cached fire times so the next Overview tick recomputes from wall-clock + prefs. */
    public void invalidateCachedAlarmTimes() {
        if (Alarm_Times == null) {
            Alarm_Times = new ArrayList<>();
        } else {
            Alarm_Times.clear();
        }
    }

    /** Invalidate the in-memory scheduler used by the Overview after AlarmManager reschedule. */
    public static void refreshInMemoryAlarmCache(int schedulerId) {
        for (Scheduler s : myRsyncApplication.schedulers) {
            if (s.id == schedulerId) {
                s.invalidateCachedAlarmTimes();
                return;
            }
        }
    }

    /**
     * Absolute time (ms) used for the overview countdown and {@link #getNextAlarm(Context)}.
     * Never returns a time that is already past or already covered by {@code last_run_}.
     */
    long getNextAlarmFireMillis(Context ctx) {
        if (Alarm_Times == null) {
            Alarm_Times = new ArrayList<>();
        }
        if (Alarm_Times.isEmpty()) {
            Alarm_Times = this.calculate_alarm_times(ctx);
        }

        SharedPreferences prefs = ctx.getSharedPreferences("schedulers", Context.MODE_PRIVATE);
        long lastRun = prefs.getLong("last_run_" + this.id, -1);
        long now = System.currentTimeMillis();

        pruneStaleAlarmTimes(now, lastRun);
        if (Alarm_Times.isEmpty()) {
            Alarm_Times = this.calculate_alarm_times(ctx);
            pruneStaleAlarmTimes(now, lastRun);
        }

        if (Alarm_Times == null || Alarm_Times.isEmpty()) {
            return Long.MAX_VALUE;
        }
        return Alarm_Times.get(0);
    }

    /** Remove cached times that are past or already consumed by a completed run. */
    private void pruneStaleAlarmTimes(long nowMs, long lastRunMs) {
        if (Alarm_Times == null || Alarm_Times.isEmpty()) {
            return;
        }
        Iterator<Long> it = Alarm_Times.iterator();
        while (it.hasNext()) {
            long t = it.next();
            boolean alreadyPast = t <= nowMs;
            boolean alreadyRan = lastRunMs > 0 && t <= lastRunMs;
            if (alreadyPast || alreadyRan) {
                it.remove();
            } else {
                break;
            }
        }
    }

    void update() {
        this.hour = d.getHour();
        this.min = d.getMinute();
    }


    @SuppressLint("ApplySharedPref")
    void saveToDisk() {
        Context appContext = myRsyncApplication.appContext;
        SharedPreferences prefs = appContext.getSharedPreferences("schedulers", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefseditor = prefs.edit();

        this.addedOn = prefs.getLong("addedon_" + String.valueOf(this.id), 0);
        if (this.addedOn == 0) this.addedOn = Calendar.getInstance().getTimeInMillis();

        prefseditor.putInt("id_" + String.valueOf(this.id), this.id);
        prefseditor.putInt("hour_" + String.valueOf(this.id), this.hour);
        prefseditor.putInt("min_" + String.valueOf(this.id), this.min);
        prefseditor.putString("days_" + String.valueOf(this.id), this.days);
        prefseditor.putString("name_" + String.valueOf(this.id), this.name);
        prefseditor.putString("ssid_" + String.valueOf(this.id), this.wifi_ssid);
        prefseditor.putInt("config_id_" + String.valueOf(this.id), this.config_id);
        prefseditor.putLong("addedon_" + String.valueOf(this.id), this.addedOn);
        prefseditor.putLong("last_run_" + String.valueOf(this.id), -1);
        prefseditor.putLong("last_save_" + String.valueOf(this.id), Calendar.getInstance().getTimeInMillis());
        prefseditor.putBoolean("is_running_" + String.valueOf(this.id), false);
        prefseditor.putBoolean("wifi_switch_" + String.valueOf(this.id), this.wifi_sw);
        prefseditor.commit();

        try {
            File debugLog = new File(appContext.getApplicationInfo().dataDir + "/debug.log");
            FileWriter debug_writer = new FileWriter(debugLog, true);
            Locale current_locale = AppLocales.of(appContext);
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);
            CharSequence message = "\n\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + "SCHEDULER SAVED: " + this.name + "{" +
                    "\nID: " + this.id + "\nTIME: " + this.hour + ":" + this.min + "\nDAYS: " + this.days + "\n}";
            debug_writer.append(message);
            debug_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @SuppressLint("ApplySharedPref")
    void deleteFromDisk() {
        Context appContext = myRsyncApplication.appContext;
        SharedPreferences prefs = appContext.getSharedPreferences("schedulers", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefseditor = prefs.edit();

        prefseditor.remove("id_" + String.valueOf(this.id));
        prefseditor.remove("hour_" + String.valueOf(this.id));
        prefseditor.remove("min_" + String.valueOf(this.id));
        prefseditor.remove("days_" + String.valueOf(this.id));
        prefseditor.remove("name_" + String.valueOf(this.id));
        prefseditor.remove("config_id_" + String.valueOf(this.id));
        prefseditor.remove("addedon_" + String.valueOf(this.id));
        prefseditor.remove("last_run_" + String.valueOf(this.id));
        prefseditor.remove("last_save_" + String.valueOf(this.id));
        prefseditor.remove("is_running_" + String.valueOf(this.id));
        prefseditor.remove("ssid_" + String.valueOf(this.id));
        prefseditor.remove("wifi_switch_" + String.valueOf(this.id));

        prefseditor.commit();

        try {
            File debugLog = new File(appContext.getApplicationInfo().dataDir + "/debug.log");
            FileWriter debug_writer = new FileWriter(debugLog, true);
            Locale current_locale = AppLocales.of(appContext);
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);
            CharSequence message = "\n\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + "SCHEDULER DELETED: " + this.name;
            debug_writer.append(message);
            debug_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private List<Long> calculate_alarm_times(Context ctx) {
        if (Alarm_Times == null) {
            Alarm_Times = new ArrayList<>();
        }
        if (!Alarm_Times.isEmpty()) Alarm_Times.clear();

        Calendar cal = Calendar.getInstance();
        Calendar saved_cal = new GregorianCalendar();
        cal.setFirstDayOfWeek(Calendar.MONDAY);

        cal.set(Calendar.HOUR_OF_DAY, d.getHour());
        cal.set(Calendar.MINUTE, d.getMinute());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
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


            for (String dayName : days_list) {
                Integer weekday = weekdays.get(dayName);
                if (weekday == null) continue;
                int delta = today_number - weekday;
                cal = getInstance();

                cal.set(Calendar.HOUR_OF_DAY, this.d.getHour());
                cal.set(Calendar.MINUTE, this.d.getMinute());
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
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
                scheduler_time = cal.getTimeInMillis();

                Alarm_Times.add(scheduler_time);
            }
            Collections.sort(Alarm_Times);
            return Alarm_Times;

        } catch (StringIndexOutOfBoundsException e) {
            CharSequence text = "No repeat Days have been selected. Scheduler will never run!";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(ctx.getApplicationContext(), text, duration);
            toast.show();
            this.deleteFromDisk();

            return this.Alarm_Times = new ArrayList<>();
        }
    }

    public void setAlarm(Context ctx) {
        // Drop any previous inexact/repeating PendingIntents for this scheduler first.
        cancelAlarm(ctx);

        ArrayList<Long> alarms = (ArrayList<Long>) this.calculate_alarm_times(ctx);
        if (alarms == null || alarms.isEmpty()) {
            return;
        }

        int jobcount = this.id * 10;
        AlarmManager alarmMgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (alarmMgr == null) {
            return;
        }

        String Debug_log_path = ctx.getApplicationInfo().dataDir + "/debug.log";
        File debugLog = new File(Debug_log_path);

        try {
            FileWriter debug_writer = new FileWriter(debugLog, true);
            Locale current_locale = AppLocales.of(ctx);
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd/MM HH:mm", current_locale);
            CharSequence message = "\n\n\n[ " + formatter.format(Calendar.getInstance().getTime()) + " ] " + "Scheduler::setAlarm { ";
            debug_writer.append(message);

            for (long t : alarms) {
                jobcount = jobcount + 1;
                PendingIntent pi = buildAlarmPendingIntent(ctx, jobcount);
                scheduleExactWakeup(ctx, alarmMgr, t, pi);
                message = "\nAlarm Time Set :" + formatter.format(t);
                debug_writer.append(message);
            }
            message = "\n} ";
            debug_writer.append(message);
            debug_writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancelAlarm(Context ctx) {
        ArrayList<Long> alarms = (ArrayList<Long>) this.calculate_alarm_times(ctx);
        if (alarms == null) {
            return;
        }

        int jobcount = this.id * 10;
        AlarmManager alarmMgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (alarmMgr == null) {
            return;
        }

        // Cancel a stable range of request codes for this scheduler id (up to 7 days + spare).
        for (int i = 1; i <= 8; i++) {
            PendingIntent pi = buildAlarmPendingIntent(ctx, jobcount + i);
            alarmMgr.cancel(pi);
            pi.cancel();
        }
    }

    private PendingIntent buildAlarmPendingIntent(Context ctx, int requestCode) {
        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.setAction(Intent.ACTION_RUN);
        i.putExtra("config_id", this.config_id);
        i.putExtra("sched_id", this.id);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(ctx, requestCode, i, pendingFlags);
    }

    /**
     * Prefer {@link AlarmManager#setAlarmClock} (most reliable on OEM/Doze devices).
     * Falls back when {@code SCHEDULE_EXACT_ALARM} is not granted so we never leave
     * the user with zero alarms after cancel+reschedule.
     */
    private static void scheduleExactWakeup(Context ctx, AlarmManager alarmMgr, long triggerAtMillis,
                                            PendingIntent operation) {
        boolean canExact = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canExact = alarmMgr.canScheduleExactAlarms();
        }

        try {
            if (canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Intent showIntent = new Intent(ctx, MainActivity.class);
                int showFlags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    showFlags |= PendingIntent.FLAG_IMMUTABLE;
                }
                PendingIntent showPi = PendingIntent.getActivity(
                        ctx, operation.hashCode(), showIntent, showFlags);
                alarmMgr.setAlarmClock(
                        new AlarmManager.AlarmClockInfo(triggerAtMillis, showPi), operation);
                return;
            }
            if (canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmMgr.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
                return;
            }
            if (canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmMgr.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
                return;
            }
        } catch (SecurityException e) {
            Log.w("Scheduler", "Exact alarm denied; using inexact fallback", e);
        }

        // Inexact but still better than weekly setRepeating with a multi-day window.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
        } else {
            alarmMgr.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
        }
    }

    /** True when the OS will honor exact / alarm-clock schedules. */
    public static boolean canScheduleExactAlarms(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager alarmMgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        return alarmMgr != null && alarmMgr.canScheduleExactAlarms();
    }

    public String getNextAlarm(Context ctx) {
        long fire = getNextAlarmFireMillis(ctx);
        if (fire == Long.MAX_VALUE) return "Never";
        long nextTime = fire - Calendar.getInstance().getTimeInMillis();
        return formatRemainingCountdown(ctx, nextTime);
    }

    /** Rebuild a scheduler from SharedPreferences (for receivers / reschedule). */
    @NonNull
    public static Scheduler fromPrefs(@NonNull Context ctx, int schedulerId) {
        SharedPreferences sched_prefs = ctx.getSharedPreferences("schedulers", Context.MODE_PRIVATE);
        String id = String.valueOf(schedulerId);
        TimePicker tp = new TimePicker(ctx);
        tp.setIs24HourView(true);
        tp.setHour(sched_prefs.getInt("hour_" + id, 0));
        tp.setMinute(sched_prefs.getInt("min_" + id, 0));
        String days = sched_prefs.getString("days_" + id, "");
        Scheduler sched = new Scheduler(days, tp, schedulerId);
        sched.name = sched_prefs.getString("name_" + id, "");
        sched.addedOn = sched_prefs.getLong("addedon_" + id, 0);
        sched.config_id = sched_prefs.getInt("config_id_" + id, 0);
        sched.wifi_sw = sched_prefs.getBoolean("wifi_switch_" + id, false);
        sched.wifi_ssid = sched_prefs.getString("ssid_" + id, "");
        return sched;
    }
}
