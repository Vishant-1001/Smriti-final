package app.smriti.screenshots

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.smriti.AppGraph
import app.smriti.MainActivity
import app.smriti.SmritiApp
import app.smriti.domain.AppLanguage
import app.smriti.domain.NavState
import app.smriti.domain.SaveData
import app.smriti.domain.SceneId
import app.smriti.domain.Settings
import app.smriti.domain.TextSize
import app.smriti.testing.TestGraph
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders real app screens on the JVM (Robolectric native graphics) and records them with
 * Roborazzi into docs/screenshots/roborazzi/<device>/. These are JVM renders, not device captures.
 * Record with: ./gradlew recordRoborazziDebug
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class SceneScreenshots(private val device: String) {
    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
        SmritiApp.graphFactory = { AppGraph(it) }
    }

    private fun stackTo(scene: SceneId): List<SceneId> = when (scene) {
        SceneId.TITLE -> listOf(SceneId.TITLE)
        SceneId.EXTERIOR -> listOf(SceneId.TITLE, SceneId.EXTERIOR)
        SceneId.VERANDA -> listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA)
        SceneId.BEDROOM -> listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.BEDROOM)
        SceneId.CAREGIVER -> listOf(SceneId.TITLE, SceneId.CAREGIVER)
        else -> listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.BEDROOM, scene)
    }

    private fun open(scene: SceneId, settings: Settings = Settings(), voice: Boolean = false) {
        val save = SaveData(nav = NavState(stackTo(scene)), settings = settings)
        SmritiApp.graphFactory = { TestGraph(it, save, voiceAvailable = voice) }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        compose.onRoot().captureRoboImage("../docs/screenshots/roborazzi/$device/$name.png")
    }

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).performClick()
        compose.waitForIdle()
    }

    @Test fun title() { open(SceneId.TITLE); shot("01_title_ta") }
    @Test fun exterior() { open(SceneId.EXTERIOR); shot("02_exterior_ta") }
    @Test fun veranda() { open(SceneId.VERANDA); shot("03_veranda_ta") }
    @Test fun bedroom() { open(SceneId.BEDROOM); shot("04_bedroom_ta") }
    @Test fun memoryBook() { open(SceneId.MEMORY_BOOK); shot("05_memory_book_ta") }
    @Test fun radio() { open(SceneId.RADIO); shot("06_radio_ta") }

    @Test
    fun verandaEnglishWithNames() {
        open(SceneId.VERANDA, Settings(language = AppLanguage.ENGLISH, alwaysShowNames = true))
        shot("07_veranda_en_names")
    }

    @Test
    fun guidePanelAndSubtitle() {
        open(SceneId.BEDROOM, Settings(language = AppLanguage.ENGLISH), voice = true)
        tap("hud_guide")
        shot("08_guide_panel_en")
        tap("guide_help_here")
        shot("09_subtitle_speaking_en")
    }

    @Test
    fun settingsLargerText() {
        open(SceneId.VERANDA, Settings(textSize = TextSize.LARGER))
        tap("hud_settings")
        shot("10_settings_larger_text_ta")
    }

    @Test
    fun destinationChooser() {
        open(SceneId.VERANDA, Settings(language = AppLanguage.ENGLISH))
        tap("hotspot_front_door")
        shot("11_destination_chooser_en")
    }

    @Test
    fun radioPlaying() {
        open(SceneId.RADIO, Settings(language = AppLanguage.ENGLISH))
        tap("track_demo_tune_1")
        shot("12_radio_playing_en")
        tap("hud_back")
        shot("13_bedroom_mini_player_en")
    }
}

@Config(sdk = [34], qualifiers = "w411dp-h914dp-port-xhdpi")
class PhonePortraitScreenshots : SceneScreenshots("phone_portrait_411x914dp")

@Config(sdk = [34], qualifiers = "w320dp-h640dp-port-xhdpi")
class NarrowPhoneScreenshots : SceneScreenshots("narrow_phone_320x640dp")

@Config(sdk = [34], qualifiers = "w914dp-h411dp-land-xhdpi")
class PhoneLandscapeScreenshots : SceneScreenshots("phone_landscape_914x411dp")

@Config(sdk = [34], qualifiers = "w1280dp-h800dp-land-hdpi")
class TabletLandscapeScreenshots : SceneScreenshots("tablet_landscape_1280x800dp")
