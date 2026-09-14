#!/usr/bin/env bash
# ==============================================================================
# Automated Build & Sign APK Script for SIMAQOM
# Package: com.idyusufm.simaqom
# ==============================================================================
set -e

KEYSTORE_FILE="simaqom-release-key.jks"
KEY_ALIAS="simaqom"
KEYSTORE_PASS="android123"

if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "==> Generating new release keystore ($KEYSTORE_FILE)..."
    keytool -genkeypair -v \
      -keystore "$KEYSTORE_FILE" \
      -alias "$KEY_ALIAS" \
      -keyalg RSA \
      -keysize 2048 \
      -validity 10000 \
      -dname "CN=SIMAQOM, OU=Mobile, O=SIMAQOM, L=City, ST=State, C=US" \
      -storepass "$KEYSTORE_PASS" \
      -keypass "$KEYSTORE_PASS"
fi

echo "==> Compiling and packaging APK with Gradle..."
chmod +x ./gradlew || true
./gradlew assembleRelease -PVERSION_NAME="1.1"

echo ""
echo "======================================================================"
echo " SUCCESS: Signed APK created at: app/build/outputs/apk/release/simaqom-1.1.apk"
echo "======================================================================"
