package com.linminitools.myrsync.rsync;

import org.junit.Test;
import java.io.File;
import java.io.IOException;
import static org.junit.Assert.*;

public class RsyncBridgeTest {
    private RsyncResult shell(String script, String... arguments) throws Exception {
        String[] argv = new String[4 + arguments.length];
        argv[0] = "rsync";
        argv[1] = "-c";
        argv[2] = script;
        argv[3] = "test";
        System.arraycopy(arguments, 0, argv, 4, arguments.length);
        return RsyncBridge.runExecutable(new File("/bin/sh"), argv);
    }

    @Test public void preservesArgumentsAndSeparatesOutput() throws Exception {
        RsyncResult result = shell("printf '%s' \"$1\"; printf 'diagnostic' >&2", "folder with spaces; $literal");
        assertEquals(0, result.getExitCode());
        assertEquals("folder with spaces; $literal", result.getStdout());
        assertEquals("diagnostic", result.getStderr());
    }

    @Test public void failedChildDoesNotTerminateCallerOrPoisonNextRun() throws Exception {
        assertEquals(23, shell("exit 23").getExitCode());
        assertEquals("still alive", shell("printf 'still alive'").getStdout());
    }

    @Test(timeout = 15000) public void drainsBothPipesBeyondTheirCapacity() throws Exception {
        RsyncResult result = shell("i=0; while [ $i -lt 12000 ]; do printf 'stdout-line\\n'; printf 'stderr-line\\n' >&2; i=$((i+1)); done");
        assertEquals(0, result.getExitCode());
        assertEquals(12000 * "stdout-line\n".length(), result.getStdout().length());
        assertEquals(12000 * "stderr-line\n".length(), result.getStderr().length());
    }

    @Test(expected = IOException.class) public void reportsMissingExecutable() throws Exception {
        RsyncBridge.runExecutable(new File("/does-not-exist/rsync"), new String[]{"rsync"});
    }
}
