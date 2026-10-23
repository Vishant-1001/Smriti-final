package app.smriti.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.smriti.audio.RadioPlayer
import app.smriti.audio.RadioPlayerListener
import app.smriti.audio.VoiceAvailability
import app.smriti.audio.VoiceOutput
import app.smriti.domain.AppLanguage
import app.smriti.domain.DefaultPlaylist
import app.smriti.domain.DialogueLine
import app.smriti.domain.GuideLines
import app.smriti.domain.MemoryBookState
import app.smriti.domain.NavState
import app.smriti.domain.PlaybackStatus
import app.smriti.domain.RadioSession
import app.smriti.domain.RadioTrack
import app.smriti.domain.SaveData
import app.smriti.domain.SceneId
import app.smriti.domain.SceneNavigator
import app.smriti.domain.Settings
import app.smriti.data.SaveRepository
import app.smriti.interactions.GameAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** Resolves a string resource in the player's chosen language (for speech). */
fun interface StringResolver {
    fun get(@StringRes id: Int, language: AppLanguage): String
}

/** Moves caregiver-chosen media into private storage. Implemented by MediaStorage in the app. */
interface MediaImporter {
    fun deletePhoto(fileName: String)
    fun deleteAudio(fileName: String)
}

enum class Overlay { GUIDE, SETTINGS, DESTINATION }

enum class VoiceStatus { SPEAKING, FINISHED, UNAVAILABLE, OFF }

data class ActiveDialogue(val line: DialogueLine, val voice: VoiceStatus)

data class GameUiState(
    val loaded: Boolean = false,
    val nav: NavState = NavState(),
    val settings: Settings = Settings(),
    val memoryBook: MemoryBookState = MemoryBookState(),
    val playlist: List<RadioTrack> = DefaultPlaylist.tracks,
    val radio: RadioSession = RadioSession(),
    val overlay: Overlay? = null,
    val dialogue: ActiveDialogue? = null,
    val showNamesHere: Boolean = false,
    val voiceSpeaking: Boolean = false,
    val voiceAvailability: Map<AppLanguage, VoiceAvailability> = emptyMap(),
) {
    val scene: SceneId get() = nav.current
    val showNames: Boolean get() = showNamesHere || settings.alwaysShowNames
    val currentVoiceAvailability: VoiceAvailability
        get() = voiceAvailability[settings.language] ?: VoiceAvailability.CHECKING
}

class GameViewModel(
    private val save: SaveRepository,
    private val voice: VoiceOutput,
    private val radioPlayer: RadioPlayer,
    private val strings: StringResolver,
    private val media: MediaImporter,
) : ViewModel(), RadioPlayerListener {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var lastLine: DialogueLine? = null

    init {
        radioPlayer.listener = this
        viewModelScope.launch {
            val saved = save.load()
            _state.update {
                it.copy(
                    loaded = true,
                    nav = saved.nav,
                    settings = saved.settings,
                    memoryBook = saved.memoryBook,
                    playlist = saved.playlist,
                    // After a restart the radio is always in a safe, stopped state.
                    radio = RadioSession(selectedTrackId = saved.selectedTrackId),
                )
            }
            radioPlayer.setVolume(saved.settings.effectiveMusicVolume)
        }
        viewModelScope.launch {
            voice.speaking.collect { speaking ->
                _state.update { s ->
                    val d = s.dialogue
                    s.copy(
                        voiceSpeaking = speaking,
                        dialogue = if (!speaking && d?.voice == VoiceStatus.SPEAKING) d.copy(voice = VoiceStatus.FINISHED) else d,
                    )
                }
            }
        }
        viewModelScope.launch {
            voice.availability.collect { a -> _state.update { it.copy(voiceAvailability = a) } }
        }
    }

    // ------------------------------------------------------------------ central dispatch

    fun dispatch(action: GameAction) {
        when (action) {
            GameAction.Start -> navigate(SceneNavigator.goTo(_state.value.nav, SceneId.EXTERIOR))
            is GameAction.GoTo -> navigate(SceneNavigator.goTo(_state.value.nav, action.scene))
            GameAction.Back -> back()
            GameAction.Home -> navigate(SceneNavigator.home(_state.value.nav))
            GameAction.EndSession -> {
                onRadio(RadioSession.Event.Stop)
                navigate(SceneNavigator.endSession())
            }
            GameAction.OpenDestinationChooser -> { stopVoice(); _state.update { it.copy(overlay = Overlay.DESTINATION, dialogue = null) } }
            GameAction.CloseDestinationChooser -> closeOverlay(Overlay.DESTINATION)
            is GameAction.NotYetAvailable -> say(
                when (action.place) {
                    GameAction.Place.KITCHEN -> GuideLines.kitchenNotReady
                    GameAction.Place.SEWING_CORNER -> GuideLines.sewingNotReady
                }
            )
            GameAction.OpenGuide -> _state.update { it.copy(overlay = Overlay.GUIDE) }
            GameAction.CloseGuide -> closeOverlay(Overlay.GUIDE)
            GameAction.GuideHelpHere -> {
                closeOverlay(Overlay.GUIDE)
                say(GuideLines.helpFor(_state.value.scene))
            }
            GameAction.GuideRepeat -> {
                closeOverlay(Overlay.GUIDE)
                say(lastLine ?: GuideLines.helpFor(_state.value.scene))
            }
            GameAction.ToggleShowNames -> _state.update { it.copy(showNamesHere = !it.showNamesHere, overlay = null) }
            GameAction.StopVoice -> stopVoice()
            GameAction.DismissDialogue -> { stopVoice(); _state.update { it.copy(dialogue = null) } }
            GameAction.OpenSettings -> { stopVoice(); _state.update { it.copy(overlay = Overlay.SETTINGS, dialogue = null) } }
            GameAction.CloseSettings -> closeOverlay(Overlay.SETTINGS)
        }
    }

    /** Hardware/gesture back: closes the top-most layer first, then moves back one scene. */
    fun back() {
        val s = _state.value
        when {
            s.overlay != null -> _state.update { it.copy(overlay = null) }
            s.dialogue != null -> dispatch(GameAction.DismissDialogue)
            else -> navigate(SceneNavigator.back(s.nav))
        }
    }

    private fun closeOverlay(which: Overlay) = _state.update { if (it.overlay == which) it.copy(overlay = null) else it }

    private fun navigate(next: NavState) {
        val prev = _state.value.nav
        if (next == prev) {
            _state.update { it.copy(overlay = null) }
            return
        }
        // Leaving a scene always silences its dialogue and clears temporary helpers.
        stopVoice()
        _state.update { it.copy(nav = next, overlay = null, dialogue = null, showNamesHere = false) }
        persist { it.copy(nav = next) }
    }

    // ------------------------------------------------------------------ guide / voice

    private fun say(line: DialogueLine) {
        lastLine = line
        val s = _state.value
        val status = when {
            !s.settings.narrationEnabled -> VoiceStatus.OFF
            s.currentVoiceAvailability != VoiceAvailability.AVAILABLE -> VoiceStatus.UNAVAILABLE
            else -> {
                val text = strings.get(line.text, s.settings.language)
                val ok = voice.speak(line.id, text, s.settings.language, s.settings.effectiveNarrationVolume)
                if (ok) VoiceStatus.SPEAKING else VoiceStatus.UNAVAILABLE
            }
        }
        _state.update { it.copy(dialogue = ActiveDialogue(line, status)) }
    }

    private fun stopVoice() {
        voice.stop()
        _state.update { s -> s.dialogue?.let { d -> if (d.voice == VoiceStatus.SPEAKING) s.copy(dialogue = d.copy(voice = VoiceStatus.FINISHED)) else s } ?: s }
    }

    // ------------------------------------------------------------------ settings

    fun updateSettings(transform: (Settings) -> Settings) {
        val next = transform(_state.value.settings)
        if (!next.narrationEnabled) stopVoice()
        _state.update { it.copy(settings = next) }
        radioPlayer.setVolume(next.effectiveMusicVolume)
        persist { it.copy(settings = next) }
    }

    // ------------------------------------------------------------------ memory book

    fun memoryNext() = updateMemoryBook { it.next() }
    fun memoryPrevious() = updateMemoryBook { it.previous() }

    fun readMemoryPageAloud(text: String) {
        val s = _state.value
        if (!s.settings.narrationEnabled || s.currentVoiceAvailability != VoiceAvailability.AVAILABLE) return
        voice.speak("memory_page", text, s.settings.language, s.settings.effectiveNarrationVolume)
    }

    fun addMemoryPage(caption: String, photoFile: String?) =
        updateMemoryBook { it.addPersonal("page_" + UUID.randomUUID(), caption, photoFile) }

    fun editMemoryCaption(id: String, caption: String) = updateMemoryBook { it.editCaption(id, caption) }

    fun deleteMemoryPage(id: String) {
        _state.value.memoryBook.pages.firstOrNull { it.id == id }?.photoFile?.let(media::deletePhoto)
        updateMemoryBook { it.remove(id) }
    }

    fun removeSamplePages() = updateMemoryBook { it.removeSamples() }
    fun restoreSamplePages() = updateMemoryBook { it.restoreSamples() }

    private fun updateMemoryBook(transform: (MemoryBookState) -> MemoryBookState) {
        val next = transform(_state.value.memoryBook)
        _state.update { it.copy(memoryBook = next) }
        persist { it.copy(memoryBook = next) }
    }

    // ------------------------------------------------------------------ radio

    fun onRadio(event: RadioSession.Event) {
        val before = _state.value.radio
        val playlist = _state.value.playlist
        val after = before.reduce(event, playlist)
        _state.update { it.copy(radio = after) }
        applyPlayback(before, after, playlist)
        if (before.selectedTrackId != after.selectedTrackId) persist { it.copy(selectedTrackId = after.selectedTrackId) }
    }

    private fun applyPlayback(before: RadioSession, after: RadioSession, playlist: List<RadioTrack>) {
        val track = playlist.firstOrNull { it.id == after.selectedTrackId }
        when (after.status) {
            PlaybackStatus.PLAYING -> when {
                track == null -> radioPlayer.stop()
                before.status == PlaybackStatus.PAUSED && before.selectedTrackId == after.selectedTrackId -> radioPlayer.resume()
                before.status != PlaybackStatus.PLAYING || before.selectedTrackId != after.selectedTrackId -> radioPlayer.play(track)
            }
            PlaybackStatus.PAUSED -> if (before.status == PlaybackStatus.PLAYING) radioPlayer.pause()
            PlaybackStatus.STOPPED -> if (before.status != PlaybackStatus.STOPPED) radioPlayer.stop()
        }
    }

    fun addTrack(fileName: String, title: String) {
        val track = RadioTrack("track_" + UUID.randomUUID(), title, RadioTrack.Source.LocalFile(fileName))
        updatePlaylist(_state.value.playlist + track)
    }

    fun removeTrack(id: String) {
        val track = _state.value.playlist.firstOrNull { it.id == id } ?: return
        if (_state.value.radio.selectedTrackId == id) onRadio(RadioSession.Event.Stop)
        (track.source as? RadioTrack.Source.LocalFile)?.let { media.deleteAudio(it.fileName) }
        updatePlaylist(_state.value.playlist - track)
    }

    fun restoreDemoTracks() {
        val missing = DefaultPlaylist.tracks.filter { d -> _state.value.playlist.none { it.id == d.id } }
        updatePlaylist(_state.value.playlist + missing)
    }

    private fun updatePlaylist(next: List<RadioTrack>) {
        val radio = _state.value.radio.reconcile(next)
        _state.update { it.copy(playlist = next, radio = radio) }
        persist { it.copy(playlist = next, selectedTrackId = radio.selectedTrackId) }
    }

    override fun onTrackCompleted(trackId: String) {
        if (_state.value.radio.selectedTrackId == trackId) onRadio(RadioSession.Event.TrackFinished)
    }

    override fun onTrackFailed(trackId: String) = onRadio(RadioSession.Event.PlaybackFailed(trackId))

    override fun onInterrupted() = onRadio(RadioSession.Event.Interrupted)

    // ------------------------------------------------------------------ lifecycle

    /** App moved to the background: never leave sound running with no visible controls. */
    fun onAppBackgrounded() {
        stopVoice()
        onRadio(RadioSession.Event.Interrupted)
    }

    private fun persist(transform: (SaveData) -> SaveData) {
        viewModelScope.launch { save.update(transform) }
    }

    override fun onCleared() {
        radioPlayer.listener = null
        radioPlayer.release()
        voice.shutdown()
    }
}
