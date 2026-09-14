@echo off
echo ==============================================================================
echo Automated Build and Sign APK for SIMAQOM
echo ==============================================================================

set KEYSTORE_FILE=simaqom-release-key.jks
set KEY_ALIAS=simaqom
set KEYSTORE_PASS=android123

if not exist %KEYSTORE_FILE% (
    echo Generating release keystore...
    keytool -genkey -v -keystore %KEYSTORE_FILE% -alias %KEY_ALIAS% -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=SIMAQOM, OU=Mobile, O=SIMAQOM, L=City, ST=State, C=US" -storepass %KEYSTORE_PASS% -keypass %KEYSTORE_PASS%
)

echo Building release APK with Gradle...
call gradlew.bat assembleRelease

echo Release APK built in app\build\outputs\apk\release\
pause
