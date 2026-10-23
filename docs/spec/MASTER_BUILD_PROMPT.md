# SMRITI — Master Build-from-Scratch Prompt v1.2 (Kotlin/Android + GitHub-Targeted)

**Purpose:** Build a real, runnable Android game from scratch in the dedicated empty GitHub repository `https://github.com/Vishant-1001/Smriti-final.git`, using preset illustrated scenes and tappable objects. This is the canonical implementation prompt for the current SMRITI direction. It supersedes all earlier open-world, first-person, realistic-3D and cartoon-3D plans. **The target repository is the project home; do not create a nested `smriti_rebuild/` directory and do not copy code from the old prototype.**

**Technology decision:** native Android application written in Kotlin, using Jetpack Compose for screens and UI, Android lifecycle/media APIs, and a simple state-driven preset-scene system. No Godot, GDScript, Unity or Unreal is part of this build.

**Current product direction:** soft, polished 2D-comic illustrations; fixed scene compositions with preset-object interactions; a South Indian home as the hub; a bedroom Memory Book and caregiver-loaded radio; kitchen cooking and dishwashing; a sewing corner; a front-door destination choice; bus-stop selection, a seated bus scene, conductor/ticket/payment; and four market stalls. No Park or Temple destinations in this version.

---

## PROMPT START — give everything below to your coding agent

You are the lead engineer, 2D game designer, UI/accessibility engineer, asset-integration artist, QA owner and release engineer for **SMRITI**, an offline-first, Tamil-first, dementia-sensitive everyday-life game. Build the product from scratch at the repository root of `https://github.com/Vishant-1001/Smriti-final.git`. Deliver working software and keep the verified code in that repository, not merely a plan, mock-up, code snippets or another design document. The user has specified this as a different, empty repository. Re-check its current state before acting; repository contents may change over time.

You must be blunt and evidence-based. The chosen stack is **Kotlin + Jetpack Compose for Android**; the architecture below is authoritative and must not be silently changed to Godot/GDScript. Never claim a feature works unless you implemented and tested it. Never call an atlas a production-ready asset library. Never claim clinical safety or benefit based on code review. Report real commands, results, screenshots, limitations and unverified areas.

## 0. Resolve the current product direction

This is **not** an open-world game. Do not implement a free-roaming camera, first-person controller, GTA-style world, 3D environment, or complex navigation. Do not use the old first-person/open-world prompts as active instructions.

The game uses **preset illustrated 2D scenes with object hotspots and scene transitions**. The visual target is soft 2D-comic illustration: clean expressive linework, gentle shading, warm natural colours, believable South Indian objects and architecture, consistent adult characters, clear silhouettes, calm compositions. Avoid childish babyish art, flat primitive geometry, photorealism, hyper-detailed clutter, and a UI made of oversized text buttons surrounding a tiny image.

The player taps a room, door, character, object or action hotspot. The view changes to the relevant prepared scene or activity. A scene may use layered parallax only if it remains stable and accessible; it is not required for the first release. Interactions must be real, stateful and testable.

## 1. Start from scratch in the exact target repository

**Repository of record:** `https://github.com/Vishant-1001/Smriti-final.git`  
**Expected initial state:** empty repository. Do not use the old prototype repository, do not migrate its code, and do not create a nested `smriti_rebuild/` folder. The native Android Gradle project should live at the root of this repository.

Before implementation:
1. Inspect the working directory, installed toolchain, files, and Git state. Verify that the current working directory is the intended new repository before writing files.
2. Run `git status --short --branch`, `git branch --show-current`, `git remote -v`, and `git log --oneline -5` where applicable. Do not assume the folder is empty merely because the remote was empty earlier.
3. Confirm the target remote URL using `git remote get-url origin` if `origin` exists. The expected URL is `https://github.com/Vishant-1001/Smriti-final.git` (GitHub repository page: `https://github.com/Vishant-1001/Smriti-final`).
4. If this directory is not a Git repository, initialise Git here with the main branch (`git init -b main`) only after verifying you are in the designated project directory. If `origin` is missing, add the target URL as `origin`.
5. If `origin` points to a different repository, **do not push**. First verify the local project directory is the dedicated new checkout for `Smriti-final` and inspect all tracked/untracked files. Because the user explicitly named the target, change `origin` to the exact target only after verifying this is the intended workspace; preserve all local work. Never push to the old prototype repo.
6. Check remote reachability with `git ls-remote --heads https://github.com/Vishant-1001/Smriti-final.git`. If GitHub authentication or network access is unavailable, keep building locally and report the exact blocker. Do not stop after a plan.
7. Use **Kotlin + Jetpack Compose for native Android**. This choice is intentional: the game consists of preset illustrated scenes, touch hotspots, dialogue panels, audio, and small stateful activities—not a free-roaming game world. Use Compose for screen navigation and responsive UI; use Android image/drawable rendering and layered `Box`/`Canvas` only where appropriate for illustrated scenes and hotspots. Do not introduce Godot, GDScript, Unity, Unreal, a game engine, or a separate cross-platform framework unless the owner explicitly approves a documented change in architecture.
8. Record Kotlin, JDK, Gradle wrapper, Android Gradle Plugin, Compose, target Android API/SDK requirements, build method and exact repository URL in `docs/BUILD_AND_RUN.md`.
9. Make a baseline report, then continue coding. Do not stop after writing a plan.
10. If the JDK, Gradle wrapper, Android SDK, emulator or device is unavailable, implement what can be verified locally, document exactly what could not be tested, and give reproducible setup steps. Never fake a successful Android build.

The project must be runnable from a clean clone using documented commands and settings. Pin or document dependency/tool versions. **Never use destructive Git operations, `git push --force`, or commands that discard user changes.**

### 1.1 Required GitHub workflow — code must land in the target repository

The user's explicit destination for all SMRITI source code and project files is `https://github.com/Vishant-1001/Smriti-final.git`. The repository page was checked during prompt preparation and displayed GitHub's “This repository is empty” state; re-check before the first push in case the state has changed.

- Use branch `main` unless the remote already has a documented branch convention; never force-push.
- Make small, meaningful commits after each milestone that builds and whose relevant tests pass. Use concise messages such as `chore: bootstrap Kotlin Android app`, `feat: add veranda and bedroom slice`, and `feat: add local radio playback`.
- Before a commit, review `git status`, `git diff --check`, and the full diff. Scan for passwords, tokens, private keys, `.env` files, machine-specific paths and personal data. Do not commit secrets, credentials, personal media, generated user Memory Book content, build caches, `.gradle/`, Android Studio temporary files or signing keys.
- Include source code, project configuration, appropriate production assets, tests and documentation required to build the product. Keep raw reference atlases separate from runtime assets; do not accidentally make concept boards the game's backgrounds.
- Add and maintain a suitable `.gitignore` for Gradle, Android Studio/IntelliJ, Android build outputs, local SDK paths, signing materials and local-only user data. Do not exclude source files the project needs to build.
- Use the existing authenticated Git credential manager or GitHub CLI if available. Never ask the user to paste a personal access token into chat, source files, command arguments or the repository. Never embed credentials in the remote URL.
- After a milestone passes tests, commit it and attempt `git push -u origin main` for the first push, or `git push origin main` after upstream tracking exists. If branch/upstream state differs, inspect it before choosing a command.
- Before the first push, fetch/check remote state. If another commit has appeared in the remote, reconcile via a normal fetch/rebase or merge without discarding either side. Never overwrite remote work by force.
- If push is rejected, authentication fails, or network access is unavailable, preserve all local commits and report the exact command/error. Do not say code was pushed until the push command succeeds.
- After a successful push, verify the remote branch hash with `git ls-remote origin refs/heads/main` and compare it with the local commit. Report the pushed commit hash and repository link.
- Do not create an unnecessary second remote, mirror or repository. Do not silently change repository visibility or GitHub settings.

### 1.2 Repository bootstrap deliverables

At repository root, create a runnable native Android app and include at minimum:
- `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml` if using a version catalog, `app/build.gradle.kts`, and `app/src/main/AndroidManifest.xml`;
- Kotlin source under `app/src/main/java/<package>/` with a real `MainActivity`, Compose screen routing and functional preset-scene interactions;
- `.gitignore`;
- `README.md` with the project purpose and quick start;
- `docs/BUILD_AND_RUN.md` with verified local run/build instructions, JDK/SDK requirements and commands such as `./gradlew test`, `./gradlew assembleDebug`, and `./gradlew connectedAndroidTest` where applicable;
- `docs/ARCHITECTURE.md`;
- `docs/ART_BIBLE.md`;
- `docs/ASSET_MANIFEST.csv`;
- `docs/SAFETY_RISK_REGISTER.md`;
- `docs/TEST_REPORT.md`;
- `tests/` or an equivalent runnable test harness;
- a clear distinction between runtime-ready assets, prototype assets and reference-only images.

Do not fill these files with claims that are not true yet. Update them as implementation progresses.

## 2. Product principles

SMRITI is an everyday-life, person-directed game and memory-support companion, not a diagnostic, cognitive assessment or treatment device.

- Offline-first; no account or internet required for core play.
- Tamil-first, with English as an available language. Do not invent claims of native-speaker review; mark unreviewed dialogue clearly in development documentation.
- All main activities can be entered, exited, paused or completed with assistance.
- No failure screens, wrong-answer sounds, red crosses, penalties, streaks, leaderboards, countdowns, forced completion, shame, or disappointed NPC reactions.
- Keep gameplay state truthful: never mark an item as collected or a task as completed unless the state changed. Display neutral status, not “wrong”.
- Help is available immediately and never unlocks only after mistakes.
- No hidden tests or inferred cognitive scores. Do not infer decline from speed, errors, skipped actions, hint use or task completion.
- No clinical claims such as improving cognition, treating dementia, preventing decline or guaranteeing safety.
- Build for a person with varying vision, hearing, motor control, language, attention and comfort—not a presumed uniform user.

## 3. Visual design system

Create a small, documented style guide before producing lots of art:
- Soft 2D-comic game illustration with consistent line thickness, palette, perspective and shading.
- South Indian setting with thoughtful, researched details: veranda/thinnai, tiled roof, wooden doors, plants, household vessels, cloth, local market stalls and culturally appropriate clothing.
- Warm cream/terracotta/leaf-green/indigo palette; sufficient contrast between interactive objects and their background.
- Characters are friendly, recognisable adults with consistent hair, clothing, proportions and faces across scenes.
- Avoid excessive visual clutter, busy patterns behind objects, blinking highlights, rapid transitions, exaggerated facial expressions and flashing animations.
- Keep text separate from art. Never bake UI labels, instructions, prices, buttons or dialogue into the background image.
- UI must be actual Jetpack Compose UI with responsive layout and localisable text.
- Avoid random generated variations of the same character; use a defined canonical character reference.

Create `docs/ART_BIBLE.md` with palette, line/shading rules, asset dimensions, naming conventions, character continuity, cultural-review notes and export rules.

## 4. Asset pack: use honestly

The supplied `SMRITI_ASSET_REFERENCE_PACK_v2.0.zip` contains seven generated **concept atlases**. They have multiple panels and labels baked into the images. They are visual references only, not a ready sprite library. Do not use a whole atlas as a scene background. Do not crop an object and automatically call it production-ready.

### Required asset workflow
1. Preserve the original reference archive and extracted concept boards under `art_source/reference_only/` (outside the runtime `assets/` tree) and retain the README/manifest. Keep reference-only atlases outside runtime resource folders; Android must not package them as scene backgrounds or sprites unless deliberately selected for an internal developer gallery. Final cleaned runtime assets belong under `app/src/main/assets/` or `app/src/main/res/drawable-nodpi/` and must be listed separately in the manifest.
2. Review all seven atlases and select one master style direction. Record decisions in `docs/ART_BIBLE.md`.
3. Create a status manifest `assets/ASSET_MANIFEST.csv` with: asset_id, path, category, intended_use, source/provenance, dimensions, alpha/transparency status, status (`reference`, `needs_cleanup`, `approved_for_prototype`, `approved_for_release`), review notes.
4. For each needed asset, use the reference as guidance and generate or author a separate, text-free scene image, prop or character sprite. If the image-generation tool is unavailable in the coding environment, create an explicit placeholder and document the missing asset; do not claim it exists.
5. Clean exports: no baked labels, UI, panel borders, neighbouring assets, watermarks, visible cropping, unexplained shadows, or background remnants. Use transparent PNG/WebP for isolated sprites where appropriate; scene backgrounds may be opaque. Preserve source files and record derivative relationships.
6. Check aspect ratio, resolution, edge quality, contrast, consistency and asset licensing/provenance. Do not state licensing rights that have not been checked.
7. Keep art assets and UI logic separate. Compose must render text, dialogue, shopping list, ticket and buttons dynamically.
8. Use actual sprite frames or state-driven animation for character speech, gestures and simple actions. Do not pretend a static portrait is an animation.
9. Do not attempt to produce every final asset before the first scene is running. Prioritise one complete vertical slice, then expand.

### Initial asset inventory
- Environments: house exterior, veranda/home hub, bedroom, kitchen, sewing corner, front-door/destination scene, bus stop, bus interior (seated passenger view), market entrance, fruit/vegetable stall, spices/pantry stall, flower stall, cloth/textile stall.
- Characters: SMRITI Guide; bus conductor; fruit/vegetable vendor; spice vendor; flower vendor; cloth vendor; optional tailor/helper. Use one canonical appearance per character, with a few useful poses/expressions.
- Household props: door, table, chair, bed, Memory Book, radio, sewing machine, fabric pieces, thread spools, needles/scissors represented safely as decorative non-interactive art, pots/pans, plates, sink, sponge, food ingredients, shelves, baskets, bag, plants.
- Market items: individually identifiable vegetables/fruits, spice jars, flowers/garlands, cloth swatches and folded fabrics.
- UI icons: Home, Back, Guide/Help, Voice replay, Stop speech, Audio on/off, Shopping list, Bag, Memory Book, Radio, Cooking, Wash dishes, Sewing, Bus, Exit.

Do not invent hundreds of items. A small coherent, high-quality set beats a large inconsistent atlas.

## 5. Target platform, layout and controls

Android phones and tablets are the initial target. Landscape and portrait must both work; landscape is not merely a rotated/cropped portrait layout.

- Use responsive containers and density-independent sizing.
- Support different aspect ratios, larger text, tablet layouts and resizable windows using Android adaptive layouts and Compose window constraints.
- Never stretch the artwork non-uniformly. Use crop/contain/layout reflow intentionally and protect important interactive regions.
- Preserve the current scene, selected items, activity progress, radio playback state (or a safe stopped state), shopping state and preferences through rotation, app backgrounding and resume.
- Use large, forgiving touch targets; tap-based alternatives must exist for any drag gesture.
- Do not require precision drawing, repeated fast taps, long presses or multi-finger gestures.
- Keep a consistent, compact global navigation region. The Guide is accessible at the bottom-left on gameplay scenes where it does not cover the active object; adapt placement on narrow screens and dialogs.
- Global actions: Help/Guide, Back/Home, Stop/Leave, audio settings. Never place these where an activity can permanently cover them.
- Provide reduced-motion mode, immediate transition/skip, independent narration and music volume, and clear mute controls.
- Test at least one narrow phone viewport, one typical phone viewport, one landscape phone, and one tablet-sized viewport in Compose previews/emulators and on available Android devices. Clearly distinguish editor/emulator tests from physical-device tests.

## 6. Core information architecture and game path

### 6.1 Launch and house exterior
- Start with a calm title/start screen that uses a real illustration and a clear Start action; avoid a dense menu.
- Show a welcoming South Indian house exterior. Tapping the front door enters the house/veranda.
- The player can leave/exit from the exterior or use an accessible Exit control. Confirm only if unsaved user-created content could be lost; do not add unnecessary confirmation dialogs.

### 6.2 Veranda/home hub
- The veranda is the main hub, clearly showing the bedroom, kitchen, sewing corner and front door.
- Use visible, well-separated hotspots or concise labels on demand. Do not permanently cover every object with a text label.
- Tapping a room opens that room's scene. Tapping the front door opens a destination chooser: Market or Stay Home/Cancel. No Park or Temple in this version.
- A small persistent Guide control opens contextual assistance; dialogue panel text and voice are dynamic, not baked into art.
- Use a small number of visible interactive elements with predictable focus order.

### 6.3 Bedroom: Memory Book + personal radio
**Memory Book**
- Open a book-like UI/illustrated scene with large pages/cards. It is a personal memory-support feature, not a quiz.
- Content is user/caregiver-added, editable, deletable and stored locally by default. Do not fabricate a user's memories or relationships.
- Make it clear which content is sample/demo content and which is user-provided.
- Do not collect or upload photos/audio. Ask only for the permissions actually needed; avoid camera/microphone permissions unless a later, explicit feature genuinely requires them.
- Offer large Previous/Next/Close controls and optional narration of a page. Never ask the person to identify who is in a photo or recall a fact to proceed.

**Radio**
- A radio beside the Memory Book opens a caregiver-curated, locally stored playlist of licensed or user-provided songs/audio.
- No automatic playback on scene entry. The user selects a track.
- Large Play/Pause, Next, Stop, volume and Back controls; Stop must stop immediately. Music and Guide narration have independent volume/mute.
- Voice assistance may read song titles if configured. Do not use streaming-only tracks for offline functionality.
- Include an easy way for the person to skip/remove a disliked track from the current listening session. Caregiver management of the playlist must be separate from casual gameplay and protected from accidental changes.
- Do not claim music therapy or therapeutic outcomes. Audio can evoke distress; the person must be able to stop or switch immediately.

### 6.4 Kitchen: Cook and Wash Dishes
Two separate activities, both accessible from the kitchen.

**Cook**
- Start with one simple familiar recipe/activity (for example, make filter coffee or assemble a simple dish) after cultural/content review.
- Use a small number of visible ingredients and tap-to-select/tap-to-place. Optional drag is not the only interaction.
- Show the current step visually. Provide Repeat, Show Me, Do It for Me, Undo/Reset and Leave at any time.
- No timers, hot-stove danger simulation, knife use, exact measurements, arithmetic or failure states.
- State changes must be real and consistent; do not show a completed dish until the interaction reaches its final state or the user requests assistance.

**Wash Dishes**
- Use large illustrated plates/cups, sink area and a simple wash/place interaction.
- Avoid realistic water physics. Tapping an item can wash it through a short animation; tap-to-place on a rack is enough.
- Provide free mode and a gently guided mode. Any reasonable placement can be accepted in free mode.
- No countdowns, penalties or negative feedback. Provide a one-tap assisted completion option.

### 6.5 Sewing corner: Stitch & Create
- A sewing machine and folded fabrics are visible in a dedicated corner, not a cluttered full-screen form.
- The player can choose fabric, then choose a simple creation: cloth bag, pillow cover, shirt/blouse as illustrated templates. Do not impose gendered assumptions; offer neutral options and user choice.
- Fabric purchased from the cloth shop should be available here after returning home. Also provide a starter/default fabric so the activity remains available even if the player skips the market or buys no cloth.
- The player selects a pattern and advances through a small number of large tap interactions. The game shows a gentle stitching animation and produces the chosen item.
- No timed sewing, precision tracing, sharp-tool interaction, fast reaction, or correctness grading.
- “Show me”, “Do it for me”, Undo, Start over, Save/Keep creation and Leave are available.
- Persist the created item and chosen pattern. The result can be displayed in the sewing corner or added to an optional personal creations gallery. Do not force the user to finish.

### 6.6 Front door and market trip
- From the veranda, tapping the front door opens a destination chooser with Market and Stay Home/Cancel. The player may revisit the chooser whenever they return home.
- Market trip can be optional. Do not lock household activities behind a successful trip.
- At home, the player may be offered a small picture shopping list before departure. Keep the list available on request and visible during shopping. It is not a hidden-memory test.
- The player may choose bus travel or, for accessibility, “Arrive at market”/skip travel. The bus activity is optional, not a gate.

### 6.7 Bus stop: optional bus-number interaction
- If the player chooses the bus activity, present one bus number clearly and read it aloud if narration is enabled. Keep Help available.
- At the bus stop, show a small number of large, legible bus options. The user can tap one. If it is not the suggested number, do not display a wrong-answer state or prevent boarding; proceed to the bus in a neutral way.
- If the user requests Help, repeat/show the number. Never make help unlock after an error.
- Do not treat a bus-number selection as a clinical memory test. If event logging is implemented, log only a transparent, consented factual event, not a cognitive score.
- Avoid confusing or visually similar numbers and ensure contrast and text sizing. Use consistent bus-number presentation from cue to stop.

### 6.8 Seated bus scene and conductor
- Show a prepared illustrated bus interior from a seated passenger viewpoint: nearby seats/passengers, handrails, windows, conductor and ticket device. This is a fixed illustrated scene, not first-person 3D.
- The conductor is visible at a comfortable distance. He does not start dialogue automatically. The user can tap “Talk to conductor” or a clear character hotspot.
- When selected, the conductor speaks through a dialogue box with subtitle text. Controls: Replay, Stop Voice, Continue/Skip, and Close/Leave where appropriate. The character may use a small authored set of expression/gesture frames, not a static image falsely described as animation.
- Conductor gives a simple ticket with route/destination and fare. The fare remains visible during payment.
- Payment is a fictional, no-real-money activity using a simplified machine with a small set of large coin/note choices. No actual financial data or payment SDK. No real-world financial literacy claims.
- Offer “Show me” and “Pay for me” from the start. If the player selects a different amount, do not show failure or trap the user; allow retry neutrally or assisted completion. The ticket amount remains visible. Do not require exact change or complex arithmetic.
- Include Skip Ride/Arrive and Exit/Back controls. Reduced Motion mode uses static window scenery or a brief non-moving transition.

### 6.9 Market entrance and four stalls
The market contains four selectable stalls: Fruits & Vegetables, Spices & Pantry, Flowers & Garlands, Cloth & Textiles. No Park or Temple destination in this version.

- Market entrance shows four illustrated stall choices or an illustrated market view with four large hotspots.
- Each stall has a distinct background, a vendor, a counter/display and a concise interaction area.
- Vendor dialogue is authored, brief, Tamil-first with English alternative, and optional. NPCs do not interrupt or pressure the player.
- Stall interiors should look like the player has entered a shop, not like a quiz card over a background.
- Show the picture shopping list as a compact, persistent/reopenable panel that adapts to portrait/landscape/tablet. It must not obscure target items.
- Each product has a stable item ID, icon/sprite, stall category, label, optional audio label and inventory state.
- Tap-to-select/tap-to-bag is the primary interaction; dragging is optional. Provide a Nearby/Items list alternative if useful.
- If a non-requested item is selected, treat it as browsing. Do not remove required items or mark them as collected incorrectly. The player may inspect, put it back or continue.
- Hints are available immediately: (1) show/replay picture list; (2) spoken name/category; (3) gently identify shelf/region; (4) highlight the relevant item; (5) demonstrate or “Place it for me”. The user can select any hint level without errors or waiting.
- The player can leave any stall at any time. Do not require all items to be bought to leave the market or return home.
- Cloth shop offers a few clearly distinct cloth patterns. On selection, show the fabric being placed into the bag. Persist the fabric's stable item ID and make it available in the home sewing corner after returning. Do not depend on remembering the pattern later.
- Use only a small coherent initial catalogue; avoid hundreds of products. Ensure pictures are visually distinguishable without relying on colour alone.

### 6.10 Return home and state persistence
- Return trip uses the same optional bus pattern or direct-arrival option. Do not force another bus-number recall task.
- On returning, the veranda hub appears. Purchases are available in the bag/inventory; purchased cloth is available in the sewing corner.
- User can unpack purchases if desired, but do not make unpacking a mandatory task.
- All progress must persist correctly through scene transitions, rotation, backgrounding and resume.
- A user can end a session from any scene without losing the entire session or being shamed.

## 7. Guide and voice/dialogue system

The SMRITI Guide is a recurring friendly adult character and the bottom-left assistance entry point on gameplay scenes, adapted to avoid covering the main activity. NPCs include conductor, four vendors and optional helper/tailor.

- All speech is opt-in at the interaction level. No automatic NPC monologues or repeated unsolicited prompts.
- Use authored, reviewed dialogue strings and recorded audio or verified on-device Android TextToSpeech where the required voice is available offline. Do not call a cloud TTS service for core functionality. If audio files are not yet available, use a clear development placeholder and make subtitles functional.
- Tamil-first with English fallback. Keep each line short, concrete and calm. Avoid idioms that may not translate well.
- Every spoken line has matching subtitle text. Replay and Stop Voice work reliably.
- Narration, NPC dialogue, music/radio and ambient sounds have separate volume controls.
- Tapping Guide opens context-relevant options such as Repeat, Show Me, Help with this scene, Do it for me, and Leave/Back. Help does not require speech recognition.
- No always-listening microphone and no speech recognition in this scope.
- Never make an LLM-generated character conversation a requirement. Dialogue must be deterministic, local and testable.
- Ensure speech cannot overlap uncontrollably: new voice starts should stop or queue previous voice predictably. Leaving a scene stops its dialogue audio.

## 8. Data, caregiver settings and privacy

Keep core game offline and local. Do not add analytics, advertising, accounts, cloud sync or unnecessary permissions.

Caregiver features should be modest and transparent in the first release:
- Optional caregiver settings for adding/removing radio tracks and managing local Memory Book content.
- Optional activity event log only if the product genuinely needs it and the UI explains what is stored and why. Require an explicit opt-in for caregiver-facing logs, with an accessible off switch and deletion/export controls as appropriate.
- If logged, store factual events such as `hint_requested`, `assisted_completion`, `item_added_to_bag`, `activity_skipped`, `bus_option_selected`; do not convert them into a cognitive score, “dementia progression”, diagnosis, ability ranking or caregiver alert without separately validated and governed research.
- Distinguish unassisted interaction, hint use, assisted completion, skip and cancellation; never interpret help-seeking as inability.
- Avoid collecting names, exact age, health records or other sensitive data unless strictly required and justified. Do not upload personal content.
- Review Android backup/device transfer rules before release; document whether personal data is backed up or excluded. Do not claim encryption or compliance unless implemented and reviewed.

## 9. Architecture and code quality

Use a maintainable Kotlin/Jetpack Compose structure. Adapt package and folder names to the project, but separate responsibilities:

### 9.1 Required Android implementation details
- Use one native Android app module unless there is a documented reason to split modules. Kotlin DSL Gradle files (`*.gradle.kts`) are preferred.
- Use a single `MainActivity` hosting Compose; implement screen/scene navigation as explicit, testable state or Navigation Compose destinations. Do not add a complex game engine.
- Use Jetpack Compose for buttons, lists, dialogue, HUD, menus, radio, memory book and minigame interaction. Avoid building the interface as huge text buttons around a tiny central image.
- Use cleaned scene illustrations as backgrounds, then add separate composable hotspots and interactive objects. Never bake labels, dialogue, shopping lists, buttons or status indicators into the scene artwork.
- Scene hotspot positions must be anchored to the rendered image content, not guessed raw screen coordinates. Preserve artwork aspect ratio; account for crop/letterboxing and recompute hit regions when the available window size changes. Where one crop cannot work well in both orientations, provide portrait and landscape art/layout variants instead of stretching the image.
- Keep UI state in `ViewModel`/state holders as appropriate, use lifecycle-aware collection, and separate persistent domain state from transient animation/dialogue state. Avoid a single giant composable or global mutable state.
- Use DataStore for simple preferences and Room or a small versioned local repository for structured state only if complexity justifies it. Keep the first version as simple as possible and document the choice.
- Use Android audio APIs/Media3 only if needed and dependency compatibility is verified. Avoid internet-only music services. For speech, provide an interface that can use bundled prerecorded audio and/or on-device Android `TextToSpeech`; check whether the Tamil voice is available offline on the actual device. Subtitles must always remain available. Never claim offline Tamil speech works until it has been tested.
- Use resource strings (`strings.xml`) for all UI strings and test Tamil Unicode/font rendering. Keep game dialogue text separate from artwork and localisable.
- Choose stable compatible dependency versions, use the Gradle wrapper, document required JDK/Android SDK, and don't upgrade the toolchain blindly.
- Unit tests should cover domain/state transitions; Compose UI tests should cover navigation and essential actions where feasible. If Android SDK/emulator/device is unavailable, clearly state what could not be tested.



- `app/src/main/java/<package>/ui/` — Compose screens, reusable widgets, design system, responsive layout and navigation.
- `app/src/main/java/<package>/domain/` — stable IDs, entities, use cases, state models and activity state machines.
- `app/src/main/java/<package>/data/` — repositories, local storage, asset/catalog access and migrations.
- `app/src/main/java/<package>/audio/` — voice output, bundled audio playback, radio playback, audio focus and interruption handling.
- `app/src/main/java/<package>/interactions/` — hotspot/action dispatch and accessible list alternatives.
- `app/src/main/assets/scenes/` — only cleaned, approved scene backgrounds; optional portrait/landscape variants.
- `app/src/main/assets/characters/`, `props/`, `items/`, `audio/` — cleaned runtime illustrations and audio, organised by stable IDs.
- `art_source/reference_only/` — concept atlases and raw AI-generated boards, excluded from runtime assets.
- `app/src/test/` and `app/src/androidTest/` — JVM unit tests and Android/Compose UI tests.
- `docs/` — build/run, architecture, art bible, asset manifest, privacy/data, safety risk register and test report.

Requirements:
- Stable IDs for scenes, objects, items and characters; never use filenames or screen coordinates as persistent item identity.
- Central scene navigation rather than scattered hard-coded scene paths.
- Central interaction/action dispatch so world hotspots and list-based alternatives call the same function.
- Explicit state machines for bus trip, ticket/payment, shopping, cooking, dishwashing and sewing.
- Versioned local save schema and safe migration/default handling.
- No giant single script containing the whole game.
- No placeholder “pass” functions for advertised features without explicit issue/status labels.
- No hard-coded text in scene artwork. Use localisation keys.
- Graceful handling of missing optional audio/asset files; never crash due to missing song or sprite.
- No secrets, API keys, network calls or unnecessary external services.

## 10. Dementia-sensitive design and risk register

This is not a clinical safety certification. Create `docs/SAFETY_RISK_REGISTER.md` and track at least:
- visual clutter or poor contrast;
- confusing navigation/unclear way back;
- overly long or unsolicited dialogue;
- distressing or unwanted music;
- motion discomfort;
- inconsistent item/inventory state;
- payment interaction confusion;
- hidden assessment/performance monitoring;
- cultural assumptions in stories, songs, clothing or household content;
- privacy loss from personal photos/audio;
- loss of state on rotation/backgrounding;
- inaccessible Help/Stop/Exit.

For each risk, document mitigation, test evidence, status, owner and what remains unverified. The UI should always offer immediate Stop/Back/Help. Use no timers, failure screens or punitive feedback. Never conduct testing with people living with dementia until appropriate expert review, consent/assent and dissent/stop procedures, and institutional ethics approval are in place. Developer testing is not clinical validation.

## 11. Implementation milestones — complete sequentially

Do not generate the entire game in one giant pass. Build in runnable increments and commit/checkpoint when safe.

### M0 — Project bootstrap
- Create the native Kotlin Android Gradle project directly at the root of `Vishant-1001/Smriti-final`; establish `.gitignore`, Gradle files, Compose navigation skeleton, design system, asset manifest and build instructions.
- Verify `origin` points only to `https://github.com/Vishant-1001/Smriti-final.git`, and verify branch/upstream state.
- Build and run the app; record actual results. Commit and push this milestone if the build/tests and GitHub authentication permit.

### M1 — One polished vertical slice: exterior → veranda → bedroom
- House exterior, tap door, veranda hub, tap bedroom, Memory Book and radio mock/functional local playback UI.
- Create clean standalone exterior/veranda/bedroom backgrounds and the required props; do not use an atlas as a background.
- Guide panel, dialogue subtitles, audio controls, Back/Home/Exit.
- Responsive portrait/landscape layouts and safe resume.
- This milestone is not passed until the app is runnable and a screenshot shows the scene looking substantially better than the old primitive UI.

### M2 — Kitchen
- Cooking activity and dishwashing activity with deterministic state and help/skip/assisted completion.

### M3 — Sewing corner
- Fabric choices, item templates, simple stitch animation, persistent result, default fabric.

### M4 — Market foundation
- Market entrance, four stalls, vendors, shopping list, bag/inventory, hints, browsing, item persistence.

### M5 — Bus travel
- Bus stop, number cue/choice, seated bus interior, opt-in conductor dialogue, ticket, fictional payment interface, direct-arrival and reduced-motion path.

### M6 — Integration and hardening
- Full round trip home → market → home; bought cloth appears in sewing; radio and Memory Book persistence; audio interruption handling; localization; accessibility; device matrix; privacy and backup review.

Do not begin a later milestone while the current milestone is broken. If time is limited, finish fewer features to a higher standard rather than adding decorative, nonfunctional scenes.

## 12. Testing and acceptance criteria

Write automated tests for state logic where practical, and run actual builds. At minimum verify:

**Navigation**
- Launch → exterior → veranda → bedroom → back to veranda.
- Veranda → kitchen → cooking/dishes → back.
- Veranda → sewing → choose fabric/item → create → leave and return; result persists.
- Veranda → front door → Market; cancellation returns home.

**Market/inventory**
- All four stalls open and close.
- Correct requested item can be added to bag.
- Unrequested browsing does not remove requested items or mark them collected.
- Hints work at any time.
- Purchased cloth is available in the sewing corner after the round trip.
- User can leave without buying everything.

**Bus**
- Bus number cue and options are legible in both orientations.
- Any bus selection can proceed without failure feedback.
- Conductor dialogue starts only after interaction.
- Ticket fare remains visible while paying.
- Show Me/Pay for Me work.
- Stop voice, skip ride and direct arrival work.
- No real money or network payment service is invoked.

**Bedroom/audio/privacy**
- Radio never autoplays on entering the room.
- Stop, pause, skip and volume work; radio and narration volumes are independent.
- Missing audio does not crash the app.
- Memory Book demo vs personal content is clear; delete/edit controls work.
- No data uploads or unnecessary permissions.

**Accessibility/lifecycle**
- Portrait and landscape; phone and tablet-sized layouts.
- Larger text and reduced motion.
- Guide, Back, Help and Stop accessible on each gameplay screen.
- Rotate/background/resume without losing the current scene or corrupting state.
- No text baked into backgrounds, no stretched images, no hidden off-screen controls.

**Visual quality**
- Compare actual screenshots to `docs/ART_BIBLE.md` and reference atlases.
- Verify that every production scene is an individual text-free asset or intentionally layered composition.
- Verify characters are consistent across scenes and no cropped atlas panel edges are visible.
- Check object silhouettes, touch target sizes, contrast and screen density.

Use actual test names, commands and results in `docs/TEST_REPORT.md`. Separate JVM unit tests, Compose UI tests, emulator tests and physical-device tests. Do not claim physical-device verification if none occurred.

## 13. Definition of done for a milestone

A milestone is done only when:
1. Code and assets exist in the project.
2. The project launches/builds under the documented JDK/Android SDK environment.
3. The feature can be reached and completed through the actual UI.
4. State is consistent and persists where required.
5. The relevant tests pass and failures are reported.
6. Visual QA has been performed using actual in-game screenshots.
7. Known defects and limitations are documented.

An attractive mock-up, screenshot, generated image or code that has not been run is not a completed feature.

## 14. Required final report after every work session

Return these exact sections:
1. **What I inspected** — Kotlin/JDK/Gradle/Android SDK versions, files, branch/Git status and baseline build result.
2. **What I implemented** — exact features and files changed.
3. **Assets created/cleaned** — exact paths, dimensions, status, and what remains reference-only.
4. **Build and test evidence** — exact commands and outputs/results.
5. **Screenshots** — paths to actual runtime screenshots, not concept art.
6. **Unimplemented/blocked** — clear list, no euphemisms.
7. **Safety/privacy risks** — status and open issues.
8. **Next step** — one specific highest-value task.

Never say “the game is complete” when only a milestone is complete. Never claim that generated artwork is fully production-ready unless it has passed the asset checks above. Explicitly report GitHub state: remote URL, branch, last local commit, whether `git push` succeeded, and verified remote commit hash. Do not claim a GitHub update without command evidence.

## 15. Begin now

Inspect the designated empty repo and Android toolchain, verify the exact GitHub remote, create the clean Kotlin/Jetpack Compose Android project at the repository root, and implement M0 followed by the smallest complete M1 vertical slice. Commit and push verified milestones to `main` when authentication and remote state allow. Use the supplied concept atlases only as references. Generate/author clean standalone assets for the exterior, veranda and bedroom. Build, run, test, capture actual runtime screenshots and report honestly. Continue coding after the baseline report rather than stopping at a plan.

## PROMPT END

---

## Package notes for the user

- Open the empty `Smriti-final` repository as the coding agent workspace. Give the agent this prompt and `SMRITI_ASSET_REFERENCE_PACK_v2.0.zip` together.
- Do not use the old prototype repo or create a nested `smriti_rebuild/` folder; build at the root of the new repository.
- Keep reference art separate from runtime art, and commit/push only after reviewing changes and running the relevant tests.
- The reference pack contains concept atlases, not final isolated sprites. The prompt explicitly requires asset cleanup/recreation and prevents the agent from claiming otherwise.
- The recommended first deliverable is the exterior → veranda → bedroom vertical slice. Do not build every feature at once.
