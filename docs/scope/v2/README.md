# SCOPE v2: diagnosis, corrected decision rule, fresh held-out benchmark

**Status: experimental and synthetic only. Disabled by default and not in the app. Verdict: NO-GO under the pre-registered v2 criteria. No claim about human preference, dementia benefit or clinical validity.**

The v1 protocol, results and NOT-MET verdict are preserved unchanged in `docs/scope/`. `./gradlew :scope:reproduceScopeV1` regenerates them into `scope/build/v1-reproduction/`; the output is byte-identical apart from the runtime line.

## 1. Diagnosis: why v1 abstained 100% of the time

The gate is `ScopeEngine.recommendUnchecked` (v1 rule `DecisionRule.TopTwoPredictive`). It calls `LaplacePosterior.probPrefer(best, runnerUp)` and proposes only if the result is ≥ 0.70.

Measured on development seeds 1–40 only (`DIAGNOSIS.md`, `./gradlew :scope:runScopeDiagnosis`):

1. **Wrong quantity.** `probPrefer` returns sigmoid(m/√(1+πv/8)), the probability that the person would *pick* best over runner-up in one noisy choice. It includes the person's own response noise, so even with zero parameter uncertainty it cannot exceed sigmoid(m). That cap reached 0.70 in **1.3%** of contexts, and the gate value itself in **0.5%**.
2. **Wrong comparison.** The runner-up among 12 grid variants is usually a near-neighbour: the median posterior gap m is 0.16.
3. **High parameter uncertainty.** The posterior sd of a difference is about 0.8 after 16 questions with 24 parameters.

## 2. Three quantities, and what each gate uses

| Quantity | Code | v1 gate | v2 gate |
|---|---|---|---|
| Parameter uncertainty (Σ, v = ΔφᵀΣΔφ) | `LaplacePosterior.covariance`, `difference()` | indirectly | indirectly |
| Pairwise predictive choice probability | `probPrefer()` | **yes**, best vs runner-up | no |
| P(candidate better than the accepted/manual variant by a margin) = Φ((m − margin)/√v) | `probBetter()` (new) | no | **yes** |

## 3. Minimal correction

- **New rule.** `DecisionRule.VersusManual(tau, margin)` in `BayesianPreferenceModel.kt`, applied by `ScopeEngine.recommendWith` against the current accepted/manual variant (`accepted` parameter). It abstains with `ACCEPTED_IS_BEST` or `UNCERTAIN`, and proposals are never applied automatically. v1 remains the default rule.
- **Simpler model.** `ModelConfig.LITE` ("B3-lite"): β plus the Solo/Together interaction, no activity residuals (8 instead of 24 parameters).
- **Fair rule baselines.** Calibratable probability scale and a vs-manual proposal margin (`RuleController`).
- **New metrics.** Utility gain over manual, coverage, harmful proposals (= simulated overrides), held-out Brier, ECE, skips.
- **Tests.** `DecisionRuleV2Test` (8 tests).

## 4. Method

**Protocol** (`SCOPE_PROTOCOL_v2.md`, commit `fa6929e`): committed before any tuning.

**Development tuning** on seeds 1–40 only (`TUNING_DEV.md`), with the policy frozen in commit `3711640` before the test:
- **Primary SCOPE model: B3-lite**, τ = 0.85, margin = 0.25.
- **Calibrated rules:** no rule setting met the ≤ 5% harm limit with useful coverage. B1 (w = 3) never proposes; B1F took its most conservative setting.
- **B1F-maxgain:** reported for context only (ungated).

**Fresh held-out test:** seeds 2001–2060, 60 synthetic people per profile, with identical passive feedback streams for every method (16 opportunities; questions asked = 16 for all, skips identical per profile).

## 5. Held-out results (`SCOPE_BENCHMARK_RESULTS_v2.md`)

**P2 (context trade-off) at budget 16:**

| Controller | Utility gain | Regret | Coverage | Harmful/proposals | CE | Brier | Switch |
|---|---|---|---|---|---|---|---|
| B1-cal | 0.000 | 0.499 | 0.000 | 0/0 | 0.696 | 0.251 | 0.000 |
| B1F-cal | 0.017 | 0.482 | 0.040 | 3/19 (16%) | 0.686 | 0.247 | 0.283 |
| B1F-maxgain (ungated) | 0.107 | 0.392 | 0.617 | 97/296 (33%) | 0.686 | 0.247 | 0.699 |
| B3-v1rule | 0.000 | 0.499 | 0.002 | 0/1 | 0.596 | 0.204 | 0.000 |
| B3-v2 (24 params) | 0.082 | 0.416 | 0.146 | 4/70 (6%) | 0.596 | 0.204 | 0.080 |
| **B3-lite-v2** | **0.109** | **0.390** | **0.190** | 3/91 (3%) | **0.591** | **0.202** | 0.104 |

- **Against the calibrated rules:** B3-lite-v2 minus B1-cal gives a utility gain of +0.109 (95% CI [0.082, 0.136]), a 21.9% regret reduction, and +15.0% CE (CI [+13.4%, +16.5%]). Against B1F-cal: +0.092 [0.067, 0.118], 19.1% regret reduction, +13.9% CE.
- **Against the aggressive rule:** B3-lite matches the gain of B1F-maxgain with about a tenth of its harmful-proposal rate and far less switching.
- **Simpler beats full here.** B3-lite beat full B3 on P2: gain +0.027 [0.014, 0.042], CE +0.8%. This was not assumed in advance.

**Per profile (B3-lite-v2):**

| Profile | Coverage | Harmful rate | Gain |
|---|---|---|---|
| P1 stable-additive | 0.304 | 0.007 | 0.188 |
| P2 context-tradeoff | 0.190 | 0.033 | 0.109 |
| P3 activity-specific | 0.223 | 0.065 | 0.115 |
| P4 low-feedback/high-skip | 0.021 | 0.000 | 0.014 |
| P5 noisy | 0.115 | **0.164** | 0.047 |
| P6 drift | 0.194 | 0.043 | 0.112 |
| P7 misspecified | 0.158 | 0.013 | 0.101 |
| P8 no-context-weak | 0.085 | 0.098 | 0.041 |
| P9 partial-metadata | 0.117 | 0.071 | 0.062 |
| P10 version-change | 0.165 | 0.025 | 0.097 |

**Calibration** (ECE, all profiles): B3-lite 0.035 vs B1-cal 0.029 and B1F-cal 0.024. B3-lite is somewhat **under-confident at the extremes** (predicted 0.83 vs true 0.90) and slightly over-confident at 0.17 vs 0.11.

## 6. Pre-registered v2 go/no-go: NO-GO

| Gate | Result | Detail |
|---|---|---|
| G1 safety | **FAIL** | Pooled harmful rate 0.046 is within the 0.05 limit, but P5 (noisy) reached 0.164, above the 0.10 per-profile limit |
| G2 practical coverage | **FAIL** | P2 coverage 0.190 < 0.25; pooled 0.157 ≥ 0.15 |
| G3 P2 vs B1-cal and B1F-cal | pass | Gain CI > 0, regret ≥ 10% lower, CE ≥ 5% better with CI > 0 |
| G4 no significant loss on P1/P8/P9 | pass | — |
| G5 stability on P2 | **FAIL** | Switch rate 0.104 vs 0.000 for B1-cal |

On G5: calibrated B1 never proposes, so any policy that ever proposes fails this comparison. This is a weakness of the gate's design. It is reported here, and the gate was **not** changed after seeing the results.

**Conclusion.** The corrected rule fixes the structural defect: SCOPE now makes proposals, and they clearly beat the calibrated rule baselines on the context profile. However:
- the proposal volume is below the predeclared practical threshold;
- harm under noisy feedback exceeds the safety limit;
- the stability criterion failed.

SCOPE has therefore **not** met the bar to be called useful. It stays disabled and outside the app, and no UI was integrated.

## 7. What a v3 would need (not done)

- Write a new protocol **before** any further runs. Do not reinterpret v2.
- Make proposal confidence robust to noisy or inconsistent feedback, for example by modelling response noise. The P5 harm is the main safety failure.
- Redesign G5 to measure oscillation, such as A→B→A reversals, rather than comparing against a baseline that never proposes.
- Decide whether 25% coverage at 16 questions is achievable without more questions. The annex forbids raising prompt pressure to get there.
- Run everything on fresh seeds again.

## Commands

```sh
./gradlew :scope:test                 # 31 tests
./gradlew :scope:runScopeDiagnosis    # docs/scope/v2/DIAGNOSIS.md (dev seeds)
./gradlew :scope:runScopeV2Tuning     # dev tuning → frozen properties + TUNING_DEV.md
./gradlew :scope:runScopeV2Test       # fresh held-out → SCOPE_BENCHMARK_RESULTS_v2.md
./gradlew :scope:reproduceScopeV1     # v1 integrity check (writes to build/)
```
