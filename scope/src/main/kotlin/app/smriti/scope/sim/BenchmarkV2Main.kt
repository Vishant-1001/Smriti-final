package app.smriti.scope.sim

import app.smriti.scope.ContextSnapshot
import app.smriti.scope.DecisionRule
import app.smriti.scope.ExplicitPreferenceResponse
import app.smriti.scope.ModelConfig
import app.smriti.scope.PairwisePreferenceEvent
import app.smriti.scope.Recommendation
import app.smriti.scope.ScopeEngine
import app.smriti.scope.SocialMode
import app.smriti.scope.VariantFeatures
import app.smriti.scope.sigmoid
import java.io.File
import java.util.Locale
import java.util.Properties
import kotlin.math.abs
import kotlin.math.ln

/**
 * SCOPE protocol v2 (docs/scope/v2/SCOPE_PROTOCOL_v2.md).
 *   tune <propertiesOut> <reportOut>   development seeds 1–40 only
 *   test <propertiesIn>  <reportOut>   fresh held-out seeds 2001–2060, frozen policy
 */
object V2 {
    val DEV_SEEDS = (1L..40L).toList()
    val TEST_SEEDS = (2001L..2060L).toList()
    const val CHECKPOINT = 16
    val TAUS = listOf(0.80, 0.85, 0.90, 0.95, 0.975)
    val MARGINS = listOf(0.0, 0.25, 0.5)
    val RULE_K = listOf(0.25, 0.5, 1.0, 1.5)
    val RULE_W = listOf(0.5, 1.0, 2.0, 3.0)
    const val MAX_HARM = 0.05
    val FAMILIES = linkedMapOf(
        "B2" to ModelConfig.NON_CONTEXTUAL,
        "B3-lite" to ModelConfig.LITE,
        "B3" to ModelConfig.FULL_SCOPE,
    )

    /** The shared passive event stream up to [upTo], built exactly as Simulator.runUser builds it. */
    fun passiveStream(profile: Profile, seed: Long, upTo: Int): Triple<Catalogue, Person, List<PairwisePreferenceEvent>> {
        val cat = Catalogue("content-1", masked = profile == Profile.P9_PARTIAL_METADATA)
        val person = Person(profile, seed)
        val events = ArrayList<PairwisePreferenceEvent>()
        for (t in 0 until upTo) {
            val ctxRng = Rng(seed * 7919 + t)
            val act = Protocol.ACTIVITIES[ctxRng.int(Protocol.ACTIVITIES.size)]
            val mode = if (ctxRng.double() < 0.5) SocialMode.SOLO else SocialMode.TOGETHER
            val ctx = ContextSnapshot(act, mode)
            val pool = cat.pool(act)
            val i = ctxRng.int(pool.size)
            var j = ctxRng.int(pool.size - 1)
            if (j >= i) j++
            val left = pool[i]; val right = pool[j]
            val respRng = Rng(seed * 31337 + t * 977 + (left.variantId.value + "|" + right.variantId.value).hashCode())
            val resp = person.respond(cat.truthOf(left.variantId), cat.truthOf(right.variantId), ctx, t, respRng)
            events += PairwisePreferenceEvent("u$seed-t$t", "u$seed", "s$t", ctx, left, right, resp, "consent-1", cat.contentVersion, ScopeEngine.MODEL_VERSION, t.toLong())
        }
        return Triple(cat, person, events)
    }

    data class Decision(var gain: Double = 0.0, var contexts: Int = 0, var proposals: Int = 0, var harmful: Int = 0) {
        fun add(o: Decision) { gain += o.gain; contexts += o.contexts; proposals += o.proposals; harmful += o.harmful }
        val meanGain get() = gain / contexts
        val harmRate get() = if (proposals == 0) 0.0 else harmful.toDouble() / proposals
        val coverage get() = proposals.toDouble() / contexts
    }

    fun decide(cat: Catalogue, person: Person, t: Int, rec: (ContextSnapshot, List<VariantFeatures>) -> Recommendation): Decision {
        val d = Decision()
        for (act in Protocol.ACTIVITIES) for (mode in SocialMode.entries) {
            val ctx = ContextSnapshot(act, mode)
            val view = cat.viewVariants.getValue(act)
            val u = view.map { person.utility(cat.truthOf(it.variantId), ctx, t) }
            val r = rec(ctx, view)
            val k = view.indexOfFirst { it.variantId == r.effectiveVariant }
            val km = view.indexOfFirst { it.variantId == cat.schema.manualDefaults.getValue(act) }
            val range = u.max() - u.min()
            if (range > 1e-9) d.gain += (u[k] - u[km]) / range
            d.contexts++
            if (r is Recommendation.Propose) { d.proposals++; if (u[k] < u[km]) d.harmful++ }
        }
        return d
    }

    fun heldoutCE(cat: Catalogue, person: Person, t: Int, prob: (ContextSnapshot, VariantFeatures, VariantFeatures) -> Double): Double {
        var ce = 0.0; var n = 0
        for (act in Protocol.ACTIVITIES) for (mode in SocialMode.entries) {
            val ctx = ContextSnapshot(act, mode)
            val view = cat.viewVariants.getValue(act)
            val u = view.map { person.utility(cat.truthOf(it.variantId), ctx, t) }
            for (i in view.indices) for (j in i + 1 until view.size) {
                val p = sigmoid(person.profile.scale * (u[i] - u[j]))
                val q = prob(ctx, view[i], view[j]).coerceIn(1e-6, 1 - 1e-6)
                ce += -(p * ln(q) + (1 - p) * ln(1 - q)); n++
            }
        }
        return ce / n
    }
}

fun main(args: Array<String>) {
    Locale.setDefault(Locale.ROOT)
    when (args.getOrNull(0)) {
        "tune" -> tune(File(args[1]), File(args[2]))
        "test" -> test(File(args[1]), File(args[2]))
        else -> error("usage: tune|test <properties> <report>")
    }
}

private fun f(x: Double, d: Int = 3) = if (x.isNaN()) "–" else "%.${d}f".format(x)
private fun pct(x: Double) = "%+.1f%%".format(100 * x)

private fun tune(propsOut: File, reportOut: File) {
    val t = V2.CHECKPOINT - 1
    val bayes = V2.FAMILIES.keys.associateWith { fam -> V2.TAUS.flatMap { tau -> V2.MARGINS.map { m -> (tau to m) to V2.Decision() } }.toMap() }
    val ruleCE = listOf(false, true).associateWith { fv -> V2.RULE_K.associateWith { 0.0 } .toMutableMap() }
    val ruleDec = listOf(false, true).associateWith { fv -> V2.RULE_K.flatMap { k -> V2.RULE_W.map { w -> (k to w) to V2.Decision() } }.toMap() }
    val bayesCE = V2.FAMILIES.keys.associateWith { 0.0 }.toMutableMap()
    var users = 0
    for (profile in Profile.entries) for (seed in V2.DEV_SEEDS) {
        users++
        val (cat, person, events) = V2.passiveStream(profile, seed, V2.CHECKPOINT)
        for ((fam, cfg) in V2.FAMILIES) {
            val e = ScopeEngine(cat.schema, cfg, enabled = true, optedIn = true)
            events.forEach { e.update(it) }
            bayesCE[fam] = bayesCE.getValue(fam) + V2.heldoutCE(cat, person, t) { c, a, b -> e.probPrefer(c, a, b) }
            for (tau in V2.TAUS) for (m in V2.MARGINS) {
                bayes.getValue(fam).getValue(tau to m).add(V2.decide(cat, person, t) { c, v -> e.recommendWith(DecisionRule.VersusManual(tau, m), c, v) })
            }
        }
        for (fv in listOf(false, true)) for (k in V2.RULE_K) {
            val ce = RuleController(cat.schema, fv, probScale = k).also { r -> events.forEach { r.observe(it) } }
            ruleCE.getValue(fv)[k] = ruleCE.getValue(fv).getValue(k) + V2.heldoutCE(cat, person, t) { c, a, b -> ce.probLeft(c, a, b) }
            for (w in V2.RULE_W) {
                val r = RuleController(cat.schema, fv, probScale = k, manualMargin = w).also { r -> events.forEach { r.observe(it) } }
                ruleDec.getValue(fv).getValue(k to w).add(V2.decide(cat, person, t) { c, v -> r.recommend(c, v) })
            }
        }
    }

    fun pick(cands: Map<Pair<Double, Double>, V2.Decision>, conservativeOrder: Comparator<Pair<Double, Double>>): Pair<Double, Double> {
        val feasible = cands.filter { it.value.harmRate <= V2.MAX_HARM }
        if (feasible.isEmpty()) return cands.keys.sortedWith(conservativeOrder).last()
        return feasible.entries.sortedWith(compareByDescending<Map.Entry<Pair<Double, Double>, V2.Decision>> { it.value.meanGain }.thenComparator { a, b -> conservativeOrder.compare(b.key, a.key) }).first().key
    }
    val consB = compareBy<Pair<Double, Double>>({ it.first }, { it.second })
    val chosenBayes = V2.FAMILIES.keys.associateWith { pick(bayes.getValue(it), consB) }
    val chosenK = listOf(false, true).associateWith { fv -> V2.RULE_K.minBy { ruleCE.getValue(fv).getValue(it) } }
    val chosenRule = listOf(false, true).associateWith { fv ->
        val k = chosenK.getValue(fv)
        pick(ruleDec.getValue(fv).filterKeys { it.first == k }, compareBy({ it.second }, { it.first })).second
    }
    val primary = V2.FAMILIES.keys.maxBy { fam -> bayes.getValue(fam).getValue(chosenBayes.getValue(fam)).let { if (it.harmRate <= V2.MAX_HARM) it.meanGain else -1.0 } }

    val props = Properties()
    for (fam in V2.FAMILIES.keys) {
        props["$fam.tau"] = chosenBayes.getValue(fam).first.toString()
        props["$fam.margin"] = chosenBayes.getValue(fam).second.toString()
    }
    props["B1.k"] = chosenK.getValue(false).toString(); props["B1.w"] = chosenRule.getValue(false).toString()
    props["B1F.k"] = chosenK.getValue(true).toString(); props["B1F.w"] = chosenRule.getValue(true).toString()
    props["primary"] = primary
    propsOut.parentFile?.mkdirs()
    propsOut.outputStream().use { props.store(it, "SCOPE v2 frozen policy, chosen on DEVELOPMENT seeds 1-40 only (do not edit by hand)") }

    val sb = StringBuilder()
    sb.appendLine("# SCOPE v2 development tuning (seeds 1–40 only)")
    sb.appendLine()
    sb.appendLine("Generated by `./gradlew :scope:runScopeV2Tuning`. ${users} synthetic development people (10 profiles × ${V2.DEV_SEEDS.size}), budget ${V2.CHECKPOINT}, passive shared streams. Objective: maximise pooled mean utility gain over manual subject to harmful-proposal rate ≤ ${pct(V2.MAX_HARM).removePrefix("+")}. **No held-out seed was used.**")
    sb.appendLine()
    sb.appendLine("## SCOPE model families (development)")
    sb.appendLine()
    sb.appendLine("| family | dev held-out CE | τ | margin | mean gain | coverage | harmful rate | proposals |")
    sb.appendLine("|---|---|---|---|---|---|---|---|")
    for (fam in V2.FAMILIES.keys) for ((key, d) in bayes.getValue(fam)) {
        val mark = if (key == chosenBayes.getValue(fam)) " **← frozen**" else ""
        sb.appendLine("| $fam | ${f(bayesCE.getValue(fam) / users)} | ${key.first} | ${key.second} | ${f(d.meanGain)} | ${f(d.coverage)} | ${f(d.harmRate)} | ${d.proposals}$mark |")
    }
    sb.appendLine()
    sb.appendLine("## Rule baselines (development)")
    sb.appendLine()
    sb.appendLine("| baseline | k | dev held-out CE | w | mean gain | coverage | harmful rate |")
    sb.appendLine("|---|---|---|---|---|---|---|")
    for (fv in listOf(false, true)) for ((key, d) in ruleDec.getValue(fv)) {
        val name = if (fv) "B1F" else "B1"
        val mark = if (key.first == chosenK.getValue(fv) && key.second == chosenRule.getValue(fv)) " **← frozen**" else ""
        sb.appendLine("| $name | ${key.first} | ${f(ruleCE.getValue(fv).getValue(key.first) / users)} | ${key.second} | ${f(d.meanGain)} | ${f(d.coverage)} | ${f(d.harmRate)}$mark |")
    }
    sb.appendLine()
    sb.appendLine("**Primary SCOPE family for the v2 gate: `$primary`** (best development objective).")
    reportOut.parentFile?.mkdirs()
    reportOut.writeText(sb.toString())
    println(sb)
}

private fun test(propsIn: File, reportOut: File) {
    val p = Properties().apply { propsIn.inputStream().use { load(it) } }
    fun d(k: String) = p.getProperty(k).toDouble()
    val primary = p.getProperty("primary")
    val names = mutableListOf<String>()
    val factories = mutableListOf<ControllerFactory>()
    fun add(n: String, fac: ControllerFactory) { names += n; factories += fac }
    add("B0-fixed") { s -> FixedController(s) }
    add("B1-cal") { s -> RuleController(s, false, probScale = d("B1.k"), manualMargin = d("B1.w"), label = "B1-cal") }
    add("B1F-cal") { s -> RuleController(s, true, probScale = d("B1F.k"), manualMargin = d("B1F.w"), label = "B1F-cal") }
    for ((fam, cfg) in V2.FAMILIES) add("$fam-v2") { s -> BayesController(s, cfg.copy(decision = DecisionRule.VersusManual(d("$fam.tau"), d("$fam.margin"))), label = "$fam-v2") }
    add("B3-v1rule") { s -> BayesController(s, ModelConfig.FULL_SCOPE, label = "B3-v1rule") }
    val prim = "$primary-v2"

    val sb = StringBuilder()
    sb.appendLine("# SCOPE v2 benchmark — fresh held-out results")
    sb.appendLine()
    sb.appendLine("Generated by `./gradlew :scope:runScopeV2Test` with the policy frozen in `scope_v2_frozen.properties` (chosen on development seeds 1–40). Fresh held-out seeds ${V2.TEST_SEEDS.first()}–${V2.TEST_SEEDS.last()} (${V2.TEST_SEEDS.size} per profile). **Synthetic simulation only. Not evidence about any real person.**")
    sb.appendLine()
    sb.appendLine("Frozen policy: " + p.stringPropertyNames().sorted().joinToString(", ") { "$it=${p.getProperty(it)}" } + ". Primary SCOPE model: **$prim**.")
    sb.appendLine()

    data class Agg(val gain: DoubleArray, val regret: DoubleArray, val ce: DoubleArray, val brier: DoubleArray, val proposals: Int, val harmful: Int, val contexts: Int, val switch: Double, val skips: Double, val asked: Double, val abstain: Double, val calib: List<Pair<Double, Double>>)
    val all = LinkedHashMap<Profile, Map<String, Agg>>()
    for (profile in Profile.entries) {
        val runs = V2.TEST_SEEDS.map { Simulator.runUser(profile, it, factories) }
        all[profile] = names.associateWith { n ->
            val ms = runs.map { it.getValue(n).getValue(V2.CHECKPOINT) }
            Agg(ms.map { it.utilityGain }.toDoubleArray(), ms.map { it.normRegret }.toDoubleArray(), ms.map { it.heldoutCE }.toDoubleArray(),
                ms.map { it.heldoutBrier }.toDoubleArray(), ms.sumOf { it.proposals }, ms.sumOf { it.harmfulProposals }, ms.sumOf { it.contexts },
                ms.map { it.switchRate }.average(), ms.map { it.skips.toDouble() }.average(), ms.map { it.asked.toDouble() }.average(),
                ms.map { it.abstainRate }.average(), ms.flatMap { it.calibration })
        }
    }
    fun ece(c: List<Pair<Double, Double>>): Double {
        var e = 0.0
        for (b in 0 until 10) {
            val inB = c.filter { it.first >= b / 10.0 && (it.first < (b + 1) / 10.0 || b == 9) }
            if (inB.isNotEmpty()) e += inB.size.toDouble() / c.size * abs(inB.map { it.first }.average() - inB.map { it.second }.average())
        }
        return e
    }
    for ((profile, aggs) in all) {
        sb.appendLine("## ${profile.label}")
        sb.appendLine()
        sb.appendLine("| controller | utility gain | norm. regret | coverage | harmful (overrides) / proposals | held-out CE | held-out Brier | ECE | switch rate | asked | skips |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|---|---|")
        for ((n, a) in aggs) sb.appendLine("| $n | ${f(a.gain.average())} | ${f(a.regret.average())} | ${f(a.proposals.toDouble() / a.contexts)} | ${a.harmful}/${a.proposals} (${f(if (a.proposals == 0) 0.0 else a.harmful.toDouble() / a.proposals)}) | ${f(a.ce.average())} | ${f(a.brier.average())} | ${f(ece(a.calib))} | ${f(a.switch)} | ${f(a.asked, 1)} | ${f(a.skips, 1)} |")
        sb.appendLine()
        sb.appendLine("Paired bootstrap 95% CIs for **$prim** minus comparator:")
        sb.appendLine()
        sb.appendLine("| vs | utility-gain diff | regret reduction | CE rel. improvement |")
        sb.appendLine("|---|---|---|---|")
        val pa = aggs.getValue(prim)
        for (cmp in listOf("B1-cal", "B1F-cal", "B0-fixed") + V2.FAMILIES.keys.map { "$it-v2" }.filter { it != prim }) {
            val b = aggs.getValue(cmp)
            val (g, gl, gh) = Simulator.pairedBootstrapDiff(b.gain, pa.gain)
            val rr = if (b.regret.average() > 0) (b.regret.average() - pa.regret.average()) / b.regret.average() else 0.0
            val (c, cl, ch) = Simulator.pairedBootstrap(b.ce, pa.ce)
            sb.appendLine("| $cmp | ${f(g)} [${f(gl)}, ${f(gh)}] | ${pct(rr)} | ${pct(c)} [${pct(cl)}, ${pct(ch)}] |")
        }
        sb.appendLine()
    }

    // ---- pre-registered v2 gates
    val pAll = all.values.map { it.getValue(prim) }
    val pooledHarm = pAll.sumOf { it.harmful }.toDouble() / pAll.sumOf { it.proposals }.coerceAtLeast(1)
    val worstProfileHarm = all.entries.maxOf { (_, a) -> a.getValue(prim).let { if (it.proposals == 0) 0.0 else it.harmful.toDouble() / it.proposals } }
    val g1 = pooledHarm <= 0.05 && worstProfileHarm <= 0.10
    val p2 = all.getValue(Profile.P2_CONTEXT_TRADEOFF)
    val p2cov = p2.getValue(prim).let { it.proposals.toDouble() / it.contexts }
    val pooledCov = pAll.sumOf { it.proposals }.toDouble() / pAll.sumOf { it.contexts }
    val g2 = p2cov >= 0.25 && pooledCov >= 0.15
    val g3Rows = listOf("B1-cal", "B1F-cal").map { cmp ->
        val b = p2.getValue(cmp); val a = p2.getValue(prim)
        val (_, gl, _) = Simulator.pairedBootstrapDiff(b.gain, a.gain)
        val rr = (b.regret.average() - a.regret.average()) / b.regret.average()
        val (c, cl, _) = Simulator.pairedBootstrap(b.ce, a.ce)
        Triple(cmp, gl > 0 && rr >= 0.10 && c >= 0.05 && cl > 0, "gain-diff CI low ${f(gl)}, regret red. ${pct(rr)}, CE ${pct(c)} (CI low ${pct(cl)})")
    }
    val g3 = g3Rows.all { it.second }
    val g4Fail = mutableListOf<String>()
    for (pr in listOf(Profile.P1_STABLE_ADDITIVE, Profile.P8_NO_CONTEXT_WEAK, Profile.P9_PARTIAL_METADATA)) for (cmp in listOf("B1-cal", "B1F-cal")) {
        val (_, _, gh) = Simulator.pairedBootstrapDiff(all.getValue(pr).getValue(cmp).gain, all.getValue(pr).getValue(prim).gain)
        if (gh < 0) g4Fail += "${pr.label} vs $cmp"
    }
    val g4 = g4Fail.isEmpty()
    val g5 = listOf("B1-cal", "B1F-cal").all { p2.getValue(prim).switch <= p2.getValue(it).switch + 0.05 }

    sb.appendLine("## Calibration buckets (all profiles)")
    sb.appendLine()
    sb.appendLine("| controller | ECE | " + (0 until 5).joinToString(" | ") { "[${f(it * 0.2, 1)},${f(it * 0.2 + 0.2, 1)}) pred→true" } + " |")
    sb.appendLine("|---|---|" + (0 until 5).joinToString("") { "---|" })
    for (n in listOf(prim, "B1-cal", "B1F-cal")) {
        val c = all.values.flatMap { it.getValue(n).calib }
        sb.appendLine("| $n | ${f(ece(c))} | " + (0 until 5).joinToString(" | ") { b ->
            val inB = c.filter { it.first >= b * 0.2 && (it.first < b * 0.2 + 0.2 || b == 4) }
            if (inB.isEmpty()) "–" else "${f(inB.map { it.first }.average(), 2)}→${f(inB.map { it.second }.average(), 2)}"
        } + " |")
    }
    sb.appendLine()
    sb.appendLine("## Pre-registered v2 go / no-go for $prim")
    sb.appendLine()
    sb.appendLine("| gate | result | detail |")
    sb.appendLine("|---|---|---|")
    sb.appendLine("| G1 safety | ${if (g1) "pass" else "**FAIL**"} | pooled harmful rate ${f(pooledHarm)}; worst profile ${f(worstProfileHarm)} |")
    sb.appendLine("| G2 coverage | ${if (g2) "pass" else "**FAIL**"} | P2 coverage ${f(p2cov)}; pooled ${f(pooledCov)} |")
    for ((cmp, ok, det) in g3Rows) sb.appendLine("| G3 P2 vs $cmp | ${if (ok) "pass" else "**FAIL**"} | $det |")
    sb.appendLine("| G4 no significant loss (P1/P8/P9) | ${if (g4) "pass" else "**FAIL**"} | ${if (g4) "none" else g4Fail.joinToString()} |")
    sb.appendLine("| G5 stability on P2 | ${if (g5) "pass" else "**FAIL**"} | switch ${f(p2.getValue(prim).switch)} vs B1 ${f(p2.getValue("B1-cal").switch)}, B1F ${f(p2.getValue("B1F-cal").switch)} |")
    sb.appendLine()
    val go = g1 && g2 && g3 && g4 && g5
    sb.appendLine(if (go) "**v2 VERDICT: GO under the pre-registered synthetic criteria.** This concerns synthetic users only; it is not evidence of benefit for any real person. SCOPE remains disabled by default and outside the app."
        else "**v2 VERDICT: NO-GO.** At least one pre-registered gate failed (see above). SCOPE must not be described as better than the rule baseline and remains disabled.")
    reportOut.parentFile?.mkdirs()
    reportOut.writeText(sb.toString())
    println(sb)
}
