# Test report: milestone M1

Date: 2026-10-23. Machine: Windows 11 Pro, JDK Temurin 17.0.20.1, Gradle 8.11.1, AGP 8.7.3.

Final run (clean build):

```
./gradlew clean :app:testDebugUnitTest :app:recordRoborazziDebug :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest
BUILD SUCCESSFUL in 1m 51s
```

## 1. JVM unit tests (pure Kotlin): 46 passed, 0 failed

| Suite | Tests | Covers |
|---|---|---|
| `SceneNavigatorTest` | 6 | exterior → veranda → bedroom → back; stack never empties; Home rules; end session; repairing saved stacks |
| `SceneFrameTest` | 9 | no stretching; cover vs. letterbox; safe area always visible across 35 window sizes × 2 orientations; hotspots kept below the HUD inset |
| `RadioSessionTest` | 13 | no autoplay; Stop from every state; pause/resume; Next; "Not now" never starts another song; play-through then rest; failure handling; interruption; empty list |
| `MemoryBookStateTest` | 7 | samples clearly marked; page limits; add/edit/delete; samples removable and restorable; index repair |
| `SaveCodecTest` | 7 | round trip; corrupt and empty files; unknown fields and enums; v0 → v1 migration; save contains no score/time/error fields |
| `SceneAssetsTest` | 4 | both orientations exist for every scene; hotspot IDs match the catalog; inside image and safe area; no overlaps; no atlases in runtime assets |
| `StringsParityTest` | 2 | Tamil and English have identical keys and placeholders; Tamil strings use Tamil script |

## 2. ViewModel tests (JVM, fakes for voice and audio): 19 passed

`GameViewModelTest`: persistence of navigation; restore after restart; radio never autoplays; restored radio is stopped; radio controls drive the player; independent music and voice volume; missing-file handling; background pause; help only on request; leaving a scene stops speech; replay; unavailable voice still shows the subtitle; narration off respected; rooms not built yet do not navigate; front-door chooser; back closes overlays first; end session; Memory Book persistence and photo deletion; removing the playing song.

## 3. Compose UI tests on Robolectric (JVM, *not a device*): 10 passed

`GameFlowUiTest` (411×914 dp): full slice navigation; Back, Settings and Guide on every gameplay screen; "Places here" list as an alternative to the picture; radio no-autoplay, mini player and Stop; Memory Book sample badge and page turn; front door Stay home / Go outside; Help subtitle and Stop voice; kitchen "not ready" message; caregiver tools behind a deliberate step; End for now from the Guide.

This suite caught a real bug: the language-switch context hid the Activity, which would have crashed the photo and song pickers. It was fixed with `LocalizedActivityContext`.

## 4. JVM screenshots (Roborazzi + Robolectric native graphics): 44 tests, 52 images

Four device configurations × 11 tests:

| Device configuration | Folder |
|---|---|
| Narrow phone 320×640 dp | `docs/screenshots/jvm/narrow_phone_320x640dp/` |
| Phone portrait 411×914 dp | `docs/screenshots/jvm/phone_portrait_411x914dp/` |
| Phone landscape 914×411 dp | `docs/screenshots/jvm/phone_landscape_914x411dp/` |
| Tablet landscape 1280×800 dp | `docs/screenshots/jvm/tablet_landscape_1280x800dp/` |

Scenes captured: title, exterior, veranda, bedroom, Memory Book, radio (Tamil); veranda with names, Guide panel, speaking subtitle, destination chooser, radio playing, mini player (English); settings at the Larger text size. These are JVM renders, not device captures.

Problems these screenshots exposed, and fixed: HUD overflow on narrow phones (two-row bar); text-size choices breaking letter by letter (now stacked); the OK button wrapping; radio buttons breaking mid-word (one per line under 380 dp); the mini player covering the Guide (moved above it); hotspot names clipped (now allowed past the outline).

## 5. Instrumented tests on an emulator: 3 passed

Device: Android 15 emulator (`sdk_gphone64_x86_64`, API 35, Pixel 7 profile, WHPX, `-no-audio`). Production wiring: real DataStore, MediaPlayer and TextToSpeech.

| Test | Result |
|---|---|
| `fullSliceWithRealAudio`: whole slice; no music before selection; selecting a song creates an active `USAGE_MEDIA`/`CONTENT_TYPE_MUSIC` player; Stop releases it; Memory Book page turn; Home | Passed |
| `sceneSurvivesRotationAndRecreation`: bedroom kept through landscape rotation and activity recreation | Passed |
| `guideHelpAlwaysShowsSubtitle` | Passed |

The emulator ran with `-no-audio`, so sound was not *heard*. Playback was checked through Android's active playback configurations and, manually, through `dumpsys audio` (`MediaPlayer … state:started … usage=USAGE_MEDIA content=CONTENT_TYPE_MUSIC … sampleRate=22050`).

## 6. Manual checks on the emulator

Screenshots are in `docs/screenshots/emulator/`. "p" files are portrait 1080×2400, "l" files landscape, and "t" files use a tablet-size display override (1600×2560 @ 320 dpi) on the same phone emulator.

- Title, exterior, veranda and bedroom in Tamil; every hotspot tapped and navigating correctly.
- Force-stop and relaunch returned to the radio screen with the radio stopped (process-death restore).
- Pressing HOME while music played changed the player to `state:paused` (`dumpsys audio`).
- **TTS:** the emulator's Google TTS engine (`googletts.google-speech-apk_20240319.00_p0.617183227`) has **no installed Tamil voice**, and SMRITI correctly showed "voice not available" in Tamil. The English line was synthesised by `en-us-x-iog-lstm-embedded`, an on-device voice, per logcat.
- The language switch, Settings, destination chooser, "not ready" message and mini player all behaved as intended.

## 7. Lint

`./gradlew lintDebug`: 0 errors, 33 warnings, all "a newer version is available" (Compose BOM, AGP, other AndroidX). These versions are pinned deliberately; see BUILD_AND_RUN.md.

## Not tested

- **Physical devices: none.** All device testing was on an emulator.
- TalkBack, Switch Access, and keyboard focus order on a device (semantics and traversal order exist but were not exercised).
- Real audio output to speakers or headphones; audio-focus loss from a phone call.
- The photo and song pickers in Caregiver tools on a device (the import code paths have no automated test).
- Reduced motion on a device; very large system font scales (above 1.3); foldables and freeform windows.
- Tamil text-to-speech on a device that has an offline Tamil voice.
- Any testing with people living with dementia or caregivers (and none should happen without ethics approval; see SAFETY_RISK_REGISTER.md).
