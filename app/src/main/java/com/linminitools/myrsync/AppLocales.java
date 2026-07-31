package com.linminitools.myrsync;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import java.util.Locale;

/** Locale helpers that avoid the deprecated {@code Configuration.locale} field. */
final class AppLocales {

    private AppLocales() {}

    @NonNull
    @SuppressWarnings("deprecation")
    static Locale of(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        }
        return context.getResources().getConfiguration().locale;
    }
}
