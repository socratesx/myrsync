package com.linminitools.mysync;


import android.arch.lifecycle.MutableLiveData;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static android.content.Context.MODE_PRIVATE;
import static com.linminitools.mysync.MainActivity.appContext;

public class liveContent extends MutableLiveData<String> {
    private MutableLiveData<String> mCurrentName;
    private Thread reading;
    private long Thread_id=0;

    private String readLog(){
        Log.d("OBSERVER","IS ACTIVE");
        SharedPreferences prefs = appContext.getSharedPreferences("Rsync_Command_build", MODE_PRIVATE);
        String log = prefs.getString("log", appContext.getApplicationInfo().dataDir + "/logfile.log");
        File file = new File(log);

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
    }       //Reads the File and Returns a String with its Contents

    @Override
    public void onInactive(){
        Log.d("OBSERVER","IS INACTIVE");
        if (reading.isAlive()) reading.interrupt();
    }

    @Override
    public void onActive(){
        Log.d("OBSERVER","IS ACTIVE");
        reading = new Thread(){
            @Override
            public void run() {
                try {
                    while (this.isAlive() && !this.isInterrupted()) {
                        String s =readLog();
                        getCurrentName().postValue(s);
                        sleep(1000);
                    }
                }
                catch (InterruptedException e){
                    //DO NOTHING
                }
            }
        };
        reading.start();
        Thread_id=reading.getId();
        Log.d("THREAD_ID",String.valueOf(Thread_id));
    }

    public liveContent() {
        getCurrentName().setValue(readLog());
    }

    public MutableLiveData<String> getCurrentName() {
        if (this.mCurrentName == null) {
            this.mCurrentName = new MutableLiveData<String>();
            }
            return this.mCurrentName;
        }
    }


