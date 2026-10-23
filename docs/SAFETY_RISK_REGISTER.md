# Safety risk register

This is a development risk register, **not** a clinical safety case. Developer testing on emulators and the JVM is not clinical validation. SMRITI must not be tested with people living with dementia until there is:
- appropriate expert review;
- a consent/assent process with clear dissent and stop procedures;
- institutional ethics approval.

**Owner** for every item until a team exists: the project owner (repository maintainer).

| # | Risk | Mitigation | Evidence | Status | Still unverified |
|---|---|---|---|---|---|
| 1 | Visual clutter / poor contrast | Few large objects per scene; one outline colour; outlined hotspots; UI text ≥ 4.5:1 on paper; numbers on bus boards are high-contrast yellow on black | Screenshots at 4 sizes; `SceneAssetsTest` (no overlaps) | Mitigated (prototype) | Expert review; low vision; glare |
| 2 | Confusing navigation / unclear way back | Back/Home/Settings first in the HUD, never pushed off-screen; place name shown; Back closes overlays first; trip screens dropped on arrival so Back at the market leads home; "Places here" list | UI tests; `IntegrationViewModelTest` stack check; narrow-screen screenshots | Mitigated | Comprehension by intended users |
| 3 | Long or unsolicited dialogue | Guide, conductor and vendors speak only when tapped; one or two short lines; new speech replaces old; leaving a scene stops speech | `IntegrationViewModelTest.vendor speaks only when tapped`, round-trip test (`conductor does not speak first`) | Mitigated | Tamil wording not reviewed |
| 4 | Distressing or unwanted music | No autoplay; Stop always visible; "Not now"; pause in the background; radio stops when leaving for the market | Radio tests; instrumented playback test | Mitigated | Caregiver song choice |
| 5 | Motion discomfort | Only cross-fades, the Guide's mouth frames and a gentle sewing-machine bob; Reduced motion removes them; bus scenery is static | Code review; JVM screenshots | Partly verified | No device test with Reduced motion |
| 6 | Inconsistent item/inventory state | Explicit state machines; browsing never ticks list items; items added once; unpacking keeps items; cloth carried to sewing; progress saved and restored | `ShoppingStateTest`, `CookingStateTest`, `DishwashStateTest`, `SewingStateTest`, integration tests | Mitigated | Long sessions over days |
| 7 | Payment interaction confusion | Ticket and fare always visible; "₹X more" shown neutrally; overpaying gives change; Show me highlights ₹10; Pay for me and Skip the ride from the start; labelled pretend money; no payment SDK, no network | `TripStateTest`; instrumented round trip | Mitigated (prototype) | Whether coins/notes are understandable without real-money cues |
| 8 | Hidden assessment / performance monitoring | No logging, timing or scoring; bus choice kept only for the ticket of that ride; save has no score/time/error fields | `SaveCodecTest.save never contains performance data`; `TripState` design | Mitigated | Re-check if logging is ever proposed |
| 9 | Cultural assumptions | Generic, people-free Memory Book samples; neutral sewing choices (bag, pillow cover, shirt); familiar items (filter coffee, jasmine, turmeric); vendors drawn respectfully | ART_BIBLE.md | **Open** | No Tamil cultural/community review; Tamil text is an unreviewed draft |
| 10 | Privacy loss from photos/audio | No internet permission; private storage; backup off; deletes remove files | PRIVACY_AND_DATA.md; merged manifest | Mitigated | Shared-device threats |
| 11 | State loss on rotation/backgrounding | Write-through save of navigation, activities, shopping and trip | Instrumented rotation/recreation test; restart tests in `IntegrationViewModelTest`; emulator walk-through showed sewing progress restored | Mitigated | Low-memory process kill on a device |
| 12 | Help/Stop/Exit inaccessible | Guide on every screen (moves to a free corner); Stop in the mini player; "End for now"; Settings always on HUD row 1; cue and panels placed so they never cover the Guide or hotspots | Screenshots at 320/411/914/1280 dp; UI tests | Mitigated | TalkBack/Switch Access on a device |
| 13 | Tamil voice unavailable offline | Subtitles always shown, with an honest "voice not available" note | Emulator had no Tamil voice; English embedded voice worked | **Open** | Target devices; recorded Tamil narration not produced |
| 14 | Prototype art mistaken for final | Manifest marks everything `approved_for_prototype` | ASSET_MANIFEST.csv | Tracked | Professional illustration pass |
| 15 | Accidental caregiver changes | Caregiver tools behind Settings and an explanatory step; delete confirmations | UI test | Mitigated | Caregiver feedback |
| 16 | Bus-number cue felt as a test | Cue always visible, never hidden after a delay; spoken on request; any bus boards with neutral wording; no number task on the way home | `TripStateTest`; round trip boards bus 63 | Mitigated | User perception |
| 17 | Stall interactions feel like a quiz | Item cards explain neutrally ("not on your list, you can still take it"); no stall payment; leaving with nothing is fine | Integration test `leaving the market without buying everything` | Mitigated | User perception |
