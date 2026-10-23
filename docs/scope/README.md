# SCOPE v2.1: experimental preference-learning engine

> **Update (protocol v2):** see [`v2/README.md`](v2/README.md). The v1 abstention was diagnosed as a structurally unreachable gate: it used a predictive choice probability, best vs runner-up. A corrected rule (posterior P(better than manual)) and a simpler 8-parameter model were tested on fresh held-out synthetic users. **v2 verdict: NO-GO** (safety under noisy feedback, coverage and stability gates failed). The v1 results below are unchanged.

**Status: experimental research code. Disabled by default (`ScopeFlags.ENABLED_BY_DEFAULT = false`). The SMRITI app does not depend on it, so gameplay is unchanged. Not scientifically validated, not clinically beneficial, and not shown to beat a rule-based baseline.**

Design basis: [`docs/spec/SCOPE_V2_RESEARCH_ANNEX_v2.1.md`](../spec/SCOPE_V2_RESEARCH_ANNEX_v2.1.md). Frozen protocol: [`SCOPE_PROTOCOL.md`](SCOPE_PROTOCOL.md). Generated results: [`SCOPE_BENCHMARK_RESULTS.md`](SCOPE_BENCHMARK_RESULTS.md).

## 1. Audit: did the app already learn preferences? (Gate S0)

**No.** Commit `901de84` was inspected before any change. Nothing in the app estimates, updates or adapts to preferences:

| What exists | Kind | Where |
|---|---|---|
| Hint levels (show list, say name, where to look, point, place for me) | **Hard-coded guidance**: a fixed list the person picks from; nothing is counted or learned | `domain/Shopping.kt` `enum class HintLevel`; `ui/GameViewModel.kt` `fun hint(level, variant)` |
| "Show me" in cooking, sewing and payment | Hard-coded: highlights the next correct target | `GameViewModel.cookShowMe`, `showMe`; `domain/Kitchen.kt` `CookStep.tool` |
| Guided vs free dishwashing | **Ordinary saved setting**, toggled by the person | `domain/Kitchen.kt` `DishwashState.guided`, `withGuided`; `GameViewModel.dishesGuided` |
| Language, text size, voice/music volume, reduced motion, show names | Ordinary saved settings | `domain/Settings.kt` `data class Settings`; `GameViewModel.updateSettings`; persisted via `data/SaveCodec.kt` |
| Help line per scene | Hard-coded mapping | `domain/Dialogue.kt` `GuideLines.helpFor` |
| Saved progress (nav, shopping, cooking, dishes, sewing, trip) | State persistence, not learning | `domain/SaveData.kt` (schema v2) |

A search of `app/src` for prefer, learn, adapt, personalis*, bayes, posterior and weight found only UI layout weights and comments. No adaptive learning existed. SCOPE is new and lives entirely in the separate `:scope` module.

## 2. What was implemented (Gates S1–S3; no UI, Gate S4 not started)

Module **`scope/`**: pure Kotlin on the JVM. It has no Android, Compose or network dependency, and the `:app` module does not depend on it (a test checks this).

| Requirement (annex) | Implementation |
|---|---|
| Versioned schema of approved variants | `VariantSchema` (feature-schema version, content version, activity list, manual defaults), `VariantFeatures` immutable snapshots; `approved` flag |
| Explicit pairwise choices | `ExplicitPreferenceResponse.PreferLeft / PreferRight / BothFine / Skip`. **Only Left/Right update the model.** Skip and both-fine are logged for audit (`UpdateResult.RecordedNoPreference`); no preference is fabricated |
| "Again?" kept separate | `ActivityAcceptability`: its own tally that never touches feature weights |
| Context limited to the chosen activity + explicit Solo/Together | `ContextSnapshot(activityId, socialMode?)`. `null` means unknown and is never inferred. No orientation, screen, device, time, behaviour, emotion or performance fields (tested by reflection) |
| Immutable snapshots / reproducible replay | `PairwisePreferenceEvent` stores both feature snapshots plus content, model and consent versions; `ScopeEngine.replay` rebuilds the posterior bit-for-bit |
| Model | `u(a,c) = (β + s·γ + δ_activity)ᵀx(a)` with 4 features (density, motion, narration, modality; openness deferred). Bradley–Terry likelihood with a Gaussian shrinkage prior (β 1.0, γ 0.5, δ 0.25). **Laplace approximation** (Newton MAP + inverse Hessian), an approximation and not an exact posterior |
| Uncertainty, abstention, fallback | `PosteriorPrediction` (mean, sd, 95% interval); `recommend` returns `Propose` or `Abstain(reason, manual fallback)` for disabled, not opted in, no evidence, uncertain (P(best ≻ runner-up) < 0.70), version mismatch or model failure. Proposals are **never applied automatically** |
| Query selection | `selectComparison`: expected information gain over decision-relevant top-4 pairs, gated by opt-in, never during an activity, ≤ 1 per session, a 14-day cooldown on declined pairs, a permanent "do not ask", and a minimum gain of 0.03 nats |
| Safety | Hard validator (same activity, approved, version-compatible); fail-closed version checks; `resolveAction` makes Help, Skip, Do it for me, Undo, Home and Stop always win; viewport availability is an `AvailabilityResult`, never a preference |
| Reset | `reset(All)` and `reset(Activity)` |
| Baselines | B0 fixed, B1 strong rules, **B1F** rules + feature votes (stronger than the annex requires), B2 non-contextual Bayes, B3 full, B3 no-pooling, and B2/B3 with VOI query selection |
| Simulator | 10 profiles (P1–P10; P11 device invariance is a unit test), 40 held-out users each, a person-level split, frozen seeds, prequential and held-out metrics, and a paired bootstrap |

## 3. Results (synthetic only)

The protocol was committed in `45eac9f` **before** the run. The full tables are in [`SCOPE_BENCHMARK_RESULTS.md`](SCOPE_BENCHMARK_RESULTS.md).

**Pre-registered verdict: NOT met. SCOPE has not demonstrated an advantage over the rule baseline under the frozen gate.**

The gate failed on the regret criterion. At the primary budget (16 comparison opportunities), full SCOPE's frozen abstention rule (P ≥ 0.70) **abstained in 100% of contexts**. Its proposals therefore fell back to the manual default, and its realised regret was no better than B0's (and worse than B1's). Under the protocol, prediction quality alone cannot pass the gate.

**What it did show:**
- **Held-out pairwise log loss.** B3 was better than B1 and B1F in every profile, with paired-bootstrap 95% CIs above zero; on P2, for example, +17.8% [+16.4, +19.2] vs B1. Caveat: B1's fixed probability mapping is poorly calibrated (its CE is worse than always saying 0.5), so pairwise **agreement** is the fairer comparison. On P2 it was 0.767 for B3 vs 0.551 for B1 and 0.577 for B1F.
- **Context.** Where context matters (P2, P4–P7, P9, P10), B3 beat the non-contextual B2 (P2 +14.0% CE).
- **No context effect.** Where there is none (P1, P3, P8), B3 was slightly *worse* than B2 (P1 −2.6%, CI [−3.1, −2.1]), as the annex predicts: the extra parameters cost a little.
- **Active selection.** VOI query selection improved B3 further (P2 CE 0.570 vs 0.590).
- **Calibration.** Approximately calibrated near 0.5 but **under-confident at the extremes**: in the top bucket the mean predicted was 0.83 against a true 0.89.
- **Post-hoc diagnostic** (added after the run, not part of the gate). Proposing the top-ranked variant regardless of abstention would give normalised regret on P2 of 0.158 (B3) and 0.100 (B3-VOI), against 0.361 (B1) and 0.357 (B1F). This suggests the **ranking** is useful and the **proposal threshold** is the bottleneck. A new protocol would need to recalibrate that threshold on dev users and re-run on fresh held-out seeds before any claim.

**Where it failed or tied:**
- no-go on every profile under the gate;
- worse than B2 when there is no context effect;
- low-feedback and noisy profiles show small gains;
- it never proposed anything at budget 16, which is the main practical weakness.

## 4. What is missing

- **Gate S4:** no human-facing UI (comparison prompt, opt-in, "don't ask me", reset). Deliberately left out of the gameplay path.
- **Gate S5:** no end-to-end vertical slice; the curated variants are synthetic grid combinations, not reviewed SMRITI content.
- **Persistence:** the event log is not persisted to disk. The atomic append + cache update the annex asks for is not implemented (`replay` exists for when it is).
- **Device matrix:** the P11 replay is a unit test with no device input at all. It is not an instrumented 360×800 / 640×360 / 800×1280 / 1280×800 run.
- **Calibration:** no formal calibration study beyond the bucket table, no identifiability analysis, and no misspecification analysis beyond P7.
- **Comfort/assistance:** SC/SA models were not implemented (out of scope).
- **Review:** no professional or ethics review. **No human data.**

## 5. Commands

```sh
./gradlew :scope:test               # 23 engine + simulator tests
./gradlew :scope:runScopeBenchmark  # regenerates SCOPE_BENCHMARK_RESULTS.md (about 12 s)
```
