package com.linminitools.myrsync;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class Settings extends AppCompatActivity {

    private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
            (preference, value) -> {
                String stringValue = String.valueOf(value);

                if (preference instanceof ListPreference) {
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);
                    preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
                } else if (preference instanceof SwitchPreferenceCompat) {
                    if ("root_access".equals(preference.getKey()) && "true".equals(stringValue)) {
                        try {
                            Process p = Runtime.getRuntime().exec("su -c whoami");
                            p.waitFor();
                            if (p.exitValue() == 0) {
                                preference.setSummary(stringValue);
                            }
                        } catch (Exception e) {
                            Log.w("Settings", "Root check failed", e);
                        }
                    } else {
                        preference.setSummary(stringValue);
                    }
                } else {
                    preference.setSummary(stringValue);
                }
                return true;
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
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new MainPreferenceFragment())
                    .commit();
        }
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        if (preference == null) {
            return;
        }
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

    public static class MainPreferenceFragment extends PreferenceFragmentCompat {

        private final ActivityResultLauncher<Intent> ringtonePicker =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() != Activity.RESULT_OK) {
                        return;
                    }
                    Intent data = result.getData();
                    Uri uri = null;
                    if (data != null) {
                        uri = ringtoneUriFromResult(data);
                    }
                    String stored = uri != null ? uri.toString() : "";
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
                    sp.edit().putString("ringtone", stored).apply();
                    Preference ringtonePref = findPreference("ringtone");
                    updateRingtoneSummary(ringtonePref, stored);
                });

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.pref_notification, rootKey);

            ListPreference themePref = findPreference("theme_mode");
            Activity activity = getActivity();
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
                    Activity act = getActivity();
                    if (act != null && !act.isFinishing()) {
                        act.getWindow().getDecorView().post(act::recreate);
                    }
                    return true;
                });
            }

            bindPreferenceSummaryToValue(findPreference("notifications"));
            bindPreferenceSummaryToValue(findPreference("vibrate"));
            bindPreferenceSummaryToValue(findPreference("root_access"));

            Preference ringtonePref = findPreference("ringtone");
            if (ringtonePref != null) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
                updateRingtoneSummary(ringtonePref, sp.getString("ringtone", ""));
                ringtonePref.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                            RingtoneManager.TYPE_NOTIFICATION);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                            getString(R.string.pref_title_ringtone));
                    String current = PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .getString("ringtone", "");
                    if (!TextUtils.isEmpty(current)) {
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(current));
                    } else {
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                    }
                    ringtonePicker.launch(intent);
                    return true;
                });
            }

            Preference privacy = findPreference("privacy_policy");
            if (privacy != null) {
                privacy.setOnPreferenceClickListener(preference -> {
                    Activity act = getActivity();
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
            SwitchPreferenceCompat debugUnlock = findPreference("pro_debug_unlock");
            Activity act = getActivity();
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

        @Nullable
        @SuppressWarnings("deprecation")
        private Uri ringtoneUriFromResult(@NonNull Intent data) {
            if (Build.VERSION.SDK_INT >= 33) {
                return data.getParcelableExtra(
                        RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);
            }
            return data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        }

        private void updateRingtoneSummary(@Nullable Preference preference, @Nullable String value) {
            if (preference == null) {
                return;
            }
            if (TextUtils.isEmpty(value)) {
                preference.setSummary(R.string.pref_ringtone_silent);
                return;
            }
            Ringtone ringtone = RingtoneManager.getRingtone(requireContext(), Uri.parse(value));
            if (ringtone == null) {
                preference.setSummary(null);
            } else {
                preference.setSummary(ringtone.getTitle(requireContext()));
            }
        }

        private void refreshProPreferenceSummaries(Preference proUpgrade,
                                                   Preference proRestore,
                                                   Activity act) {
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
