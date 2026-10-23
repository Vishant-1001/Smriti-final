package app.smriti

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import app.smriti.audio.MediaPlayerRadioPlayer
import app.smriti.audio.RadioPlayer
import app.smriti.audio.TtsVoiceOutput
import app.smriti.audio.VoiceOutput
import app.smriti.data.AssetSceneLayouts
import app.smriti.data.DataStoreSaveRepository
import app.smriti.data.MediaStorage
import app.smriti.data.SaveRepository
import app.smriti.data.SceneLayoutSource
import app.smriti.domain.AppLanguage
import app.smriti.ui.MediaImporter
import app.smriti.ui.StringResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.Locale

/** Hand-wired dependencies. Tests replace [SmritiApp.graphFactory] before the activity starts. */
open class AppGraph(val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    open val save: SaveRepository by lazy { DataStoreSaveRepository(context, appScope) }
    val mediaStorage: MediaStorage by lazy { MediaStorage(context) }
    open val layouts: SceneLayoutSource by lazy { AssetSceneLayouts(context.assets) }
    open fun createVoice(): VoiceOutput = TtsVoiceOutput(context)
    open fun createRadioPlayer(): RadioPlayer = MediaPlayerRadioPlayer(context) { mediaStorage.radioFile(it) }
    open val mediaImporter: MediaImporter by lazy {
        object : MediaImporter {
            override fun deletePhoto(fileName: String) = mediaStorage.deletePhoto(fileName)
            override fun deleteAudio(fileName: String) = mediaStorage.deleteAudio(fileName)
        }
    }
    val strings: StringResolver by lazy {
        StringResolver { id, language -> localizedContext(context, language).getString(id) }
    }
}

fun localizedContext(base: Context, language: AppLanguage): Context {
    val config = Configuration(base.resources.configuration)
    config.setLocale(Locale(language.tag))
    return base.createConfigurationContext(config)
}

/**
 * The activity context with only its resources swapped for the chosen language. Because it still
 * wraps the activity, lookups such as the activity-result registry (used by the photo and audio
 * pickers) keep working.
 */
class LocalizedActivityContext(base: Context, language: AppLanguage) : ContextWrapper(base) {
    private val localizedResources: Resources = localizedContext(base, language).resources
    override fun getResources(): Resources = localizedResources
}

class SmritiApp : Application() {
    val graph: AppGraph by lazy { graphFactory(this) }

    companion object {
        var graphFactory: (Context) -> AppGraph = { AppGraph(it) }
    }
}
