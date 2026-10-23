# SMRITI Fit: evaluation protocol v2 (committed before any v2 evaluation or validation run)

## Why a v2 exists: v1 verdict NO-GO

Under `FIT_PROTOCOL.md` (v1, commit `f11b561`):

| Gate | v1 result | Where |
|---|---|---|
| F1 correctness | pass | `FIT_EVAL_RESULTS.md` |
| F2 vs current rules | pass | `FIT_EVAL_RESULTS.md` |
| F3 not worse than greedy repair | pass | `FIT_EVAL_RESULTS.md` |
| F4 latency | pass | `FIT_EVAL_RESULTS.md` |
| **F5 model validity** | **FAIL: 507/600 = 84.5% < 90%** | `FIT_VALIDATION.md` |

**v1 verdict: NO-GO.** Under the v1 integration rule Fit was therefore **not** integrated. These files are kept unchanged.

## Diagnosis (on the burned v1 validation seeds 9001–9060 and the dev split only)

1. **Instrument error: title check.** The validator used `TextLayoutResult.hasVisualOverflow`, which is true for every one-line title, including "Radio" on a 1029 dp tablet. The cause is that the 28 sp line height trims the glyph box. This single check produced 57 of the 93 disagreements.
2. **Instrument error: squeezing.** In a non-scrolling column, Compose squeezes rows instead of moving them off screen. Pause/Stop then stay "visible" but are shorter than their 64 dp minimum with clipped labels, or the next row disappears (renders saved under `app/build/fit-validation/`, not committed). The model's "essential control not fully usable" is right; the validator only detected the off-screen case.
3. **Model error: font scaling.** On Android 14+ (API 34), Compose converts sp to dp **non-linearly** for total font scales between about 1.03 and 2.0, and linearly above 2.0. The v1 spec assumed linear scaling. `FontScaleTableTest` recorded the conversion from Compose on SDK 34 into `fit/src/main/resources/app/smriti/fit/font_scale_sdk34.tsv`. Example: 18 sp at ×1.3 → 21.6 dp (linear would give 23.4), and at ×1.82 → 27.8 dp. A consequence for the app: system 1.5 × in-app LARGER (= 2.1) jumps back to linear (18 sp → 37.8 dp), which is *larger* than system 2.0 alone (30 dp).
4. **Ruled out: font measurement.** `FontWidthTableTest` measured all 1,424 strings and words with Android `Paint` and the bundled fonts. AWT agrees with Android to within ±1% (p5–p95) in both languages (`FIT_FONT_CHECK.md`).

With fixes 1–3, a development re-run on the burned seeds 9001–9060 agreed 582/600 = 97.0% (`FIT_VALIDATION_v2_DEV_seeds9001-9060.md`). Those seeds have been seen twice, so this is **not** evidence for the gate.

## Changes in v2 (all made before this file was committed)

| Change | What |
|---|---|
| Model | `FontScaling` uses the recorded SDK 34 table when `Device.nonLinearFontScaling` (API 34+), and linear scaling otherwise |
| Generator | 60% of devices are API 34+ (an assumption; strata are reported) |
| Validator | "Pause/Stop hidden" means outside the visible area **or** squeezed below 64 dp. "Settings squeezed" also checks height. The title check uses ellipsis, > 2 lines or a mid-word break instead of `hasVisualOverflow` |

The solver, preference order, baselines (B1, B2), checks and gate thresholds are **unchanged**.

## Seeds (fresh; never used by SCOPE or Fit)

| Use | Seeds |
|---|---|
| v2 evaluation (F1–F4) | **10001–15000**, held-out content pool |
| v2 validation (F5) | **19001–19100**: 100 instances, real radio screen, Robolectric SDK 34 (non-linear scaling applies) |
| Development (unchanged) | 1–2000 |

## Gates

Identical to v1:

| Gate | Requirement |
|---|---|
| F1 | Correctness |
| F2 | B1 − Fit geometric-violation rate: CI lower bound > 0 and ≥ 0.05 |
| F3 | Not significantly worse than B2 on violations and text deficit |
| F4 | p95 ≤ 20 ms |
| F5 | Per-check agreement ≥ 90% over all (instance, check) pairs, on the 100 fresh validation instances |

**If any gate fails, the v2 verdict is NO-GO.** No v3 would then be run without a new protocol, and the fallback (`RESEARCH_REPORT.md`, B) is recommended.

## Integration rule (unchanged)

Only if F1–F5 all pass may Fit drive **one** real screen (the radio). It must:
- sit behind a flag;
- use the same solver with an on-device text measurer;
- be followed by a descriptive real-render comparison of Fit and the current layout on the F5 instances (V6).

Fit is enabled by default only if V6 shows fewer observed problems than the current layout and no instance where Fit shows a problem that the current layout does not.
