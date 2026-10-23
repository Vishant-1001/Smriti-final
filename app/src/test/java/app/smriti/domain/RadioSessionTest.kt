package app.smriti.domain

import app.smriti.domain.RadioSession.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioSessionTest {
    private fun t(id: String) = RadioTrack(id, id, RadioTrack.Source.Asset("audio/$id.ogg"))
    private val list = listOf(t("a"), t("b"), t("c"))

    @Test
    fun `starts stopped with nothing playing`() {
        val s = RadioSession()
        assertEquals(PlaybackStatus.STOPPED, s.status)
        assertNull(s.selectedTrackId)
    }

    @Test
    fun `selecting a track plays it`() {
        val s = RadioSession().reduce(Event.Select("b"), list)
        assertEquals("b", s.selectedTrackId)
        assertEquals(PlaybackStatus.PLAYING, s.status)
    }

    @Test
    fun `play with nothing selected plays the first available track`() {
        val s = RadioSession().reduce(Event.Play, list)
        assertEquals("a", s.selectedTrackId)
        assertEquals(PlaybackStatus.PLAYING, s.status)
    }

    @Test
    fun `stop always stops immediately from any state`() {
        for (start in PlaybackStatus.entries) {
            val s = RadioSession(selectedTrackId = "a", status = start).reduce(Event.Stop, list)
            assertEquals(PlaybackStatus.STOPPED, s.status)
        }
    }

    @Test
    fun `pause and resume keep the same track`() {
        var s = RadioSession().reduce(Event.Select("b"), list)
        s = s.reduce(Event.Pause, list)
        assertEquals(PlaybackStatus.PAUSED, s.status)
        s = s.reduce(Event.Play, list)
        assertEquals(PlaybackStatus.PLAYING, s.status)
        assertEquals("b", s.selectedTrackId)
    }

    @Test
    fun `next wraps around while playing and only moves selection when stopped`() {
        var s = RadioSession().reduce(Event.Select("c"), list).reduce(Event.Next, list)
        assertEquals("a", s.selectedTrackId)
        assertEquals(PlaybackStatus.PLAYING, s.status)
        s = s.reduce(Event.Stop, list).reduce(Event.Next, list)
        assertEquals("b", s.selectedTrackId)
        assertEquals(PlaybackStatus.STOPPED, s.status)
    }

    @Test
    fun `not now on the current track stops and does not start another song`() {
        val s = RadioSession().reduce(Event.Select("a"), list).reduce(Event.SkipForSession("a"), list)
        assertEquals(PlaybackStatus.STOPPED, s.status)
        assertTrue("a" in s.skippedThisSession)
        assertEquals("b", s.selectedTrackId)
        assertEquals(listOf("b", "c"), s.available(list).map { it.id })
    }

    @Test
    fun `skipped tracks cannot be selected for the rest of the session`() {
        val s = RadioSession(skippedThisSession = setOf("b")).reduce(Event.Select("b"), list)
        assertNull(s.selectedTrackId)
        assertEquals(PlaybackStatus.STOPPED, s.status)
    }

    @Test
    fun `finishing plays through the list once and then rests`() {
        var s = RadioSession().reduce(Event.Select("b"), list).reduce(Event.TrackFinished, list)
        assertEquals("c", s.selectedTrackId)
        assertEquals(PlaybackStatus.PLAYING, s.status)
        s = s.reduce(Event.TrackFinished, list)
        assertEquals(PlaybackStatus.STOPPED, s.status)
    }

    @Test
    fun `playback failure stops and reports without crashing`() {
        val s = RadioSession().reduce(Event.Select("a"), list).reduce(Event.PlaybackFailed("a"), list)
        assertEquals(PlaybackStatus.STOPPED, s.status)
        assertEquals("a", s.unavailableTrackId)
        assertNull(s.reduce(Event.Select("b"), list).unavailableTrackId)
    }

    @Test
    fun `interruption pauses only when playing`() {
        val playing = RadioSession().reduce(Event.Select("a"), list).reduce(Event.Interrupted, list)
        assertEquals(PlaybackStatus.PAUSED, playing.status)
        val stopped = RadioSession().reduce(Event.Interrupted, list)
        assertEquals(PlaybackStatus.STOPPED, stopped.status)
    }

    @Test
    fun `empty playlist never plays`() {
        val s = RadioSession().reduce(Event.Play, emptyList())
        assertEquals(PlaybackStatus.STOPPED, s.status)
        assertNull(s.selectedTrackId)
    }

    @Test
    fun `reconcile drops a removed selected track`() {
        val s = RadioSession(selectedTrackId = "b", status = PlaybackStatus.STOPPED, skippedThisSession = setOf("c"))
            .reconcile(listOf(t("a"), t("c")))
        assertNull(s.selectedTrackId)
        assertEquals(setOf("c"), s.skippedThisSession)
    }
}
