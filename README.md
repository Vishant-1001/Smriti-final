# SMRITI (ஸ்மிருதி)

SMRITI is an offline, Tamil-first everyday-life game for adults, designed to be gentle for people living with dementia. The player taps illustrated places in a South Indian home and on a trip to the market:
- the veranda;
- a bedroom with a personal Memory Book and a radio;
- a kitchen for making filter coffee and washing up;
- a sewing corner;
- a bus ride with a conductor;
- four market stalls.

Help is always one tap away. There are no timers, scores, failure states or hidden tests.

> **Not a medical device.** SMRITI does not diagnose, assess, treat or slow dementia, and makes no claim of clinical benefit. It has not been tested with people living with dementia, and it has not run on a physical phone yet. See [docs/SAFETY_RISK_REGISTER.md](docs/SAFETY_RISK_REGISTER.md).

## Status: breadth-first playable prototype (all planned areas)

Every area below can be reached and completed through the real UI. Each one was tested on the JVM (unit tests, Robolectric UI tests and screenshots) and on an Android 15 **emulator**. Nothing has been tested on a physical device.

| Area | What works |
|---|---|
| Exterior → veranda hub | Hotspots, Guide, Back/Home/Settings, "End for now" |
| Bedroom: Memory Book | Sample pages (clearly marked); caregiver-added pages with photos; edit/delete |
| Bedroom: radio | Local playlist; no autoplay; Stop/Pause/Next/"Not now"; separate volume; caregiver import |
| Kitchen: filter coffee | Four tap steps; Repeat, Show me, Do it for me, Undo, Start again; no timers or failure |
| Kitchen: wash dishes | Tap to wash, tap to put on the rack; free or guided mode; Do it for me |
| Sewing corner | Choose cloth (starter cloth always there, market cloth carried home); bag, pillow cover or shirt; three Stitch taps; Keep; creations on the rack and in a gallery |
| Front door → market trip | Picture shopping list; bus or go straight there |
| Bus stop | Number cue shown and spoken on request; any bus can be boarded (no wrong answer) |
| On the bus | Conductor speaks only when tapped; ticket and fare stay visible; pretend coins/notes with change; Show me / Pay for me from the start; Skip the ride |
| Market + four stalls | Fruit & vegetables, spices & pantry, flowers, cloth. Vendors speak only when tapped. Item cards for browsing, bag with put back, five hints at any time |
| Return home | Bus (no number task) or straight home; optional unpacking; bought cloth appears in the sewing corner |
| Settings | Tamil/English, text size, separate voice and music volume, reduced motion, names on demand |

**Honest limitations:**
- Spoken Tamil works only on devices with an offline Tamil TTS voice; the test emulator had none, so it showed subtitles.
- The Tamil text is an unreviewed draft.
- The art is original hand-authored vector illustration, approved for prototype only.
- No payment happens at the stalls; only the bus has a pretend payment.
- There is no caregiver activity log (deliberately).

## Experimental: SCOPE preference-learning research engine

`scope/` holds an **experimental**, offline, pure-Kotlin preference-learning engine and its synthetic benchmark (see [docs/scope/README.md](docs/scope/README.md)). It is **disabled by default**, and the app does not depend on it, so gameplay is unchanged. Under the pre-registered protocol it did **not** demonstrate an advantage over the rule baseline, and it is not validated or clinically beneficial.

## Quick start

Requires JDK 17 and the Android SDK (platform 35). Full details are in [docs/BUILD_AND_RUN.md](docs/BUILD_AND_RUN.md).

```sh
./gradlew testDebugUnitTest          # JVM unit + Robolectric UI + screenshot tests
./gradlew assembleDebug              # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug               # onto a connected device/emulator
./gradlew connectedDebugAndroidTest  # instrumented tests on a device/emulator
./gradlew recordRoborazziDebug       # re-record JVM screenshots (then art_source/tools/export_screenshots.py)
```

## Repository map

| Path | What it is |
|---|---|
| `app/` | The Android app (Kotlin, Jetpack Compose) |
| `app/src/main/assets/` | Runtime art (12 scenes × 2 orientations, characters, items, kitchen and sewing sprites), hotspot layouts, demo audio |
| `art_source/generator/` | Python source that draws every runtime illustration as SVG, plus the demo-audio synthesiser |
| `art_source/reference_only/` | The supplied concept atlases. **Reference only, never shipped** |
| `docs/` | Build, architecture, art bible, asset manifest, privacy, safety risks, test report, screenshots |

## Privacy in one paragraph

There is no account and no internet permission. Nothing leaves the device. Photos and songs a caregiver chooses are copied into the app's private storage. There are no analytics, ads or activity log. Backup and device transfer are switched off for app data. See [docs/PRIVACY_AND_DATA.md](docs/PRIVACY_AND_DATA.md).
