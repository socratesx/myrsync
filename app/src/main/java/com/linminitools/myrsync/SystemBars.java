package com.linminitools.myrsync;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Pads chrome / content around system bars when edge-to-edge is enforced
 * (Android 15+ / targetSdk 35+).
 */
final class SystemBars {

    private SystemBars() {}

    static void setup(@NonNull Activity activity) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        // Edge-to-edge: draw under transparent system bars; contrast comes from icon appearance.
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }

        boolean lightUi = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        // Light theme → dark status/nav icons; dark theme → light icons.
        controller.setAppearanceLightStatusBars(lightUi);
        controller.setAppearanceLightNavigationBars(lightUi);
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
