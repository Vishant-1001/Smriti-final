package app.smriti.scope

/** Why the engine declined to recommend; the caller then uses the fixed/manual variant. */
enum class AbstainReason { DISABLED, NOT_OPTED_IN, NO_CANDIDATES, NO_EVIDENCE, UNCERTAIN, VERSION_MISMATCH, MODEL_FAILURE, ACCEPTED_IS_BEST }

sealed interface Recommendation {
    /** A research proposal only: never applied automatically (annex §3.1 step 4). */
    data class Propose(val variantId: VariantId, val confidence: Double, val predictions: List<PosteriorPrediction>) : Recommendation
    data class Abstain(val reason: AbstainReason, val fallback: VariantId, val predictions: List<PosteriorPrediction>) : Recommendation

    val effectiveVariant: VariantId
        get() = when (this) {
            is Propose -> variantId
            is Abstain -> fallback
        }
}

enum class DoNotAskReason { DISABLED, NOT_OPTED_IN, USER_SAID_DO_NOT_ASK, DURING_ACTIVITY, SESSION_LIMIT, NOT_ENOUGH_CANDIDATES, COOLDOWN, LOW_VALUE }

sealed interface ComparisonDecision {
    data class Ask(val left: VariantFeatures, val right: VariantFeatures, val expectedInformationGain: Double) : ComparisonDecision
    data class DoNotAsk(val reason: DoNotAskReason) : ComparisonDecision
}

/** Burden controls (annex §2.3): opt-in, never during an activity, ≤1 per session, 14-day cooldown, permanent opt-out. */
data class QueryBurdenState(
    val optedIn: Boolean,
    val doNotAskAgain: Boolean,
    val inActivity: Boolean,
    val promptsThisSession: Int,
    val nowEpochMs: Long,
    /** Unordered pair key → time it was declined (Skip) or snoozed. */
    val declinedPairs: Map<Set<VariantId>, Long> = emptyMap(),
    val maxPromptsPerSession: Int = 1,
    val cooldownMs: Long = 14L * 24 * 60 * 60 * 1000,
)

sealed interface UpdateResult {
    /** Left/right answer committed; posterior refitted. */
    data object Learned : UpdateResult
    /** Both-fine or skip: kept in the log for audit, no preference fabricated, posterior unchanged. */
    data object RecordedNoPreference : UpdateResult
    data class Rejected(val reason: String) : UpdateResult
}

sealed interface PreferenceResetScope {
    data object All : PreferenceResetScope
    data class Activity(val activityId: ActivityId) : PreferenceResetScope
}

/** Debug-only diagnostics; contains counts and model health, no preference content for third parties. */
data class ModelDiagnostics(
    val modelName: String,
    val modelVersion: String,
    val enabled: Boolean,
    val totalEvents: Int,
    val labelledComparisons: Int,
    val noPreferenceAnswers: Int,
    val rejectedEvents: Int,
    val parameterCount: Int,
    val converged: Boolean,
)

/** What the person sees happen. A control command always wins over any recommendation. */
sealed interface EffectiveAction {
    data class Command(val command: ControlCommand) : EffectiveAction
    data class UseVariant(val variantId: VariantId) : EffectiveAction
}

fun resolveAction(command: ControlCommand?, recommendation: Recommendation): EffectiveAction =
    if (command != null) EffectiveAction.Command(command) else EffectiveAction.UseVariant(recommendation.effectiveVariant)

/** Viewport-dependent availability: removing a variant is NOT a preference signal (annex §3.3). */
data class AvailabilityResult(val eligible: List<VariantFeatures>, val unavailableOnThisScreen: List<VariantId>)

/** Annex §6 model API. */
interface ContextualPreferenceModel {
    fun predict(context: ContextSnapshot, candidates: List<VariantFeatures>): List<PosteriorPrediction>
    fun recommend(context: ContextSnapshot, candidates: List<VariantFeatures>): Recommendation
    fun selectComparison(context: ContextSnapshot, candidates: List<VariantFeatures>, burdenState: QueryBurdenState): ComparisonDecision
    fun update(event: PairwisePreferenceEvent): UpdateResult
    fun reset(scope: PreferenceResetScope)
    fun diagnostics(): ModelDiagnostics
}

/**
 * The SCOPE engine: hard validator + append-only event log + Laplace posterior rebuilt by replay.
 * All state is derived from the event log, so replaying the same events gives the same posterior.
 */
class ScopeEngine(
    val schema: VariantSchema,
    val config: ModelConfig = ModelConfig.FULL_SCOPE,
    val modelVersion: String = MODEL_VERSION,
    /** `scope.v2.enabled`; false by default. While false the engine never proposes or asks. */
    var enabled: Boolean = ScopeFlags.ENABLED_BY_DEFAULT,
    /** The person must opt into preference personalisation before any proposal or question. */
    var optedIn: Boolean = false,
) : ContextualPreferenceModel {

    private val log = ArrayList<PairwisePreferenceEvent>()
    private var rejected = 0
    private var posterior: LaplacePosterior = fitPosterior(config, schema.activities, emptyList())

    val events: List<PairwisePreferenceEvent> get() = log.toList()
    val currentPosterior: LaplacePosterior get() = posterior

    // ------------------------------------------------------------ validator

    /** Hard, deterministic filter: same activity, approved, version-compatible, available on this screen. */
    fun eligible(context: ContextSnapshot, candidates: List<VariantFeatures>, availableOnScreen: Set<VariantId>? = null): AvailabilityResult {
        val ok = candidates.filter {
            it.activityId == context.activityId && it.approved &&
                it.contentVersion == schema.contentVersion && it.featureSchemaVersion == schema.featureSchemaVersion &&
                schema.variants.any { s -> s == it }
        }
        if (availableOnScreen == null) return AvailabilityResult(ok, emptyList())
        val (shown, hidden) = ok.partition { it.variantId in availableOnScreen }
        return AvailabilityResult(shown, hidden.map { it.variantId })
    }

    private fun manualFor(context: ContextSnapshot): VariantId = schema.manualDefaults.getValue(context.activityId)

    // ------------------------------------------------------------ prediction

    override fun predict(context: ContextSnapshot, candidates: List<VariantFeatures>): List<PosteriorPrediction> =
        eligible(context, candidates).eligible.map { posterior.predict(context, it) }

    override fun recommend(context: ContextSnapshot, candidates: List<VariantFeatures>): Recommendation {
        val fallback = manualFor(context)
        if (!enabled) return Recommendation.Abstain(AbstainReason.DISABLED, fallback, emptyList())
        if (!optedIn) return Recommendation.Abstain(AbstainReason.NOT_OPTED_IN, fallback, emptyList())
        if (candidates.any { it.contentVersion != schema.contentVersion || it.featureSchemaVersion != schema.featureSchemaVersion }) {
            return Recommendation.Abstain(AbstainReason.VERSION_MISMATCH, fallback, emptyList())
        }
        return try {
            recommendUnchecked(context, candidates)
        } catch (e: RuntimeException) {
            Recommendation.Abstain(AbstainReason.MODEL_FAILURE, fallback, emptyList())
        }
    }

    /**
     * Recommendation logic without the enable/consent gates; used by the offline simulator.
     * [accepted] is the variant currently in use (defaults to the manual variant); v2 proposals must beat it.
     */
    fun recommendUnchecked(context: ContextSnapshot, candidates: List<VariantFeatures>, accepted: VariantId? = null): Recommendation =
        recommendWith(config.decision, context, candidates, accepted)

    /** Applies a given decision rule to the current posterior (used to score rules on identical posteriors). */
    fun recommendWith(decision: DecisionRule, context: ContextSnapshot, candidates: List<VariantFeatures>, accepted: VariantId? = null): Recommendation {
        val fallback = accepted ?: manualFor(context)
        val elig = eligible(context, candidates).eligible
        if (elig.size < 2) return Recommendation.Abstain(AbstainReason.NO_CANDIDATES, fallback, emptyList())
        val preds = elig.map { posterior.predict(context, it) }
        if (posterior.labelledComparisons == 0) return Recommendation.Abstain(AbstainReason.NO_EVIDENCE, fallback, preds)
        val order = elig.indices.sortedWith(compareByDescending<Int> { preds[it].expectedUtility }.thenBy { elig[it].variantId.value })
        val best = elig[order[0]]
        return when (val rule = decision) {
            is DecisionRule.TopTwoPredictive -> {
                val runner = elig[order[1]]
                val conf = posterior.probPrefer(context, best, runner)
                if (conf >= rule.threshold) Recommendation.Propose(best.variantId, conf, preds)
                else Recommendation.Abstain(AbstainReason.UNCERTAIN, fallback, preds)
            }
            is DecisionRule.VersusManual -> {
                val current = elig.firstOrNull { it.variantId == fallback }
                    ?: return Recommendation.Abstain(AbstainReason.NO_CANDIDATES, fallback, preds)
                if (best.variantId == current.variantId) return Recommendation.Abstain(AbstainReason.ACCEPTED_IS_BEST, fallback, preds)
                val (m, _) = posterior.difference(context, best, current)
                val pBetter = posterior.probBetter(context, best, current, rule.margin)
                if (m >= rule.margin && pBetter >= rule.tau) Recommendation.Propose(best.variantId, pBetter, preds)
                else Recommendation.Abstain(AbstainReason.UNCERTAIN, fallback, preds)
            }
        }
    }

    fun probPrefer(context: ContextSnapshot, a: VariantFeatures, b: VariantFeatures): Double = posterior.probPrefer(context, a, b)

    // ------------------------------------------------------------ comparison selection

    override fun selectComparison(context: ContextSnapshot, candidates: List<VariantFeatures>, burdenState: QueryBurdenState): ComparisonDecision {
        if (!enabled) return ComparisonDecision.DoNotAsk(DoNotAskReason.DISABLED)
        if (!optedIn || !burdenState.optedIn) return ComparisonDecision.DoNotAsk(DoNotAskReason.NOT_OPTED_IN)
        if (burdenState.doNotAskAgain) return ComparisonDecision.DoNotAsk(DoNotAskReason.USER_SAID_DO_NOT_ASK)
        if (burdenState.inActivity) return ComparisonDecision.DoNotAsk(DoNotAskReason.DURING_ACTIVITY)
        if (burdenState.promptsThisSession >= burdenState.maxPromptsPerSession) return ComparisonDecision.DoNotAsk(DoNotAskReason.SESSION_LIMIT)
        return selectComparisonUnchecked(context, candidates, burdenState)
    }

    /** Value-of-information choice among decision-relevant pairs, honouring cooldowns. */
    fun selectComparisonUnchecked(context: ContextSnapshot, candidates: List<VariantFeatures>, burdenState: QueryBurdenState? = null): ComparisonDecision {
        val elig = eligible(context, candidates).eligible
        if (elig.size < 2) return ComparisonDecision.DoNotAsk(DoNotAskReason.NOT_ENOUGH_CANDIDATES)
        val ranked = elig.sortedWith(compareByDescending<VariantFeatures> { posterior.predict(context, it).expectedUtility }.thenBy { it.variantId.value })
        val top = ranked.take(config.queryTopK.coerceAtLeast(2))
        var best: ComparisonDecision.Ask? = null
        var anyAllowed = false
        for (i in top.indices) for (j in i + 1 until top.size) {
            val a = top[i]
            val b = top[j]
            val declinedAt = burdenState?.declinedPairs?.get(setOf(a.variantId, b.variantId))
            if (declinedAt != null && burdenState!!.nowEpochMs - declinedAt < burdenState.cooldownMs) continue
            anyAllowed = true
            val gain = posterior.expectedInformationGain(context, a, b)
            val (l, r) = if (a.variantId.value <= b.variantId.value) a to b else b to a
            if (best == null || gain > best.expectedInformationGain + 1e-12) best = ComparisonDecision.Ask(l, r, gain)
        }
        if (!anyAllowed) return ComparisonDecision.DoNotAsk(DoNotAskReason.COOLDOWN)
        return if (best != null && best.expectedInformationGain >= config.minInformationGain) best
        else ComparisonDecision.DoNotAsk(DoNotAskReason.LOW_VALUE)
    }

    // ------------------------------------------------------------ updates

    override fun update(event: PairwisePreferenceEvent): UpdateResult {
        val problem = validateEvent(event)
        if (problem != null) {
            rejected++
            return UpdateResult.Rejected(problem)
        }
        log += event
        return when (event.response) {
            ExplicitPreferenceResponse.PreferLeft, ExplicitPreferenceResponse.PreferRight -> {
                refit()
                UpdateResult.Learned
            }
            ExplicitPreferenceResponse.BothFine, ExplicitPreferenceResponse.Skip -> UpdateResult.RecordedNoPreference
        }
    }

    /** Fail closed: anything inconsistent is rejected, never reinterpreted with today's definitions. */
    private fun validateEvent(e: PairwisePreferenceEvent): String? = when {
        e.modelVersion != modelVersion -> "model version mismatch"
        e.contentVersion != schema.contentVersion -> "content version mismatch"
        e.leftFeaturesSnapshot.featureSchemaVersion != schema.featureSchemaVersion ||
            e.rightFeaturesSnapshot.featureSchemaVersion != schema.featureSchemaVersion -> "feature schema mismatch"
        e.leftFeaturesSnapshot.contentVersion != e.contentVersion || e.rightFeaturesSnapshot.contentVersion != e.contentVersion -> "snapshot content version mismatch"
        e.leftVariantId == e.rightVariantId -> "left and right are the same variant"
        e.leftFeaturesSnapshot.activityId != e.activityId || e.rightFeaturesSnapshot.activityId != e.activityId -> "variant from another activity"
        e.activityId !in schema.activities -> "unknown activity"
        !e.leftFeaturesSnapshot.approved || !e.rightFeaturesSnapshot.approved -> "unapproved variant"
        log.any { it.eventId == e.eventId } -> "duplicate event id"
        else -> null
    }

    private fun labelled(): List<Triple<ContextSnapshot, VariantFeatures, VariantFeatures>> = log.mapNotNull { e ->
        when (e.response) {
            ExplicitPreferenceResponse.PreferLeft -> Triple(e.context, e.leftFeaturesSnapshot, e.rightFeaturesSnapshot)
            ExplicitPreferenceResponse.PreferRight -> Triple(e.context, e.rightFeaturesSnapshot, e.leftFeaturesSnapshot)
            else -> null
        }
    }

    private fun refit() {
        posterior = fitPosterior(config, schema.activities, labelled())
    }

    override fun reset(scope: PreferenceResetScope) {
        when (scope) {
            PreferenceResetScope.All -> log.clear()
            is PreferenceResetScope.Activity -> log.removeAll { it.activityId == scope.activityId }
        }
        refit()
    }

    /** Rebuilds derived state from an event log (e.g. after process death). Invalid events are rejected. */
    fun replay(events: List<PairwisePreferenceEvent>): List<UpdateResult> {
        log.clear()
        rejected = 0
        val results = events.map { e ->
            val problem = validateEvent(e)
            if (problem != null) {
                rejected++
                UpdateResult.Rejected(problem)
            } else {
                log += e
                if (e.response is ExplicitPreferenceResponse.PreferLeft || e.response is ExplicitPreferenceResponse.PreferRight) UpdateResult.Learned
                else UpdateResult.RecordedNoPreference
            }
        }
        refit()
        return results
    }

    override fun diagnostics(): ModelDiagnostics = ModelDiagnostics(
        modelName = config.name,
        modelVersion = modelVersion,
        enabled = enabled,
        totalEvents = log.size,
        labelledComparisons = posterior.labelledComparisons,
        noPreferenceAnswers = log.count { it.response is ExplicitPreferenceResponse.BothFine || it.response is ExplicitPreferenceResponse.Skip },
        rejectedEvents = rejected,
        parameterCount = posterior.dim,
        converged = posterior.converged,
    )

    companion object {
        const val MODEL_VERSION = "scope-v2.1-laplace-bt-1"
    }
}
