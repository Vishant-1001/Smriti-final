package app.smriti.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.smriti.MainActivity
import app.smriti.domain.AppLanguage
import app.smriti.domain.SaveData
import app.smriti.domain.SceneId
import app.smriti.domain.Settings
import app.smriti.testing.TestGraphRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Drives the real Compose UI (Robolectric, JVM) through the M1 slice. Not a device test. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h914dp-port-xhdpi")
class GameFlowUiTest {
    private val graphRule = TestGraphRule(SaveData(settings = Settings(language = AppLanguage.ENGLISH)))
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(graphRule).around(compose)

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).performClick()
        compose.waitForIdle()
    }

    private fun assertShown(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()

    private val saved get() = graphRule.graph.memorySave.current

    @Test
    fun exteriorToVerandaToBedroomAndBack() {
        tap("start")
        assertShown("scene_exterior")
        tap("hotspot_front_door")
        assertShown("scene_veranda")
        tap("hotspot_bedroom_door")
        assertShown("scene_bedroom")
        assertEquals(SceneId.BEDROOM, saved.nav.current)
        tap("hud_back")
        assertShown("scene_veranda")
        assertEquals(SceneId.VERANDA, saved.nav.current)
    }

    @Test
    fun globalControlsAreOnEveryGameplayScreen() {
        tap("start")
        for (step in listOf<String?>(null, "hotspot_front_door", "hotspot_bedroom_door", "hotspot_memory_book")) {
            if (step != null) tap(step)
            assertShown("hud_back")
            assertShown("hud_settings")
            assertShown("hud_guide")
        }
        tap("hud_back")
        tap("hotspot_radio")
        assertShown("hud_back")
        assertShown("hud_guide")
        assertShown("radio_stop")
    }

    @Test
    fun placesListIsAnAlternativeToTappingThePicture() {
        tap("start")
        tap("hotspot_front_door")
        tap("hud_guide")
        assertShown("guide_panel")
        tap("place_bedroom_door")
        assertShown("scene_bedroom")
    }

    @Test
    fun radioDoesNotAutoplayAndStopWorks() {
        tap("start")
        tap("hotspot_front_door")
        tap("hotspot_bedroom_door")
        tap("hotspot_radio")
        assertShown("scene_radio")
        assertTrue(graphRule.graph.radio.log.none { it.startsWith("play") })
        tap("track_demo_tune_1")
        assertEquals(listOf("play:demo_tune_1"), graphRule.graph.radio.log)
        tap("hud_back")
        assertShown("mini_player")
        tap("mini_stop")
        assertEquals("stop", graphRule.graph.radio.log.last())
        compose.onNodeWithTag("mini_player").assertDoesNotExist()
    }

    @Test
    fun memoryBookShowsSampleAndTurnsPages() {
        tap("start")
        tap("hotspot_front_door")
        tap("hotspot_bedroom_door")
        tap("hotspot_memory_book")
        assertShown("sample_badge")
        tap("memory_next")
        assertEquals(1, saved.memoryBook.currentIndex)
    }

    @Test
    fun frontDoorChooserStayHomeAndGoOutside() {
        tap("start")
        tap("hotspot_front_door")
        tap("hotspot_front_door")
        assertShown("destination_chooser")
        tap("destination_stay")
        assertShown("scene_veranda")
        tap("hotspot_front_door")
        tap("destination_outside")
        assertShown("scene_exterior")
    }

    @Test
    fun helpShowsSubtitleAndStopVoiceStopsIt() {
        tap("start")
        tap("hud_guide")
        tap("guide_help_here")
        assertShown("dialogue_text")
        assertShown("stop_voice")
        val stops = graphRule.graph.voice.stopCount
        tap("stop_voice")
        assertTrue(graphRule.graph.voice.stopCount > stops)
        compose.onNodeWithTag("stop_voice").assertDoesNotExist()
        assertShown("dialogue_text")
    }

    @Test
    fun kitchenIsHonestlyNotReady() {
        tap("start")
        tap("hotspot_front_door")
        tap("hotspot_kitchen_door")
        assertShown("dialogue_panel")
        assertShown("scene_veranda")
        tap("dialogue_ok")
        compose.onNodeWithTag("dialogue_panel").assertDoesNotExist()
    }

    @Test
    fun caregiverToolsAreBehindADeliberateStep() {
        tap("hud_settings")
        tap("open_caregiver")
        assertShown("caregiver_continue")
        compose.onNodeWithTag("caregiver_caption").assertDoesNotExist()
        tap("caregiver_continue")
        assertShown("caregiver_caption")
    }

    @Test
    fun endSessionFromGuideReturnsToTitleAndStopsMusic() {
        tap("start")
        tap("hotspot_front_door")
        tap("hotspot_bedroom_door")
        tap("hotspot_radio")
        tap("track_demo_tune_2")
        tap("hud_guide")
        tap("guide_end_session")
        assertShown("start")
        assertEquals("stop", graphRule.graph.radio.log.last())
        assertEquals(listOf(SceneId.TITLE), saved.nav.stack)
    }
}
