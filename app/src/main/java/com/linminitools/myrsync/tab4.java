package com.linminitools.myrsync;


import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;
import static com.linminitools.myrsync.MainActivity.appContext;

public class tab4 extends Fragment{

        private TextView tv_log;
        private final Handler h= new Handler();
        private File log_file;
        private Thread log_thread;


    @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tab4, container, false);
            rootView.setTag("tab4");

            SharedPreferences prefs = Objects.requireNonNull(getContext()).getSharedPreferences("Rsync_Command_build", MODE_PRIVATE);
            String log = prefs.getString("log", appContext.getApplicationInfo().dataDir + "/logfile.log");
            log_file = new File(log);

            tv_log= rootView.findViewById(R.id.log_view);
            tv_log.setText(readLogFile());

            Button export = rootView.findViewById(R.id.bt_export);

            export.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    export_log(view);
                }
            });


        Button clear = rootView.findViewById(R.id.bt_clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clear_log(view);
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
                    e.printStackTrace();
                }
            }
        };
    }


    private String readLogFile(){

        File file = log_file;
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');

            }
            br.close();
        } catch (IOException e) {
            text.append(e.getMessage());
        }
        return text.toString();
    }

    public void clear_log(View v){
        try{
            PrintWriter writer = new PrintWriter(log_file);
            writer.print("cleared");
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    public void export_log(View v){

        Uri selectedUri = Uri.parse(Environment.getDataDirectory().toString());
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        Objects.requireNonNull(getActivity()).startActivityForResult(Intent.createChooser(i,"Choose Directory"),41);
    }



}

