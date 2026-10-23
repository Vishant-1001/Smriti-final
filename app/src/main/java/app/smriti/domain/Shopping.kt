package app.smriti.domain

import kotlinx.serialization.Serializable

/**
 * The picture shopping list and the bag. The list is a reminder the person can open at any time,
 * not a memory test. Taking something that is not on the list is simply browsing: it can go in the
 * bag as an extra, and it never ticks off or removes a list entry.
 */
@Serializable
data class ShoppingState(
    val list: List<String> = DEFAULT_LIST,
    /** Items currently in the shopping bag (one of each). */
    val bag: List<String> = emptyList(),
    /** Items unpacked at home (optional; purely a tidy-up). */
    val putAway: List<String> = emptyList(),
) {
    fun isCollected(entry: String): Boolean =
        if (entry == ANY_CLOTH) (bag + putAway).any { Catalog.item(it)?.isCloth == true } else entry in bag || entry in putAway

    val collectedCount: Int get() = list.count { isCollected(it) }
    val allCollected: Boolean get() = list.all { isCollected(it) }

    fun isOnList(itemId: String): Boolean =
        itemId in list || (Catalog.item(itemId)?.isCloth == true && ANY_CLOTH in list)

    fun addToBag(itemId: String): ShoppingState =
        if (itemId in bag || Catalog.item(itemId)?.stall == null) this else copy(bag = bag + itemId)

    fun putBack(itemId: String): ShoppingState = copy(bag = bag - itemId)

    /** Moves everything from the bag onto the shelves at home. Nothing is lost. */
    fun unpack(): ShoppingState = copy(bag = emptyList(), putAway = (putAway + bag).distinct())

    /** Fabrics the sewing corner can use: the starter fabric plus any cloth that was bought. */
    val fabrics: List<String>
        get() = listOf(Catalog.DEFAULT_FABRIC) + (bag + putAway).filter { Catalog.item(it)?.isCloth == true }.distinct()

    /** The first list entry not yet collected that this stall sells, for hints. */
    fun nextWanted(stall: StallId): String? =
        list.firstOrNull { !isCollected(it) && (if (it == ANY_CLOTH) stall == StallId.CLOTH else Catalog.item(it)?.stall == stall) }

    companion object {
        /** Any of the cloth patterns satisfies this entry; the person chooses the one they like. */
        const val ANY_CLOTH = "any_cloth"
        val DEFAULT_LIST = listOf("tomato", "banana", "turmeric", "jasmine", ANY_CLOTH)
    }
}

/**
 * The five progressive hints. Any level can be chosen directly at any time; none is unlocked by
 * mistakes and none is recorded.
 */
enum class HintLevel { SHOW_LIST, SAY_NAME, WHERE_TO_LOOK, HIGHLIGHT, PLACE_FOR_ME }

/** Where an item sits on the counter, from its hotspot centre (0..1 across the picture). */
enum class Region { LEFT, MIDDLE, RIGHT }

fun regionOf(centerX: Float): Region = when {
    centerX < 0.36f -> Region.LEFT
    centerX > 0.64f -> Region.RIGHT
    else -> Region.MIDDLE
}
