package app.smriti.domain

import kotlinx.serialization.Serializable

@Serializable
enum class TripDirection { TO_MARKET, TO_HOME }

@Serializable
enum class TripPhase { AT_STOP, ON_BUS, TICKET, PAID }

/**
 * A fictional bus ride. The bus number is a cue shown and spoken on request, never a test: any bus
 * the person taps goes to the market. Money is pretend, no payment service exists, and paying more
 * than the fare simply gives change.
 */
@Serializable
data class TripState(
    val direction: TripDirection = TripDirection.TO_MARKET,
    val phase: TripPhase = if (direction == TripDirection.TO_MARKET) TripPhase.AT_STOP else TripPhase.ON_BUS,
    val chosenBus: Int? = null,
    val tray: List<Int> = emptyList(),
    val change: Int = 0,
) {
    val fare: Int get() = FARE
    val trayTotal: Int get() = tray.sum()
    val stillNeeded: Int get() = (fare - trayTotal).coerceAtLeast(0)

    /** Any bus is fine. The choice is kept only so the ride can show the same number; it is not scored. */
    fun board(busNumber: Int): TripState = copy(chosenBus = busNumber, phase = TripPhase.ON_BUS)

    fun talkToConductor(): TripState = if (phase == TripPhase.ON_BUS) copy(phase = TripPhase.TICKET) else this

    fun addMoney(value: Int): TripState =
        if (phase != TripPhase.TICKET || value !in MONEY) this else copy(tray = tray + value)

    fun clearTray(): TripState = if (phase == TripPhase.TICKET) copy(tray = emptyList()) else this

    /** Pays if the tray covers the fare; otherwise nothing changes and the UI says how much more is needed. */
    fun pay(): TripState =
        if (phase == TripPhase.TICKET && trayTotal >= fare) copy(phase = TripPhase.PAID, change = trayTotal - fare) else this

    fun payForMe(): TripState =
        if (phase == TripPhase.ON_BUS || phase == TripPhase.TICKET) copy(phase = TripPhase.PAID, tray = listOf(fare), change = 0) else this

    companion object {
        const val SUGGESTED_BUS = 27
        /** Clearly different numbers (no look-alikes such as 16/61 or 13/31). */
        val BUS_OPTIONS = listOf(12, 27, 45, 63)
        const val FARE = 10
        val MONEY = listOf(1, 2, 5, 10, 20)

        fun busForHotspot(hotspotId: String): Int? =
            hotspotId.removePrefix("bus_").toIntOrNull()?.let { BUS_OPTIONS.getOrNull(it - 1) }
    }
}
