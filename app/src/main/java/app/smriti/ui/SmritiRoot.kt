package app.smriti.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.smriti.R
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import app.smriti.data.LayoutVariant
import app.smriti.domain.PxRect
import app.smriti.domain.TripDirection
import app.smriti.domain.TripPhase
import app.smriti.domain.TripState
import app.smriti.ui.components.BigButton
import app.smriti.ui.components.Sprite
import app.smriti.ui.overlays.Scrim
import app.smriti.ui.overlays.blockTaps
import app.smriti.ui.screens.BusCue
import app.smriti.ui.screens.BusPanel
import app.smriti.ui.screens.CookingScreen
import app.smriti.ui.screens.CreationsGallery
import app.smriti.ui.screens.DishesScreen
import app.smriti.ui.screens.ItemCard
import app.smriti.ui.screens.ShoppingPanel
import app.smriti.ui.screens.StitchScreen
import app.smriti.ui.screens.TripScreen
import kotlin.math.roundToInt
import app.smriti.data.MediaStorage
import app.smriti.data.SceneLayoutSource
import app.smriti.domain.PlaybackStatus
import app.smriti.domain.RadioSession
import app.smriti.domain.SceneId
import app.smriti.domain.SceneNavigator
import app.smriti.interactions.GameAction
import app.smriti.LocalizedActivityContext
import app.smriti.ui.components.GuidePortrait
import app.smriti.ui.components.HudButton
import app.smriti.ui.components.SmritiIcons
import app.smriti.ui.overlays.DestinationChooser
import app.smriti.ui.overlays.DialoguePanel
import app.smriti.ui.overlays.GuidePanel
import app.smriti.ui.overlays.SettingsPanel
import app.smriti.ui.scene.IllustratedScene
import app.smriti.ui.screens.CaregiverActions
import app.smriti.ui.screens.CaregiverScreen
import app.smriti.ui.screens.MemoryBookScreen
import app.smriti.ui.screens.RadioScreen
import app.smriti.ui.screens.TitleScreen
import app.smriti.ui.screens.trackTitle
import app.smriti.ui.theme.SmritiColors
import app.smriti.ui.theme.SmritiTheme
import app.smriti.ui.theme.LocalSystemFontScale
import app.smriti.ui.fit.FitFlags
import app.smriti.ui.fit.rememberRadioFit
import app.smriti.ui.screens.RadioLayout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density

@Composable
fun SmritiRoot(vm: GameViewModel, layouts: SceneLayoutSource, media: MediaStorage) {
    val state by vm.state.collectAsStateWithLifecycle()
    if (!state.loaded) {
        Box(Modifier.fillMaxSize().background(SmritiColors.Cream))
        return
    }
    // Resources follow the in-app language choice, independent of the device language.
    val base = LocalContext.current
    val localized = remember(state.settings.language, base) { LocalizedActivityContext(base, state.settings.language) }
    CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides localized.resources.configuration) {
        SmritiTheme(state.settings) {
            BackHandler(enabled = state.nav.canGoBack || state.overlay != null || state.dialogue != null) { vm.back() }
            val caregiverActions = remember(vm, media) { caregiverActions(vm, media) }
            GameScreen(state, vm::dispatch, layouts, media, vm, caregiverActions)
        }
    }
}

private fun caregiverActions(vm: GameViewModel, media: MediaStorage) = object : CaregiverActions {
    override fun importPhoto(uri: Uri) = media.importPhoto(uri)
    override fun importAudio(uri: Uri) = media.importAudio(uri)
    override fun addPage(caption: String, photoFile: String?) = vm.addMemoryPage(caption, photoFile)
    override fun editCaption(id: String, caption: String) = vm.editMemoryCaption(id, caption)
    override fun deletePage(id: String) = vm.deleteMemoryPage(id)
    override fun removeSamples() = vm.removeSamplePages()
    override fun restoreSamples() = vm.restoreSamplePages()
    override fun addTrack(fileName: String, title: String) = vm.addTrack(fileName, title)
    override fun removeTrack(id: String) = vm.removeTrack(id)
    override fun restoreDemoTracks() = vm.restoreDemoTracks()
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun GameScreen(
    state: GameUiState,
    onAction: (GameAction) -> Unit,
    layouts: SceneLayoutSource,
    media: MediaStorage,
    vm: GameViewModel,
    caregiverActions: CaregiverActions,
) {
    val reduced = state.settings.reducedMotion
    // testTagsAsResourceId lets UI Automator (and instrumented tests) find controls by stable tag.
    BoxWithConstraints(Modifier.fillMaxSize().background(SmritiColors.Cream).semantics { testTagsAsResourceId = true }) {
        val narrow = maxWidth < 600.dp
        // SMRITI Fit (radio only, behind FitFlags): layout chosen from explicit settings and window size.
        val radioFit = if (FitFlags.radioEnabled && state.scene == SceneId.RADIO) {
            rememberRadioFit(state.settings, maxWidth, maxHeight, state.playlist.firstOrNull { it.id == state.radio.selectedTrackId }?.let { trackTitle(it) })
        } else null
        var fitHudHeight by remember { mutableStateOf<androidx.compose.ui.unit.Dp?>(null) }
        // If Fit chose a smaller text level than the setting (only when the asked size cannot fit),
        // the radio screen and its top bar are drawn at that level.
        val fitDensity = radioFit?.config?.let { c ->
            val d = LocalDensity.current
            Density(d.density, LocalSystemFontScale.current * c.textLevel.scale.toFloat())
        }
        Crossfade(targetState = state.scene, animationSpec = if (reduced) snap() else tween(350), label = "scene") { scene ->
            when {
                scene == SceneId.TITLE -> TitleScreen(onAction)
                scene.isIllustratedScene -> IllustratedScene(
                    scene, layouts, state.showNames, onAction, Modifier.testTag("scene_${scene.name.lowercase()}"),
                    highlight = state.helpers.highlight,
                    // In tall layouts the bus cue sits above the buses, so keep room for it.
                    extraTop = if (scene == SceneId.BUS_STOP && maxHeight > maxWidth) 150.dp else 0.dp,
                    decorations = { rects -> SceneDecorations(scene, state, rects) },
                ) { guideOnRight ->
                    SceneCorners(state, onAction, vm, guideOnRight)
                }
                else -> Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    val reserved = if (radioFit != null && scene == SceneId.RADIO) {
                        fitHudHeight ?: if (narrow) 168.dp else 90.dp
                    } else if (narrow) 168.dp else 90.dp
                    Spacer(Modifier.padding(top = reserved))
                    Box(Modifier.weight(1f).padding(bottom = if (state.radio.status != PlaybackStatus.STOPPED && scene != SceneId.RADIO) 86.dp else 12.dp)) {
                        when (scene) {
                            SceneId.MEMORY_BOOK -> MemoryBookScreen(
                                state.memoryBook, media::photoFile, vm::memoryPrevious, vm::memoryNext, vm::readMemoryPageAloud,
                                Modifier.testTag("scene_memory_book"),
                            )
                            SceneId.RADIO -> WithDensity(fitDensity) {
                                RadioScreen(
                                    state.playlist, state.radio, state.settings.musicVolume, state.settings.musicMuted, vm::onRadio,
                                    { v -> vm.updateSettings { it.copy(musicVolume = v) } },
                                    Modifier.testTag("scene_radio"),
                                    fit = radioFit?.config?.let {
                                        RadioLayout(it.columns, it.style == app.smriti.fit.ButtonStyle.STACKED, it.illustration, it.landscapeSplit)
                                    },
                                )
                            }
                            SceneId.CAREGIVER -> CaregiverScreen(state.memoryBook, state.playlist, caregiverActions, Modifier.testTag("scene_caregiver"))
                            SceneId.COOKING -> CookingScreen(
                                state.cooking, state.helpers.highlight, vm::cook, vm::cookRepeat, vm::cookShowMe, vm::cookDoStep, vm::cookUndo, vm::cookReset,
                                Modifier.testTag("scene_cooking"),
                            )
                            SceneId.DISHES -> DishesScreen(state.dishes, vm::dishTap, vm::dishesGuided, vm::dishesDoAll, vm::dishesReset, Modifier.testTag("scene_dishes"))
                            SceneId.STITCH -> StitchScreen(
                                state.sewing, state.shopping.fabrics, state.helpers.highlight,
                                vm::chooseFabric, vm::chooseKind, vm::stitch, { vm.showMe("stitch") }, vm::sewDoItForMe, vm::sewUndo, vm::sewStartOver, vm::sewKeep,
                                Modifier.testTag("scene_stitch"),
                            )
                            SceneId.TRIP -> TripScreen(
                                state.trip?.direction ?: TripDirection.TO_MARKET, state.shopping,
                                { onAction(GameAction.TakeBus) }, { onAction(GameAction.ArriveDirectly) },
                                Modifier.testTag("scene_trip"),
                            )
                            else -> Unit
                        }
                    }
                }
            }
        }

        if (state.scene != SceneId.TITLE) {
            WithDensity(fitDensity) {
                TopBar(state, onAction, guideInBar = !state.scene.isIllustratedScene, fit = radioFit?.config, onHeight = { fitHudHeight = it })
            }
        }
        if (state.radio.status != PlaybackStatus.STOPPED && !state.scene.isIllustratedScene && state.scene != SceneId.RADIO) {
            MiniPlayer(state, vm, Modifier.align(Alignment.BottomEnd).safeDrawingPadding().padding(16.dp))
        }

        // Travel controls drawn over the illustrated bus stop and bus.
        val hudTop = if (narrow) 152.dp else 96.dp
        if (state.scene == SceneId.BUS_STOP && state.overlay == null) {
            BusCue(
                { onAction(GameAction.SayBusNumber) },
                Modifier.align(if (maxWidth > maxHeight) Alignment.TopStart else Alignment.TopCenter).safeDrawingPadding().padding(start = 16.dp, end = 16.dp, top = hudTop),
            )
        }
        if (state.scene == SceneId.BUS && state.overlay == null) {
            val trip = state.trip ?: TripState(phase = TripPhase.ON_BUS)
            val beforeTicket = trip.phase == TripPhase.ON_BUS || trip.phase == TripPhase.AT_STOP
            BusPanel(
                trip, state.helpers.highlight,
                onTalk = { onAction(GameAction.TalkToConductor) }, onSkip = { onAction(GameAction.SkipRide) },
                onMoney = vm::addMoney, onClear = vm::clearTray, onPay = vm::pay, onShowMe = { vm.showMe("money_${TripState.FARE}") },
                onPayForMe = vm::payForMe, onArrive = vm::arriveFromBus,
                modifier = Modifier
                    .then(if (beforeTicket) Modifier.widthIn(max = 300.dp) else Modifier)
                    .align(if (narrow) Alignment.BottomCenter else Alignment.BottomEnd)
                    .safeDrawingPadding()
                    .padding(start = 16.dp, end = 16.dp, top = hudTop, bottom = if (narrow) 150.dp else 16.dp),
            )
        }

        state.dialogue?.let { d ->
            val m = when {
                // On the bus the ticket stays visible at the bottom, so the conductor speaks from the top.
                state.scene == SceneId.BUS -> Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(start = 16.dp, end = 16.dp, top = if (narrow) 152.dp else 96.dp)
                !state.scene.isIllustratedScene -> Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(16.dp)
                narrow -> Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(start = 16.dp, end = 16.dp, bottom = 120.dp)
                else -> Modifier.align(Alignment.BottomStart).safeDrawingPadding().padding(start = 132.dp, end = 16.dp, bottom = 16.dp)
            }
            DialoguePanel(d, state.voiceSpeaking, onAction, m)
        }

        when (state.overlay) {
            Overlay.GUIDE -> GuidePanel(state.scene, state.showNames, onAction)
            Overlay.SHOPPING -> Scrim({ onAction(GameAction.CloseShopping) }) {
                ShoppingPanel(
                    state.shopping, inStall = state.currentStall != null, atHome = state.isHome,
                    onHint = { level -> vm.hint(level, if (maxWidth > maxHeight) LayoutVariant.LANDSCAPE else LayoutVariant.PORTRAIT) },
                    onPutBack = vm::putBack, onUnpack = vm::unpack, onClose = { onAction(GameAction.CloseShopping) },
                    modifier = Modifier.blockTaps(),
                )
            }
            Overlay.ITEM -> state.helpers.selectedItem?.let { id ->
                Scrim({ onAction(GameAction.CloseItem) }) {
                    ItemCard(
                        id, state.shopping,
                        onAdd = { vm.addToBag(id); onAction(GameAction.CloseItem) },
                        onPutBack = { vm.putBack(id); onAction(GameAction.CloseItem) },
                        onClose = { onAction(GameAction.CloseItem) },
                        modifier = Modifier.blockTaps(),
                    )
                }
            }
            Overlay.CREATIONS -> Scrim({ onAction(GameAction.CloseCreations) }) {
                CreationsGallery(state.creations, Modifier.blockTaps())
            }
            Overlay.DESTINATION -> DestinationChooser(onAction)
            Overlay.SETTINGS -> SettingsPanel(
                state.settings, state.currentVoiceAvailability, vm::updateSettings,
                onOpenCaregiver = { onAction(GameAction.CloseSettings); onAction(GameAction.GoTo(SceneId.CAREGIVER)) },
                onClose = { onAction(GameAction.CloseSettings) },
            )
            null -> Unit
        }
    }
}

@Composable
private fun sceneName(scene: SceneId): String = stringResource(
    when (scene) {
        SceneId.TITLE -> R.string.scene_title
        SceneId.EXTERIOR -> R.string.scene_exterior
        SceneId.VERANDA -> R.string.scene_veranda
        SceneId.BEDROOM -> R.string.scene_bedroom
        SceneId.MEMORY_BOOK -> R.string.scene_memory_book
        SceneId.RADIO -> R.string.scene_radio
        SceneId.CAREGIVER -> R.string.scene_caregiver
        SceneId.KITCHEN -> R.string.scene_kitchen
        SceneId.COOKING -> R.string.scene_cooking
        SceneId.DISHES -> R.string.scene_dishes
        SceneId.SEWING -> R.string.scene_sewing
        SceneId.STITCH -> R.string.scene_stitch
        SceneId.TRIP -> R.string.scene_trip
        SceneId.BUS_STOP -> R.string.scene_bus_stop
        SceneId.BUS -> R.string.scene_bus
        SceneId.MARKET -> R.string.scene_market
        SceneId.STALL_PRODUCE -> R.string.stall_produce
        SceneId.STALL_SPICE -> R.string.stall_spice
        SceneId.STALL_FLOWER -> R.string.stall_flower
        SceneId.STALL_CLOTH -> R.string.stall_cloth
    }
)

/**
 * Back / Home on the left, where-am-I in the middle, End / Settings on the right. Never covered by
 * activities. On narrow screens the scene name moves to its own row so no control is squeezed.
 */
/** Draws [content] with [density] when given (Fit's text level), otherwise unchanged. */
@Composable
private fun WithDensity(density: Density?, content: @Composable () -> Unit) {
    if (density == null) content() else CompositionLocalProvider(LocalDensity provides density, content = content)
}

@Composable
private fun TopBar(
    state: GameUiState,
    onAction: (GameAction) -> Unit,
    guideInBar: Boolean,
    fit: app.smriti.fit.Config? = null,
    onHeight: (androidx.compose.ui.unit.Dp) -> Unit = {},
) {
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth().safeDrawingPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
        // With SMRITI Fit the number of rows, the label size and the button width cap are chosen
        // by the solver; otherwise the fixed 600 dp threshold applies.
        val narrow = fit?.let { it.hudRows >= 2 } ?: (maxWidth < 600.dp)
        val ownRowForName = fit?.hudRows == 3
        val follow = fit?.hudFollowsTextSize ?: false
        val cap = if (fit?.hudWideButtons == true) 220.dp else 150.dp
        val capWide = if (fit?.hudWideButtons == true) 220.dp else 170.dp
        val name: @Composable (Modifier) -> Unit = { m ->
            Box(m, contentAlignment = Alignment.Center) {
                Text(
                    sceneName(state.scene),
                    modifier = Modifier
                        .semantics { heading() }
                        .background(SmritiColors.Paper.copy(alpha = 0.92f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("scene_name"),
                    style = MaterialTheme.typography.titleMedium,
                    color = SmritiColors.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
        // Hints are one tap away in every stall; they open the list with the hint choices.
        val hintButton: @Composable () -> Unit = {
            HudButton(stringResource(R.string.hud_hint), SmritiIcons.Help, { onAction(GameAction.OpenShopping) }, Modifier.widthIn(max = cap).testTag("hud_hint"), follow)
        }
        val endButton: @Composable () -> Unit = {
            HudButton(stringResource(R.string.action_end_session), SmritiIcons.Exit, { onAction(GameAction.EndSession) }, Modifier.widthIn(max = capWide).testTag("hud_end"), follow)
        }
        val guide: @Composable () -> Unit = {
            HudButton(stringResource(R.string.action_guide), SmritiIcons.Help, { onAction(GameAction.OpenGuide) }, Modifier.widthIn(max = capWide).testTag("hud_guide"), follow)
        }
        // Context buttons for this place. On narrow screens they share the second row with the
        // place name, so the first row (Back, Home, Settings) can never be pushed off screen.
        val showList = state.scene.isMarket || state.scene == SceneId.TRIP || (state.scene == SceneId.VERANDA && state.shopping.bag.isNotEmpty())
        val extras: @Composable () -> Unit = {
            if (guideInBar) guide()
            if (state.scene == SceneId.EXTERIOR) endButton()
            if (state.currentStall != null) hintButton()
            if (state.scene == SceneId.MARKET) {
                HudButton(
                    stringResource(R.string.market_go_home), SmritiIcons.Home,
                    { onAction(GameAction.PlanTrip(TripDirection.TO_HOME)) }, Modifier.widthIn(max = cap).testTag("market_go_home"), follow,
                )
            }
            if (showList) {
                HudButton(
                    stringResource(R.string.hud_list) + " ${state.shopping.collectedCount}/${state.shopping.list.size}",
                    SmritiIcons.List, { onAction(GameAction.OpenShopping) }, Modifier.widthIn(max = cap).testTag("hud_list"), follow,
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            // Fit reserves exactly the space the top bar takes (plus its 8 dp padding above and below).
            modifier = Modifier.onSizeChanged { with(density) { onHeight(it.height.toDp() + 16.dp) } },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudButton(stringResource(R.string.action_back), SmritiIcons.Back, { onAction(GameAction.Back) }, Modifier.widthIn(max = cap).testTag("hud_back"), follow)
                val home = SceneNavigator.homeFor(state.nav)
                if (state.scene != home && state.scene.isInsideHouse) {
                    HudButton(stringResource(R.string.action_home), SmritiIcons.Home, { onAction(GameAction.Home) }, Modifier.widthIn(max = cap).testTag("hud_home"), follow)
                }
                if (narrow) Spacer(Modifier.weight(1f)) else name(Modifier.weight(1f))
                if (!narrow) extras()
                HudButton(stringResource(R.string.action_settings), SmritiIcons.Settings, { onAction(GameAction.OpenSettings) }, Modifier.widthIn(max = capWide).testTag("hud_settings"), follow)
            }
            if (narrow && ownRowForName) {
                Row(verticalAlignment = Alignment.CenterVertically) { name(Modifier.weight(1f)) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { extras() }
            } else if (narrow) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    name(Modifier.weight(1f))
                    extras()
                }
            }
        }
    }
}

/** Guide button and music mini-player on picture scenes, in whichever bottom corners are free. */
@Composable
private fun BoxScope.SceneCorners(state: GameUiState, onAction: (GameAction) -> Unit, vm: GameViewModel, guideOnRight: Boolean) {
    GuideButton(
        state.voiceSpeaking, onAction,
        Modifier.align(if (guideOnRight) Alignment.BottomEnd else Alignment.BottomStart).safeDrawingPadding().padding(16.dp),
    )

    if (state.radio.status != PlaybackStatus.STOPPED) {
        val m = Modifier.safeDrawingPadding()
        // Narrow phones have no room beside the Guide, so the player sits above it instead.
        val narrow = LocalConfiguration.current.screenWidthDp < 420
        MiniPlayer(
            state, vm,
            when {
                narrow -> m.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 150.dp)
                guideOnRight -> m.align(Alignment.BottomStart).padding(start = 16.dp, end = 136.dp, bottom = 16.dp)
                else -> m.align(Alignment.BottomEnd).padding(start = 136.dp, end = 16.dp, bottom = 16.dp)
            },
        )
    }
}

@Composable
private fun GuideButton(speaking: Boolean, onAction: (GameAction) -> Unit, modifier: Modifier) {
    val label = stringResource(R.string.action_guide)
    Surface(
        onClick = { onAction(GameAction.OpenGuide) },
        modifier = modifier.semantics { contentDescription = label }.testTag("hud_guide"),
        shape = RoundedCornerShape(28.dp),
        color = SmritiColors.Paper.copy(alpha = 0.95f),
        border = BorderStroke(2.dp, SmritiColors.Outline),
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GuidePortrait(speaking = speaking, size = 72.dp)
            Text(label, style = MaterialTheme.typography.labelMedium, color = SmritiColors.Ink)
        }
    }
}

/** While music plays anywhere outside the radio screen, Pause/Play and Stop stay one tap away. */
@Composable
private fun MiniPlayer(state: GameUiState, vm: GameViewModel, modifier: Modifier) {
    val track = state.playlist.firstOrNull { it.id == state.radio.selectedTrackId }
    Surface(
        modifier = modifier.widthIn(max = 420.dp).testTag("mini_player"),
        shape = RoundedCornerShape(24.dp),
        color = SmritiColors.Paper,
        border = BorderStroke(2.dp, SmritiColors.Outline),
        shadowElevation = 6.dp,
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(SmritiIcons.Music, contentDescription = null, tint = SmritiColors.Ink, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                track?.let { trackTitle(it) } ?: stringResource(R.string.mini_player_music),
                style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(4.dp))
            val playing = state.radio.status == PlaybackStatus.PLAYING
            val playPauseLabel = stringResource(if (playing) R.string.radio_pause else R.string.radio_play)
            IconButton(
                onClick = { vm.onRadio(if (playing) RadioSession.Event.Pause else RadioSession.Event.Play) },
                modifier = Modifier.size(56.dp).semantics { contentDescription = playPauseLabel },
            ) { Icon(if (playing) SmritiIcons.Pause else SmritiIcons.Play, contentDescription = null, tint = SmritiColors.Leaf) }
            val stopLabel = stringResource(R.string.radio_stop)
            IconButton(
                onClick = { vm.onRadio(RadioSession.Event.Stop) },
                modifier = Modifier.size(56.dp).semantics { contentDescription = stopLabel }.testTag("mini_stop"),
            ) { Icon(SmritiIcons.Stop, contentDescription = null, tint = SmritiColors.Terracotta) }
        }
    }
}

/** Things drawn by the app on top of a scene, anchored to hotspots: made items hang on the sewing rack. */
@Composable
private fun BoxScope.SceneDecorations(scene: SceneId, state: GameUiState, rects: Map<String, PxRect>) {
    if (scene != SceneId.SEWING) return
    val rack = rects["creations_rack"] ?: return
    val density = LocalDensity.current
    state.creations.takeLast(2).forEachIndexed { i, c ->
        val w = rack.width / 2f
        Sprite(
            c.sprite,
            Modifier
                .offset { IntOffset((rack.left + i * w + w * 0.05f).roundToInt(), (rack.top + rack.height * 0.12f).roundToInt()) }
                .size(with(density) { (w * 0.9f).toDp() })
                .testTag("rack_creation_${c.id}"),
        )
    }
}
