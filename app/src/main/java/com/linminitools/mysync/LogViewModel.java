package com.linminitools.mysync;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static android.content.Context.MODE_PRIVATE;
import static com.linminitools.mysync.MainActivity.appContext;

public class LogViewModel extends ViewModel {
    liveContent log_string;


    public LogViewModel(){
        log_string = new liveContent();
        Log.d("LOG_VIEW+MODEL", log_string.getCurrentName().getValue());

    }

    public String returnLog(){
        return log_string.getCurrentName().getValue();
    }


}

