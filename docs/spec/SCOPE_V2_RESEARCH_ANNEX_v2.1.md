# SMRITI / SCOPE v2.1 — Non-Operative Research Annex (Adaptive Devices)

**Status:** NON-OPERATIVE RESEARCH ANNEX to `SMRITI_FINAL_BUILD_SPEC_SCOPE_v1.3_CANONICAL.md`. It is not an implementation spec, does not supersede canonical v1.3 and is not allowed to affect demo/release builds. Any experimental code must be behind `scope.v2.enabled=false` by default and kept off the human-facing app path. This is a proposal, not evidence that code, simulations, a pilot or clinical benefits exist. Thresholds are provisional engineering hypotheses, not established truths.

**v2.1 patch:** This version clarifies that viewport, orientation and Android form factor are rendering/runtime variables, not inferred preference features. It supplements v2.0; it does not change the core preference-learning hypothesis or claim that the model has been implemented or validated.

## 0. The uncomfortable truth

No finite software system can be made literally impossible to reproduce with rules. A deterministic rule engine with enough state, thresholds, lookup tables, and memory can emulate any bounded system's outputs. A learned model can also be distilled into rules after the fact. Therefore, “cannot be replaced by a rule-based algorithm” is not a technically sound absolute requirement.

The defensible objective is:

> **SCOPE must solve a real, specified preference-learning problem that a practical hand-authored policy cannot handle as accurately, with as little feedback, across as many unseen configuration/context combinations, at an acceptable interaction burden. It must demonstrate that advantage against a deliberately strong rule-based baseline.**

If it cannot meet that bar, the honest engineering decision is to ship the deterministic controller and keep SCOPE as a research experiment—not to add complexity just to say “AI.”

## 1. Verdict on the current design

The audited design is a solid safety-conscious starting point, but its current learning core is not yet a convincing reason that a rule-based implementation is inadequate.

1. **SC (comfort) is highly rule-reproducible.** A four-band ordinal model followed by an adjacent-band limit, entropy threshold, minimum-rating count, improvement threshold, and decline cooldown is a useful conservative predictor, but most of the final behaviour is a fixed selector wrapped around a small estimator. A rules policy can approximate much of this.
2. **SA (assistance) is also easily replaced.** A five-level preference estimate and a selector with thresholds/cooldowns is not enough by itself to establish a need for machine learning. Direct requests such as “help me now,” “skip,” and “do it for me” must remain immediate user commands, not latent preferences.
3. **SF as presently specified is too small to carry the AI claim.** A low-dimensional additive logistic preference model over a handful of features, limited to variants in the same activity/band, can be weakly identified with sparse pairwise comparisons. A proposed threshold such as six comparisons across three features is not evidence that the parameter estimates are reliable. The earlier audit explicitly noted that more feedback could be required under plausible feature gaps.
4. **The model is not meaningfully contextual unless context enters the preference equation.** Storing a `Solo/Together` field or time bucket in an event log does not make the algorithm contextual if its utility score does not use those fields to predict different preferences in those contexts.
5. **“Yes, again” does not identify feature trade-offs.** It can express acceptability of an activity, but it cannot tell the system whether someone preferred less motion, shorter narration, larger objects, or a more open interaction. Do not use a weak label as though it identifies all of those parameters.
6. **The data budget is the bottleneck.** With sparse feedback, a complex model can be less reliable than simple explicit preferences. More parameters do not mean more intelligence; without informative observations they mean more uncertainty and overfitting risk.

Keep the safety-conscious SC/SA components if useful, but do not present them as the irreplaceable learning contribution. The proposed central learning contribution is below.

## 2. The new centre of SCOPE: contextual, multi-attribute preference learning

SCOPE should become an **on-device Bayesian Contextual Preference Learner with Active Comparison Selection**. This is a statistical machine-learning system, not an LLM and not a claim to have invented Bayesian preference learning.

The person still chooses the activity. SCOPE must never choose an activity, infer what activity the person ought to do, diagnose ability, or use task performance as a reward. After a person opens an activity, SCOPE may rank a small, curated set of *presentation variants* that have already passed deterministic content and safety checks.

### 2.1 Learn a real latent utility function

Each curated presentation variant `a` has an immutable feature vector `x(a)`. Start with no more than five interpretable dimensions:

- `visual_density`: sparse ↔ visually rich;
- `motion_intensity`: still ↔ more animated;
- `narration_length`: brief ↔ more descriptive;
- `instruction_modality`: primarily visual ↔ audio-supported (categorical encoding, not an invented continuous scale);
- `interaction_openness`: guided sequence ↔ open-ended/free interaction. This feature is **deferred from the v1 canonical model** because it can confound with assistance and complexity; it may be investigated only with a separate operational rubric and identifiability test.

Exact endpoints, encodings, and which dimensions apply to each activity must be defined in a content schema, reviewed, versioned, and covered by tests. Use only curated combinations. These dimensions concern presentation and interaction preference; they are not measures of cognitive ability.

The explicit context `c` should be deliberately small. If future evidence justifies a v2 study, allow only:

- the activity the person has chosen (categorical; enables an activity-specific residual);
- `Solo` or `Together`, but only when the person explicitly selects it or confirms it for that session.

Do **not** infer the presence of a companion, mood, fatigue, confusion, distress, comprehension, or cognitive state. Do not add time-of-day, demographics, diagnosis, or sensor-derived context merely because a contextual model can accept it. New context fields require a design justification and review.

Model the utility of a variant as:

`u(a, c) = (β_global + B · c + δ_activity)^T x(a)`

where:

- `β_global` represents the person’s cross-activity preferences for general presentation features;
- `B · c` represents explicitly contextual changes in the value of those features (for example, a person may prefer shorter narration when solo but more spoken description when together);
- `δ_activity` represents an activity-specific deviation from general preferences.

Use a hierarchical/shrinkage prior so an activity-specific estimate starts near the person’s general presentation preferences and only diverges when explicit evidence supports that. This is *partial pooling*, not a claim that every activity shares the same preferences. It must be possible to turn off cross-activity pooling and test whether it helps. **Only presentation preferences may be shared across activities. Comfort/difficulty and assistance preferences must remain activity-specific and must never transfer between activities.**

To control data hunger, do not freely learn every possible interaction. The initial model should estimate only the main effects plus the single predeclared `Solo/Together × feature` interaction. Activity-specific residuals should be strongly regularised and should remain at the prior until enough data support activity-specific evidence. More interactions are deferred until the data and validation justify them.

### 2.2 Learn from explicit comparative feedback

When a feedback query is appropriate, show two safe, previously approved variants of the *same person-selected activity*. Ask one simple question such as: “Which feels nicer for you?” Provide four clear choices:

- left option;
- right option;
- both are fine / no preference;
- skip / not now.

The UI must present genuinely distinct, accurately labelled variants. It must not intentionally show a poor or unsafe version to force a distinction. Do not require an answer. If both are fine or the person skips, do not invent a preference. `Skip` is missing data, not a negative reward.

For a left/right answer, use a Bayesian pairwise choice likelihood, e.g. a Bradley–Terry/logistic model:

`P(a ≻ b | c, θ) = sigmoid((φ(a,c) − φ(b,c))ᵀ θ)`

where `φ(a,c)` contains the feature main effects and the permitted context interactions. The posterior represents uncertainty over the person’s utility weights, rather than storing only a point score. If a Laplace approximation is used, document it as an approximation and validate its calibration; do not call it an exact posterior.

**Keep feedback types separate:**

- Pairwise “left/right” updates the contextual preference model.
- “Would you like to do this activity again?” updates only a separate activity-acceptability record/model, if retained. It does not update individual feature weights.
- “Help me now,” “skip this,” “do this for me,” undo, home, and stop are immediate control commands. They are not preference labels unless the person separately and explicitly answers a preference question.
- SC comfort labels update only the activity-specific comfort model.
- SA guidance feedback updates only the activity-specific default-guidance model.

### 2.3 Select questions by value of information, not a timer

SCOPE must decide whether a question is worth asking. At each eligible opportunity, estimate the expected information gain from each permissible comparison and whether that information could change a future configuration choice. Choose the comparison with the highest expected information gain only if it passes a burden/utility gate.

A query is not justified just because the model is uncertain: the choice must concern a real, future-useful trade-off. Do not repeatedly ask questions that will not change the next decision. Never prompt during an activity or interrupt a user interaction to gather data.

Initial conservative UX defaults (validate, do not treat as clinical facts): at most one optional comparison prompt per session; do not repeat a declined/snoozed query for at least 14 days; do not show any comparison until the person has opted into preference personalisation; and provide a permanent “Do not ask me these questions” control. If these settings result in too little data to learn reliably, **accept that limitation rather than increasing prompt pressure**.

Relevant prior research supports active, query-efficient preference learning under explicit modelling assumptions; it does not guarantee that such a strategy is useful or low-burden for Smriti’s users. See the research notes in §8.

## 3. A contextual model that can generalise to unseen combinations

The key advantage must be **generalisation across combinations the developer did not enumerate as rules**.

Example of a legitimate learnable pattern: in `Solo` mode, a person’s explicit choices repeatedly favour low-motion variants with brief narration; in `Together` mode, their explicit choices favour audio-supported narration and richer scenes. SCOPE should learn the weights and interaction from those comparisons and rank a new, approved combination using its feature vector. A rule baseline may store each known configuration or hand-code a `Solo/Together` table, but it should not be given a hidden, exhaustive table of every possible feature combination.

This generalisation is a hypothesis, not an automatic advantage. The evaluator must test unseen variants and contexts. If the variants are only six hand-authored combinations and a small set of conditions, rules may be simpler and better. The feature space must be large enough to make generalisation meaningful but small enough to learn from the realistically sparse labels. The correct scope should emerge from evaluation, not from a desire to add more parameters. **Canonical v1.3 currently specifies a smaller non-contextual model only; this five-feature/contextual equation below is exploratory and cannot be implemented as product behaviour without a separately reviewed change.**

### 3.1 Research hypothesis for selection after the person opens an activity

When preference personalisation is enabled:

1. The person selects an activity and confirms/chooses the current session mode if needed.
2. The content validator filters all variants for that activity to approved, compatible configurations.
3. The learned model computes posterior-predictive utility for the remaining variants in that explicit context.
4. In this annex, any top-variant output is a **research prediction/proposal only**. It may not be automatically applied to a human-facing session. The only permitted interaction model for any future promoted version is explicit preview/acceptance, effective no earlier than the next session.
5. If evidence is insufficient, candidates tie, the design is unidentifiable, or any content/model version mismatches, return the manual/fixed choice and abstain.
6. The person must always be able to change the configuration, turn personalisation off, reset learned preferences, or return to the previous manual variant immediately.

The person’s activity choice is never overridden. SCOPE does not create content, change the activity’s meaning, add unreviewed content, or use its recommendation to hide or reorder activities.

### 3.2 Separate presentation personalisation from safety-sensitive adjustments

- **Presentation preferences** are proposals only in this research annex. Automatic application is prohibited. Any future promoted implementation must preserve preview/explicit acceptance and apply at next session start only.
- **Comfort/difficulty band** must never increase automatically based on correctness, speed, repetition, hints, task completion, or estimated ability. Proposals require an explicit acceptance action. No score, failure state, or performance-driven escalation.
- **Default assistance level** must never prevent direct help. Raising or lowering stored default guidance requires explicit confirmation under the approved selector. Direct “help now” and “do it for me” commands override the model immediately.
- **Activity choice and sequencing** remain person-controlled. SCOPE is not an activity recommender in v2.

A learned model must not receive more authority than its evidence supports. The objective is preference-fit, not clinical improvement, persistence, completion, or engagement maximisation.

### 3.3 Device form factor and viewport are not preference context

Smriti must support portrait and landscape on Android phones, tablets and foldables and adapt to supported resizable/multi-window windows. The responsive UI/world renderer decides how a given approved experience is laid out. **SCOPE must not interpret screen orientation, raw pixel dimensions, density, aspect ratio, window-size class, device model, foldable posture or input hardware as evidence of what the person prefers.** These are presentation/runtime metadata, not preference labels.

The model's context remains limited to person-selected activity and an explicitly selected/confirmed Solo/Together mode in v2. A viewport change must not create a new learning context, trigger a preference query or update any posterior. Identical eligible observations and identical model versions must produce identical model updates regardless of orientation or device viewport.

If a narrow viewport makes a candidate variant genuinely unavailable or inaccessible, the deterministic content validator may remove that variant from the eligible candidate set. This is a candidate-availability constraint, not a preference update. Do not translate “variant not rendered on this screen” into dislike. Variant feature vectors must describe the semantic presentation choices, not incidental pixel geometry. If responsive layout changes the semantic amount/type of content, version and annotate that content/configuration explicitly and validate that the comparison remains meaningful.

Required cross-device tests:
- replay identical eligible event fixtures at 360×800 dp portrait, 640×360 dp landscape, 800×1280 dp tablet portrait and 1280×800 dp tablet landscape; all model posteriors and eligibility outcomes must match within the reference implementation's declared deterministic tolerance;
- orientation/resize events alone produce zero SCOPE events and zero model updates;
- viewport-dependent variant exclusion produces an explicit availability result, not a negative preference label;
- user configuration, consent, model version, and event replay remain coherent after rotate, fold/unfold, background/foreground and process recreation.

## 4. Safety, uncertainty and cold start

SCOPE is allowed to say “I don’t know yet.” It must not disguise uncertainty by selecting a random option for exploration.

- Start with a documented prior; record its version and provenance. No unconsented cross-user training or cloud upload.
- The cold-start configuration is fixed/manual and fully functional without any learned model.
- If posterior uncertainty is high or predicted utility differences are small relative to uncertainty, abstain and use the last accepted/manual variant.
- Novelty/exploration occurs only through the optional comparison interaction, using approved variants. Never explore by silently changing a person’s configuration during an activity.
- The person may correct a preference through a new explicit choice. A correction changes future suggestions immediately after the update is committed; it must not be inferred from a back/exit gesture.
- Provide `Reset learned presentation preferences` and `Disable personalisation`. Reset must remove posterior/cache state for the selected profile and leave personal memories untouched.
- Do not treat contradictory answers as error. Preserve the event history and uncertainty; a person’s preferences can be context-dependent or change over time.
- Do not silently decay or discard old preferences in v2. If preference drift is later modelled, it must be an explicit, tested hypothesis, not inferred disease progression. In the meantime, latest explicit choices, manual overrides, and reset controls provide user control.
- If data schema, content version, feature manifest, or model version mismatches, fail closed to fixed/manual mode; never reinterpret old feature vectors using new live definitions.

## 5. Strong rule-based baseline: make SCOPE earn its place

Do not compare SCOPE only to a static default or intentionally weak baseline. Implement and freeze all of the following before definitive evaluation:

**B0 — Fixed/manual:** never learns; the person controls every setting.

**B1 — Strong rule-based controller:** uses the same eligible explicit feedback, last accepted choices, per-context majority/recency-weighted counts, deterministic tie-breaking, explicit user locks, adjacent-change limits, cooldowns, and the strongest sensible rules-based mapping over the same approved feature dimensions. It can distinguish `Solo` and `Together` using a hand-written table, but may not fit a statistical preference function or generalise from feature weights to unseen combinations.

**B2 — Non-contextual Bayesian preference ablation:** same pairwise likelihood and priors but omits context interactions and activity residuals. This isolates whether context/partial pooling adds value.

**B3 — Full SCOPE:** hierarchical Bayesian contextual preference model plus value-of-information query selection and the same hard safety validator.

All controllers receive the same event streams, approved variant catalogues, query budget, consent state, and deterministic safety rules. The model cannot win by asking more questions or accessing more information. Screen orientation, device class and viewport geometry are held out of the preference feature/context vectors; cross-device invariance is tested separately.

### 5.1 Required simulation profiles

Freeze the profiles and random seeds before running the final experiment. At minimum include:

1. stable, additive preferences (where simple rules may be just as good);
2. context-dependent trade-offs (the case where SCOPE should have an advantage);
3. activity-specific preferences with partial cross-activity similarity;
4. low-feedback and high-skip users;
5. inconsistent/noisy feedback;
6. preference change over time without any disease interpretation;
7. misspecified/non-additive preferences;
8. no real context effect (to detect harmful overfitting);
9. sparse features and variants with only partial metadata;
10. cold start and content/model version changes;
11. orientation changes, fold/unfold, resized windows, device-width changes and viewport-dependent candidate availability, including a negative-control test where preferences are identical across devices.

Do not tune thresholds on the final test profiles. Split by synthetic person/seed, not by individual events from the same simulated person, to avoid leakage. Add property tests that every model/controller obeys the same safety invariants.

### 5.2 Metrics and pre-registered go/no-go

Evaluate more than a single aggregate score:

- prequential held-out pairwise log loss and Brier score (prediction must be logged before observing each answer);
- choice agreement and simulated regret on future choices/held-out approved variants;
- number of explicit comparisons needed before the learned policy becomes better than B1;
- uncertainty calibration and abstention rate;
- prompt count, skip rate, cooldown violations, override/undo rate, and configuration-switch instability;
- separate results by profile, especially low-feedback, misspecified, and no-context-effect profiles;
- zero consent, privacy, or safety-invariant violations.

**Provisional go gate to pre-register:** full SCOPE must improve held-out pairwise log loss by at least 5% relative to B1 with a paired-bootstrap 95% confidence interval whose lower bound is above zero; show a separately predeclared practically meaningful reduction in simulated preference regret at the same query budget; and remain non-inferior to B1 for prompt burden and switching/oscillation across every safety-critical stress profile. It must not create invariant violations. The 5% figure is a proposed engineering threshold, not a literature-established boundary; adjust it before the definitive run if power analysis or baseline variance justifies a different threshold.

Also report where SCOPE loses. It is expected to tie or lose when preferences are stable, data are too sparse, or the context has no effect. If it loses broadly, if confidence intervals overlap zero, or if gains require too many questions, **do not claim that it beats rules and do not enable it by default**.

Simulation proves only behaviour under those synthetic assumptions. It is not evidence of benefit for people living with dementia. Any human study involving people with dementia requires the appropriate institutional ethics review, consent/assent and dissent procedures, specialist oversight, accessible materials, and a prewritten harm/stop protocol. Until then, do not claim patient benefit or deploy the model in a clinical decision role.

## 6. Reproducibility and implementation contracts

If this research annex is later authorised for an implementation experiment, model logic must live in a pure Kotlin module independent of Compose and libGDX. The Android application and JVM simulator must call the same implementation—not separate copies. Until formal promotion through a numbered change to canonical v1.3, this annex remains research documentation and has no product runtime effect.

Recommended domain types:

```kotlin
 data class ContextSnapshot(
     val activityId: ActivityId,
     val socialMode: SocialMode? // null means unknown; never infer
     // Deliberately excludes orientation, screen size, density, and device type.
 )

 data class VariantFeatures(
     val variantId: VariantId,
     val contentVersion: String,
     val featureSchemaVersion: String,
     val visualDensity: Float?,
     val motionIntensity: Float?,
     val narrationLength: Float?,
     val instructionModality: InstructionModality?,
     val interactionOpenness: Float?
 )

 sealed interface ExplicitPreferenceResponse {
     data object PreferLeft : ExplicitPreferenceResponse
     data object PreferRight : ExplicitPreferenceResponse
     data object BothFine : ExplicitPreferenceResponse
     data object Skip : ExplicitPreferenceResponse
 }

 data class PairwisePreferenceEvent(
     val eventId: String,
     val profileId: String,
     val sessionId: String,
     val activityId: ActivityId,
     val context: ContextSnapshot,
     val leftVariantId: VariantId,
     val rightVariantId: VariantId,
     val leftFeaturesSnapshot: VariantFeatures,
     val rightFeaturesSnapshot: VariantFeatures,
     val response: ExplicitPreferenceResponse,
     val consentVersion: String,
     val contentVersion: String,
     val modelVersion: String,
     val createdAtEpochMs: Long
 )
```

Adapt types to the actual codebase, but preserve the contracts. The event stores immutable feature vectors and schema/content hashes; replay must never depend on today’s mutable content catalogue. A `Skip` or `BothFine` event can be stored for UX audit, but must not be converted to a left/right label. Store event append and derived cache update atomically. Rebuild derived state from the event log and verify determinism. Personal data and the preference profile remain local; no network permission is needed for the learning algorithm.

Required model API surface:

```kotlin
interface ContextualPreferenceModel {
    fun predict(
        context: ContextSnapshot,
        candidates: List<VariantFeatures>
    ): List<PosteriorPrediction>

    fun selectComparison(
        context: ContextSnapshot,
        candidates: List<VariantFeatures>,
        burdenState: QueryBurdenState
    ): ComparisonDecision // Ask(pair) or DoNotAsk(reason)

    fun update(event: PairwisePreferenceEvent): UpdateResult
    fun reset(scope: PreferenceResetScope)
    fun diagnostics(): ModelDiagnostics
}
```

`PosteriorPrediction` must expose at least the posterior expected utility, uncertainty/credible interval or calibrated predictive comparison probabilities, and a reason when it abstains. Diagnostics must be available in a debug build without exposing personal preference details to third parties. The UI must never show scientific uncertainty graphs to the end user; use accessible language and simple controls.

Hard validator requirements:

- candidates must belong to the activity the person chose;
- variants must be enabled, curated, version-compatible, and within the person’s allowed presentation settings;
- no unreviewed generated content;
- no automatic activity/difficulty/assistance changes through the feature model;
- direct help, skip, undo, and stop actions always dominate recommendations;
- no update from tap speed, success/failure, completion, pauses, hint use, exits, or inferred emotion/ability;
- cross-activity transfer is limited to approved presentation-feature weights; no transfer of comfort/difficulty or assistance;
- any validator/model failure returns fixed/manual behaviour;
- all safety invariants tested against every controller and every model ablation.

## 7. What not to do

- Do not add an LLM, neural network, embeddings, or cloud service merely to make SCOPE sound like more advanced AI. With sparse feedback and offline constraints, these may increase fragility without increasing information.
- Do not claim Bayesian models are inherently better than rules.
- Do not call a fixed list of threshold rules a “contextual bandit.”
- Do not ask more questions to manufacture statistical significance.
- Do not turn successful completion, slow play, help requests, or stopping into hidden preference or cognitive-state labels.
- Do not personalise the difficulty of the game using a proxy for dementia severity.
- Do not claim the feature learner is novel just because it combines known techniques.
- Do not build every activity before validating whether context-aware generalisation works on a tiny, honest set of configurations.

## 8. Research and prior-art notes (checked via Firecrawl research search/read)

These sources establish that important ingredients already exist. They do **not** establish that SCOPE works for Smriti or that it is clinically beneficial.

1. **Sekhari et al., “Contextual Bandits and Imitation Learning via Preference-Based Active Queries.”** The work studies noisy pairwise expert preference feedback and algorithms that balance decision regret with the number of feedback queries, under explicit function-class and regularity assumptions. This supports investigating query-efficient preference learning; its theoretical guarantees do not transfer automatically to Smriti’s population, UI, or feedback process. https://arxiv.org/abs/2307.12926
2. **“Comparison-based Active Preference Learning for Multi-dimensional Personalization” (AMPLe).** The paper uses Bayesian inference and active comparison selection to infer multi-dimensional preferences with fewer user queries, evaluated on language-generation personalization tasks. It is relevant prior art, not evidence of effectiveness for dementia-related activity software. https://arxiv.org/abs/2411.00524
3. **“Contextual bandits with entropy-based human feedback.”** This preprint studies soliciting feedback when the learner’s policy entropy exceeds a threshold and reports experiments in its own setup. It supports uncertainty-aware feedback solicitation as a research direction; an expert-feedback setup and simulated/benchmark results are not proof of acceptable burden or benefit for Smriti users. https://arxiv.org/abs/2502.08759
4. **COACH prompting system.** COACH used AI-guided prompts to support older adults with dementia during handwashing and was evaluated as an assistance system. It is important prior art against claims that AI-guided assistance for people with dementia is itself new. Its sensing, task, and study context differ from Smriti’s offline creative/activity companion. https://pmc.ncbi.nlm.nih.gov/articles/PMC2588599/
5. **“A Planning System Based on Markov Decision Processes to Guide People with Dementia through Activities of Daily Living.”** This earlier work used MDPs to reason about when/how to prompt during handwashing. It further cautions against claiming novelty for adaptive prompting in dementia simply by using a probabilistic model. https://pubmed.ncbi.nlm.nih.gov/16617621/

### Correct novelty statement

Until a specific prior-art review and successful benchmark are completed, use wording like:

> “SCOPE is a proposed offline, consent-gated contextual preference-learning engine for choosing among curator-approved activity presentation variants. It combines Bayesian preference estimation with selective comparison queries, explicit user control, and deterministic safety constraints. Its value over a strong rules-based controller remains a testable hypothesis.”

Do not write “cannot be replaced by rules,” “first-of-its-kind,” “novel AI for dementia,” or “proven to improve wellbeing.”

## 9. Research order and promotion gates

**Gate S0 — Repository truth:** inspect current code and working tree; identify what already exists; run baseline tests; never claim that planned code is present.

**Gate S1 — Pure model reference implementation:** implement feature metadata/schema validation, pairwise likelihood, posterior update, active query score, abstention and event replay in pure Kotlin. Use executable numeric fixtures and property tests.

**Gate S2 — Strong rule baseline and simulator:** implement B0/B1/B2/B3 against the identical API and synthetic person generator; freeze profiles, metrics, seeds, and thresholds in a committed protocol before the definitive run.

**Gate S3 — Evidence gate:** evaluate full SCOPE against B1 and B2. Publish per-profile results, failure cases, query burden, confidence intervals and all invariant checks. No cherry-picked seeds.

**Gate S4 — UX and consent:** build the optional pairwise preference prompt, no-preference/skip controls, personalisation toggle, immediate override, reset and accessibility checks. No interruption during activity.

**Gate S5 — End-to-end vertical slice:** use one real, curated activity with several genuinely different approved variants. Demonstrate online learning from explicit comparisons, prediction on a held-out variant, persisted replay after process death, airplane-mode operation, and deterministic fallback.

**Gate S6 — Research/field testing:** only after the appropriate ethics and expert reviews. Simulation is not user validation, and a user pilot is not evidence of clinical efficacy.

If a gate fails, halt feature expansion and fix or disable the feature. Do not bury a failed learning result under more screens or better art.

## 10. Final decision

SCOPE’s best chance of being genuinely worth keeping is not “more algorithms.” It is a narrow, interpretable model that learns person-specific trade-offs among multiple safe presentation features, conditions those trade-offs on a very small amount of explicit context, actively asks only high-value comparisons, generalises to approved unseen combinations, represents its uncertainty, and abstains when evidence is weak.

The model must face a strong baseline that uses the exact same explicit data and user controls. If it cannot beat that baseline with a useful margin at the same feedback burden, the research answer is that rules are better for this product. That is not failure; refusing to ship a needless model is evidence of engineering discipline.


## 11. Non-operative status and promotion criteria

This annex conflicts with some earlier v2 design language about automatic presentation adaptation and the size of the feature/context model. The canonical source of truth is `SMRITI_FINAL_BUILD_SPEC_SCOPE_v1.3_CANONICAL.md`. When they differ, v1.3 wins. In particular:

- no automatic application of a learned variant;
- no active contextual model or activity-specific residual hierarchy in the canonical app;
- no context-driven query or presentation changes based on device orientation/window dimensions;
- no user-facing SCOPE v2 flag enabled by default;
- no learning from success, failure, speed, hint use, stopping, skipping, or inferred state.

A request to promote v2 must include: (1) a source-control change and numbered architecture/spec decision; (2) a mathematical identifiability analysis and executable fixtures; (3) explicit prior/hyperprior, feature schemas and immutable event contracts; (4) a strong rule baseline and non-contextual model comparator; (5) equal prompt-budget testing and held-out performance results; (6) calibration and model-misspecification analysis; (7) privacy/accessibility and relevant professional review; (8) proof that the gain is practically meaningful under realistic feedback sparsity. Even after promotion it cannot make clinical-benefit claims without suitable human evidence.
