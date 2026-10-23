package app.smriti.fit.sim

import app.smriti.fit.Control
import app.smriti.fit.HudSpec
import app.smriti.fit.Lang
import app.smriti.fit.PanelSpec
import app.smriti.fit.Screen
import app.smriti.fit.TextMeasurer
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.io.File

/**
 * Measures text with the app's bundled bold Noto fonts (`app/src/main/res/font`) via AWT
 * TextLayout (HarfBuzz shaping, fractional metrics). A run of characters the Tamil font cannot
 * draw (digits, '/', Latin) falls back to Noto Sans, as Android's font fallback would.
 */
class AwtMeasurer(resDir: File) : TextMeasurer {
    private val tamil = Font.createFont(Font.TRUETYPE_FONT, File(resDir, "font/noto_sans_tamil_bold.ttf"))
    private val latin = Font.createFont(Font.TRUETYPE_FONT, File(resDir, "font/noto_sans_bold.ttf"))
    private val frc = FontRenderContext(null, true, true)

    override fun widthDp(text: String, lang: Lang, fontSizeDp: Double): Double {
        if (text.isEmpty()) return 0.0
        val primary = (if (lang == Lang.TAMIL) tamil else latin).deriveFont(fontSizeDp.toFloat())
        val fallback = latin.deriveFont(fontSizeDp.toFloat())
        var total = 0.0
        var start = 0
        while (start < text.length) {
            val usePrimary = primary.canDisplay(text.codePointAt(start))
            var end = start
            while (end < text.length && primary.canDisplay(text.codePointAt(end)) == usePrimary) end += Character.charCount(text.codePointAt(end))
            val run = text.substring(start, end)
            total += TextLayout(run, if (usePrimary) primary else fallback, frc).advance
            start = end
        }
        return total
    }
}

/** Parses `values/strings.xml` (English) and `values-ta/strings.xml` (Tamil) into name → text. */
object StringsXml {
    private val entry = Regex("""<string name="([a-z0-9_]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)

    fun load(file: File): Map<String, String> = entry.findAll(file.readText(Charsets.UTF_8)).associate { m ->
        m.groupValues[1] to m.groupValues[2]
            .replace("\\'", "'").replace("\\\"", "\"").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
    }
}

/**
 * Real SMRITI content. The radio screen and the top bars are the screens the current rules were
 * written for ("known content"). Other short UI strings form a pool for new activity panels,
 * split by a stable hash of the resource name into a development half and a held-out half.
 */
class Content(resDir: File) {
    val en = StringsXml.load(File(resDir, "values/strings.xml"))
    val ta = StringsXml.load(File(resDir, "values-ta/strings.xml"))

    fun text(name: String, lang: Lang): String = (if (lang == Lang.TAMIL) ta else en).getValue(name)

    private fun usable(name: String) = name in en && name in ta && '%' !in en.getValue(name) && '%' !in ta.getValue(name)

    /** Names reserved for the real screens and the top bar. */
    private val reserved = setOf(
        "action_back", "action_home", "action_settings", "action_guide", "action_end_session",
        "radio_play", "radio_pause", "radio_stop", "radio_next", "radio_not_now", "hud_hint", "hud_list", "market_go_home",
    )

    private fun isButtonLike(name: String): Boolean {
        val v = en.getValue(name)
        return v.length <= 30 && !v.endsWith(".") && !v.endsWith("!") && !name.startsWith("guide_help") &&
            !name.startsWith("scene_") && !name.startsWith("stall_") && name != "app_name" && name !in reserved
    }

    private val buttonNames = en.keys.filter { usable(it) && isButtonLike(it) }.sorted()
    private val titleNames = en.keys.filter { usable(it) && (it.startsWith("scene_") || it.startsWith("stall_") || it.startsWith("trip_title_")) }.sorted()
    private val statusNames = en.keys.filter { usable(it) && !isButtonLike(it) && it !in reserved && en.getValue(it).length <= 110 }.sorted()

    /** Stable split: java.lang.String.hashCode is specified by the JLS, so this is reproducible. */
    private fun inSplit(name: String, eval: Boolean) = (Math.floorMod(name.hashCode(), 2) == 1) == eval

    fun buttonPool(eval: Boolean) = buttonNames.filter { inSplit(it, eval) }
    fun titlePool(eval: Boolean) = titleNames.filter { inSplit(it, eval) }
    fun statusPool(eval: Boolean) = statusNames.filter { inSplit(it, eval) }

    private fun c(name: String, lang: Lang, essential: Boolean = true, suffix: String = "") = Control(name, text(name, lang) + suffix, essential)

    /** Top bars as drawn by `SmritiRoot.TopBar` in four kinds of place. */
    fun hud(variant: Int, title: String, lang: Lang): HudSpec = when (variant % 4) {
        0 -> HudSpec(listOf(c("action_back", lang), c("action_home", lang)), title, listOf(c("action_guide", lang)), listOf(c("action_settings", lang)))
        1 -> HudSpec(listOf(c("action_back", lang)), title, listOf(c("market_go_home", lang), c("hud_list", lang, suffix = " 2/5")), listOf(c("action_settings", lang)))
        2 -> HudSpec(listOf(c("action_back", lang)), title, listOf(c("hud_hint", lang), c("hud_list", lang, suffix = " 2/5")), listOf(c("action_settings", lang)))
        else -> HudSpec(listOf(c("action_back", lang)), title, listOf(c("action_end_session", lang)), listOf(c("action_settings", lang)))
    }

    /** The real radio screen while a song plays (`RadioScreen`): Pause and Stop are essential. */
    fun radio(lang: Lang): Screen = Screen(
        "radio", lang,
        hud(0, text("scene_radio", lang), lang),
        PanelSpec(
            status = text("radio_status_playing", lang) + " · " + text("radio_demo_1", lang),
            controls = listOf(c("radio_pause", lang), c("radio_stop", lang), c("radio_next", lang, false), c("radio_not_now", lang, false)),
            hasIllustration = true,
        ),
    )

    /** A new activity panel made of real strings the current rules were never written for. */
    fun generated(id: String, lang: Lang, hudVariant: Int, title: String, status: String?, buttons: List<String>, illustration: Boolean): Screen =
        Screen(
            id, lang, hud(hudVariant, text(title, lang), lang),
            PanelSpec(status?.let { text(it, lang) }, buttons.mapIndexed { i, b -> c(b, lang, essential = i < 2) }, illustration),
        )
}
