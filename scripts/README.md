# rsync Android executable

`build_rsync_android.sh` cross-compiles rsync v3.4.1 as an arm64 Android PIE
executable. Set `ANDROID_NDK_ROOT` to NDK r26b or newer; install `git`, `make`,
`autoconf`, `automake` (including `aclocal`), `flock` (util-linux), and a POSIX shell.

```sh
scripts/build_rsync_android.sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The default Android API level is 21, matching the app's minSdk. The executable
is linked with 16 KiB segment alignment. The resulting file is packaged as
`app/src/main/jniLibs/arm64-v8a/librsync-exec.so`. Despite its suffix it is a
PIE executable, not a shared library to load with `System.loadLibrary`.

Gradle enables legacy JNI packaging and the manifest requests native-library
extraction. Android's installer places the executable in `nativeLibraryDir`.
`RsyncBridge` starts that absolute path with `ProcessBuilder`, captures stdout
and stderr concurrently, and returns the child's exit status. No shell is used.
Paths passed as arguments may contain spaces.

Do not copy an executable out of assets into app data and chmod/execute it:
Android 10+ restricts executing files from writable app data for apps targeting
API 29+. Do not call rsync's `main()` through JNI in the application process:
rsync calls `exit()` and would terminate the application.

Only arm64-v8a is currently built. Device verification should include `--version`,
a real transfer to a disposable rsync-daemon module, a failed transfer, and a
second successful transfer without restarting the app. JVM tests exercise process
exit isolation, argument preservation, missing binaries, and output-pipe draining;
they do not replace Android device tests. Storage permissions/scoped storage still
apply to the source and destination directories. Root execution is unsupported.
