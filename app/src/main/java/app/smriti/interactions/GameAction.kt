package app.smriti.interactions

import app.smriti.domain.SceneId

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

    /** A place that exists in the house but is not built in this version (honest, neutral message). */
    data class NotYetAvailable(val place: Place) : GameAction

    data object OpenGuide : GameAction
    data object CloseGuide : GameAction
    data object GuideHelpHere : GameAction
    data object GuideRepeat : GameAction
    data object ToggleShowNames : GameAction
    data object StopVoice : GameAction
    data object DismissDialogue : GameAction

    data object OpenSettings : GameAction
    data object CloseSettings : GameAction

    enum class Place { KITCHEN, SEWING_CORNER }
}
