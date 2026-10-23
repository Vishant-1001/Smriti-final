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
import app.smriti.domain.CookingState
import app.smriti.domain.Creation
import app.smriti.domain.DishwashState
import app.smriti.domain.SewKind
import app.smriti.domain.SewingState
import app.smriti.domain.ShoppingState
import app.smriti.domain.TripState
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
        SceneId.KITCHEN, SceneId.SEWING, SceneId.TRIP, SceneId.MARKET -> listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, scene)
        SceneId.COOKING, SceneId.DISHES -> listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.KITCHEN, scene)
        SceneId.STITCH -> listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.SEWING, scene)
        SceneId.BUS_STOP -> listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.TRIP, scene)
        SceneId.BUS -> listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.TRIP, SceneId.BUS_STOP, scene)
        SceneId.STALL_PRODUCE, SceneId.STALL_SPICE, SceneId.STALL_FLOWER, SceneId.STALL_CLOTH ->
            listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.MARKET, scene)
        else -> listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.BEDROOM, scene)
    }

    private fun open(scene: SceneId, settings: Settings = Settings(), voice: Boolean = false, edit: (SaveData) -> SaveData = { it }) {
        val save = edit(SaveData(nav = NavState(stackTo(scene)), settings = settings))
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

    // ---- Pass A scenes
    private val en = Settings(language = AppLanguage.ENGLISH)

    @Test fun kitchen() { open(SceneId.KITCHEN); shot("20_kitchen_ta") }

    @Test
    fun cookingMidway() {
        open(SceneId.COOKING, en) { it.copy(cooking = CookingState().doStep().doStep()) }
        shot("21_cooking_step3_en")
    }

    @Test
    fun dishesGuided() {
        open(SceneId.DISHES, en) { it.copy(dishes = DishwashState(guided = true).tap("plate_1").tap("plate_1").tap("cup_1")) }
        shot("22_dishes_guided_en")
    }

    @Test
    fun sewingWithCreation() {
        open(SceneId.SEWING) { it.copy(creations = listOf(Creation("c1", SewKind.BAG, "cloth_leaf_print"), Creation("c2", SewKind.PILLOW, "cloth_indigo_dots"))) }
        shot("23_sewing_ta")
    }

    @Test
    fun stitching() {
        open(SceneId.STITCH, en) { it.copy(sewing = SewingState().chooseFabric("cloth_maroon_checks").chooseKind(SewKind.SHIRT).stitch()) }
        shot("24_stitch_en")
    }

    @Test fun trip() { open(SceneId.TRIP, en) { it.copy(trip = TripState()) }; shot("25_trip_en") }
    @Test fun busStop() { open(SceneId.BUS_STOP) { it.copy(trip = TripState()) }; shot("26_bus_stop_ta") }

    @Test
    fun busTicket() {
        open(SceneId.BUS, en) { it.copy(trip = TripState().board(27).talkToConductor().addMoney(5)) }
        shot("27_bus_ticket_en")
    }

    @Test fun market() { open(SceneId.MARKET); shot("28_market_ta") }
    @Test fun stallProduce() { open(SceneId.STALL_PRODUCE); shot("29_stall_produce_ta") }
    @Test fun stallSpice() { open(SceneId.STALL_SPICE, en); shot("30_stall_spice_en") }
    @Test fun stallFlower() { open(SceneId.STALL_FLOWER); shot("31_stall_flower_ta") }
    @Test fun stallCloth() { open(SceneId.STALL_CLOTH, en); shot("32_stall_cloth_en") }

    @Test
    fun shoppingPanelWithHints() {
        open(SceneId.STALL_PRODUCE, en) { it.copy(shopping = ShoppingState().addToBag("tomato").addToBag("onion")) }
        tap("hud_list")
        shot("33_shopping_hints_en")
    }

    @Test
    fun itemCard() {
        open(SceneId.STALL_FLOWER, en)
        tap("hotspot_jasmine")
        shot("34_item_card_en")
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