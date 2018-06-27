package com.linminitools.mysync;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import static com.linminitools.mysync.MainActivity.configs;



public class tab3 extends android.support.v4.app.Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.tab3, container, false);

        Context c = getContext();
        final ListView lv_configs = (ListView) rootView.findViewById(R.id.lv_configs);
        lv_configs.setEmptyView(rootView.findViewById(android.R.id.empty));
        customAdapter adapter = new customAdapter(c,configs);

        lv_configs.setAdapter(adapter);
        return rootView;
    }



}

