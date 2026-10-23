package app.smriti.fit

/** A way of choosing a configuration, for benchmarking against [FitSolver]. */
interface Policy {
    val name: String
    fun choose(screen: Screen, needs: Needs, device: Device): Config
}

/**
 * B1: the app's current hand-written rules, ported faithfully.
 * - Top bar: two rows below 600 dp wide (`SmritiRoot.TopBar`); labels capped at the system font
 *   scale, ignoring the in-app size (`Components.HudButton`).
 * - Content starts below a fixed reservation: 168 dp narrow, 90 dp wide (`SmritiRoot.GameScreen`).
 * - Buttons stacked (icon above) below 600 dp, or in landscape below 900 dp; one per row below
 *   380 dp, else two (`RadioScreen`).
 * - Picture always shown, and text at the size asked for.
 */
object CurrentRules : Policy {
    override val name = "B1 current rules"
    override fun choose(screen: Screen, needs: Needs, device: Device): Config {
        val w = device.widthDp
        val stacked = w < 600 || (device.landscape && w < 900)
        return Config(
            hudRows = if (w < 600) 2 else 1,
            hudFollowsTextSize = false,
            columns = if (w < 380) 1 else minOf(screen.panel.designColumns, screen.panel.controls.size.coerceAtLeast(1)),
            style = if (stacked) ButtonStyle.STACKED else ButtonStyle.INLINE,
            illustration = screen.panel.hasIllustration,
            textLevel = needs.textLevel,
            measuredHudReservation = false,
            motionOff = needs.reducedMotion,
            hudWideButtons = false,
            landscapeSplit = true,
        )
    }
}

/**
 * B2: what a careful engineer would build next. It starts from the current rules, reserves the
 * measured top-bar height, lets top-bar labels follow the in-app text size, then repairs
 * greedily with the same checker in the loop and a fixed precedence:
 *   top bar: 2 rows → 3 rows → wider buttons → labels back to system size (only if text is a preference);
 *   panel: stack icon above label → one fewer column (down to 1) → one scrolling column in landscape
 *   → hide picture → smaller text (only if text is a preference).
 * It has exactly the same knobs as the solver.
 * Each step is kept once taken (no backtracking).
 */
class GreedyRepair(private val layout: LayoutModel) : Policy {
    override val name = "B2 rules + greedy repair"

    override fun choose(screen: Screen, needs: Needs, device: Device): Config {
        var c = CurrentRules.choose(screen, needs, device)
            .copy(measuredHudReservation = true, hudFollowsTextSize = needs.textLevel != TextLevel.NORMAL)
        repeat(32) {
            val v = layout.evaluate(screen, needs, device, c).violations
            val hud = v.any { it.where.startsWith("hud") && it.kind != ViolationKind.TEXT_NEED }
            val panel = v.any { it.where.startsWith("panel") && it.kind != ViolationKind.TEXT_NEED }
            val next = when {
                hud && c.hudRows < 3 && screen.hud.extras.isNotEmpty() -> c.copy(hudRows = c.hudRows + 1)
                hud && c.hudRows < 2 -> c.copy(hudRows = 2)
                hud && !c.hudWideButtons -> c.copy(hudWideButtons = true)
                hud && c.hudFollowsTextSize && !needs.textIsNeed -> c.copy(hudFollowsTextSize = false)
                panel && c.style == ButtonStyle.INLINE -> c.copy(style = ButtonStyle.STACKED)
                panel && c.columns > 1 -> c.copy(columns = c.columns - 1)
                panel && device.landscape && c.landscapeSplit -> c.copy(landscapeSplit = false)
                panel && c.illustration -> c.copy(illustration = false)
                (hud || panel) && !needs.textIsNeed && c.textLevel.ordinal > 0 -> c.copy(textLevel = TextLevel.entries[c.textLevel.ordinal - 1])
                else -> null
            } ?: return c
            c = next
        }
        return c
    }
}

/** The solver as a [Policy] (no locks, no previous configuration). */
class FitPolicy(private val solver: FitSolver) : Policy {
    override val name = "Fit solver"
    override fun choose(screen: Screen, needs: Needs, device: Device): Config = solver.solve(screen, needs, device).config
}
