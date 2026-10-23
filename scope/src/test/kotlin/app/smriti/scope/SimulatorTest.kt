package app.smriti.scope

import app.smriti.scope.sim.Catalogue
import app.smriti.scope.sim.Profile
import app.smriti.scope.sim.Protocol
import app.smriti.scope.sim.RuleController
import app.smriti.scope.sim.Simulator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulatorTest {
    @Test
    fun `same synthetic user and seed reproduce identical metrics`() {
        val a = Simulator.runUser(Profile.P2_CONTEXT_TRADEOFF, 3)
        val b = Simulator.runUser(Profile.P2_CONTEXT_TRADEOFF, 3)
        assertEquals(a.keys, b.keys)
        for (n in a.keys) for (cp in Protocol.CHECKPOINTS) {
            val x = a.getValue(n).getValue(cp); val y = b.getValue(n).getValue(cp)
            assertEquals(x.copy(calibration = emptyList()), y.copy(calibration = emptyList()))
        }
    }

    @Test
    fun `every controller only ever proposes approved variants of the person's chosen activity`() {
        val cat = Catalogue("content-1")
        for (factory in Simulator.FACTORIES) {
            val c = factory(cat.schema)
            for (act in Protocol.ACTIVITIES) for (mode in SocialMode.entries) {
                val ctx = ContextSnapshot(act, mode)
                val pool = cat.pool(act)
                c.observe(PairwisePreferenceEvent("e-${act.value}-$mode-${c.name}", "p", "s", ctx, pool[0], pool[1],
                    ExplicitPreferenceResponse.PreferLeft, "c", "content-1", ScopeEngine.MODEL_VERSION, 0))
                // Offer candidates from every activity: the proposal must still belong to the chosen one.
                val rec = c.recommend(ctx, cat.schema.variants)
                val v = cat.schema.variants.first { it.variantId == rec.effectiveVariant }
                assertEquals("${c.name} proposed outside the chosen activity", act, v.activityId)
                assertTrue(v.approved)
                c.choosePair(ctx, pool)?.let { (l, r) -> assertTrue(l.activityId == act && r.activityId == act && l != r) }
            }
        }
    }

    @Test
    fun `rule baseline ignores skips and both-fine like the model does`() {
        val cat = Catalogue("content-1")
        val act = Protocol.ACTIVITIES.first()
        val ctx = ContextSnapshot(act, SocialMode.SOLO)
        val pool = cat.pool(act)
        val r = RuleController(cat.schema, featureVotes = true)
        val before = r.probLeft(ctx, pool[0], pool[1])
        r.observe(PairwisePreferenceEvent("a", "p", "s", ctx, pool[0], pool[1], ExplicitPreferenceResponse.Skip, "c", "content-1", ScopeEngine.MODEL_VERSION, 0))
        r.observe(PairwisePreferenceEvent("b", "p", "s", ctx, pool[0], pool[1], ExplicitPreferenceResponse.BothFine, "c", "content-1", ScopeEngine.MODEL_VERSION, 0))
        assertEquals(before, r.probLeft(ctx, pool[0], pool[1]), 0.0)
    }

    @Test
    fun `paired bootstrap is deterministic and centred on the point estimate`() {
        val base = DoubleArray(40) { 0.6 + 0.01 * (it % 5) }
        val cand = DoubleArray(40) { base[it] * 0.9 }
        val (pt, lo, hi) = Simulator.pairedBootstrap(base, cand)
        assertEquals(0.1, pt, 1e-9)
        assertTrue(lo <= pt && pt <= hi)
        assertEquals(Simulator.pairedBootstrap(base, cand), Simulator.pairedBootstrap(base, cand))
    }

    @Test
    fun `catalogue split is fixed and pool and unseen variants never overlap`() {
        val a = Catalogue("content-1"); val b = Catalogue("content-1")
        assertEquals(a.schema.variants, b.schema.variants)
        for (act in Protocol.ACTIVITIES) {
            assertEquals(Protocol.POOL_SIZE, a.pool(act).size)
            assertTrue(a.pool(act).none { a.isUnseen(it.variantId) })
        }
    }
}
