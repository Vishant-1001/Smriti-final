# SMRITI (ஸ்மிருதி)

SMRITI is an offline, Tamil-first everyday-life game for adults, designed to be gentle for people living with dementia. The player taps illustrated places in a South Indian home: the veranda, a bedroom with a personal Memory Book and a radio, and later a kitchen, a sewing corner and a market trip. Help is always one tap away. There are no timers, scores, failure states or hidden tests.

> **Not a medical device.** SMRITI does not diagnose, assess, treat or slow dementia, and makes no claim of clinical benefit. It has not been tested with people living with dementia. See [docs/SAFETY_RISK_REGISTER.md](docs/SAFETY_RISK_REGISTER.md).

## Status: milestone M1 (vertical slice)

| Area | State |
|---|---|
| Title → house exterior → veranda hub → bedroom | Working, tested on JVM and on an Android 15 emulator |
| Memory Book (sample pages + caregiver-added pages with photos) | Working |
| Radio (local playlist, no autoplay, Stop/Pause/Next/"Not now", volume) | Working; real playback verified on an emulator |
| Guide: contextual help, subtitles, Replay / Stop voice, "Places here" list | Working; English voice verified offline on an emulator; **no Tamil voice was available on the test emulator** (subtitles shown instead) |
| Settings: Tamil/English, text size, separate voice/music volume, reduced motion, names on demand | Working |
| Caregiver tools (Memory Book pages, radio songs) | Working on JVM tests; photo/audio pickers not exercised on a device |
| Kitchen, sewing corner, market, bus | **Not built yet.** The veranda shows them, and tapping one gives a neutral "not ready yet" message |

The art is original, hand-authored vector illustration that is *approved for prototype*, not final production art. See [docs/ART_BIBLE.md](docs/ART_BIBLE.md) and [docs/ASSET_MANIFEST.csv](docs/ASSET_MANIFEST.csv).

## Quick start

Requires JDK 17 and the Android SDK (platform 35). Full details are in [docs/BUILD_AND_RUN.md](docs/BUILD_AND_RUN.md).

```sh
./gradlew testDebugUnitTest          # JVM unit + Robolectric UI tests
./gradlew assembleDebug              # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug               # onto a connected device/emulator
./gradlew connectedDebugAndroidTest  # instrumented tests on a device/emulator
./gradlew recordRoborazziDebug       # re-record JVM screenshots (then art_source/tools/export_screenshots.py)
```

## Repository map

| Path | What it is |
|---|---|
| `app/` | The Android app (Kotlin, Jetpack Compose) |
| `app/src/main/assets/` | Runtime art (scenes, Guide, radio, Memory Book samples), scene hotspot layouts, demo audio |
| `art_source/generator/` | Python source that draws every runtime illustration as SVG, plus the demo-audio synthesiser |
| `art_source/tools/` | SVG → PNG renderer (resvg) |
| `art_source/reference_only/` | The supplied concept atlases. **Reference only, never shipped** |
| `art_source/third_party/fonts/` | Noto Sans / Noto Sans Tamil (SIL OFL 1.1) |
| `docs/` | Build, architecture, art bible, asset manifest, privacy, safety risks, test report, screenshots |
| `docs/spec/` | The build kit's master prompt and tech-stack decision, kept for reference |

## Privacy in one paragraph

No account and no internet permission. Nothing leaves the device. Photos and songs that a caregiver chooses are copied into the app's private storage, and the app has no analytics, ads or activity log. Backup and device transfer are switched off for app data. See [docs/PRIVACY_AND_DATA.md](docs/PRIVACY_AND_DATA.md).
