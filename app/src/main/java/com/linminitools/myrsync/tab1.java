package com.linminitools.myrsync;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import static com.linminitools.myrsync.myRsyncApplication.schedulers;

public class tab1 extends Fragment {

    private final Handler h = new Handler();
    private RecyclerView recyclerView;
    private TextView emptyView;
    private Thread t;
    private customAdapter adapter;

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
        adapter = new customAdapter(this.getContext(), schedulers, 1);
        recyclerView.setAdapter(adapter);
        updateEmptyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        t = refresh();
        t.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        t.interrupt();
    }

    private void updateEmptyView() {
        if (schedulers.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private Thread refresh() {
        return new Thread() {
            @Override
            public void run() {
                try {
                    while (!this.isInterrupted()) {
                        h.post(() -> {
                            adapter.refreshAdapter(schedulers);
                            updateEmptyView();
                        });
                        sleep(1000);
                    }
                } catch (InterruptedException e) {
                    // thread interrupted, exit
                }
            }
        };
    }
}
