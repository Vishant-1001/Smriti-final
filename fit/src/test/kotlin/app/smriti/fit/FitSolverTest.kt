package app.smriti.fit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests with a simple fixed-advance measurer (no fonts), so expectations are hand-checkable. */
class FitSolverTest {
    /** Every character is 0.6 em wide in English and 0.9 em in Tamil (Tamil glyphs are wider). */
    private val fake = TextMeasurer { text, lang, size -> text.length * size * if (lang == Lang.TAMIL) 0.9 else 0.6 }
    private val solver = FitSolver(fake)
    private val layout = solver.layout

    private fun ctl(id: String, label: String, essential: Boolean = true) = Control(id, label, essential)
    private fun screen(lang: Lang = Lang.ENGLISH, labels: List<String> = listOf("Pause", "Stop", "Next song", "Not now"), picture: Boolean = true) = Screen(
        "radio", lang,
        HudSpec(listOf(ctl("back", "Back"), ctl("home", "Home")), "Radio", listOf(ctl("guide", "Guide")), listOf(ctl("settings", "Settings"))),
        PanelSpec("Playing", labels.mapIndexed { i, l -> ctl("b$i", l, essential = i < 2) }, picture),
    )

    private val devices = listOf(Device(320, 568), Device(360, 800), Device(800, 360), Device(412, 915, 1.3), Device(1280, 800), Device(640, 360, 1.5), Device(360, 640, 2.0))
    private val needsGrid = TextLevel.entries.flatMap { l -> listOf(true, false).flatMap { need -> listOf(48, 64).map { t -> Needs(Lang.ENGLISH, l, need, t) } } }

    @Test
    fun `whenever some configuration meets every need, the solver returns one with no violations`() {
        for (lang in Lang.entries) for (d in devices) for (n in needsGrid) {
            val s = screen(lang)
            val needs = n.copy(language = lang)
            val anyFeasible = solver.candidates(s, needs, d).any { layout.evaluate(s, needs, d, it).violations.isEmpty() }
            val dec = solver.solve(s, needs, d)
            assertEquals("$lang $d $needs", anyFeasible, dec.status == Status.OPTIMAL)
            if (anyFeasible) assertTrue("$lang $d $needs ${dec.violations}", dec.violations.isEmpty())
        }
    }

    @Test
    fun `the chosen configuration is optimal - no feasible candidate has larger text or keeps a lost picture`() {
        for (d in devices) for (n in needsGrid) {
            val s = screen()
            val dec = solver.solve(s, n, d)
            if (dec.status != Status.OPTIMAL) continue
            val chosen = layout.evaluate(s, n, d, dec.config)
            for (c in solver.candidates(s, n, d)) {
                val e = layout.evaluate(s, n, d, c)
                if (e.violations.isNotEmpty()) continue
                assertTrue(e.textDeficit >= chosen.textDeficit)
                if (e.textDeficit == chosen.textDeficit && e.illustrationShown) assertTrue("$d $n", chosen.illustrationShown)
            }
        }
    }

    @Test
    fun `text is never made larger than the person asked for`() {
        for (d in devices) for (n in needsGrid) assertTrue(solver.solve(screen(), n, d).config.textLevel <= n.textLevel)
    }

    @Test
    fun `reduced motion is always honoured`() {
        for (d in devices) {
            val dec = solver.solve(screen(), Needs(reducedMotion = true, language = Lang.ENGLISH), d)
            assertTrue(dec.config.motionOff)
            assertTrue(dec.violations.none { it.kind == ViolationKind.MOTION })
            assertTrue(dec.explanations.any { it.field == "motion" })
        }
    }

    @Test
    fun `a feasible lock is kept and explained`() {
        val d = Device(1280, 800)
        val free = solver.solve(screen(), Needs(Lang.ENGLISH), d)
        assertEquals(ButtonStyle.INLINE, free.config.style)
        val locked = solver.solve(screen(), Needs(Lang.ENGLISH), d, Locks(style = ButtonStyle.STACKED))
        assertEquals(Status.OPTIMAL, locked.status)
        assertEquals(ButtonStyle.STACKED, locked.config.style)
        assertEquals("kept as you set it", locked.explanations.single { it.field == "style" }.reason)
    }

    @Test
    fun `an infeasible lock is set aside for this screen only, with an explanation`() {
        val d = Device(320, 568)
        // A 20-letter word never fits half the width at any text size, but fits a full-width button.
        val s = screen(labels = listOf("Supercalifragilistic", "Stop"))
        val n = Needs(Lang.ENGLISH, TextLevel.NORMAL)
        val dec = solver.solve(s, n, d, Locks(columns = 2))
        assertEquals(Status.RELAXED_LOCKS, dec.status)
        assertEquals(1, dec.config.columns)
        assertTrue(dec.explanations.any { it.field == "status" && "kept setting" in it.reason })
        assertTrue(dec.violations.isEmpty())
    }

    @Test
    fun `the text-size need is relaxed before a lock`() {
        val s = screen(labels = listOf("Pause the music", "Stop the music"))
        val dec = solver.solve(s, Needs(Lang.ENGLISH, TextLevel.LARGER, textIsNeed = true), Device(320, 568), Locks(columns = 2))
        assertEquals(Status.RELAXED_TEXT_NEED, dec.status)
        assertEquals(2, dec.config.columns)
    }

    @Test
    fun `a text-size need is relaxed only when nothing can meet it, and a preference is reduced without a violation`() {
        val d = Device(360, 640, 1.3)
        val s = screen(Lang.TAMIL)
        val asNeed = solver.solve(s, Needs(Lang.TAMIL, TextLevel.LARGER, textIsNeed = true), d)
        val asPref = solver.solve(s, Needs(Lang.TAMIL, TextLevel.LARGER, textIsNeed = false), d)
        if (asNeed.status == Status.RELAXED_TEXT_NEED) {
            assertTrue(asNeed.violations.all { it.kind == ViolationKind.TEXT_NEED })
            assertTrue(asNeed.explanations.first().reason.contains("cannot fit"))
        }
        assertTrue(asPref.violations.none { it.kind == ViolationKind.TEXT_NEED })
    }

    @Test
    fun `best effort returns the least severe configuration and lists what remains`() {
        val d = Device(300, 340, 2.0)
        val s = screen(labels = List(6) { "A very long control label number $it" })
        val dec = solver.solve(s, Needs(Lang.ENGLISH, TextLevel.LARGER, textIsNeed = true, minTargetDp = 64), d)
        assertEquals(Status.BEST_EFFORT, dec.status)
        assertTrue(dec.violations.isNotEmpty())
        val worst = { c: Config -> layout.evaluate(s, dec.let { Needs(Lang.ENGLISH, TextLevel.LARGER, true, 64) }, d, c).violations.count { it.kind.severity == 6 } }
        val best = solver.candidates(s, Needs(Lang.ENGLISH, TextLevel.LARGER, true, 64), d).minOf(worst)
        assertEquals(best, worst(dec.config))
    }

    @Test
    fun `decisions are deterministic and replay identically from persisted state`() {
        val s = screen(Lang.TAMIL)
        val d = Device(412, 915, 1.15)
        val state = FitState(Needs(Lang.TAMIL, TextLevel.LARGER, true, 56, true), Locks(illustration = true))
        val first = state.replay(solver, s, d)
        val accepted = state.accept(s.id, first.config)
        val decoded = FitState.decode(FitState.encode(accepted))
        assertEquals(accepted, decoded)
        assertEquals(accepted.replay(solver, s, d), decoded!!.replay(solver, s, d))
        assertEquals(first.config, accepted.replay(solver, s, d).config) // the accepted decision is a fixed point
    }

    @Test
    fun `stability - a still-feasible previous layout is kept when it is as good on text and picture`() {
        val s = screen()
        val d = Device(1280, 800)
        val n = Needs(Lang.ENGLISH)
        val fresh = solver.solve(s, n, d)
        val previous = fresh.config.copy(columns = 1)
        assertTrue(layout.evaluate(s, n, d, previous).violations.isEmpty())
        val kept = solver.solve(s, n, d, previous = previous)
        assertEquals(previous, kept.config)
        assertTrue(kept.explanations.single { it.field == "columns" }.reason.startsWith("kept from the previous"))
    }

    @Test
    fun `every field changed from the design as drawn is explained`() {
        for (d in devices) for (n in needsGrid) {
            val s = screen(Lang.TAMIL)
            val nn = n.copy(language = Lang.TAMIL)
            val dec = solver.solve(s, nn, d)
            val base = solver.asAsked(s, nn)
            val changed = listOf(
                "textLevel" to (dec.config.textLevel != base.textLevel),
                "hudRows" to (dec.config.hudRows != base.hudRows),
                "columns" to (dec.config.columns != base.columns),
                "style" to (dec.config.style != base.style),
                "illustration" to (dec.config.illustration != base.illustration),
                "hudWideButtons" to (dec.config.hudWideButtons != base.hudWideButtons),
            ).filter { it.second }.map { it.first }
            for (f in changed) assertTrue("$d $nn $f", dec.explanations.any { it.field == f })
        }
    }

    @Test
    fun `persisted state decoding fails closed`() {
        val good = FitState.encode(FitState())
        assertNotNull(FitState.decode(good))
        assertNull(FitState.decode(good.replace("fit.version=1", "fit.version=2")))
        assertNull(FitState.decode(good.replace("needs.minTargetDp=48", "needs.minTargetDp=4")))
        assertNull(FitState.decode(good + "lock.columns=9\n"))
        assertNull(FitState.decode(good + "needs.mood=sad\n"))
        assertNull(FitState.decode("garbage"))
    }

    @Test
    fun `inputs are explicit settings only - no behavioural, performance or sensor fields`() {
        val needsFields = Needs::class.java.declaredFields.map { it.name }.filter { !it.startsWith("$") }.toSet()
        assertEquals(setOf("language", "textLevel", "textIsNeed", "minTargetDp", "reducedMotion"), needsFields)
        val deviceFields = Device::class.java.declaredFields.map { it.name }.filter { !it.startsWith("$") }.toSet()
        assertEquals(setOf("widthDp", "heightDp", "systemFontScale", "systemBarsDp"), deviceFields)
    }

    @Test
    fun `B1 reproduces the app's thresholds exactly`() {
        val s = screen()
        val n = Needs(Lang.ENGLISH)
        fun b1(w: Int, h: Int) = CurrentRules.choose(s, n, Device(w, h))
        assertEquals(2, b1(599, 900).hudRows); assertEquals(1, b1(600, 900).hudRows)
        assertEquals(1, b1(379, 800).columns); assertEquals(2, b1(380, 800).columns)
        assertEquals(ButtonStyle.STACKED, b1(599, 900).style); assertEquals(ButtonStyle.INLINE, b1(600, 900).style)
        assertEquals(ButtonStyle.STACKED, b1(899, 400).style); assertEquals(ButtonStyle.INLINE, b1(900, 400).style)
        assertFalse(b1(900, 400).hudFollowsTextSize)
        assertFalse(b1(900, 400).measuredHudReservation)
    }

    @Test
    fun `layout model detects broken words, overlap and controls below a non-scrolling landscape column`() {
        val w = layout.wrap("Supercalifragilistic word", Lang.ENGLISH, 10.0, 60.0)
        assertEquals("Supercalifragilistic", w.brokenWord)
        assertNull(layout.wrap("Two short", Lang.ENGLISH, 10.0, 60.0).brokenWord)
        assertEquals(2, layout.wrap("Two short", Lang.ENGLISH, 10.0, 40.0).lines)

        val big = Needs(Lang.ENGLISH, TextLevel.LARGER)
        val rules = CurrentRules.choose(screen(), big, Device(412, 915, 2.0))
        assertTrue(layout.evaluate(screen(), big, Device(412, 915, 2.0), rules).violations.any { it.kind == ViolationKind.OVERLAP })

        val land = Device(640, 300, 1.5)
        val e = layout.evaluate(screen(), big, land, CurrentRules.choose(screen(), big, land))
        assertTrue(e.violations.any { it.kind == ViolationKind.ESSENTIAL_HIDDEN && it.where.startsWith("panel") })
    }

    @Test
    fun `greedy baseline uses only the solver's knobs and never beats a feasible optimum`() {
        val greedy = GreedyRepair(layout)
        for (d in devices) for (n in needsGrid) {
            val s = screen(Lang.TAMIL)
            val nn = n.copy(language = Lang.TAMIL)
            val g = greedy.choose(s, nn, d)
            val ge = layout.evaluate(s, nn, d, g)
            val dec = solver.solve(s, nn, d)
            if (dec.status == Status.OPTIMAL && ge.violations.isEmpty()) {
                assertTrue(layout.evaluate(s, nn, d, dec.config).textDeficit <= ge.textDeficit)
            }
        }
    }
}
