package com.linminitools.myrsync;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.Objects;

import static android.app.Activity.RESULT_OK;
import static com.linminitools.myrsync.myRsyncApplication.schedulers;

public class tab1 extends Fragment {

    private final Handler h= new Handler();
    private ListView status;
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
        // Setup any handles to view objects here
        //        // EditText etFoo = (EditText) view.findViewById(R.id.etFoo);
        status = view.findViewById(R.id.status_list);
        status.setEmptyView(view.findViewById(android.R.id.empty));

        adapter = new customAdapter(this.getContext(),schedulers,1);

        status.setAdapter(adapter);
    }


    @Override
    public void onResume(){
        super.onResume();
        try{
            status.setEmptyView(Objects.requireNonNull(getView()).findViewById(android.R.id.empty));
        }
        catch (Exception e){
            e.printStackTrace();
        }
        t=refresh();
        t.start();
    }


    @Override
    public void onPause(){
        super.onPause();
        t.interrupt();
        onActivityResult(1,RESULT_OK,null);

    }


    private Thread refresh() {
        return new Thread() {
            @Override
            public void run() {
                try {
                    while (!this.isInterrupted()) {
                        h.post(new Runnable() {
                            @Override
                            public void run() {
                                adapter.refreshAdapter(schedulers);

                            }
                        });
                        sleep(1000);
                    }
                }
                catch(InterruptedException e){
                    e.getMessage();
                }
            }
        };
    }






}

