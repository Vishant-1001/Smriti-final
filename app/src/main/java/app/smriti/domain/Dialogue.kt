package app.smriti.domain

import androidx.annotation.StringRes
import app.smriti.R

/** Who is talking. [portrait] is the sprite name prefix in assets/characters. */
enum class Speaker(val portrait: String, @StringRes val label: Int) {
    GUIDE("guide", R.string.guide_name),
    CONDUCTOR("conductor", R.string.speaker_conductor),
    VENDOR_PRODUCE("vendor_produce", R.string.speaker_vendor_produce),
    VENDOR_SPICE("vendor_spice", R.string.speaker_vendor_spice),
    VENDOR_FLOWER("vendor_flower", R.string.speaker_vendor_flower),
    VENDOR_CLOTH("vendor_cloth", R.string.speaker_vendor_cloth),
}

/**
 * One authored, deterministic line. Text lives in strings.xml (Tamil-first, English fallback).
 * [args] are inserted into the text (an item name as a string resource, or a number), so lines
 * stay localisable.
 */
data class DialogueLine(
    val id: String,
    val speaker: Speaker,
    @StringRes val text: Int,
    val args: List<LineArg> = emptyList(),
)

sealed interface LineArg {
    data class Res(@StringRes val id: Int) : LineArg
    data class Num(val value: Int) : LineArg
}

object GuideLines {
    val title = DialogueLine("guide_title", Speaker.GUIDE, R.string.guide_help_title)
    val exterior = DialogueLine("guide_exterior", Speaker.GUIDE, R.string.guide_help_exterior)
    val veranda = DialogueLine("guide_veranda", Speaker.GUIDE, R.string.guide_help_veranda)
    val bedroom = DialogueLine("guide_bedroom", Speaker.GUIDE, R.string.guide_help_bedroom)
    val memoryBook = DialogueLine("guide_memory_book", Speaker.GUIDE, R.string.guide_help_memory_book)
    val radio = DialogueLine("guide_radio", Speaker.GUIDE, R.string.guide_help_radio)
    val caregiver = DialogueLine("guide_caregiver", Speaker.GUIDE, R.string.guide_help_caregiver)
    val kitchen = DialogueLine("guide_kitchen", Speaker.GUIDE, R.string.guide_help_kitchen)
    val cooking = DialogueLine("guide_cooking", Speaker.GUIDE, R.string.guide_help_cooking)
    val dishes = DialogueLine("guide_dishes", Speaker.GUIDE, R.string.guide_help_dishes)
    val sewing = DialogueLine("guide_sewing", Speaker.GUIDE, R.string.guide_help_sewing)
    val stitch = DialogueLine("guide_stitch", Speaker.GUIDE, R.string.guide_help_stitch)
    val trip = DialogueLine("guide_trip", Speaker.GUIDE, R.string.guide_help_trip)
    val busStop = DialogueLine("guide_bus_stop", Speaker.GUIDE, R.string.guide_help_bus_stop)
    val bus = DialogueLine("guide_bus", Speaker.GUIDE, R.string.guide_help_bus)
    val market = DialogueLine("guide_market", Speaker.GUIDE, R.string.guide_help_market)
    val stall = DialogueLine("guide_stall", Speaker.GUIDE, R.string.guide_help_stall)

    fun helpFor(scene: SceneId): DialogueLine = when (scene) {
        SceneId.TITLE -> title
        SceneId.EXTERIOR -> exterior
        SceneId.VERANDA -> veranda
        SceneId.BEDROOM -> bedroom
        SceneId.MEMORY_BOOK -> memoryBook
        SceneId.RADIO -> radio
        SceneId.CAREGIVER -> caregiver
        SceneId.KITCHEN -> kitchen
        SceneId.COOKING -> cooking
        SceneId.DISHES -> dishes
        SceneId.SEWING -> sewing
        SceneId.STITCH -> stitch
        SceneId.TRIP -> trip
        SceneId.BUS_STOP -> busStop
        SceneId.BUS -> bus
        SceneId.MARKET -> market
        SceneId.STALL_PRODUCE, SceneId.STALL_SPICE, SceneId.STALL_FLOWER, SceneId.STALL_CLOTH -> stall
    }

    /** The bus number cue, spoken only when asked for. */
    val busNumber = DialogueLine("guide_bus_number", Speaker.GUIDE, R.string.guide_bus_number, listOf(LineArg.Num(TripState.SUGGESTED_BUS)))

    fun lookFor(item: ShopItem) = DialogueLine("hint_say_${item.id}", Speaker.GUIDE, R.string.hint_say_name, listOf(LineArg.Res(item.label), LineArg.Res(item.category)))
    fun whereToLook(item: ShopItem, region: Region) = DialogueLine(
        "hint_where_${item.id}", Speaker.GUIDE, R.string.hint_where,
        listOf(
            LineArg.Res(item.label),
            LineArg.Res(when (region) { Region.LEFT -> R.string.region_left; Region.MIDDLE -> R.string.region_middle; Region.RIGHT -> R.string.region_right }),
        ),
    )
    val anyCloth = DialogueLine("hint_any_cloth", Speaker.GUIDE, R.string.hint_any_cloth)
    val nothingHere = DialogueLine("hint_nothing_here", Speaker.GUIDE, R.string.hint_nothing_here)
    fun placed(item: ShopItem) = DialogueLine("hint_placed_${item.id}", Speaker.GUIDE, R.string.hint_placed, listOf(LineArg.Res(item.label)))

    val cookSteps = listOf(
        DialogueLine("cook_step_1", Speaker.GUIDE, R.string.cook_step_powder),
        DialogueLine("cook_step_2", Speaker.GUIDE, R.string.cook_step_water),
        DialogueLine("cook_step_3", Speaker.GUIDE, R.string.cook_step_decoction),
        DialogueLine("cook_step_4", Speaker.GUIDE, R.string.cook_step_milk),
        DialogueLine("cook_done", Speaker.GUIDE, R.string.cook_done),
    )
}

object NpcLines {
    val conductorTicket = DialogueLine("conductor_ticket", Speaker.CONDUCTOR, R.string.conductor_ticket)
    val conductorTicketHome = DialogueLine("conductor_ticket_home", Speaker.CONDUCTOR, R.string.conductor_ticket_home)
    val conductorThanks = DialogueLine("conductor_thanks", Speaker.CONDUCTOR, R.string.conductor_thanks)

    fun vendorGreeting(stall: StallId) = when (stall) {
        StallId.PRODUCE -> DialogueLine("vendor_produce_hello", Speaker.VENDOR_PRODUCE, R.string.vendor_produce_hello)
        StallId.SPICE -> DialogueLine("vendor_spice_hello", Speaker.VENDOR_SPICE, R.string.vendor_spice_hello)
        StallId.FLOWER -> DialogueLine("vendor_flower_hello", Speaker.VENDOR_FLOWER, R.string.vendor_flower_hello)
        StallId.CLOTH -> DialogueLine("vendor_cloth_hello", Speaker.VENDOR_CLOTH, R.string.vendor_cloth_hello)
    }
}
