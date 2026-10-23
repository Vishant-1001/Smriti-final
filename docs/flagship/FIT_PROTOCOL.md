# SMRITI Fit: evaluation protocol (v1, committed before the evaluation run)

## Data and split

| Split | Seeds | Content | Use |
|---|---|---|---|
| Development | 1–2000 | Development half of the button, title and status pools + the real radio screen | Model debugging (`FIT_DEV_RESULTS.md`, `runFitExamples`). Already run |
| **Evaluation** | **3001–8000** | **Held-out half** of the pools (split by `String.hashCode` of the resource name) + the real radio screen | The gates below. Run once, after this file is committed |
| Validation (F5) | 9001–9060 | Real radio screen only | Robolectric rendering of the real app against the model |

Seeds ≥ 3001 have never been used by SCOPE or by Fit development.

The instance mix (device classes, font scales, text levels, need vs preference, targets, language) is an assumption stated in `Instances.kt`. Results are also reported per stratum.

**Changes made on development data before this protocol:**
- text is never set above the level asked for;
- added the top-bar width and landscape-layout knobs, and gave B2 the same knobs;
- the OVERLAP tolerance accounts for the 16 dp content padding;
- B2 starts with top-bar labels following the text size, which makes it stronger.

The code is frozen at the commit that adds this file. Any later change to the solver, model or baselines will be reported as a deviation.

## Policies

- **B1:** the current app rules, ported exactly (`CurrentRules`; thresholds unit-tested).
- **B2:** the current rules plus measured top-bar reservation plus greedy checker-in-the-loop repair with a fixed precedence. It has the same knobs as Fit (`GreedyRepair`).
- **Fit:** `FitSolver.solve`, with no locks and no previous configuration.

## Metrics

All metrics are per instance and computed by the same `LayoutModel.evaluate`:
- any geometric violation;
- severe violation (hidden essential control or overlap);
- any violation including the text need;
- text deficit (levels below what was asked, top bar + content);
- picture kept;
- violations by kind;
- solver status;
- explanation coverage;
- time per decision.

Paired bootstrap 95% CIs over instances (2000 resamples, seed 99).

## Gates (all must pass for GO)

| Gate | Requirement |
|---|---|
| **F1 correctness** | On every evaluation instance, Fit's status is OPTIMAL exactly when its output has no violation, and it has no violation on every instance where a feasible configuration exists |
| **F2 vs current rules** | Geometric-violation rate: B1 − Fit has a CI lower bound > 0 and a point estimate ≥ 0.05 |
| **F3 not worse than greedy repair** | For both the geometric-violation rate and the text deficit, the CI of B2 − Fit has an upper bound ≥ 0 (Fit not significantly worse). Whether Fit is *strictly* better (CI lower bound > 0) is reported, but it is not required |
| **F4 latency** | p95 time per screen decision ≤ 20 ms on the JVM (this machine, including first-call text measurement) |
| **F5 model validity** | On the 60 validation instances, the app's **current** radio screen is rendered with Robolectric (native graphics, no system bars) at the instance's size, font scale, language and text size, with a song playing. The observed checks are: Pause/Stop below the window, mid-word breaks or > 2 lines in the four button labels, top bar overlapping content, Settings squeezed, and place name ellipsised. These are compared with the model's B1 predictions for the same instance. **Pass if per-check agreement ≥ 90% over all (instance, check) pairs.** Confusion matrices are reported per check |

The picture-kept rate is reported but not gated. Fit gives up the picture to satisfy hard constraints by design.

**Thresholds will not be changed after seeing evaluation or validation results.** If any gate fails, the verdict is NO-GO and the fallback (verified assistance statechart, `RESEARCH_REPORT.md` B) is considered.

## Integration rule

Only if F1–F5 pass may Fit drive the layout of **one** real screen (the radio). It must:
- sit behind a flag;
- use the same solver with a Compose text measurer;
- be followed by a descriptive (not gated) real-render comparison of Fit and the current layout on the F5 instances (V6).

Fit is enabled by default only if V6 shows fewer observed violations than the current layout and no instance where Fit has an observed violation that the current layout does not.
