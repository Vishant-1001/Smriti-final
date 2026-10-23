# Architecture

SMRITI is one Android app module: Kotlin, Jetpack Compose, a single `MainActivity`, no game engine.

```
app/src/main/java/app/smriti/
├── MainActivity.kt         Hosts Compose; pauses audio when the app leaves the screen
├── SmritiApp.kt            Application + AppGraph (hand-wired dependencies; tests swap it)
├── domain/                 Pure Kotlin, JVM-tested
│   ├── SceneId.kt          Stable scene IDs (persisted; never rename)
│   ├── Navigation.kt       NavState back stack + SceneNavigator (goTo/back/home/endSession/sanitize)
│   ├── SceneFrame.kt       Places art without stretching; maps hotspots; respects HUD insets
│   ├── Radio.kt            RadioTrack + RadioSession state machine
│   ├── MemoryBook.kt       MemoryPage / MemoryBookState
│   ├── Settings.kt         Language, text size, separate voice/music volume, reduced motion
│   ├── Dialogue.kt         Authored Guide lines (string resources)
│   ├── Catalog.kt          Stable shop item IDs, stalls, starter fabric
│   ├── Shopping.kt         List, bag, put back/unpack, hint levels, regions
│   ├── Kitchen.kt          CookingState (filter coffee) and DishwashState
│   ├── Sewing.kt           SewingState and Creation
│   ├── Trip.kt             TripState: bus cue options, ticket, pretend payment with change
│   └── SaveData.kt         Versioned save schema v2 (no scores, timings or error counts)
├── data/
│   ├── SaveCodec.kt        JSON encode/decode, migrations, sanitising
│   ├── SaveRepository.kt   DataStore-backed save (+ in-memory version for tests)
│   ├── SceneLayouts.kt     Loads generated hotspot layouts from assets
│   └── MediaStorage.kt     Copies caregiver-picked photos/songs into private storage
├── audio/
│   ├── VoiceOutput.kt      On-device TextToSpeech; network/uninstalled voices treated as unavailable
│   └── RadioPlayer.kt      MediaPlayer playback with audio focus; failures reported, never thrown
├── interactions/
│   ├── GameAction.kt       Every player intent
│   └── HotspotCatalog.kt   Hotspot ID → label + action + focus order
└── ui/
    ├── GameViewModel.kt    Single dispatch(GameAction); owns UI state; persists through SaveRepository
    ├── SmritiRoot.kt       Language context, theme, scene routing, HUD, overlays, mini player
    ├── scene/SceneView.kt  Illustrated scene + hotspots + Guide corner choice
    ├── overlays/           Guide panel, subtitle panel, destination chooser, settings
    ├── screens/            Title, Memory Book, Radio, Caregiver tools, KitchenScreens (cooking, dishes),
    │                       StitchScreen (+ creations gallery), TravelScreens (trip, bus cue, bus panel,
    │                       shopping panel with hints, item card)
    ├── components/         Buttons, panels, Guide portrait animation, icons, image loading
    └── theme/              Palette tokens, Tamil/Latin fonts, text-size multiplier
```

## Key decisions

**One action type, one dispatcher.** Scene hotspots, the Guide's "Places here" list, HUD buttons and tests all send the same `GameAction` to `GameViewModel.dispatch`. The picture and its accessible list alternative therefore cannot drift apart.

**Explicit navigation state, no Navigation Compose.** The whole position is a small back stack (`NavState`). It is serialisable, so restoring after process death is just loading it. `SceneNavigator.sanitize` repairs any invalid stack from disk.

**Hotspots come from the art pipeline.** `art_source/generator` draws each scene and writes `scenes/<scene>_<land|port>.json` from the same coordinates it used to draw the objects. The JSON gives each hotspot a normalised rectangle and a "safe area" that must stay visible. At runtime `SceneFrame.compute` picks a uniform scale and offset that:
1. covers the window when that keeps the safe area visible below the HUD;
2. otherwise scales down just enough, letterboxing with a dimmed copy of the art;
3. never stretches the image.

Hotspots are mapped through the same frame and enlarged to at least 64 dp. The Guide button moves from bottom-left to bottom-right when it would cover a hotspot.

**Portrait and landscape art.** Each scene has a 1920×1080 landscape and a 1080×2400 (20:9) portrait composition, with the layout chosen by window shape.

**State.**
- *Persistent:* `SaveData` holds the nav stack, settings, Memory Book, playlist and the selected track. It is stored as JSON in DataStore (`files/datastore/smriti_save.json`) and written through on every change.
- *Transient:* overlays, the current subtitle, the "show names" toggle, the radio session (playing/paused, songs skipped this session).
- After a restart the radio is always stopped. When the app goes to the background, music pauses and speech stops.

**Speech.** Voice is optional and on-device only. A new line replaces the old one (`QUEUE_FLUSH`), and leaving a scene stops speech. Subtitles are always shown, with a note when no voice is available.

**Language.** Tamil is the default; English is the fallback in `values/`. The chosen language is applied by wrapping the activity context and swapping only its resources (`LocalizedActivityContext`). Activity lookups such as the picker launchers keep working. Fonts are bundled Noto Sans Tamil and Noto Sans.

**Text size.** The in-app size (1.0×, 1.2× or 1.4×) multiplies the system font scale. HUD labels use only the system scale, so the always-visible controls fit side by side on 320 dp phones.

**Persistence schema.** `SaveData.schemaVersion = 1`. `SaveCodec.decode` tolerates missing fields, unknown fields, unknown enum values and corrupt files. It falls back to defaults and never crashes.

## Game path and state flow

```
Title → Exterior → Veranda ─┬─ Bedroom ─ Memory Book / Radio
                            ├─ Kitchen ─ Cooking / Dishes
                            ├─ Sewing ─ Stitch (fabrics = starter + cloth bought)
                            └─ Front door → chooser → Trip(TO_MARKET) ─┬─ bus: Bus stop → Bus (ticket, pay) ─┐
                                                                      └─ go straight ───────────────────────┴→ Market → 4 stalls
Market → "Go home" → Trip(TO_HOME) ─┬─ bus: Bus (no number task) ─┐
                                    └─ go straight ───────────────┴→ Veranda
```

- `SceneNavigator.arriveAt` resets the stack on arrival, so Back at the market goes home and Back at home stays home.
- `TripState` exists only during travel and is cleared on arrival.
- `ShoppingState.bag`/`putAway` persist. Their cloth items feed `ShoppingState.fabrics` for sewing.
- Every activity state is written through to `SaveData`, so leaving and coming back (or a process restart) resumes the activity.
- Transient helpers (hint highlight, selected item card) live in `Helpers` and are never saved.

## Not in this version

There is no activity/event log: the safest log is the one that does not exist. The optional caregiver report described in the brief was deliberately not built. Also absent: Room, Media3, Navigation Compose, network code, analytics, speech recognition, and LLM dialogue.
