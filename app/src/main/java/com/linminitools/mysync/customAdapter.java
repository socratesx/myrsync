package com.linminitools.mysync;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.AlarmManagerCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static com.linminitools.mysync.MainActivity.appContext;
import static com.linminitools.mysync.MainActivity.configs;


class customAdapter extends BaseAdapter {

        Context context;
        String[] data;
        private static LayoutInflater inflater = null;
        private int fromTab;
        private ArrayList<Scheduler> original_data = new ArrayList<>();



        public customAdapter(Context context, ArrayList<?> data, int request_code) {
        this.context = context;
        this.data=new String[data.size()];
        this.fromTab=request_code;

        Log.d("CONTEXT", context.toString());


        if (!data.isEmpty()) {
            for (Object c : data) {

                if (fromTab==2) this.data[data.indexOf(c)] = ((RS_Configuration) c).name;
                else if (fromTab==1 || fromTab==3) {
                    this.data[data.indexOf(c)] = c.getClass().getName();
                    original_data.add(((Scheduler) c));
                }

            }

        }





        inflater = (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
        // TODO Auto-generated method stub
        return data.length;
        }

        @Override
        public Object getItem(int position) {
        // TODO Auto-generated method stub
        return data[position];
        }

        @Override
        public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
        }


        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View vi = convertView;
            if (vi == null)

                if(this.fromTab==1) {

                    vi = inflater.inflate(R.layout.status_row, null);
                    TextView tv_conf_name = vi.findViewById(R.id.tv_get_config_name);
                    TextView tv_sched_name = vi.findViewById(R.id.tv_get_scheduler_name);
                    TextView tv_next_run = vi.findViewById(R.id.tv_get_nextrun);
                    TextView tv_last_run = vi.findViewById(R.id.tv_get_lastrun);
                    TextView tv_result = vi.findViewById(R.id.tv_get_result);
                    ImageView error = vi.findViewById(R.id.img_error_status);
                    error.setVisibility(View.INVISIBLE);

                    Scheduler sched = original_data.get(position);
                    RS_Configuration conf;
                    try{
                        conf= configs.get(sched.config_pos);
                    }catch (IndexOutOfBoundsException e){
                        conf= configs.get(0);
                        sched.config_pos=conf.id;
                    }
                    tv_conf_name.setText(conf.name);
                    tv_sched_name.setText(sched.name);
                    tv_next_run.setText(sched.getNextAlarm());

                    SharedPreferences result_prefs = context.getSharedPreferences("configs", MODE_PRIVATE);
                    Log.d("SHAREDPREFS", "last_result_" + String.valueOf(conf.id));
                    Log.d("CONTEXT", context.toString());
                    String result = result_prefs.getString("last_result_" + String.valueOf(conf.id), "Never Run");
                    String last_run = result_prefs.getString("last_run_" + String.valueOf(conf.id), "Never Run");
                    tv_last_run.setText(last_run);
                    tv_result.setText(result);

                    if (result.equals("OK")) {
                        vi.findViewById(R.id.img_success_status).setVisibility(View.VISIBLE);
                    } else if (result.equals("Warning! Check Log!")) {
                        Log.d("RESULT_1", result);
                        error.setVisibility(View.VISIBLE);
                    }
                }

                else if (this.fromTab==2) {
                    vi = inflater.inflate(R.layout.row, null);
                    TextView text = vi.findViewById(R.id.list_item_id);
                    text.setText(data[position]);
                    vi.findViewById(R.id.bt_edit).setTag(R.id.bt_edit, position);
                    vi.findViewById(R.id.bt_delete).setTag(R.id.bt_delete, position);
                }
                else if (this.fromTab==3 ) {
                    vi = inflater.inflate(R.layout.row_sched, null);

                    Scheduler sched = original_data.get(position);
                    TextView tv_name = vi.findViewById(R.id.tv_sched_name);
                    tv_name.setText(sched.name);

                    vi.findViewById(R.id.bt_edit_sched).setTag(R.id.bt_edit_sched, position);
                    vi.findViewById(R.id.bt_delete_sched).setTag(R.id.bt_delete_sched, position);

                    TextView tv_time = vi.findViewById(R.id.tv_sched_showtime);
                    tv_time.setText(String.valueOf(sched.hour) + ":" + String.valueOf(sched.min));


                    String days = sched.days;
                    Log.d ("DAYS",days);
                    Log.d ("NAME",sched.name);
                    String[] active_days = days.split("[.]");
                    Log.d("ACTIVE DAYS", String.valueOf(active_days.length));

                    for (int i=0; i<active_days.length;i++) {
                        Log.d("ACTIVE DAYS", String.valueOf(active_days[i]));
                        }

                        for (String d : active_days) {
                        if (!d.isEmpty()) {
                            int rid = context.getResources().getIdentifier("tv_" + d, "id", context.getPackageName());
                            TextView tv_day = vi.findViewById(rid);
                            tv_day.setBackgroundColor(R.drawable.rectangle);

                        }

                    }


                }

        return vi;
        }
}