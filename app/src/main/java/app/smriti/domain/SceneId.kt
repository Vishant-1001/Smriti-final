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
}

/** Asset key used for scene artwork and hotspot layouts (`scenes/<key>_<variant>.webp|json`). */
val SceneId.artKey: String? get() = if (isIllustratedScene) name.lowercase() else null
