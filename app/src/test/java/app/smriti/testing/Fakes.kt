package app.smriti.testing

import app.smriti.audio.RadioPlayer
import app.smriti.audio.RadioPlayerListener
import app.smriti.audio.VoiceAvailability
import app.smriti.audio.VoiceOutput
import app.smriti.domain.AppLanguage
import app.smriti.domain.RadioTrack
import app.smriti.ui.MediaImporter
import app.smriti.ui.StringResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeVoice(available: Boolean = true) : VoiceOutput {
    val spoken = mutableListOf<Pair<String, AppLanguage>>()
    var stopCount = 0
    override val speaking = MutableStateFlow(false)
    override val availability: StateFlow<Map<AppLanguage, VoiceAvailability>> = MutableStateFlow(
        AppLanguage.entries.associateWith { if (available) VoiceAvailability.AVAILABLE else VoiceAvailability.UNAVAILABLE }
    )
    private val canSpeak = available

    override fun speak(utteranceId: String, text: String, language: AppLanguage, volume: Float): Boolean {
        if (!canSpeak) return false
        spoken += utteranceId to language
        speaking.value = true
        return true
    }

    fun finish() { speaking.value = false }

    override fun stop() {
        stopCount++
        speaking.value = false
    }

    override fun shutdown() = Unit
}

class FakeRadioPlayer : RadioPlayer {
    val log = mutableListOf<String>()
    var lastVolume = -1f
    override var listener: RadioPlayerListener? = null
    override fun play(track: RadioTrack) { log += "play:${track.id}" }
    override fun pause() { log += "pause" }
    override fun resume() { log += "resume" }
    override fun stop() { log += "stop" }
    override fun setVolume(volume: Float) { lastVolume = volume }
    override fun release() { log += "release" }
}

class FakeMedia : MediaImporter {
    val deleted = mutableListOf<String>()
    override fun deletePhoto(fileName: String) { deleted += "photo:$fileName" }
    override fun deleteAudio(fileName: String) { deleted += "audio:$fileName" }
}

val fakeStrings = StringResolver { id, lang -> "${lang.tag}:$id" }
