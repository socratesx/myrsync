package com.linminitools.myrsync.tasker;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Contract describing the Tasker plug-in protocol supported by myRSync.
 *
 * <p>This implementation is based on the modern Tasker plug-in specification exposed
 * by the Tasker Plugin Library. The contract avoids any dependency on the legacy
 * twofortyfouram packages that used to ship with Locale.</p>
 */
public final class TaskerPluginContract {

    /**
     * Intent action fired by Tasker when a setting should be executed.
     */
    @NonNull
    public static final String ACTION_FIRE_SETTING =
            "com.joaomgcd.tasker.intent.action.FIRE_SETTING";

    /**
     * Intent action fired by Tasker when the user edits an existing setting.
     */
    @NonNull
    public static final String ACTION_EDIT_SETTING =
            "com.joaomgcd.tasker.intent.action.EDIT_SETTING";

    /**
     * Extra key describing the plug-in bundle Tasker expects.
     */
    @NonNull
    public static final String EXTRA_BUNDLE =
            "com.joaomgcd.tasker.intent.extra.BUNDLE";

    /**
     * Extra key describing the blurb Tasker displays to the user.
     */
    @NonNull
    public static final String EXTRA_STRING_BLURB =
            "com.joaomgcd.tasker.intent.extra.BLURB";

    private TaskerPluginContract() {
        throw new AssertionError("No instances");
    }

    /**
     * Utility helper that extracts the bundle payload from an intent created by Tasker.
     *
     * @param intent The incoming intent.
     * @return The payload bundle or {@code null} if none exists.
     */
    @Nullable
    public static Bundle extractBundle(@Nullable final Intent intent) {
        if (intent == null) {
            return null;
        }

        final Bundle bundle = intent.getBundleExtra(EXTRA_BUNDLE);
        if (bundle != null) {
            return bundle;
        }

        // The modern Tasker library still understands the old Locale constants. To maintain
        // backwards compatibility with users that may still have workflows relying on the
        // legacy names, we silently fall back to them when present. This keeps the new
        // implementation interoperable without bringing the legacy dependency back.
        return intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE");
    }

    /**
     * Reads the blurb stored in an intent and falls back to the legacy extra name when needed.
     */
    @Nullable
    public static String extractBlurb(@Nullable final Intent intent) {
        if (intent == null) {
            return null;
        }

        final String blurb = intent.getStringExtra(EXTRA_STRING_BLURB);
        if (blurb != null) {
            return blurb;
        }

        return intent.getStringExtra("com.twofortyfouram.locale.intent.extra.BLURB");
    }
}
