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
        Crossfade(targetState = state.scene, animationSpec = if (reduced) snap() else tween(350), label = "scene") { scene ->
            when {
                scene == SceneId.TITLE -> TitleScreen(onAction)
                scene.isIllustratedScene -> IllustratedScene(scene, layouts, state.showNames, onAction, Modifier.testTag("scene_${scene.name.lowercase()}")) { guideOnRight ->
                    SceneCorners(state, onAction, vm, guideOnRight)
                }
                else -> Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Spacer(Modifier.padding(top = if (narrow) 168.dp else 90.dp))
                    Box(Modifier.weight(1f).padding(bottom = if (state.radio.status != PlaybackStatus.STOPPED && scene != SceneId.RADIO) 86.dp else 12.dp)) {
                        when (scene) {
                            SceneId.MEMORY_BOOK -> MemoryBookScreen(
                                state.memoryBook, media::photoFile, vm::memoryPrevious, vm::memoryNext, vm::readMemoryPageAloud,
                                Modifier.testTag("scene_memory_book"),
                            )
                            SceneId.RADIO -> RadioScreen(
                                state.playlist, state.radio, state.settings.musicVolume, state.settings.musicMuted, vm::onRadio,
                                { v -> vm.updateSettings { it.copy(musicVolume = v) } },
                                Modifier.testTag("scene_radio"),
                            )
                            SceneId.CAREGIVER -> CaregiverScreen(state.memoryBook, state.playlist, caregiverActions, Modifier.testTag("scene_caregiver"))
                            else -> Unit
                        }
                    }
                }
            }
        }

        if (state.scene != SceneId.TITLE) {
            TopBar(state, onAction, guideInBar = !state.scene.isIllustratedScene)
        }
        if (state.radio.status != PlaybackStatus.STOPPED && !state.scene.isIllustratedScene && state.scene != SceneId.RADIO) {
            MiniPlayer(state, vm, Modifier.align(Alignment.BottomEnd).safeDrawingPadding().padding(16.dp))
        }

        state.dialogue?.let { d ->
            val m = when {
                !state.scene.isIllustratedScene -> Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(16.dp)
                narrow -> Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(start = 16.dp, end = 16.dp, bottom = 120.dp)
                else -> Modifier.align(Alignment.BottomStart).safeDrawingPadding().padding(start = 132.dp, end = 16.dp, bottom = 16.dp)
            }
            DialoguePanel(d, state.voiceSpeaking, onAction, m)
        }

        when (state.overlay) {
            Overlay.GUIDE -> GuidePanel(state.scene, state.showNames, onAction)
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
    }
)

/**
 * Back / Home on the left, where-am-I in the middle, End / Settings on the right. Never covered by
 * activities. On narrow screens the scene name moves to its own row so no control is squeezed.
 */
@Composable
private fun TopBar(state: GameUiState, onAction: (GameAction) -> Unit, guideInBar: Boolean) {
    BoxWithConstraints(Modifier.fillMaxWidth().safeDrawingPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
        val narrow = maxWidth < 600.dp
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
        val endButton: @Composable () -> Unit = {
            HudButton(stringResource(R.string.action_end_session), SmritiIcons.Exit, { onAction(GameAction.EndSession) }, Modifier.widthIn(max = 170.dp).testTag("hud_end"))
        }
        val guide: @Composable () -> Unit = {
            HudButton(stringResource(R.string.action_guide), SmritiIcons.Help, { onAction(GameAction.OpenGuide) }, Modifier.widthIn(max = 170.dp).testTag("hud_guide"))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudButton(stringResource(R.string.action_back), SmritiIcons.Back, { onAction(GameAction.Back) }, Modifier.widthIn(max = 150.dp).testTag("hud_back"))
                val home = SceneNavigator.homeFor(state.nav)
                if (state.scene != home && state.scene.isInsideHouse) {
                    HudButton(stringResource(R.string.action_home), SmritiIcons.Home, { onAction(GameAction.Home) }, Modifier.widthIn(max = 150.dp).testTag("hud_home"))
                }
                if (narrow) Spacer(Modifier.weight(1f)) else name(Modifier.weight(1f))
                if (guideInBar && !narrow) guide()
                if (state.scene == SceneId.EXTERIOR && !narrow) endButton()
                HudButton(stringResource(R.string.action_settings), SmritiIcons.Settings, { onAction(GameAction.OpenSettings) }, Modifier.widthIn(max = 170.dp).testTag("hud_settings"))
            }
            // Narrow screens: the place name (and the Guide on non-picture screens) get their own row.
            if (narrow) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    name(Modifier.weight(1f))
                    if (guideInBar) guide()
                    if (state.scene == SceneId.EXTERIOR) endButton()
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
