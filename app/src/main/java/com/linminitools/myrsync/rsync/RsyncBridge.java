package com.linminitools.myrsync.rsync;

import android.content.Context;
import androidx.annotation.NonNull;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/** Runs the installer-extracted rsync PIE in its own process, never via JNI main(). */
public final class RsyncBridge {
    private RsyncBridge() {}

    public static File ensureExecutable(@NonNull Context context) throws IOException {
        File executable = new File(context.getApplicationInfo().nativeLibraryDir, "librsync-exec.so");
        if (!executable.isFile() || !executable.canExecute()) {
            throw new IOException("Packaged rsync executable is missing or not executable: " + executable);
        }
        return executable;
    }

    @NonNull
    public static RsyncResult runRsync(@NonNull Context context, @NonNull String[] args)
            throws IOException, InterruptedException {
        return runExecutable(ensureExecutable(context), args);
    }

    static RsyncResult runExecutable(File executable, String[] args)
            throws IOException, InterruptedException {
        if (args.length == 0) {
            throw new IllegalArgumentException("Missing rsync argv[0]");
        }
        String[] command = Arrays.copyOf(args, args.length);
        command[0] = executable.getAbsolutePath();
        Process process = new ProcessBuilder(command).start();
        FutureTask<String> stdout = capture(process.getInputStream(), "rsync-stdout");
        FutureTask<String> stderr = capture(process.getErrorStream(), "rsync-stderr");
        try {
            // A daemon connection must not wait for interactive input from the app.
            process.getOutputStream().close();
            int exitCode = process.waitFor();
            return new RsyncResult(exitCode, stdout.get(), stderr.get());
        } catch (ExecutionException error) {
            throw new IOException("Unable to capture rsync output", error.getCause());
        } finally {
            process.destroy();
            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
        }
    }

    private static FutureTask<String> capture(InputStream stream, String name) {
        FutureTask<String> task = new FutureTask<>(() -> {
            try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        });
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
        return task;
    }
}
