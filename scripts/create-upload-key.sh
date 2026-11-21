#!/usr/bin/env bash
# Creates the Google Play upload key and a keystore.properties next to it.
# Keep the .jks and the passwords safe and OUT of git: losing them means asking
# Google for an upload key reset. Play App Signing keeps the real app signing key.
set -euo pipefail
OUT="${1:-$HOME/focus-upload.jks}"
read -rsp "Keystore password: " PASS; echo
keytool -genkeypair -v -keystore "$OUT" -alias upload -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$PASS" -keypass "$PASS" -dname "CN=Focus upload key"
cat > keystore.properties <<PROPS
storeFile=$OUT
storePassword=$PASS
keyAlias=upload
keyPassword=$PASS
PROPS
echo "Done. Build the bundle with: ./gradlew bundleRelease"
