package app.smriti.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.smriti.R
import app.smriti.domain.Catalog
import app.smriti.domain.HintLevel
import app.smriti.domain.ShoppingState
import app.smriti.domain.TripDirection
import app.smriti.domain.TripPhase
import app.smriti.domain.TripState
import app.smriti.ui.components.Badge
import app.smriti.ui.components.HudButton
import app.smriti.ui.components.BigButton
import app.smriti.ui.components.Panel
import app.smriti.ui.components.QuietButton
import app.smriti.ui.components.SmritiIcons
import app.smriti.ui.components.Sprite
import app.smriti.ui.components.ToolButton
import app.smriti.ui.components.rememberAssetBitmap
import app.smriti.ui.theme.SmritiColors

/** Label for a list entry (an item, or "a cloth you like"). */
@Composable
fun entryLabel(entry: String): String =
    if (entry == ShoppingState.ANY_CLOTH) stringResource(R.string.shopping_any_cloth)
    else Catalog.item(entry)?.let { stringResource(it.label) } ?: entry

fun entrySprite(entry: String, shopping: ShoppingState): String =
    if (entry == ShoppingState.ANY_CLOTH) {
        val owned = (shopping.bag + shopping.putAway).firstOrNull { Catalog.item(it)?.isCloth == true }
        "items/${owned ?: "cloth_indigo_dots"}.webp"
    } else "items/$entry.webp"

// ---------------------------------------------------------------- trip planner

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripScreen(
    direction: TripDirection,
    shopping: ShoppingState,
    onTakeBus: () -> Unit,
    onDirect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toMarket = direction == TripDirection.TO_MARKET
    BoxWithConstraints(modifier.fillMaxSize().background(SmritiColors.Cream)) {
        val wide = maxWidth > 700.dp
        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(if (toMarket) R.string.trip_title_market else R.string.trip_title_home), style = MaterialTheme.typography.headlineSmall)
            Panel(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.shopping_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                ListPictures(shopping)
            }
            val cards: @Composable (Modifier) -> Unit = { m ->
                TravelCard(stringResource(R.string.trip_take_bus), "scenes/bus_stop_land.webp", onTakeBus, m.testTag("trip_bus"))
                TravelCard(
                    stringResource(if (toMarket) R.string.trip_direct_market else R.string.trip_direct_home),
                    if (toMarket) "scenes/market_land.webp" else "scenes/veranda_land.webp", onDirect, m.testTag("trip_direct"),
                )
            }
            if (wide) Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { cards(Modifier.weight(1f)) }
            else Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { cards(Modifier.fillMaxWidth()) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListPictures(shopping: ShoppingState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        shopping.list.forEach { entry ->
            val got = shopping.isCollected(entry)
            Column(
                Modifier.width(104.dp).background(if (got) SmritiColors.Leaf.copy(alpha = 0.16f) else SmritiColors.Paper, RoundedCornerShape(16.dp))
                    .border(2.dp, if (got) SmritiColors.Leaf else SmritiColors.Outline, RoundedCornerShape(16.dp)).padding(6.dp)
                    .testTag("list_$entry"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Sprite(entrySprite(entry, shopping), Modifier.size(64.dp))
                Text(entryLabel(entry), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                if (got) Text("✓ " + stringResource(R.string.shopping_in_bag), style = MaterialTheme.typography.labelMedium, color = SmritiColors.Leaf, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun TravelCard(label: String, image: String, onClick: () -> Unit, modifier: Modifier) {
    val bmp = rememberAssetBitmap(image)
    Surface(onClick = onClick, modifier = modifier.semantics { contentDescription = label }, shape = RoundedCornerShape(20.dp), color = SmritiColors.PaperDim, border = BorderStroke(2.dp, SmritiColors.Outline)) {
        Column {
            if (bmp != null) Image(bmp, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(130.dp).clipToBounds())
            Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(14.dp))
        }
    }
}

// ---------------------------------------------------------------- bus stop cue

/** The number to look for, in the same board style as the buses. Always visible: a cue, not a test. */
@Composable
fun BusCue(onSay: () -> Unit, modifier: Modifier = Modifier) {
    // One compact row so it never reaches the bus fronts or the Guide on short screens.
    Panel(modifier.widthIn(max = 360.dp).testTag("bus_cue")) {
        Text(stringResource(R.string.bus_cue), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberBoard(TripState.SUGGESTED_BUS)
            HudButton(stringResource(R.string.bus_cue_say), SmritiIcons.Speaker, onSay, Modifier.widthIn(max = 120.dp).testTag("bus_say"))
        }
    }
}

@Composable
fun NumberBoard(n: Int) {
    Box(
        Modifier.background(Color(0xFF1F1F22), RoundedCornerShape(10.dp)).border(3.dp, SmritiColors.Outline, RoundedCornerShape(10.dp)).padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(n.toString(), color = Color(0xFFFFE27A), fontSize = 44.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("bus_cue_number"))
    }
}

// ---------------------------------------------------------------- on the bus: ticket and payment

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BusPanel(
    trip: TripState,
    highlight: String?,
    onTalk: () -> Unit,
    onSkip: () -> Unit,
    onMoney: (Int) -> Unit,
    onClear: () -> Unit,
    onPay: () -> Unit,
    onShowMe: () -> Unit,
    onPayForMe: () -> Unit,
    onArrive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val home = trip.direction == TripDirection.TO_HOME
    Panel(modifier.widthIn(max = 560.dp).verticalScroll(rememberScrollState()).testTag("bus_panel")) {
        when (trip.phase) {
            TripPhase.AT_STOP, TripPhase.ON_BUS -> {
                BigButton(stringResource(R.string.bus_talk), onTalk, Modifier.fillMaxWidth().testTag("bus_talk"), SmritiIcons.Speaker, color = SmritiColors.Leaf)
                Spacer(Modifier.height(10.dp))
                QuietButton(stringResource(R.string.bus_skip), onSkip, Modifier.fillMaxWidth().testTag("bus_skip"), SmritiIcons.Next)
            }
            TripPhase.TICKET, TripPhase.PAID -> {
                Ticket(trip)
                Spacer(Modifier.height(10.dp))
                if (trip.phase == TripPhase.TICKET) {
                    Text(stringResource(R.string.pay_in_hand, trip.trayTotal), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("pay_in_hand"))
                    if (trip.trayTotal in 1 until trip.fare) {
                        Text(stringResource(R.string.pay_more, trip.stillNeeded), style = MaterialTheme.typography.bodyMedium, color = SmritiColors.InkSoft, modifier = Modifier.testTag("pay_more"))
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TripState.MONEY.forEach { v -> Money(v, highlight == "money_$v") { onMoney(v) } }
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BigButton(stringResource(R.string.pay_button), onPay, Modifier.testTag("pay"), color = SmritiColors.Leaf, enabled = trip.trayTotal > 0)
                        ToolButton(stringResource(R.string.pay_clear), SmritiIcons.Back, onClear, Modifier.testTag("pay_clear"), enabled = trip.tray.isNotEmpty())
                        ToolButton(stringResource(R.string.pay_show_me), SmritiIcons.Eye, onShowMe, Modifier.testTag("pay_show_me"))
                        ToolButton(stringResource(R.string.pay_for_me), SmritiIcons.Help, onPayForMe, Modifier.testTag("pay_for_me"))
                    }
                    Text(stringResource(R.string.pay_pretend), style = MaterialTheme.typography.labelMedium, color = SmritiColors.InkSoft, modifier = Modifier.padding(top = 6.dp))
                } else {
                    Text(stringResource(R.string.pay_done), style = MaterialTheme.typography.titleMedium, color = SmritiColors.Leaf, modifier = Modifier.testTag("pay_done"))
                    if (trip.change > 0) Text(stringResource(R.string.pay_change, trip.change), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("pay_change"))
                    Spacer(Modifier.height(10.dp))
                    BigButton(
                        stringResource(if (home) R.string.bus_arrive_home else R.string.bus_arrive_market), onArrive,
                        Modifier.fillMaxWidth().testTag("bus_arrive"), SmritiIcons.Next, color = SmritiColors.Leaf,
                    )
                }
                if (trip.phase == TripPhase.TICKET) {
                    Spacer(Modifier.height(8.dp))
                    QuietButton(stringResource(R.string.bus_skip), onSkip, Modifier.fillMaxWidth().testTag("bus_skip"), SmritiIcons.Next)
                }
            }
        }
    }
}

/** The ticket stays on screen for the whole payment, so the fare never has to be remembered. */
@Composable
private fun Ticket(trip: TripState) {
    val home = trip.direction == TripDirection.TO_HOME
    val from = stringResource(if (home) R.string.place_market else R.string.place_home_stop)
    val to = stringResource(if (home) R.string.place_home_stop else R.string.place_market)
    Column(
        Modifier.fillMaxWidth().background(Color(0xFFFBF6E6), RoundedCornerShape(10.dp)).border(2.dp, SmritiColors.Outline, RoundedCornerShape(10.dp)).padding(12.dp).testTag("ticket"),
    ) {
        Text(stringResource(R.string.ticket_title), style = MaterialTheme.typography.labelMedium, color = SmritiColors.InkSoft)
        Text(stringResource(R.string.ticket_bus, trip.chosenBus ?: TripState.SUGGESTED_BUS), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.ticket_route, from, to), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.ticket_fare, trip.fare), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("ticket_fare"))
    }
}

/** A pretend coin (round) or note (rectangular). Values are drawn as text, never baked into art. */
@Composable
private fun Money(value: Int, highlighted: Boolean, onClick: () -> Unit) {
    val note = value >= 20
    val label = stringResource(R.string.money, value)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(width = if (note) 104.dp else 72.dp, height = 72.dp)
            .then(if (highlighted) Modifier.border(6.dp, SmritiColors.Brass, if (note) RoundedCornerShape(10.dp) else CircleShape) else Modifier)
            .semantics { contentDescription = label }
            .testTag("money_$value"),
        shape = if (note) RoundedCornerShape(10.dp) else CircleShape,
        border = BorderStroke(3.dp, SmritiColors.Outline),
        color = Color.Transparent,
    ) {
        Box(
            Modifier.fillMaxSize().background(
                if (note) Brush.horizontalGradient(listOf(Color(0xFFE8B7C8), Color(0xFFD9A1B5)))
                else if (value >= 5) Brush.radialGradient(listOf(Color(0xFFF3D27A), Color(0xFFC9973A)))
                else Brush.radialGradient(listOf(Color(0xFFEEF1F2), Color(0xFFA9B2B8))),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = SmritiColors.Ink)
        }
    }
}

// ---------------------------------------------------------------- shopping list, bag and hints

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShoppingPanel(
    shopping: ShoppingState,
    inStall: Boolean,
    atHome: Boolean,
    onHint: (HintLevel) -> Unit,
    onPutBack: (String) -> Unit,
    onUnpack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Panel(modifier.widthIn(max = 760.dp).verticalScroll(rememberScrollState()).testTag("shopping_panel")) {
        Text(stringResource(R.string.shopping_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.shopping_progress, shopping.collectedCount, shopping.list.size), style = MaterialTheme.typography.labelLarge, color = SmritiColors.InkSoft)
        Spacer(Modifier.height(8.dp))
        ListPictures(shopping)
        if (inStall) {
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.hint_title), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    HintLevel.SAY_NAME to R.string.hint_say_name_button,
                    HintLevel.WHERE_TO_LOOK to R.string.hint_where_button,
                    HintLevel.HIGHLIGHT to R.string.hint_highlight_button,
                    HintLevel.PLACE_FOR_ME to R.string.hint_place_button,
                ).forEach { (level, label) ->
                    QuietButton(stringResource(label), { onHint(level) }, Modifier.testTag("hint_${level.name.lowercase()}"))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.shopping_bag), style = MaterialTheme.typography.titleMedium)
        if (shopping.bag.isEmpty()) Text(stringResource(R.string.shopping_bag_empty), style = MaterialTheme.typography.bodyLarge)
        shopping.bag.forEach { id ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("bag_$id"), verticalAlignment = Alignment.CenterVertically) {
                Sprite("items/$id.webp", Modifier.size(52.dp))
                Spacer(Modifier.width(8.dp))
                Text(entryLabel(id), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (!shopping.isOnList(id)) Badge(stringResource(R.string.shopping_extra), SmritiColors.Indigo)
                Spacer(Modifier.width(6.dp))
                QuietButton(stringResource(R.string.shopping_put_back), { onPutBack(id) }, Modifier.testTag("put_back_$id"))
            }
        }
        if (atHome && shopping.bag.isNotEmpty()) {
            QuietButton(stringResource(R.string.shopping_unpack), onUnpack, Modifier.fillMaxWidth().testTag("unpack"), SmritiIcons.Home)
        }
        if (shopping.putAway.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.shopping_put_away), style = MaterialTheme.typography.titleMedium)
            Text(shopping.putAway.map { entryLabel(it) }.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))
        BigButton(stringResource(R.string.action_close), onClose, Modifier.fillMaxWidth().testTag("shopping_close"), SmritiIcons.Close)
    }
}

/** Looking at something on a stall. Taking it, putting it back or just looking are all fine. */
@Composable
fun ItemCard(
    itemId: String,
    shopping: ShoppingState,
    onAdd: () -> Unit,
    onPutBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = Catalog.item(itemId) ?: return
    val inBag = itemId in shopping.bag
    Panel(modifier.widthIn(max = 520.dp).verticalScroll(rememberScrollState()).testTag("item_card")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Sprite(item.sprite, Modifier.size(140.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(item.label), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(item.category), style = MaterialTheme.typography.bodyMedium, color = SmritiColors.InkSoft)
                if (shopping.isOnList(itemId)) Badge(stringResource(R.string.item_on_list), SmritiColors.Leaf, Modifier.padding(top = 6.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        when {
            inBag -> Text(stringResource(R.string.item_in_bag), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("item_in_bag"))
            !shopping.isOnList(itemId) -> Text(stringResource(R.string.item_not_on_list), style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(10.dp))
        if (inBag) QuietButton(stringResource(R.string.shopping_put_back), onPutBack, Modifier.fillMaxWidth().testTag("item_put_back"))
        else BigButton(stringResource(R.string.item_add), onAdd, Modifier.fillMaxWidth().testTag("item_add"), SmritiIcons.Add, color = SmritiColors.Leaf)
        Spacer(Modifier.height(8.dp))
        QuietButton(stringResource(R.string.item_just_looking), onClose, Modifier.fillMaxWidth().heightIn(min = 60.dp).testTag("item_close"))
        Text(stringResource(R.string.shop_no_payment), style = MaterialTheme.typography.labelMedium, color = SmritiColors.InkSoft, modifier = Modifier.padding(top = 6.dp))
    }
}
