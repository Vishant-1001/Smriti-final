package app.smriti.domain

import kotlinx.serialization.Serializable

/**
 * The player's position as an explicit back stack. Kept tiny and serialisable so it can be
 * restored after rotation, backgrounding or process death.
 */
@Serializable
data class NavState(val stack: List<SceneId> = listOf(SceneId.TITLE)) {
    init {
        require(stack.isNotEmpty()) { "Navigation stack must never be empty" }
    }

    val current: SceneId get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1
}

/** Central scene navigation. Every move in the game goes through here. */
object SceneNavigator {

    fun goTo(state: NavState, target: SceneId): NavState {
        if (state.current == target) return state
        // Re-entering a scene already on the stack returns to it rather than growing the stack.
        val existing = state.stack.indexOf(target)
        if (existing >= 0) return NavState(state.stack.subList(0, existing + 1))
        return NavState(state.stack + target)
    }

    fun back(state: NavState): NavState =
        if (state.canGoBack) NavState(state.stack.dropLast(1)) else state

    /** Home is the veranda hub once the player is inside; outside the house it is the exterior. */
    fun home(state: NavState): NavState {
        val home = homeFor(state)
        return NavState(baseStackTo(home))
    }

    fun homeFor(state: NavState): SceneId =
        if (state.stack.any { it.isInsideHouse || it == SceneId.VERANDA }) SceneId.VERANDA else SceneId.EXTERIOR

    /**
     * Arriving somewhere by bus or directly. The trip screens are dropped from the stack, so Back at
     * the market leads home rather than back onto the bus.
     */
    fun arriveAt(target: SceneId): NavState = NavState(baseStackTo(target))

    /** Ending a session returns to the title screen; nothing the user made is lost. */
    fun endSession(): NavState = NavState(listOf(SceneId.TITLE))

    private fun baseStackTo(target: SceneId): List<SceneId> = when (target) {
        SceneId.TITLE -> listOf(SceneId.TITLE)
        SceneId.EXTERIOR -> listOf(SceneId.TITLE, SceneId.EXTERIOR)
        SceneId.VERANDA -> listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA)
        SceneId.KITCHEN, SceneId.SEWING, SceneId.BEDROOM, SceneId.MARKET, SceneId.TRIP ->
            listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, target)
        SceneId.COOKING, SceneId.DISHES -> listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.KITCHEN, target)
        SceneId.STITCH -> listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.SEWING, target)
        SceneId.MEMORY_BOOK, SceneId.RADIO -> listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.BEDROOM, target)
        SceneId.STALL_PRODUCE, SceneId.STALL_SPICE, SceneId.STALL_FLOWER, SceneId.STALL_CLOTH ->
            listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.MARKET, target)
        else -> listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, target)
    }

    /** Repairs a stack loaded from disk (e.g. from an older app version) into a valid one. */
    fun sanitize(state: NavState?): NavState {
        if (state == null) return NavState()
        val cleaned = state.stack.distinct()
        if (cleaned.isEmpty() || cleaned.first() != SceneId.TITLE) return NavState()
        return NavState(cleaned)
    }
}
