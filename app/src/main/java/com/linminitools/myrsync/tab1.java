package com.linminitools.myrsync;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import static com.linminitools.myrsync.myRsyncApplication.configs;

public class tab1 extends Fragment {

    private final Handler h = new Handler(Looper.getMainLooper());
    private RecyclerView recyclerView;
    private TextView emptyView;
    private customAdapter adapter;

    /**
     * Main-loop ticker (same 2s cadence as pre–RecyclerView refresh). Updates only {@code now} in
     * the countdown while {@link Scheduler#getNextAlarmFireMillis} keeps a stable next-fire instant.
     */
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isResumed() || adapter == null || recyclerView == null) {
                return;
            }
            adapter.refreshAdapter(configs);
            updateEmptyView();
            h.postDelayed(this, 1000);
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tab1, container, false);
        rootView.setTag("tab1");
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.status_list);
        emptyView = view.findViewById(R.id.empty_view);
        recyclerView.setItemAnimator(null);
        adapter = new customAdapter(this.getContext(), configs, 1);
        recyclerView.setAdapter(adapter);
        updateEmptyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        h.removeCallbacks(tickRunnable);
        h.post(tickRunnable);
    }

    @Override
    public void onPause() {
        h.removeCallbacks(tickRunnable);
        super.onPause();
    }

    private void updateEmptyView() {
        if (configs.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
}
