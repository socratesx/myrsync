#!/usr/bin/env bash
# Rebuild rsync for Android with 16 KB ELF LOAD alignment (Play requirement).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$ROOT/../.." && pwd)"
VERSION="${RSYNC_VERSION:-3.4.1}"
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
SRC_DIR="$ROOT/rsync-$VERSION"
TARBALL="$ROOT/rsync-$VERSION.tar.gz"
OUT_JNILIBS="$REPO_ROOT/app/src/main/jniLibs"

if [[ ! -f "$TARBALL" ]]; then
  echo "Downloading rsync $VERSION..."
  curl -fsSL -o "$TARBALL" \
    "https://download.samba.org/pub/rsync/src/rsync-${VERSION}.tar.gz"
fi

if [[ ! -d "$SRC_DIR" ]]; then
  tar -xzf "$TARBALL" -C "$ROOT"
fi

build_abi() {
  local abi="$1"
  local target="$2"
  local build_dir="$ROOT/build-$abi"

  echo "==== Building rsync $abi ($target$API) ===="
  rm -rf "$build_dir"
  mkdir -p "$build_dir"
  rm -rf "$build_dir/src"
  cp -a "$SRC_DIR" "$build_dir/src"

  local cc="$TOOLCHAIN/bin/${target}${API}-clang"
  local ar="$TOOLCHAIN/bin/llvm-ar"
  local ranlib="$TOOLCHAIN/bin/llvm-ranlib"
  local strip="$TOOLCHAIN/bin/llvm-strip"

  pushd "$build_dir/src" >/dev/null
  ./configure \
    --host="$target" \
    --disable-md2man \
    --with-included-zlib \
    --disable-openssl \
    --disable-xxhash \
    --disable-zstd \
    --disable-lz4 \
    AR="$ar" \
    CC="$cc" \
    RANLIB="$ranlib" \
    STRIP="$strip" \
    CFLAGS="-O2 -fPIC" \
    LDFLAGS="-O2 $PAGE_LDFLAGS"

  make -j"$(sysctl -n hw.ncpu 2>/dev/null || echo 4)"
  "$strip" rsync

  mkdir -p "$OUT_JNILIBS/$abi"
  cp -f rsync "$OUT_JNILIBS/$abi/librsync.so"
  chmod 755 "$OUT_JNILIBS/$abi/librsync.so"

  python3 - "$OUT_JNILIBS/$abi/librsync.so" <<'PY'
import struct, sys
path = sys.argv[1]
data = open(path, "rb").read()
if data[:4] != b"\x7fELF":
    raise SystemExit("not ELF")
ei_class = data[4]
if ei_class == 2:
    e_phoff = struct.unpack_from("<Q", data, 32)[0]
    e_phentsize = struct.unpack_from("<H", data, 54)[0]
    e_phnum = struct.unpack_from("<H", data, 56)[0]
    align_off, align_fmt = 48, "<Q"
else:
    e_phoff = struct.unpack_from("<I", data, 28)[0]
    e_phentsize = struct.unpack_from("<H", data, 42)[0]
    e_phnum = struct.unpack_from("<H", data, 44)[0]
    align_off, align_fmt = 28, "<I"
bad = []
for i in range(e_phnum):
    off = e_phoff + i * e_phentsize
    if struct.unpack_from("<I", data, off)[0] != 1:
        continue
    p_align = struct.unpack_from(align_fmt, data, off + align_off)[0]
    if p_align < 0x4000:
        bad.append(hex(p_align))
if bad:
    raise SystemExit("ERROR: LOAD align not 16KB: " + ", ".join(bad))
print("16KB LOAD alignment OK:", path)
PY

  echo "Installed $OUT_JNILIBS/$abi/librsync.so ($(wc -c < "$OUT_JNILIBS/$abi/librsync.so") bytes)"
  popd >/dev/null
}

build_abi arm64-v8a aarch64-linux-android
build_abi armeabi-v7a armv7a-linux-androideabi
build_abi x86_64 x86_64-linux-android

echo "Done. Rebuild the Android app to pick up the new native libs."
