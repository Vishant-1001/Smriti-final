package app.smriti.ui.scene

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.smriti.R
import app.smriti.data.LayoutVariant
import app.smriti.data.SceneLayout
import app.smriti.data.SceneLayoutSource
import app.smriti.domain.Insets
import app.smriti.domain.PxRect
import app.smriti.domain.SceneFrame
import app.smriti.domain.SceneId
import app.smriti.interactions.GameAction
import app.smriti.interactions.HotspotCatalog
import app.smriti.ui.components.rememberAssetBitmap
import app.smriti.ui.theme.SmritiColors
import kotlin.math.roundToInt

private const val MIN_TOUCH_DP = 64f

/** Height of the top bar (one row, or two rows on narrow screens), see TopBar in SmritiRoot. */
private const val HUD_DP = 84f
private const val NARROW_HUD_DP = 136f

/** Footprint of the floating Guide button including its margin. */
private const val GUIDE_CORNER_W_DP = 140f
private const val GUIDE_CORNER_H_DP = 160f

/**
 * An illustrated scene with its tappable objects. The artwork is never stretched: it is placed with
 * [SceneFrame], and every hotspot is mapped through the same frame so it stays on its object when
 * the window size, orientation or aspect ratio changes.
 */
@Composable
fun IllustratedScene(
    scene: SceneId,
    layouts: SceneLayoutSource,
    showNames: Boolean,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier,
    /** Floating controls (Guide, music) drawn over the scene; told which bottom corner is free of hotspots. */
    corners: @Composable BoxScope.(guideOnRight: Boolean) -> Unit = {},
) {
    BoxWithConstraints(modifier.fillMaxSize().background(SmritiColors.Cream)) {
        val density = LocalDensity.current
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val variant = if (wPx >= hPx) LayoutVariant.LANDSCAPE else LayoutVariant.PORTRAIT
        val layout: SceneLayout? = layouts.layout(scene, variant) ?: layouts.layout(scene, LayoutVariant.LANDSCAPE)
        if (layout == null || wPx <= 0f || hPx <= 0f) return@BoxWithConstraints
        val image = rememberAssetBitmap(layout.image)
        // Keep every hotspot clear of the always-visible top bar and the system bars.
        val bars = WindowInsets.safeDrawing
        val hudPx = with(density) { (if (maxWidth < 600.dp) NARROW_HUD_DP else HUD_DP).dp.toPx() }
        val insets = Insets(
            left = bars.getLeft(density, LayoutDirection.Ltr).toFloat(),
            top = bars.getTop(density) + hudPx,
            right = bars.getRight(density, LayoutDirection.Ltr).toFloat(),
            // The gesture-navigation strip still receives taps, so it is not reserved.
            bottom = 0f,
        )
        val frame = SceneFrame.compute(wPx, hPx, layout.width.toFloat(), layout.height.toFloat(), layout.safeArea, insets)

        Canvas(Modifier.fillMaxSize()) { drawScene(image, frame) }

        val minTouch = with(density) { MIN_TOUCH_DP.dp.toPx() }
        layout.hotspots.forEach { hs ->
            val spec = HotspotCatalog.find(scene, hs.id) ?: return@forEach
            val r = ensureMinSize(frame.map(hs.rect), minTouch)
            val label = stringResource(spec.label)
            val comingLater = spec.action is GameAction.NotYetAvailable
            val shape = RoundedCornerShape(18.dp)
            Box(
                Modifier
                    .offset { IntOffset(r.left.roundToInt(), r.top.roundToInt()) }
                    .requiredSize(with(density) { DpSize(r.width.toDp(), r.height.toDp()) })
                    .border(3.dp, SmritiColors.HotspotRing.copy(alpha = if (showNames) 0.95f else 0.7f), shape)
                    .background(Color.White.copy(alpha = if (showNames) 0.14f else 0.06f), shape)
                    .clickable(role = Role.Button, onClickLabel = label) { onAction(spec.action) }
                    .semantics {
                        contentDescription = label
                        traversalIndex = spec.order.toFloat()
                    }
                    .testTag("hotspot_${hs.id}"),
                contentAlignment = Alignment.BottomCenter,
            ) {
                if (showNames) {
                    val text = if (comingLater) "$label · ${stringResource(R.string.hotspot_coming_later)}" else label
                    Text(
                        text,
                        // The name may be wider than a narrow doorway; let it extend past the outline.
                        modifier = Modifier
                            .wrapContentWidth(unbounded = true)
                            .widthIn(max = with(density) { r.width.toDp() } + 24.dp)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SmritiColors.Ink.copy(alpha = 0.86f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // The Guide lives bottom-left unless that would cover a hotspot and bottom-right is clear.
        val guideW = with(density) { GUIDE_CORNER_W_DP.dp.toPx() }
        val guideH = with(density) { GUIDE_CORNER_H_DP.dp.toPx() } + bars.getBottom(density)
        val mapped = layout.hotspots.map { frame.map(it.rect) }
        val leftCorner = PxRect(0f, hPx - guideH, guideW, hPx)
        val rightCorner = PxRect(wPx - guideW, hPx - guideH, wPx, hPx)
        val guideOnRight = mapped.any { it.intersects(leftCorner) } && mapped.none { it.intersects(rightCorner) }
        corners(guideOnRight)
    }
}

private fun PxRect.intersects(o: PxRect) = left < o.right && o.left < right && top < o.bottom && o.top < bottom

private fun DrawScope.drawScene(image: ImageBitmap?, frame: SceneFrame) {
    if (image == null) return
    val d = frame.displayed
    // Letterbox areas (only when the safe area cannot otherwise fit) are filled with a soft,
    // heavily dimmed stretch of the same art so there are no hard bars.
    if (d.left > 0.5f || d.top > 0.5f || d.right < size.width - 0.5f || d.bottom < size.height - 0.5f) {
        val cover = maxOf(size.width / image.width, size.height / image.height)
        val cw = image.width * cover
        val ch = image.height * cover
        drawImage(
            image,
            dstOffset = IntOffset(((size.width - cw) / 2).roundToInt(), ((size.height - ch) / 2).roundToInt()),
            dstSize = IntSize(cw.roundToInt(), ch.roundToInt()),
            alpha = 0.35f,
        )
        drawRect(SmritiColors.Cream.copy(alpha = 0.55f))
    }
    drawImage(
        image,
        dstOffset = IntOffset(d.left.roundToInt(), d.top.roundToInt()),
        dstSize = IntSize(d.width.roundToInt(), d.height.roundToInt()),
    )
}

private fun ensureMinSize(r: PxRect, min: Float): PxRect {
    val w = maxOf(r.width, min)
    val h = maxOf(r.height, min)
    val cx = (r.left + r.right) / 2f
    val cy = (r.top + r.bottom) / 2f
    return PxRect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
}
