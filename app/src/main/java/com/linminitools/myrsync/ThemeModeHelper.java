package com.linminitools.myrsync;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Maps preference {@code theme_mode} values to {@link AppCompatDelegate} night modes.
 * Uses semantic strings ({@code follow_system}, {@code light}, {@code dark}) so the setting
 * cannot be confused with raw integer constants; also migrates legacy {@code -1} / {@code 1} / {@code 2}.
 */
public final class ThemeModeHelper {

    private ThemeModeHelper() {}

    /** Call from {@link android.app.Application#onCreate()} before {@link AppCompatDelegate#setDefaultNightMode(int)}. */
    public static void migrateLegacyThemePreference(@NonNull SharedPreferences prefs) {
        String raw = prefs.getString("theme_mode", null);
        if (raw == null || raw.isEmpty()) {
            prefs.edit().putString("theme_mode", "follow_system").apply();
            return;
        }
        switch (raw) {
            case "follow_system":
            case "light":
            case "dark":
                return;
            case "-1":
                prefs.edit().putString("theme_mode", "follow_system").apply();
                break;
            case "1":
                prefs.edit().putString("theme_mode", "light").apply();
                break;
            case "2":
                prefs.edit().putString("theme_mode", "dark").apply();
                break;
            default:
                prefs.edit().putString("theme_mode", "follow_system").apply();
                break;
        }
    }

    public static void applyFromPreferenceString(String raw) {
        AppCompatDelegate.setDefaultNightMode(parseNightMode(raw));
    }

    public static int parseNightMode(String raw) {
        if (raw == null) raw = "";
        switch (raw) {
            case "light":
                return AppCompatDelegate.MODE_NIGHT_NO;
            case "dark":
                return AppCompatDelegate.MODE_NIGHT_YES;
            case "follow_system":
            case "-1":
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            case "1":
                return AppCompatDelegate.MODE_NIGHT_NO;
            case "2":
                return AppCompatDelegate.MODE_NIGHT_YES;
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }
}
