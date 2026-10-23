package app.smriti.ui

import app.smriti.data.InMemorySaveRepository
import app.smriti.data.LayoutVariant
import app.smriti.domain.CookTool
import app.smriti.domain.HintLevel
import app.smriti.domain.RadioSession
import app.smriti.domain.SaveData
import app.smriti.domain.SceneId
import app.smriti.domain.SewKind
import app.smriti.domain.SewPhase
import app.smriti.domain.TripDirection
import app.smriti.domain.TripPhase
import app.smriti.domain.TripState
import app.smriti.domain.PlaybackStatus
import app.smriti.interactions.GameAction
import app.smriti.testing.FakeMedia
import app.smriti.testing.FakeRadioPlayer
import app.smriti.testing.FakeVoice
import app.smriti.testing.fakeStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pass B: state flowing between features through the real ViewModel (fakes for audio/voice). */
@OptIn(ExperimentalCoroutinesApi::class)
class IntegrationViewModelTest {
    private lateinit var save: InMemorySaveRepository
    private lateinit var voice: FakeVoice
    private val radio = FakeRadioPlayer()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(initial: SaveData = SaveData()): GameViewModel {
        save = InMemorySaveRepository(initial)
        voice = FakeVoice()
        return GameViewModel(save, voice, radio, fakeStrings, FakeMedia())
    }

    private fun GameViewModel.toVeranda() {
        dispatch(GameAction.Start)
        dispatch(GameAction.GoTo(SceneId.VERANDA))
    }

    @Test
    fun `full round trip by bus carries bought cloth into the sewing corner`() {
        val vm = vm()
        vm.toVeranda()
        vm.dispatch(GameAction.OpenDestinationChooser)
        vm.dispatch(GameAction.PlanTrip(TripDirection.TO_MARKET))
        assertEquals(SceneId.TRIP, vm.state.value.scene)
        vm.dispatch(GameAction.TakeBus)
        assertEquals(SceneId.BUS_STOP, vm.state.value.scene)
        vm.dispatch(GameAction.ChooseBus(63)) // not the suggested 27: still fine
        assertEquals(SceneId.BUS, vm.state.value.scene)
        assertEquals(TripPhase.ON_BUS, vm.state.value.trip?.phase)
        assertTrue("conductor does not speak first", voice.spoken.isEmpty())
        vm.dispatch(GameAction.TalkToConductor)
        assertEquals("conductor_ticket", vm.state.value.dialogue?.line?.id)
        vm.addMoney(20)
        vm.pay()
        assertEquals(TripPhase.PAID, vm.state.value.trip?.phase)
        assertEquals(10, vm.state.value.trip?.change)
        vm.arriveFromBus()
        assertEquals(SceneId.MARKET, vm.state.value.scene)
        assertNull(vm.state.value.trip)
        // Back at the market goes home, not back onto the bus.
        assertEquals(listOf(SceneId.TITLE, SceneId.EXTERIOR, SceneId.VERANDA, SceneId.MARKET), vm.state.value.nav.stack)

        vm.dispatch(GameAction.GoTo(SceneId.STALL_CLOTH))
        vm.dispatch(GameAction.SelectItem("cloth_leaf_print"))
        assertEquals(Overlay.ITEM, vm.state.value.overlay)
        vm.addToBag("cloth_leaf_print")
        vm.dispatch(GameAction.Back)
        vm.dispatch(GameAction.Back)
        assertEquals(SceneId.MARKET, vm.state.value.scene)

        vm.dispatch(GameAction.PlanTrip(TripDirection.TO_HOME))
        vm.dispatch(GameAction.TakeBus)
        assertEquals(SceneId.BUS, vm.state.value.scene) // no bus-number task on the way home
        vm.payForMe()
        vm.arriveFromBus()
        assertEquals(SceneId.VERANDA, vm.state.value.scene)

        assertTrue("cloth_leaf_print" in vm.state.value.shopping.fabrics)
        vm.dispatch(GameAction.GoTo(SceneId.SEWING))
        vm.dispatch(GameAction.GoTo(SceneId.STITCH))
        vm.chooseFabric("cloth_leaf_print")
        vm.chooseKind(SewKind.BAG)
        repeat(3) { vm.stitch() }
        vm.sewKeep()
        assertEquals("cloth_leaf_print", save.current.creations.single().fabric)
        assertEquals(SewPhase.CHOOSE_FABRIC, save.current.sewing.phase)
    }

    @Test
    fun `skipping travel arrives directly both ways`() {
        val vm = vm()
        vm.toVeranda()
        vm.dispatch(GameAction.PlanTrip(TripDirection.TO_MARKET))
        vm.dispatch(GameAction.ArriveDirectly)
        assertEquals(SceneId.MARKET, vm.state.value.scene)
        vm.dispatch(GameAction.PlanTrip(TripDirection.TO_HOME))
        vm.dispatch(GameAction.ArriveDirectly)
        assertEquals(SceneId.VERANDA, vm.state.value.scene)
    }

    @Test
    fun `skip ride from the bus arrives at the market`() {
        val vm = vm()
        vm.toVeranda()
        vm.dispatch(GameAction.PlanTrip(TripDirection.TO_MARKET))
        vm.dispatch(GameAction.TakeBus)
        vm.dispatch(GameAction.ChooseBus(27))
        vm.dispatch(GameAction.SkipRide)
        assertEquals(SceneId.MARKET, vm.state.value.scene)
    }

    @Test
    fun `leaving the market without buying everything is allowed`() {
        val vm = vm()
        vm.toVeranda()
        vm.dispatch(GameAction.PlanTrip(TripDirection.TO_MARKET))
        vm.dispatch(GameAction.ArriveDirectly)
        vm.dispatch(GameAction.Home)
        assertEquals(SceneId.VERANDA, vm.state.value.scene)
        assertEquals(0, vm.state.value.shopping.collectedCount)
    }

    @Test
    fun `hints work at any level, any time, without errors first`() {
        val vm = vm()
        vm.toVeranda()
        vm.dispatch(GameAction.PlanTrip(TripDirection.TO_MARKET))
        vm.dispatch(GameAction.ArriveDirectly)
        vm.dispatch(GameAction.GoTo(SceneId.STALL_PRODUCE))
        vm.hint(HintLevel.SHOW_LIST, LayoutVariant.LANDSCAPE)
        assertEquals(Overlay.SHOPPING, vm.state.value.overlay)
        vm.hint(HintLevel.SAY_NAME, LayoutVariant.LANDSCAPE)
        assertEquals("hint_say_tomato", vm.state.value.dialogue?.line?.id)
        vm.hint(HintLevel.HIGHLIGHT, LayoutVariant.LANDSCAPE)
        assertEquals("tomato", vm.state.value.helpers.highlight)
        vm.hint(HintLevel.PLACE_FOR_ME, LayoutVariant.LANDSCAPE)
        assertTrue("tomato" in vm.state.value.shopping.bag)
        assertEquals("hint_placed_tomato", vm.state.value.dialogue?.line?.id)
        vm.hint(HintLevel.SAY_NAME, LayoutVariant.LANDSCAPE)
        assertEquals("hint_say_banana", vm.state.value.dialogue?.line?.id)
    }

    @Test
    fun `bus number help repeats the cue on request`() {
        val vm = vm()
        vm.toVeranda()
        vm.dispatch(GameAction.PlanTrip(TripDirection.TO_MARKET))
        vm.dispatch(GameAction.TakeBus)
        vm.dispatch(GameAction.SayBusNumber)
        assertEquals("guide_bus_number", vm.state.value.dialogue?.line?.id)
        assertEquals("en:${app.smriti.R.string.guide_bus_number}", fakeStrings.get(app.smriti.R.string.guide_bus_number, app.smriti.domain.AppLanguage.ENGLISH))
    }

    @Test
    fun `cooking and dishes keep progress across a restart`() {
        val vm = vm()
        vm.toVeranda()
        vm.dispatch(GameAction.GoTo(SceneId.KITCHEN))
        vm.dispatch(GameAction.GoTo(SceneId.COOKING))
        vm.cook(CookTool.COFFEE_POWDER)
        vm.cook(CookTool.MILK) // a gentle pointer, nothing changes
        vm.dishTap("plate_1")
        val restored = GameViewModel(save, FakeVoice(), FakeRadioPlayer(), fakeStrings, FakeMedia())
        assertEquals(SceneId.COOKING, restored.state.value.scene)
        assertEquals(2, restored.state.value.cooking.stepNumber)
        assertNull(restored.state.value.cooking.notNow)
        assertEquals("plate_1", restored.state.value.dishes.dishes.first { it.condition.name == "CLEAN" }.id)
    }

    @Test
    fun `trip in progress survives a restart and radio stops when leaving for the market`() {
        val vm = vm()
        vm.toVeranda()
        vm.onRadio(RadioSession.Event.Select("demo_tune_1"))
        vm.dispatch(GameAction.PlanTrip(TripDirection.TO_MARKET))
        assertEquals(PlaybackStatus.STOPPED, vm.state.value.radio.status)
        vm.dispatch(GameAction.TakeBus)
        vm.dispatch(GameAction.ChooseBus(45))
        val restored = GameViewModel(save, FakeVoice(), FakeRadioPlayer(), fakeStrings, FakeMedia())
        assertEquals(SceneId.BUS, restored.state.value.scene)
        assertEquals(45, restored.state.value.trip?.chosenBus)
        assertEquals(TripState.FARE, restored.state.value.trip?.fare)
    }

    @Test
    fun `vendor speaks only when tapped`() {
        val vm = vm()
        vm.toVeranda()
        vm.dispatch(GameAction.PlanTrip(TripDirection.TO_MARKET))
        vm.dispatch(GameAction.ArriveDirectly)
        vm.dispatch(GameAction.GoTo(SceneId.STALL_FLOWER))
        assertNull(vm.state.value.dialogue)
        vm.dispatch(GameAction.TalkToVendor(app.smriti.domain.StallId.FLOWER))
        assertEquals("vendor_flower_hello", vm.state.value.dialogue?.line?.id)
        vm.dispatch(GameAction.Back)
        vm.dispatch(GameAction.Back)
        assertNull(vm.state.value.dialogue)
    }
}
