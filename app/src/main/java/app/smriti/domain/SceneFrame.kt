package app.smriti.domain

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/** A rectangle in normalised image coordinates (0..1 on both axes). */
@Serializable
data class NormRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

data class PxRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * How a scene illustration is placed in the available window: a uniform scale (never stretched)
 * and an offset. Hotspots are mapped through the same frame, so they stay on their objects.
 */
data class SceneFrame(val scale: Float, val offsetX: Float, val offsetY: Float, val imageWidth: Float, val imageHeight: Float) {
    val displayed: PxRect get() = PxRect(offsetX, offsetY, offsetX + imageWidth * scale, offsetY + imageHeight * scale)

    fun map(r: NormRect): PxRect = PxRect(
        left = offsetX + r.left * imageWidth * scale,
        top = offsetY + r.top * imageHeight * scale,
        right = offsetX + r.right * imageWidth * scale,
        bottom = offsetY + r.bottom * imageHeight * scale,
    )

    companion object {
        /**
         * Fill the container (cover) when that keeps the whole [safe] area visible inside the
         * unobstructed region (the container minus [insets], e.g. the HUD row); otherwise scale down
         * just enough to show the safe area there, letterboxing the rest. Then centre the safe area in
         * the unobstructed region and clamp so the image never leaves an avoidable gap.
         */
        fun compute(
            containerW: Float,
            containerH: Float,
            imageW: Float,
            imageH: Float,
            safe: NormRect,
            insets: Insets = Insets.NONE,
        ): SceneFrame {
            require(containerW > 0 && containerH > 0 && imageW > 0 && imageH > 0)
            val innerW = (containerW - insets.left - insets.right).coerceAtLeast(containerW * 0.5f)
            val innerH = (containerH - insets.top - insets.bottom).coerceAtLeast(containerH * 0.5f)
            val innerLeft = insets.left.coerceAtMost(containerW - innerW)
            val innerTop = insets.top.coerceAtMost(containerH - innerH)
            val cover = max(containerW / imageW, containerH / imageH)
            val safeW = safe.width * imageW
            val safeH = safe.height * imageH
            val safeFit = min(innerW / safeW, innerH / safeH)
            val scale = min(cover, safeFit)
            val dispW = imageW * scale
            val dispH = imageH * scale
            val ox = place(containerW, dispW, innerLeft, innerLeft + innerW, safe.left * dispW, safe.right * dispW)
            val oy = place(containerH, dispH, innerTop, innerTop + innerH, safe.top * dispH, safe.bottom * dispH)
            return SceneFrame(scale, ox, oy, imageW, imageH)
        }

        /**
         * Offset along one axis: centre the safe span in the unobstructed span, avoid gaps at the
         * container edges where possible, but never let the safe span slide under an inset.
         */
        private fun place(container: Float, displayed: Float, innerStart: Float, innerEnd: Float, safeStart: Float, safeEnd: Float): Float {
            val slack = container - displayed
            var o = ((innerStart + innerEnd) / 2f - (safeStart + safeEnd) / 2f).coerceIn(min(0f, slack), max(0f, slack))
            val lo = innerStart - safeStart
            val hi = innerEnd - safeEnd
            if (lo <= hi) o = o.coerceIn(lo, hi)
            return o
        }
    }
}

/** Screen areas covered by always-visible controls, in the same units as the container. */
data class Insets(val left: Float = 0f, val top: Float = 0f, val right: Float = 0f, val bottom: Float = 0f) {
    companion object {
        val NONE = Insets()
    }
}
