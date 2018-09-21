package com.linminitools.myrsync;


import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.TimePicker;

import java.util.Objects;

import static com.linminitools.myrsync.MainActivity.configs;
import static com.linminitools.myrsync.MainActivity.schedulers;

public class rsyncJobScheduler extends JobService {

    private static final String TAG = "rsyncJobScheduler";

    @Override
    public boolean onStartJob(JobParameters params) {

        Log.d("JOB_SCHEDULER_START=","TRUE");


        SharedPreferences sched_prefs = getBaseContext().getSharedPreferences("schedulers", MODE_PRIVATE);
        SharedPreferences config_prefs = getBaseContext().getSharedPreferences("configs", MODE_PRIVATE);


        configs.clear();
        schedulers.clear();

        for(int c=1; c<100;c++){
            if (config_prefs.getString("rs_ip_"+String.valueOf(c),"").isEmpty()){
                break;
            }
            else {
                RS_Configuration config = new RS_Configuration(c);
                config.rs_user=config_prefs.getString("rs_user_"+String.valueOf(c),"");
                config.rs_ip = config_prefs.getString("rs_ip_"+String.valueOf(c), "");
                config.rs_port = config_prefs.getString("rs_port_"+String.valueOf(c), "");
                config.rs_options = config_prefs.getString("rs_options_"+String.valueOf(c),"");
                config.rs_module = config_prefs.getString("rs_module_"+String.valueOf(c), "");
                config.local_path = config_prefs.getString("local_path_"+String.valueOf(c), "");
                configs.add(config);
            }
        }


        for (int c = 1; c < 100; c++) {
            if (sched_prefs.getInt("id_" + String.valueOf(c), -1) < 0) {
                break;
            } else {
                TimePicker tp = new TimePicker(getBaseContext());
                tp.setIs24HourView(true);

                tp.setCurrentHour(sched_prefs.getInt("hour_" + String.valueOf(c), 0));
                tp.setCurrentMinute(sched_prefs.getInt("min_" + String.valueOf(c), 0));
                String days = sched_prefs.getString("days_" + String.valueOf(c), "");
                String name = sched_prefs.getString("name_" + String.valueOf(c), "");
                int config_pos = sched_prefs.getInt("config_pos_" + String.valueOf(c), 0);
                Scheduler sched = new Scheduler(days, tp, c);
                sched.name = name;
                sched.config_pos = config_pos;
                schedulers.add(sched);
            }
        }

        int jobid= params.getExtras().getInt("jobid");
        int config_to_run =params.getExtras().getInt("config_pos");
        configs.get(config_to_run).executeConfig(getBaseContext());
        reschedule(config_to_run,jobid);
        jobFinished(params,false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d("JOB_SCHEDULER_STOP=","TRUE");
        return true;
    }

    private void reschedule(int config_pos, int jobid){

        ComponentName serviceComponent = new ComponentName(getBaseContext(), rsyncJobScheduler.class);
        long five_min_in_millis= 300000;
        long next_week_in_millis= 604800000;

        PersistableBundle perbun=new PersistableBundle();
        perbun.putInt("config_pos",config_pos);

        JobInfo.Builder builder= new JobInfo.Builder(jobid, serviceComponent)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .setMinimumLatency(next_week_in_millis)
                .setOverrideDeadline(next_week_in_millis + five_min_in_millis)
                .setRequiresDeviceIdle(false)
                .setRequiresCharging(false)
                .setExtras(perbun)
                .setPersisted(true);
        JobInfo rsyncJob= builder.build();

        JobScheduler jobScheduler = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            jobScheduler = getBaseContext().getSystemService(JobScheduler.class);
        }
        else {
            jobScheduler = (JobScheduler)getBaseContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        }
        Objects.requireNonNull(jobScheduler).schedule(rsyncJob);

        Log.d("JO_SCHEDULER:","2nd RUN");

    }


}
