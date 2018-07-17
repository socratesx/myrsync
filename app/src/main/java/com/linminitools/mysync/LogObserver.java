package com.linminitools.mysync;

import android.os.FileObserver;
import android.os.Handler;
import android.util.Log;


public class LogObserver extends FileObserver {

    private String logpath;
    public Handler h;

    public LogObserver(String path, Handler handler){
        super(path,FileObserver.CREATE);
        logpath = path;
        Log.d("FileObserver: ",logpath);
        h=handler;
    }

    @Override
    public void onEvent(int event, String path) {
        if(path != null){
             h.post(new Runnable() {
                 @Override
                 public void run() {
                     Log.d("FileObserver","EVENT Catch");
                 }
             });
        }
    }


    @Override
    public void finalize(){
        Log.d("FileObserver","Finalized");
        super.finalize();
    }

    @Override
    public void startWatching(){
        Log.d("FileObserver","Started Watching");
        super.startWatching();
    }

}
