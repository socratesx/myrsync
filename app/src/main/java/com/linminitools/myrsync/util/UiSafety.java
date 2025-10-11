package com.linminitools.myrsync.util;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public final class UiSafety {

    private UiSafety() {
    }

    public static boolean isUiDead(@Nullable Activity activity) {
        if (activity == null) {
            return true;
        }
        if (activity.isFinishing()) {
            return true;
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed();
    }

    public static void runOnUiThreadIfAlive(@Nullable Activity activity, @NonNull Runnable action) {
        if (activity == null || isUiDead(activity)) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (!isUiDead(activity)) {
                action.run();
            }
            return;
        }
        activity.runOnUiThread(() -> {
            if (!isUiDead(activity)) {
                action.run();
            }
        });
    }

    public static void safelyTearDownViewTree(@Nullable View root) {
        if (root == null) {
            return;
        }
        cancelViewAnimations(root);
        clearViewDrawables(root);
        if (root instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) root;
            for (int index = 0; index < viewGroup.getChildCount(); index++) {
                safelyTearDownViewTree(viewGroup.getChildAt(index));
            }
        }
    }

    public static void stripDecorViewDrawables(@Nullable View root) {
        if (root == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Drawable foreground = root.getForeground();
            if (foreground != null) {
                foreground.setCallback(null);
            }
            root.setForeground(null);
        }
        Drawable background = root.getBackground();
        if (background != null) {
            background.setCallback(null);
        }
        root.setBackground(null);
    }

    private static void cancelViewAnimations(@NonNull View view) {
        view.clearAnimation();
        view.animate().cancel();
    }

    private static void clearViewDrawables(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Drawable foreground = view.getForeground();
            if (foreground != null) {
                foreground.setCallback(null);
            }
            view.setForeground(null);
        }

        Drawable background = view.getBackground();
        if (background != null) {
            background.setCallback(null);
        }
        view.setBackground(null);

        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            Drawable drawable = imageView.getDrawable();
            if (drawable != null) {
                drawable.setCallback(null);
            }
            imageView.setImageDrawable(null);
        }

        if (view instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
            if (animator != null) {
                animator.endAnimations();
            }
            recyclerView.setItemAnimator(null);
        }
    }
}
