package com.linminitools.mysync;


import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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

public class tab4 extends Fragment{


        @Override
        public void onActivityCreated(Bundle savedInstanceState){
            super.onActivityCreated(savedInstanceState);
            //new logFragment();

        }





        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tab4, container, false);
            rootView.setTag("tab4");
            return rootView;
        }

        public static class logFragment extends Fragment{

            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

                ViewGroup viewlog = (ViewGroup) inflater.inflate(R.layout.logview, container, false);


                final LogViewModel mModel = ViewModelProviders.of(this).get(LogViewModel.class);
                final TextView log = this.getView().findViewById(R.id.tv_log);

                // Create the observer which updates the UI.
                final Observer<String> LogObserver = new Observer<String>() {
                    @Override
                    public void onChanged(@Nullable final String newName) {
                        // Update the UI, in this case, a TextView.
                        log.setText(newName);
                        Log.d("OBSERVER_ON_CHANGED", mModel.returnLog());
                    }
                };

                mModel.log_string.getCurrentName().observe(this, LogObserver);
                return viewlog;
            }
        }

 /*
    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {

        SharedPreferences prefs = appContext.getSharedPreferences("Rsync_Command_build", MODE_PRIVATE);
        String log = prefs.getString("log", appContext.getApplicationInfo().dataDir + "/logfile.log");
        File file = new File(log);

        StringBuilder text = new StringBuilder();
        final TextView tv = v.findViewById(R.id.tv_log);

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


        Log.d("LOG", text.toString());
        tv.setText(text);


    }
*/

}

