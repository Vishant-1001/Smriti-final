package app.smriti.testing

import android.content.Context
import app.smriti.AppGraph
import app.smriti.SmritiApp
import app.smriti.data.InMemorySaveRepository
import app.smriti.data.SaveRepository
import app.smriti.domain.SaveData
import org.junit.rules.ExternalResource

/** App wiring for Robolectric tests: in-memory save, fake voice and radio. */
class TestGraph(context: Context, initial: SaveData, voiceAvailable: Boolean) : AppGraph(context) {
    val memorySave = InMemorySaveRepository(initial)
    val voice = FakeVoice(voiceAvailable)
    val radio = FakeRadioPlayer()
    override val save: SaveRepository get() = memorySave
    override fun createVoice() = voice
    override fun createRadioPlayer() = radio
}

/** Installs a [TestGraph] before the activity is created. Use as the outermost rule. */
class TestGraphRule(private val initial: SaveData = SaveData(), private val voiceAvailable: Boolean = true) : ExternalResource() {
    lateinit var graph: TestGraph
        private set

    override fun before() {
        SmritiApp.graphFactory = { ctx -> TestGraph(ctx, initial, voiceAvailable).also { graph = it } }
    }

    override fun after() {
        SmritiApp.graphFactory = { AppGraph(it) }
    }
}
