package com.linminitools.myrsync;


import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import static com.linminitools.myrsync.myRsyncApplication.schedulers;


public class tab3 extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.tab3, container, false);
        rootView.setTag("tab3");
        Context c = getContext();
        ListView lv_scheds = rootView.findViewById(R.id.lv_schedulers);
        lv_scheds.setEmptyView(rootView.findViewById(android.R.id.empty));
        customAdapter adapter = new customAdapter(c,schedulers,3);

        lv_scheds.setAdapter(adapter);
        return rootView;
    }



}

