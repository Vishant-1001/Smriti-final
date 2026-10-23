# SMRITI flagship technology: SCOPE go/no-go and alternative research

Status: research report, written 2026-10-24. It is based on SMRITI's own synthetic benchmarks and on the literature listed below. No human study has been run, and no clinical benefit is claimed anywhere in this document.

---

## Phase 1: SCOPE evidence (inspected, not re-run)

| Question | Answer | Evidence |
|---|---|---|
| Did v1 pass its frozen gate? | **No: criterion NOT met.** At budget 16 it abstained in 100% of contexts | `docs/scope/SCOPE_BENCHMARK_RESULTS.md`, protocol `45eac9f` |
| Did v2 pass? | **No: NO-GO.** Gates G1 (P5 noisy-feedback harmful rate 0.164 > 0.10), G2 (P2 coverage 0.190 < 0.25) and G5 (stability) failed; G3 and G4 passed | `docs/scope/v2/SCOPE_BENCHMARK_RESULTS_v2.md`, `f92dc44` |
| Was v2 tuned on held-out data? | **No.** Tuning used seeds 1–40 only. The policy was frozen in `3711640` before the test on seeds 2001–2060 | `docs/scope/v2/TUNING_DEV.md`, `scope_v2_frozen.properties` |
| Is there a v3? | **No.** There is no protocol, code or result for a v3 | `git log`, `docs/scope/` tree |
| Which seeds are still fresh? | Seeds 1001–1040 and 2001–2060 have been reported, so they are no longer held out. Seeds ≥ 3001 have never been used | — |

**Verdict: SCOPE fails.** It is frozen as research code: it stays disabled, is not a dependency of `:app`, and all of its protocols and failed results are kept unchanged.

The failure analysis motivates the alternative:
1. **Noisy explicit feedback causes wrong proposals.** Inconsistent answers are exactly the case we should expect in this population.
2. **Low coverage at an acceptable question burden.** Learning needs many pairwise questions, and the annex forbids increasing prompt pressure to get them.
3. **The benefit can only be shown in simulation, against simulated people whose utilities we invented.**

So the alternative should need **no learning from behaviour, and no questions beyond the explicit settings people already make**. Its correctness should be checkable against something real, not against simulated preferences.

---

## Phase 2: Five materially different alternatives

Labels used below: **[Published]** means a result reported by the cited source. **[Inference]** means our own reasoning, not tested.

### A. Constraint-based presentation from explicit needs (an ability-based design solver)

1. **User problem.** SMRITI already has independent settings (text size ×1.0/1.2/1.4 on top of the system font scale, reduced motion, show names, language) and many screen sizes. Every screen currently resolves these with hand-written width thresholds: `maxWidth < 600.dp`, `< 380.dp`, `landscape && < 900.dp`, `< 420`, `maxHeight < 520.dp` and `< 500.dp` (see `SmritiRoot.kt`, `RadioScreen.kt`, `MemoryBookScreen.kt`, `TravelScreens.kt`). HUD labels also deliberately *ignore* the in-app text size so they fit (`Components.kt` `HudButton`). A person who sets "Larger text" therefore does not get it on the always-visible controls. Combinations such as Tamil with large text on a narrow phone have needed repeated hand fixes (two-row HUD, stacked buttons, one control per line). These are real, observed problems in this codebase.
2. **Algorithm.** Declarative screen templates, explicit needs and preferences, and the measured viewport define a small discrete space of presentation configurations: columns, icon-beside or icon-above label, HUD rows, whether the HUD follows the in-app text size, illustration shown or hidden, and text scale. **Hard constraints** must hold: essential controls visible without scrolling, touch target ≥ the person's minimum, labels within 2 lines with no word broken, a declared text-size need honoured, reduced motion honoured, and user locks honoured. **Soft preferences** are ordered lexicographically: text scale closest to what was asked for, illustration kept, fewest changes from the last accepted configuration, then compactness. Exhaustive search finds the best feasible configuration and records why each decision was made. If nothing is feasible, a predeclared relaxation order and a known-safe fallback apply, and the conflict is reported.
3. **Evidence and transfer.**
   - **[Published]** SUPPLE treats UI generation as constrained optimisation over a device model and a user model. Its ability-based variant SUPPLE++ made motor-impaired participants about **26% faster** with fewer errors than default interfaces, in a lab study with a small sample (Gajos, Wobbrock & Weld, CHI 2008; Gajos & Weld, AIJ 2010).
   - **[Published]** Ability-based design argues that systems should adapt to the person's abilities, not the reverse (Wobbrock et al., CACM 2018).
   - **[Published]** Cassowary separates *required* constraints from *preferred* ones with strengths (Badros, Borning & Stuckey, TOCHI 2001).
   - **[Published]** MyUI and the GPII/AccessForAll (ISO/IEC 24751) line represent explicit needs and preferences as portable profiles. The MyUI authors report that run-time adaptation can disorient users unless people can see and control it (Peissner et al. 2012, 2013).
   - **[Published]** For targets, WCAG 2.2 SC 2.5.8 sets a minimum of 24×24 CSS px (AA), and Android guidance recommends ≥ 48×48 dp.
   - **[Published]** Touchscreen use by people living with dementia is feasible, but large targets, simple layouts and little clutter matter (Joddrell & Astell 2016; Link et al. 2025 scoping review).
   - **[Inference]** None of this shows benefit for dementia in particular. The transferable part is the engineering method: verified satisfaction of explicit needs.
4. **Prior work and how this differs.** SUPPLE (2004–2010) is the clear precedent, and we do not claim the idea is new. The differences are:
   - a bilingual Tamil/English app, where Tamil labels are much wider than English;
   - checking against **the app's real bundled fonts** (Noto Sans Tamil and Noto Sans);
   - a caregiver-editable explicit profile with locks;
   - deterministic replay;
   - a *fallback with an explicit report* rather than silent overflow.
   This is an application and verification contribution, not a new algorithm.
5. **Labels, data, cloud, hardware and questions.** No labels and no behavioural data. The only inputs are settings the person or caregiver already makes, plus the screen size. No cloud and no special hardware. No questions beyond the existing settings, and one optional "lock this" control.
6. **Offline Kotlin feasibility.** High. The search space is a few hundred configurations per screen, evaluated in milliseconds. Pure Kotlin with an injected text measurer: AWT `TextLayout` on the JVM for the benchmark, and Compose `TextMeasurer` on the device.
7. **Vertical slice.** Feasible for the radio screen and the top HUD, both of which already contain the ad-hoc rules.
8. **Fair benchmark.** Exhaustive and random-sampled profile × device × content instances, using real strings and real font metrics, scored by one checker. Content not used in development is held out to test generalisation to new activities. Tie the checker to reality with a device-side measurement comparison, then report violation rates, text-scale deficit and illustration retention.
9. **Strongest baseline.** The current hand-written threshold rules, ported faithfully (**B1**). Also a stronger baseline (**B2**): the same rules plus a greedy, checker-in-the-loop repair cascade with a fixed precedence. B2 is what a careful engineer would build next.
10. **Failure modes, privacy and safety.**
    - The geometry model may disagree with real rendering; mitigate with device-side measurement validation and conservative margins.
    - The objective's order may not match what a person wants; mitigate with locks and explanations.
    - Layout changes between sessions can disorient; mitigate with a stability term, and recompute only when inputs change.
    - Privacy: the inputs are settings that already exist, and nothing new is recorded.

### B. Task-aware assistance controller as an explicit, verified statechart (fallback candidate)

1. **User problem.** SMRITI's activities (cooking, dishes, sewing, payment) offer help (Repeat, Show me, Do this step, Undo, Start over), but the help logic is spread across view-model functions. Nothing guarantees that "Stop", "Help" or "Do it for me" is always reachable, or that help never removes the person's choice.
2. **Algorithm.** Each activity becomes an explicit finite-state assistance model: steps, prompts with levels of escalation, and caregiver-set ceilings. Help is offered only on an **explicit request** or **elapsed idle time**, never from mistakes used as labels. Invariants are checked by exhaustive state-space exploration, for example: Stop is always reachable, no prompt loop longer than k, and "do it for me" never fires without a request.
3. **Evidence.**
   - **[Published]** COACH modelled handwashing assistance as a POMDP with prompts of graded specificity. A small pilot (Mihailidis et al. 2008, n = 6) reported fewer caregiver interventions, but camera tracking caused problems.
   - **[Published]** PVS-based verification of medical-device user interfaces found real interaction-design flaws (Masci et al., FASE 2014).
   - **[Published]** Errorless-learning evidence in dementia is mixed (Clare & Jones 2008; 2017 review).
   - **[Inference]** A deterministic statechart with no sensing keeps the verifiable part and drops the sensing problems.
4. **Prior work.** COACH (Hoey, Mihailidis, Boutilier et al.) and model-checked interactive systems. The difference here is a small, sensor-free, exhaustively verified local controller.
5. **Data.** None beyond the activity state. Idle time is a timer, not a label. No cloud and no questions.
6. **Feasibility.** High (pure Kotlin, a state graph per activity).
7. **Vertical slice.** Cooking (four steps) is small enough to verify exhaustively.
8. **Benchmark.** Exhaustive exploration of invariants, compared with the current view-model logic analysed the same way. Measures: invariant violations found, reachable states, and worst-case prompt chains.
9. **Strongest baseline.** The current ad-hoc view-model handlers.
10. **Risks.**
    - The claimed advantage may be zero if the current code already satisfies every invariant. That would be a useful finding, but not a flagship one.
    - Timers can feel pushy; caregiver ceilings and an off switch are needed.

### C. Case-based reuse of configurations (CBR)

1. **Problem.** Initial setup: suggest settings from similar people.
2. **Algorithm.** Retrieve, reuse, revise, retain (Aamodt & Plaza 1994) over profiles and accepted configurations.
3. **Evidence.** **[Published]** CBR is an established framework, but we found no dementia-specific UI personalisation evaluation. **[Inference]** It needs a case base of *other* people.
4. **Prior work.** Extensive in recommender and help-desk systems; SMRITI's case would not be distinctive.
5. **Data.** Needs other people's profiles, which means pooling data or a curated library. A purely local device has one case.
6. **Feasibility.** Easy in Kotlin with a curated case library.
7. **Slice.** A setup wizard.
8. **Benchmark.** Hard to make fair without real cases; it would be simulation again.
9. **Baseline.** Sensible defaults.
10. **Risks.** Stereotyping, privacy if cases are shared, and silent mismatch.

### D. A simpler online learner: dueling bandits over explicit pairwise choices

1. **Problem.** The same problem as SCOPE.
2. **Algorithm.** RUCB or sparse dueling bandits over a few presentation variants, using only explicit A/B choices.
3. **Evidence.** **[Published]** Dueling bandits have regret guarantees (Yue et al. 2009/2012; Zoghi et al. 2014, RUCB), and sparse variants reduce the comparisons needed (Jamieson et al., AISTATS 2015). These guarantees are asymptotic and assume many comparisons.
4. **[Inference]** It would run into the same SCOPE failures: few answers, noisy answers, and a variant space that grows quickly. A bandit explores by design, which is the opposite of the stability we need.
5. **Data.** Explicit pairwise answers, with the same burden as SCOPE.
6. **Feasibility.** Easy.
7. **Slice.** Same as SCOPE.
8. **Benchmark.** Again simulation-only.
9. **Baseline.** Fixed rules.
10. **Risks.** Exploration shows people worse variants on purpose, which conflicts with the safety constraints.

### E. Local personalised memory/content organisation

1. **Problem.** Organising caregiver-added photos and songs into reminiscence sessions.
2. **Algorithm.** Caregiver-tagged items with simple scheduling (rotation, novelty), à la CIRCA.
3. **Evidence.**
   - **[Published]** CIRCA (Alm et al., IEEE Computer 2007) is a system description with qualitative reports.
   - **[Published]** The Cochrane review of reminiscence therapy (Woods et al. 2018; 22 RCTs, n = 1972) finds small, inconsistent effects that depend on the setting.
4. **Prior work.** Many reminiscence apps exist, and the technical contribution would be thin.
5. **Data.** Personal photos and tags, which is sensitive data, though local.
6. **Feasibility.** Easy.
7. **Slice.** Memory Book.
8. **Benchmark.** There is no objective technical benchmark; value can only be shown with humans.
9. **Baseline.** Chronological order.
10. **Risks.** Distressing content, which needs caregiver curation, and a temptation to over-claim therapeutic value.

### Excluded on the brief's grounds

Camera or voice emotion detection, speech-based diagnosis, cognitive-stage prediction, any implicit label (success, speed, hints, stopping, mistakes), cloud LLMs, and surveillance.

---

## Comparison matrix

Scores run from 1 (poor) to 5 (strong). These are our judgements, justified by the sections above.

| Criterion | A. Constraint presentation | B. Verified assistance | C. CBR | D. Dueling bandit | E. Memory organisation |
|---|---|---|---|---|---|
| Technical feasibility | 5 | 5 | 4 | 5 | 5 |
| Reliability with sparse data | 5 (needs none) | 5 | 2 | 1 | 4 |
| Robustness to inconsistent feedback | 5 (no feedback learning) | 5 | 3 | 1 | 4 |
| Explainability | 5 (per-decision reasons) | 5 | 3 | 2 | 4 |
| Offline operation | 5 | 5 | 3 | 5 | 5 |
| User burden | 5 (existing settings only) | 4 | 4 | 2 | 3 (caregiver tagging) |
| Integration effort | 3 | 3 | 4 | 4 | 4 |
| Measurable advantage over a simpler system | **4** (against real fonts and strings, independent of simulated preferences) | 2 (may find nothing) | 1 | 1 | 1 |
| Demo value | 4 (same screen, different needs, visibly correct) | 3 | 2 | 2 | 4 |
| Strength of a defensible novelty claim | 2 (SUPPLE precedent; application and verification only) | 2 | 1 | 1 | 1 |

**Winner: A. Fallback: B.**

---

## Phase 3: Selection

**Primary: A, "SMRITI Fit", explicit-needs presentation solver.** It meets every brief requirement:
- it solves an observed SMRITI problem (hand-written thresholds, HUD labels that ignore the text size);
- it is offline and local-first;
- it collects nothing new;
- it can be tested reproducibly against **real fonts and real strings**;
- it fits the Kotlin architecture as a pure JVM module;
- it has a visible demo;
- its advantage over the current rules can be measured without inventing human preferences.

**Why it addresses SCOPE's limitations:**
- it has no learning, so noisy answers cannot cause harmful proposals;
- it covers every instance where its inputs exist (100% coverage) at zero extra question burden;
- it is deterministic, and stability is an explicit objective;
- harm is defined as "a declared need is violated", which the checker can measure.

The honest limitation: it is engineering, not AI. Its advantage against a *greedy repair* baseline may be small, and the benchmark is set up to show this if so.

**Fallback: B, verified assistance statechart**, if A's benchmark shows no advantage over B2, or if device-side validation of the geometry model fails.

---

## References (inspected)

- Gajos, K. Z., & Weld, D. S. (2010). Automatically generating personalized user interfaces with SUPPLE. *Artificial Intelligence* 174. https://kgajos.seas.harvard.edu/papers/gajos10supple-aij.pdf · project page https://www.eecs.harvard.edu/~kgajos/research/supple/
- Gajos, K. Z., Wobbrock, J. O., & Weld, D. S. (2008). Improving the performance of motor-impaired users with automatically-generated, ability-based interfaces. *CHI 2008*. https://www.eecs.harvard.edu/~kgajos/papers/2008/gajos08improving.shtml · AAAI-08 overview https://faculty.washington.edu/wobbrock/pubs/aaai-08.pdf
- Wobbrock, J. O., Gajos, K. Z., Kane, S. K., & Vanderheiden, G. C. (2018). Ability-based design. *CACM* 61(6). DOI 10.1145/3148051. https://faculty.washington.edu/wobbrock/pubs/cacm-18.pdf
- Badros, G. J., Borning, A., & Stuckey, P. J. (2001). The Cassowary linear arithmetic constraint solving algorithm. *ACM TOCHI*. https://constraints.cs.washington.edu/solvers/cassowary-tr.html
- Peissner, M. et al. (2012). MyUI: generating accessible user interfaces from multimodal design patterns. *EICS 2012*. https://publica.fraunhofer.de/handle/publica/377092 · Peissner & Edlin-White (2013), user control in adaptive UIs, *INTERACT 2013*. https://dl.ifip.org/IFIP-LNCS-8117/hal-01497467
- Raising the Floor: AccessForAll / ISO/IEC 24751 preference sets. https://raisingthefloor.org/our-approach-accessforall/
- W3C WCAG 2.2 SC 2.5.8 Target Size (Minimum), as summarised in DAISY KB https://kb.daisy.org/publishing/docs/wcag/target-size-minimum.html and Deque axe rule https://dequeuniversity.com/rules/axe/4.8/target-size
- Android: "Make apps more accessible" (48 dp targets) https://developer.android.com/guide/topics/ui/accessibility/apps · Touch target size https://support.google.com/accessibility/android/answer/7101858
- Joddrell, P., & Astell, A. J. (2016). Studies involving people with dementia and touchscreen technology: a literature review. *JMIR Rehabil Assist Technol*. https://pmc.ncbi.nlm.nih.gov/articles/PMC5454556 · Link et al. (2025) scoping review https://www.open-access.bcu.ac.uk/16826/
- Amouzadeh, E. et al. (2025), age-friendly app design review. https://www.ncbi.nlm.nih.gov/pmc/articles/PMC12350549/
- Hoey, J. et al. (2010). Automated handwashing assistance for persons with dementia using video and a POMDP. *CVIU*. https://cs.uwaterloo.ca/~jhoey/research/coach/index.php · Boutilier et al. COACH https://www.cs.toronto.edu/~cebly/Papers/CoachIJCAI-abs.html · Mihailidis et al. (2008) *BMC Geriatrics* (n = 6 pilot)
- Masci, P. et al. (2014). Formal verification of medical device user interfaces using PVS. *FASE 2014*. https://link.springer.com/10.1007/978-3-642-54804-8_14
- Clare, L., & Jones, R. S. P. (2008). Errorless learning in the rehabilitation of memory impairment: a critical review. https://research.bangor.ac.uk/en/publications/errorless-learning-in-the-rehabilitation-of-memory-impairment-a-c/ · 2017 review https://alzres.biomedcentral.com/track/pdf/10.1186/s13195-017-0247-9
- Aamodt, A., & Plaza, E. (1994). Case-based reasoning: foundational issues, methodological variations, and system approaches. *AI Communications* 7(1). https://www.doi.org/10.3233/AIC-1994-7104
- Yue, Y., Broder, J., Kleinberg, R., & Joachims, T. (2009/2012). The K-armed dueling bandits problem. · Zoghi, M. et al. (2014). Relative upper confidence bound. https://arxiv.org/pdf/1312.3393 · Jamieson, K. et al. (2015). Sparse dueling bandits. *AISTATS*. https://proceedings.mlr.press/v38/jamieson15.html
- Alm, N. et al. (2007). A communication support system for older people with dementia (CIRCA). *IEEE Computer* 40(5). https://research-repository.st-andrews.ac.uk/handle/10023/1589
- Woods, B. et al. (2018). Reminiscence therapy for dementia. *Cochrane Database Syst Rev*, CD001120.pub3. https://cochranelibrary.com/cdsr/doi/10.1002/14651858.CD001120.pub3/information
- Kay, J. Scrutable user models. https://ercim.eu/publication/Ercim_News/enw46/kay.html
