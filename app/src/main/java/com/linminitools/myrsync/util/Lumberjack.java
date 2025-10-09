package com.linminitools.myrsync.util;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class Lumberjack {

    private static final String TAG = "myRsync"; //$NON-NLS-1$

    private Lumberjack() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }

    public static void e(@NonNull final String message, @Nullable final Object... args) {
        Throwable throwable = null;
        if (args != null && args.length > 0) {
            final Object last = args[args.length - 1];
            if (last instanceof Throwable) {
                throwable = (Throwable) last;
            }
        }

        final String formattedMessage;
        if (args != null && args.length > 0) {
            formattedMessage = String.format(Locale.US, message, args);
        } else {
            formattedMessage = message;
        }

        if (throwable != null) {
            Log.e(TAG, formattedMessage, throwable);
        } else {
            Log.e(TAG, formattedMessage);
        }
    }
}
