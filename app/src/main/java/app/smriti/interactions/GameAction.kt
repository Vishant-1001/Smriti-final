package app.smriti.interactions

import app.smriti.domain.SceneId
import app.smriti.domain.StallId
import app.smriti.domain.TripDirection

/**
 * Every player intent. Scene hotspots, the Guide's "Places here" list, HUD buttons and tests all
 * dispatch these same actions, so the tappable picture and its accessible alternatives cannot drift.
 */
sealed interface GameAction {
    data object Start : GameAction
    data class GoTo(val scene: SceneId) : GameAction
    data object Back : GameAction
    data object Home : GameAction
    data object EndSession : GameAction

    data object OpenDestinationChooser : GameAction
    data object CloseDestinationChooser : GameAction

    /** Opens the travel screen (shopping list + bus or go straight there). */
    data class PlanTrip(val direction: TripDirection) : GameAction
    data object TakeBus : GameAction
    data object ArriveDirectly : GameAction
    data class ChooseBus(val number: Int) : GameAction
    data object TalkToConductor : GameAction
    data object SkipRide : GameAction
    data object SayBusNumber : GameAction

    data class TalkToVendor(val stall: StallId) : GameAction
    data class SelectItem(val itemId: String) : GameAction
    data object CloseItem : GameAction
    data object OpenShopping : GameAction
    data object CloseShopping : GameAction

    data object OpenCreations : GameAction
    data object CloseCreations : GameAction

    data object OpenGuide : GameAction
    data object CloseGuide : GameAction
    data object GuideHelpHere : GameAction
    data object GuideRepeat : GameAction
    data object ToggleShowNames : GameAction
    data object StopVoice : GameAction
    data object DismissDialogue : GameAction

    data object OpenSettings : GameAction
    data object CloseSettings : GameAction
}
