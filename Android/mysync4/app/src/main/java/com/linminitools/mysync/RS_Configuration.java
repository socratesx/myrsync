package com.linminitools.mysync;

import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static com.linminitools.mysync.MainActivity.appContext;


public class RS_Configuration {

    protected String rs_ip, rs_user, rs_module, rs_options, local_path,name;
    protected String rs_port="873";
    protected  int id;

    RS_Configuration(int id){
        this.id=id;
        this.name="config_"+String.valueOf(id);
    }
    
    protected void saveToDisk(){
        
        SharedPreferences prefs = appContext.getSharedPreferences("configs", MODE_PRIVATE);
        SharedPreferences.Editor prefseditor = prefs.edit();

        prefseditor.putString("rs_options_"+String.valueOf(this.id),rs_options);
        prefseditor.putString("rs_user_"+String.valueOf(this.id),rs_user);
        prefseditor.putString("rs_module_"+String.valueOf(this.id),rs_module);
        prefseditor.putString("rs_ip_"+String.valueOf(this.id),rs_ip);
        prefseditor.putString("rs_port_"+String.valueOf(this.id),rs_port);
        prefseditor.putString("local_path_"+String.valueOf(this.id),local_path);
        prefseditor.apply();
        
    }
    
    protected void deleteFromDisk(){
        SharedPreferences prefs = appContext.getSharedPreferences("configs", MODE_PRIVATE);
        SharedPreferences.Editor prefseditor = prefs.edit();

        prefseditor.remove("rs_options_"+String.valueOf(this.id));
        prefseditor.remove("rs_user_"+String.valueOf(this.id));
        prefseditor.remove("rs_module_"+String.valueOf(this.id));
        prefseditor.remove("rs_ip_"+String.valueOf(this.id));
        prefseditor.remove("rs_port_"+String.valueOf(this.id));
        prefseditor.remove("local_path_"+String.valueOf(this.id));
        prefseditor.apply();
    }

    protected void executeConfig(){


        SharedPreferences prefs = appContext.getSharedPreferences("Rsync_Command_build", MODE_PRIVATE);

        String options = this.rs_options;
        String log = prefs.getString("log",appContext.getApplicationInfo().dataDir+"/logfile.log");
        String local_path=this.local_path;
        String cmd = "rsync://"+this.rs_user+"@"+this.rs_ip+":"
                +this.rs_port+"/"+this.rs_module;
        Log.d("LOGFILE PATH",log);

        try {
            ProcessBuilder p = new ProcessBuilder("rsync",options,"--log-file=",log,local_path,cmd);
            p.redirectErrorStream(true);
            Map<String, String> env = p.environment();
            env.put("PATH", "/su/bin:/sbin:/vendor/bin:/system/sbin:/system/bin:/su/xbin:/system/xbin");
            p.directory(new File(appContext.getApplicationInfo().dataDir));
            Process process=p.start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ( (line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            String result = builder.toString();
            Log.d("RSYNC",result);
            //TextView tv_result = findViewById(R.id.tv_rs_cmd_View);
            //tv_result.setText(result);

        } catch (SecurityException e) {
            e.printStackTrace();
            Log.d("EXCEPTION 1","Security");
            //TextView tv_result = findViewById(R.id.tv_rs_cmd_View);
            //tv_result.setText(System.err.toString());
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.d("EXCEPTION 2","IO Exception");
            //TextView tv_result = findViewById(R.id.tv_rs_cmd_View);
            //tv_result.setText(System.err.toString());
        }



    }
    
}
