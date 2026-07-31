package com.linminitools.myrsync;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import static com.linminitools.myrsync.MainActivity.SELECTED_TAB;
import static com.linminitools.myrsync.MainActivity.appContext;

/**
 * Tasker / Locale plug-in editor.
 * <p>
 * Intentionally does <b>not</b> extend the Locale SDK base activity — that path has been
 * unstable with this app's AppCompat / Material setup. Implements the Locale edit protocol
 * directly: {@code RESULT_OK} + {@code EXTRA_BUNDLE} + {@code EXTRA_STRING_BLURB}.
 */
public final class TaskerPluginEditActivity extends AppCompatActivity {

    private static final String TAG = "TaskerPluginEdit";
    private static final String NONE_SELECTED = "None selected...";
    private static final String EXTRA_BUNDLE =
            "com.twofortyfouram.locale.intent.extra.BUNDLE";
    private static final String EXTRA_BLURB =
            "com.twofortyfouram.locale.intent.extra.BLURB";
    private static final int MAX_BLURB_LENGTH = 60;

    private final ArrayList<RS_Configuration> pluginConfigs = new ArrayList<>();
    private final List<Integer> spinnerConfigIds = new ArrayList<>();

    private Spinner configSpinner;
    private int selectedConfigId = -1;
    private boolean refreshConfigsOnResume;
    private boolean finishingWithResult;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Default until the user saves a valid selection.
        setResult(Activity.RESULT_CANCELED);

        if (!ProEntitlement.canUseTasker(this)) {
            Toast.makeText(this, R.string.pro_limit_tasker, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        appContext = getApplicationContext();
        loadPluginConfigs();
        SystemBars.setup(this);
        setContentView(R.layout.plugin);
        SystemBars.applyTopInsets(findViewById(R.id.plugin_app_bar));
        SystemBars.applyBottomInsets(findViewById(R.id.plugin_content));

        MaterialToolbar toolbar = findViewById(R.id.plugin_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

        configSpinner = findViewById(R.id.plugin_sp_configs);
        configSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSelectedConfigId(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedConfigId = -1;
            }
        });

        bindConfigSpinner();

        Bundle previous = getIntent() != null ? getIntent().getBundleExtra(EXTRA_BUNDLE) : null;
        if (previous != null) {
            int previousId = previous.getInt(PluginBundleValues.BUNDLE_EXTRA_INT_CONFIG, -1);
            if (previousId >= 0) {
                selectConfigId(previousId);
            }
        }

        Log.i(TAG, "opened; configs=" + pluginConfigs.size()
                + " action=" + (getIntent() != null ? getIntent().getAction() : "null"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!refreshConfigsOnResume) {
            return;
        }
        refreshConfigsOnResume = false;
        int keepId = selectedConfigId;
        loadPluginConfigs();
        bindConfigSpinner();
        if (keepId >= 0) {
            selectConfigId(keepId);
        }
    }

    private void loadPluginConfigs() {
        myRsyncApplication.loadConfigsInto(getApplicationContext(), pluginConfigs);
    }

    private void bindConfigSpinner() {
        List<String> labels = new ArrayList<>();
        spinnerConfigIds.clear();
        labels.add(NONE_SELECTED);
        spinnerConfigIds.add(-1);

        for (RS_Configuration c : pluginConfigs) {
            labels.add(c.name);
            spinnerConfigIds.add(c.id);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        configSpinner.setAdapter(adapter);
        configSpinner.setSelection(0, false);
        selectedConfigId = -1;
    }

    private void selectConfigId(int configId) {
        for (int i = 1; i < spinnerConfigIds.size(); i++) {
            if (spinnerConfigIds.get(i) == configId) {
                configSpinner.setSelection(i, false);
                selectedConfigId = configId;
                return;
            }
        }
    }

    private void updateSelectedConfigId(int position) {
        if (position > 0 && position < spinnerConfigIds.size()) {
            selectedConfigId = spinnerConfigIds.get(position);
        } else {
            selectedConfigId = -1;
        }
    }

    @Nullable
    private RS_Configuration findConfig(int configId) {
        for (RS_Configuration c : pluginConfigs) {
            if (c.id == configId) {
                return c;
            }
        }
        return myRsyncApplication.loadConfigById(getApplicationContext(), configId);
    }

    @NonNull
    private String buildBlurb(int configId) {
        RS_Configuration config = findConfig(configId);
        if (config == null) {
            return getString(R.string.tasker_plugin_no_config);
        }
        String modeLabel = "SSH".equals(config.rs_transport_type) ? "SSH" : "Daemon";
        String blurb = getString(R.string.tasker_plugin_blurb_format, config.name, modeLabel);
        if (blurb.length() > MAX_BLURB_LENGTH) {
            return blurb.substring(0, MAX_BLURB_LENGTH);
        }
        return blurb;
    }

    @SuppressWarnings("deprecation")
    private int readVersionCode() {
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                return (int) getPackageManager()
                        .getPackageInfo(getPackageName(), 0)
                        .getLongVersionCode();
            }
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private void saveAndReturnToHost() {
        if (finishingWithResult) {
            return;
        }
        if (selectedConfigId < 0) {
            Toast.makeText(this, R.string.tasker_plugin_pick_config, Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle pluginBundle = new Bundle();
        pluginBundle.putInt(PluginBundleValues.BUNDLE_EXTRA_INT_CONFIG, selectedConfigId);
        pluginBundle.putInt(PluginBundleValues.BUNDLE_EXTRA_INT_VERSION_CODE, readVersionCode());

        String blurb = buildBlurb(selectedConfigId);
        Intent result = new Intent();
        result.putExtra(EXTRA_BUNDLE, pluginBundle);
        result.putExtra(EXTRA_BLURB, blurb);

        Log.i(TAG, "returning RESULT_OK configId=" + selectedConfigId + " blurb=" + blurb);
        finishingWithResult = true;
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // Match Locale host UX: back with a valid selection saves.
        if (selectedConfigId >= 0) {
            saveAndReturnToHost();
            return;
        }
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

    public void savePluginConfig(View view) {
        saveAndReturnToHost();
    }

    public void editConfigs(View view) {
        refreshConfigsOnResume = true;
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(SELECTED_TAB, 1);
        startActivity(intent);
    }
}
