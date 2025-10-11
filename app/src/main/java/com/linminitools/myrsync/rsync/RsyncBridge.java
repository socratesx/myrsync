package com.linminitools.myrsync.rsync;

import androidx.annotation.NonNull;

/**
 * Thin JNI wrapper that exposes rsync's entry point within the host process.
 */
public final class RsyncBridge {
    private static volatile boolean loaded;
    private static volatile UnsatisfiedLinkError loadError;

    private RsyncBridge() {
    }

    private static void loadLibraries() {
        if (loaded) {
            return;
        }
        synchronized (RsyncBridge.class) {
            if (loaded) {
                return;
            }
            if (loadError != null) {
                throw loadError;
            }
            try {
                System.loadLibrary("rsync-android");
                System.loadLibrary("myrsync-jni");
                loaded = true;
            } catch (UnsatisfiedLinkError error) {
                loadError = error;
                throw error;
            }
        }
    }

    public static void ensureLoaded() {
        loadLibraries();
    }

    @NonNull
    public static RsyncResult runRsync(@NonNull String[] args) {
        if (args == null) {
            throw new NullPointerException("args == null");
        }
        loadLibraries();
        RsyncResult result = runRsyncNative(args);
        if (result == null) {
            throw new IllegalStateException("Native rsync invocation returned null result");
        }
        return result;
    }

    private static native RsyncResult runRsyncNative(String[] args);
}
