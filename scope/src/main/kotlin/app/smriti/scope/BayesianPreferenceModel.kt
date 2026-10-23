package app.smriti.scope

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Bayesian contextual pairwise preference model (annex §2.1–2.2).
 *
 *   u(a, c) = (β_global + s(c)·γ + δ_activity)ᵀ x(a)
 *
 * - x(a): 4 presentation features, centred so that "no metadata" (null) contributes 0:
 *   visual density, motion intensity, narration length (each value − 0.5) and instruction modality
 *   (audio-supported +0.5 / visual −0.5). Interaction openness is deferred and ignored.
 * - s(c): +1 Together, −1 Solo, 0 unknown. This is the single predeclared Solo/Together × feature
 *   interaction; no other context is possible.
 * - δ_activity: activity-specific residual with a tighter (shrinkage) prior, i.e. partial pooling
 *   towards the person's global presentation preferences.
 *
 * Likelihood: Bradley–Terry/logistic on explicit left/right answers only.
 * Posterior: Laplace approximation (MAP by Newton's method + inverse Hessian covariance). This is an
 * approximation, not an exact posterior; calibration is reported by the simulator, not assumed.
 */
data class ModelConfig(
    val name: String,
    val useContext: Boolean = true,
    val useActivityResiduals: Boolean = true,
    /** Off = no shared β: each activity learns alone (the pooling-off ablation). */
    val pooling: Boolean = true,
    val priorVarGlobal: Double = 1.0,
    val priorVarContext: Double = 0.5,
    val priorVarActivity: Double = 0.25,
    /** Abstain unless P(best ≻ runner-up) reaches this (provisional, frozen in the protocol). */
    val recommendConfidence: Double = 0.70,
    /** Minimum expected information gain (nats) for a comparison to be worth asking. */
    val minInformationGain: Double = 0.03,
    /** Only pairs among the current top-K are decision-relevant enough to ask about. */
    val queryTopK: Int = 4,
) {
    companion object {
        val FULL_SCOPE = ModelConfig("B3-full-SCOPE")
        val NON_CONTEXTUAL = ModelConfig("B2-non-contextual", useContext = false, useActivityResiduals = false)
        val NO_POOLING = ModelConfig("B3-no-pooling", pooling = false, priorVarActivity = 1.0)
    }
}

const val FEATURE_DIM = 4

/** Centred feature vector; null metadata → 0 (no contribution, never imputed). */
fun encodeFeatures(v: VariantFeatures): DoubleArray = doubleArrayOf(
    v.visualDensity?.let { it - 0.5 } ?: 0.0,
    v.motionIntensity?.let { it - 0.5 } ?: 0.0,
    v.narrationLength?.let { it - 0.5 } ?: 0.0,
    when (v.instructionModality) {
        InstructionModality.AUDIO_SUPPORTED -> 0.5
        InstructionModality.VISUAL -> -0.5
        null -> 0.0
    },
)

fun socialSign(mode: SocialMode?): Double = when (mode) {
    SocialMode.TOGETHER -> 1.0
    SocialMode.SOLO -> -1.0
    null -> 0.0
}

fun sigmoid(z: Double): Double = if (z >= 0) 1.0 / (1.0 + exp(-z)) else exp(z).let { it / (1.0 + it) }

/** Posterior summary for one candidate in one context. */
data class PosteriorPrediction(
    val variantId: VariantId,
    val expectedUtility: Double,
    val utilitySd: Double,
    val credibleLow95: Double,
    val credibleHigh95: Double,
)

/** One fitted Laplace posterior. Immutable; refit from the event list on every change. */
class LaplacePosterior internal constructor(
    val config: ModelConfig,
    private val activities: List<ActivityId>,
    val mean: DoubleArray,
    val covariance: Array<DoubleArray>,
    val labelledComparisons: Int,
    val converged: Boolean,
) {
    val dim: Int get() = mean.size

    fun phi(context: ContextSnapshot, v: VariantFeatures): DoubleArray = buildPhi(config, activities, context, v)

    fun predict(context: ContextSnapshot, v: VariantFeatures): PosteriorPrediction {
        val p = phi(context, v)
        val mu = Linalg.dot(p, mean)
        val sd = sqrt(Linalg.quadForm(covariance, p).coerceAtLeast(0.0))
        return PosteriorPrediction(v.variantId, mu, sd, mu - 1.96 * sd, mu + 1.96 * sd)
    }

    /** Mean and variance of the utility difference u(a) − u(b). */
    fun difference(context: ContextSnapshot, a: VariantFeatures, b: VariantFeatures): Pair<Double, Double> {
        val pa = phi(context, a)
        val pb = phi(context, b)
        val d = DoubleArray(pa.size) { pa[it] - pb[it] }
        return Linalg.dot(d, mean) to Linalg.quadForm(covariance, d).coerceAtLeast(0.0)
    }

    /** Posterior-predictive P(a ≻ b) using the probit approximation to the logistic-Gaussian integral. */
    fun probPrefer(context: ContextSnapshot, a: VariantFeatures, b: VariantFeatures): Double {
        val (m, v) = difference(context, a, b)
        return sigmoid(m / sqrt(1.0 + PI * v / 8.0))
    }

    /**
     * Expected information gain (mutual information, nats) about θ from asking a vs b:
     * H(E[p]) − E[H(p)], with the expectation over Δ ~ N(m, v) by fixed-grid quadrature (deterministic).
     */
    fun expectedInformationGain(context: ContextSnapshot, a: VariantFeatures, b: VariantFeatures): Double {
        val (m, v) = difference(context, a, b)
        val sd = sqrt(v)
        var wSum = 0.0
        var pBar = 0.0
        var condEntropy = 0.0
        for ((z, w) in GRID) {
            val p = sigmoid(m + sd * z)
            pBar += w * p
            condEntropy += w * entropy(p)
            wSum += w
        }
        pBar /= wSum
        condEntropy /= wSum
        return (entropy(pBar) - condEntropy).coerceAtLeast(0.0)
    }

    private companion object {
        val GRID: List<Pair<Double, Double>> = (-32..32).map { k ->
            val z = k * 0.125
            z to exp(-z * z / 2.0)
        }

        fun entropy(p: Double): Double {
            val q = p.coerceIn(1e-12, 1 - 1e-12)
            return -(q * ln(q) + (1 - q) * ln(1 - q))
        }
    }
}

internal fun buildPhi(config: ModelConfig, activities: List<ActivityId>, context: ContextSnapshot, v: VariantFeatures): DoubleArray {
    val x = encodeFeatures(v)
    val out = DoubleArray(dimension(config, activities))
    var o = 0
    if (config.pooling) {
        for (i in 0 until FEATURE_DIM) out[o + i] = x[i]
        o += FEATURE_DIM
    }
    if (config.useContext) {
        val s = socialSign(context.socialMode)
        for (i in 0 until FEATURE_DIM) out[o + i] = s * x[i]
        o += FEATURE_DIM
    }
    if (config.useActivityResiduals || !config.pooling) {
        val k = activities.indexOf(context.activityId)
        if (k >= 0) for (i in 0 until FEATURE_DIM) out[o + k * FEATURE_DIM + i] = x[i]
    }
    return out
}

internal fun dimension(config: ModelConfig, activities: List<ActivityId>): Int =
    (if (config.pooling) FEATURE_DIM else 0) +
        (if (config.useContext) FEATURE_DIM else 0) +
        (if (config.useActivityResiduals || !config.pooling) FEATURE_DIM * activities.size else 0)

internal fun priorPrecision(config: ModelConfig, activities: List<ActivityId>): DoubleArray {
    val out = ArrayList<Double>()
    if (config.pooling) repeat(FEATURE_DIM) { out += 1.0 / config.priorVarGlobal }
    if (config.useContext) repeat(FEATURE_DIM) { out += 1.0 / config.priorVarContext }
    if (config.useActivityResiduals || !config.pooling) repeat(FEATURE_DIM * activities.size) { out += 1.0 / config.priorVarActivity }
    return out.toDoubleArray()
}

/**
 * Fits the Laplace posterior to labelled comparisons, given as (context, winner, loser), in the
 * order supplied. Deterministic: fixed start (prior mean 0), fixed iteration cap, fixed sums.
 */
fun fitPosterior(
    config: ModelConfig,
    activities: List<ActivityId>,
    labelled: List<Triple<ContextSnapshot, VariantFeatures, VariantFeatures>>,
): LaplacePosterior {
    val n = dimension(config, activities)
    val lambda = priorPrecision(config, activities)
    val zs = labelled.map { (c, w, l) ->
        val pw = buildPhi(config, activities, c, w)
        val pl = buildPhi(config, activities, c, l)
        DoubleArray(n) { pw[it] - pl[it] }
    }
    var theta = DoubleArray(n)
    var converged = false
    var hessian = Array(n) { i -> DoubleArray(n).also { it[i] = lambda[i] } }
    for (iter in 0 until 100) {
        val grad = DoubleArray(n) { -lambda[it] * theta[it] }
        hessian = Array(n) { i -> DoubleArray(n).also { it[i] = lambda[i] } }
        for (z in zs) {
            val p = sigmoid(Linalg.dot(z, theta))
            val r = 1.0 - p
            val w = p * (1.0 - p)
            for (i in 0 until n) {
                if (z[i] == 0.0) continue
                grad[i] += r * z[i]
                for (j in 0 until n) if (z[j] != 0.0) hessian[i][j] += w * z[i] * z[j]
            }
        }
        val l = Linalg.cholesky(hessian) ?: break
        val step = Linalg.cholSolve(l, grad)
        theta = DoubleArray(n) { theta[it] + step[it] }
        if (step.maxOf { abs(it) } < 1e-10) {
            converged = true
            break
        }
    }
    // Covariance from the Hessian at the final mean.
    val hFinal = Array(n) { i -> DoubleArray(n).also { it[i] = lambda[i] } }
    for (z in zs) {
        val p = sigmoid(Linalg.dot(z, theta))
        val w = p * (1.0 - p)
        for (i in 0 until n) {
            if (z[i] == 0.0) continue
            for (j in 0 until n) if (z[j] != 0.0) hFinal[i][j] += w * z[i] * z[j]
        }
    }
    val l = Linalg.cholesky(hFinal) ?: Linalg.cholesky(hessian)!!
    return LaplacePosterior(config, activities, theta, Linalg.inverseFromCholesky(l), labelled.size, converged || labelled.isEmpty())
}
