package app.smriti.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingStateTest {
    private val s = ShoppingState()

    @Test
    fun `requested item ticks off the list`() {
        val after = s.addToBag("tomato")
        assertTrue(after.isCollected("tomato"))
        assertEquals(1, after.collectedCount)
    }

    @Test
    fun `browsing an unrequested item never ticks or removes list entries`() {
        val after = s.addToBag("onion")
        assertEquals(0, after.collectedCount)
        assertEquals(ShoppingState.DEFAULT_LIST, after.list)
        assertFalse(after.isOnList("onion"))
        assertTrue("onion" in after.bag)
        assertEquals(0, after.putBack("onion").bag.size)
    }

    @Test
    fun `any cloth satisfies the cloth entry and becomes a sewing fabric`() {
        val after = s.addToBag("cloth_maroon_checks")
        assertTrue(after.isCollected(ShoppingState.ANY_CLOTH))
        assertTrue(after.isOnList("cloth_maroon_checks"))
        assertEquals(listOf(Catalog.DEFAULT_FABRIC, "cloth_maroon_checks"), after.fabrics)
    }

    @Test
    fun `starter fabric is always available even without shopping`() {
        assertEquals(listOf(Catalog.DEFAULT_FABRIC), s.fabrics)
    }

    @Test
    fun `starter fabric cannot be bought and items are added once`() {
        assertEquals(s, s.addToBag(Catalog.DEFAULT_FABRIC))
        assertEquals(listOf("tomato"), s.addToBag("tomato").addToBag("tomato").bag)
    }

    @Test
    fun `unpacking keeps items collected and cloth usable`() {
        val after = s.addToBag("banana").addToBag("cloth_leaf_print").unpack()
        assertTrue(after.bag.isEmpty())
        assertTrue(after.isCollected("banana"))
        assertTrue("cloth_leaf_print" in after.fabrics)
    }

    @Test
    fun `next wanted item depends on the stall`() {
        assertEquals("tomato", s.nextWanted(StallId.PRODUCE))
        assertEquals("banana", s.addToBag("tomato").nextWanted(StallId.PRODUCE))
        assertEquals(ShoppingState.ANY_CLOTH, s.nextWanted(StallId.CLOTH))
        assertNull(s.addToBag("jasmine").nextWanted(StallId.FLOWER))
    }

    @Test
    fun `region follows hotspot position`() {
        assertEquals(Region.LEFT, regionOf(0.1f))
        assertEquals(Region.MIDDLE, regionOf(0.5f))
        assertEquals(Region.RIGHT, regionOf(0.9f))
    }
}

class CookingStateTest {
    @Test
    fun `right tools in order make coffee`() {
        var c = CookingState()
        listOf(CookTool.COFFEE_POWDER, CookTool.KETTLE, CookTool.FILTER, CookTool.MILK).forEach { c = c.tap(it) }
        assertTrue(c.isDone)
        assertEquals("coffee", c.tumblerLook)
    }

    @Test
    fun `another tool is a gentle pointer and changes nothing`() {
        val c = CookingState().tap(CookTool.MILK)
        assertEquals(CookStep.POWDER_IN_FILTER, c.step)
        assertEquals(CookTool.MILK, c.notNow)
        assertEquals(CookStep.WATER_IN_FILTER, c.tap(CookTool.COFFEE_POWDER).step)
        assertNull(c.tap(CookTool.COFFEE_POWDER).notNow)
    }

    @Test
    fun `do it for me, undo and reset`() {
        val c = CookingState().doStep().doStep()
        assertEquals(CookStep.DECOCTION_TO_TUMBLER, c.step)
        assertEquals("brewing", c.filterLook)
        assertEquals(CookStep.WATER_IN_FILTER, c.undo().step)
        assertEquals(CookingState(), c.reset())
        assertEquals(CookStep.POWDER_IN_FILTER, CookingState().undo().step)
    }

    @Test
    fun `dish never shows done before the last step`() {
        var c = CookingState()
        repeat(3) { c = c.doStep(); assertFalse(c.isDone); assertFalse(c.tumblerLook == "coffee") }
        assertTrue(c.doStep().isDone)
        assertEquals(c.doStep(), c.doStep().doStep())
    }
}

class DishwashStateTest {
    @Test
    fun `tap washes then racks in any order`() {
        var d = DishwashState()
        d = d.tap("cup_1")
        assertEquals(DishCondition.CLEAN, d.dishes.first { it.id == "cup_1" }.condition)
        d = d.tap("cup_1")
        assertEquals(listOf("cup_1"), d.onRack.map { it.id })
        d = d.tap("cup_1")
        assertEquals(1, d.onRack.size)
        assertFalse(d.isDone)
    }

    @Test
    fun `guided mode only suggests, assisted completion finishes everything`() {
        val g = DishwashState().withGuided(true)
        assertEquals("plate_1", g.suggested?.id)
        assertEquals("plate_2", g.tap("plate_1").tap("plate_1").suggested?.id)
        assertTrue(g.doAll().isDone)
        assertTrue(g.doAll().reset().guided)
        assertFalse(g.doAll().reset().isDone)
    }
}

class SewingStateTest {
    @Test
    fun `choose, stitch three times, keep`() {
        var s = SewingState().chooseFabric("cloth_indigo_dots").chooseKind(SewKind.PILLOW)
        assertEquals(SewPhase.STITCHING, s.phase)
        repeat(SewingState.STITCHES_NEEDED) { s = s.stitch() }
        assertEquals(SewPhase.FINISHED, s.phase)
        val c = s.toCreation("c1")
        assertNotNull(c)
        assertEquals("sewing/pillow_cloth_indigo_dots.webp", c!!.sprite)
    }

    @Test
    fun `not finished cannot be kept, undo and start over work`() {
        val s = SewingState().chooseFabric(Catalog.DEFAULT_FABRIC).chooseKind(SewKind.BAG).stitch()
        assertNull(s.toCreation("x"))
        assertEquals(0, s.undo().stitches)
        assertEquals(SewPhase.CHOOSE_ITEM, s.undo().undo().phase)
        assertEquals(SewPhase.CHOOSE_FABRIC, s.undo().undo().undo().phase)
        assertEquals(SewingState(), s.startOver())
    }

    @Test
    fun `do it for me completes even from the very start`() {
        val s = SewingState().doItForMe(Catalog.DEFAULT_FABRIC)
        assertEquals(SewPhase.FINISHED, s.phase)
        assertNotNull(s.toCreation("y"))
    }
}

class TripStateTest {
    @Test
    fun `any bus can be boarded without judgement`() {
        TripState.BUS_OPTIONS.forEach { n ->
            val t = TripState().board(n)
            assertEquals(TripPhase.ON_BUS, t.phase)
            assertEquals(n, t.chosenBus)
        }
    }

    @Test
    fun `bus options are distinct and include the suggested number`() {
        assertTrue(TripState.SUGGESTED_BUS in TripState.BUS_OPTIONS)
        assertEquals(TripState.BUS_OPTIONS.size, TripState.BUS_OPTIONS.toSet().size)
        assertEquals(27, TripState.busForHotspot("bus_2"))
        assertNull(TripState.busForHotspot("bus_9"))
    }

    @Test
    fun `paying works with exact money or with change, never traps`() {
        val t = TripState().board(27).talkToConductor()
        assertEquals(TripPhase.TICKET, t.phase)
        val short = t.addMoney(5).pay()
        assertEquals(TripPhase.TICKET, short.phase)
        assertEquals(5, short.stillNeeded)
        val exact = short.addMoney(5).pay()
        assertEquals(TripPhase.PAID, exact.phase)
        assertEquals(0, exact.change)
        val over = t.addMoney(20).pay()
        assertEquals(TripPhase.PAID, over.phase)
        assertEquals(10, over.change)
        assertTrue(t.addMoney(2).clearTray().tray.isEmpty())
        assertEquals(t, t.addMoney(3))
    }

    @Test
    fun `pay for me is available from the start of the ride`() {
        assertEquals(TripPhase.PAID, TripState(phase = TripPhase.ON_BUS).payForMe().phase)
        assertEquals(TripPhase.PAID, TripState().board(12).talkToConductor().payForMe().phase)
    }

    @Test
    fun `going home starts on the bus with no number task`() {
        assertEquals(TripPhase.ON_BUS, TripState(direction = TripDirection.TO_HOME).phase)
    }
}
