package app.smriti.fit

import app.smriti.fit.sim.AwtMeasurer
import app.smriti.fit.sim.Content
import app.smriti.fit.sim.Instances
import app.smriti.fit.sim.run
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Uses the app's bundled fonts and strings (path passed by Gradle as `smriti.res`). */
class RealFontsTest {
    private val res = File(System.getProperty("smriti.res") ?: "../app/src/main/res")
    private val measurer = CachingMeasurer(AwtMeasurer(res))
    private val content = Content(res)
    private val solver = FitSolver(measurer)

    @Test
    fun `Tamil labels are measured with the Tamil font and are much wider than English`() {
        val ta = measurer.widthDp(content.text("radio_pause", Lang.TAMIL), Lang.TAMIL, 18.0)
        val en = measurer.widthDp(content.text("radio_pause", Lang.ENGLISH), Lang.ENGLISH, 18.0)
        assertTrue("ta=$ta en=$en", ta > 2 * en)
        // Mixed runs (Tamil + digits) fall back to the Latin font instead of measuring missing glyphs.
        assertTrue(measurer.widthDp("பட்டியல் 2/5", Lang.TAMIL, 15.0) > measurer.widthDp("பட்டியல்", Lang.TAMIL, 15.0))
    }

    @Test
    fun `the current rules have no violations on the configuration they were tuned for`() {
        // Tamil, default settings, 360×800 portrait phone: the app's main target, checked in earlier screenshot passes.
        val s = content.radio(Lang.TAMIL)
        val e = solver.layout.evaluate(s, Needs(), Device(360, 800), CurrentRules.choose(s, Needs(), Device(360, 800)))
        assertEquals(emptyList<Violation>(), e.violations)
    }

    @Test
    fun `benchmark runs are reproducible`() {
        val a = run(1L..40L, false, content, solver).map { Triple(it.b1.config, it.b2.config, it.decision.copy(candidatesEvaluated = 0)) }
        val b = run(1L..40L, false, content, FitSolver(CachingMeasurer(AwtMeasurer(res)))).map { Triple(it.b1.config, it.b2.config, it.decision.copy(candidatesEvaluated = 0)) }
        assertEquals(a, b)
    }

    @Test
    fun `development and held-out content pools do not overlap`() {
        assertTrue(content.buttonPool(false).intersect(content.buttonPool(true).toSet()).isEmpty())
        assertTrue(content.titlePool(false).intersect(content.titlePool(true).toSet()).isEmpty())
        assertTrue(content.buttonPool(true).size >= 20 && content.buttonPool(false).size >= 20)
        assertEquals(Instances.make(7, content, true), Instances.make(7, content, true))
    }
}
