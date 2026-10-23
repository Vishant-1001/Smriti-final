package app.smriti.domain

import androidx.annotation.StringRes
import app.smriti.R
import kotlinx.serialization.Serializable

/** The four market stalls. Names are persisted; never rename. */
@Serializable
enum class StallId(val scene: SceneId, @StringRes val label: Int) {
    PRODUCE(SceneId.STALL_PRODUCE, R.string.stall_produce),
    SPICE(SceneId.STALL_SPICE, R.string.stall_spice),
    FLOWER(SceneId.STALL_FLOWER, R.string.stall_flower),
    CLOTH(SceneId.STALL_CLOTH, R.string.stall_cloth),
}

/** A product with a stable ID. The ID doubles as the hotspot ID in the stall art and the sprite name. */
data class ShopItem(
    val id: String,
    val stall: StallId?,
    @StringRes val label: Int,
    @StringRes val category: Int,
) {
    val isCloth: Boolean get() = id.startsWith("cloth_")
    val sprite: String get() = "items/$id.webp"
}

object Catalog {
    val items: List<ShopItem> = listOf(
        ShopItem("tomato", StallId.PRODUCE, R.string.item_tomato, R.string.category_vegetable),
        ShopItem("banana", StallId.PRODUCE, R.string.item_banana, R.string.category_fruit),
        ShopItem("brinjal", StallId.PRODUCE, R.string.item_brinjal, R.string.category_vegetable),
        ShopItem("onion", StallId.PRODUCE, R.string.item_onion, R.string.category_vegetable),
        ShopItem("turmeric", StallId.SPICE, R.string.item_turmeric, R.string.category_spice),
        ShopItem("chilli_powder", StallId.SPICE, R.string.item_chilli_powder, R.string.category_spice),
        ShopItem("rice", StallId.SPICE, R.string.item_rice, R.string.category_pantry),
        ShopItem("coffee_powder", StallId.SPICE, R.string.item_coffee_powder, R.string.category_pantry),
        ShopItem("jasmine", StallId.FLOWER, R.string.item_jasmine, R.string.category_flower),
        ShopItem("marigold", StallId.FLOWER, R.string.item_marigold, R.string.category_flower),
        ShopItem("rose", StallId.FLOWER, R.string.item_rose, R.string.category_flower),
        ShopItem("cloth_indigo_dots", StallId.CLOTH, R.string.item_cloth_indigo_dots, R.string.category_cloth),
        ShopItem("cloth_maroon_checks", StallId.CLOTH, R.string.item_cloth_maroon_checks, R.string.category_cloth),
        ShopItem("cloth_leaf_print", StallId.CLOTH, R.string.item_cloth_leaf_print, R.string.category_cloth),
        // Always at home, never sold: keeps sewing available without a market trip.
        ShopItem(DEFAULT_FABRIC, null, R.string.item_cloth_cream_stripes, R.string.category_cloth),
    )

    private val byId = items.associateBy { it.id }

    fun item(id: String): ShopItem? = byId[id]
    fun forStall(stall: StallId): List<ShopItem> = items.filter { it.stall == stall }

    const val DEFAULT_FABRIC = "cloth_cream_stripes"
}
