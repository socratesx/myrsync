package com.linminitools.myrsync;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class FragmentInitializer extends FragmentActivity {

    Fragment fHolder;

    public FragmentInitializer(){
    }

    FragmentInitializer(Fragment f){
        fHolder=f;
    }

}
