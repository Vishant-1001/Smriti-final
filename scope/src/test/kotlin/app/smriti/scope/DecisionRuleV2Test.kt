package app.smriti.scope

import app.smriti.scope.sim.Profile
import app.smriti.scope.sim.RuleController
import app.smriti.scope.sim.Simulator
import app.smriti.scope.sim.V2
import app.smriti.scope.sim.BayesController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionRuleV2Test {
    private val still = Fx.byId("c_still_brief_vis")
    private val moving = Fx.byId("c_moving_long_aud")
    private val manual = Fx.byId("c_mid")
    private val v2 = ModelConfig.FULL_SCOPE.copy(decision = DecisionRule.VersusManual(tau = 0.9, margin = 0.0))

    @Test
    fun `v1 rule stays the default so the original benchmark is reproducible`() {
        assertEquals(DecisionRule.TopTwoPredictive(0.70), ModelConfig.FULL_SCOPE.decision)
    }

    @Test
    fun `normal cdf is accurate`() {
        assertEquals(0.5, normalCdf(0.0), 1e-7)
        assertEquals(0.975, normalCdf(1.959964), 1e-6)
        assertEquals(0.025, normalCdf(-1.959964), 1e-6)
    }

    @Test
    fun `posterior probability is not capped by response noise, predictive probability is`() {
        val e = Fx.engine()
        repeat(200) { e.update(Fx.event(Fx.cook(), still, manual, ExplicitPreferenceResponse.PreferLeft)) }
        val p = e.currentPosterior
        val (m, _) = p.difference(Fx.cook(), still, manual)
        assertTrue(p.probPrefer(Fx.cook(), still, manual) <= sigmoid(m) + 1e-12)
        assertTrue(p.probBetter(Fx.cook(), still, manual) > 0.99)
    }

    @Test
    fun `versus-manual proposes only with posterior evidence of beating manual`() {
        val e = Fx.engine(v2)
        assertEquals(AbstainReason.NO_EVIDENCE, (e.recommendUnchecked(Fx.cook(), Fx.cookVariants) as Recommendation.Abstain).reason)
        repeat(3) { for (o in Fx.cookVariants) if (o != still) e.update(Fx.event(Fx.cook(), still, o, ExplicitPreferenceResponse.PreferLeft)) }
        val rec = e.recommendUnchecked(Fx.cook(), Fx.cookVariants)
        assertTrue(rec is Recommendation.Propose)
        assertEquals(still.variantId, rec.effectiveVariant)
        assertTrue((rec as Recommendation.Propose).confidence >= 0.9)
    }

    @Test
    fun `keeps the accepted variant when it is already the best or evidence is mixed`() {
        val e = Fx.engine(v2)
        repeat(3) { for (o in Fx.cookVariants) if (o != manual) e.update(Fx.event(Fx.cook(), manual, o, ExplicitPreferenceResponse.PreferLeft)) }
        assertEquals(AbstainReason.ACCEPTED_IS_BEST, (e.recommendUnchecked(Fx.cook(), Fx.cookVariants) as Recommendation.Abstain).reason)
        val mixed = Fx.engine(v2)
        mixed.update(Fx.event(Fx.cook(), still, manual, ExplicitPreferenceResponse.PreferLeft))
        mixed.update(Fx.event(Fx.cook(), still, manual, ExplicitPreferenceResponse.PreferRight))
        val r = mixed.recommendUnchecked(Fx.cook(), Fx.cookVariants)
        assertTrue(r is Recommendation.Abstain)
        assertEquals(manual.variantId, r.effectiveVariant)
    }

    @Test
    fun `the comparison is against the currently accepted variant when one is given`() {
        val e = Fx.engine(v2)
        repeat(3) { for (o in Fx.cookVariants) if (o != still) e.update(Fx.event(Fx.cook(), still, o, ExplicitPreferenceResponse.PreferLeft)) }
        val r = e.recommendUnchecked(Fx.cook(), Fx.cookVariants, accepted = still.variantId)
        assertEquals(AbstainReason.ACCEPTED_IS_BEST, (r as Recommendation.Abstain).reason)
        assertEquals(still.variantId, r.effectiveVariant)
    }

    @Test
    fun `calibrated rule baseline proposes only when beating manual by the margin`() {
        val ctx = Fx.cook()
        // One win gives a score gap of 2 (+1 / −1); two wins give 1.9 − (−1.9) = 3.8 with 0.9 recency weighting.
        val r = RuleController(Fx.schema, featureVotes = false, manualMargin = 3.0)
        r.observe(Fx.event(ctx, still, manual, ExplicitPreferenceResponse.PreferLeft))
        assertTrue(r.recommend(ctx, Fx.cookVariants) is Recommendation.Abstain)
        r.observe(Fx.event(ctx, still, manual, ExplicitPreferenceResponse.PreferLeft))
        assertEquals(still.variantId, (r.recommend(ctx, Fx.cookVariants) as Recommendation.Propose).variantId)
    }

    @Test
    fun `tuning stream is exactly the simulator's passive stream`() {
        val seed = 5L
        val (cat, person, events) = V2.passiveStream(Profile.P2_CONTEXT_TRADEOFF, seed, 16)
        val e = ScopeEngine(cat.schema, ModelConfig.FULL_SCOPE, enabled = true, optedIn = true).apply { events.forEach { update(it) } }
        val viaStream = V2.heldoutCE(cat, person, 15) { c, a, b -> e.probPrefer(c, a, b) }
        val viaSim = Simulator.runUser(Profile.P2_CONTEXT_TRADEOFF, seed, listOf({ s -> BayesController(s, ModelConfig.FULL_SCOPE) }))
            .values.first().getValue(16).heldoutCE
        assertEquals(viaSim, viaStream, 1e-12)
    }
}
