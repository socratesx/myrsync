package com.linminitools.myrsync.rsync;

import androidx.annotation.NonNull;

public final class RsyncResult {
    private final int exitCode;
    @NonNull
    private final String stdout;
    @NonNull
    private final String stderr;

    public RsyncResult(int exitCode, @NonNull String stdout, @NonNull String stderr) {
        this.exitCode = exitCode;
        this.stdout = stdout == null ? "" : stdout;
        this.stderr = stderr == null ? "" : stderr;
    }

    public int getExitCode() {
        return exitCode;
    }

    @NonNull
    public String getStdout() {
        return stdout;
    }

    @NonNull
    public String getStderr() {
        return stderr;
    }
}
