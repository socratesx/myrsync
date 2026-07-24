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
import androidx.core.app.NavUtils;
import androidx.appcompat.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import java.io.IOException;

public class Settings  extends AppCompatPreferenceActivity {

    private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            }
            else if (preference instanceof SwitchPreference){
                Log.d("Preference_value",stringValue);

                 if (preference.getKey().equals("root_access") && stringValue.equals("true")){

                     Runtime root = Runtime.getRuntime();
                     try {
                         root.addShutdownHook(Thread.currentThread());
                     }
                     catch(IllegalArgumentException e) {
                         e.printStackTrace();
                     }

                     try {
                     Process p = root.exec("su -c whoami");
                     p.waitFor();
                     int exit_value= p.exitValue() ;
                     if(exit_value==0) preference.setSummary(stringValue);
                     }
                     catch (Exception e){
                         e.printStackTrace();
                     }
                     root.runFinalization();
                     root.removeShutdownHook(Thread.currentThread());
                 }
                 else {
                     preference.setSummary(stringValue);
                 }

            }

            else {
                // For all other preferences, set the summary to the value's
                // simple string representation.

                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setupActionBar();
            getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();

        }

        private static void bindPreferenceSummaryToValue(Preference preference) {
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

            // Trigger the listener immediately with the preference's
            // current value.
            try {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
                String def = "theme_mode".equals(preference.getKey()) ? "follow_system" : "";
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, sp.getString(preference.getKey(), def));
            }
            catch (ClassCastException e){
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getBoolean(preference.getKey(), true));
            }
        }

        /**
         * Set up the {@link android.app.ActionBar}, if the API is available.
        */
        private void setupActionBar() {
            //ActionBar actionBar = getActionBar();
            ActionBar actionBar = this.getSupportActionBar();
            if (actionBar != null) {
                // Show the Up button in the action bar.
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }


        public static class MainPreferenceFragment extends PreferenceFragment{

            @Override
            public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                addPreferencesFromResource(R.xml.pref_notification);
                //setHasOptionsMenu(true);

                ListPreference themePref = (ListPreference) findPreference("theme_mode");
                android.app.Activity activity = getActivity();
                if (activity != null) {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
                    ThemeModeHelper.migrateLegacyThemePreference(sp);
                    String themeVal = sp.getString("theme_mode", "follow_system");
                    themePref.setValue(themeVal);
                    int ti = themePref.findIndexOfValue(themeVal);
                    if (ti >= 0) {
                        themePref.setSummary(themePref.getEntries()[ti]);
                    }
                }

                /*
                 * Theme list: sync value + summary from prefs explicitly (ListPreference can disagree
                 * with SharedPreferences until setValue). Persist with commit() before recreate so
                 * the restarted activity reads the choice the user just picked; then recreate() so
                 * the screen matches AppCompatDelegate night mode.
                 */
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
                        ProUpgradeUi.showUpgradeDialog(act,
                                act.getString(R.string.pro_limit_configs));
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


