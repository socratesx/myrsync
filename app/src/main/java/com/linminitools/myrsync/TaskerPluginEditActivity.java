package com.linminitools.myrsync;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.linminitools.myrsync.tasker.TaskerPluginContract;
import com.linminitools.myrsync.util.Lumberjack;
import com.linminitools.myrsync.util.NotThreadSafe;

import java.util.ArrayList;
import java.util.List;

import static com.linminitools.myrsync.MainActivity.SELECTED_TAB;
import static com.linminitools.myrsync.MainActivity.appContext;
import static com.linminitools.myrsync.myRsyncApplication.configs;

@NotThreadSafe
public final class TaskerPluginEditActivity extends AppCompatActivity {

    private boolean mIsCancelled = false;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        // Make sure the context is initialised, otherwise the plugin will crash if the app is not already running
        appContext = getApplicationContext();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.plugin);

        setResult(RESULT_CANCELED);

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

        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(R.string.app_name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        populateSpinner(null);

        final Bundle previousBundle = TaskerPluginContract.extractBundle(getIntent());
        if (previousBundle != null && PluginBundleValues.isBundleValid(previousBundle)) {
            onPostCreateWithPreviousResult(previousBundle);
        }
    }

    private void populateSpinner(@Nullable final RS_Configuration configurationToSelect) {
        final Spinner spinner = findViewById(R.id.plugin_sp_configs);

        final List<String> items = new ArrayList<>();
        items.add("None selected...");

        for (final RS_Configuration configuration : configs) {
            items.add(configuration.name);
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                appContext,
                android.R.layout.simple_spinner_dropdown_item,
                items
        );

        spinner.setAdapter(adapter);

        if (configurationToSelect != null) {
            final int position = items.indexOf(configurationToSelect.name);
            if (position >= 0) {
                spinner.setSelection(position);
            }
        }
    }

    public void onPostCreateWithPreviousResult(@NonNull final Bundle previousBundle) {
        final int configId = PluginBundleValues.getConfigId(previousBundle);

        RS_Configuration selected = null;
        for (final RS_Configuration configuration : configs) {
            if (configuration.id == configId) {
                selected = configuration;
                break;
            }
        }

        populateSpinner(selected);
    }

    public boolean isBundleValid(@NonNull final Bundle bundle) {
        return PluginBundleValues.isBundleValid(bundle);
    }

    @Nullable
    public Bundle getResultBundle() {
        Bundle result = null;

        final Spinner spinner = findViewById(R.id.plugin_sp_configs);
        final Object selectedItem = spinner.getSelectedItem();
        if (selectedItem instanceof String) {
            final String selectedConfigName = (String) selectedItem;
            for (final RS_Configuration configuration : configs) {
                if (configuration.name.equals(selectedConfigName)) {
                    result = PluginBundleValues.generateBundle(getApplicationContext(), configuration.id);
                    break;
                }
            }
        }

        return result;
    }

    @NonNull
    public String getResultBlurb(@NonNull final Bundle bundle) {
        String message = null;

        final int configId = PluginBundleValues.getConfigId(bundle);

        for (final RS_Configuration configuration : configs) {
            if (configuration.id == configId) {
                message = "\"" + configuration.name + "\"\nMode: " + configuration.rs_mode +
                        "\nIP: " + configuration.rs_ip + "\nModule: " + configuration.rs_module;
                break;
            }
        }

        final int maxBlurbLength = getResources().getInteger(R.integer.tasker_plugin_maximum_blurb_length);

        if (message != null && message.length() > maxBlurbLength) {
            return message.substring(0, maxBlurbLength);
        }

        return message == null ? "" : message;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.plugin_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            mIsCancelled = true;
            finish();
        } else if (R.id.plugin_menu_discard_changes == item.getItemId()) {
            mIsCancelled = true;
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        mIsCancelled = true;
        super.onBackPressed();
    }

    @Override
    public void finish() {
        if (!mIsCancelled) {
            final Bundle resultBundle = getResultBundle();
            if (resultBundle != null && isBundleValid(resultBundle)) {
                final Intent resultIntent = new Intent();
                resultIntent.putExtra(TaskerPluginContract.EXTRA_BUNDLE, resultBundle);
                resultIntent.putExtra(TaskerPluginContract.EXTRA_STRING_BLURB, getResultBlurb(resultBundle));
                setResult(RESULT_OK, resultIntent);
            } else {
                setResult(RESULT_CANCELED);
            }
        } else {
            setResult(RESULT_CANCELED);
        }

        super.finish();
    }

    public void editConfigs(final View view) {
        final Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(SELECTED_TAB, 1);
        startActivity(intent);
    }
}
