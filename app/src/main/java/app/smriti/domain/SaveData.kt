package app.smriti.domain

import kotlinx.serialization.Serializable

/**
 * Everything SMRITI keeps between sessions, stored locally as versioned JSON.
 *
 * Deliberately absent: any score, timing, error count or performance metric. There is no activity
 * log in this version. Activity progress (cooking, dishes, sewing, the bus trip) is kept so the
 * person can leave and come back without losing their place.
 *
 * Schema history: 1 = M1 (navigation, settings, Memory Book, radio); 2 = adds shopping, kitchen,
 * sewing and trip state. Missing fields default safely, so v1 files load unchanged.
 */
@Serializable
data class SaveData(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val nav: NavState = NavState(),
    val settings: Settings = Settings(),
    val memoryBook: MemoryBookState = MemoryBookState(),
    val playlist: List<RadioTrack> = DefaultPlaylist.tracks,
    val selectedTrackId: String? = null,
    val shopping: ShoppingState = ShoppingState(),
    val cooking: CookingState = CookingState(),
    val dishes: DishwashState = DishwashState(),
    val sewing: SewingState = SewingState(),
    val creations: List<Creation> = emptyList(),
    val trip: TripState? = null,
) {
    companion object {
        const val CURRENT_SCHEMA = 2
    }
}
