package app.smriti.domain

import androidx.annotation.StringRes
import app.smriti.R

enum class Speaker { GUIDE }

/** One authored, deterministic line. Text lives in strings.xml (Tamil-first, English fallback). */
data class DialogueLine(val id: String, val speaker: Speaker, @StringRes val text: Int)

object GuideLines {
    val title = DialogueLine("guide_title", Speaker.GUIDE, R.string.guide_help_title)
    val exterior = DialogueLine("guide_exterior", Speaker.GUIDE, R.string.guide_help_exterior)
    val veranda = DialogueLine("guide_veranda", Speaker.GUIDE, R.string.guide_help_veranda)
    val bedroom = DialogueLine("guide_bedroom", Speaker.GUIDE, R.string.guide_help_bedroom)
    val memoryBook = DialogueLine("guide_memory_book", Speaker.GUIDE, R.string.guide_help_memory_book)
    val radio = DialogueLine("guide_radio", Speaker.GUIDE, R.string.guide_help_radio)
    val caregiver = DialogueLine("guide_caregiver", Speaker.GUIDE, R.string.guide_help_caregiver)
    val kitchenNotReady = DialogueLine("kitchen_not_ready", Speaker.GUIDE, R.string.guide_kitchen_not_ready)
    val sewingNotReady = DialogueLine("sewing_not_ready", Speaker.GUIDE, R.string.guide_sewing_not_ready)

    fun helpFor(scene: SceneId): DialogueLine = when (scene) {
        SceneId.TITLE -> title
        SceneId.EXTERIOR -> exterior
        SceneId.VERANDA -> veranda
        SceneId.BEDROOM -> bedroom
        SceneId.MEMORY_BOOK -> memoryBook
        SceneId.RADIO -> radio
        SceneId.CAREGIVER -> caregiver
    }
}
