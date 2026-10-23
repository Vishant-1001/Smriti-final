package app.smriti.ui

import app.smriti.data.InMemorySaveRepository
import app.smriti.domain.NavState
import app.smriti.domain.PlaybackStatus
import app.smriti.domain.RadioSession.Event
import app.smriti.domain.RadioTrack
import app.smriti.domain.SaveData
import app.smriti.domain.SceneId
import app.smriti.interactions.GameAction
import app.smriti.testing.FakeMedia
import app.smriti.testing.FakeRadioPlayer
import app.smriti.testing.FakeVoice
import app.smriti.testing.fakeStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private lateinit var save: InMemorySaveRepository
    private lateinit var voice: FakeVoice
    private lateinit var radio: FakeRadioPlayer
    private lateinit var media: FakeMedia

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        save = InMemorySaveRepository()
        voice = FakeVoice()
        radio = FakeRadioPlayer()
        media = FakeMedia()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(initial: SaveData = SaveData(), voiceAvailable: Boolean = true): GameViewModel {
        save = InMemorySaveRepository(initial)
        voice = FakeVoice(voiceAvailable)
        return GameViewModel(save, voice, radio, fakeStrings, media)
    }

    private fun GameViewModel.walkToBedroom() {
        dispatch(GameAction.Start)
        dispatch(GameAction.GoTo(SceneId.VERANDA))
        dispatch(GameAction.GoTo(SceneId.BEDROOM))
    }

    @Test
    fun `full slice navigation is persisted`() {
        val vm = vm()
        vm.walkToBedroom()
        assertEquals(SceneId.BEDROOM, vm.state.value.scene)
        assertEquals(SceneId.BEDROOM, save.current.nav.current)
        vm.dispatch(GameAction.Back)
        assertEquals(SceneId.VERANDA, save.current.nav.current)
    }

    @Test
    fun `saved scene is restored on a new view model`() {
        val saved = SaveData(nav = NavState(listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.BEDROOM)))
        val vm = vm(saved)
        assertTrue(vm.state.value.loaded)
        assertEquals(SceneId.BEDROOM, vm.state.value.scene)
    }

    @Test
    fun `radio never autoplays on entering the bedroom or the radio`() {
        val vm = vm()
        vm.walkToBedroom()
        vm.dispatch(GameAction.GoTo(SceneId.RADIO))
        assertEquals(PlaybackStatus.STOPPED, vm.state.value.radio.status)
        assertTrue(radio.log.none { it.startsWith("play") })
    }

    @Test
    fun `restored session is always stopped even if a track was selected`() {
        val vm = vm(SaveData(selectedTrackId = "demo_tune_1"))
        assertEquals("demo_tune_1", vm.state.value.radio.selectedTrackId)
        assertEquals(PlaybackStatus.STOPPED, vm.state.value.radio.status)
    }

    @Test
    fun `radio controls drive the player`() {
        val vm = vm()
        vm.onRadio(Event.Select("demo_tune_2"))
        vm.onRadio(Event.Pause)
        vm.onRadio(Event.Play)
        vm.onRadio(Event.Stop)
        assertEquals(listOf("play:demo_tune_2", "pause", "resume", "stop"), radio.log)
        assertEquals("demo_tune_2", save.current.selectedTrackId)
    }

    @Test
    fun `music and narration volumes are independent`() {
        val vm = vm()
        vm.updateSettings { it.copy(musicVolume = 0.2f, narrationVolume = 1f) }
        assertEquals(0.2f, radio.lastVolume, 0.001f)
        vm.updateSettings { it.copy(musicMuted = true) }
        assertEquals(0f, radio.lastVolume, 0.001f)
        assertEquals(1f, vm.state.value.settings.effectiveNarrationVolume, 0.001f)
    }

    @Test
    fun `missing track reports neutrally and stops`() {
        val vm = vm()
        vm.onRadio(Event.Select("demo_tune_1"))
        vm.onTrackFailed("demo_tune_1")
        assertEquals(PlaybackStatus.STOPPED, vm.state.value.radio.status)
        assertEquals("demo_tune_1", vm.state.value.radio.unavailableTrackId)
    }

    @Test
    fun `backgrounding pauses music and stops speech`() {
        val vm = vm()
        vm.onRadio(Event.Select("demo_tune_1"))
        vm.dispatch(GameAction.GuideHelpHere)
        vm.onAppBackgrounded()
        assertEquals(PlaybackStatus.PAUSED, vm.state.value.radio.status)
        assertTrue(voice.stopCount > 0)
        assertEquals(false, vm.state.value.voiceSpeaking)
    }

    @Test
    fun `help speaks only when asked and leaving the scene stops it`() {
        val vm = vm()
        vm.walkToBedroom()
        assertTrue("no unsolicited speech", voice.spoken.isEmpty())
        vm.dispatch(GameAction.GuideHelpHere)
        assertEquals("guide_bedroom", voice.spoken.single().first)
        assertEquals(VoiceStatus.SPEAKING, vm.state.value.dialogue?.voice)
        val stopsBefore = voice.stopCount
        vm.dispatch(GameAction.Back)
        assertTrue(voice.stopCount > stopsBefore)
        assertNull(vm.state.value.dialogue)
    }

    @Test
    fun `repeat replays the last line`() {
        val vm = vm()
        vm.walkToBedroom()
        vm.dispatch(GameAction.GuideHelpHere)
        voice.finish()
        assertEquals(VoiceStatus.FINISHED, vm.state.value.dialogue?.voice)
        vm.dispatch(GameAction.GuideRepeat)
        assertEquals(listOf("guide_bedroom", "guide_bedroom"), voice.spoken.map { it.first })
    }

    @Test
    fun `unavailable voice still shows the subtitle`() {
        val vm = vm(voiceAvailable = false)
        vm.dispatch(GameAction.GuideHelpHere)
        assertEquals(VoiceStatus.UNAVAILABLE, vm.state.value.dialogue?.voice)
    }

    @Test
    fun `turning narration off is respected`() {
        val vm = vm()
        vm.updateSettings { it.copy(narrationEnabled = false) }
        vm.dispatch(GameAction.GuideHelpHere)
        assertEquals(VoiceStatus.OFF, vm.state.value.dialogue?.voice)
        assertTrue(voice.spoken.isEmpty())
    }

    @Test
    fun `kitchen and sewing corner are real rooms reachable from the veranda`() {
        val vm = vm()
        vm.dispatch(GameAction.Start)
        vm.dispatch(GameAction.GoTo(SceneId.VERANDA))
        vm.dispatch(GameAction.GoTo(SceneId.KITCHEN))
        assertEquals(SceneId.KITCHEN, vm.state.value.scene)
        vm.dispatch(GameAction.Back)
        vm.dispatch(GameAction.GoTo(SceneId.SEWING))
        assertEquals(SceneId.SEWING, vm.state.value.scene)
    }

    @Test
    fun `front door chooser can go outside or stay home`() {
        val vm = vm()
        vm.dispatch(GameAction.Start)
        vm.dispatch(GameAction.GoTo(SceneId.VERANDA))
        vm.dispatch(GameAction.OpenDestinationChooser)
        assertEquals(Overlay.DESTINATION, vm.state.value.overlay)
        vm.dispatch(GameAction.CloseDestinationChooser)
        assertNull(vm.state.value.overlay)
        assertEquals(SceneId.VERANDA, vm.state.value.scene)
        vm.dispatch(GameAction.OpenDestinationChooser)
        vm.dispatch(GameAction.CloseDestinationChooser)
        vm.dispatch(GameAction.GoTo(SceneId.EXTERIOR))
        assertEquals(SceneId.EXTERIOR, vm.state.value.scene)
    }

    @Test
    fun `back closes overlays before leaving the scene`() {
        val vm = vm()
        vm.walkToBedroom()
        vm.dispatch(GameAction.OpenGuide)
        vm.back()
        assertNull(vm.state.value.overlay)
        assertEquals(SceneId.BEDROOM, vm.state.value.scene)
        vm.back()
        assertEquals(SceneId.VERANDA, vm.state.value.scene)
    }

    @Test
    fun `end session stops music and returns to title`() {
        val vm = vm()
        vm.walkToBedroom()
        vm.onRadio(Event.Select("demo_tune_1"))
        vm.dispatch(GameAction.EndSession)
        assertEquals(SceneId.TITLE, vm.state.value.scene)
        assertEquals(PlaybackStatus.STOPPED, vm.state.value.radio.status)
        assertEquals(listOf(SceneId.TITLE), save.current.nav.stack)
    }

    @Test
    fun `memory book changes are saved and deleting removes the photo`() {
        val vm = vm()
        vm.addMemoryPage("Our garden", "abc.img")
        val id = save.current.memoryBook.pages.first().id
        assertEquals("Our garden", save.current.memoryBook.pages.first().caption)
        vm.memoryNext()
        assertEquals(1, save.current.memoryBook.currentIndex)
        vm.deleteMemoryPage(id)
        assertTrue(save.current.memoryBook.pages.none { it.id == id })
        assertEquals(listOf("photo:abc.img"), media.deleted)
    }

    @Test
    fun `removing the playing track stops it and deletes its copy`() {
        val vm = vm()
        vm.addTrack("song.audio", "Song")
        val id = vm.state.value.playlist.last().id
        vm.onRadio(Event.Select(id))
        vm.removeTrack(id)
        assertEquals(PlaybackStatus.STOPPED, vm.state.value.radio.status)
        assertNull(vm.state.value.radio.selectedTrackId)
        assertTrue(save.current.playlist.none { it.id == id })
        assertEquals(listOf("audio:song.audio"), media.deleted)
    }

    @Test
    fun `demo tracks can be removed and restored`() {
        val vm = vm()
        vm.removeTrack("demo_tune_1")
        assertTrue(vm.state.value.playlist.none { it.id == "demo_tune_1" })
        vm.restoreDemoTracks()
        assertTrue(vm.state.value.playlist.any { it.id == "demo_tune_1" && it.source is RadioTrack.Source.Asset })
    }
}
