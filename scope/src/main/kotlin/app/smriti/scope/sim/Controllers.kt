package app.smriti.scope.sim

import app.smriti.scope.ComparisonDecision
import app.smriti.scope.ExplicitPreferenceResponse
import app.smriti.scope.InstructionModality
import app.smriti.scope.ModelConfig
import app.smriti.scope.PairwisePreferenceEvent
import app.smriti.scope.Recommendation
import app.smriti.scope.ScopeEngine
import app.smriti.scope.SocialMode
import app.smriti.scope.VariantFeatures
import app.smriti.scope.VariantId
import app.smriti.scope.VariantSchema
import app.smriti.scope.ContextSnapshot
import app.smriti.scope.AbstainReason
import app.smriti.scope.encodeFeatures
import app.smriti.scope.sigmoid
import kotlin.math.sign

/** Common interface so every controller receives identical events, candidates and budgets. */
interface Controller {
    val name: String
    fun probLeft(ctx: ContextSnapshot, a: VariantFeatures, b: VariantFeatures): Double
    fun recommend(ctx: ContextSnapshot, candidates: List<VariantFeatures>): Recommendation
    fun observe(event: PairwisePreferenceEvent)
    /** Active controllers choose their own comparison (null = do not ask). Passive ones return null. */
    fun choosePair(ctx: ContextSnapshot, pool: List<VariantFeatures>): Pair<VariantFeatures, VariantFeatures>? = null
    val active: Boolean get() = false
    /** POST-HOC DIAGNOSTIC ONLY: the controller's top-ranked variant ignoring its abstention rule. */
    fun topRanked(ctx: ContextSnapshot, candidates: List<VariantFeatures>): VariantId = recommend(ctx, candidates).effectiveVariant
}

/** B0: never learns; always the manual default. */
class FixedController(private val schema: VariantSchema) : Controller {
    override val name = "B0-fixed"
    override fun probLeft(ctx: ContextSnapshot, a: VariantFeatures, b: VariantFeatures) = 0.5
    override fun recommend(ctx: ContextSnapshot, candidates: List<VariantFeatures>): Recommendation =
        Recommendation.Abstain(AbstainReason.DISABLED, schema.manualDefaults.getValue(ctx.activityId), emptyList())
    override fun observe(event: PairwisePreferenceEvent) = Unit
}

/** B2/B3 and ablations: thin wrapper over the real engine. */
class BayesController(schema: VariantSchema, config: ModelConfig, override val active: Boolean = false, label: String? = null) : Controller {
    private val engine = ScopeEngine(schema, config, enabled = true, optedIn = true)
    override val name = label ?: config.name
    override fun probLeft(ctx: ContextSnapshot, a: VariantFeatures, b: VariantFeatures) = engine.probPrefer(ctx, a, b)
    override fun recommend(ctx: ContextSnapshot, candidates: List<VariantFeatures>) = engine.recommendUnchecked(ctx, candidates)
    override fun topRanked(ctx: ContextSnapshot, candidates: List<VariantFeatures>): VariantId {
        val preds = engine.predict(ctx, candidates)
        if (engine.currentPosterior.labelledComparisons == 0 || preds.isEmpty()) return recommend(ctx, candidates).effectiveVariant
        return preds.sortedWith(compareByDescending<app.smriti.scope.PosteriorPrediction> { it.expectedUtility }.thenBy { it.variantId.value }).first().variantId
    }
    override fun observe(event: PairwisePreferenceEvent) {
        engine.update(event)
    }
    override fun choosePair(ctx: ContextSnapshot, pool: List<VariantFeatures>): Pair<VariantFeatures, VariantFeatures>? {
        if (!active) return null
        return when (val d = engine.selectComparisonUnchecked(ctx, pool)) {
            is ComparisonDecision.Ask -> d.left to d.right
            is ComparisonDecision.DoNotAsk -> null
        }
    }
}

/**
 * B1 (and B1F with [featureVotes]): a deliberately strong hand-authored controller.
 * - Recency-weighted (0.9^age) variant win/loss scores per (activity, mode), falling back to per-activity.
 * - Hand-written Solo/Together table for unseen or tied variants.
 * - B1F only: per-context majority "direction votes" per feature, applied as a heuristic score.
 * No fitted statistical model. Deterministic.
 */
class RuleController(
    private val schema: VariantSchema,
    private val featureVotes: Boolean,
    /** Probability mapping scale (v1: 1.2 for variant score, 1.0 for feature score). Calibrated on dev in v2. */
    private val probScale: Double = 1.0,
    /** v2: propose only if the top score beats the manual variant's score by at least this. Null = v1 rule. */
    private val manualMargin: Double? = null,
    label: String? = null,
) : Controller {
    override val name = label ?: if (featureVotes) "B1F-rules+feature-votes" else "B1-rules"

    private data class Obs(val ctx: ContextSnapshot, val winner: VariantFeatures, val loser: VariantFeatures, val t: Int)
    private val history = ArrayList<Obs>()
    private var lastTop: VariantId? = null

    override fun topRanked(ctx: ContextSnapshot, candidates: List<VariantFeatures>): VariantId {
        recommend(ctx, candidates)
        return lastTop ?: schema.manualDefaults.getValue(ctx.activityId)
    }
    private var clock = 0

    override fun observe(event: PairwisePreferenceEvent) {
        clock++
        when (event.response) {
            ExplicitPreferenceResponse.PreferLeft -> history += Obs(event.context, event.leftFeaturesSnapshot, event.rightFeaturesSnapshot, clock)
            ExplicitPreferenceResponse.PreferRight -> history += Obs(event.context, event.rightFeaturesSnapshot, event.leftFeaturesSnapshot, clock)
            else -> Unit // both-fine and skip are not preferences
        }
    }

    private fun weight(o: Obs) = Math.pow(0.9, (clock - o.t).toDouble())

    private fun relevant(ctx: ContextSnapshot, exactMode: Boolean) =
        history.filter { it.ctx.activityId == ctx.activityId && (!exactMode || it.ctx.socialMode == ctx.socialMode) }

    private fun variantScores(obs: List<Obs>): Map<VariantId, Double> {
        val m = HashMap<VariantId, Double>()
        for (o in obs) {
            val w = weight(o)
            m[o.winner.variantId] = (m[o.winner.variantId] ?: 0.0) + w
            m[o.loser.variantId] = (m[o.loser.variantId] ?: 0.0) - w
        }
        return m
    }

    /** Per-feature direction: +1 if chosen variants usually had more of it, −1 if less, 0 if no majority. */
    private fun featureDirections(obs: List<Obs>): DoubleArray {
        val votes = DoubleArray(4)
        for (o in obs) {
            val xw = encodeFeatures(o.winner)
            val xl = encodeFeatures(o.loser)
            for (f in 0 until 4) votes[f] += weight(o) * sign(xw[f] - xl[f])
        }
        return DoubleArray(4) { f -> if (kotlin.math.abs(votes[f]) >= 1.0) sign(votes[f]) else 0.0 }
    }

    private fun tableScore(ctx: ContextSnapshot, v: VariantFeatures): Double = when (ctx.socialMode) {
        SocialMode.SOLO -> -(v.motionIntensity ?: 0.5) - (v.narrationLength ?: 0.5)
        SocialMode.TOGETHER -> (if (v.instructionModality == InstructionModality.AUDIO_SUPPORTED) 1.0 else 0.0) + (v.visualDensity ?: 0.5)
        null -> 0.0
    }

    /** Most specific level with evidence about a or b: (activity, mode) then activity. */
    private fun levelFor(ctx: ContextSnapshot, ids: Set<VariantId>): List<Obs> {
        val exact = relevant(ctx, true)
        if (exact.any { it.winner.variantId in ids || it.loser.variantId in ids }) return exact
        return relevant(ctx, false)
    }

    private fun featureScore(ctx: ContextSnapshot, v: VariantFeatures): Double {
        if (!featureVotes) return 0.0
        val exact = relevant(ctx, true)
        val obs = if (exact.isNotEmpty()) exact else relevant(ctx, false)
        val dir = featureDirections(obs)
        val x = encodeFeatures(v)
        var s = 0.0
        for (f in 0 until 4) s += dir[f] * x[f]
        return s
    }

    override fun probLeft(ctx: ContextSnapshot, a: VariantFeatures, b: VariantFeatures): Double {
        val obs = levelFor(ctx, setOf(a.variantId, b.variantId))
        val vs = variantScores(obs)
        val dv = (vs[a.variantId] ?: 0.0) - (vs[b.variantId] ?: 0.0)
        val df = featureScore(ctx, a) - featureScore(ctx, b)
        if (dv != 0.0 || df != 0.0) return sigmoid(probScale * (1.2 * dv + 1.0 * df))
        val dt = tableScore(ctx, a) - tableScore(ctx, b)
        return 0.5 + 0.1 * sign(dt)
    }

    override fun recommend(ctx: ContextSnapshot, candidates: List<VariantFeatures>): Recommendation {
        val fallback = schema.manualDefaults.getValue(ctx.activityId)
        val elig = candidates.filter { it.activityId == ctx.activityId && it.approved }
        val obs = relevant(ctx, true).ifEmpty { relevant(ctx, false) }
        val vs = variantScores(obs)
        fun total(v: VariantFeatures) = (vs[v.variantId] ?: 0.0) + featureScore(ctx, v)
        val best = elig.sortedWith(compareByDescending<VariantFeatures> { total(it) }.thenByDescending { tableScore(ctx, it) }.thenBy { it.variantId.value }).firstOrNull()
            ?: return Recommendation.Abstain(AbstainReason.NO_CANDIDATES, fallback, emptyList())
        lastTop = best.variantId
        if (manualMargin != null) {
            val manual = elig.firstOrNull { it.variantId == fallback } ?: return Recommendation.Abstain(AbstainReason.NO_CANDIDATES, fallback, emptyList())
            if (best.variantId == fallback) return Recommendation.Abstain(AbstainReason.ACCEPTED_IS_BEST, fallback, emptyList())
            return if (total(best) - total(manual) >= manualMargin) Recommendation.Propose(best.variantId, 1.0, emptyList())
            else Recommendation.Abstain(AbstainReason.UNCERTAIN, fallback, emptyList())
        }
        return if (total(best) >= 1.0) Recommendation.Propose(best.variantId, 1.0, emptyList())
        else Recommendation.Abstain(AbstainReason.NO_EVIDENCE, fallback, emptyList())
    }
}
