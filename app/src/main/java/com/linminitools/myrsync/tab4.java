package com.linminitools.myrsync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static com.linminitools.myrsync.MainActivity.appContext;
import static com.linminitools.myrsync.MainActivity.debug_log;
import static com.linminitools.myrsync.MainActivity.log_file;

public class tab4 extends Fragment {

    private TextView tv_log;
    private final Handler h = new Handler();
    private Thread log_thread;
    private int selected_item = -1;
    private Spinner sp;
    private String lastLogContent = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tab4, container, false);
        rootView.setTag("tab4");

        SharedPreferences prefs = requireContext().getSharedPreferences("Rsync_Command_build", MODE_PRIVATE);(getContext()).getSharedPreferences("Rsync_Command_build", MODE_PRIVATE);

        sp = rootView.findViewById(R.id.sp_selected_logfile);
        List<String> listLoadToSpinner = new ArrayList<>();
        listLoadToSpinner.add("rsync");
        listLoadToSpinner.add("debug");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, listLoadToSpinner);
        if (selected_item == -1) selected_item = 0;
        sp.setAdapter(spinnerAdapter);
        sp.setSelection(selected_item);

        String log = prefs.getString("log", appContext.getApplicationInfo().dataDir + "/logfile.log");
        if (sp.getSelectedItem().equals("debug")) {
            log = appContext.getApplicationInfo().dataDir + "/debug.log";
        }

        log_file = new File(log);

        tv_log = rootView.findViewById(R.id.log_view);
        tv_log.setTextIsSelectable(true);
        lastLogContent = readLogFile();
        tv_log.setText(lastLogContent);

        MaterialButton export = rootView.findViewById(R.id.bt_export);
        export.setOnClickListener(view -> export_log());

        MaterialButton clear = rootView.findViewById(R.id.bt_clear);
        clear.setOnClickListener(view -> clear_log());

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        log_thread = refresh();
        log_thread.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (log_thread != null) {
            log_thread.interrupt();
        }
    }

    private Thread refresh() {
        return new Thread() {
            @Override
            public void run() {
                try {
                    while (!this.isInterrupted()) {
                        String currentContent = readLogFile();
                        if (!currentContent.equals(lastLogContent)) {
                            lastLogContent = currentContent;
                            h.post(() -> {
                                tv_log.setText(lastLogContent);
                            });
                        }
                        sleep(2000);
                    }
                } catch (Exception e) {
                    // thread interrupted
                }
            }
        };
    }

    private String readLogFile() {
        String log = appContext.getApplicationInfo().dataDir + "/logfile.log";
        if (sp != null && sp.getSelectedItem() != null && sp.getSelectedItem().equals("debug")) {
            log = appContext.getApplicationInfo().dataDir + "/debug.log";
            this.selected_item = 1;
        } else {
            this.selected_item = 0;
        }
        File current_log_file = new File(log);

        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(current_log_file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            text.append(e.getMessage());
        }

        int start = 0;
        if (text.length() > 16000) {
            start = text.length() - 16000;
        }
        return text.substring(start);
    }

    private void clear_log() {
        try {
            File log = sp.getSelectedItem().equals("debug") ? debug_log : log_file;

            PrintWriter writer = new PrintWriter(log);
            writer.print("");
            writer.close();
            Toast.makeText(getContext(), "Log cleared", Toast.LENGTH_SHORT).show();
            lastLogContent = "";
            tv_log.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void export_log() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        Log.d("selected_log", String.valueOf(this.selected_item));
        Intent chooserIntent = Intent.createChooser(i, "Choose Directory");
        if (this.selected_item == 0)
            requireActivity().startActivityForResult(chooserIntent, 41);
        else if (this.selected_item == 1)
            requireActivity().startActivityForResult(chooserIntent, 42);
    }
}
