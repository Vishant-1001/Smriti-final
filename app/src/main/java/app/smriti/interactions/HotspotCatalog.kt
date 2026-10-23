package app.smriti.interactions

import androidx.annotation.StringRes
import app.smriti.R
import app.smriti.domain.SceneId

/**
 * What each hotspot does and what it is called. Positions come from the scene layout JSON that is
 * generated together with the artwork; this catalog gives each hotspot ID its meaning.
 * [order] is the focus/reading order for TalkBack and for the "Places here" list.
 */
data class HotspotSpec(
    val id: String,
    @StringRes val label: Int,
    val action: GameAction,
    val order: Int,
)

object HotspotCatalog {
    private val specs: Map<SceneId, List<HotspotSpec>> = mapOf(
        SceneId.EXTERIOR to listOf(
            HotspotSpec("front_door", R.string.hotspot_front_door_enter, GameAction.GoTo(SceneId.VERANDA), 0),
        ),
        SceneId.VERANDA to listOf(
            HotspotSpec("bedroom_door", R.string.hotspot_bedroom, GameAction.GoTo(SceneId.BEDROOM), 0),
            HotspotSpec("kitchen_door", R.string.hotspot_kitchen, GameAction.NotYetAvailable(GameAction.Place.KITCHEN), 1),
            HotspotSpec("sewing_corner", R.string.hotspot_sewing, GameAction.NotYetAvailable(GameAction.Place.SEWING_CORNER), 2),
            HotspotSpec("front_door", R.string.hotspot_front_door, GameAction.OpenDestinationChooser, 3),
        ),
        SceneId.BEDROOM to listOf(
            HotspotSpec("memory_book", R.string.hotspot_memory_book, GameAction.GoTo(SceneId.MEMORY_BOOK), 0),
            HotspotSpec("radio", R.string.hotspot_radio, GameAction.GoTo(SceneId.RADIO), 1),
        ),
    )

    fun forScene(scene: SceneId): List<HotspotSpec> = specs[scene].orEmpty().sortedBy { it.order }

    fun find(scene: SceneId, id: String): HotspotSpec? = specs[scene]?.firstOrNull { it.id == id }

    val scenesWithHotspots: Set<SceneId> get() = specs.keys
}
