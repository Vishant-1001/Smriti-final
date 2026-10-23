# Test report: breadth-first prototype (all planned areas)

Date: 2026-10-23. Machine: Windows 11 Pro, JDK Temurin 17.0.20.1, Gradle 8.11.1, AGP 8.7.3.
Previous report (milestone M1) is superseded; its tests are still part of the suites below.

Final run (clean build):

```
./gradlew clean :app:testDebugUnitTest :app:recordRoborazziDebug :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest
```

| What | Result |
|---|---|
| JVM tests | **212 passed, 0 failed** |
| Lint | 0 errors (see section 6) |
| APK | `app/build/outputs/apk/debug/app-debug.apk`, 13.5 MB |
| Instrumented tests | Passed after one fix (see section 4) |

In that clean run one instrumented test (`marketRoundTripByBusThenSewWithBoughtCloth`) failed. Its expectations were wrong, not the app: sewing progress saved by an earlier manual session was correctly restored, so the screen opened mid-piece. The test now presses "Start again" first. A re-run of `connectedDebugAndroidTest` passed **4/4**.

## 1. JVM unit tests (pure Kotlin): 70

| Suite | Tests | Covers |
|---|---|---|
| `SceneNavigatorTest` | 6 | navigation stack, Home rules, end session, repair of saved stacks |
| `SceneFrameTest` | 9 | no stretching, cover vs. letterbox, safe area visible below the HUD at many sizes |
| `RadioSessionTest` | 13 | no autoplay, Stop always works, "Not now", failures, interruptions |
| `MemoryBookStateTest` | 7 | samples marked, add/edit/delete, index repair |
| `SaveCodecTest` | 7 | round trip, corrupt files, migration, no score/time/error fields |
| `ShoppingStateTest` | 8 | requested items tick off; browsing never ticks or removes list items; any cloth satisfies the cloth entry; starter fabric always available; unpack; next wanted item per stall; regions |
| `CookingStateTest` | 4 | right order makes coffee; another tool is a gentle pointer with no state change; do-it/undo/reset; never shows done early |
| `DishwashStateTest` | 2 | wash then rack in any order; guided suggestion; one-tap completion |
| `SewingStateTest` | 3 | choose, stitch, keep; cannot keep unfinished; undo/start over; "do it for me" from the start |
| `TripStateTest` | 5 | any bus boards; distinct numbers; exact or over-payment with change, never trapped; Pay for me from the start; no number task going home |
| `SceneAssetsTest` | 4 | all 12 scenes in both orientations; hotspot IDs match the code; inside the image and safe area; no overlaps; no atlases in runtime assets |
| `StringsParityTest` | 2 | Tamil and English have the same keys and placeholders; Tamil strings are in Tamil script |

## 2. ViewModel tests with fakes: 28

`GameViewModelTest` (19) covers the M1 behaviours.

`IntegrationViewModelTest` (9) covers state flowing between features (Pass B):
- full round trip: veranda → front door → trip → bus stop → board a *non-suggested* bus → conductor only when tapped → pay ₹20, get ₹10 change → market (Back then leads home) → cloth stall → buy cloth → bus home with no number task → veranda → sewing with the bought cloth → creation kept;
- going straight there and back, and Skip the ride;
- leaving the market without buying everything;
- all hint levels at any time;
- spoken bus-number cue;
- cooking and dishes progress kept across a restart;
- the trip kept across a restart, and the radio stopped when leaving for the market;
- a vendor speaks only when tapped, and the speech is cleared on leaving.

## 3. Robolectric Compose UI tests and screenshots (JVM, *not a device*): 114

**`GameFlowUiTest` (10):** tests the M1 flows plus `kitchenCoffeeAndDishesThroughTheRealUi` (coffee to completion and all dishes racked through real taps).

**`*Screenshots` (26 per size × 4 sizes = 104):**
- **Sizes:** 320×640, 411×914, 914×411 and 1280×800 dp.
- **Images:** 26 per size, 104 in total, in `docs/screenshots/jvm/`.
- **New scenes covered:** kitchen, cooking at step 3, dishes in guided mode, sewing with creations on the rack, stitching, trip, bus stop, bus ticket during payment, market, four stalls, the shopping panel with hints, and the item card.

**Layout defects these screenshots exposed, now fixed:**
- Settings was pushed off-screen on narrow market screens; the two-row HUD now keeps Back, Home and Settings first.
- The bus cue covered bus numbers; space is now reserved above the buses.
- The Guide covered the bottom-row stall items; the portrait art was re-composed.
- The Go home button covered a stall; it moved to the HUD.
- The bus panel covered the conductor before the ticket; it is now narrow until then.
- The rack items were too small.
- Stitch was below the fold in landscape.

## 4. Instrumented tests on an emulator: 4/4 passed

Device: Android 15 emulator (`sdk_gphone64_x86_64`, API 35, Pixel 7 profile, `-no-audio`), with production wiring (DataStore, MediaPlayer, TextToSpeech).

| Test | Result |
|---|---|
| `fullSliceWithRealAudio`: no music before selection; a real media player starts on selection and is released by Stop | Passed |
| `sceneSurvivesRotationAndRecreation` | Passed |
| `guideHelpAlwaysShowsSubtitle` | Passed |
| `marketRoundTripByBusThenSewWithBoughtCloth`: front door → Market → bus → tap bus 63 (not the cue) → talk → pay ₹20 → change shown → market → cloth stall → bag → Go home → straight home → sewing with the bought cloth → keep | Passed (after the test fix above) |

This run also caught a real bug: the ticket panel hid while the conductor's "thank you" showed, hiding the fare and change. Fixed by moving bus dialogue to the top.

## 5. Manual emulator walk-through

Screenshots are in `docs/screenshots/emulator/`. The `q*` files are portrait Pass A captures and the `r*` files landscape ones; the M1 captures are `p*`, `l*` and `t*`.

**Seen working in Tamil:**
- kitchen, coffee steps and the dishes;
- sewing and stitching;
- the destination chooser and the trip;
- the bus stop, with the spoken-cue request showing "voice not available";
- the conductor, payment and the market;
- a vendor greeting, an item card, hints, and the "Point to it" ring;
- the cloth stall in landscape, and the bus home with a pretend payment;
- sewing progress restored after leaving.

**Fixed after the walk-through:**
- Trip choices were below the fold in landscape; they now come first.
- "Get off" was below the fold; there is now a two-column bus panel, and "Get off" comes first once paid.
- Narrow bus-panel labels broke mid-word; the buttons are now stacked.
- Pay for me is now offered from the start of the ride.

## 6. Lint

0 errors. 33 warnings say newer AGP/AndroidX versions exist; those versions are pinned deliberately. One `PluralsCandidate` warning concerns "₹%d more" and does not apply to an amount of money.

## Not tested

- **Physical devices: none.**
- TalkBack, Switch Access and keyboard navigation on a device.
- Real speaker output (the emulator had no audio); interruption by phone calls.
- Tamil text-to-speech on a device that has an offline Tamil voice.
- The caregiver photo and song pickers on a device.
- Reduced motion on a device; system font scales above 1.3; foldables.
- Any testing with people living with dementia or caregivers (see SAFETY_RISK_REGISTER.md).

## SCOPE experimental module (added later; separate from the app)

```
./gradlew :scope:test               # 23 passed, 0 failed (ScopeEngineTest 18, SimulatorTest 5)
./gradlew :scope:runScopeBenchmark  # exit 0; docs/scope/SCOPE_BENCHMARK_RESULTS.md
./gradlew :app:testDebugUnitTest    # re-run after adding the module: 212 passed, 0 failed
```

**Benchmark verdict: pre-registered criterion NOT met.** See `docs/scope/README.md`.

