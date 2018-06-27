package com.linminitools.mysync;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import static com.linminitools.mysync.MainActivity.appContext;
import static com.linminitools.mysync.MainActivity.configs;
import static com.linminitools.mysync.MainActivity.schedulers;


public class tab2 extends android.support.v4.app.Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
            ViewGroup rootView = (android.view.ViewGroup) inflater.inflate(
                R.layout.tab2, container, false);

        Context c = getContext();
        final ListView lv_scheds = (ListView) rootView.findViewById(R.id.lv_schedulers);

        lv_scheds.setEmptyView(rootView.findViewById(android.R.id.empty));

        customAdapter adapter= new customAdapter(c,schedulers);

        lv_scheds.setAdapter(adapter);

        return rootView;
    }


}

