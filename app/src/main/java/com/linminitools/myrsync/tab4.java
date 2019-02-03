package com.linminitools.myrsync;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;
import static com.linminitools.myrsync.MainActivity.appContext;

public class tab4 extends Fragment{

        private TextView tv_log;
        private final Handler h= new Handler();
        private File log_file;
        private Thread log_thread;
        private SparseArray<String> selected_logfile = new SparseArray<>();
        private int selected_item=-1;
        private  Spinner sp;

    @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tab4, container, false);
        rootView.setTag("tab4");

        SharedPreferences prefs = Objects.requireNonNull(getContext()).getSharedPreferences("Rsync_Command_build", MODE_PRIVATE);

        sp = rootView.findViewById(R.id.sp_selected_logfile);
        List<String> listLoadToSpinner = new ArrayList<>();
        listLoadToSpinner.add("rsync");
        listLoadToSpinner.add("debug");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(appContext, android.R.layout.simple_spinner_dropdown_item, listLoadToSpinner);
        if (selected_item==-1) selected_item=0;
        sp.setAdapter(spinnerAdapter);
        sp.setSelection(selected_item);


        String log = prefs.getString("log", appContext.getApplicationInfo().dataDir + "/logfile.log");

        if (sp.getSelectedItem().equals("debug")){
            log = appContext.getApplicationInfo().dataDir + "/debug.log";
        }


        log_file = new File(log);

        tv_log= rootView.findViewById(R.id.log_view);
        tv_log.setText(readLogFile());

        Button export = rootView.findViewById(R.id.bt_export);

        export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                export_log();
            }
        });


        Button clear = rootView.findViewById(R.id.bt_clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clear_log();
            }
        });
        return rootView;
        }

        @Override
        public void onResume(){
            super.onResume();
            log_thread=refresh();
            log_thread.start();
        }

        @Override
        public void onPause(){
            super.onPause();
            log_thread.interrupt();
        }


    private Thread refresh() {
        return new Thread() {
            @SuppressWarnings("CatchMayIgnoreException")
            @Override
            public void run() {
                try {
                    while (this.isAlive()) {
                        h.post(new Runnable() {
                            @Override
                            public void run() {
                                tv_log.invalidate();
                                tv_log.setText(readLogFile());
                            }
                        });
                        sleep(2000);
                    }
                }
                catch(Exception e){
                    e.getMessage();
                }
            }
        };
    }


    private String readLogFile(){


        String log = appContext.getApplicationInfo().dataDir + "/logfile.log";

        if (((String)sp.getSelectedItem()).equals("debug")){
            log = appContext.getApplicationInfo().dataDir + "/debug.log";
            this.selected_item=1;
        }
        log_file = new File(log);

        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(log_file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');

            }
            br.close();
        } catch (IOException e) {
            text.append(e.getMessage());
        }
        int start = text.length()-16000;
        return text.substring(start);
    }

    private void clear_log(){
        try{
            PrintWriter writer = new PrintWriter(log_file);
            writer.print("cleared");
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    private void export_log(){
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        Log.d("selected_log",String.valueOf(this.selected_item));
        Intent chooserIntent = Intent.createChooser(i,"Choose Directory");
        if (this.selected_item==0) Objects.requireNonNull(getActivity()).startActivityForResult(chooserIntent,41);
        else if (this.selected_item==1) Objects.requireNonNull(getActivity()).startActivityForResult(chooserIntent,42);
    }



}

