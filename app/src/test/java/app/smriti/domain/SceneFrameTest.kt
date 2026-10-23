package app.smriti.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneFrameTest {
    private val eps = 0.5f

    private fun assertSafeVisible(cw: Float, ch: Float, iw: Float, ih: Float, safe: NormRect) {
        val f = SceneFrame.compute(cw, ch, iw, ih, safe)
        val r = f.map(safe)
        assertTrue("safe left ${r.left}", r.left >= -eps)
        assertTrue("safe top ${r.top}", r.top >= -eps)
        assertTrue("safe right ${r.right} > $cw", r.right <= cw + eps)
        assertTrue("safe bottom ${r.bottom} > $ch", r.bottom <= ch + eps)
        // Uniform scale: displayed aspect ratio equals the image's aspect ratio (never stretched).
        val d = f.displayed
        assertEquals(iw / ih, d.width / d.height, 0.001f)
    }

    @Test
    fun `same aspect fills exactly`() {
        val f = SceneFrame.compute(1920f, 1080f, 1920f, 1080f, NormRect(0.2f, 0.2f, 0.8f, 0.8f))
        assertEquals(1f, f.scale, 0.0001f)
        assertEquals(0f, f.offsetX, eps)
        assertEquals(0f, f.offsetY, eps)
    }

    @Test
    fun `wide phone landscape crops top and bottom but keeps safe area`() {
        val safe = NormRect(0.15f, 0.12f, 0.85f, 0.88f)
        assertSafeVisible(2400f, 1080f, 1920f, 1080f, safe)
        val f = SceneFrame.compute(2400f, 1080f, 1920f, 1080f, safe)
        // Fills the width (cover) because the safe area still fits vertically.
        assertEquals(2400f, f.displayed.width, eps)
    }

    @Test
    fun `tablet 4 by 3 keeps safe area and covers when possible`() {
        val safe = NormRect(0.2f, 0.1f, 0.8f, 0.9f)
        assertSafeVisible(2048f, 1536f, 1920f, 1080f, safe)
        val f = SceneFrame.compute(2048f, 1536f, 1920f, 1080f, safe)
        assertEquals(1536f, f.displayed.height, eps)
    }

    @Test
    fun `safe area too wide for container letterboxes instead of cutting objects`() {
        val safe = NormRect(0.02f, 0.3f, 0.98f, 0.7f)
        assertSafeVisible(1080f, 2400f, 1920f, 1080f, safe)
        val f = SceneFrame.compute(1080f, 2400f, 1920f, 1080f, safe)
        assertTrue(f.displayed.height < 2400f)
        assertEquals((2400f - f.displayed.height) / 2f, f.offsetY, eps)
    }

    @Test
    fun `off-centre safe area is followed and clamped inside the image`() {
        val safe = NormRect(0.7f, 0.1f, 0.98f, 0.9f)
        assertSafeVisible(1200f, 1080f, 1920f, 1080f, safe)
        val f = SceneFrame.compute(1200f, 1080f, 1920f, 1080f, safe)
        // Image still covers the container: no gap on the right.
        assertTrue(f.displayed.right >= 1200f - eps)
        assertTrue(f.displayed.left <= eps)
    }

    @Test
    fun `hotspot mapping is consistent with the frame`() {
        val f = SceneFrame(scale = 0.5f, offsetX = 10f, offsetY = 20f, imageWidth = 1000f, imageHeight = 800f)
        val r = f.map(NormRect(0.1f, 0.25f, 0.2f, 0.5f))
        assertEquals(60f, r.left, 0.01f)
        assertEquals(120f, r.top, 0.01f)
        assertEquals(110f, r.right, 0.01f)
        assertEquals(220f, r.bottom, 0.01f)
    }

    @Test
    fun `many window sizes never hide the safe area`() {
        val safe = NormRect(0.1f, 0.15f, 0.9f, 0.85f)
        for (w in listOf(320f, 480f, 720f, 1080f, 1600f, 2560f, 3200f)) {
            for (h in listOf(320f, 600f, 1080f, 1600f, 2400f)) {
                assertSafeVisible(w, h, 1920f, 1080f, safe)
                assertSafeVisible(w, h, 1080f, 1920f, safe)
            }
        }
    }

    @Test
    fun `hotspots stay below the HUD row on a short landscape phone`() {
        val safe = NormRect(0.03f, 0.17f, 0.75f, 0.88f)
        val hud = 230f
        val f = SceneFrame.compute(2400f, 1080f, 1920f, 1080f, safe, Insets(top = hud))
        val r = f.map(safe)
        assertTrue("safe top ${r.top} under HUD", r.top >= hud - eps)
        assertTrue(r.bottom <= 1080f + eps)
        assertEquals(1920f / 1080f, f.displayed.width / f.displayed.height, 0.001f)
    }

    @Test
    fun `insets that leave room keep full cover`() {
        val safe = NormRect(0.3f, 0.3f, 0.7f, 0.7f)
        val f = SceneFrame.compute(1920f, 1080f, 1920f, 1080f, safe, Insets(top = 200f))
        assertEquals(1f, f.scale, 0.0001f)
        assertEquals(0f, f.offsetY, eps)
        assertTrue(f.map(safe).top >= 200f)
    }
}
