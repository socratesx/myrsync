package com.linminitools.myrsync;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.util.SparseArrayCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;
import static com.linminitools.myrsync.myRsyncApplication.configs;
import static com.linminitools.myrsync.myRsyncApplication.schedulers;


class customAdapter extends BaseAdapter {

    private final Context context;
    private static LayoutInflater inflater = null;
    private final int fromTab;

    private final SparseArrayCompat<Object> viewHolder = new SparseArrayCompat<>();



    customAdapter(Context context, ArrayList<?> data, int request_code) {
        this.context = context;
        this.fromTab=request_code;

        if (!data.isEmpty()) {
            for (Object c : data) viewHolder.put(data.indexOf(c),c);
        }

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void refreshAdapter(ArrayList<?> newData){
        viewHolder.clear();
        if (!newData.isEmpty()) {
            for (Object c : newData) viewHolder.put(newData.indexOf(c),c);
        }
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return viewHolder.size();
    }

    @Override
    public Object getItem(int position) {
        return viewHolder.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        //if (vi == null)
        switch (this.fromTab) {
            case 1: {

                vi = inflater.inflate(R.layout.status_row, parent, false);
                TextView tv_conf_name = vi.findViewById(R.id.tv_get_config_name);
                TextView tv_sched_name = vi.findViewById(R.id.tv_get_scheduler_name);
                TextView tv_next_run = vi.findViewById(R.id.tv_get_nextrun);
                TextView tv_last_run = vi.findViewById(R.id.tv_get_lastrun);
                TextView tv_result = vi.findViewById(R.id.tv_get_result);
                ImageView error = vi.findViewById(R.id.img_error_status);
                error.setVisibility(View.INVISIBLE);

                Scheduler sched = (Scheduler) getItem(position);

                try {
                    RS_Configuration conf = configs.get(0);
                    for (RS_Configuration c : configs) if (c.id == sched.config_id) conf = c;
                    tv_conf_name.setText(conf.name);
                    tv_sched_name.setText(sched.name);
                    if (context.getSharedPreferences("CMD_" + String.valueOf(conf.id), MODE_PRIVATE).getBoolean("is_running", false)
                            && context.getSharedPreferences("schedulers", MODE_PRIVATE).getBoolean("is_running_" + String.valueOf(sched.id), false))
                        tv_next_run.setText(R.string.running);
                    else tv_next_run.setText(sched.getNextAlarm(context));

                    SharedPreferences result_prefs = context.getSharedPreferences("configs", MODE_PRIVATE);

                    String result = result_prefs.getString("last_result_" + String.valueOf(conf.id), "Never Run");
                    String last_run = result_prefs.getString("last_run_" + String.valueOf(conf.id), "Never Run");
                    tv_last_run.setText(last_run);
                    tv_result.setText(result);

                    if (result.equals("OK")) {
                        vi.findViewById(R.id.img_success_status).setVisibility(View.VISIBLE);
                    } else if (result.equals("Warning! Check Log!")) {

                        error.setVisibility(View.VISIBLE);
                    }
                } catch (IndexOutOfBoundsException e) {
                    schedulers.remove(sched);
                    sched.deleteFromDisk();
                }
                break;
            }
            case 2:
                vi = inflater.inflate(R.layout.row, parent, false);
                TextView text = vi.findViewById(R.id.list_item_id);
                text.setText(((RS_Configuration) getItem(position)).name);
                vi.findViewById(R.id.bt_edit).setTag(R.id.bt_edit, position);
                vi.findViewById(R.id.bt_delete).setTag(R.id.bt_delete, position);
                break;
            case 3: {
                vi = inflater.inflate(R.layout.row_sched, parent, false);
                Scheduler sched = (Scheduler) getItem(position);
                TextView tv_name = vi.findViewById(R.id.tv_sched_name);
                tv_name.setText(sched.name);

                vi.findViewById(R.id.bt_edit_sched).setTag(R.id.bt_edit_sched, position);
                vi.findViewById(R.id.bt_delete_sched).setTag(R.id.bt_delete_sched, position);

                TextView tv_time = vi.findViewById(R.id.tv_sched_showtime);

                tv_time.setText(String.format(Locale.getDefault(), "%02d:%02d", sched.hour, sched.min));


                String days = sched.days;
                String[] active_days = days.split("[.]");

                for (String d : active_days) {
                    if (!d.isEmpty()) {
                        int rid = context.getResources().getIdentifier("tv_" + d, "id", context.getPackageName());
                        TextView tv_day = vi.findViewById(rid);
                        tv_day.setBackgroundResource(R.drawable.textview_selector);


                    }

                }


                break;
            }
        }

        return vi;
    }


}