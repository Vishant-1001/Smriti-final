package app.smriti.data

import app.smriti.domain.AppLanguage
import app.smriti.domain.NavState
import app.smriti.domain.RadioTrack
import app.smriti.domain.SaveData
import app.smriti.domain.SceneId
import app.smriti.domain.Settings
import app.smriti.domain.TextSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SaveCodecTest {

    @Test
    fun `round trip keeps everything`() {
        val data = SaveData(
            nav = NavState(listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.BEDROOM)),
            settings = Settings(language = AppLanguage.ENGLISH, textSize = TextSize.LARGER, musicMuted = true, reducedMotion = true),
            memoryBook = SaveData().memoryBook.addPersonal("p1", "Garden", "x.img"),
            playlist = SaveData().playlist + RadioTrack("t1", "My song", RadioTrack.Source.LocalFile("f.audio")),
            selectedTrackId = "t1",
        )
        assertEquals(data, SaveCodec.decode(SaveCodec.encode(data)))
    }

    @Test
    fun `empty or corrupt input falls back to a fresh save`() {
        assertEquals(SaveData(), SaveCodec.decode(""))
        assertEquals(SaveData(), SaveCodec.decode("{not json"))
        assertEquals(SaveData(), SaveCodec.decode("[1,2,3]"))
    }

    @Test
    fun `missing fields take defaults and unknown fields are ignored`() {
        val decoded = SaveCodec.decode("""{"schemaVersion":1,"settings":{"language":"ENGLISH"},"futureFeature":{"x":1}}""")
        assertEquals(AppLanguage.ENGLISH, decoded.settings.language)
        assertEquals(Settings().musicVolume, decoded.settings.musicVolume)
        assertEquals(NavState(), decoded.nav)
    }

    @Test
    fun `pre-release v0 saves migrate to the current schema`() {
        val decoded = SaveCodec.decode("""{"nav":{"stack":["TITLE","EXTERIOR"]}}""")
        assertEquals(SaveData.CURRENT_SCHEMA, decoded.schemaVersion)
        assertEquals(SceneId.EXTERIOR, decoded.nav.current)
    }

    @Test
    fun `unknown enum values and broken stacks are repaired`() {
        val decoded = SaveCodec.decode("""{"schemaVersion":1,"settings":{"language":"KLINGON"},"nav":{"stack":["BEDROOM"]}}""")
        assertEquals(Settings().language, decoded.settings.language)
        assertEquals(NavState(), decoded.nav)
    }

    @Test
    fun `selected track that no longer exists is cleared`() {
        val decoded = SaveCodec.decode("""{"schemaVersion":1,"selectedTrackId":"gone"}""")
        assertNull(decoded.selectedTrackId)
    }

    @Test
    fun `save never contains performance data`() {
        val text = SaveCodec.encode(SaveData()).lowercase()
        for (word in listOf("score", "error", "mistake", "duration", "time")) {
            assertFalse("save JSON unexpectedly contains '$word'", word in text)
        }
    }
}
