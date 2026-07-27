package com.linminitools.myrsync;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Pads toolbars so they sit below the status bar when edge-to-edge is enforced
 * (Android 15+ / targetSdk 35+).
 */
final class SystemBars {

    private SystemBars() {}

    static void setup(@NonNull Activity activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
    }

    /** Top padding = status bar; keeps AppBar / toolbar clear of the clock and icons. */
    static void applyTopInsets(@Nullable View appBar) {
        if (appBar == null) {
            return;
        }
        final int initialLeft = appBar.getPaddingLeft();
        final int initialTop = appBar.getPaddingTop();
        final int initialRight = appBar.getPaddingRight();
        final int initialBottom = appBar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(initialLeft + bars.left, initialTop + bars.top,
                    initialRight + bars.right, initialBottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(appBar);
    }
}
