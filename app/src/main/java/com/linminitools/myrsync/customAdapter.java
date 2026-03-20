package com.linminitools.myrsync;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;
import static com.linminitools.myrsync.myRsyncApplication.configs;
import static com.linminitools.myrsync.myRsyncApplication.schedulers;

class customAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final int fromTab;
    private ArrayList<?> data;

    customAdapter(Context context, ArrayList<?> data, int request_code) {
        this.context = context;
        this.fromTab = request_code;
        this.data = data;
    }

    void refreshAdapter(ArrayList<?> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        return fromTab;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        switch (viewType) {
            case 1: {
                View v = inflater.inflate(R.layout.status_row, parent, false);
                return new StatusViewHolder(v);
            }
            case 2: {
                View v = inflater.inflate(R.layout.row, parent, false);
                return new ConfigViewHolder(v);
            }
            case 3: {
                View v = inflater.inflate(R.layout.row_sched, parent, false);
                return new SchedViewHolder(v);
            }
            default:
                throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (fromTab) {
            case 1:
                bindStatus((StatusViewHolder) holder, position);
                break;
            case 2:
                bindConfig((ConfigViewHolder) holder, position);
                break;
            case 3:
                bindSched((SchedViewHolder) holder, position);
                break;
        }
    }

    private void bindStatus(StatusViewHolder vh, int position) {
        Scheduler sched = (Scheduler) data.get(position);
        vh.warningIcon.setVisibility(View.INVISIBLE);
        vh.successIcon.setVisibility(View.INVISIBLE);
        vh.errorIcon.setVisibility(View.INVISIBLE);

        try {
            RS_Configuration conf = configs.get(0);
            for (RS_Configuration c : configs) if (c.id == sched.config_id) conf = c;

            vh.configName.setText(conf.name);
            vh.schedName.setText(sched.name);

            if (context.getSharedPreferences("CMD_" + conf.id, MODE_PRIVATE).getBoolean("is_running", false)
                    && context.getSharedPreferences("schedulers", MODE_PRIVATE).getBoolean("is_running_" + sched.id, false)) {
                vh.nextRun.setText(R.string.running);
            } else {
                vh.nextRun.setText(sched.getNextAlarm(context));
            }

            SharedPreferences result_prefs = context.getSharedPreferences("configs", MODE_PRIVATE);
            String result = result_prefs.getString("last_result_" + conf.id, "Never Run");
            String last_run = result_prefs.getString("last_run_" + conf.id, "Never Run");

            vh.lastRun.setText(last_run);
            vh.result.setText(result);

            if (result.equals("OK")) {
                vh.successIcon.setVisibility(View.VISIBLE);
            } else if (result.equals("Warning! Check Log!") || result.contains("Aborted")) {
                vh.warningIcon.setVisibility(View.VISIBLE);
            } else if (result.startsWith("Error")) {
                vh.errorIcon.setVisibility(View.VISIBLE);
            }
        } catch (IndexOutOfBoundsException e) {
            schedulers.remove(sched);
            sched.deleteFromDisk();
        }
    }

    private void bindConfig(ConfigViewHolder vh, int position) {
        RS_Configuration config = (RS_Configuration) data.get(position);
        vh.name.setText(config.name);
        vh.editBtn.setTag(R.id.bt_edit, position);
        vh.deleteBtn.setTag(R.id.bt_delete, position);
        vh.runBtn.setTag(R.id.bt_run, position);
        vh.cloneBtn.setTag(R.id.bt_clone, position);
    }

    private void bindSched(SchedViewHolder vh, int position) {
        Scheduler sched = (Scheduler) data.get(position);
        vh.name.setText(sched.name);
        vh.editBtn.setTag(R.id.bt_edit_sched, position);
        vh.deleteBtn.setTag(R.id.bt_delete_sched, position);
        vh.time.setText(String.format(Locale.getDefault(), "%02d:%02d", sched.hour, sched.min));

        int[] dayViewIds = {R.id.tv_monday, R.id.tv_tuesday, R.id.tv_wednesday,
                R.id.tv_thursday, R.id.tv_friday, R.id.tv_saturday, R.id.tv_sunday};

        for (int dayId : dayViewIds) {
            TextView tv = vh.itemView.findViewById(dayId);
            tv.setBackgroundResource(0);
            tv.setTextColor(tv.getResources().getColor(android.R.color.darker_gray, tv.getContext().getTheme()));
        }

        String days = sched.days;
        String[] active_days = days.split("[.]");
        for (String d : active_days) {
            if (!d.isEmpty()) {
                int rid = context.getResources().getIdentifier("tv_" + d, "id", context.getPackageName());
                if (rid != 0) {
                    TextView tv_day = vh.itemView.findViewById(rid);
                    tv_day.setBackgroundResource(R.drawable.textview_selector);
                }
            }
        }
    }

    static class StatusViewHolder extends RecyclerView.ViewHolder {
        final TextView configName, schedName, nextRun, lastRun, result;
        final ImageView errorIcon, warningIcon, successIcon;

        StatusViewHolder(View v) {
            super(v);
            configName = v.findViewById(R.id.tv_get_config_name);
            schedName = v.findViewById(R.id.tv_get_scheduler_name);
            nextRun = v.findViewById(R.id.tv_get_nextrun);
            lastRun = v.findViewById(R.id.tv_get_lastrun);
            result = v.findViewById(R.id.tv_get_result);
            errorIcon = v.findViewById(R.id.img_error_status);
            warningIcon = v.findViewById(R.id.img_warning_status);
            successIcon = v.findViewById(R.id.img_success_status);
        }
    }

    static class ConfigViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final View editBtn, deleteBtn, cloneBtn, runBtn;

        ConfigViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.list_item_id);
            editBtn = v.findViewById(R.id.bt_edit);
            deleteBtn = v.findViewById(R.id.bt_delete);
            cloneBtn = v.findViewById(R.id.bt_clone);
            runBtn = v.findViewById(R.id.bt_run);
        }
    }

    static class SchedViewHolder extends RecyclerView.ViewHolder {
        final TextView name, time;
        final View editBtn, deleteBtn;

        SchedViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.tv_sched_name);
            time = v.findViewById(R.id.tv_sched_showtime);
            editBtn = v.findViewById(R.id.bt_edit_sched);
            deleteBtn = v.findViewById(R.id.bt_delete_sched);
        }
    }
}
