package com.linminitools.myrsync;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ScrollView;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Enables edge-to-edge (including pre-Android 15) and pads chrome / content
 * around system bars and display cutouts.
 *
 * <p>Does not call {@code EdgeToEdge.enable}. That helper still invokes
 * {@code Window.setStatusBarColor}, {@code Window.setNavigationBarColor}, and
 * {@code LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES}, which Play Console flags.
 * Bar colors come from the theme; icon contrast is set here.
 */
final class SystemBars {

    private SystemBars() {}

    /**
     * Call after {@code super.onCreate()} and before {@code setContentView}.
     */
    static void setup(@NonNull ComponentActivity activity) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        // ALWAYS is the non-deprecated cutout mode (API 30+). API 28–29 keep the
        // platform default so the deprecated SHORT_EDGES constant stays out of the APK.
        if (Build.VERSION.SDK_INT >= 30) {
            WindowManager.LayoutParams attrs = window.getAttributes();
            if (attrs.layoutInDisplayCutoutMode
                    != WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS) {
                attrs.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                window.setAttributes(attrs);
            }
        }
        boolean night = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(!night);
        controller.setAppearanceLightNavigationBars(!night);
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
     * Bottom padding clears the navigation / gesture bar, and the keyboard when it is
     * taller. The window does not shrink for the IME ({@code decorFitsSystemWindows}
     * is false), so a focused field in a {@link ScrollView} is scrolled above that padding.
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
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(initialLeft, initialTop, initialRight, initialBottom + bars.bottom);
            revealFocusedField(v);
            return windowInsets;
        });
        if (content instanceof ScrollView) {
            watchFocus((ScrollView) content);
        }
        ViewCompat.requestApplyInsets(content);
    }

    /** Keep the focused field above the keyboard when focus moves while the IME is already open. */
    private static void watchFocus(@NonNull ScrollView scrollView) {
        ViewTreeObserver.OnGlobalFocusChangeListener listener = (oldFocus, newFocus) -> {
            if (newFocus != null) {
                revealFocusedField(scrollView);
            }
        };
        scrollView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                v.getViewTreeObserver().addOnGlobalFocusChangeListener(listener);
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                v.getViewTreeObserver().removeOnGlobalFocusChangeListener(listener);
            }
        });
        if (scrollView.isAttachedToWindow()) {
            scrollView.getViewTreeObserver().addOnGlobalFocusChangeListener(listener);
        }
    }

    private static boolean revealingField;

    /**
     * Scroll so the focused descendant sits above the bottom inset. ScrollView treats the
     * padded region as visible, which is exactly where the keyboard is drawn.
     */
    private static void revealFocusedField(@NonNull View container) {
        if (revealingField || !(container instanceof ScrollView) || container.getHeight() == 0) {
            return;
        }
        View focused = container.findFocus();
        if (focused == null || focused == container) {
            return;
        }
        Rect rect = new Rect(0, 0, focused.getWidth(), focused.getHeight());
        try {
            ((ScrollView) container).offsetDescendantRectToMyCoords(focused, rect);
        } catch (IllegalArgumentException notDescendant) {
            return;
        }
        int slack = Math.round(16f * container.getResources().getDisplayMetrics().density);
        int visibleBottom = container.getScrollY() + container.getHeight()
                - container.getPaddingBottom() - slack;
        int overlap = rect.bottom - visibleBottom;
        if (overlap > 0) {
            revealingField = true;
            try {
                ((ScrollView) container).scrollBy(0, overlap);
            } finally {
                revealingField = false;
            }
        }
    }
}
