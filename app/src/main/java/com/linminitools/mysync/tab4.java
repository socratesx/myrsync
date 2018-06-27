package com.linminitools.mysync;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static android.content.Context.MODE_PRIVATE;
import static com.linminitools.mysync.MainActivity.appContext;

public class tab4 extends android.support.v4.app.Fragment {

/*
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t.start();
    }
*/



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tab4, container, false);

        SharedPreferences prefs = appContext.getSharedPreferences("Rsync_Command_build", MODE_PRIVATE);
        String log = prefs.getString("log",appContext.getApplicationInfo().dataDir+"/logfile.log");
        rootView.setTag("tab4");
        File file = new File(log);


        StringBuilder text = new StringBuilder();
        TextView tv = rootView.findViewById(R.id.tv_log);

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');

            }
            br.close();
        }
        catch (IOException e) {
            text.append(e.getMessage());
        }


        Log.d("LOG",text.toString());
        tv.setText(text);


        return rootView;
    }


}
