# rsync Android build helper

This directory contains tooling used to produce the bundled `librsync-android.so` shared library for Android/arm64-v8a devices such as the Poco F5 Pro and Xiaomi 14 5G.

## Prerequisites

* Android NDK r26b or newer (set `ANDROID_NDK_ROOT` or `ANDROID_NDK_HOME`).
* `git`, `autoconf`, and a POSIX shell environment.

## Usage

```bash
scripts/build_rsync_android.sh
```

The script checks out the official [`RsyncProject/rsync`](https://github.com/RsyncProject/rsync) sources, cross-compiles them for `arm64-v8a`, and exports the resulting shared library to:

```
app/src/main/jniLibs/arm64-v8a/librsync-android.so
```

The Gradle `preBuild` task validates that this library exists and is non-empty before packaging the APK. Re-run the script whenever you need to update the bundled `rsync` version or rebuild after cleaning the `jniLibs` directory.
