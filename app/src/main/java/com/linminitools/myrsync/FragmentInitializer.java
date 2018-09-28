package com.linminitools.myrsync;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

public class FragmentInitializer extends FragmentActivity {

    Fragment fHolder;

    public FragmentInitializer(){
    }

    FragmentInitializer(Fragment f){
        fHolder=f;
    }

}
