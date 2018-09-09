package com.linminitools.myrsync;

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
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

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
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

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

                preference.setSummary(stringValue);
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

            //addPreferencesFromResource(R.xml.pref_notification);
            //setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            //bindPreferenceSummaryToValue(findPreference("notifications"));
            //bindPreferenceSummaryToValue(findPreference("vibrate"));
            //bindPreferenceSummaryToValue(findPreference("ringtone"));
            //bindPreferenceSummaryToValue(findPreference("clear_log"));
        }

        private static void bindPreferenceSummaryToValue(Preference preference) {
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

            // Trigger the listener immediately with the preference's
            // current value.
            try {
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
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

                // Bind the summaries of EditText/List/Dialog/Ringtone preferences
                // to their values. When their values change, their summaries are
                // updated to reflect the new value, per the Android Design
                // guidelines.
                bindPreferenceSummaryToValue(findPreference("notifications"));
                bindPreferenceSummaryToValue(findPreference("vibrate"));
                bindPreferenceSummaryToValue(findPreference("ringtone"));
                bindPreferenceSummaryToValue(findPreference("clear_log"));
            }



        }


    }


