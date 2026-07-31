# rsync for myrsync

Rebuilds Samba rsync as `librsync.so` for Android ABIs, with **16 KB ELF LOAD
alignment** required by Google Play for Android 15+ targets.

```bash
./native/rsync/build.sh
```

Default version: 3.4.1 (`RSYNC_VERSION` / `ANDROID_NDK_HOME` / `ANDROID_API` overridable).

Libraries are installed **with** debug/symbol info so Play can symbolicate native crashes.
AGP release builds set `ndk.debugSymbolLevel = SYMBOL_TABLE` and strip what ships to devices.
