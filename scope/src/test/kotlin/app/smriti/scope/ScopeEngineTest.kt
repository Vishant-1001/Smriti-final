package app.smriti.scope

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

internal object Fx {
    val COOK = ActivityId("cooking")
    val SEW = ActivityId("sewing")
    const val SCHEMA = "features-1"
    const val CONTENT = "content-1"

    fun v(id: String, act: ActivityId, vd: Double?, mo: Double?, na: Double?, mod: InstructionModality?, content: String = CONTENT, approved: Boolean = true) =
        VariantFeatures(VariantId(id), act, content, SCHEMA, vd, mo, na, mod, approved = approved)

    val cookVariants = listOf(
        v("c_still_brief_vis", COOK, 0.2, 0.0, 0.0, InstructionModality.VISUAL),
        v("c_moving_long_aud", COOK, 0.8, 1.0, 1.0, InstructionModality.AUDIO_SUPPORTED),
        v("c_still_long_aud", COOK, 0.5, 0.0, 1.0, InstructionModality.AUDIO_SUPPORTED),
        v("c_moving_brief_vis", COOK, 0.5, 1.0, 0.0, InstructionModality.VISUAL),
        v("c_mid", COOK, 0.5, 0.5, 0.5, InstructionModality.VISUAL),
    )
    val sewVariants = listOf(
        v("s_still", SEW, 0.3, 0.0, 0.5, InstructionModality.VISUAL),
        v("s_moving", SEW, 0.7, 1.0, 0.5, InstructionModality.AUDIO_SUPPORTED),
        v("s_mid", SEW, 0.5, 0.5, 0.5, InstructionModality.VISUAL),
    )
    val schema = VariantSchema(SCHEMA, CONTENT, listOf(COOK, SEW), cookVariants + sewVariants,
        mapOf(COOK to VariantId("c_mid"), SEW to VariantId("s_mid")))

    fun engine(config: ModelConfig = ModelConfig.FULL_SCOPE) = ScopeEngine(schema, config, enabled = true, optedIn = true)

    private var counter = 0
    fun event(ctx: ContextSnapshot, left: VariantFeatures, right: VariantFeatures, r: ExplicitPreferenceResponse, id: String = "e${counter++}", content: String = CONTENT, model: String = ScopeEngine.MODEL_VERSION) =
        PairwisePreferenceEvent(id, "p1", "s1", ctx, left, right, r, "consent-1", content, model, 0L)

    fun cook(mode: SocialMode? = SocialMode.SOLO) = ContextSnapshot(COOK, mode)
    fun byId(id: String) = schema.variants.first { it.variantId.value == id }
}

class ScopeEngineTest {
    private val still = Fx.byId("c_still_brief_vis")
    private val moving = Fx.byId("c_moving_long_aud")

    @Test
    fun `disabled by default never proposes or asks`() {
        val e = ScopeEngine(Fx.schema)
        assertFalse(ScopeFlags.ENABLED_BY_DEFAULT)
        assertFalse(e.enabled)
        val rec = e.recommend(Fx.cook(), Fx.cookVariants)
        assertEquals(AbstainReason.DISABLED, (rec as Recommendation.Abstain).reason)
        assertEquals(VariantId("c_mid"), rec.effectiveVariant)
        val q = e.selectComparison(Fx.cook(), Fx.cookVariants, QueryBurdenState(true, false, false, 0, 0))
        assertEquals(ComparisonDecision.DoNotAsk(DoNotAskReason.DISABLED), q)
    }

    @Test
    fun `explicit left and right answers update the preference`() {
        val e = Fx.engine()
        assertEquals(0.5, e.probPrefer(Fx.cook(), still, moving), 1e-12)
        repeat(4) { assertEquals(UpdateResult.Learned, e.update(Fx.event(Fx.cook(), still, moving, ExplicitPreferenceResponse.PreferLeft))) }
        assertTrue(e.probPrefer(Fx.cook(), still, moving) > 0.75)
        e.update(Fx.event(Fx.cook(), moving, still, ExplicitPreferenceResponse.PreferRight))
        assertEquals(5, e.diagnostics().labelledComparisons)
    }

    @Test
    fun `skip and both-fine never fabricate a preference`() {
        val e = Fx.engine()
        e.update(Fx.event(Fx.cook(), still, moving, ExplicitPreferenceResponse.PreferLeft))
        val before = e.currentPosterior
        assertEquals(UpdateResult.RecordedNoPreference, e.update(Fx.event(Fx.cook(), still, moving, ExplicitPreferenceResponse.Skip)))
        assertEquals(UpdateResult.RecordedNoPreference, e.update(Fx.event(Fx.cook(), moving, still, ExplicitPreferenceResponse.BothFine)))
        assertArrayEquals(before.mean, e.currentPosterior.mean, 0.0)
        assertEquals(1, e.diagnostics().labelledComparisons)
        assertEquals(2, e.diagnostics().noPreferenceAnswers)
        // Only skips and both-fine: the model has no evidence at all.
        val f = Fx.engine()
        repeat(5) { f.update(Fx.event(Fx.cook(), still, moving, ExplicitPreferenceResponse.Skip)) }
        f.update(Fx.event(Fx.cook(), still, moving, ExplicitPreferenceResponse.BothFine))
        assertTrue(f.currentPosterior.mean.all { it == 0.0 })
        assertEquals(AbstainReason.NO_EVIDENCE, (f.recommend(Fx.cook(), Fx.cookVariants) as Recommendation.Abstain).reason)
    }

    @Test
    fun `reset all and reset one activity`() {
        val e = Fx.engine()
        val s1 = Fx.byId("s_still"); val s2 = Fx.byId("s_moving")
        repeat(3) { e.update(Fx.event(Fx.cook(), still, moving, ExplicitPreferenceResponse.PreferLeft)) }
        repeat(3) { e.update(Fx.event(ContextSnapshot(Fx.SEW, null), s2, s1, ExplicitPreferenceResponse.PreferLeft)) }
        e.reset(PreferenceResetScope.Activity(Fx.SEW))
        assertEquals(3, e.diagnostics().labelledComparisons)
        assertTrue(e.events.none { it.activityId == Fx.SEW })
        e.reset(PreferenceResetScope.All)
        assertEquals(0, e.diagnostics().totalEvents)
        assertTrue(e.currentPosterior.mean.all { it == 0.0 })
    }

    @Test
    fun `replay of the same event log is bit-for-bit deterministic`() {
        val e = Fx.engine()
        val ctxs = listOf(Fx.cook(SocialMode.SOLO), Fx.cook(SocialMode.TOGETHER), Fx.cook(null))
        val vs = Fx.cookVariants
        var k = 0
        for (i in vs.indices) for (j in vs.indices) if (i < j) {
            val r = if ((i + j) % 3 == 0) ExplicitPreferenceResponse.PreferRight else if ((i + j) % 3 == 1) ExplicitPreferenceResponse.PreferLeft else ExplicitPreferenceResponse.Skip
            e.update(Fx.event(ctxs[k++ % 3], vs[i], vs[j], r))
        }
        val a = ScopeEngine(Fx.schema, enabled = true, optedIn = true).apply { replay(e.events) }
        val b = ScopeEngine(Fx.schema, enabled = true, optedIn = true).apply { replay(e.events) }
        assertArrayEquals(e.currentPosterior.mean, a.currentPosterior.mean, 0.0)
        assertArrayEquals(a.currentPosterior.mean, b.currentPosterior.mean, 0.0)
        for (r in a.currentPosterior.covariance.indices) assertArrayEquals(a.currentPosterior.covariance[r], b.currentPosterior.covariance[r], 0.0)
        assertEquals(a.recommendUnchecked(Fx.cook(), vs), b.recommendUnchecked(Fx.cook(), vs))
    }

    @Test
    fun `abstains with the manual fallback when evidence is absent or contradictory`() {
        val e = Fx.engine()
        val none = e.recommend(Fx.cook(), Fx.cookVariants) as Recommendation.Abstain
        assertEquals(AbstainReason.NO_EVIDENCE, none.reason)
        assertEquals(VariantId("c_mid"), none.fallback)
        e.update(Fx.event(Fx.cook(), still, moving, ExplicitPreferenceResponse.PreferLeft))
        e.update(Fx.event(Fx.cook(), still, moving, ExplicitPreferenceResponse.PreferRight))
        val mixed = e.recommend(Fx.cook(), Fx.cookVariants)
        assertTrue(mixed is Recommendation.Abstain)
        assertEquals(AbstainReason.UNCERTAIN, (mixed as Recommendation.Abstain).reason)
        assertEquals(VariantId("c_mid"), mixed.effectiveVariant)
    }

    @Test
    fun `strong consistent evidence produces a proposal, never auto-applied`() {
        val e = Fx.engine()
        val vs = Fx.cookVariants
        // The person consistently prefers still, brief, visual presentation when solo.
        repeat(3) {
            for (other in vs) if (other != still) e.update(Fx.event(Fx.cook(), still, other, ExplicitPreferenceResponse.PreferLeft))
        }
        val rec = e.recommend(Fx.cook(), vs)
        assertTrue(rec is Recommendation.Propose)
        assertEquals(still.variantId, rec.effectiveVariant)
    }

    @Test
    fun `version mismatches fail closed`() {
        val e = Fx.engine()
        val r = e.update(Fx.event(Fx.cook(), still, moving, ExplicitPreferenceResponse.PreferLeft, content = "content-2"))
        assertTrue(r is UpdateResult.Rejected)
        assertTrue(e.update(Fx.event(Fx.cook(), still, moving, ExplicitPreferenceResponse.PreferLeft, model = "other-model")) is UpdateResult.Rejected)
        val newer = Fx.v("c_new", Fx.COOK, 0.1, 0.1, 0.1, InstructionModality.VISUAL, content = "content-2")
        assertEquals(AbstainReason.VERSION_MISMATCH, (e.recommend(Fx.cook(), Fx.cookVariants + newer) as Recommendation.Abstain).reason)
        assertEquals(0, e.diagnostics().labelledComparisons)
        assertEquals(2, e.diagnostics().rejectedEvents)
    }

    @Test
    fun `hard validator rejects other activities, unapproved and identical variants`() {
        val e = Fx.engine()
        val sew = Fx.byId("s_still")
        assertTrue(e.update(Fx.event(Fx.cook(), still, sew, ExplicitPreferenceResponse.PreferLeft)) is UpdateResult.Rejected)
        assertTrue(e.update(Fx.event(Fx.cook(), still, still, ExplicitPreferenceResponse.PreferLeft)) is UpdateResult.Rejected)
        val unapproved = still.copy(variantId = VariantId("c_x"), approved = false)
        assertTrue(e.update(Fx.event(Fx.cook(), unapproved, moving, ExplicitPreferenceResponse.PreferLeft)) is UpdateResult.Rejected)
        assertTrue(e.eligible(Fx.cook(), Fx.cookVariants + sew + unapproved).eligible.all { it.activityId == Fx.COOK && it.approved && it in Fx.cookVariants })
    }

    @Test
    fun `query gates - opt-in, never during an activity, one per session, cooldown, do-not-ask`() {
        val e = Fx.engine()
        fun b(opt: Boolean = true, dna: Boolean = false, act: Boolean = false, n: Int = 0, declined: Map<Set<VariantId>, Long> = emptyMap(), now: Long = 0) =
            QueryBurdenState(opt, dna, act, n, now, declined)
        assertEquals(DoNotAskReason.NOT_OPTED_IN, (e.selectComparison(Fx.cook(), Fx.cookVariants, b(opt = false)) as ComparisonDecision.DoNotAsk).reason)
        assertEquals(DoNotAskReason.USER_SAID_DO_NOT_ASK, (e.selectComparison(Fx.cook(), Fx.cookVariants, b(dna = true)) as ComparisonDecision.DoNotAsk).reason)
        assertEquals(DoNotAskReason.DURING_ACTIVITY, (e.selectComparison(Fx.cook(), Fx.cookVariants, b(act = true)) as ComparisonDecision.DoNotAsk).reason)
        assertEquals(DoNotAskReason.SESSION_LIMIT, (e.selectComparison(Fx.cook(), Fx.cookVariants, b(n = 1)) as ComparisonDecision.DoNotAsk).reason)
        val ask = e.selectComparison(Fx.cook(), Fx.cookVariants, b()) as ComparisonDecision.Ask
        assertTrue(ask.left.activityId == Fx.COOK && ask.right.activityId == Fx.COOK && ask.left != ask.right)
        // Declining that pair: not asked again for 14 days.
        val declined = mapOf(setOf(ask.left.variantId, ask.right.variantId) to 0L)
        val again = e.selectComparison(Fx.cook(), Fx.cookVariants, b(declined = declined, now = 1000L))
        assertTrue(again !is ComparisonDecision.Ask || setOf(again.left.variantId, again.right.variantId) != declined.keys.first())
        val opted = ScopeEngine(Fx.schema, enabled = true, optedIn = false)
        assertEquals(DoNotAskReason.NOT_OPTED_IN, (opted.selectComparison(Fx.cook(), Fx.cookVariants, b()) as ComparisonDecision.DoNotAsk).reason)
    }

    @Test
    fun `does not ask when the answer would not be informative`() {
        val e = Fx.engine()
        repeat(30) { e.update(Fx.event(Fx.cook(), still, moving, ExplicitPreferenceResponse.PreferLeft)) }
        val vs = listOf(still, moving)
        assertEquals(DoNotAskReason.LOW_VALUE, (e.selectComparisonUnchecked(Fx.cook(), vs) as ComparisonDecision.DoNotAsk).reason)
    }

    @Test
    fun `activity acceptability is separate and never moves feature weights`() {
        val e = Fx.engine()
        e.update(Fx.event(Fx.cook(), still, moving, ExplicitPreferenceResponse.PreferLeft))
        val before = e.currentPosterior.mean.copyOf()
        val again = ActivityAcceptability()
        again.record(Fx.COOK, true); again.record(Fx.COOK, false); again.record(Fx.COOK, null)
        assertEquals(1 to 1, again.counts(Fx.COOK))
        assertArrayEquals(before, e.currentPosterior.mean, 0.0)
    }

    @Test
    fun `control commands always dominate recommendations`() {
        val e = Fx.engine()
        repeat(3) { for (o in Fx.cookVariants) if (o != still) e.update(Fx.event(Fx.cook(), still, o, ExplicitPreferenceResponse.PreferLeft)) }
        val rec = e.recommend(Fx.cook(), Fx.cookVariants)
        for (c in ControlCommand.entries) assertEquals(EffectiveAction.Command(c), resolveAction(c, rec))
        assertEquals(EffectiveAction.UseVariant(rec.effectiveVariant), resolveAction(null, rec))
    }

    @Test
    fun `context holds only activity and explicit social mode - no viewport or device fields`() {
        val fields = ContextSnapshot::class.java.declaredFields.map { it.name }.filterNot { it.startsWith("$") }.toSet()
        assertEquals(setOf("activityId", "socialMode"), fields)
        val engineApi = ScopeEngine::class.java.methods.map { it.name }
        listOf("orientation", "viewport", "density", "screen", "device", "speed", "success", "hint", "emotion").forEach { bad ->
            assertTrue("engine API mentions $bad", engineApi.none { it.contains(bad, ignoreCase = true) })
        }
    }

    @Test
    fun `screen-dependent availability is not a preference and identical events give identical posteriors on every device`() {
        val e = Fx.engine()
        repeat(3) { e.update(Fx.event(Fx.cook(), still, moving, ExplicitPreferenceResponse.PreferLeft)) }
        val before = e.currentPosterior.mean.copyOf()
        val avail = e.eligible(Fx.cook(), Fx.cookVariants, availableOnScreen = setOf(still.variantId, moving.variantId))
        assertEquals(3, avail.unavailableOnThisScreen.size)
        assertArrayEquals(before, e.currentPosterior.mean, 0.0)
        // The same event fixtures replayed for four device profiles (the engine has no device input at all).
        val devices = listOf("360x800-portrait", "640x360-landscape", "800x1280-tablet", "1280x800-tablet-land")
        val means = devices.map { ScopeEngine(Fx.schema, enabled = true, optedIn = true).apply { replay(e.events) }.currentPosterior.mean }
        means.forEach { assertArrayEquals(before, it, 0.0) }
    }

    @Test
    fun `learns a Solo versus Together trade-off that the non-contextual model cannot`() {
        val full = Fx.engine()
        val flat = Fx.engine(ModelConfig.NON_CONTEXTUAL)
        repeat(6) {
            for (eng in listOf(full, flat)) {
                eng.update(Fx.event(Fx.cook(SocialMode.SOLO), still, moving, ExplicitPreferenceResponse.PreferLeft))
                eng.update(Fx.event(Fx.cook(SocialMode.TOGETHER), still, moving, ExplicitPreferenceResponse.PreferRight))
            }
        }
        assertTrue(full.probPrefer(Fx.cook(SocialMode.SOLO), still, moving) > 0.7)
        assertTrue(full.probPrefer(Fx.cook(SocialMode.TOGETHER), still, moving) < 0.3)
        assertEquals(0.5, flat.probPrefer(Fx.cook(SocialMode.SOLO), still, moving), 0.05)
        assertNotEquals(full.probPrefer(Fx.cook(SocialMode.SOLO), still, moving), full.probPrefer(Fx.cook(SocialMode.TOGETHER), still, moving), 0.2)
    }

    @Test
    fun `generalises from feature weights to an unseen approved combination`() {
        val e = Fx.engine()
        val ctx = Fx.cook(SocialMode.SOLO)
        // Learn "less motion is nicer" from pairs that never include c_still_long_aud.
        val seen = listOf(still, moving, Fx.byId("c_moving_brief_vis"), Fx.byId("c_mid"))
        repeat(2) {
            e.update(Fx.event(ctx, still, moving, ExplicitPreferenceResponse.PreferLeft))
            e.update(Fx.event(ctx, Fx.byId("c_mid"), Fx.byId("c_moving_brief_vis"), ExplicitPreferenceResponse.PreferLeft))
            e.update(Fx.event(ctx, still, Fx.byId("c_moving_brief_vis"), ExplicitPreferenceResponse.PreferLeft))
        }
        assertTrue(seen.isNotEmpty())
        val unseen = Fx.byId("c_still_long_aud")
        assertTrue(e.probPrefer(ctx, unseen, Fx.byId("c_moving_brief_vis")) > 0.6)
    }

    @Test
    fun `the SMRITI app does not depend on the experimental engine`() {
        val appBuild = listOf(File("../app/build.gradle.kts"), File("app/build.gradle.kts")).first { it.exists() }.readText()
        assertFalse(appBuild.contains(":scope"))
    }
}
