package app.smriti

import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioAttributes
import android.media.AudioManager
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on an emulator or device with the production wiring (DataStore save, MediaPlayer radio,
 * Android TextToSpeech). Each test first returns to the title screen.
 */
@RunWith(AndroidJUnit4::class)
class SliceInstrumentedTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun tap(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        val node = compose.onNodeWithTag(tag, useUnmergedTree = true)
        // Like a person would, scroll the control into view first (no-op outside scrolling lists).
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }

    private fun shown(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
    }

    /**
     * True while SMRITI holds a media player for music. Uses the platform's playback configurations
     * rather than isMusicActive, which stays false on an emulator started without an audio device.
     */
    private fun musicActive(): Boolean =
        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).activePlaybackConfigurations
            .any { it.audioAttributes.usage == AudioAttributes.USAGE_MEDIA && it.audioAttributes.contentType == AudioAttributes.CONTENT_TYPE_MUSIC }

    /** The save persists for the whole test process, so return to the title through the real UI. */
    private fun toTitle() {
        compose.waitForIdle()
        if (compose.onAllNodes(hasTestTag("start"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) {
            tap("hud_guide")
            tap("guide_end_session")
        }
        shown("start")
    }

    private fun waitFor(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (condition()) return true
            Thread.sleep(100)
        }
        return condition()
    }

    @Test
    fun fullSliceWithRealAudio() {
        toTitle()
        tap("start")
        shown("scene_exterior")
        tap("hotspot_front_door")
        shown("scene_veranda")
        tap("hotspot_bedroom_door")
        shown("scene_bedroom")
        tap("hotspot_radio")
        shown("scene_radio")
        assertFalse("radio must not autoplay", musicActive())
        tap("track_demo_tune_1")
        shown("radio_pause")
        assertTrue("a music player should be running", waitFor(5_000) { musicActive() })
        tap("radio_stop")
        shown("radio_play")
        assertTrue("stop should release the music player", waitFor(3_000) { !musicActive() })
        tap("hud_back")
        tap("hotspot_memory_book")
        shown("sample_badge")
        tap("memory_next")
        tap("hud_home")
        shown("scene_veranda")
    }

    @Test
    fun sceneSurvivesRotationAndRecreation() {
        toTitle()
        tap("start")
        tap("hotspot_front_door")
        tap("hotspot_bedroom_door")
        shown("scene_bedroom")
        compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        compose.waitForIdle()
        shown("scene_bedroom")
        compose.activityRule.scenario.recreate()
        shown("scene_bedroom")
        compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    @Test
    fun marketRoundTripByBusThenSewWithBoughtCloth() {
        toTitle()
        tap("start")
        tap("hotspot_front_door")
        tap("hotspot_front_door")
        tap("destination_market")
        shown("scene_trip")
        tap("trip_bus")
        shown("bus_cue_number")
        tap("hotspot_bus_4") // not the suggested number: boarding still works
        shown("scene_bus")
        tap("bus_talk")
        tap("dialogue_ok")
        tap("money_20")
        tap("pay")
        shown("pay_change")
        tap("bus_arrive")
        shown("scene_market")
        tap("hotspot_stall_cloth")
        tap("hotspot_cloth_indigo_dots")
        tap("item_add")
        tap("hud_back")
        tap("market_go_home")
        tap("trip_direct")
        shown("scene_veranda")
        tap("hotspot_sewing_corner")
        tap("hotspot_sewing_machine")
        tap("sew_start_over") // earlier sessions may have left a piece in progress (progress is kept on purpose)
        tap("fabric_cloth_indigo_dots")
        tap("kind_pillow")
        tap("sew_do_it")
        tap("sew_keep")
        tap("hud_back")
        shown("scene_sewing")
    }

    @Test
    fun guideHelpAlwaysShowsSubtitle() {
        toTitle()
        tap("start")
        tap("hud_guide")
        tap("guide_help_here")
        shown("dialogue_text")
        tap("dialogue_ok")
    }
}
