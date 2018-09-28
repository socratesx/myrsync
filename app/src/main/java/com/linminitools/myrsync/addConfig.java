package com.linminitools.myrsync;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.linminitools.myrsync.MainActivity.appContext;
import static com.linminitools.myrsync.MainActivity.configs;
import static com.linminitools.myrsync.MainActivity.getPath;


public class addConfig extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_config);
        Button save = findViewById(R.id.bt_save);
        Button view = findViewById(R.id.bt_view);
        save.setEnabled(false);
        view.setEnabled(false);
    }



    public void addPath(View v){
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(Intent.createChooser(i,"Choose Directory"),1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request it is that we're responding to
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // Get the URI that points to the selected path

                Uri pathUri = data.getData();
                Uri dirUri = DocumentsContract.buildDocumentUriUsingTree(pathUri, DocumentsContract.getTreeDocumentId(pathUri));
                String local_path = getPath(this, dirUri);


                SharedPreferences path_prefs = appContext.getSharedPreferences("Rsync_Config_path", MODE_PRIVATE);
                SharedPreferences.Editor path_prefseditor = path_prefs.edit();
                path_prefseditor.putString("local_path",local_path);
                path_prefseditor.apply();

                TextView tv_path= findViewById(R.id.tv_path);
                if (!Objects.requireNonNull(local_path).isEmpty()) {
                    tv_path.setText(path_prefs.getString("local_path", ""));
                    tv_path.setVisibility(View.VISIBLE);

                    Button tv_addpath = findViewById(R.id.bt_add);
                    tv_addpath.setText(R.string.change_path);

                    Button save = findViewById(R.id.bt_save);
                    Button view = findViewById(R.id.bt_view);
                    save.setEnabled(true);
                    view.setEnabled(true);

                }

            }
        }
    }



    Map<String,String> processForm(){

        Map<String,String> configMap = new HashMap<>();

        EditText et_srv_ip= findViewById(R.id.ed_srv_ip);
        EditText et_srv_port= findViewById(R.id.ed_srv_port);
        EditText et_rs_user= findViewById(R.id.ed_rsync_user);
        EditText et_rs_mod= findViewById(R.id.ed_rsync_mod);
        EditText et_config_name = findViewById(R.id.et_rsync_alias);


        String options="-";


        String[] availableOptions={"a","r","z","v","n","p","t","O","q","m","u","g"};

        for(String check_box : availableOptions){
            int resID = getResources().getIdentifier("cb_" + check_box, "id", getPackageName());
            Log.d("RESID",String.valueOf(resID));
            CheckBox cb = findViewById(resID);
            if(cb.isChecked()){
                options=options.concat(check_box);
            }
        }
        if (Objects.equals(options, "-")){options="";}

        String rs_user=String.valueOf(et_rs_user.getText()) ;
        String rs_ip=String.valueOf(et_srv_ip.getText());
        String rs_port=String.valueOf(et_srv_port.getText());
        String rs_module=String.valueOf(et_rs_mod.getText());
        String rs_name = String.valueOf(et_config_name.getText());
        String local_path=appContext.getSharedPreferences("Rsync_Config_path", MODE_PRIVATE).getString("local_path","");
        if (rs_port.isEmpty()) { rs_port="873";}

        configMap.put("rs_ip",rs_ip);
        configMap.put("rs_user",rs_user);
        configMap.put("rs_port",rs_port);
        configMap.put("rs_module",rs_module);
        configMap.put("local_path",local_path);
        configMap.put("rs_options",options);
        configMap.put("rs_name",rs_name);

        return configMap;
    }



    public void viewCommand(View v){

        Map<String,String> configMap=processForm();

        String options = configMap.get("rs_options");
        String rs_user = configMap.get("rs_user");
        String rs_ip = configMap.get("rs_ip");
        String rs_port = configMap.get("rs_port");
        String rs_module = configMap.get("rs_module");
        String local_path = configMap.get("local_path");


        TextView tv= findViewById(R.id.tv_rs_cmd_View);

        String Rsync_command = "rsync "+options+" "+ local_path+" "+"rsync://"+rs_user+"@"+rs_ip+":"+rs_port+"/"+rs_module;


        tv.setText(Rsync_command);

    }


    public void saveConfig(View v){
        Map<String,String> configMap=processForm();

        String rs_options = configMap.get("rs_options");
        String rs_user = configMap.get("rs_user");
        String rs_ip = configMap.get("rs_ip");
        String rs_port = configMap.get("rs_port");
        String rs_module = configMap.get("rs_module");
        String local_path = configMap.get("local_path");
        String rs_name = configMap.get("rs_name");



        if (!rs_ip.isEmpty() && !rs_module.isEmpty() && !local_path.isEmpty()) {

            int id = 1;

            SharedPreferences config_prefs = getSharedPreferences("configs",MODE_PRIVATE);

            while (config_prefs.getInt("rs_id_"+String.valueOf(id),-1)>0){
                id=id+1;
            }

            RS_Configuration config = new RS_Configuration(id);

            config.rs_ip = rs_ip;
            config.rs_user = rs_user;
            config.rs_port = rs_port;
            config.rs_options = rs_options;
            config.rs_module = rs_module;
            config.local_path = local_path;
            config.name= rs_name;
            config.saveToDisk();
            configs.add(config);
            this.finish();

        }
        else{

            Context context = getApplicationContext();
            CharSequence text = "Configuration is not Complete!";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }


    }

    public void rsync_help(View v){
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Rsync Options Help")
                        .setMessage(getResources().getString(R.string.rsync_options))
                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                ;
        alertDialogBuilder.show();
    }

    public void rsync_daemon_info(View v){
        SpannableString s = new SpannableString(getResources().getString(R.string.rsync_daemon_info));
        Pattern p = Pattern.compile("https://download.samba.org/pub/rsync/rsyncd.conf.html");
        Linkify.addLinks(s,p,null);

        AlertDialog d= new AlertDialog.Builder(v.getContext())
                        .setTitle("Build Rsync Configuration")
                        .setMessage(s)
                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
        TextView textView = d.findViewById(android.R.id.message);

        textView.setTextSize(13);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
