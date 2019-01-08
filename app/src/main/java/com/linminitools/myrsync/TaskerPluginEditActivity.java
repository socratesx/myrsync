package com.linminitools.myrsync;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.linminitools.myrsync.PluginBundleValues;
import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity;
import com.twofortyfouram.log.Lumberjack;

import net.jcip.annotations.NotThreadSafe;

import java.util.ArrayList;
import java.util.List;

import static android.provider.AlarmClock.EXTRA_MESSAGE;
import static com.linminitools.myrsync.MainActivity.SELECTED_TAB;
import static com.linminitools.myrsync.MainActivity.appContext;
import static com.linminitools.myrsync.myRsyncApplication.configs;
import static com.linminitools.myrsync.myRsyncApplication.schedulers;

@NotThreadSafe
public final class TaskerPluginEditActivity extends AbstractAppCompatPluginActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        // Make sure the context is initialised, otherwise the plugin will crash if the app is not already running
        appContext = getApplicationContext();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.plugin);

        /*
         * To help the user keep context, the title shows the host's name and the subtitle
         * shows the plug-in's name.
         */
        CharSequence callingApplicationLabel = null;
        try {
            callingApplicationLabel =
                    getPackageManager().getApplicationLabel(
                            getPackageManager().getApplicationInfo(getCallingPackage(),
                                    0));
        } catch (final PackageManager.NameNotFoundException e) {
            Lumberjack.e("Calling package couldn't be found%s", e); //$NON-NLS-1$
        }
        if (null != callingApplicationLabel) {
            setTitle(callingApplicationLabel);
        }

        getSupportActionBar().setSubtitle(R.string.app_name);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Spinner sp = findViewById(R.id.plugin_sp_configs);

        List<String> listLoadToSpinner = new ArrayList<>();

        listLoadToSpinner.add("None selected...");

        for (RS_Configuration c : configs) {
            listLoadToSpinner.add(c.name);
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(appContext, android.R.layout.simple_spinner_dropdown_item, listLoadToSpinner);


        sp.setAdapter(spinnerAdapter);
    }

    @Override
    public void onPostCreateWithPreviousResult(@NonNull final Bundle previousBundle,
                                               @NonNull final String previousBlurb) {
        Spinner sp = findViewById(R.id.plugin_sp_configs);

        List<String> listLoadToSpinner = new ArrayList<>();

        listLoadToSpinner.add("None selected...");

        int configId = PluginBundleValues.getConfigId(previousBundle);

        int selected_config=0;

        for (RS_Configuration c : configs) {
            listLoadToSpinner.add(c.name);
            if (c.id==configId) selected_config=listLoadToSpinner.indexOf(c.name);
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(appContext, android.R.layout.simple_spinner_dropdown_item, listLoadToSpinner);

        sp.setAdapter(spinnerAdapter);
        sp.setSelection(selected_config);
    }

    @Override
    public boolean isBundleValid(@NonNull final Bundle bundle) {
        return PluginBundleValues.isBundleValid(bundle);
    }

    @Nullable
    @Override
    public Bundle getResultBundle() {
        Bundle result = null;

        Spinner sp = findViewById(R.id.plugin_sp_configs);

        String selected_config_name = (String)sp.getSelectedItem();

        for (RS_Configuration c : configs) if((c.name).equals(selected_config_name)) {
            result = PluginBundleValues.generateBundle(getApplicationContext(), c.id);
        }

        return result;
    }

    @NonNull
    @Override
    public String getResultBlurb(@NonNull final Bundle bundle) {
        String message = null;

        final int configId = PluginBundleValues.getConfigId(bundle);

        for (RS_Configuration c : configs) if((c.id==configId)) {
            message = "\"" + c.name + "\"\nMode: " + c.rs_mode + "\nIP: " + c.rs_ip + "\nModule: " + c.rs_module;
        }

        final int maxBlurbLength = getResources().getInteger(
                R.integer.com_twofortyfouram_locale_sdk_client_maximum_blurb_length);

        if (message != null && message.length() > maxBlurbLength) {
            return message.substring(0, maxBlurbLength);
        }

        return message;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.plugin_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            finish();
        }
        else if (R.id.plugin_menu_discard_changes == item.getItemId()) {
            // Signal to AbstractAppCompatPluginActivity that the user canceled.
            mIsCancelled = true;
            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void editConfigs(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(SELECTED_TAB, 1);
        startActivity(intent);
    }
}