package com.linminitools.myrsync;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import static com.linminitools.myrsync.myRsyncApplication.schedulers;

public class tab2 extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tab2, container, false);
        rootView.setTag("tab2");
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        RecyclerView rv = view.findViewById(R.id.lv_schedulers);
        TextView emptyView = view.findViewById(R.id.empty_view);
        customAdapter adapter = new customAdapter(this.getContext(), schedulers, 3);
        rv.setAdapter(adapter);

        if (schedulers.isEmpty()) {
            rv.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
}
