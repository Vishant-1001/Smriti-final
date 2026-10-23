package app.smriti.domain

import kotlinx.serialization.Serializable

@Serializable
enum class AppLanguage(val tag: String) { TAMIL("ta"), ENGLISH("en") }

@Serializable
enum class TextSize(val scale: Float) { NORMAL(1.0f), LARGE(1.2f), LARGER(1.4f) }

/**
 * Player-facing preferences. Narration (Guide/NPC speech) and music (radio) have fully independent
 * volume and mute so one can never silence or overpower the other by accident.
 */
@Serializable
data class Settings(
    val language: AppLanguage = AppLanguage.TAMIL,
    val textSize: TextSize = TextSize.LARGE,
    val narrationEnabled: Boolean = true,
    val narrationVolume: Float = 0.9f,
    val musicMuted: Boolean = false,
    val musicVolume: Float = 0.7f,
    val reducedMotion: Boolean = false,
    val alwaysShowNames: Boolean = false,
) {
    val effectiveMusicVolume: Float get() = if (musicMuted) 0f else musicVolume.coerceIn(0f, 1f)
    val effectiveNarrationVolume: Float get() = if (narrationEnabled) narrationVolume.coerceIn(0f, 1f) else 0f
}
