package app.smriti.domain

import kotlinx.serialization.Serializable

/** A song on the caregiver-curated radio playlist. Always a local file; never a stream. */
@Serializable
data class RadioTrack(
    val id: String,
    val title: String,
    val source: Source,
    /** Bundled demo tunes are synthesised test audio and are labelled as such in the UI. */
    val isDemo: Boolean = false,
) {
    @Serializable
    sealed interface Source {
        /** Bundled with the app under assets/. */
        @Serializable
        data class Asset(val path: String) : Source

        /** Imported by a caregiver; file name inside the app's private radio folder. */
        @Serializable
        data class LocalFile(val fileName: String) : Source
    }
}

enum class PlaybackStatus { STOPPED, PLAYING, PAUSED }

/**
 * The radio's listening-session state machine. Pure: the audio layer observes this state and
 * follows it, so every transition is testable without Android.
 *
 * Rules that protect the listener:
 *  - nothing ever starts playing without a direct user action (no autoplay on entering a room);
 *  - Stop always moves to STOPPED immediately;
 *  - "Not now" removes a track for this session only and does not start another track.
 */
data class RadioSession(
    val selectedTrackId: String? = null,
    val status: PlaybackStatus = PlaybackStatus.STOPPED,
    val skippedThisSession: Set<String> = emptySet(),
    /** Set when a track could not be played (e.g. the file is missing). Cleared by the next action. */
    val unavailableTrackId: String? = null,
) {
    sealed interface Event {
        data class Select(val trackId: String) : Event
        data object Play : Event
        data object Pause : Event
        data object Stop : Event
        data object Next : Event
        data class SkipForSession(val trackId: String) : Event
        data object TrackFinished : Event
        data class PlaybackFailed(val trackId: String) : Event
        /** Leaving the radio, backgrounding the app, or another app taking audio focus. */
        data object Interrupted : Event
    }

    fun available(playlist: List<RadioTrack>): List<RadioTrack> =
        playlist.filterNot { it.id in skippedThisSession }

    fun reduce(event: Event, playlist: List<RadioTrack>): RadioSession {
        val avail = available(playlist)
        val cleared = copy(unavailableTrackId = null)
        return when (event) {
            is Event.Select ->
                if (avail.any { it.id == event.trackId }) cleared.copy(selectedTrackId = event.trackId, status = PlaybackStatus.PLAYING)
                else cleared
            Event.Play -> {
                val target = selectedTrackId?.takeIf { id -> avail.any { it.id == id } } ?: avail.firstOrNull()?.id
                if (target == null) cleared.copy(status = PlaybackStatus.STOPPED)
                else cleared.copy(selectedTrackId = target, status = PlaybackStatus.PLAYING)
            }
            Event.Pause -> if (status == PlaybackStatus.PLAYING) cleared.copy(status = PlaybackStatus.PAUSED) else cleared
            Event.Stop -> cleared.copy(status = PlaybackStatus.STOPPED)
            Event.Next -> {
                val next = nextAfter(selectedTrackId, avail)
                if (next == null) cleared.copy(status = PlaybackStatus.STOPPED)
                else cleared.copy(selectedTrackId = next.id, status = if (status == PlaybackStatus.STOPPED) PlaybackStatus.STOPPED else PlaybackStatus.PLAYING)
            }
            is Event.SkipForSession -> {
                val skipped = skippedThisSession + event.trackId
                if (event.trackId == selectedTrackId) {
                    val next = nextAfter(selectedTrackId, avail)?.takeIf { it.id != event.trackId }
                    cleared.copy(skippedThisSession = skipped, selectedTrackId = next?.id, status = PlaybackStatus.STOPPED)
                } else cleared.copy(skippedThisSession = skipped)
            }
            Event.TrackFinished -> {
                val idx = avail.indexOfFirst { it.id == selectedTrackId }
                val next = if (idx >= 0) avail.getOrNull(idx + 1) else null
                // Play through the list once, then rest in silence rather than looping forever.
                if (next == null) cleared.copy(status = PlaybackStatus.STOPPED)
                else cleared.copy(selectedTrackId = next.id, status = PlaybackStatus.PLAYING)
            }
            is Event.PlaybackFailed -> copy(status = PlaybackStatus.STOPPED, unavailableTrackId = event.trackId)
            Event.Interrupted -> if (status == PlaybackStatus.PLAYING) cleared.copy(status = PlaybackStatus.PAUSED) else cleared
        }
    }

    /** Called when the caregiver edits the playlist: drop references to tracks that no longer exist. */
    fun reconcile(playlist: List<RadioTrack>): RadioSession {
        val ids = playlist.map { it.id }.toSet()
        return if (selectedTrackId != null && selectedTrackId !in ids) RadioSession(skippedThisSession = skippedThisSession intersect ids)
        else copy(skippedThisSession = skippedThisSession intersect ids)
    }

    private fun nextAfter(currentId: String?, avail: List<RadioTrack>): RadioTrack? {
        if (avail.isEmpty()) return null
        val idx = avail.indexOfFirst { it.id == currentId }
        return if (idx < 0) avail.first() else avail[(idx + 1) % avail.size]
    }
}

object DefaultPlaylist {
    val tracks = listOf(
        RadioTrack("demo_tune_1", "", RadioTrack.Source.Asset("audio/radio/demo_tune_1.ogg"), isDemo = true),
        RadioTrack("demo_tune_2", "", RadioTrack.Source.Asset("audio/radio/demo_tune_2.ogg"), isDemo = true),
    )
}
