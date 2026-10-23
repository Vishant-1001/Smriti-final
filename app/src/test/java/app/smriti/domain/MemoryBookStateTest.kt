package app.smriti.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryBookStateTest {

    @Test
    fun `default book contains only clearly marked samples`() {
        val book = MemoryBookState()
        assertTrue(book.pages.isNotEmpty())
        assertTrue(book.pages.all { it.isSample && it.caption == null && it.photoFile == null })
    }

    @Test
    fun `page turning stops at both ends`() {
        var b = MemoryBookState()
        assertFalse(b.hasPrevious)
        b = b.previous()
        assertEquals(0, b.currentIndex)
        repeat(10) { b = b.next() }
        assertEquals(b.pages.lastIndex, b.currentIndex)
        assertFalse(b.hasNext)
    }

    @Test
    fun `personal page is added before samples and shown`() {
        val b = MemoryBookState().next().addPersonal("p1", "  Our garden  ", "photo.img")
        assertEquals("p1", b.pages.first().id)
        assertEquals("Our garden", b.pages.first().caption)
        assertEquals(0, b.currentIndex)
        assertFalse(b.pages.first().isSample)
    }

    @Test
    fun `edit only changes personal pages`() {
        val b = MemoryBookState().addPersonal("p1", "A", null)
            .editCaption("p1", "B")
            .editCaption("sample_kolam", "should not apply")
        assertEquals("B", b.pages.first { it.id == "p1" }.caption)
        assertNull(b.pages.first { it.id == "sample_kolam" }.caption)
    }

    @Test
    fun `delete keeps the index valid`() {
        var b = MemoryBookState()
        repeat(2) { b = b.next() }
        b = b.remove(b.pages.last().id)
        assertEquals(b.pages.lastIndex, b.currentIndex)
        b = b.removeSamples()
        assertTrue(b.pages.isEmpty())
        assertNull(b.currentPage)
        assertEquals(0, b.currentIndex)
    }

    @Test
    fun `samples can be removed and restored without touching personal pages`() {
        val b = MemoryBookState().addPersonal("p1", "Mine", null).removeSamples()
        assertEquals(listOf("p1"), b.pages.map { it.id })
        val restored = b.restoreSamples()
        assertEquals(1 + MemoryBookState.DEFAULT_SAMPLE_PAGES.size, restored.pages.size)
        assertEquals("p1", restored.pages.first().id)
    }

    @Test
    fun `normalized clamps a bad index from disk`() {
        assertEquals(2, MemoryBookState(currentIndex = 99).normalized().currentIndex)
        assertEquals(0, MemoryBookState(currentIndex = -3).normalized().currentIndex)
    }
}
