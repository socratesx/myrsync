package com.linminitools.mysync;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import static android.app.Activity.RESULT_OK;
import static com.linminitools.mysync.MainActivity.schedulers;

public class tab1 extends android.support.v4.app.Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tab1, container, false);
        rootView.setTag("tab1");

        return rootView;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Setup any handles to view objects here
        //        // EditText etFoo = (EditText) view.findViewById(R.id.etFoo);
        ListView status = view.findViewById(R.id.status_list);
        status.setEmptyView(view.findViewById(android.R.id.empty));
        customAdapter adapter = new customAdapter(this.getContext(),schedulers,1);

        status.setAdapter(adapter);

    }
    @Override
    public void onPause(){
        super.onPause();
        onActivityResult(1,RESULT_OK,null);
        Log.d("ONPAUSE","TRUE");
        //getFragmentManager().beginTransaction().replace(R.id.pager,new tab1(),"tab1").commit();
    }

}

