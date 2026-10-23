package app.smriti.fit

/**
 * SMRITI Fit: inputs and outputs of the explicit-needs presentation solver.
 *
 * Every input is something the person, a caregiver or the device states explicitly. There are no
 * behavioural, performance or sensor fields, so nothing is inferred about the person.
 */

enum class Lang { TAMIL, ENGLISH }

/** In-app text size levels (the app's `TextSize`: NORMAL 1.0, LARGE 1.2, LARGER 1.4). */
enum class TextLevel(val scale: Double) { NORMAL(1.0), LARGE(1.2), LARGER(1.4) }

/** Explicit needs and preferences, as set in Settings by the person or a caregiver. */
data class Needs(
    val language: Lang = Lang.TAMIL,
    val textLevel: TextLevel = TextLevel.LARGE,
    /** true: text size is a need (never shown smaller); false: a preference that may be reduced, with an explanation. */
    val textIsNeed: Boolean = false,
    /** Minimum touch target in dp (Android guidance ≥ 48; larger for people who ask for it). */
    val minTargetDp: Int = 48,
    val reducedMotion: Boolean = false,
)

/** The window SMRITI is drawn in, in dp, and the system font scale. Not the person. */
data class Device(
    val widthDp: Int,
    val heightDp: Int,
    val systemFontScale: Double = 1.0,
    /** Height taken by system bars: status bar (24) + gesture navigation (24) by assumption; 0 in Robolectric. */
    val systemBarsDp: Double = 48.0,
    /** Android 14+ (API 34): Compose scales large text non-linearly (measured table, [FontScaling]). */
    val nonLinearFontScaling: Boolean = true,
) {
    val landscape: Boolean get() = widthDp > heightDp
}

/** A control on screen. [essential] controls (Back, Home, Settings, Stop, ...) must always be visible without scrolling. */
data class Control(val id: String, val label: String, val essential: Boolean = true, val icon: Boolean = true)

/**
 * Top bar: [leading] (Back, Home), the place name [title], context [extras] and [trailing] (Settings).
 * Mirrors `SmritiRoot.TopBar`.
 */
data class HudSpec(val leading: List<Control>, val title: String, val extras: List<Control>, val trailing: List<Control>) {
    val buttons: List<Control> get() = leading + extras + trailing
}

/**
 * An activity panel such as the radio controls: optional illustration, a status line, a group of
 * buttons, a fixed-height row below them (for example, volume), then scrollable content.
 * Mirrors `RadioScreen`.
 */
data class PanelSpec(
    val status: String?,
    val controls: List<Control>,
    val hasIllustration: Boolean,
    /** Height (dp at scale 1) of a fixed row under the buttons, such as the volume slider label + slider. */
    val trailingRowLabelLines: Int = 1,
    val trailingRowFixedDp: Double = 48.0,
    /** Columns the designer intended (the radio uses two buttons per row). */
    val designColumns: Int = 2,
)

data class Screen(val id: String, val language: Lang, val hud: HudSpec, val panel: PanelSpec)

enum class ButtonStyle { INLINE, STACKED }

/**
 * Explicit "keep this" overrides set by the person or a caregiver. Locked fields are hard
 * constraints; `null` means free.
 */
data class Locks(
    val hudRows: Int? = null,
    val hudFollowsTextSize: Boolean? = null,
    val columns: Int? = null,
    val style: ButtonStyle? = null,
    val illustration: Boolean? = null,
    val textLevel: TextLevel? = null,
) {
    fun allows(c: Config): Boolean =
        (hudRows == null || hudRows == c.hudRows) &&
            (hudFollowsTextSize == null || hudFollowsTextSize == c.hudFollowsTextSize) &&
            (columns == null || columns == c.columns) &&
            (style == null || style == c.style) &&
            (illustration == null || illustration == c.illustration) &&
            (textLevel == null || textLevel == c.textLevel)

    val isEmpty: Boolean get() = this == NONE

    companion object { val NONE = Locks() }
}

/** One presentation configuration of a screen: the solver's decision variables. */
data class Config(
    val hudRows: Int,
    /** Whether HUD labels use the in-app text size (the current app always caps them at the system scale). */
    val hudFollowsTextSize: Boolean,
    val columns: Int,
    val style: ButtonStyle,
    val illustration: Boolean,
    val textLevel: TextLevel,
    /** Content starts below a fixed reservation (current app) or below the measured HUD (Fit). */
    val measuredHudReservation: Boolean = true,
    /** Animations off. Always equals the need; recorded so the decision is complete. */
    val motionOff: Boolean = false,
    /** Top-bar buttons may grow to 220 dp instead of the current 150 dp cap. */
    val hudWideButtons: Boolean = false,
    /** Landscape: picture + controls on the left, list on the right (current); false = one scrolling column. */
    val landscapeSplit: Boolean = true,
) {
    /** Number of decision fields that differ from [other] (stability). */
    fun distance(other: Config): Int = listOf(
        hudRows != other.hudRows, hudFollowsTextSize != other.hudFollowsTextSize, columns != other.columns,
        style != other.style, illustration != other.illustration, textLevel != other.textLevel,
        hudWideButtons != other.hudWideButtons, landscapeSplit != other.landscapeSplit,
    ).count { it }
}

enum class ViolationKind(val severity: Int, val geometric: Boolean) {
    /** An essential control is pushed off screen, squeezed out of its row, or needs scrolling to reach. */
    ESSENTIAL_HIDDEN(6, true),
    /** The top bar covers activity content. */
    OVERLAP(5, true),
    /** A control is narrower or shorter than the person's minimum touch target. */
    TARGET_TOO_SMALL(4, true),
    /** A word does not fit on one line and would be broken mid-word. */
    WORD_BROKEN(3, true),
    /** A label needs more than two lines. */
    TOO_MANY_LINES(2, true),
    /** The place name does not fit in two lines and is cut off with an ellipsis. */
    TITLE_TRUNCATED(1, true),
    /** Text is smaller than a declared text-size need. */
    TEXT_NEED(1, false),
    /** Animation shown although reduced motion is on. */
    MOTION(6, false),
}

data class Violation(val kind: ViolationKind, val where: String, val detail: String)

enum class Status {
    /** Every hard constraint and lock holds; best by the preference order. */
    OPTIMAL,
    /** The text-size need could not be met on this screen; everything else holds. */
    RELAXED_TEXT_NEED,
    /** A lock conflicts with the other needs on this screen; the lock was set aside for this screen only. */
    RELAXED_LOCKS,
    /** Nothing satisfies every need here: the least severe configuration is shown and the problems are listed. */
    BEST_EFFORT,
}

data class Explanation(val field: String, val chosen: String, val reason: String)

data class Decision(
    val config: Config,
    val status: Status,
    val violations: List<Violation>,
    val explanations: List<Explanation>,
    val candidatesEvaluated: Int,
)
