package app.smriti.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.smriti.R
import app.smriti.domain.PlaybackStatus
import app.smriti.domain.RadioSession
import app.smriti.domain.RadioTrack
import app.smriti.ui.components.Badge
import app.smriti.ui.components.BigButton
import app.smriti.ui.components.Panel
import app.smriti.ui.components.QuietButton
import app.smriti.ui.components.SmritiIcons
import app.smriti.ui.components.rememberAssetBitmap
import app.smriti.ui.overlays.VolumeRow
import app.smriti.ui.theme.SmritiColors

@Composable
fun trackTitle(track: RadioTrack): String = when (track.id) {
    "demo_tune_1" -> stringResource(R.string.radio_demo_1)
    "demo_tune_2" -> stringResource(R.string.radio_demo_2)
    else -> track.title
}

/** A radio layout chosen by SMRITI Fit; `null` keeps the fixed width thresholds below. */
data class RadioLayout(val columns: Int, val stacked: Boolean, val picture: Boolean, val landscapeSplit: Boolean)

/**
 * The bedroom radio. Nothing plays until a song is chosen; Stop is always visible and stops at
 * once; "Not now" sets a song aside for this listening session only.
 */
@Composable
fun RadioScreen(
    playlist: List<RadioTrack>,
    session: RadioSession,
    musicVolume: Float,
    musicMuted: Boolean,
    onEvent: (RadioSession.Event) -> Unit,
    onVolume: (Float) -> Unit,
    modifier: Modifier = Modifier,
    fit: RadioLayout? = null,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(SmritiColors.Cream)) {
        val landscape = maxWidth > maxHeight && (fit?.landscapeSplit ?: true)
        val stacked = fit?.stacked ?: (maxWidth < 600.dp || (landscape && maxWidth < 900.dp))
        // Very narrow phones: one control per line so long Tamil words never break.
        val veryNarrow = maxWidth < 380.dp
        val showPicture = fit?.picture ?: true
        val available = session.available(playlist)
        val selected = playlist.firstOrNull { it.id == session.selectedTrackId }
        val picture: @Composable (Modifier) -> Unit = { m ->
            val radio = rememberAssetBitmap("props/radio.webp")
            Box(m, contentAlignment = Alignment.Center) {
                if (radio != null) Image(radio, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
        }
        val controls: @Composable (Modifier) -> Unit = { m ->
            Panel(m) {
                val statusText = when (session.status) {
                    PlaybackStatus.PLAYING -> R.string.radio_status_playing
                    PlaybackStatus.PAUSED -> R.string.radio_status_paused
                    PlaybackStatus.STOPPED -> R.string.radio_status_stopped
                }
                Text(
                    stringResource(statusText) + (selected?.let { " · " + trackTitle(it) } ?: ""),
                    style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("radio_status"),
                )
                if (session.unavailableTrackId != null) {
                    Text(stringResource(R.string.radio_unavailable), style = MaterialTheme.typography.bodyMedium, color = SmritiColors.InkSoft, modifier = Modifier.testTag("radio_unavailable"))
                }
                Spacer(Modifier.size(12.dp))
                val playPause: @Composable (Modifier) -> Unit = { itemModifier ->
                    if (session.status == PlaybackStatus.PLAYING) {
                        BigButton(stringResource(R.string.radio_pause), { onEvent(RadioSession.Event.Pause) }, itemModifier.testTag("radio_pause"), SmritiIcons.Pause, color = SmritiColors.Leaf, stacked = stacked)
                    } else {
                        BigButton(stringResource(R.string.radio_play), { onEvent(RadioSession.Event.Play) }, itemModifier.testTag("radio_play"), SmritiIcons.Play, color = SmritiColors.Leaf, enabled = available.isNotEmpty(), stacked = stacked)
                    }
                }
                val stop: @Composable (Modifier) -> Unit = { itemModifier ->
                    BigButton(stringResource(R.string.radio_stop), { onEvent(RadioSession.Event.Stop) }, itemModifier.testTag("radio_stop"), SmritiIcons.Stop, stacked = stacked)
                }
                val next: @Composable (Modifier) -> Unit = { itemModifier ->
                    QuietButton(stringResource(R.string.radio_next), { onEvent(RadioSession.Event.Next) }, itemModifier.testTag("radio_next"), SmritiIcons.SkipNext, enabled = available.size > 1, stacked = stacked)
                }
                val notNow: @Composable (Modifier) -> Unit = { itemModifier ->
                    QuietButton(
                        stringResource(R.string.radio_not_now),
                        { selected?.let { onEvent(RadioSession.Event.SkipForSession(it.id)) } },
                        itemModifier.testTag("radio_not_now"), SmritiIcons.NotNow, enabled = selected != null, stacked = stacked,
                    )
                }
                if (fit == null) {
                    ControlRow(veryNarrow) { itemModifier -> playPause(itemModifier); stop(itemModifier) }
                    ControlRow(veryNarrow) { itemModifier -> next(itemModifier); notNow(itemModifier) }
                } else {
                    // Fit's column count: rows of equal-width buttons, in the same order.
                    listOf(playPause, stop, next, notNow).chunked(fit.columns.coerceIn(1, 4)).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                            row.forEach { it(Modifier.weight(1f)) }
                            repeat(fit.columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                VolumeRow(stringResource(R.string.radio_volume), musicVolume, !musicMuted, "radio_volume", onVolume)
            }
        }
        val list: @Composable () -> Unit = {
            Text(stringResource(R.string.radio_choose), style = MaterialTheme.typography.titleLarge)
            if (available.isEmpty()) Text(stringResource(R.string.radio_empty), style = MaterialTheme.typography.bodyLarge)
            available.forEach { track -> TrackRow(track, track.id == session.selectedTrackId) { onEvent(RadioSession.Event.Select(track.id)) } }
            if (session.skippedThisSession.isNotEmpty()) {
                Text(stringResource(R.string.radio_skipped_count, session.skippedThisSession.size), style = MaterialTheme.typography.bodyMedium, color = SmritiColors.InkSoft)
            }
        }
        if (landscape) {
            Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    if (showPicture) picture(Modifier.weight(1f).fillMaxWidth())
                    controls(Modifier.fillMaxWidth())
                }
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) { list() }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (showPicture) picture(Modifier.fillMaxWidth().heightIn(max = 220.dp).padding(horizontal = 40.dp))
                controls(Modifier.fillMaxWidth())
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { list() }
            }
        }
    }
}

/** Two buttons side by side, or one per line on very narrow screens. */
@Composable
private fun ControlRow(singleColumn: Boolean, content: @Composable (itemModifier: Modifier) -> Unit) {
    if (singleColumn) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) { content(Modifier.fillMaxWidth()) }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) { content(Modifier.weight(1f)) }
    }
}

@Composable
private fun TrackRow(track: RadioTrack, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp).semantics { this.selected = selected }.testTag("track_${track.id}"),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) SmritiColors.Leaf else SmritiColors.Paper,
        contentColor = if (selected) Color.White else SmritiColors.Ink,
        border = BorderStroke(2.dp, SmritiColors.Outline),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(SmritiIcons.Music, contentDescription = null, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Text(trackTitle(track), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (track.isDemo) Badge(stringResource(R.string.radio_demo_badge), SmritiColors.Indigo)
        }
    }
}
