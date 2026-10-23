package app.smriti.domain

import kotlinx.serialization.Serializable

/**
 * Everything SMRITI keeps between sessions, stored locally as versioned JSON.
 *
 * Deliberately absent: any score, timing, error count or performance metric. There is no activity
 * log in this version.
 */
@Serializable
data class SaveData(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val nav: NavState = NavState(),
    val settings: Settings = Settings(),
    val memoryBook: MemoryBookState = MemoryBookState(),
    val playlist: List<RadioTrack> = DefaultPlaylist.tracks,
    val selectedTrackId: String? = null,
) {
    companion object {
        const val CURRENT_SCHEMA = 1
    }
}
