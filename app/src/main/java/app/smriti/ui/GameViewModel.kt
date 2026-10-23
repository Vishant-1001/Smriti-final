package app.smriti.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.smriti.audio.RadioPlayer
import app.smriti.audio.RadioPlayerListener
import app.smriti.audio.VoiceAvailability
import app.smriti.audio.VoiceOutput
import app.smriti.data.LayoutVariant
import app.smriti.data.SaveRepository
import app.smriti.data.SceneLayoutSource
import app.smriti.domain.AppLanguage
import app.smriti.domain.Catalog
import app.smriti.domain.CookTool
import app.smriti.domain.CookingState
import app.smriti.domain.Creation
import app.smriti.domain.DefaultPlaylist
import app.smriti.domain.DialogueLine
import app.smriti.domain.DishwashState
import app.smriti.domain.GuideLines
import app.smriti.domain.HintLevel
import app.smriti.domain.LineArg
import app.smriti.domain.MemoryBookState
import app.smriti.domain.NavState
import app.smriti.domain.NpcLines
import app.smriti.domain.PlaybackStatus
import app.smriti.domain.RadioSession
import app.smriti.domain.RadioTrack
import app.smriti.domain.SaveData
import app.smriti.domain.SceneId
import app.smriti.domain.SceneNavigator
import app.smriti.domain.Settings
import app.smriti.domain.SewKind
import app.smriti.domain.SewingState
import app.smriti.domain.ShoppingState
import app.smriti.domain.StallId
import app.smriti.domain.TripDirection
import app.smriti.domain.TripPhase
import app.smriti.domain.TripState
import app.smriti.domain.regionOf
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

enum class Overlay { GUIDE, SETTINGS, DESTINATION, SHOPPING, ITEM, CREATIONS }

enum class VoiceStatus { SPEAKING, FINISHED, UNAVAILABLE, OFF }

data class ActiveDialogue(val line: DialogueLine, val voice: VoiceStatus)

/** Short-lived visual helpers. Never saved, cleared when the scene changes. */
data class Helpers(
    /** Hotspot/item the Guide is pointing at (hint level 4, or "Show me" in activities). */
    val highlight: String? = null,
    val selectedItem: String? = null,
)

data class GameUiState(
    val loaded: Boolean = false,
    val nav: NavState = NavState(),
    val settings: Settings = Settings(),
    val memoryBook: MemoryBookState = MemoryBookState(),
    val playlist: List<RadioTrack> = DefaultPlaylist.tracks,
    val radio: RadioSession = RadioSession(),
    val shopping: ShoppingState = ShoppingState(),
    val cooking: CookingState = CookingState(),
    val dishes: DishwashState = DishwashState(),
    val sewing: SewingState = SewingState(),
    val creations: List<Creation> = emptyList(),
    val trip: TripState? = null,
    val overlay: Overlay? = null,
    val dialogue: ActiveDialogue? = null,
    val helpers: Helpers = Helpers(),
    val showNamesHere: Boolean = false,
    val voiceSpeaking: Boolean = false,
    val voiceAvailability: Map<AppLanguage, VoiceAvailability> = emptyMap(),
) {
    val scene: SceneId get() = nav.current
    val showNames: Boolean get() = showNamesHere || settings.alwaysShowNames
    val currentVoiceAvailability: VoiceAvailability
        get() = voiceAvailability[settings.language] ?: VoiceAvailability.CHECKING
    val currentStall: StallId? get() = StallId.entries.firstOrNull { it.scene == scene }
    val isHome: Boolean get() = scene.isInsideHouse
}

class GameViewModel(
    private val save: SaveRepository,
    private val voice: VoiceOutput,
    private val radioPlayer: RadioPlayer,
    private val strings: StringResolver,
    private val media: MediaImporter,
    private val layouts: SceneLayoutSource? = null,
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
                    shopping = saved.shopping,
                    cooking = saved.cooking.copy(notNow = null),
                    dishes = saved.dishes,
                    sewing = saved.sewing,
                    creations = saved.creations,
                    trip = saved.trip,
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
            GameAction.OpenDestinationChooser -> openOverlay(Overlay.DESTINATION)
            GameAction.CloseDestinationChooser -> closeOverlay(Overlay.DESTINATION)

            is GameAction.PlanTrip -> {
                // The home radio does not follow the person to the market.
                if (action.direction == TripDirection.TO_MARKET) onRadio(RadioSession.Event.Stop)
                updateTrip(TripState(direction = action.direction))
                navigate(SceneNavigator.goTo(_state.value.nav, SceneId.TRIP))
            }
            GameAction.TakeBus -> {
                val trip = _state.value.trip ?: TripState()
                updateTrip(trip)
                navigate(SceneNavigator.goTo(_state.value.nav, if (trip.direction == TripDirection.TO_MARKET) SceneId.BUS_STOP else SceneId.BUS))
            }
            GameAction.ArriveDirectly, GameAction.SkipRide -> arrive()
            is GameAction.ChooseBus -> {
                // Any bus goes to the market. The choice is not judged or recorded beyond this ride.
                updateTrip((_state.value.trip ?: TripState()).board(action.number))
                navigate(SceneNavigator.goTo(_state.value.nav, SceneId.BUS))
            }
            GameAction.TalkToConductor -> {
                val trip = _state.value.trip ?: TripState(phase = TripPhase.ON_BUS)
                updateTrip(trip.talkToConductor())
                say(if (trip.direction == TripDirection.TO_HOME) NpcLines.conductorTicketHome else NpcLines.conductorTicket)
            }
            GameAction.SayBusNumber -> say(GuideLines.busNumber)

            is GameAction.TalkToVendor -> say(NpcLines.vendorGreeting(action.stall))
            is GameAction.SelectItem -> {
                stopVoice()
                _state.update { it.copy(helpers = it.helpers.copy(selectedItem = action.itemId), overlay = Overlay.ITEM, dialogue = null) }
            }
            GameAction.CloseItem -> _state.update { it.copy(overlay = null, helpers = it.helpers.copy(selectedItem = null)) }
            GameAction.OpenShopping -> openOverlay(Overlay.SHOPPING)
            GameAction.CloseShopping -> closeOverlay(Overlay.SHOPPING)
            GameAction.OpenCreations -> openOverlay(Overlay.CREATIONS)
            GameAction.CloseCreations -> closeOverlay(Overlay.CREATIONS)

            GameAction.OpenGuide -> _state.update { it.copy(overlay = Overlay.GUIDE) }
            GameAction.CloseGuide -> closeOverlay(Overlay.GUIDE)
            GameAction.GuideHelpHere -> {
                closeOverlay(Overlay.GUIDE)
                say(helpLineHere())
            }
            GameAction.GuideRepeat -> {
                closeOverlay(Overlay.GUIDE)
                say(lastLine ?: helpLineHere())
            }
            GameAction.ToggleShowNames -> _state.update { it.copy(showNamesHere = !it.showNamesHere, overlay = null) }
            GameAction.StopVoice -> stopVoice()
            GameAction.DismissDialogue -> { stopVoice(); _state.update { it.copy(dialogue = null) } }
            GameAction.OpenSettings -> openOverlay(Overlay.SETTINGS)
            GameAction.CloseSettings -> closeOverlay(Overlay.SETTINGS)
        }
    }

    /** In the cooking activity, help reads the current step; elsewhere it explains the place. */
    private fun helpLineHere(): DialogueLine {
        val s = _state.value
        return if (s.scene == SceneId.COOKING) GuideLines.cookSteps[s.cooking.step.ordinal] else GuideLines.helpFor(s.scene)
    }

    /** Hardware/gesture back: closes the top-most layer first, then moves back one scene. */
    fun back() {
        val s = _state.value
        when {
            s.overlay != null -> _state.update { it.copy(overlay = null, helpers = it.helpers.copy(selectedItem = null)) }
            s.dialogue != null -> dispatch(GameAction.DismissDialogue)
            else -> navigate(SceneNavigator.back(s.nav))
        }
    }

    private fun openOverlay(which: Overlay) {
        stopVoice()
        _state.update { it.copy(overlay = which, dialogue = null) }
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
        val keepTrip = next.current == SceneId.TRIP || next.current == SceneId.BUS_STOP || next.current == SceneId.BUS
        _state.update {
            it.copy(nav = next, overlay = null, dialogue = null, showNamesHere = false, helpers = Helpers(), trip = if (keepTrip) it.trip else null)
        }
        persist { it.copy(nav = next, trip = if (keepTrip) it.trip else null) }
    }

    /** Ends the current journey: at the market, or back on the veranda. */
    private fun arrive() {
        val trip = _state.value.trip
        val target = if (trip?.direction == TripDirection.TO_HOME) SceneId.VERANDA else SceneId.MARKET
        navigate(SceneNavigator.arriveAt(target))
    }

    fun arriveFromBus() = arrive()

    // ------------------------------------------------------------------ guide / voice

    fun resolve(line: DialogueLine, language: AppLanguage): String {
        val args = line.args.map { a ->
            when (a) {
                is LineArg.Res -> strings.get(a.id, language)
                is LineArg.Num -> a.value.toString()
            }
        }
        val raw = strings.get(line.text, language)
        return if (args.isEmpty()) raw else runCatching { String.format(raw, *args.toTypedArray()) }.getOrDefault(raw)
    }

    private fun say(line: DialogueLine) {
        lastLine = line
        val s = _state.value
        val status = when {
            !s.settings.narrationEnabled -> VoiceStatus.OFF
            s.currentVoiceAvailability != VoiceAvailability.AVAILABLE -> VoiceStatus.UNAVAILABLE
            else -> {
                val ok = voice.speak(line.id, resolve(line, s.settings.language), s.settings.language, s.settings.effectiveNarrationVolume)
                if (ok) VoiceStatus.SPEAKING else VoiceStatus.UNAVAILABLE
            }
        }
        _state.update { it.copy(dialogue = ActiveDialogue(line, status), overlay = if (it.overlay == Overlay.GUIDE) null else it.overlay) }
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

    // ------------------------------------------------------------------ shopping

    fun addToBag(itemId: String) {
        updateShopping { it.addToBag(itemId) }
        _state.update { it.copy(helpers = it.helpers.copy(highlight = if (it.helpers.highlight == itemId) null else it.helpers.highlight)) }
    }

    fun putBack(itemId: String) = updateShopping { it.putBack(itemId) }
    fun unpack() = updateShopping { it.unpack() }

    /**
     * Any hint level can be chosen at any time. The target is the next list entry this stall sells.
     * [variant] is the layout on screen, so "left/middle/right" matches what the person sees.
     */
    fun hint(level: HintLevel, variant: LayoutVariant) {
        val s = _state.value
        if (level == HintLevel.SHOW_LIST) {
            openOverlay(Overlay.SHOPPING)
            return
        }
        val stall = s.currentStall ?: run { say(GuideLines.market); return }
        val wanted = s.shopping.nextWanted(stall) ?: run { say(GuideLines.nothingHere); return }
        val itemId = if (wanted == ShoppingState.ANY_CLOTH) Catalog.forStall(StallId.CLOTH).first().id else wanted
        val item = Catalog.item(itemId) ?: return
        _state.update { it.copy(overlay = null) }
        when (level) {
            HintLevel.SAY_NAME -> say(if (wanted == ShoppingState.ANY_CLOTH) GuideLines.anyCloth else GuideLines.lookFor(item))
            HintLevel.WHERE_TO_LOOK -> {
                val rect = layouts?.layout(s.scene, variant)?.hotspots?.firstOrNull { it.id == itemId }?.rect
                if (rect == null || wanted == ShoppingState.ANY_CLOTH) say(GuideLines.anyCloth)
                else say(GuideLines.whereToLook(item, regionOf(rect.centerX)))
            }
            HintLevel.HIGHLIGHT -> _state.update { it.copy(helpers = it.helpers.copy(highlight = itemId)) }
            HintLevel.PLACE_FOR_ME -> {
                addToBag(itemId)
                say(GuideLines.placed(item))
            }
            HintLevel.SHOW_LIST -> Unit
        }
    }

    private fun updateShopping(transform: (ShoppingState) -> ShoppingState) {
        val next = transform(_state.value.shopping)
        _state.update { it.copy(shopping = next) }
        persist { it.copy(shopping = next) }
    }

    // ------------------------------------------------------------------ trip and payment

    fun addMoney(value: Int) = updateTrip(_state.value.trip?.addMoney(value))
    fun clearTray() = updateTrip(_state.value.trip?.clearTray())
    fun pay() {
        val before = _state.value.trip ?: return
        val after = before.pay()
        updateTrip(after)
        if (after.phase == TripPhase.PAID && before.phase != TripPhase.PAID) say(NpcLines.conductorThanks)
    }

    fun payForMe() {
        val before = _state.value.trip ?: TripState(phase = TripPhase.ON_BUS)
        updateTrip(before.payForMe())
        say(NpcLines.conductorThanks)
    }

    fun showMe(target: String) = _state.update { it.copy(helpers = it.helpers.copy(highlight = target)) }

    private fun updateTrip(next: TripState?) {
        if (next == null) return
        _state.update { it.copy(trip = next) }
        persist { it.copy(trip = next) }
    }

    // ------------------------------------------------------------------ kitchen

    fun cook(tool: CookTool) = updateCooking { it.tap(tool) }.also { clearHighlight() }
    fun cookDoStep() = updateCooking { it.doStep() }.also { clearHighlight() }
    fun cookUndo() = updateCooking { it.undo() }
    fun cookReset() = updateCooking { it.reset() }
    fun cookRepeat() = say(GuideLines.cookSteps[_state.value.cooking.step.ordinal])
    fun cookShowMe() = _state.value.cooking.step.tool?.let { showMe(it.name) }

    private fun updateCooking(transform: (CookingState) -> CookingState) {
        val next = transform(_state.value.cooking)
        _state.update { it.copy(cooking = next) }
        persist { it.copy(cooking = next.copy(notNow = null)) }
    }

    fun dishTap(id: String) = updateDishes { it.tap(id) }
    fun dishesDoAll() = updateDishes { it.doAll() }
    fun dishesReset() = updateDishes { it.reset() }
    fun dishesGuided(on: Boolean) = updateDishes { it.withGuided(on) }

    private fun updateDishes(transform: (DishwashState) -> DishwashState) {
        val next = transform(_state.value.dishes)
        _state.update { it.copy(dishes = next) }
        persist { it.copy(dishes = next) }
    }

    // ------------------------------------------------------------------ sewing

    fun chooseFabric(id: String) = updateSewing { it.chooseFabric(id) }
    fun chooseKind(kind: SewKind) = updateSewing { it.chooseKind(kind) }
    fun stitch() = updateSewing { it.stitch() }.also { clearHighlight() }
    fun sewUndo() = updateSewing { it.undo() }
    fun sewStartOver() = updateSewing { it.startOver() }
    fun sewDoItForMe() = updateSewing { it.doItForMe(_state.value.shopping.fabrics.last()) }.also { clearHighlight() }

    /** Keeps the finished piece in the creations rack and gets ready for the next one. */
    fun sewKeep() {
        val creation = _state.value.sewing.toCreation("creation_" + UUID.randomUUID()) ?: return
        val creations = _state.value.creations + creation
        _state.update { it.copy(creations = creations, sewing = SewingState()) }
        persist { it.copy(creations = creations, sewing = SewingState()) }
    }

    private fun updateSewing(transform: (SewingState) -> SewingState) {
        val next = transform(_state.value.sewing)
        _state.update { it.copy(sewing = next) }
        persist { it.copy(sewing = next) }
    }

    private fun clearHighlight() = _state.update { it.copy(helpers = it.helpers.copy(highlight = null)) }

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
