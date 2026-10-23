package app.smriti.fit

/**
 * Exhaustive, deterministic solver: of all presentation configurations of a screen, return the
 * best one that satisfies every hard constraint (no hidden essential control, no overlap, touch
 * targets ≥ the person's minimum, labels in ≤ 2 lines with no broken words, the text-size need if
 * declared, reduced motion, and the person's locks).
 *
 * Preferences are ordered, not weighted, so there are no weights to tune:
 *  1. text closest to the size the person asked for;
 *  2. keep the illustration;
 *  3. fewest changes from the previously accepted configuration (stability);
 *  4. fewest rows (top bar + buttons);
 *  5. icon beside the label (the default design) over icon above it;
 *  6. fixed enumeration order (deterministic tie-break).
 *
 * If nothing is feasible, constraints are relaxed in a predeclared order (text-size need, then
 * locks) and, failing that, the least severe configuration is returned together with its
 * remaining violations. Nothing is ever silently clipped.
 */
class FitSolver(measurer: TextMeasurer) {
    val layout = LayoutModel(measurer)

    fun candidates(screen: Screen, needs: Needs, device: Device): List<Config> {
        val n = screen.panel.controls.size.coerceAtLeast(1)
        val out = ArrayList<Config>()
        // Never larger than asked: the person's size is the ceiling, smaller sizes are fallbacks.
        val levels = TextLevel.entries.filter { it <= needs.textLevel }.reversed()
        for (level in levels) for (follows in listOf(true, false)) for (rows in 1..3) for (wide in listOf(false, true))
            for (split in listOf(true, false)) for (cols in minOf(n, 4) downTo 1) for (style in ButtonStyle.entries) for (ill in listOf(true, false)) {
                if (!screen.panel.hasIllustration && ill) continue
                if (rows == 3 && screen.hud.extras.isEmpty()) continue
                if (!split && !device.landscape) continue // portrait is always one scrolling column
                if (follows && level == TextLevel.NORMAL) continue // identical to not following
                out += Config(
                    rows, follows, cols, style, ill, level, measuredHudReservation = true, motionOff = needs.reducedMotion,
                    hudWideButtons = wide, landscapeSplit = split,
                )
            }
        return out
    }

    private data class Scored(val config: Config, val eval: Evaluation, val key: List<Int>)

    private fun score(screen: Screen, needs: Needs, device: Device, previous: Config?, cs: List<Config>): List<Scored> =
        cs.mapIndexed { i, c ->
            val e = layout.evaluate(screen, needs, device, c)
            val lost = if (screen.panel.hasIllustration && !e.illustrationShown) 1 else 0
            val key = listOf(
                e.textDeficit, lost, previous?.distance(c) ?: 0, c.hudRows + e.panelRows,
                if (c.style == ButtonStyle.STACKED) 1 else 0, i,
            )
            Scored(c, e, key)
        }

    fun solve(screen: Screen, needs: Needs, device: Device, locks: Locks = Locks.NONE, previous: Config? = null): Decision {
        val all = score(screen, needs, device, previous, candidates(screen, needs, device))
        val byKey = compareBy<Scored>({ it.key[0] }, { it.key[1] }, { it.key[2] }, { it.key[3] }, { it.key[4] }, { it.key[5] })
        fun best(filter: (Scored) -> Boolean) = all.filter(filter).minWithOrNull(byKey)

        val stages = listOf<Pair<Status, (Scored) -> Boolean>>(
            Status.OPTIMAL to { s -> s.eval.violations.isEmpty() && locks.allows(s.config) },
            Status.RELAXED_TEXT_NEED to { s -> s.eval.hardExcept(ViolationKind.TEXT_NEED).isEmpty() && locks.allows(s.config) },
            Status.RELAXED_LOCKS to { s -> s.eval.violations.isEmpty() },
            Status.RELAXED_LOCKS to { s -> s.eval.hardExcept(ViolationKind.TEXT_NEED).isEmpty() },
        )
        for ((status, ok) in stages) {
            val s = best(ok) ?: continue
            return Decision(s.config, status, s.eval.violations, explain(screen, needs, device, locks, previous, s, status), all.size)
        }
        // Best effort: minimise violations by severity (most severe first), then the usual preferences.
        val severity = compareBy<Scored>(*ViolationKind.entries.map { it.severity }.distinct().sortedDescending().map { sev ->
            { s: Scored -> s.eval.violations.count { it.kind.severity == sev } }
        }.toTypedArray())
        val s = all.minWithOrNull(severity.then(byKey))!!
        return Decision(s.config, Status.BEST_EFFORT, s.eval.violations, explain(screen, needs, device, locks, previous, s, Status.BEST_EFFORT), all.size)
    }

    /** The design as drawn, at the size the person asked for: what every change is explained against. */
    fun asAsked(screen: Screen, needs: Needs): Config = Config(
        hudRows = 1, hudFollowsTextSize = needs.textLevel != TextLevel.NORMAL, columns = minOf(screen.panel.designColumns, screen.panel.controls.size.coerceAtLeast(1)),
        style = ButtonStyle.INLINE, illustration = screen.panel.hasIllustration, textLevel = needs.textLevel,
        measuredHudReservation = true, motionOff = needs.reducedMotion, hudWideButtons = false, landscapeSplit = true,
    )

    private fun explain(screen: Screen, needs: Needs, device: Device, locks: Locks, previous: Config?, s: Scored, status: Status): List<Explanation> {
        val out = mutableListOf<Explanation>()
        when (status) {
            Status.RELAXED_TEXT_NEED -> out += Explanation("status", status.name, "${needs.textLevel} text cannot fit every control on this screen; the largest size that fits is used")
            Status.RELAXED_LOCKS -> {
                out += Explanation("status", status.name, "a kept setting cannot fit this screen; it still applies elsewhere")
                if (s.eval.violations.any { it.kind == ViolationKind.TEXT_NEED }) {
                    out += Explanation("status", status.name, "${needs.textLevel} text also cannot fit every control here; the largest size that fits is used")
                }
            }
            Status.BEST_EFFORT -> out += Explanation("status", status.name, "no layout meets every need here: ${s.eval.violations.size} problem(s) remain, least severe shown")
            Status.OPTIMAL -> Unit
        }
        if (needs.reducedMotion) out += Explanation("motion", "off", "reduced motion is on")
        val chosen = s.config
        val base = asAsked(screen, needs)
        val fields = listOf<Triple<String, (Config) -> Any, (Config, Config) -> Config>>(
            Triple("textLevel", { c -> c.textLevel }, { c, b -> c.copy(textLevel = b.textLevel) }),
            Triple("hudFollowsTextSize", { c -> c.hudFollowsTextSize }, { c, b -> c.copy(hudFollowsTextSize = b.hudFollowsTextSize) }),
            Triple("hudRows", { c -> c.hudRows }, { c, b -> c.copy(hudRows = b.hudRows) }),
            Triple("columns", { c -> c.columns }, { c, b -> c.copy(columns = b.columns) }),
            Triple("style", { c -> c.style }, { c, b -> c.copy(style = b.style) }),
            Triple("illustration", { c -> c.illustration }, { c, b -> c.copy(illustration = b.illustration) }),
            Triple("hudWideButtons", { c -> c.hudWideButtons }, { c, b -> c.copy(hudWideButtons = b.hudWideButtons) }),
            Triple("landscapeSplit", { c -> c.landscapeSplit }, { c, b -> c.copy(landscapeSplit = b.landscapeSplit) }),
        )
        for ((name, get, reset) in fields) {
            if (get(chosen) == get(base)) continue
            // hudFollowsTextSize is irrelevant at NORMAL text: both values draw the same screen.
            if (name == "hudFollowsTextSize" && chosen.textLevel == TextLevel.NORMAL) continue
            if (name == "landscapeSplit" && !device.landscape) continue
            val alt = reset(chosen, base)
            val altEval = layout.evaluate(screen, needs, device, alt)
            val newProblems = altEval.violations.filter { it !in s.eval.violations }
            val reason = when {
                !locks.allows(alt) && locks.allows(chosen) -> "kept as you set it"
                newProblems.isNotEmpty() -> "${get(base)} would cause: " + newProblems.first().let { "${it.kind.label()} at ${it.where.substringAfter(':')} (${it.detail})" }
                previous != null && get(previous) == get(chosen) -> "kept from the previous layout to avoid changes"
                else -> "${get(base)} also fits but ranks lower (${rankReason(s.eval, altEval)})"
            }
            out += Explanation(name, get(chosen).toString(), reason)
        }
        return out
    }

    private fun rankReason(chosen: Evaluation, alt: Evaluation): String = when {
        alt.textDeficit > chosen.textDeficit -> "smaller text"
        !alt.illustrationShown && chosen.illustrationShown -> "hides the picture"
        alt.panelRows > chosen.panelRows -> "more rows"
        else -> "a less compact layout"
    }
}

fun ViolationKind.label(): String = when (this) {
    ViolationKind.ESSENTIAL_HIDDEN -> "a control off screen"
    ViolationKind.OVERLAP -> "the top bar covering content"
    ViolationKind.TARGET_TOO_SMALL -> "a button smaller than the touch minimum"
    ViolationKind.WORD_BROKEN -> "a word broken across lines"
    ViolationKind.TOO_MANY_LINES -> "a label over two lines"
    ViolationKind.TITLE_TRUNCATED -> "the place name cut off"
    ViolationKind.TEXT_NEED -> "text smaller than needed"
    ViolationKind.MOTION -> "animation"
}
