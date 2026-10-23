package app.smriti.scope.sim

import app.smriti.scope.ActivityId
import app.smriti.scope.ContextSnapshot
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
import app.smriti.scope.encodeFeatures
import app.smriti.scope.sigmoid
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/** Deterministic RNG with Gaussian draws (Box–Muller) on top of Kotlin's seeded generator. */
class Rng(seed: Long) {
    private val r = Random(seed)
    fun double() = r.nextDouble()
    fun int(n: Int) = r.nextInt(n)
    fun gaussian(sd: Double = 1.0): Double {
        val u1 = r.nextDouble().coerceAtLeast(1e-12)
        val u2 = r.nextDouble()
        return sd * sqrt(-2.0 * ln(u1)) * cos(2.0 * Math.PI * u2)
    }
}

/** Frozen protocol constants (docs/scope/SCOPE_PROTOCOL.md). */
object Protocol {
    val ACTIVITIES = listOf("cooking", "dishes", "sewing", "market_stall").map { ActivityId(it) }
    const val CATALOGUE_SEED = 7L
    const val VARIANTS_PER_ACTIVITY = 12
    const val POOL_SIZE = 8
    const val BUDGET = 32
    val CHECKPOINTS = listOf(4, 8, 16, 32)
    const val PRIMARY_CHECKPOINT = 16
    val DEV_SEEDS = (1L..10L).toList()
    val HELDOUT_SEEDS = (1001L..1040L).toList()
    const val BOOTSTRAP_RESAMPLES = 2000
    const val BOOTSTRAP_SEED = 99L
    const val BOTH_FINE_MARGIN = 0.15
    const val FEATURE_SCHEMA = "scope-features-v1"
}

/** Catalogue of curated variants per activity, with a query pool and held-out unseen variants. */
class Catalogue(val contentVersion: String, val masked: Boolean = false) {
    val truthVariants: Map<ActivityId, List<VariantFeatures>>
    val viewVariants: Map<ActivityId, List<VariantFeatures>>
    val schema: VariantSchema

    init {
        val grid = ArrayList<DoubleArray>()
        val levels = listOf(0.0, 0.5, 1.0)
        for (vd in levels) for (mo in levels) for (na in levels) for (m in 0..1) grid += doubleArrayOf(vd, mo, na, m.toDouble())
        val truth = LinkedHashMap<ActivityId, List<VariantFeatures>>()
        val view = LinkedHashMap<ActivityId, List<VariantFeatures>>()
        Protocol.ACTIVITIES.forEachIndexed { k, act ->
            val rng = Rng(Protocol.CATALOGUE_SEED * 1000 + k)
            val idx = grid.indices.toMutableList()
            for (i in idx.size - 1 downTo 1) { val j = rng.int(i + 1); val t = idx[i]; idx[i] = idx[j]; idx[j] = t }
            val chosen = idx.take(Protocol.VARIANTS_PER_ACTIVITY)
            val vs = chosen.mapIndexed { n, g ->
                val f = grid[g]
                VariantFeatures(
                    VariantId("${act.value}_v%02d".format(n)), act, contentVersion, Protocol.FEATURE_SCHEMA,
                    f[0], f[1], f[2], if (f[3] == 1.0) InstructionModality.AUDIO_SUPPORTED else InstructionModality.VISUAL,
                )
            }
            truth[act] = vs
            // P9: the models see partial metadata (fixed per variant); the truth keeps every feature.
            val maskRng = Rng(Protocol.CATALOGUE_SEED * 5000 + k)
            view[act] = if (!masked) vs else vs.map { v ->
                val dropNarration = maskRng.double() < 0.5
                val dropModality = maskRng.double() < 0.25
                v.copy(narrationLength = if (dropNarration) null else v.narrationLength, instructionModality = if (dropModality) null else v.instructionModality)
            }
        }
        truthVariants = truth
        viewVariants = view
        schema = VariantSchema(
            Protocol.FEATURE_SCHEMA, contentVersion, Protocol.ACTIVITIES, view.values.flatten(),
            view.mapValues { it.value.first().variantId },
        )
    }

    fun pool(act: ActivityId) = viewVariants.getValue(act).take(Protocol.POOL_SIZE)
    fun isUnseen(v: VariantId) = viewVariants.values.any { list -> list.drop(Protocol.POOL_SIZE).any { it.variantId == v } }
    fun truthOf(v: VariantId) = truthVariants.values.flatten().first { it.variantId == v }
}

enum class Profile(val label: String, val scale: Double, val skip: Double) {
    P1_STABLE_ADDITIVE("P1 stable-additive", 1.0, 0.1),
    P2_CONTEXT_TRADEOFF("P2 context-tradeoff", 1.0, 0.1),
    P3_ACTIVITY_SPECIFIC("P3 activity-specific", 1.0, 0.1),
    P4_LOW_FEEDBACK("P4 low-feedback/high-skip", 1.0, 0.6),
    P5_NOISY("P5 noisy", 0.35, 0.1),
    P6_DRIFT("P6 drift", 1.0, 0.1),
    P7_MISSPECIFIED("P7 misspecified", 1.0, 0.1),
    P8_NO_CONTEXT_WEAK("P8 no-context-weak", 0.6, 0.1),
    P9_PARTIAL_METADATA("P9 partial-metadata", 1.0, 0.1),
    P10_VERSION_CHANGE("P10 version-change", 1.0, 0.1),
}

/** A synthetic person's true preferences (never visible to controllers). */
class Person(val profile: Profile, seed: Long) {
    private val rng = Rng(seed * 104729 + profile.ordinal)
    private fun vec(sd: Double) = DoubleArray(4) { rng.gaussian(sd) }
    private val beta: DoubleArray
    private val gamma: DoubleArray
    private val delta: List<DoubleArray>
    private val beta2: DoubleArray
    private val gamma2: DoubleArray
    private val s1 = if (rng.double() < 0.5) 1.0 else -1.0
    private val s2 = if (rng.double() < 0.5) 1.0 else -1.0

    init {
        when (profile) {
            Profile.P1_STABLE_ADDITIVE -> { beta = vec(2.0); gamma = DoubleArray(4); delta = List(4) { DoubleArray(4) } }
            Profile.P3_ACTIVITY_SPECIFIC -> { beta = vec(1.5); gamma = DoubleArray(4); delta = List(4) { vec(1.2) } }
            Profile.P8_NO_CONTEXT_WEAK -> { beta = vec(0.8); gamma = DoubleArray(4); delta = List(4) { DoubleArray(4) } }
            else -> { beta = vec(1.0); gamma = vec(2.0); delta = List(4) { DoubleArray(4) } }
        }
        beta2 = vec(1.0)
        gamma2 = vec(2.0)
    }

    fun utility(v: VariantFeatures, ctx: ContextSnapshot, t: Int): Double {
        val x = encodeFeatures(v)
        val drifted = profile == Profile.P6_DRIFT && t >= Protocol.BUDGET / 2
        val b = if (drifted) beta2 else beta
        val g = if (drifted) gamma2 else gamma
        val s = when (ctx.socialMode) { SocialMode.TOGETHER -> 1.0; SocialMode.SOLO -> -1.0; null -> 0.0 }
        val d = delta[Protocol.ACTIVITIES.indexOf(ctx.activityId)]
        var u = 0.0
        for (i in 0 until 4) u += (b[i] + s * g[i] + d[i]) * x[i]
        if (profile == Profile.P7_MISSPECIFIED) {
            val vd = v.visualDensity ?: 0.5; val mo = v.motionIntensity ?: 0.5; val na = v.narrationLength ?: 0.5
            u += 3.0 * s1 * (mo - 0.5) * (na - 0.5) + 2.0 * s2 * ((vd - 0.5) * (vd - 0.5) - 1.0 / 12.0)
        }
        return u
    }

    fun respond(a: VariantFeatures, b: VariantFeatures, ctx: ContextSnapshot, t: Int, r: Rng): ExplicitPreferenceResponse {
        if (r.double() < profile.skip) return ExplicitPreferenceResponse.Skip
        val diff = utility(a, ctx, t) - utility(b, ctx, t)
        if (abs(diff) < Protocol.BOTH_FINE_MARGIN && r.double() < 0.5) return ExplicitPreferenceResponse.BothFine
        return if (r.double() < sigmoid(profile.scale * diff)) ExplicitPreferenceResponse.PreferLeft else ExplicitPreferenceResponse.PreferRight
    }
}

/** Per-user, per-controller, per-checkpoint metrics. */
data class Metrics(
    val heldoutCE: Double,
    val heldoutCEUnseen: Double,
    val agreement: Double,
    val normRegret: Double,
    val abstainRate: Double,
    val prequentialLogLoss: Double,
    val prequentialBrier: Double,
    val labelledSoFar: Int,
    val asked: Int,
    val switchRate: Double,
    /** POST-HOC DIAGNOSTIC (not part of the gate): regret if the top-ranked variant were always proposed. */
    val regretNoAbstain: Double,
    /** (predicted q, true p*) pairs for calibration buckets. */
    val calibration: List<Pair<Double, Double>>,
)

typealias ControllerFactory = (VariantSchema) -> Controller

object Simulator {
    val FACTORIES: List<ControllerFactory> = listOf(
        { s -> FixedController(s) },
        { s -> RuleController(s, featureVotes = false) },
        { s -> RuleController(s, featureVotes = true) },
        { s -> BayesController(s, ModelConfig.NON_CONTEXTUAL) },
        { s -> BayesController(s, ModelConfig.FULL_SCOPE) },
        { s -> BayesController(s, ModelConfig.NO_POOLING) },
        { s -> BayesController(s, ModelConfig.NON_CONTEXTUAL, active = true, label = "B2-VOI") },
        { s -> BayesController(s, ModelConfig.FULL_SCOPE, active = true, label = "B3-VOI") },
    )

    /** Runs one synthetic person through all controllers. Returns controller name → checkpoint → metrics. */
    fun runUser(profile: Profile, seed: Long, factories: List<ControllerFactory> = FACTORIES): Map<String, Map<Int, Metrics>> {
        var catalogue = Catalogue("content-1", masked = profile == Profile.P9_PARTIAL_METADATA)
        val person = Person(profile, seed)
        var controllers = factories.map { it(catalogue.schema) }
        val names = controllers.map { it.name }
        val out = names.associateWith { LinkedHashMap<Int, Metrics>() }
        val preLL = names.associateWith { ArrayList<Double>() }
        val preBrier = names.associateWith { ArrayList<Double>() }
        val asked = names.associateWith { intArrayOf(0) }
        val labelled = names.associateWith { intArrayOf(0) }
        val switches = names.associateWith { intArrayOf(0, 0) }
        val lastRec = names.associateWith { HashMap<ContextSnapshot, VariantId>() }

        for (t in 0 until Protocol.BUDGET) {
            if (profile == Profile.P10_VERSION_CHANGE && t == Protocol.BUDGET / 2) {
                // Content version changes: every controller restarts; old events would be rejected (fail closed).
                catalogue = Catalogue("content-2")
                controllers = factories.map { it(catalogue.schema) }
                lastRec.values.forEach { it.clear() }
            }
            val ctxRng = Rng(seed * 7919 + t)
            val act = Protocol.ACTIVITIES[ctxRng.int(Protocol.ACTIVITIES.size)]
            val mode = if (ctxRng.double() < 0.5) SocialMode.SOLO else SocialMode.TOGETHER
            val ctx = ContextSnapshot(act, mode)
            val pool = catalogue.pool(act)
            val i = ctxRng.int(pool.size)
            var j = ctxRng.int(pool.size - 1)
            if (j >= i) j++
            val passive = pool[i] to pool[j]

            controllers.forEachIndexed { ci, c ->
                val n = names[ci]
                val pair = if (c.active) c.choosePair(ctx, pool) else passive
                if (pair != null) {
                    asked.getValue(n)[0]++
                    val (left, right) = pair
                    val tl = catalogue.truthOf(left.variantId)
                    val tr = catalogue.truthOf(right.variantId)
                    val respRng = Rng(seed * 31337 + t * 977 + (left.variantId.value + "|" + right.variantId.value).hashCode())
                    val resp = person.respond(tl, tr, ctx, t, respRng)
                    if (resp is ExplicitPreferenceResponse.PreferLeft || resp is ExplicitPreferenceResponse.PreferRight) {
                        val q = c.probLeft(ctx, left, right).coerceIn(1e-6, 1 - 1e-6)
                        val y = if (resp is ExplicitPreferenceResponse.PreferLeft) 1.0 else 0.0
                        preLL.getValue(n) += -(y * ln(q) + (1 - y) * ln(1 - q))
                        preBrier.getValue(n) += (q - y) * (q - y)
                        labelled.getValue(n)[0]++
                    }
                    c.observe(
                        PairwisePreferenceEvent(
                            "u$seed-t$t", "u$seed", "s$t", ctx, left, right, resp, "consent-1",
                            catalogue.contentVersion, ScopeEngine.MODEL_VERSION, t.toLong(),
                        )
                    )
                    val rec = c.recommend(ctx, catalogue.viewVariants.getValue(act)).effectiveVariant
                    val prev = lastRec.getValue(n).put(ctx, rec)
                    if (prev != null) {
                        switches.getValue(n)[1]++
                        if (prev != rec) switches.getValue(n)[0]++
                    }
                }
            }

            if ((t + 1) in Protocol.CHECKPOINTS) {
                controllers.forEachIndexed { ci, c ->
                    val n = names[ci]
                    out.getValue(n)[t + 1] = evaluate(c, catalogue, person, t).copy(
                        prequentialLogLoss = preLL.getValue(n).averageOrNaN(),
                        prequentialBrier = preBrier.getValue(n).averageOrNaN(),
                        labelledSoFar = labelled.getValue(n)[0],
                        asked = asked.getValue(n)[0],
                        switchRate = switches.getValue(n).let { if (it[1] == 0) 0.0 else it[0].toDouble() / it[1] },
                    )
                }
            }
        }
        return out
    }

    private fun List<Double>.averageOrNaN() = if (isEmpty()) Double.NaN else average()

    fun evaluate(c: Controller, cat: Catalogue, person: Person, t: Int): Metrics {
        var ce = 0.0; var ceN = 0
        var ceU = 0.0; var ceUN = 0
        var agree = 0.0
        var regret = 0.0; var regretN = 0
        var regretTop = 0.0
        var abstain = 0
        val calib = ArrayList<Pair<Double, Double>>()
        for (act in Protocol.ACTIVITIES) for (mode in SocialMode.entries) {
            val ctx = ContextSnapshot(act, mode)
            val view = cat.viewVariants.getValue(act)
            val u = view.map { person.utility(cat.truthOf(it.variantId), ctx, t) }
            for (i in view.indices) for (j in i + 1 until view.size) {
                val p = sigmoid(person.profile.scale * (u[i] - u[j]))
                val q = c.probLeft(ctx, view[i], view[j]).coerceIn(1e-6, 1 - 1e-6)
                val loss = -(p * ln(q) + (1 - p) * ln(1 - q))
                ce += loss; ceN++
                if (cat.isUnseen(view[i].variantId) || cat.isUnseen(view[j].variantId)) { ceU += loss; ceUN++ }
                agree += if (q == 0.5) 0.5 else if ((q > 0.5) == (p > 0.5)) 1.0 else 0.0
                calib += q to p
            }
            val rec = c.recommend(ctx, view)
            if (rec is Recommendation.Abstain) abstain++
            val k = view.indexOfFirst { it.variantId == rec.effectiveVariant }
            val max = u.max(); val min = u.min()
            val kt = view.indexOfFirst { it.variantId == c.topRanked(ctx, view) }
            if (max - min > 1e-9) {
                regret += (max - u[k]) / (max - min); regretN++
                regretTop += (max - u[kt]) / (max - min)
            }
        }
        val contexts = Protocol.ACTIVITIES.size * SocialMode.entries.size
        return Metrics(ce / ceN, ceU / ceUN, agree / ceN, regret / regretN, abstain.toDouble() / contexts, Double.NaN, Double.NaN, 0, 0, 0.0, regretTop / regretN, calib)
    }

    /** Paired bootstrap of the relative improvement (base − cand)/base in mean held-out CE. Returns (point, low95, high95). */
    fun pairedBootstrap(base: DoubleArray, cand: DoubleArray, resamples: Int = Protocol.BOOTSTRAP_RESAMPLES, seed: Long = Protocol.BOOTSTRAP_SEED): Triple<Double, Double, Double> {
        fun rel(idx: IntArray): Double {
            var b = 0.0; var c = 0.0
            for (i in idx) { b += base[i]; c += cand[i] }
            return (b - c) / b
        }
        val n = base.size
        val point = rel(IntArray(n) { it })
        val r = Rng(seed)
        val stats = DoubleArray(resamples) { rel(IntArray(n) { r.int(n) }) }.sorted()
        return Triple(point, stats[(0.025 * resamples).toInt()], stats[(0.975 * resamples).toInt() - 1])
    }
}
