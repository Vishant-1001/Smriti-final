package app.smriti.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import app.smriti.R
import app.smriti.domain.AppLanguage
import app.smriti.domain.Settings

/** Palette tokens, mirrored in docs/ART_BIBLE.md. UI colours meet WCAG AA against their surfaces. */
object SmritiColors {
    val Cream = Color(0xFFF6E7C8)
    val Paper = Color(0xFFFFF9EE)
    val PaperDim = Color(0xFFF1E2C4)
    val Ink = Color(0xFF3A2418)           // body text on paper: ~13:1
    val InkSoft = Color(0xFF5E4334)       // secondary text on paper: ~8:1
    val Outline = Color(0xFF4A2E22)
    val Terracotta = Color(0xFFA44A26)    // primary buttons, white text ~5.6:1
    val TerracottaLight = Color(0xFFE9B597)
    val Leaf = Color(0xFF3F6B3A)          // secondary buttons, white text ~6.3:1
    val Indigo = Color(0xFF34427E)
    val Maroon = Color(0xFF7A2633)
    val Brass = Color(0xFFE2B04A)
    val Wood = Color(0xFF8A5230)
    val HotspotRing = Color(0xFFFFF6DC)
    val Scrim = Color(0x993A2418)
}

private val TamilFamily = FontFamily(
    Font(R.font.noto_sans_tamil_regular, FontWeight.Normal),
    Font(R.font.noto_sans_tamil_bold, FontWeight.Bold),
)
private val LatinFamily = FontFamily(
    Font(R.font.noto_sans_regular, FontWeight.Normal),
    Font(R.font.noto_sans_bold, FontWeight.Bold),
)

data class SmritiPrefs(val reducedMotion: Boolean, val language: AppLanguage)

/** The device font scale before SMRITI's own text-size multiplier is applied. */
val LocalSystemFontScale = staticCompositionLocalOf { 1f }

val LocalSmritiPrefs = staticCompositionLocalOf { SmritiPrefs(reducedMotion = false, language = AppLanguage.TAMIL) }

private fun typography(family: FontFamily): Typography {
    // Tamil glyph stacks need generous line height; sizes start large for readability.
    fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
        TextStyle(fontFamily = family, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)
    return Typography(
        displaySmall = style(40, 54, FontWeight.Bold),
        headlineSmall = style(26, 36, FontWeight.Bold),
        titleLarge = style(22, 32, FontWeight.Bold),
        titleMedium = style(19, 28, FontWeight.Bold),
        bodyLarge = style(19, 30),
        bodyMedium = style(17, 26),
        labelLarge = style(18, 26, FontWeight.Bold),
        labelMedium = style(15, 22, FontWeight.Bold),
    )
}

@Composable
fun SmritiTheme(settings: Settings, content: @Composable () -> Unit) {
    val family = if (settings.language == AppLanguage.TAMIL) TamilFamily else LatinFamily
    val base = LocalDensity.current
    // The in-app text size multiplies the system font scale, so both preferences are respected.
    val density = Density(base.density, base.fontScale * settings.textSize.scale)
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = SmritiColors.Terracotta,
            onPrimary = Color.White,
            secondary = SmritiColors.Leaf,
            onSecondary = Color.White,
            background = SmritiColors.Cream,
            onBackground = SmritiColors.Ink,
            surface = SmritiColors.Paper,
            onSurface = SmritiColors.Ink,
            onSurfaceVariant = SmritiColors.InkSoft,
            outline = SmritiColors.Outline,
        ),
        typography = typography(family),
    ) {
        CompositionLocalProvider(
            LocalDensity provides density,
            LocalSystemFontScale provides base.fontScale,
            LocalSmritiPrefs provides SmritiPrefs(settings.reducedMotion, settings.language),
            content = content,
        )
    }
}
