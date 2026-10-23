package app.smriti.interactions

import androidx.annotation.StringRes
import app.smriti.R
import app.smriti.domain.Catalog
import app.smriti.domain.SceneId
import app.smriti.domain.StallId
import app.smriti.domain.TripState

/**
 * What each hotspot does and what it is called. Positions come from the scene layout JSON that is
 * generated together with the artwork; this catalog gives each hotspot ID its meaning.
 * [order] is the focus/reading order for TalkBack and for the "Places here" list.
 * [number] is drawn by the app on the hotspot (bus number boards), never baked into the art.
 */
data class HotspotSpec(
    val id: String,
    @StringRes val label: Int,
    val action: GameAction,
    val order: Int,
    val number: Int? = null,
)

object HotspotCatalog {
    private fun stallItems(stall: StallId): List<HotspotSpec> =
        listOf(HotspotSpec("vendor", vendorLabel(stall), GameAction.TalkToVendor(stall), 0)) +
            Catalog.forStall(stall).mapIndexed { i, item -> HotspotSpec(item.id, item.label, GameAction.SelectItem(item.id), i + 1) }

    private fun vendorLabel(stall: StallId) = when (stall) {
        StallId.PRODUCE -> R.string.speaker_vendor_produce
        StallId.SPICE -> R.string.speaker_vendor_spice
        StallId.FLOWER -> R.string.speaker_vendor_flower
        StallId.CLOTH -> R.string.speaker_vendor_cloth
    }

    private val specs: Map<SceneId, List<HotspotSpec>> = mapOf(
        SceneId.EXTERIOR to listOf(
            HotspotSpec("front_door", R.string.hotspot_front_door_enter, GameAction.GoTo(SceneId.VERANDA), 0),
        ),
        SceneId.VERANDA to listOf(
            HotspotSpec("bedroom_door", R.string.hotspot_bedroom, GameAction.GoTo(SceneId.BEDROOM), 0),
            HotspotSpec("kitchen_door", R.string.hotspot_kitchen, GameAction.GoTo(SceneId.KITCHEN), 1),
            HotspotSpec("sewing_corner", R.string.hotspot_sewing, GameAction.GoTo(SceneId.SEWING), 2),
            HotspotSpec("front_door", R.string.hotspot_front_door, GameAction.OpenDestinationChooser, 3),
        ),
        SceneId.BEDROOM to listOf(
            HotspotSpec("memory_book", R.string.hotspot_memory_book, GameAction.GoTo(SceneId.MEMORY_BOOK), 0),
            HotspotSpec("radio", R.string.hotspot_radio, GameAction.GoTo(SceneId.RADIO), 1),
        ),
        SceneId.KITCHEN to listOf(
            HotspotSpec("cook_area", R.string.hotspot_cook, GameAction.GoTo(SceneId.COOKING), 0),
            HotspotSpec("sink", R.string.hotspot_wash_dishes, GameAction.GoTo(SceneId.DISHES), 1),
        ),
        SceneId.SEWING to listOf(
            HotspotSpec("sewing_machine", R.string.hotspot_sewing_machine, GameAction.GoTo(SceneId.STITCH), 0),
            HotspotSpec("creations_rack", R.string.hotspot_creations, GameAction.OpenCreations, 1),
        ),
        SceneId.BUS_STOP to TripState.BUS_OPTIONS.mapIndexed { i, n ->
            HotspotSpec("bus_${i + 1}", R.string.hotspot_bus, GameAction.ChooseBus(n), i, number = n)
        },
        SceneId.BUS to listOf(
            HotspotSpec("conductor", R.string.speaker_conductor, GameAction.TalkToConductor, 0),
        ),
        SceneId.MARKET to StallId.entries.mapIndexed { i, st -> HotspotSpec(st.scene.name.lowercase(), st.label, GameAction.GoTo(st.scene), i) },
        SceneId.STALL_PRODUCE to stallItems(StallId.PRODUCE),
        SceneId.STALL_SPICE to stallItems(StallId.SPICE),
        SceneId.STALL_FLOWER to stallItems(StallId.FLOWER),
        SceneId.STALL_CLOTH to stallItems(StallId.CLOTH),
    )

    fun forScene(scene: SceneId): List<HotspotSpec> = specs[scene].orEmpty().sortedBy { it.order }

    fun find(scene: SceneId, id: String): HotspotSpec? = specs[scene]?.firstOrNull { it.id == id }

    val scenesWithHotspots: Set<SceneId> get() = specs.keys
}
