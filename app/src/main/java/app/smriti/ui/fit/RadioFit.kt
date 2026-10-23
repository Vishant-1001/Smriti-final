package app.smriti.ui.fit

import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import app.smriti.R
import app.smriti.domain.AppLanguage
import app.smriti.domain.Settings
import app.smriti.fit.CachingMeasurer
import app.smriti.fit.Config
import app.smriti.fit.Control
import app.smriti.fit.Decision
import app.smriti.fit.Device
import app.smriti.fit.FitSolver
import app.smriti.fit.HudSpec
import app.smriti.fit.Lang
import app.smriti.fit.Needs
import app.smriti.fit.PanelSpec
import app.smriti.fit.Screen
import app.smriti.fit.TextLevel
import app.smriti.fit.TextMeasurer
import app.smriti.ui.theme.LocalSystemFontScale

/**
 * SMRITI Fit on the radio screen (docs/flagship/FIT_SPEC.md). When enabled, the radio screen's
 * layout (top-bar rows, button columns and style, picture, text level) is chosen by the Fit solver
 * from the person's explicit settings and the window size, instead of fixed width thresholds.
 * Nothing about the person's behaviour is read or stored.
 */
object FitFlags {
    /** Enabled by default only if the V6 real-render comparison allowed it (FIT_PROTOCOL_v2.md). */
    @Volatile
    var radioEnabled: Boolean = false
}

/** Compose text measurement for the solver: width in dp of bold text at a font size in dp. */
private class ComposeMeasurer(
    private val measurer: androidx.compose.ui.text.TextMeasurer,
    private val family: FontFamily?,
) : TextMeasurer {
    private val unit = Density(1f, 1f) // 1 px == 1 dp and 1 sp == 1 dp, so sizes pass through unchanged
    override fun widthDp(text: String, lang: Lang, fontSizeDp: Double): Double =
        measurer.measure(
            text, TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = fontSizeDp.sp),
            softWrap = false, maxLines = 1, density = unit,
        ).size.width.toDouble()
}

/**
 * The radio screen as the solver sees it, built from the strings actually shown. Play/Pause and the
 * status line use their longest variants, so starting or pausing a song never changes the layout.
 */
@Composable
private fun radioScreen(lang: Lang, selectedTitle: String?): Screen {
    val play = stringResource(R.string.radio_play)
    val pause = stringResource(R.string.radio_pause)
    val status = listOf(R.string.radio_status_playing, R.string.radio_status_paused, R.string.radio_status_stopped)
        .map { stringResource(it) }.maxBy { it.length } + (selectedTitle?.let { " · $it" } ?: "")
    return Screen(
        "radio", lang,
        HudSpec(
            listOf(Control("action_back", stringResource(R.string.action_back)), Control("action_home", stringResource(R.string.action_home))),
            stringResource(R.string.scene_radio),
            listOf(Control("action_guide", stringResource(R.string.action_guide))),
            listOf(Control("action_settings", stringResource(R.string.action_settings))),
        ),
        PanelSpec(
            status,
            listOf(
                Control("radio_pause", if (pause.length >= play.length) pause else play),
                Control("radio_stop", stringResource(R.string.radio_stop)),
                Control("radio_next", stringResource(R.string.radio_next), essential = false),
                Control("radio_not_now", stringResource(R.string.radio_not_now), essential = false),
            ),
            hasIllustration = true,
        ),
    )
}

/** The solver's decision for the radio screen in a window of [width] × [height]. */
@Composable
fun rememberRadioFit(settings: Settings, width: Dp, height: Dp, selectedTitle: String?): Decision {
    val density = LocalDensity.current
    val dir = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing
    val lang = if (settings.language == AppLanguage.TAMIL) Lang.TAMIL else Lang.ENGLISH
    val screen = radioScreen(lang, selectedTitle)
    val system = LocalSystemFontScale.current.toDouble()
    val family = MaterialTheme.typography.labelLarge.fontFamily
    val textMeasurer = rememberTextMeasurer()
    val solver = remember(textMeasurer, family) { FitSolver(CachingMeasurer(ComposeMeasurer(textMeasurer, family))) }
    val horizontal = with(density) { (insets.getLeft(this, dir) + insets.getRight(this, dir)).toDp() }
    val vertical = with(density) { (insets.getTop(this) + insets.getBottom(this)).toDp() }
    val device = Device(
        widthDp = (width - horizontal).value.toInt(),
        heightDp = height.value.toInt(),
        systemFontScale = system,
        systemBarsDp = vertical.value.toDouble(),
        nonLinearFontScaling = Build.VERSION.SDK_INT >= 34,
    )
    val needs = Needs(
        language = lang,
        textLevel = TextLevel.entries[settings.textSize.ordinal],
        reducedMotion = settings.reducedMotion,
    )
    // Stability: the last decision on this screen is preferred while it still fits.
    val previous = remember { arrayOfNulls<Config>(1) }
    val decision = remember(screen, device, needs, solver) { solver.solve(screen, needs, device, previous = previous[0]) }
    SideEffect { previous[0] = decision.config }
    return decision
}
