package com.linminitools.mysync;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import static com.linminitools.mysync.MainActivity.configs;



public class tab2 extends android.support.v4.app.Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.tab2, container, false);
        rootView.setTag("tab2");
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Context c = this.getContext();
        ListView lv_configs = view.findViewById(R.id.lv_configs);
        lv_configs.setEmptyView(view.findViewById(android.R.id.empty));
        customAdapter adapter = new customAdapter(c,configs,2);

        lv_configs.setAdapter(adapter);
    }

}

