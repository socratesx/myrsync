package com.linminitools.myrsync.util;

import android.text.TextUtils;

import androidx.annotation.Nullable;

public final class Assertions {

    private Assertions() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }

    public static void assertNotNull(@Nullable final Object object, @Nullable final String name) {
        if (object == null) {
            final String message = name == null ? "Object must not be null" //$NON-NLS-1$
                    : String.format("%s must not be null", name); //$NON-NLS-1$
            throw new AssertionError(message);
        }
    }

    public static void assertNotEmpty(@Nullable final CharSequence value,
                                      @Nullable final String name) {
        if (TextUtils.isEmpty(value)) {
            final String message = name == null ? "Value must not be empty" //$NON-NLS-1$
                    : String.format("%s must not be empty", name); //$NON-NLS-1$
            throw new AssertionError(message);
        }
    }
}
