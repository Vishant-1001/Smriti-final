# SMRITI Fit: design specification (v1)

**Status: research prototype.** It is a pure-Kotlin module (`:fit`) that the app does not use unless a later integration step enables it. It makes no clinical claim. It uses no behavioural data and no learning.

## 1. Problem

SMRITI has explicit settings: language, in-app text size (×1.0/1.2/1.4 on top of the system font scale), reduced motion and names. It runs on phones, foldables, tablets and multi-window, in Tamil and English. Each screen currently turns these into a layout with hand-written thresholds:

| Rule | Where |
|---|---|
| Top bar in two rows below 600 dp | `SmritiRoot.TopBar` |
| Content starts at a fixed 168 dp (narrow) or 90 dp (wide), whatever the real top-bar height | `SmritiRoot.GameScreen` |
| Top-bar labels ignore the in-app text size so they fit | `Components.HudButton` |
| Buttons stacked below 600 dp (or in landscape below 900 dp); one per row below 380 dp | `RadioScreen` |
| Other screens: `< 420`, `maxHeight < 520`, `< 500` | `MiniPlayer`, `MemoryBookScreen`, `TravelScreens` |

These rules were tuned for the default settings, and they do work there (see `RealFontsTest`). With larger text, Tamil, landscape phones or new strings, nothing checks the result. The thresholds silently produce broken words, hidden controls, or a top bar that covers content, and the person's text-size choice is silently ignored on the top bar.

## 2. Approach

Fit turns explicit needs into a presentation that is checked against them, with reasons.

```
Needs (Settings: language, text level, need-or-preference, min target, reduced motion)
  + Device (width, height, system font scale, system bars)
  + Screen template (top bar + activity panel, real strings)
  + Locks (person/caregiver "keep this")      + previous accepted Config (stability)
        │
        ▼
FitSolver.solve ── enumerate Config space (≤ 960 per screen)
        │              └─ LayoutModel.evaluate: geometry from the app's Compose constants
        │                    + TextMeasurer (real fonts) → violations
        ├─ stage 1: all hard constraints + locks → best by preference order → OPTIMAL
        ├─ stage 2: relax the text-size need → RELAXED_TEXT_NEED
        ├─ stage 3: relax locks (this screen only) → RELAXED_LOCKS
        └─ else least-severe configuration → BEST_EFFORT (+ remaining violations listed)
        ▼
Decision(config, status, violations, explanations)
```

**Decision variables (`Config`):**
- top-bar rows (1–3);
- whether top-bar labels follow the in-app text size;
- top-bar button width cap (150 or 220 dp);
- button columns (1–4);
- icon beside or above the label;
- illustration on or off;
- landscape split or one scrolling column;
- text level (never above the level asked for).

The space is small enough to search exhaustively, so the result is optimal under the model and fully deterministic.

**Hard constraints (`ViolationKind`):**

| Constraint | Meaning |
|---|---|
| `ESSENTIAL_HIDDEN` | An essential control is off screen, squeezed out, or needs scrolling |
| `OVERLAP` | The top bar covers content |
| `TARGET_TOO_SMALL` | A control is smaller than the person's minimum target |
| `WORD_BROKEN` | A label word would be broken mid-word |
| `TOO_MANY_LINES` | A label needs more than 2 lines |
| `TITLE_TRUNCATED` | The place name is cut off |
| `TEXT_NEED` | Text is below a size declared as a need |
| `MOTION` | Animation is shown with reduced motion on |
| Locks | Every lock holds |

**Preferences (lexicographic, no weights to tune):**
1. Text closest to the level asked for.
2. Keep the illustration.
3. Fewest changes from the previous accepted configuration.
4. Fewest rows.
5. Icon beside the label.
6. Enumeration order.

**Explanations.** For every field that differs from the design as drawn (`asAsked`), Fit evaluates the counterfactual (that field reset) and reports the first new violation it would cause, the lock, or the stability reason. This contrastive form follows the idea of scrutable user models: the person can see why. It is not a learned explanation.

## 3. Geometry model (what is real and what is assumed)

**Real:**
- the bundled bold Noto Sans and Noto Sans Tamil fonts, measured with AWT `TextLayout`, which uses HarfBuzz shaping and falls back to the Latin font for runs the Tamil font lacks;
- the 282 English and 282 Tamil strings in `strings.xml`;
- the padding, sizes and type scale copied from `Components.kt`, `SmritiRoot.kt`, `RadioScreen.kt` and `Theme.kt` (constants in `Dims`);
- linear font scaling, as the app builds `Density(density, fontScale × textSize)` by hand.

**Assumed:**
- system bars take 48 dp (0 in Robolectric validation);
- the radio picture aspect ratio is 0.75;
- greedy word wrapping at spaces;
- generated "new activity" panels reuse the radio layout convention.

The model's agreement with real Compose rendering is checked separately (`FIT_PROTOCOL.md` F5).

## 4. Why this addresses SCOPE's failures

| SCOPE v2 failure | Fit |
|---|---|
| Harm under noisy or inconsistent feedback (P5 0.164) | No feedback is learned. Inputs are explicit settings, and "harm" is a checkable violation |
| Coverage 0.19 < 0.25 | Decides for every screen, every time (100% coverage) at zero question burden |
| Instability (switching) | Deterministic. Stability is an explicit preference, and the accepted decision is a fixed point (tested) |
| Value only visible against invented utilities | Measured against real strings and fonts, and validated against real Compose rendering |

## 5. Safety, privacy and control

- **Inputs.** Only explicit settings plus window size. A reflection test (`FitSolverTest`) pins the fields so nothing behavioural can be added silently.
- **Persistence (`FitState`).** Stores needs, locks and the last accepted config per screen. Versioned `key=value` encoding, fail-closed decoding (unknown version, key or range → `null` → defaults), deterministic replay.
- **Overrides.** Locks are hard constraints. If a lock cannot fit a screen it is set aside **for that screen only**, with an explanation, never silently.
- **Nothing hidden silently.** When no layout meets every need, the remaining problems are returned so the UI can show them.
- **No clinical claim.** Correct layout is an accessibility and engineering property, not a health outcome.

## 6. Files

- `fit/src/main/kotlin/app/smriti/fit/`:
  - `Model.kt` (types)
  - `Layout.kt` (measurer, geometry model)
  - `Solver.kt` (solver, explanations)
  - `Baselines.kt` (B1 current rules, B2 greedy repair)
  - `FitState.kt` (persistence and replay)
- `fit/src/main/kotlin/app/smriti/fit/sim/`:
  - `Content.kt` (AWT measurer, strings, real screens, content split)
  - `Instances.kt` (seeded generator)
  - `FitBenchmark.kt` (benchmark, bootstrap, gates, demo)
- `fit/src/test/kotlin/app/smriti/fit/`: `FitSolverTest.kt`, `RealFontsTest.kt`
