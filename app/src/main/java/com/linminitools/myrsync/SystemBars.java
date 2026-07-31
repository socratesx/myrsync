package com.linminitools.myrsync;

import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Enables edge-to-edge (including pre-Android 15) and pads chrome / content
 * around system bars and display cutouts.
 */
final class SystemBars {

    private SystemBars() {}

    /**
     * Call after {@code super.onCreate()} and before or right after {@code setContentView}.
     * Uses {@link EdgeToEdge#enable} instead of deprecated bar-color APIs.
     */
    static void setup(@NonNull ComponentActivity activity) {
        EdgeToEdge.enable(activity);
    }

    /** Top padding = status bar / cutout; keeps AppBar / toolbar clear of the clock and icons. */
    static void applyTopInsets(@Nullable View appBar) {
        if (appBar == null) {
            return;
        }
        final int initialLeft = appBar.getPaddingLeft();
        final int initialTop = appBar.getPaddingTop();
        final int initialRight = appBar.getPaddingRight();
        final int initialBottom = appBar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(initialLeft + bars.left, initialTop + bars.top,
                    initialRight + bars.right, initialBottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(appBar);
    }

    /**
     * Bottom padding = navigation / gesture bar so FABs and action rows stay tappable
     * above the system nav (3-button or gesture).
     */
    static void applyBottomInsets(@Nullable View content) {
        if (content == null) {
            return;
        }
        final int initialLeft = content.getPaddingLeft();
        final int initialTop = content.getPaddingTop();
        final int initialRight = content.getPaddingRight();
        final int initialBottom = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(initialLeft, initialTop, initialRight, initialBottom + bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(content);
    }
}
