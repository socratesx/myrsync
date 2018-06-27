package com.linminitools.mysync;

import android.arch.lifecycle.LiveData;

public class liveContent extends LiveData<String> {
    private static final liveContent ourInstance = new liveContent();

    public static liveContent getInstance() {
        return ourInstance;
    }

    private liveContent() {


    }




}
