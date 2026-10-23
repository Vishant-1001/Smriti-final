package app.smriti.ui.overlays

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.smriti.R
import app.smriti.audio.VoiceAvailability
import app.smriti.domain.AppLanguage
import app.smriti.domain.SceneId
import app.smriti.domain.Settings
import app.smriti.domain.TextSize
import app.smriti.domain.TripDirection
import app.smriti.interactions.GameAction
import app.smriti.interactions.HotspotCatalog
import app.smriti.ui.ActiveDialogue
import app.smriti.ui.VoiceStatus
import app.smriti.ui.components.BigButton
import app.smriti.ui.components.GuidePortrait
import app.smriti.ui.components.lineText
import app.smriti.ui.components.Panel
import app.smriti.ui.components.QuietButton
import app.smriti.ui.components.SmritiIcons
import app.smriti.ui.components.rememberAssetBitmap
import app.smriti.ui.theme.SmritiColors

/** Dims the scene and closes the overlay when the dimmed area is tapped. */
@Composable
fun Scrim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(SmritiColors.Scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

/** Consumes taps so clicks inside a panel do not reach the scrim. */
fun Modifier.blockTaps() = this.clickable(interactionSource = MutableInteractionSource(), indication = null) {}

// ---------------------------------------------------------------- guide

@Composable
fun GuidePanel(scene: SceneId, showNames: Boolean, onAction: (GameAction) -> Unit) {
    Scrim(onDismiss = { onAction(GameAction.CloseGuide) }) {
        Panel(
            Modifier
                .widthIn(max = 560.dp)
                .blockTaps()
                .verticalScroll(rememberScrollState())
                .testTag("guide_panel"),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GuidePortrait(speaking = false, size = 72.dp)
                Spacer(Modifier.width(14.dp))
                Text(stringResource(R.string.guide_panel_title), style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton(stringResource(R.string.guide_help_here), { onAction(GameAction.GuideHelpHere) }, Modifier.fillMaxWidth().testTag("guide_help_here"), SmritiIcons.Help)
                QuietButton(stringResource(R.string.guide_repeat), { onAction(GameAction.GuideRepeat) }, Modifier.fillMaxWidth(), SmritiIcons.Replay)
                val hotspots = HotspotCatalog.forScene(scene)
                if (hotspots.isNotEmpty()) {
                    QuietButton(
                        stringResource(if (showNames) R.string.guide_hide_names else R.string.guide_show_names),
                        { onAction(GameAction.ToggleShowNames) }, Modifier.fillMaxWidth(), SmritiIcons.Eye,
                    )
                    // The same actions as the picture, as a plain list: an alternative to finding objects in the art.
                    Text(stringResource(R.string.guide_places_here), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
                    hotspots.forEach { spec ->
                        val label = spec.number?.let { "${stringResource(spec.label)} $it" } ?: stringResource(spec.label)
                        QuietButton(label, {
                            onAction(GameAction.CloseGuide)
                            onAction(spec.action)
                        }, Modifier.fillMaxWidth().testTag("place_${spec.id}"), SmritiIcons.Door)
                    }
                }
                QuietButton(stringResource(R.string.action_end_session), {
                    onAction(GameAction.CloseGuide)
                    onAction(GameAction.EndSession)
                }, Modifier.fillMaxWidth().testTag("guide_end_session"), SmritiIcons.Exit)
                QuietButton(stringResource(R.string.action_close), { onAction(GameAction.CloseGuide) }, Modifier.fillMaxWidth(), SmritiIcons.Close)
            }
        }
    }
}

// ---------------------------------------------------------------- dialogue

/**
 * Subtitle panel for spoken lines. The text is always shown, whether or not a voice is available,
 * and Replay / Stop voice / OK are always reachable.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialoguePanel(dialogue: ActiveDialogue, speaking: Boolean, onAction: (GameAction) -> Unit, modifier: Modifier = Modifier) {
    Panel(modifier.widthIn(max = 640.dp).testTag("dialogue_panel")) {
        Row(verticalAlignment = Alignment.Top) {
            GuidePortrait(speaking = speaking, size = 64.dp, portrait = dialogue.line.speaker.portrait)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(dialogue.line.speaker.label), style = MaterialTheme.typography.labelMedium, color = SmritiColors.InkSoft)
                Text(lineText(dialogue.line), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("dialogue_text"))
                val note = when (dialogue.voice) {
                    VoiceStatus.UNAVAILABLE -> R.string.voice_unavailable_note
                    VoiceStatus.OFF -> R.string.voice_off_note
                    else -> null
                }
                if (note != null) {
                    Text(stringResource(note), style = MaterialTheme.typography.bodyMedium, color = SmritiColors.InkSoft, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (speaking) {
                QuietButton(stringResource(R.string.dialogue_stop_voice), { onAction(GameAction.StopVoice) }, Modifier.testTag("stop_voice"), SmritiIcons.SpeakerOff)
            } else {
                QuietButton(stringResource(R.string.dialogue_replay), { onAction(GameAction.GuideRepeat) }, icon = SmritiIcons.Replay)
            }
            BigButton(stringResource(R.string.dialogue_ok), { onAction(GameAction.DismissDialogue) }, Modifier.testTag("dialogue_ok"))
        }
    }
}

// ---------------------------------------------------------------- destination

@Composable
fun DestinationChooser(onAction: (GameAction) -> Unit) {
    Scrim(onDismiss = { onAction(GameAction.CloseDestinationChooser) }) {
        Panel(Modifier.widthIn(max = 1000.dp).blockTaps().verticalScroll(rememberScrollState()).testTag("destination_chooser")) {
            Text(stringResource(R.string.destination_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            BoxWithConstraints {
                val wide = maxWidth > 760.dp
                val cards: @Composable (Modifier) -> Unit = { m ->
                    DestinationCard(
                        stringResource(R.string.destination_market), "scenes/market_land.webp",
                        { onAction(GameAction.CloseDestinationChooser); onAction(GameAction.PlanTrip(TripDirection.TO_MARKET)) },
                        m.testTag("destination_market"),
                    )
                    DestinationCard(
                        stringResource(R.string.destination_outside), "scenes/exterior_land.webp",
                        { onAction(GameAction.CloseDestinationChooser); onAction(GameAction.GoTo(SceneId.EXTERIOR)) },
                        m.testTag("destination_outside"),
                    )
                    DestinationCard(
                        stringResource(R.string.destination_stay), "scenes/veranda_land.webp",
                        { onAction(GameAction.CloseDestinationChooser) },
                        m.testTag("destination_stay"),
                    )
                }
                if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { cards(Modifier.weight(1f)) }
                else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { cards(Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
private fun DestinationCard(label: String, imagePath: String, onClick: () -> Unit, modifier: Modifier) {
    val image = rememberAssetBitmap(imagePath)
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = label },
        shape = RoundedCornerShape(20.dp),
        color = SmritiColors.PaperDim,
        border = androidx.compose.foundation.BorderStroke(2.dp, SmritiColors.Outline),
    ) {
        Column {
            if (image != null) {
                // Short screens (landscape phones) get a shorter picture so the label always fits.
                val pictureHeight = if (LocalConfiguration.current.screenHeightDp < 500) 110.dp else 180.dp
                Image(image, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(pictureHeight).clipToBounds())
            }
            Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(14.dp))
        }
    }
}

// ---------------------------------------------------------------- settings

@Composable
fun SettingsPanel(
    settings: Settings,
    voiceAvailability: VoiceAvailability,
    onChange: ((Settings) -> Settings) -> Unit,
    onOpenCaregiver: () -> Unit,
    onClose: () -> Unit,
) {
    Scrim(onDismiss = onClose) {
        Panel(Modifier.widthIn(max = 620.dp).wrapContentHeight().blockTaps().verticalScroll(rememberScrollState()).testTag("settings_panel")) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            SectionTitle(stringResource(R.string.settings_language))
            Choice(
                listOf(AppLanguage.TAMIL to stringResource(R.string.lang_tamil), AppLanguage.ENGLISH to stringResource(R.string.lang_english)),
                settings.language,
            ) { lang -> onChange { it.copy(language = lang) } }

            SectionTitle(stringResource(R.string.settings_text_size))
            Choice(
                listOf(
                    TextSize.NORMAL to stringResource(R.string.text_normal),
                    TextSize.LARGE to stringResource(R.string.text_large),
                    TextSize.LARGER to stringResource(R.string.text_larger),
                ),
                settings.textSize,
            ) { size -> onChange { it.copy(textSize = size) } }

            ToggleRow(stringResource(R.string.settings_narration), settings.narrationEnabled, "toggle_narration") { v -> onChange { it.copy(narrationEnabled = v) } }
            Text(
                stringResource(
                    when (voiceAvailability) {
                        VoiceAvailability.AVAILABLE -> R.string.voice_status_available
                        VoiceAvailability.UNAVAILABLE -> R.string.voice_status_unavailable
                        VoiceAvailability.CHECKING -> R.string.voice_status_checking
                    }
                ),
                style = MaterialTheme.typography.bodyMedium, color = SmritiColors.InkSoft,
            )
            VolumeRow(stringResource(R.string.settings_narration_volume), settings.narrationVolume, settings.narrationEnabled, "slider_narration") { v ->
                onChange { it.copy(narrationVolume = v) }
            }
            ToggleRow(stringResource(R.string.settings_music_mute), settings.musicMuted, "toggle_music_mute") { v -> onChange { it.copy(musicMuted = v) } }
            VolumeRow(stringResource(R.string.settings_music_volume), settings.musicVolume, !settings.musicMuted, "slider_music") { v ->
                onChange { it.copy(musicVolume = v) }
            }
            ToggleRow(stringResource(R.string.settings_reduced_motion), settings.reducedMotion, "toggle_reduced_motion") { v -> onChange { it.copy(reducedMotion = v) } }
            ToggleRow(stringResource(R.string.settings_always_names), settings.alwaysShowNames, "toggle_names") { v -> onChange { it.copy(alwaysShowNames = v) } }

            Spacer(Modifier.height(16.dp))
            QuietButton(stringResource(R.string.settings_caregiver), onOpenCaregiver, Modifier.fillMaxWidth().testTag("open_caregiver"), SmritiIcons.Edit)
            Spacer(Modifier.height(10.dp))
            BigButton(stringResource(R.string.action_close), onClose, Modifier.fillMaxWidth().testTag("settings_close"), SmritiIcons.Close)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
}

@Composable
private fun <T> Choice(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    // Side by side when there is room; stacked when each option would be too narrow for its word.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth / options.size < 150.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (value, label) -> ChoiceItem(label, value == selected, "choice_$value", Modifier.fillMaxWidth()) { onSelect(value) } }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (value, label) -> ChoiceItem(label, value == selected, "choice_$value", Modifier.weight(1f)) { onSelect(value) } }
            }
        }
    }
}

@Composable
private fun ChoiceItem(label: String, on: Boolean, tag: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp).semantics { stateDescription = if (on) "✓" else "" }.testTag(tag),
        shape = RoundedCornerShape(16.dp),
        color = if (on) SmritiColors.Leaf else SmritiColors.Paper,
        contentColor = if (on) Color.White else SmritiColors.Ink,
        border = androidx.compose.foundation.BorderStroke(2.dp, SmritiColors.Outline),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
            Text((if (on) "✓ " else "") + label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 6.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = SmritiColors.Leaf, uncheckedBorderColor = SmritiColors.Outline, uncheckedThumbColor = SmritiColors.Outline),
        )
    }
}

@Composable
fun VolumeRow(label: String, value: Float, enabled: Boolean, tag: String, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value, onValueChange = onChange, enabled = enabled, valueRange = 0f..1f, steps = 9,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag),
            colors = SliderDefaults.colors(thumbColor = SmritiColors.Terracotta, activeTrackColor = SmritiColors.Terracotta),
        )
    }
}
