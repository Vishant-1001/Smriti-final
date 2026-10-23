package app.smriti.fit

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Measures the advance width, in dp, of [text] set in SMRITI's bold UI font for [lang] at
 * [fontSizeDp]. On the JVM benchmark this is AWT on the app's bundled Noto fonts; on a device it
 * would be Compose's `TextMeasurer`.
 */
fun interface TextMeasurer {
    fun widthDp(text: String, lang: Lang, fontSizeDp: Double): Double
}

/** Caches widths at a reference size and scales linearly (fractional metrics, no hinting). */
class CachingMeasurer(private val base: TextMeasurer, private val referenceDp: Double = 100.0) : TextMeasurer {
    private val cache = HashMap<Pair<String, Lang>, Double>()
    override fun widthDp(text: String, lang: Lang, fontSizeDp: Double): Double =
        cache.getOrPut(text to lang) { base.widthDp(text, lang, referenceDp) } * fontSizeDp / referenceDp
}

/**
 * Geometry constants copied from the app's Compose code so the model describes the real screens.
 * All values are dp, or sp for text sizes. Source locations are in docs/flagship/FIT_SPEC.md §3.
 */
object Dims {
    const val HUD_PAD_H = 12.0
    const val HUD_PAD_V = 8.0
    const val HUD_GAP = 8.0
    const val HUD_BTN_MIN_W = 76.0
    const val HUD_BTN_MIN_H = 64.0
    const val HUD_BTN_MAX_W = 150.0
    const val HUD_BTN_MAX_W_WIDE = 220.0
    const val HUD_BTN_PAD = 6.0
    const val HUD_ICON = 28.0
    const val HUD_LABEL_SP = 15.0
    const val HUD_LABEL_LINE_SP = 22.0
    const val TITLE_SP = 19.0
    const val TITLE_LINE_SP = 28.0
    const val TITLE_PAD_H = 16.0
    const val TITLE_PAD_V = 6.0
    const val TITLE_MIN_W = 48.0
    const val RESERVE_NARROW = 168.0
    const val RESERVE_WIDE = 90.0
    const val SCREEN_PAD = 16.0
    const val PORTRAIT_GAP = 14.0
    const val LANDSCAPE_GAP = 20.0
    const val PANEL_PAD = 20.0
    const val STATUS_SP = 22.0
    const val STATUS_LINE_SP = 32.0
    const val STATUS_SPACER = 12.0
    const val BTN_GAP = 10.0
    const val BTN_MIN_H = 64.0
    const val BTN_LABEL_SP = 18.0
    const val BTN_LABEL_LINE_SP = 26.0
    const val INLINE_PAD_H = 20.0
    const val INLINE_PAD_V = 12.0
    const val STACK_PAD_H = 10.0
    const val STACK_PAD_V = 8.0
    const val BTN_ICON = 28.0
    const val ICON_GAP = 10.0
    const val TRAILING_LABEL_LINE_SP = 26.0
    const val ILLUS_MAX_H = 220.0
    const val ILLUS_SIDE_PAD = 40.0
    const val ILLUS_ASPECT = 0.75 // assumed height/width of the radio picture
    const val ILLUS_MIN_LANDSCAPE = 80.0
    const val BOTTOM_PAD = 12.0
    const val MAX_LINES = 2
    const val EPS = 0.5
}

data class Wrapped(val lines: Int, val brokenWord: String?, val widest: Double)

data class Evaluation(
    val violations: List<Violation>,
    val hudHeightDp: Double,
    val illustrationShown: Boolean,
    /** Text-size levels below what was asked, summed over the HUD and the activity content. */
    val textDeficit: Int,
    val panelRows: Int,
) {
    val geometric: List<Violation> get() = violations.filter { it.kind.geometric }
    fun hardExcept(kind: ViolationKind): List<Violation> = violations.filter { it.kind != kind }
}

/** Pure geometry model of one screen under one configuration. Deterministic. */
class LayoutModel(private val measurer: TextMeasurer) {

    fun wrap(text: String, lang: Lang, fontDp: Double, avail: Double): Wrapped {
        val words = text.split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return Wrapped(1, null, 0.0)
        if (avail <= 1.0) return Wrapped(99, words.first(), measurer.widthDp(text, lang, fontDp))
        var lines = 1
        var current = ""
        var broken: String? = null
        var widest = 0.0
        for (w in words) {
            val ww = measurer.widthDp(w, lang, fontDp)
            if (ww > avail + Dims.EPS) {
                if (broken == null) broken = w
                if (current.isNotEmpty()) lines++
                lines += ceil(ww / avail).toInt() - 1
                widest = max(widest, avail)
                current = ""
                continue
            }
            val candidate = if (current.isEmpty()) w else "$current $w"
            val cw = measurer.widthDp(candidate, lang, fontDp)
            if (cw <= avail + Dims.EPS) {
                current = candidate
                widest = max(widest, cw)
            } else {
                lines++
                current = w
                widest = max(widest, ww)
            }
        }
        return Wrapped(lines, broken, widest)
    }

    private fun labelChecks(where: String, text: String, lang: Lang, fontDp: Double, avail: Double, out: MutableList<Violation>): Wrapped {
        val w = wrap(text, lang, fontDp, avail)
        if (w.brokenWord != null) {
            val need = measurer.widthDp(w.brokenWord, lang, fontDp)
            out += Violation(ViolationKind.WORD_BROKEN, where, "'${w.brokenWord}' needs ${need.fmt()} dp but has ${avail.fmt()} dp")
        } else if (w.lines > Dims.MAX_LINES) {
            out += Violation(ViolationKind.TOO_MANY_LINES, where, "'$text' needs ${w.lines} lines at ${avail.fmt()} dp")
        }
        return w
    }

    private data class Item(val width: Double, val height: Double, val control: Control?)

    fun evaluate(screen: Screen, needs: Needs, device: Device, c: Config): Evaluation {
        val v = mutableListOf<Violation>()
        val lang = screen.language
        val sys = device.systemFontScale
        val contentScale = sys * c.textLevel.scale
        val hudScale = if (c.hudFollowsTextSize) contentScale else sys
        val w = device.widthDp.toDouble()
        val usableH = device.heightDp - device.systemBarsDp

        // ---------------------------------------------------------------- top bar
        val hud = screen.hud
        val hudFont = Dims.HUD_LABEL_SP * hudScale
        val hudMaxW = if (c.hudWideButtons) Dims.HUD_BTN_MAX_W_WIDE else Dims.HUD_BTN_MAX_W
        fun hudButton(ctl: Control): Item {
            val textMax = hudMaxW - 2 * Dims.HUD_BTN_PAD
            val natural = measurer.widthDp(ctl.label, lang, hudFont)
            val wrapped = labelChecks("hud:${ctl.id}", ctl.label, lang, hudFont, textMax, v)
            val content = if (natural <= textMax + Dims.EPS) natural else wrapped.widest
            val width = (max(Dims.HUD_ICON, content) + 2 * Dims.HUD_BTN_PAD).coerceIn(Dims.HUD_BTN_MIN_W, hudMaxW)
            val lines = min(wrapped.lines, Dims.MAX_LINES)
            val height = max(Dims.HUD_BTN_MIN_H, 2 * Dims.HUD_BTN_PAD + Dims.HUD_ICON + lines * Dims.HUD_LABEL_LINE_SP * hudScale)
            return Item(width, height, ctl)
        }
        val titleFont = Dims.TITLE_SP * contentScale
        fun titleHeight(avail: Double): Double {
            val textAvail = avail - 2 * Dims.TITLE_PAD_H
            if (avail < Dims.TITLE_MIN_W) {
                v += Violation(ViolationKind.TITLE_TRUNCATED, "hud:title", "no room for the place name (${avail.fmt()} dp)")
                return 0.0
            }
            val wr = wrap(hud.title, lang, titleFont, textAvail)
            if (wr.brokenWord != null || wr.lines > Dims.MAX_LINES) {
                v += Violation(ViolationKind.TITLE_TRUNCATED, "hud:title", "'${hud.title}' needs more than two lines or a broken word at ${textAvail.fmt()} dp")
            }
            return min(wr.lines, Dims.MAX_LINES) * Dims.TITLE_LINE_SP * contentScale + 2 * Dims.TITLE_PAD_V
        }
        val hudItems = hud.buttons.associateWith { hudButton(it) }
        val rowW = w - 2 * Dims.HUD_PAD_H
        // Returns the row height; buttons that do not fit are squeezed out (Compose Row measures in order).
        fun row(buttons: List<Control>, withTitle: Boolean): Double {
            val items = buttons.map { hudItems.getValue(it) }
            var used = 0.0
            var height = 0.0
            items.forEachIndexed { i, it ->
                val gap = if (i == 0) 0.0 else Dims.HUD_GAP
                if (used + gap + it.width > rowW + Dims.EPS) {
                    v += Violation(ViolationKind.ESSENTIAL_HIDDEN, "hud:${it.control!!.id}", "top bar row needs more than ${rowW.fmt()} dp")
                }
                used += gap + it.width
                height = max(height, it.height)
            }
            if (withTitle) {
                val rem = rowW - used - (if (items.isEmpty()) 0.0 else Dims.HUD_GAP)
                height = max(height, titleHeight(rem))
            }
            return height
        }
        val first = hud.leading + hud.trailing
        val rows: List<Double> = when {
            c.hudRows <= 1 -> listOf(row(hud.leading + hud.extras + hud.trailing, withTitle = true))
            c.hudRows == 2 || hud.extras.isEmpty() -> listOf(row(first, false), row(hud.extras, true))
            else -> listOf(row(first, false), row(emptyList(), true), row(hud.extras, false))
        }
        val hudHeight = 2 * Dims.HUD_PAD_V + rows.sum() + Dims.HUD_GAP * (rows.size - 1)
        val reservation = if (c.measuredHudReservation) hudHeight else if (w < 600) Dims.RESERVE_NARROW else Dims.RESERVE_WIDE
        // Content has SCREEN_PAD of empty padding first, so only a top bar taller than that covers anything.
        if (hudHeight > reservation + Dims.SCREEN_PAD + Dims.EPS) {
            v += Violation(ViolationKind.OVERLAP, "hud", "top bar is ${hudHeight.fmt()} dp tall but content starts at ${(reservation + Dims.SCREEN_PAD).fmt()} dp")
        }
        for ((ctl, item) in hudItems) targetCheck("hud:${ctl.id}", item.width, item.height, needs, v)

        // ---------------------------------------------------------------- activity panel
        val p = screen.panel
        // Split = the current landscape layout; otherwise one scrolling column, as in portrait.
        val landscape = device.landscape && c.landscapeSplit
        val outerW = if (landscape) (w - 2 * Dims.SCREEN_PAD - Dims.LANDSCAPE_GAP) / 2 else w - 2 * Dims.SCREEN_PAD
        val innerW = outerW - 2 * Dims.PANEL_PAD
        val cols = c.columns.coerceIn(1, max(1, p.controls.size))
        val cellW = (innerW - (cols - 1) * Dims.BTN_GAP) / cols
        val btnFont = Dims.BTN_LABEL_SP * contentScale
        val textAvail = when (c.style) {
            ButtonStyle.INLINE -> cellW - 2 * Dims.INLINE_PAD_H - Dims.BTN_ICON - Dims.ICON_GAP
            ButtonStyle.STACKED -> cellW - 2 * Dims.STACK_PAD_H
        }
        val btnHeights = p.controls.map { ctl ->
            val wr = labelChecks("panel:${ctl.id}", ctl.label, lang, btnFont, textAvail, v)
            val textH = wr.lines * Dims.BTN_LABEL_LINE_SP * contentScale
            val h = when (c.style) {
                ButtonStyle.INLINE -> max(Dims.BTN_ICON, textH) + 2 * Dims.INLINE_PAD_V
                ButtonStyle.STACKED -> Dims.BTN_ICON + textH + 2 * Dims.STACK_PAD_V
            }
            max(Dims.BTN_MIN_H, h)
        }
        p.controls.forEachIndexed { i, ctl -> targetCheck("panel:${ctl.id}", cellW, btnHeights[i], needs, v) }

        val statusH = p.status?.let {
            Dims.STATUS_SPACER + wrap(it, lang, Dims.STATUS_SP * contentScale, innerW).lines * Dims.STATUS_LINE_SP * contentScale
        } ?: 0.0
        val illusPortrait = if (p.hasIllustration && c.illustration && !landscape) {
            min(Dims.ILLUS_MAX_H, (w - 2 * Dims.SCREEN_PAD - 2 * Dims.ILLUS_SIDE_PAD) * Dims.ILLUS_ASPECT) + Dims.PORTRAIT_GAP
        } else 0.0
        // y measured from the top of the safe area; the top of the panel's first button row
        var y = reservation + Dims.SCREEN_PAD + illusPortrait + Dims.PANEL_PAD + statusH
        val rowBottoms = mutableListOf<Double>()
        btnHeights.chunked(cols).forEach { r ->
            y += r.max()
            rowBottoms += y
            y += Dims.BTN_GAP
        }
        val panelBottom = y + p.trailingRowLabelLines * Dims.TRAILING_LABEL_LINE_SP * contentScale + p.trailingRowFixedDp + Dims.PANEL_PAD
        val limit = if (landscape) usableH - Dims.SCREEN_PAD - Dims.BOTTOM_PAD else usableH - Dims.BOTTOM_PAD
        p.controls.forEachIndexed { i, ctl ->
            val bottom = rowBottoms[i / cols]
            if (ctl.essential && bottom > limit + Dims.EPS) {
                v += Violation(
                    ViolationKind.ESSENTIAL_HIDDEN, "panel:${ctl.id}",
                    if (landscape) "below the visible area (${bottom.fmt()} > ${limit.fmt()} dp, not scrollable)"
                    else "needs scrolling to reach (${bottom.fmt()} > ${limit.fmt()} dp)",
                )
            }
        }
        val illusShown = when {
            !p.hasIllustration || !c.illustration -> false
            // The picture takes whatever height the left column has left above the panel.
            landscape -> limit - panelBottom >= Dims.ILLUS_MIN_LANDSCAPE
            else -> true
        }

        // ---------------------------------------------------------------- explicit needs
        val want = needs.textLevel.ordinal
        val contentIdx = c.textLevel.ordinal
        val hudIdx = if (c.hudFollowsTextSize) contentIdx else 0
        if (needs.textIsNeed) {
            if (contentIdx < want) v += Violation(ViolationKind.TEXT_NEED, "panel", "text ${c.textLevel} is smaller than the needed ${needs.textLevel}")
            if (hudIdx < want) v += Violation(ViolationKind.TEXT_NEED, "hud", "top bar labels ignore the needed ${needs.textLevel} text size")
        }
        if (needs.reducedMotion && !c.motionOff) v += Violation(ViolationKind.MOTION, "screen", "animation shown although reduced motion is on")

        val deficit = max(0, want - contentIdx) + max(0, want - hudIdx)
        return Evaluation(v.distinct(), hudHeight, illusShown, deficit, ceil(p.controls.size / cols.toDouble()).toInt())
    }

    private fun targetCheck(where: String, wd: Double, ht: Double, needs: Needs, out: MutableList<Violation>) {
        val t = needs.minTargetDp.toDouble()
        if (wd + Dims.EPS < t || ht + Dims.EPS < t) {
            out += Violation(ViolationKind.TARGET_TOO_SMALL, where, "${wd.fmt()}×${ht.fmt()} dp is below the ${needs.minTargetDp} dp minimum")
        }
    }
}

internal fun Double.fmt(): String = "%.0f".format(java.util.Locale.ROOT, this)
