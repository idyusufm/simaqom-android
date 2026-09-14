# SIMAQOM — Android WebView Wrapper

Production-ready modern Android Studio project (Kotlin + Jetpack + Material 3) wrapping **https://idyusufm.github.io/simaqom**.

## Features Included
- **Target URL**: `https://idyusufm.github.io/simaqom`
- **Application ID / Package**: `com.idyusufm.simaqom`
- **Modern Kotlin**: `OnBackPressedDispatcher`, `ActivityResultContracts` for file chooser.
- **Pull-to-Refresh**: Enabled with SwipeRefreshLayout
- **Progress Bar**: Top loading bar synced with page progress
- **Offline Screen**: Automatic detection & retry button
- **File & Camera Uploads**: Supported via WebChromeClient file chooser
- **Hardware Back Button**: Navigates WebView history before exiting
- **Native Bridge**: Exposed as window.AndroidBridge

## How to Run in Android Studio
1. Unzip this package.
2. Open **Android Studio** (Koala / Iguana or later recommended).
3. Select **File > Open** and choose this project folder.
4. Allow Gradle to sync dependencies automatically.
5. Click the green **Run (Play)** button to launch on an Android Emulator or connected USB device.

## Generating a Release APK or AAB (Google Play)
1. In Android Studio, go to **Build > Generate Signed Bundle / APK**.
2. Select **Android App Bundle (.aab)** for Google Play, or **APK** for direct sideloading.
3. Choose or create your keystore key and click **Finish**.

## Web Developer JS Bridge Example
Add this to your website frontend to trigger native Android actions:

```javascript
if (window.AndroidBridge) {
  // Show native Android Toast
  window.AndroidBridge.showToast("Hello from your website!");

  // Trigger device haptic vibration
  window.AndroidBridge.vibrate(100);

  // Native share sheet
  window.AndroidBridge.shareUrl("Check this out!", window.location.href);

  // App version
  console.log("App Version:", window.AndroidBridge.getAppVersion());
}
```
