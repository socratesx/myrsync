package com.linminitools.myrsync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class Settings extends AppCompatActivity {

    private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
            new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object value) {
                    String stringValue = value.toString();

                    if (preference instanceof ListPreference) {
                        ListPreference listPreference = (ListPreference) preference;
                        int index = listPreference.findIndexOfValue(stringValue);
                        preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
                    } else if (preference instanceof RingtonePreference) {
                        if (TextUtils.isEmpty(stringValue)) {
                            preference.setSummary(R.string.pref_ringtone_silent);
                        } else {
                            Ringtone ringtone = RingtoneManager.getRingtone(
                                    preference.getContext(), Uri.parse(stringValue));
                            if (ringtone == null) {
                                preference.setSummary(null);
                            } else {
                                preference.setSummary(ringtone.getTitle(preference.getContext()));
                            }
                        }
                    } else if (preference instanceof SwitchPreference) {
                        Log.d("Preference_value", stringValue);
                        if (preference.getKey().equals("root_access") && stringValue.equals("true")) {
                            Runtime root = Runtime.getRuntime();
                            try {
                                root.addShutdownHook(Thread.currentThread());
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            }
                            try {
                                Process p = root.exec("su -c whoami");
                                p.waitFor();
                                if (p.exitValue() == 0) {
                                    preference.setSummary(stringValue);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            root.runFinalization();
                            root.removeShutdownHook(Thread.currentThread());
                        } else {
                            preference.setSummary(stringValue);
                        }
                    } else {
                        preference.setSummary(stringValue);
                    }
                    return true;
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemBars.setup(this);
        setContentView(R.layout.activity_settings);
        SystemBars.applyTopInsets(findViewById(R.id.settings_app_bar));
        SystemBars.applyBottomInsets(findViewById(R.id.settings_container));

        MaterialToolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new MainPreferenceFragment())
                    .commit();
        }
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
            String def = "theme_mode".equals(preference.getKey()) ? "follow_system" : "";
            sBindPreferenceSummaryToValueListener.onPreferenceChange(
                    preference, sp.getString(preference.getKey(), def));
        } catch (ClassCastException e) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(
                    preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), true));
        }
    }

    public static class MainPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);

            ListPreference themePref = (ListPreference) findPreference("theme_mode");
            android.app.Activity activity = getActivity();
            if (activity != null && themePref != null) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
                ThemeModeHelper.migrateLegacyThemePreference(sp);
                String themeVal = sp.getString("theme_mode", "follow_system");
                themePref.setValue(themeVal);
                int ti = themePref.findIndexOfValue(themeVal);
                if (ti >= 0) {
                    themePref.setSummary(themePref.getEntries()[ti]);
                }

                themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String sv = String.valueOf(newValue);
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                            preference.getContext());
                    prefs.edit().putString("theme_mode", sv).commit();
                    ThemeModeHelper.applyFromPreferenceString(sv);
                    ListPreference lp = (ListPreference) preference;
                    int idx = lp.findIndexOfValue(sv);
                    if (idx >= 0) {
                        preference.setSummary(lp.getEntries()[idx]);
                    }
                    android.app.Activity act = getActivity();
                    if (act != null && !act.isFinishing()) {
                        act.getWindow().getDecorView().post(act::recreate);
                    }
                    return true;
                });
            }

            bindPreferenceSummaryToValue(findPreference("notifications"));
            bindPreferenceSummaryToValue(findPreference("vibrate"));
            bindPreferenceSummaryToValue(findPreference("ringtone"));
            bindPreferenceSummaryToValue(findPreference("root_access"));

            Preference privacy = findPreference("privacy_policy");
            if (privacy != null) {
                privacy.setOnPreferenceClickListener(preference -> {
                    android.app.Activity act = getActivity();
                    if (act == null) {
                        return true;
                    }
                    Uri uri = Uri.parse(act.getString(R.string.privacy_policy_url));
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        act.startActivity(intent);
                    } catch (Exception e) {
                        Log.w("Settings", "Unable to open privacy policy URL", e);
                    }
                    return true;
                });
            }

            Preference proUpgrade = findPreference("pro_upgrade");
            Preference proRestore = findPreference("pro_restore");
            SwitchPreference debugUnlock = (SwitchPreference) findPreference("pro_debug_unlock");
            android.app.Activity act = getActivity();
            if (act != null && proUpgrade != null) {
                refreshProPreferenceSummaries(proUpgrade, proRestore, act);
                proUpgrade.setOnPreferenceClickListener(preference -> {
                    if (ProEntitlement.isPro(act)) {
                        return true;
                    }
                    ProUpgradeUi.showUpgradeDialog(act, act.getString(R.string.pro_limit_configs));
                    return true;
                });
            }
            if (act != null && proRestore != null) {
                proRestore.setOnPreferenceClickListener(preference -> {
                    ProUpgradeUi.restore(act);
                    refreshProPreferenceSummaries(proUpgrade, proRestore, act);
                    return true;
                });
            }
            if (debugUnlock != null) {
                if (!BuildConfig.DEBUG) {
                    getPreferenceScreen().removePreference(debugUnlock);
                } else if (act != null) {
                    debugUnlock.setChecked(ProEntitlement.isPro(act));
                    debugUnlock.setOnPreferenceChangeListener((preference, newValue) -> {
                        boolean unlock = Boolean.TRUE.equals(newValue);
                        ProEntitlement.setPro(preference.getContext(), unlock);
                        refreshProPreferenceSummaries(proUpgrade, proRestore, act);
                        return true;
                    });
                } else {
                    getPreferenceScreen().removePreference(debugUnlock);
                }
            }
        }

        private void refreshProPreferenceSummaries(Preference proUpgrade,
                                                   Preference proRestore,
                                                   android.app.Activity act) {
            if (act == null) {
                return;
            }
            boolean pro = ProEntitlement.isPro(act);
            if (proUpgrade != null) {
                proUpgrade.setSummary(pro ? R.string.pro_status_pro : R.string.pro_status_free);
                proUpgrade.setEnabled(!pro);
            }
            if (proRestore != null) {
                proRestore.setEnabled(!pro);
            }
        }
    }
}
