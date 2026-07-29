#!/usr/bin/env bash
# Create a NEW Play upload keystore + PEM for "Request upload key reset".
# Run from repo root: ./scripts/create-play-upload-keystore.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KEYSTORE_DIR="${MYRSYNC_KEYSTORE_DIR:-$HOME/.android-keys}"
STORE_FILE="$KEYSTORE_DIR/myrsync-upload.jks"
PEM_FILE="$KEYSTORE_DIR/myrsync-upload-certificate.pem"
PROPS_FILE="$ROOT/keystore.properties"
CREDS_FILE="$KEYSTORE_DIR/myrsync-upload.credentials.txt"
ALIAS="upload"

mkdir -p "$KEYSTORE_DIR"

if [[ -f "$STORE_FILE" ]]; then
  echo "Already exists: $STORE_FILE"
  echo "Refusing to overwrite. Move/rename it if you want a fresh key."
  exit 1
fi

echo "This will create:"
echo "  $STORE_FILE"
echo "  $PEM_FILE   ← upload this in Play Console"
echo "  $PROPS_FILE (gitignored)"
echo "  $CREDS_FILE (backup — store offline)"
echo
read -r -s -p "New store/key password (min 6 chars): " STORE_PASS
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
  -dname "CN=myRSync Upload, OU=Mobile, O=linminitools, L=Unknown, ST=Unknown, C=US"

keytool -export -rfc \
  -keystore "$STORE_FILE" \
  -alias "$ALIAS" \
  -storepass "$STORE_PASS" \
  -file "$PEM_FILE"

umask 077
cat > "$PROPS_FILE" <<EOF
# Play upload key (after Console upload-key reset). Do not commit.
storeFile=$STORE_FILE
storePassword=$STORE_PASS
keyAlias=$ALIAS
keyPassword=$STORE_PASS
EOF

cat > "$CREDS_FILE" <<EOF
# BACK UP OFFLINE. This is your Play UPLOAD key after reset.
storeFile=$STORE_FILE
storePassword=$STORE_PASS
keyAlias=$ALIAS
keyPassword=$STORE_PASS
pemFile=$PEM_FILE
created=$(date -u +%Y-%m-%dT%H:%M:%SZ)
EOF

chmod 600 "$STORE_FILE" "$PROPS_FILE" "$CREDS_FILE" "$PEM_FILE"

echo
echo "Done."
echo
echo "1) In Play Console → Protected with Play → Play Store protection → Manage Play app signing"
echo "2) Upload key certificate → Request upload key reset"
echo "3) Upload this file:"
echo "   $PEM_FILE"
echo "4) After Google approves, build with:"
echo "   ./gradlew :app:bundleRelease"
echo
echo "Credentials backup: $CREDS_FILE"
