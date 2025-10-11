package com.linminitools.myrsync;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.Objects;

import static android.app.Activity.RESULT_OK;
import static com.linminitools.myrsync.myRsyncApplication.schedulers;

public class tab1 extends Fragment {

    private static final long REFRESH_INTERVAL_MS = 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (adapter != null) {
                adapter.refreshAdapter(schedulers);
            }
            if (isAdded() && isResumed()) {
                handler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        }
    };

    private ListView status;
    private customAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tab1, container, false);
        rootView.setTag("tab1");
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        status = view.findViewById(R.id.status_list);
        status.setEmptyView(view.findViewById(android.R.id.empty));

        adapter = new customAdapter(this.getContext(), schedulers, 1);
        status.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            status.setEmptyView(Objects.requireNonNull(getView()).findViewById(android.R.id.empty));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        handler.removeCallbacks(refreshRunnable);
        handler.post(refreshRunnable);
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(refreshRunnable);
        super.onPause();
        onActivityResult(1, RESULT_OK, null);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(refreshRunnable);
        adapter = null;
        status = null;
        super.onDestroyView();
    }
}
