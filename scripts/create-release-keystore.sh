#!/usr/bin/env bash
# Creates a Play release keystore outside the repo and writes keystore.properties.
# Run once from the repo root: ./scripts/create-release-keystore.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KEYSTORE_DIR="${MYRSYNC_KEYSTORE_DIR:-$HOME/.android-keys}"
STORE_FILE="$KEYSTORE_DIR/myrsync-release.jks"
PROPS_FILE="$ROOT/keystore.properties"
CREDS_FILE="$KEYSTORE_DIR/myrsync-release.credentials.txt"
ALIAS="myrsync"

mkdir -p "$KEYSTORE_DIR"

if [[ -f "$STORE_FILE" ]]; then
  echo "Keystore already exists: $STORE_FILE"
  echo "Refusing to overwrite. Delete it manually if you really want a new one."
  exit 1
fi

if [[ -f "$PROPS_FILE" ]]; then
  echo "keystore.properties already exists. Move/remove it first."
  exit 1
fi

echo "This will create:"
echo "  $STORE_FILE"
echo "  $PROPS_FILE (gitignored)"
echo "  $CREDS_FILE (backup — store offline)"
echo
read -r -s -p "Store/key password (min 6 chars): " STORE_PASS
echo
read -r -s -p "Confirm password: " STORE_PASS2
echo
if [[ "$STORE_PASS" != "$STORE_PASS2" ]]; then
  echo "Passwords do not match."
  exit 1
fi
if [[ ${#STORE_PASS} -lt 6 ]]; then
  echo "Password too short."
  exit 1
fi

keytool -genkeypair -v \
  -keystore "$STORE_FILE" \
  -alias "$ALIAS" \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$STORE_PASS" -keypass "$STORE_PASS" \
  -dname "CN=myRSync, OU=Mobile, O=linminitools, L=Unknown, ST=Unknown, C=US"

umask 077
cat > "$PROPS_FILE" <<EOF
storeFile=$STORE_FILE
storePassword=$STORE_PASS
keyAlias=$ALIAS
keyPassword=$STORE_PASS
EOF

cat > "$CREDS_FILE" <<EOF
# BACK UP OFFLINE. Losing this keystore means you cannot update the Play app.
storeFile=$STORE_FILE
storePassword=$STORE_PASS
keyAlias=$ALIAS
keyPassword=$STORE_PASS
created=$(date -u +%Y-%m-%dT%H:%M:%SZ)
EOF

chmod 600 "$STORE_FILE" "$PROPS_FILE" "$CREDS_FILE"

echo
echo "Done."
echo "Build a signed release with: ./gradlew :app:bundleRelease"
echo "Credentials backup: $CREDS_FILE"
