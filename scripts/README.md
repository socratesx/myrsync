# rsync Android build helper

This directory contains tooling used to produce the bundled `rsync` binary for Android/arm64-v8a devices such as the Poco F5 Pro and Xiaomi 14 5G.

## Prerequisites

* Android NDK r26b or newer (set `ANDROID_NDK_ROOT` or `ANDROID_NDK_HOME`).
* `git`, `autoconf`, and a POSIX shell environment.

## Usage

```bash
scripts/build_rsync_android.sh
```

The script checks out the official [`RsyncProject/rsync`](https://github.com/RsyncProject/rsync) sources, cross-compiles them for `arm64-v8a`, and exports the resulting PIE executable to:

```
app/src/main/assets/rsync_binary/arm64-v8a/rsync
```

The Gradle `preBuild` task validates that this executable exists and is marked as executable before packaging the APK. Re-run the script whenever you need to update the bundled `rsync` version or rebuild after cleaning the assets directory.
