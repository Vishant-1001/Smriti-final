package app.smriti.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import app.smriti.ui.theme.LocalSystemFontScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import app.smriti.domain.DialogueLine
import app.smriti.domain.LineArg
import app.smriti.ui.theme.LocalSmritiPrefs
import app.smriti.ui.theme.SmritiColors
import kotlinx.coroutines.delay
import java.io.File

// ---------------------------------------------------------------- images

private object BitmapCache {
    // Decoded scene art is large; keep only a few recent images (sized in kilobytes).
    val cache = object : LruCache<String, ImageBitmap>(48 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4 / 1024
    }
}

/** Loads an image bundled under assets/. Returns null (never throws) if the file is missing. */
fun loadAssetBitmap(context: Context, path: String): ImageBitmap? {
    BitmapCache.cache.get("a:$path")?.let { return it }
    val bmp = runCatching { context.assets.open(path).use { BitmapFactory.decodeStream(it) } }.getOrNull() ?: return null
    return bmp.asImageBitmap().also { BitmapCache.cache.put("a:$path", it) }
}

/** Loads a caregiver photo from private storage, downsampled to at most ~1600 px. */
fun loadFileBitmap(file: File): ImageBitmap? {
    if (!file.exists()) return null
    BitmapCache.cache.get("f:${file.path}")?.let { return it }
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
        BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
    }.getOrNull()?.also { BitmapCache.cache.put("f:${file.path}", it) }
}

@Composable
fun rememberAssetBitmap(path: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(path) { loadAssetBitmap(context, path) }
}

// ---------------------------------------------------------------- buttons

val PanelShape = RoundedCornerShape(24.dp)
val ButtonShape = RoundedCornerShape(18.dp)

/** Large primary action. Minimum 64 dp tall so it is easy to hit. */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = SmritiColors.Terracotta,
    enabled: Boolean = true,
    stacked: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 64.dp),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = if (stacked) 10.dp else 20.dp, vertical = if (stacked) 8.dp else 12.dp),
    ) { IconLabel(text, icon, stacked, 28.dp) }
}

@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    stacked: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 60.dp),
        shape = ButtonShape,
        border = BorderStroke(2.dp, SmritiColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = SmritiColors.Paper, contentColor = SmritiColors.Ink),
        contentPadding = PaddingValues(horizontal = if (stacked) 10.dp else 18.dp, vertical = if (stacked) 8.dp else 10.dp),
    ) { IconLabel(text, icon, stacked, 26.dp) }
}

/**
 * Icon and label side by side, or stacked (icon above) where width is tight, so long Tamil words
 * get the full button width instead of breaking mid-word.
 */
@Composable
private fun IconLabel(text: String, icon: ImageVector?, stacked: Boolean, iconSize: Dp) {
    if (stacked) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
            Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    } else {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/** Compact HUD control: icon in a paper disc with a short label underneath, always readable over art. */
@Composable
fun HudButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minWidth = 76.dp, minHeight = 64.dp),
        shape = RoundedCornerShape(20.dp),
        color = SmritiColors.Paper.copy(alpha = 0.94f),
        border = BorderStroke(2.dp, SmritiColors.Outline),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = SmritiColors.Ink, modifier = Modifier.size(28.dp))
            // HUD labels follow the system font scale but not the in-app multiplier, so the
            // always-visible controls still fit beside each other on small phones.
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, minOf(d.fontScale, LocalSystemFontScale.current))) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = SmritiColors.Ink, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

/** Paper card used for every panel and dialog. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .shadow(10.dp, PanelShape)
            .clip(PanelShape)
            .background(SmritiColors.Paper)
            .border(2.5.dp, SmritiColors.Outline, PanelShape)
            .padding(20.dp),
        content = content,
    )
}

@Composable
fun Badge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
    )
}

// ---------------------------------------------------------------- guide portrait

/**
 * The SMRITI Guide. While [speaking] the two authored frames alternate (a real two-frame mouth
 * animation); with reduced motion, or when silent, the neutral frame is shown.
 */
@Composable
fun GuidePortrait(speaking: Boolean, size: Dp, modifier: Modifier = Modifier, portrait: String = "guide") {
    val reduced = LocalSmritiPrefs.current.reducedMotion
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(speaking, reduced) {
        open = false
        if (speaking && !reduced) {
            while (true) {
                delay(190)
                open = !open
            }
        }
    }
    val neutral = rememberAssetBitmap("characters/${portrait}_neutral.webp")
    val talking = rememberAssetBitmap("characters/${portrait}_speaking.webp")
    val frame = if (open) talking ?: neutral else neutral
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(SmritiColors.TerracottaLight)
            .border(3.dp, SmritiColors.Outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (frame != null) {
            Image(frame, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        }
    }
}

@Composable
fun RowSpacer(w: Dp = 12.dp) = Spacer(Modifier.width(w))

@Composable
fun ButtonRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) { content() }
}

/** A bundled illustration (sprite) by asset path; draws nothing if the file is missing. */
@Composable
fun Sprite(path: String, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val bmp = rememberAssetBitmap(path)
    if (bmp != null) Image(bmp, contentDescription = contentDescription, contentScale = ContentScale.Fit, modifier = modifier)
    else Box(modifier)
}

/** A dialogue line with its arguments resolved in the current language. */
@Composable
fun lineText(line: DialogueLine): String {
    val args = line.args.map { a ->
        when (a) {
            is LineArg.Res -> stringResource(a.id)
            is LineArg.Num -> a.value.toString()
        }
    }
    return if (args.isEmpty()) stringResource(line.text) else stringResource(line.text, *args.toTypedArray())
}

/**
 * Compact labelled action for activity toolbars (Repeat, Show me, Do it for me, Undo, Start again).
 * Same size everywhere so the controls are predictable.
 */
@Composable
fun ToolButton(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minWidth = 88.dp, minHeight = 64.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (enabled) SmritiColors.Paper else SmritiColors.PaperDim,
        border = BorderStroke(2.dp, SmritiColors.Outline.copy(alpha = if (enabled) 1f else 0.4f)),
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = SmritiColors.Ink.copy(alpha = if (enabled) 1f else 0.4f), modifier = Modifier.size(26.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = SmritiColors.Ink.copy(alpha = if (enabled) 1f else 0.4f), textAlign = TextAlign.Center)
        }
    }
}
