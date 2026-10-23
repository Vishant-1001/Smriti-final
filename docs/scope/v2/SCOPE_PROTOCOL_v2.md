# SCOPE benchmark protocol **v2** (revised; committed before any v2 tuning or testing)

**This does not replace v1.** `docs/scope/SCOPE_PROTOCOL.md` and `docs/scope/SCOPE_BENCHMARK_RESULTS.md` (v1, verdict: NOT met) are kept unchanged. `./gradlew :scope:reproduceScopeV1` regenerates v1 into `scope/build/` for an integrity diff.

**Status:** experimental and synthetic only. SCOPE stays disabled by default and outside the app. Nothing here says anything about human preference or clinical benefit.

## Why a v2 exists

`DIAGNOSIS.md` (development seeds 1–40 only) shows that the v1 proposal rule was **structurally almost unreachable**:

- **It gated on the wrong quantity.** It used the pairwise *predictive choice* probability `sigmoid(m/√(1+πv/8))`, which includes the person's response noise. Even with zero parameter uncertainty this is capped at `sigmoid(m)`, which reached 0.70 in about 1% of contexts.
- **It compared against the wrong variant.** The comparison was best vs *runner-up*, a near-neighbour (median posterior gap 0.16), instead of best vs the *current manual/accepted* variant.

v2 changes **only** the decision rule. The model, likelihood, priors, catalogue, synthetic profiles and metrics code are unchanged, apart from added metrics.

## Three distinct quantities (and which v1 used)

| Quantity | Formula | Meaning |
|---|---|---|
| **Parameter uncertainty** | posterior covariance Σ; for a pair, v = ΔφᵀΣΔφ | How unsure the model is about the person's weights |
| **Pairwise predictive probability** | sigmoid(m/√(1+πv/8)) | Chance the person would pick a over b in one noisy choice. **Used by the v1 gate (best vs runner-up)** |
| **P(candidate better than the current/manual variant)** | Φ((m − margin)/√v), with m = E[u(a) − u(manual)] | Posterior probability that switching is an improvement. **Used by the v2 rule** |

## v2 decision rule (SCOPE)

Propose the posterior-mean best variant a* only if all three hold:
1. a* ≠ the current accepted/manual variant;
2. m(a*, manual) ≥ margin;
3. Φ((m − margin)/√v) ≥ τ.

Otherwise abstain and keep the manual variant. Proposals are research outputs only and are never applied automatically.

## Calibrated rule baselines (fair treatment)

B1 and B1F get the same development calibration budget:
- **Probability scale k** for their pairwise mapping, chosen to minimise pooled development held-out CE.
- **Proposal margin w**: propose the top-scoring variant only if its score beats the manual variant's score by ≥ w.

## Models compared (identical feedback streams and budgets)

| Model | Parameters |
|---|---|
| B2 non-contextual | 4 (β) |
| **B3-lite** (new, simpler) | 8 (β + Solo/Together interaction, no activity residuals) |
| B3 full | 24 (adds activity residuals) |

The B0 fixed/manual baseline is reported throughout. Passive shared query streams only (the same random pairs for everyone), with budget 32 and a primary checkpoint of 16, exactly as in v1.

## Development tuning (seeds 1–40; held-out seeds never used)

| What | Grid |
|---|---|
| SCOPE models | {B2, B3-lite, B3} × τ ∈ {0.80, 0.85, 0.90, 0.95, 0.975} × margin ∈ {0, 0.25, 0.5} |
| Rules | {B1, B1F} × k ∈ {0.25, 0.5, 1.0, 1.5} (min dev CE) × w ∈ {0.5, 1, 2, 3} |

**Selection objective,** identical for every family and pooled over **all ten profiles** at budget 16: maximise the mean development utility gain over manual, subject to a pooled harmful-proposal rate ≤ 5% of proposals. A harmful proposal is one that is truly worse than manual, which the simulated person would override. If no setting is feasible, take the most conservative setting.

**Primary SCOPE model for the gate:** the family (B2, B3-lite or B3) with the best development objective. All three are reported on held-out data.

The frozen choices are written to `scope/src/main/resources/scope_v2_frozen.properties` and committed **before** the held-out run.

## Fresh held-out test

Seeds 2001–2060: 60 synthetic people per profile, profiles P1–P10. These seeds were never used by v1 or by v2 development.

## Metrics (per profile, budget 16)

- **Decision quality:** utility gain over manual (normalised), regret, proposal coverage (share of contexts with a proposal), abstention, harmful-proposal (override) rate.
- **Prediction quality:** held-out pairwise CE (log loss), held-out Brier score, calibration (bucket table, ECE).
- **Burden and stability:**
  - questions asked and skips, which are identical by design in passive streams;
  - switching instability.
- **Statistics:** paired bootstrap 95% CIs over held-out users (2000 resamples, seed 99).

## Pre-registered v2 go / no-go

GO only if **all** of the following hold for the primary SCOPE model:

| Gate | Requirement |
|---|---|
| **G1 safety** | Pooled harmful-proposal rate ≤ 5% of proposals, and ≤ 10% on every individual profile |
| **G2 practical coverage** | Proposal coverage ≥ 25% of contexts on P2, and ≥ 15% pooled |
| **G3 performance on P2, against BOTH calibrated B1 and B1F** | (a) utility gain higher, with the bootstrap CI of the difference entirely > 0; (b) normalised regret ≥ 10% lower; (c) held-out CE ≥ 5% better, with CI lower bound > 0 |
| **G4 no significant loss** | On P1, P8 and P9, the utility-gain difference vs B1 and vs B1F must not have a CI entirely below 0 |
| **G5 stability** | Switching rate on P2 ≤ that of B1 and B1F + 0.05 |

**If any gate fails, the verdict is NO-GO.** The thresholds above will not be changed after seeing held-out results. SCOPE stays disabled by default either way.
