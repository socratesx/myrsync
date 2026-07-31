# Dropbear (dbclient) for myrsync

Stock Android Dropbear builds often disable client password auth (`DROPBEAR_CLI_PASSWORD_AUTH=0`) because Android libc has no `getpass()`. That made password configs fail with:

`No auth methods could be used`

even when `DROPBEAR_PASSWORD` was set.

## Rebuild

Requires Android NDK (default: `$HOME/Library/Android/sdk/ndk/25.2.9519653`).

```bash
./native/dropbear/build.sh
```

Writes `libdbclient.so` / `libdbconvert.so` into `app/src/main/jniLibs/{arm64-v8a,armeabi-v7a,x86_64}/`.

Options live in `localoptions.h` (`DROPBEAR_CLI_PASSWORD_AUTH`, `DROPBEAR_USE_PASSWORD_ENV`).

Binaries are linked with `-Wl,-z,max-page-size=16384` for Google Play 16 KB page-size compatibility.

Libraries are installed **with** debug/symbol info so Play can symbolicate native crashes.
AGP release builds set `ndk.debugSymbolLevel = SYMBOL_TABLE` and strip what ships to devices.
