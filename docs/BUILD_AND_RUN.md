# Build and run

Repository: https://github.com/Vishant-1001/Smriti-final.git

## Verified toolchain (2026-10-23, Windows 11)

| Tool | Version | Notes |
|---|---|---|
| JDK | Eclipse Temurin 17.0.20.1 | Any JDK 17 should work; JDK 21 not tested |
| Gradle (wrapper) | 8.11.1 | Distribution SHA-256 pinned in `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | 8.7.3 | |
| Kotlin | 2.1.0 | With the Compose compiler plugin and kotlinx.serialization plugin |
| Compose BOM | 2024.12.01 | |
| compileSdk / targetSdk | 35 (Android 15) | |
| minSdk | 26 (Android 8.0) | |
| Android build-tools | 35.0.0 | |
| Robolectric / Roborazzi | 4.14.1 / 1.32.2 | JVM UI tests and screenshots |
| Emulator | 37.2.12.0, system image `android-35;google_apis;x86_64` | Accelerated with WHPX |

The versions are pinned deliberately. Android Lint reports that newer AGP and Compose releases exist. Upgrade them as a separate, tested change, not blindly.

## One-time setup

1. Install JDK 17 and set `JAVA_HOME`.
2. Install the Android SDK (Android Studio, or the command-line tools) with `platforms;android-35`, `build-tools;35.0.0` and `platform-tools`.
3. Create `local.properties` in the repo root (it is git-ignored):
   ```
   sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk
   ```
   or set `ANDROID_HOME`.

## Commands

| Purpose | Command | Needs a device? |
|---|---|---|
| Unit tests + Robolectric Compose UI tests | `./gradlew testDebugUnitTest` | No |
| Re-record JVM screenshots | `./gradlew recordRoborazziDebug` then `python -I art_source/tools/export_screenshots.py` (raw PNGs are git-ignored; WebP copies go to `docs/screenshots/jvm/`) | No |
| Lint | `./gradlew lintDebug` | No |
| Debug APK | `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` | No |
| Install | `./gradlew installDebug` | Yes |
| Instrumented tests | `./gradlew connectedDebugAndroidTest` | Yes |

On Windows Git Bash use `./gradlew`; in PowerShell or cmd use `gradlew.bat`.

## Emulator used for testing

```sh
sdkmanager "emulator" "system-images;android-35;google_apis;x86_64"
avdmanager create avd -n smriti_api35 -k "system-images;android-35;google_apis;x86_64" -d pixel_7
emulator -avd smriti_api35 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect
```

`-no-audio` means sound is not heard. Playback was verified through the platform's audio playback state instead (see TEST_REPORT.md). Tablet-size checks used `adb shell wm size 1600x2560` and `adb shell wm density 320` on the same emulator. That is a display override, **not** a real tablet.

## Rebuilding the art (optional)

The runtime images in `app/src/main/assets/` are committed, so the art tools are only needed to change the art.

```sh
cd art_source/tools && npm ci && cd ../..        # Node 18+; installs @resvg/resvg-js 2.6.2
python -I art_source/generator/build.py           # Python 3.10+ with Pillow; regenerates scenes, sprites and layouts
python -I art_source/generator/demo_audio.py      # needs ffmpeg with libvorbis; regenerates demo radio tunes
```

After changing scene art, run `./gradlew testDebugUnitTest`. `SceneAssetsTest` checks that hotspots match the code catalog, stay inside the protected area, and do not overlap.

## APK

`./gradlew assembleDebug` produces `app/build/outputs/apk/debug/app-debug.apk` (about 13.5 MB). It is a debug-signed build, suitable for sideloading onto a test device with `adb install`, not for distribution.

## Release builds

There is no signing configuration and no release has been made. `isMinifyEnabled = false`. App bundles do not split by language, because Tamil and English are switched inside the app.
