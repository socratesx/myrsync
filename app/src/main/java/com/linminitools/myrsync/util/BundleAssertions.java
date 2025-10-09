package com.linminitools.myrsync.util;

import android.os.Bundle;

import androidx.annotation.NonNull;

public final class BundleAssertions {

    private BundleAssertions() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }

    public static void assertHasInt(@NonNull final Bundle bundle, @NonNull final String key) {
        if (!bundle.containsKey(key)) {
            throw new AssertionError(String.format("Bundle must contain key %s", key)); //$NON-NLS-1$
        }

        final Object value = bundle.get(key);
        if (!(value instanceof Integer)) {
            throw new AssertionError(String.format("Bundle key %s must be an Integer", key)); //$NON-NLS-1$
        }
    }

    public static void assertKeyCount(@NonNull final Bundle bundle, final int expectedCount) {
        if (bundle.keySet().size() != expectedCount) {
            throw new AssertionError(String.format("Bundle must contain %d keys", expectedCount)); //$NON-NLS-1$
        }
    }
}
