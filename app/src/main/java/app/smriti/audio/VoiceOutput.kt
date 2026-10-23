package app.smriti.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import app.smriti.domain.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceAvailability { CHECKING, AVAILABLE, UNAVAILABLE }

/**
 * Spoken narration. Subtitles are always shown by the UI whether or not a voice is available, so
 * an unavailable voice never blocks anything.
 */
interface VoiceOutput {
    val speaking: StateFlow<Boolean>
    val availability: StateFlow<Map<AppLanguage, VoiceAvailability>>

    /** Starts speaking, replacing anything already being spoken. Returns false if no voice could speak. */
    fun speak(utteranceId: String, text: String, language: AppLanguage, volume: Float): Boolean
    fun stop()
    fun shutdown()
}

/**
 * Android on-device TextToSpeech. Voices that need a network connection, or whose data is not
 * installed, are treated as unavailable: SMRITI never sends text to a cloud speech service.
 * Whether a Tamil voice exists depends on the device's TTS engine and installed voice data.
 */
class TtsVoiceOutput(context: Context) : VoiceOutput {
    private val _speaking = MutableStateFlow(false)
    override val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    private val _availability = MutableStateFlow(AppLanguage.entries.associateWith { VoiceAvailability.CHECKING })
    override val availability: StateFlow<Map<AppLanguage, VoiceAvailability>> = _availability.asStateFlow()

    private val chosenVoices = mutableMapOf<AppLanguage, Voice?>()
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) inspectVoices() else _availability.value = AppLanguage.entries.associateWith { VoiceAvailability.UNAVAILABLE }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _speaking.value = true }
            override fun onDone(utteranceId: String?) { _speaking.value = false }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { _speaking.value = false }
            override fun onError(utteranceId: String?, errorCode: Int) { _speaking.value = false }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { _speaking.value = false }
        })
    }

    private fun inspectVoices() {
        val voices = runCatching { tts.voices.orEmpty() }.getOrDefault(emptySet())
        val result = AppLanguage.entries.associateWith { lang ->
            val voice = voices
                .filter { it.locale.language == lang.tag }
                .filterNot { it.isNetworkConnectionRequired }
                .filterNot { TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED in it.features.orEmpty() }
                .maxByOrNull { it.quality }
            chosenVoices[lang] = voice
            if (voice != null) VoiceAvailability.AVAILABLE else VoiceAvailability.UNAVAILABLE
        }
        _availability.value = result
    }

    override fun speak(utteranceId: String, text: String, language: AppLanguage, volume: Float): Boolean {
        if (!ready) return false
        val voice = chosenVoices[language] ?: return false
        tts.voice = voice
        tts.language = Locale(language.tag)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))
            putString(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS, "true")
        }
        // QUEUE_FLUSH: a new line always replaces the previous one, so speech never piles up.
        return tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId) == TextToSpeech.SUCCESS
    }

    override fun stop() {
        if (ready) tts.stop()
        _speaking.value = false
    }

    override fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
