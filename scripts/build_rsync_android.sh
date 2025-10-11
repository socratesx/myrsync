#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
RSYNC_REPO_URL=${RSYNC_REPO_URL:-"https://github.com/RsyncProject/rsync.git"}
RSYNC_VERSION=${RSYNC_VERSION:-"v3.4.1"}
ANDROID_API_LEVEL=${ANDROID_API_LEVEL:-"24"}
ANDROID_ABI=${ANDROID_ABI:-"arm64-v8a"}
LIB_OUTPUT_DIR=${LIB_OUTPUT_DIR:-"$ROOT_DIR/app/src/main/jniLibs/$ANDROID_ABI"}
BUILD_DIR=${BUILD_DIR:-"$ROOT_DIR/build/rsync-android/$ANDROID_ABI"}
RSYNC_SRC_DIR=${RSYNC_SRC_DIR:-"$ROOT_DIR/build/rsync-src"}

if ! command -v git >/dev/null 2>&1; then
  echo "[rsync-android] git is required to fetch Rsync sources." >&2
  exit 1
fi

if ! command -v autoconf >/dev/null 2>&1; then
  echo "[rsync-android] autoconf is required (install autotools)." >&2
  exit 1
fi

if [[ -z "${ANDROID_NDK_ROOT:-${ANDROID_NDK_HOME:-}}" ]]; then
  echo "[rsync-android] ANDROID_NDK_ROOT or ANDROID_NDK_HOME must point to a valid Android NDK installation." >&2
  exit 1
fi

NDK_ROOT=${ANDROID_NDK_ROOT:-${ANDROID_NDK_HOME:-}}
if [[ ! -d "$NDK_ROOT/toolchains/llvm" ]]; then
  echo "[rsync-android] Unable to locate LLVM toolchain inside NDK root: $NDK_ROOT" >&2
  exit 1
fi

TOOLCHAIN="$NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64"
if [[ ! -d "$TOOLCHAIN" ]]; then
  echo "[rsync-android] Expected toolchain directory not found: $TOOLCHAIN" >&2
  exit 1
fi

HOST_TAG="aarch64-linux-android"
CC="$TOOLCHAIN/bin/${HOST_TAG}${ANDROID_API_LEVEL}-clang"
CXX="$TOOLCHAIN/bin/${HOST_TAG}${ANDROID_API_LEVEL}-clang++"
AR="$TOOLCHAIN/bin/llvm-ar"
AS="$TOOLCHAIN/bin/llvm-as"
RANLIB="$TOOLCHAIN/bin/llvm-ranlib"
STRIP="$TOOLCHAIN/bin/llvm-strip"

mkdir -p "$RSYNC_SRC_DIR"

if [[ ! -d "$RSYNC_SRC_DIR/.git" ]]; then
  echo "[rsync-android] Fetching rsync sources from $RSYNC_REPO_URL"
  rm -rf "$RSYNC_SRC_DIR"
  git clone --depth 1 "$RSYNC_REPO_URL" "$RSYNC_SRC_DIR"
fi

pushd "$RSYNC_SRC_DIR" >/dev/null

CURRENT_REMOTE=$(git config --get remote.origin.url || echo "")
if [[ "$CURRENT_REMOTE" != "$RSYNC_REPO_URL" ]]; then
  popd >/dev/null
  echo "[rsync-android] Existing rsync checkout points to $CURRENT_REMOTE – resetting to $RSYNC_REPO_URL"
  rm -rf "$RSYNC_SRC_DIR"
  git clone --depth 1 "$RSYNC_REPO_URL" "$RSYNC_SRC_DIR"
  pushd "$RSYNC_SRC_DIR" >/dev/null
fi

echo "[rsync-android] Checking out $RSYNC_VERSION"

if git fetch --depth 1 origin tag "$RSYNC_VERSION" >/dev/null 2>&1; then
  git checkout --force "refs/tags/$RSYNC_VERSION"
  git reset --hard "refs/tags/$RSYNC_VERSION"
elif git fetch --depth 1 origin "$RSYNC_VERSION" >/dev/null 2>&1; then
  git checkout --force -B "$RSYNC_VERSION" "origin/$RSYNC_VERSION"
  git reset --hard "origin/$RSYNC_VERSION"
else
  echo "[rsync-android] Unable to fetch $RSYNC_VERSION from $RSYNC_REPO_URL" >&2
  echo "[rsync-android] Verify that the tag or branch exists and that the network is available." >&2
  exit 1
fi

autoconf -o configure.sh
chmod +x configure.sh

mkdir -p "$BUILD_DIR"
rm -rf "$BUILD_DIR"/*

export CC CXX AR AS RANLIB STRIP
export CFLAGS="--target=${HOST_TAG}${ANDROID_API_LEVEL} -fPIE -fPIC"
export LDFLAGS="-fPIE -pie"
export CPPFLAGS="-D__ANDROID_API__=${ANDROID_API_LEVEL}"

./configure.sh \
  --host=${HOST_TAG} \
  --build=$(./config.guess) \
  --with-included-zlib=yes \
  --with-included-popt=yes \
  --disable-md2man \
  --disable-openssl \
  --disable-xxhash \
  --disable-zstd \
  --disable-lz4 \
  --disable-iconv \
  --disable-debug \
  --disable-locale \
  --prefix="$BUILD_DIR/prefix"

make -j"${JOBS:-$(nproc)}"
make install

RSYNC_BIN="$BUILD_DIR/prefix/bin/rsync"
if [[ ! -f "$RSYNC_BIN" ]]; then
  echo "[rsync-android] Build succeeded but could not locate rsync binary at $RSYNC_BIN" >&2
  exit 1
fi

mkdir -p "$LIB_OUTPUT_DIR"

WRAPPER_SRC="$ROOT_DIR/scripts/rsync_android_wrapper.c"
WRAPPER_OBJ="$BUILD_DIR/rsync_android_wrapper.o"

echo "[rsync-android] Compiling Android wrapper"
"$CC" -fPIC $CPPFLAGS -c "$WRAPPER_SRC" -o "$WRAPPER_OBJ"

echo "[rsync-android] Collecting build artifacts for shared library"
mapfile -t OBJECT_FILES < <(find . -type f -name '*.o' \
  ! -path './support/*' \
  ! -path './testsuite/*' \
  ! -path './packaging/*')
mapfile -t STATIC_LIBS < <(find . -type f -name '*.a' \
  ! -path './support/*' \
  ! -path './testsuite/*' \
  ! -path './packaging/*')

if [[ ${#OBJECT_FILES[@]} -eq 0 ]]; then
  echo "[rsync-android] Unable to locate compiled object files. Verify that the build succeeded." >&2
  exit 1
fi

echo "[rsync-android] Linking shared library"
"$CC" -shared -Wl,-soname,librsync-android.so \
  -o "$LIB_OUTPUT_DIR/librsync-android.so" \
  "${OBJECT_FILES[@]}" \
  "$WRAPPER_OBJ" \
  "${STATIC_LIBS[@]}" \
  -llog

chmod 755 "$LIB_OUTPUT_DIR/librsync-android.so"

${STRIP} --strip-unneeded "$LIB_OUTPUT_DIR/librsync-android.so" || true

echo "[rsync-android] rsync shared library exported to $LIB_OUTPUT_DIR/librsync-android.so"

popd >/dev/null
