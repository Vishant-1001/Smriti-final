package app.smriti.fit

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.smriti.AppGraph
import app.smriti.MainActivity
import app.smriti.SmritiApp
import app.smriti.domain.AppLanguage
import app.smriti.domain.NavState
import app.smriti.domain.SaveData
import app.smriti.domain.SceneId
import app.smriti.domain.Settings
import app.smriti.domain.TextSize
import app.smriti.fit.sim.AwtMeasurer
import app.smriti.fit.sim.Content
import app.smriti.testing.TestGraph
import app.smriti.ui.fit.FitFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import com.github.takahirom.roborazzi.captureRoboImage

/**
 * F5 (docs/flagship/FIT_PROTOCOL_v2.md): does the Fit geometry model agree with real Compose
 * rendering? The app's current radio screen is rendered by Robolectric (native graphics) for each
 * validation instance. What is observed on screen is compared with what the model predicts for the
 * current rules (B1) on the same instance.
 *
 * Default: protocol v2 seeds 19001–19100 → docs/flagship/FIT_VALIDATION_v2.md.
 * Run: ./gradlew :app:testDebugUnitTest --tests app.smriti.fit.FitValidationTest
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class FitValidationTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val res = File("src/main/res")
    private val content = Content(res)
    private val model = LayoutModel(CachingMeasurer(AwtMeasurer(res)))

    private data class Case(val seed: Long, val lang: Lang, val w: Int, val h: Int, val scale: Double, val level: TextLevel)

    /** Same device, language, font-scale and text-level mix as `Instances`, radio screen only. */
    private fun case(seed: Long): Case {
        val r = Random(seed)
        val phones = listOf(320 to 568, 360 to 640, 360 to 740, 360 to 800, 384 to 854, 393 to 851, 411 to 891, 412 to 915, 430 to 932)
        val tablets = listOf(600 to 960, 673 to 841, 768 to 1024, 800 to 1280, 834 to 1194, 1024 to 1366)
        val (w, h) = when (r.nextInt(100)) {
            in 0 until 55 -> phones[r.nextInt(phones.size)].let { (a, b) -> if (r.nextInt(100) < 70) a to b else b to a }
            in 55 until 80 -> tablets[r.nextInt(tablets.size)].let { (a, b) -> if (r.nextBoolean()) a to b else b to a }
            else -> (300 + r.nextInt(1001)) to (320 + r.nextInt(981))
        }
        val lang = if (r.nextInt(100) < 60) Lang.TAMIL else Lang.ENGLISH
        val level = listOf(TextLevel.NORMAL to 20, TextLevel.LARGE to 50, TextLevel.LARGER to 30).let { items ->
            var x = r.nextInt(100); items.first { (_, wt) -> (x < wt).also { x -= wt } }.first
        }
        val scale = listOf(0.85 to 5, 1.0 to 45, 1.15 to 20, 1.3 to 15, 1.5 to 8, 1.8 to 4, 2.0 to 3).let { items ->
            var x = r.nextInt(100); items.first { (_, wt) -> (x < wt).also { x -= wt } }.first
        }
        return Case(seed, lang, w, h, scale, level)
    }

    private val checks = listOf(
        "pause hidden", "stop hidden", "pause label", "stop label", "next label", "not-now label",
        "top bar overlaps content", "settings squeezed or off screen", "settings label", "place name cut",
    )

    private fun predicted(c: Case): Map<String, Boolean> {
        val screen = content.radio(c.lang)
        val needs = Needs(language = c.lang, textLevel = c.level)
        val device = Device(c.w, c.h, c.scale, systemBarsDp = 0.0)
        val v = model.evaluate(screen, needs, device, CurrentRules.choose(screen, needs, device)).violations
        fun has(kind: Set<ViolationKind>, where: String) = v.any { it.kind in kind && it.where == where }
        val label = setOf(ViolationKind.WORD_BROKEN, ViolationKind.TOO_MANY_LINES)
        return mapOf(
            "pause hidden" to has(setOf(ViolationKind.ESSENTIAL_HIDDEN), "panel:radio_pause"),
            "stop hidden" to has(setOf(ViolationKind.ESSENTIAL_HIDDEN), "panel:radio_stop"),
            "pause label" to has(label, "panel:radio_pause"),
            "stop label" to has(label, "panel:radio_stop"),
            "next label" to has(label, "panel:radio_next"),
            "not-now label" to has(label, "panel:radio_not_now"),
            "top bar overlaps content" to v.any { it.kind == ViolationKind.OVERLAP },
            "settings squeezed or off screen" to has(setOf(ViolationKind.ESSENTIAL_HIDDEN), "hud:action_settings"),
            "settings label" to has(label, "hud:action_settings"),
            "place name cut" to v.any { it.kind == ViolationKind.TITLE_TRUNCATED },
        )
    }

    private fun node(tag: String): SemanticsNode =
        compose.onNode(androidx.compose.ui.test.hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNode()

    /** Fraction of the node's height that is inside its ancestors' bounds (1.0 = fully visible). */
    private fun visibleFraction(n: SemanticsNode): Double = if (n.size.height == 0) 0.0 else n.boundsInRoot.height / n.size.height.toDouble()

    /**
     * Not fully usable: partly outside the visible area, or squeezed below the button's 64 dp
     * minimum height (Compose shrinks rows of a non-scrolling column instead of moving them).
     */
    private fun unusable(n: SemanticsNode, density: Float, minHeightDp: Float = 64f): Boolean =
        visibleFraction(n) < 0.99 || n.size.height / density < minHeightDp - 0.5f

    private fun textLayout(text: String): TextLayoutResult? {
        val nodes = compose.onAllNodes(androidx.compose.ui.test.hasText(text, substring = false), useUnmergedTree = true).fetchSemanticsNodes()
        val n = nodes.firstOrNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { s -> s.text } == text } ?: return null
        val out = mutableListOf<TextLayoutResult>()
        n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(out)
        return out.firstOrNull()
    }

    /** True if the label is broken inside a word or needs more than two lines. */
    private fun labelBad(text: String): Boolean {
        val l = textLayout(text) ?: error("no text node for '$text'; texts: " + compose.onAllNodes(androidx.compose.ui.test.hasText("", substring = true), useUnmergedTree = true).fetchSemanticsNodes().map { it.config.getOrNull(SemanticsProperties.Text)?.joinToString("|") { s -> s.text } })
        if (l.lineCount > 2) return true
        for (i in 0 until l.lineCount - 1) {
            val end = l.getLineEnd(i)
            if (end in 1 until text.length && text[end - 1] != ' ' && text[end] != ' ') return true
        }
        return false
    }

    /** Where renders are saved (needs -Proborazzi.test.record=true for Roborazzi to write images). */
    private var captureDir: String? = System.getProperty("fit.capture")

    /** Font scale the Pause label was drawn at (to see whether Fit lowered the text level). */
    private var lastPauseFontScale = 0f

    private fun observed(c: Case, fitOn: Boolean = false): Map<String, Boolean> {
        FitFlags.radioEnabled = fitOn
        val settings = Settings(
            language = if (c.lang == Lang.TAMIL) AppLanguage.TAMIL else AppLanguage.ENGLISH,
            textSize = TextSize.entries[c.level.ordinal],
        )
        val save = SaveData(nav = NavState(listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.BEDROOM, SceneId.RADIO)), settings = settings)
        RuntimeEnvironment.setQualifiers("w${c.w}dp-h${c.h}dp-${if (c.w > c.h) "land" else "port"}-xhdpi")
        RuntimeEnvironment.setFontScale(c.scale.toFloat())
        // The application (and its lazily created graph) lives for the whole test, so each case
        // writes its own save into the shared in-memory repository before the activity starts.
        SmritiApp.graphFactory = { TestGraph(it, save, voiceAvailable = false) }
        val graph = (RuntimeEnvironment.getApplication() as SmritiApp).graph as TestGraph
        runBlocking { graph.memorySave.update { save } }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            compose.waitForIdle()
            compose.onNode(androidx.compose.ui.test.hasTestTag("track_demo_tune_1"), useUnmergedTree = true).performScrollTo().performClick()
            compose.waitForIdle()
            // Back to the top of any scrolling column, so positions are what the person first sees.
            compose.onAllNodes(hasScrollAction(), useUnmergedTree = true).fetchSemanticsNodes().forEachIndexed { i, _ ->
                compose.onAllNodes(hasScrollAction(), useUnmergedTree = true)[i].performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -1e6f) }
            }
            compose.waitForIdle()
            val density = compose.onRoot().fetchSemanticsNode().layoutInfo.density.density
            val hudBottom = listOf("hud_back", "hud_home", "hud_guide", "hud_settings", "scene_name").maxOf { t ->
                val n = node(t); (n.positionInRoot.y + n.size.height) / density
            }
            val contentTop = node("scene_radio").positionInRoot.y / density
            val settingsNode = node("hud_settings")
            val rootW = compose.onRoot().fetchSemanticsNode().size.width / density
            val settingsRight = (settingsNode.positionInRoot.x + settingsNode.size.width) / density
            val titleLayout = textLayout(content.text("scene_radio", c.lang))
            captureDir?.let { dir ->
                File(dir).mkdirs()
                compose.onRoot().captureRoboImage("$dir/${c.seed}${if (fitOn) "_fit" else "_current"}.png")
                val tl = titleLayout
                File("$dir/${c.seed}.txt").writeText(
                    "title=${tl?.layoutInput?.text} lines=${tl?.lineCount} overflow=${tl?.hasVisualOverflow} " +
                        "w=${tl?.size} hud=$hudBottom content=$contentTop density=$density " +
                        "titleFontScale=${tl?.layoutInput?.density?.fontScale} titleSize=${tl?.layoutInput?.style?.fontSize} " +
                        "pause=${textLayout(content.text("radio_pause", c.lang))?.let { "${it.layoutInput.density.fontScale} ${it.layoutInput.style.fontSize} lines=${it.lineCount} size=${it.size} constraints=${it.layoutInput.constraints}" }}",
                )
            }
            fun t(name: String) = content.text(name, c.lang)
            lastPauseFontScale = textLayout(t("radio_pause"))?.layoutInput?.density?.fontScale ?: 0f
            return mapOf(
                "pause hidden" to unusable(node("radio_pause"), density),
                "stop hidden" to unusable(node("radio_stop"), density),
                "pause label" to labelBad(t("radio_pause")),
                "stop label" to labelBad(t("radio_stop")),
                "next label" to labelBad(t("radio_next")),
                "not-now label" to labelBad(t("radio_not_now")),
                "top bar overlaps content" to (hudBottom > contentTop + 16.5),
                "settings squeezed or off screen" to (settingsNode.size.width / density < 76 - 0.5 || settingsRight > rootW + 0.5 || unusable(settingsNode, density)),
                "settings label" to labelBad(t("action_settings")),
                // Ellipsised, over two lines or broken mid-word. (hasVisualOverflow is not used: it is
                // true for every one-line title because the 28 sp line height trims the glyph box.)
                "place name cut" to (titleLayout == null || titleLayout.isLineEllipsized(titleLayout.lineCount - 1) || labelBad(t("scene_radio"))),
            )
        } finally {
            FitFlags.radioEnabled = false
            scenario.close()
        }
    }

    @Test
    fun modelAgreesWithRealRendering() {
        // Writes committed result files; runs only when asked: -Dfit.validation=true
        assumeTrue(System.getProperty("fit.validation") == "true")
        // v1 used seeds 9001–9060 (FIT_VALIDATION.md, FAIL). Protocol v2: fresh seeds 19001–19100.
        val (from, to) = (System.getProperty("fit.seeds") ?: "19001-19100").split('-').map { it.toLong() }
        val outPath = System.getProperty("fit.out") ?: "../docs/flagship/FIT_VALIDATION_v2.md"
        val rows = (from..to).map { s -> val c = case(s); Triple(c, predicted(c), observed(c)) }
        var agree = 0
        var total = 0
        val report = buildString {
            appendLine("# SMRITI Fit: F5 model validation against real Compose rendering")
            appendLine()
            appendLine("Generated by `app/src/test/java/app/smriti/fit/FitValidationTest.kt` (Robolectric native graphics, SDK 34, no system bars). For ${rows.size} instances (seeds $from–$to), the app's **current** radio screen is rendered with a song playing, and what is on screen is compared with the Fit model's prediction for the current rules (B1). These are JVM renders, not device captures.")
            appendLine()
            appendLine("| Check | Agree | Model yes / real yes | Model yes / real no | Model no / real yes | Model no / real no |")
            appendLine("|---|---|---|---|---|---|")
            for (k in checks) {
                val tp = rows.count { it.second.getValue(k) && it.third.getValue(k) }
                val fp = rows.count { it.second.getValue(k) && !it.third.getValue(k) }
                val fn = rows.count { !it.second.getValue(k) && it.third.getValue(k) }
                val tn = rows.count { !it.second.getValue(k) && !it.third.getValue(k) }
                agree += tp + tn; total += rows.size
                appendLine("| $k | ${tp + tn}/${rows.size} | $tp | $fp | $fn | $tn |")
            }
            appendLine()
            val rate = agree / total.toDouble()
            appendLine("**Overall agreement: $agree/$total = ${"%.1f".format(Locale.ROOT, 100 * rate)}%.** Gate F5 (≥ 90%): **${if (rate >= 0.9) "PASS" else "FAIL"}**.")
            appendLine()
            appendLine("Instances where model and rendering disagree:")
            appendLine()
            for ((c, p, o) in rows) {
                val diff = checks.filter { p.getValue(it) != o.getValue(it) }
                if (diff.isNotEmpty()) appendLine("- seed ${c.seed}: ${c.lang} ${c.w}×${c.h} font ${c.scale} text ${c.level}: " + diff.joinToString { "$it (model ${p.getValue(it)}, real ${o.getValue(it)})" })
            }
            appendLine()
            appendLine("Instances where the rendering shows at least one problem with the current rules: ${rows.count { r -> r.third.values.any { it } }}/${rows.size}.")
        }
        SmritiApp.graphFactory = { AppGraph(it) }
        File(outPath).writeText(report)
        println(report)
        assertTrue(total == rows.size * checks.size)
    }

    /**
     * V6 (FIT_PROTOCOL_v2.md, descriptive): the same validation instances rendered with the current
     * layout and with SMRITI Fit driving the radio screen, judged by the same on-screen checks.
     * Writes docs/flagship/FIT_V6_REAL_RENDER.md.
     */
    @Test
    fun fitVersusCurrentOnRealRendering() {
        // Writes committed result files; runs only when asked: -Dfit.validation=true
        assumeTrue(System.getProperty("fit.validation") == "true")
        val (from, to) = (System.getProperty("fit.seeds") ?: "19001-19100").split('-').map { it.toLong() }
        data class R(val c: Case, val current: Map<String, Boolean>, val fit: Map<String, Boolean>, val currentScale: Float, val fitScale: Float)
        val rows = (from..to).map { s ->
            val c = case(s)
            val cur = observed(c, fitOn = false); val cs = lastPauseFontScale
            val fit = observed(c, fitOn = true); val fs = lastPauseFontScale
            R(c, cur, fit, cs, fs)
        }
        val curBad = rows.count { r -> r.current.values.any { it } }
        val fitBad = rows.count { r -> r.fit.values.any { it } }
        val fitWorse = rows.filter { r -> checks.any { r.fit.getValue(it) && !r.current.getValue(it) } }
        val lowered = rows.count { it.fitScale < it.currentScale - 0.01f }
        val enable = fitBad < curBad && fitWorse.isEmpty()
        val report = buildString {
            appendLine("# SMRITI Fit: V6 real-render comparison on the radio screen")
            appendLine()
            appendLine("Generated by `FitValidationTest.fitVersusCurrentOnRealRendering` (Robolectric native graphics, SDK 34). The same ${rows.size} instances as F5 (seeds $from–$to) are rendered twice, once with the current fixed thresholds and once with SMRITI Fit choosing the layout in the app (Compose text measurement), and judged by the same on-screen checks. Descriptive, as pre-registered: there is no significance test. JVM renders, not device captures.")
            appendLine()
            appendLine("| Check | Current layout: problem | Fit: problem |")
            appendLine("|---|---|---|")
            for (k in checks) appendLine("| $k | ${rows.count { it.current.getValue(k) }} | ${rows.count { it.fit.getValue(k) }} |")
            appendLine()
            appendLine("- Instances with at least one observed problem: **current $curBad/${rows.size}, Fit $fitBad/${rows.size}**.")
            appendLine("- Instances where Fit shows a problem that the current layout does not: **${fitWorse.size}**" +
                (if (fitWorse.isEmpty()) "." else ": " + fitWorse.joinToString { r -> "seed ${r.c.seed} (" + checks.filter { r.fit.getValue(it) && !r.current.getValue(it) }.joinToString() + ")" }))
            appendLine("- Instances where Fit drew text at a smaller size than the setting (to make it fit; text size is a preference in the app today): **$lowered**.")
            appendLine()
            appendLine("**Pre-registered default rule** (fewer problems and none new): **${if (enable) "met: Fit may be enabled by default on the radio screen" else "not met: Fit stays off by default"}**.")
            appendLine()
            appendLine("Instances with a problem under Fit:")
            appendLine()
            for (r in rows.filter { r -> r.fit.values.any { it } }) {
                appendLine("- seed ${r.c.seed}: ${r.c.lang} ${r.c.w}×${r.c.h} font ${r.c.scale} text ${r.c.level}: " + checks.filter { r.fit.getValue(it) }.joinToString())
            }
        }
        SmritiApp.graphFactory = { AppGraph(it) }
        File(System.getProperty("fit.v6out") ?: "../docs/flagship/FIT_V6_REAL_RENDER.md").writeText(report)
        println(report)
        assertTrue(rows.isNotEmpty())
    }

    /** Fit is off by default: the V6 default rule was not met (FIT_V6_REAL_RENDER.md). */
    @Test
    fun fitIsOffByDefault() {
        assertEquals(false, FitFlags.radioEnabled)
    }

    /**
     * Demo renders for docs/flagship/img: the radio screen with the current thresholds and with Fit,
     * for two v1 validation cases. Run with -Dfit.validation=true -Proborazzi.test.record=true.
     */
    @Test
    fun demoScreenshots() {
        assumeTrue(System.getProperty("fit.validation") == "true")
        captureDir = "../docs/flagship/img"
        for (seed in listOf(9031L, 9051L)) {
            val c = case(seed)
            observed(c, fitOn = false)
            observed(c, fitOn = true)
        }
        captureDir = System.getProperty("fit.capture")
        SmritiApp.graphFactory = { AppGraph(it) }
    }
}
