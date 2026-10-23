package app.smriti.domain

import kotlinx.serialization.Serializable

/**
 * Stable identifiers for every screen the player can be on. These names are persisted in the
 * save file, so never rename an entry; add new ones instead.
 */
@Serializable
enum class SceneId(val isIllustratedScene: Boolean, val isInsideHouse: Boolean) {
    TITLE(isIllustratedScene = false, isInsideHouse = false),
    EXTERIOR(isIllustratedScene = true, isInsideHouse = false),
    VERANDA(isIllustratedScene = true, isInsideHouse = true),
    BEDROOM(isIllustratedScene = true, isInsideHouse = true),
    MEMORY_BOOK(isIllustratedScene = false, isInsideHouse = true),
    RADIO(isIllustratedScene = false, isInsideHouse = true),
    CAREGIVER(isIllustratedScene = false, isInsideHouse = false),
    KITCHEN(isIllustratedScene = true, isInsideHouse = true),
    COOKING(isIllustratedScene = false, isInsideHouse = true),
    DISHES(isIllustratedScene = false, isInsideHouse = true),
    SEWING(isIllustratedScene = true, isInsideHouse = true),
    STITCH(isIllustratedScene = false, isInsideHouse = true),
    /** Getting ready to travel (shopping list + bus or go straight there), both directions. */
    TRIP(isIllustratedScene = false, isInsideHouse = false),
    BUS_STOP(isIllustratedScene = true, isInsideHouse = false),
    BUS(isIllustratedScene = true, isInsideHouse = false),
    MARKET(isIllustratedScene = true, isInsideHouse = false),
    STALL_PRODUCE(isIllustratedScene = true, isInsideHouse = false),
    STALL_SPICE(isIllustratedScene = true, isInsideHouse = false),
    STALL_FLOWER(isIllustratedScene = true, isInsideHouse = false),
    STALL_CLOTH(isIllustratedScene = true, isInsideHouse = false),
    ;

    val isMarket: Boolean get() = this == MARKET || name.startsWith("STALL_")
}

/** Asset key used for scene artwork and hotspot layouts (`scenes/<key>_<variant>.webp|json`). */
val SceneId.artKey: String? get() = if (isIllustratedScene) name.lowercase() else null
