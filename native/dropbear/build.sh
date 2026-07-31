#!/usr/bin/env bash
# Rebuild Dropbear dbclient/dropbearconvert for Android with password auth.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$ROOT/../.." && pwd)"
VERSION="${DROPBEAR_VERSION:-2025.89}"
API="${ANDROID_API:-24}"
NDK="${ANDROID_NDK_HOME:-${ANDROID_HOME:+$ANDROID_HOME/ndk/25.2.9519653}}"
NDK="${NDK:-$HOME/Library/Android/sdk/ndk/25.2.9519653}"
PAGE_LDFLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"

if [[ ! -d "$NDK" ]]; then
  echo "Android NDK not found. Set ANDROID_NDK_HOME." >&2
  exit 1
fi

HOST_TAG="$(ls "$NDK/toolchains/llvm/prebuilt" | head -1)"
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/$HOST_TAG"
SRC_DIR="$ROOT/dropbear-$VERSION"
TARBALL="$ROOT/dropbear-$VERSION.tar.bz2"
OUT_JNILIBS="$REPO_ROOT/app/src/main/jniLibs"

if [[ ! -f "$TARBALL" ]]; then
  echo "Downloading Dropbear $VERSION..."
  curl -fsSL -o "$TARBALL" \
    "https://matt.ucc.asn.au/dropbear/releases/dropbear-${VERSION}.tar.bz2"
fi

if [[ ! -d "$SRC_DIR" ]]; then
  tar -xjf "$TARBALL" -C "$ROOT"
fi

# Dropbear looks for localoptions.h in the source tree.
cp "$ROOT/localoptions.h" "$SRC_DIR/localoptions.h"

build_abi() {
  local abi="$1"
  local target="$2"
  local build_dir="$ROOT/build-$abi"

  echo "==== Building $abi ($target$API) ===="
  rm -rf "$build_dir"
  mkdir -p "$build_dir"
  # Keep a clean per-ABI source copy so configure/make do not clash.
  rm -rf "$build_dir/src"
  cp -a "$SRC_DIR" "$build_dir/src"
  cp "$ROOT/localoptions.h" "$build_dir/src/localoptions.h"

  local cc="$TOOLCHAIN/bin/${target}${API}-clang"
  local ar="$TOOLCHAIN/bin/llvm-ar"
  local ranlib="$TOOLCHAIN/bin/llvm-ranlib"
  local strip="$TOOLCHAIN/bin/llvm-strip"

  # Compile getpass stub for this ABI.
  "$cc" -c -O2 -fPIC -o "$build_dir/android_getpass.o" "$ROOT/android_getpass.c"

  pushd "$build_dir/src" >/dev/null
  ./configure \
    --host="$target" \
    --disable-zlib \
    --disable-syslog \
    --disable-utmp \
    --disable-wtmp \
    --disable-lastlog \
    --disable-pututline \
    --disable-pututxline \
    AR="$ar" \
    CC="$cc" \
    RANLIB="$ranlib" \
    STRIP="$strip" \
    CFLAGS="-O2 -g -fPIC" \
    LDFLAGS="-O2 -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"

  # Ensure our getpass is linked into client programs.
  if ! grep -q 'android_getpass.o' Makefile; then
    # Append to LDLIBS-style link for dbclient / dropbearconvert.
    # Dropbear uses $(CC) ... $(LIBS) when linking PROGRAMS.
    if grep -q '^LIBS=' Makefile; then
      sed -i.bak "s|^LIBS=|LIBS=$build_dir/android_getpass.o |" Makefile
    else
      echo "LIBS=$build_dir/android_getpass.o" >> Makefile
    fi
  fi

  make -j"$(sysctl -n hw.ncpu 2>/dev/null || echo 4)" \
    PROGRAMS="dbclient dropbearconvert"
  # Keep debug/symbol info for Play native crash symbolication.
  # AGP release builds package symbols via ndk.debugSymbolLevel and strip the shipped .so.

  mkdir -p "$OUT_JNILIBS/$abi"
  cp -f dbclient "$OUT_JNILIBS/$abi/libdbclient.so"
  cp -f dropbearconvert "$OUT_JNILIBS/$abi/libdbconvert.so"
  chmod 755 "$OUT_JNILIBS/$abi/libdbclient.so" "$OUT_JNILIBS/$abi/libdbconvert.so"

  # Sanity: password auth must be compiled in (avoid grep -q | pipefail SIGPIPE).
  if ! python3 - "$OUT_JNILIBS/$abi/libdbclient.so" <<'PY'
import sys
data = open(sys.argv[1], "rb").read()
need = [b"password", b"DROPBEAR_PASSWORD", b"%s@%s's password:"]
missing = [x.decode() for x in need if x not in data]
if missing:
    print("missing:", ", ".join(missing), file=sys.stderr)
    sys.exit(1)
PY
  then
    echo "ERROR: rebuilt libdbclient.so for $abi lacks password auth" >&2
    exit 1
  fi

  python3 - "$OUT_JNILIBS/$abi/libdbclient.so" "$OUT_JNILIBS/$abi/libdbconvert.so" <<'PY'
import struct, sys

def check(path):
    data = open(path, "rb").read()
    if data[:4] != b"\x7fELF":
        raise SystemExit(f"ERROR: {path} is not ELF")
    ei_class = data[4]  # 1=32-bit, 2=64-bit
    if ei_class == 2:
        e_phoff = struct.unpack_from("<Q", data, 32)[0]
        e_phentsize = struct.unpack_from("<H", data, 54)[0]
        e_phnum = struct.unpack_from("<H", data, 56)[0]
        align_off = 48
        align_fmt = "<Q"
    else:
        e_phoff = struct.unpack_from("<I", data, 28)[0]
        e_phentsize = struct.unpack_from("<H", data, 42)[0]
        e_phnum = struct.unpack_from("<H", data, 44)[0]
        align_off = 28
        align_fmt = "<I"
    for i in range(e_phnum):
        off = e_phoff + i * e_phentsize
        if struct.unpack_from("<I", data, off)[0] != 1:
            continue
        align = struct.unpack_from(align_fmt, data, off + align_off)[0]
        if align < 0x4000:
            raise SystemExit(f"ERROR: {path} LOAD align {hex(align)} < 16KB")
    print("16KB LOAD alignment OK:", path)

for p in sys.argv[1:]:
    check(p)
PY

  echo "Installed $OUT_JNILIBS/$abi/libdbclient.so ($(wc -c < "$OUT_JNILIBS/$abi/libdbclient.so") bytes)"
  popd >/dev/null
}

build_abi arm64-v8a aarch64-linux-android
build_abi armeabi-v7a armv7a-linux-androideabi
build_abi x86_64 x86_64-linux-android

echo "Done. Rebuild the Android app to pick up the new native libs."
