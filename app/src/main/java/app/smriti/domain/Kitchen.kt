package app.smriti.domain

import kotlinx.serialization.Serializable

/** Things on the kitchen counter that can be tapped while making coffee. */
@Serializable
enum class CookTool { COFFEE_POWDER, KETTLE, FILTER, MILK }

/** Filter coffee in four steps. No timers, heat danger, measurements or failure state. */
@Serializable
enum class CookStep(val tool: CookTool?) {
    POWDER_IN_FILTER(CookTool.COFFEE_POWDER),
    WATER_IN_FILTER(CookTool.KETTLE),
    DECOCTION_TO_TUMBLER(CookTool.FILTER),
    ADD_MILK(CookTool.MILK),
    DONE(null),
}

@Serializable
data class CookingState(
    val step: CookStep = CookStep.POWDER_IN_FILTER,
    /** Set when a different tool was tapped: shown as a gentle pointer, never as an error. Not persisted meaning. */
    val notNow: CookTool? = null,
    val assistedSteps: Int = 0,
) {
    val isDone: Boolean get() = step == CookStep.DONE
    val stepNumber: Int get() = step.ordinal + 1
    val totalSteps: Int get() = CookStep.entries.size - 1

    fun tap(tool: CookTool): CookingState = when {
        isDone -> this
        tool == step.tool -> copy(step = CookStep.entries[step.ordinal + 1], notNow = null)
        else -> copy(notNow = tool)
    }

    /** "Do it for me": completes the current step exactly as if the person had done it. */
    fun doStep(): CookingState = if (isDone) this else copy(step = CookStep.entries[step.ordinal + 1], notNow = null, assistedSteps = assistedSteps + 1)

    fun undo(): CookingState = if (step.ordinal == 0) copy(notNow = null) else copy(step = CookStep.entries[step.ordinal - 1], notNow = null)

    fun reset(): CookingState = CookingState()

    /** What the filter and tumbler look like now (sprite suffixes), derived from the real step. */
    val filterLook: String
        get() = when (step) {
            CookStep.POWDER_IN_FILTER -> "empty"
            CookStep.WATER_IN_FILTER -> "powder"
            CookStep.DECOCTION_TO_TUMBLER -> "brewing"
            else -> "empty"
        }
    val tumblerLook: String
        get() = when (step) {
            CookStep.ADD_MILK -> "decoction"
            CookStep.DONE -> "coffee"
            else -> "empty"
        }
}

@Serializable
enum class DishKind { PLATE, CUP, BOWL }

@Serializable
enum class DishCondition { DIRTY, CLEAN, ON_RACK }

@Serializable
data class Dish(val id: String, val kind: DishKind, val condition: DishCondition = DishCondition.DIRTY)

/**
 * Washing up by tapping: a dirty dish becomes clean, a clean dish goes onto the rack. In free mode
 * any order is fine; guided mode only suggests the next dish. Nothing is ever rejected.
 */
@Serializable
data class DishwashState(
    val dishes: List<Dish> = DEFAULT_DISHES,
    val guided: Boolean = false,
) {
    val isDone: Boolean get() = dishes.all { it.condition == DishCondition.ON_RACK }
    val onRack: List<Dish> get() = dishes.filter { it.condition == DishCondition.ON_RACK }

    /** The dish guided mode points at: the first one not yet on the rack. */
    val suggested: Dish? get() = dishes.firstOrNull { it.condition != DishCondition.ON_RACK }

    fun tap(dishId: String): DishwashState = copy(dishes = dishes.map { d ->
        if (d.id != dishId) d else when (d.condition) {
            DishCondition.DIRTY -> d.copy(condition = DishCondition.CLEAN)
            DishCondition.CLEAN -> d.copy(condition = DishCondition.ON_RACK)
            DishCondition.ON_RACK -> d
        }
    })

    fun doAll(): DishwashState = copy(dishes = dishes.map { it.copy(condition = DishCondition.ON_RACK) })
    fun reset(): DishwashState = DishwashState(guided = guided)
    fun withGuided(on: Boolean): DishwashState = copy(guided = on)

    companion object {
        val DEFAULT_DISHES = listOf(
            Dish("plate_1", DishKind.PLATE),
            Dish("plate_2", DishKind.PLATE),
            Dish("cup_1", DishKind.CUP),
            Dish("bowl_1", DishKind.BOWL),
        )
    }
}
