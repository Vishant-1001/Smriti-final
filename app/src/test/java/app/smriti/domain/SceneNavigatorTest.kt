package app.smriti.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneNavigatorTest {
    private val start = NavState()

    @Test
    fun `launch to bedroom and back to veranda`() {
        var s = SceneNavigator.goTo(start, SceneId.EXTERIOR)
        s = SceneNavigator.goTo(s, SceneId.VERANDA)
        s = SceneNavigator.goTo(s, SceneId.BEDROOM)
        assertEquals(listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.BEDROOM), s.stack)
        s = SceneNavigator.back(s)
        assertEquals(SceneId.VERANDA, s.current)
    }

    @Test
    fun `back never empties the stack`() {
        val s = SceneNavigator.back(SceneNavigator.back(start))
        assertEquals(SceneId.TITLE, s.current)
        assertFalse(s.canGoBack)
    }

    @Test
    fun `going to a scene already on the stack returns to it instead of growing`() {
        var s = NavState(listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.BEDROOM, SceneId.RADIO))
        s = SceneNavigator.goTo(s, SceneId.VERANDA)
        assertEquals(listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA), s.stack)
    }

    @Test
    fun `home is the veranda once inside and the exterior outside`() {
        val inside = NavState(listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.BEDROOM, SceneId.MEMORY_BOOK))
        assertEquals(SceneId.VERANDA, SceneNavigator.home(inside).current)
        val outside = NavState(listOf(SceneId.TITLE, SceneId.EXTERIOR))
        assertEquals(SceneId.EXTERIOR, SceneNavigator.home(outside).current)
        val caregiverFromTitle = NavState(listOf(SceneId.TITLE, SceneId.CAREGIVER))
        assertEquals(SceneId.EXTERIOR, SceneNavigator.home(caregiverFromTitle).current)
    }

    @Test
    fun `end session returns to title`() {
        assertEquals(listOf(SceneId.TITLE), SceneNavigator.endSession().stack)
    }

    @Test
    fun `sanitize repairs invalid stacks from disk`() {
        assertEquals(NavState(), SceneNavigator.sanitize(null))
        assertEquals(NavState(), SceneNavigator.sanitize(NavState(listOf(SceneId.BEDROOM))))
        val dup = NavState(listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.EXTERIOR, SceneId.VERANDA))
        assertEquals(listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA), SceneNavigator.sanitize(dup).stack)
        assertTrue(SceneNavigator.sanitize(dup).canGoBack)
    }
}
