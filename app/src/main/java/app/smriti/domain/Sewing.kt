package app.smriti.domain

import kotlinx.serialization.Serializable

/** Neutral things to make; no gendered assumptions. */
@Serializable
enum class SewKind { BAG, PILLOW, SHIRT }

@Serializable
data class Creation(val id: String, val kind: SewKind, val fabric: String) {
    val sprite: String get() = "sewing/${kind.name.lowercase()}_$fabric.webp"
}

@Serializable
enum class SewPhase { CHOOSE_FABRIC, CHOOSE_ITEM, STITCHING, FINISHED }

/**
 * Stitch & Create: choose a fabric, choose what to make, then a few large taps to stitch.
 * No timing, tracing or grading. The person may leave at any point; progress is kept.
 */
@Serializable
data class SewingState(
    val phase: SewPhase = SewPhase.CHOOSE_FABRIC,
    val fabric: String? = null,
    val kind: SewKind? = null,
    val stitches: Int = 0,
) {
    val progress: Float get() = stitches.toFloat() / STITCHES_NEEDED

    fun chooseFabric(fabricId: String): SewingState = copy(phase = SewPhase.CHOOSE_ITEM, fabric = fabricId, stitches = 0)

    fun chooseKind(k: SewKind): SewingState = if (fabric == null) this else copy(phase = SewPhase.STITCHING, kind = k, stitches = 0)

    fun stitch(): SewingState {
        if (phase != SewPhase.STITCHING) return this
        val next = stitches + 1
        return if (next >= STITCHES_NEEDED) copy(stitches = STITCHES_NEEDED, phase = SewPhase.FINISHED) else copy(stitches = next)
    }

    /** "Do it for me": finishes the stitching (or the whole item if a choice is still open). */
    fun doItForMe(defaultFabric: String): SewingState {
        val f = fabric ?: defaultFabric
        val k = kind ?: SewKind.BAG
        return copy(fabric = f, kind = k, stitches = STITCHES_NEEDED, phase = SewPhase.FINISHED)
    }

    fun undo(): SewingState = when (phase) {
        SewPhase.FINISHED -> copy(phase = SewPhase.STITCHING, stitches = STITCHES_NEEDED - 1)
        SewPhase.STITCHING -> if (stitches > 0) copy(stitches = stitches - 1) else copy(phase = SewPhase.CHOOSE_ITEM, kind = null)
        SewPhase.CHOOSE_ITEM -> copy(phase = SewPhase.CHOOSE_FABRIC, fabric = null)
        SewPhase.CHOOSE_FABRIC -> this
    }

    fun startOver(): SewingState = SewingState()

    /** Turns the finished piece into a kept creation. Returns null if it is not finished. */
    fun toCreation(id: String): Creation? =
        if (phase == SewPhase.FINISHED && fabric != null && kind != null) Creation(id, kind, fabric) else null

    companion object {
        const val STITCHES_NEEDED = 3
    }
}
