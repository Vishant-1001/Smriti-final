package app.smriti.fit

/**
 * Everything Fit needs to reproduce a decision: the explicit needs, the person's locks and the last
 * accepted configuration per screen. No history and no behaviour are stored.
 *
 * Encoded as versioned `key=value` lines. Decoding is fail-closed: an unknown version or any
 * malformed field returns `null`, and the caller falls back to defaults (the current rules).
 */
data class FitState(
    val needs: Needs = Needs(),
    val locks: Locks = Locks.NONE,
    val accepted: Map<String, Config> = emptyMap(),
) {
    /** Re-solve [screen] from this state: the same state and device always give the same decision. */
    fun replay(solver: FitSolver, screen: Screen, device: Device): Decision =
        solver.solve(screen, needs, device, locks, accepted[screen.id])

    fun accept(screenId: String, config: Config): FitState = copy(accepted = accepted + (screenId to config))

    companion object {
        const val VERSION = 1

        fun encode(s: FitState): String = buildString {
            appendLine("fit.version=$VERSION")
            with(s.needs) {
                appendLine("needs.language=$language")
                appendLine("needs.textLevel=$textLevel")
                appendLine("needs.textIsNeed=$textIsNeed")
                appendLine("needs.minTargetDp=$minTargetDp")
                appendLine("needs.reducedMotion=$reducedMotion")
            }
            with(s.locks) {
                hudRows?.let { appendLine("lock.hudRows=$it") }
                hudFollowsTextSize?.let { appendLine("lock.hudFollowsTextSize=$it") }
                columns?.let { appendLine("lock.columns=$it") }
                style?.let { appendLine("lock.style=$it") }
                illustration?.let { appendLine("lock.illustration=$it") }
                textLevel?.let { appendLine("lock.textLevel=$it") }
            }
            for ((id, c) in s.accepted.toSortedMap()) {
                require(id.matches(Regex("[a-z0-9_]+"))) { "screen id '$id'" }
                appendLine("accepted.$id=${c.hudRows},${c.hudFollowsTextSize},${c.columns},${c.style},${c.illustration},${c.textLevel},${c.measuredHudReservation},${c.motionOff},${c.hudWideButtons},${c.landscapeSplit}")
            }
        }

        fun decode(text: String): FitState? = runCatching {
            val kv = text.lines().filter { it.isNotBlank() }.associate { line ->
                val i = line.indexOf('=')
                require(i > 0)
                line.substring(0, i) to line.substring(i + 1)
            }
            require(kv["fit.version"] == VERSION.toString())
            fun bool(v: String) = when (v) { "true" -> true; "false" -> false; else -> error("bool $v") }
            val needs = Needs(
                language = Lang.valueOf(kv.getValue("needs.language")),
                textLevel = TextLevel.valueOf(kv.getValue("needs.textLevel")),
                textIsNeed = bool(kv.getValue("needs.textIsNeed")),
                minTargetDp = kv.getValue("needs.minTargetDp").toInt().also { require(it in 24..120) },
                reducedMotion = bool(kv.getValue("needs.reducedMotion")),
            )
            val locks = Locks(
                hudRows = kv["lock.hudRows"]?.toInt()?.also { require(it in 1..3) },
                hudFollowsTextSize = kv["lock.hudFollowsTextSize"]?.let(::bool),
                columns = kv["lock.columns"]?.toInt()?.also { require(it in 1..4) },
                style = kv["lock.style"]?.let { ButtonStyle.valueOf(it) },
                illustration = kv["lock.illustration"]?.let(::bool),
                textLevel = kv["lock.textLevel"]?.let { TextLevel.valueOf(it) },
            )
            val known = setOf("fit.version") +
                listOf("language", "textLevel", "textIsNeed", "minTargetDp", "reducedMotion").map { "needs.$it" } +
                listOf("hudRows", "hudFollowsTextSize", "columns", "style", "illustration", "textLevel").map { "lock.$it" }
            val accepted = kv.filterKeys { it.startsWith("accepted.") }.map { (k, v) ->
                val f = v.split(',')
                require(f.size == 10)
                k.removePrefix("accepted.") to Config(
                    f[0].toInt().also { require(it in 1..3) }, bool(f[1]), f[2].toInt().also { require(it in 1..4) },
                    ButtonStyle.valueOf(f[3]), bool(f[4]), TextLevel.valueOf(f[5]), bool(f[6]), bool(f[7]), bool(f[8]), bool(f[9]),
                )
            }.toMap()
            require(kv.keys.all { it in known || it.startsWith("accepted.") }) { "unknown key" }
            FitState(needs, locks, accepted)
        }.getOrNull()
    }
}
