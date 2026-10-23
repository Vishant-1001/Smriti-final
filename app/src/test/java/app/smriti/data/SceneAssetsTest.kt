package app.smriti.data

import app.smriti.domain.SceneId
import app.smriti.domain.artKey
import app.smriti.interactions.HotspotCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Checks the generated art and layouts against the code: every illustrated scene has landscape and
 * portrait art, every hotspot in the art has a meaning in the catalog (and vice versa), hit areas
 * are inside the image, inside the protected safe area, and do not overlap each other.
 */
class SceneAssetsTest {
    private val assets = listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.exists() }

    private fun layout(scene: SceneId, variant: LayoutVariant): SceneLayout {
        val f = File(assets, "scenes/${scene.artKey}_${variant.key}.json")
        assertTrue("missing $f", f.exists())
        return AssetSceneLayouts.parse(f.readText())
    }

    @Test
    fun `every illustrated scene has art and layouts in both orientations`() {
        for (scene in SceneId.entries.filter { it.isIllustratedScene }) {
            for (variant in LayoutVariant.entries) {
                val l = layout(scene, variant)
                assertTrue("image ${l.image}", File(assets, l.image).exists())
                if (variant == LayoutVariant.LANDSCAPE) assertTrue(l.width > l.height) else assertTrue(l.height > l.width)
            }
        }
    }

    @Test
    fun `hotspot ids match the catalog exactly`() {
        for (scene in SceneId.entries.filter { it.isIllustratedScene }) {
            val expected = HotspotCatalog.forScene(scene).map { it.id }.toSet()
            for (variant in LayoutVariant.entries) {
                assertEquals("$scene/$variant", expected, layout(scene, variant).hotspots.map { it.id }.toSet())
            }
        }
    }

    @Test
    fun `hotspots are inside the image and the safe area and do not overlap`() {
        for (scene in SceneId.entries.filter { it.isIllustratedScene }) {
            for (variant in LayoutVariant.entries) {
                val l = layout(scene, variant)
                val safe = l.safeArea
                l.hotspots.forEach { h ->
                    val r = h.rect
                    assertTrue("$scene/$variant/${h.id} in image", r.left >= 0f && r.top >= 0f && r.right <= 1f && r.bottom <= 1f)
                    assertTrue("$scene/$variant/${h.id} in safe area",
                        r.left >= safe.left - 0.001f && r.top >= safe.top - 0.001f && r.right <= safe.right + 0.001f && r.bottom <= safe.bottom + 0.001f)
                    assertTrue("$scene/$variant/${h.id} has size", r.width > 0.03f && r.height > 0.03f)
                }
                for (i in l.hotspots.indices) for (j in i + 1 until l.hotspots.size) {
                    val a = l.hotspots[i].rect
                    val b = l.hotspots[j].rect
                    val overlap = a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom
                    assertFalse("$scene/$variant ${l.hotspots[i].id} overlaps ${l.hotspots[j].id}", overlap)
                }
            }
        }
    }

    @Test
    fun `runtime assets never include the reference atlases`() {
        val bad = assets.walkTopDown().filter { it.isFile && "atlas" in it.name.lowercase() }.toList()
        assertTrue("reference atlases found in runtime assets: $bad", bad.isEmpty())
    }
}
