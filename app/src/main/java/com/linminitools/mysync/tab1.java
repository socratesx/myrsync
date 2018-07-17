package com.linminitools.mysync;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import static android.app.Activity.RESULT_OK;
import static com.linminitools.mysync.MainActivity.schedulers;

public class tab1 extends Fragment {

    Handler h= new Handler();
    ListView status;
    Thread t;

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
        status = view.findViewById(R.id.status_list);
        status.setEmptyView(view.findViewById(android.R.id.empty));
        customAdapter adapter = new customAdapter(this.getContext(),schedulers,1);

        status.setAdapter(adapter);

    }

    @Override
    public void onResume(){
        super.onResume();
        try{
            status.setEmptyView(getView().findViewById(android.R.id.empty));
        }
        catch (Exception e){
            e.printStackTrace();
        }
        t=refresh(getContext());
        t.start();
    }


    @Override
    public void onPause(){
        super.onPause();
        t.interrupt();
        onActivityResult(1,RESULT_OK,null);
        Log.d("ONPAUSE","TRUE");

    }

    public Thread refresh(final Context ctx) {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!this.isInterrupted()) {
                        h.post(new Runnable() {
                            @Override
                            public void run() {
                                customAdapter adapter = new customAdapter(ctx,schedulers,1);
                                status.setAdapter(adapter);
                            }
                        });
                        sleep(2000);
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        };
        return t;
    }



}

