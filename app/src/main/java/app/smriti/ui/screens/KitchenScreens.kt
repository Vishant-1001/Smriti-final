package app.smriti.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.smriti.R
import app.smriti.domain.CookStep
import app.smriti.domain.CookTool
import app.smriti.domain.CookingState
import app.smriti.domain.Dish
import app.smriti.domain.DishCondition
import app.smriti.domain.DishKind
import app.smriti.domain.DishwashState
import app.smriti.ui.components.BigButton
import app.smriti.ui.components.Panel
import app.smriti.ui.components.SmritiIcons
import app.smriti.ui.components.Sprite
import app.smriti.ui.components.ToolButton
import app.smriti.ui.theme.LocalSmritiPrefs
import app.smriti.ui.theme.SmritiColors

/** Large selectable picture card used by every activity (tools, dishes, fabrics, templates). */
@Composable
fun PictureCard(
    label: String,
    sprite: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    selected: Boolean = false,
    note: String? = null,
    imageSize: Dp = 96.dp,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 120.dp)
            .then(if (highlighted) Modifier.border(6.dp, SmritiColors.Brass, RoundedCornerShape(22.dp)) else Modifier)
            .semantics { contentDescription = listOfNotNull(label, note).joinToString(", "); if (selected) stateDescription = "✓" },
        shape = RoundedCornerShape(20.dp),
        color = if (selected) SmritiColors.Leaf.copy(alpha = 0.18f) else SmritiColors.Paper,
        border = BorderStroke(if (selected) 4.dp else 2.dp, if (selected) SmritiColors.Leaf else SmritiColors.Outline),
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Sprite(sprite, Modifier.size(imageSize))
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            if (note != null) Text(note, style = MaterialTheme.typography.labelMedium, color = SmritiColors.InkSoft, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun cookStepText(step: CookStep): String = stringResource(
    when (step) {
        CookStep.POWDER_IN_FILTER -> R.string.cook_step_powder
        CookStep.WATER_IN_FILTER -> R.string.cook_step_water
        CookStep.DECOCTION_TO_TUMBLER -> R.string.cook_step_decoction
        CookStep.ADD_MILK -> R.string.cook_step_milk
        CookStep.DONE -> R.string.cook_done
    }
)

private fun toolSprite(tool: CookTool, state: CookingState) = when (tool) {
    CookTool.COFFEE_POWDER -> "items/coffee_powder.webp"
    CookTool.KETTLE -> "kitchen/kettle.webp"
    CookTool.FILTER -> "kitchen/filter_${state.filterLook}.webp"
    CookTool.MILK -> "kitchen/milk_pot.webp"
}

@Composable
private fun toolLabel(tool: CookTool) = stringResource(
    when (tool) {
        CookTool.COFFEE_POWDER -> R.string.item_coffee_powder
        CookTool.KETTLE -> R.string.tool_kettle
        CookTool.FILTER -> R.string.tool_filter
        CookTool.MILK -> R.string.tool_milk
    }
)

/** Filter coffee, one tap per step. The pictures always show the real state of the drink. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CookingScreen(
    state: CookingState,
    highlight: String?,
    onTool: (CookTool) -> Unit,
    onRepeat: () -> Unit,
    onShowMe: () -> Unit,
    onDoIt: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = LocalSmritiPrefs.current.reducedMotion
    BoxWithConstraints(modifier.fillMaxSize().background(SmritiColors.Cream)) {
        val landscape = maxWidth > maxHeight
        val stepCard: @Composable () -> Unit = {
            Panel(Modifier.fillMaxWidth().testTag("cook_step")) {
                if (!state.isDone) {
                    Text(stringResource(R.string.cook_step_of, state.stepNumber, state.totalSteps), style = MaterialTheme.typography.labelMedium, color = SmritiColors.InkSoft)
                }
                Text(cookStepText(state.step), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("cook_instruction"))
                if (state.notNow != null) {
                    Text(stringResource(R.string.cook_not_now), style = MaterialTheme.typography.bodyMedium, color = SmritiColors.InkSoft)
                }
            }
        }
        val work: @Composable (Modifier) -> Unit = { m ->
            Row(m, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Crossfade(state.filterLook, animationSpec = if (reduced) snap() else tween(400), label = "filter") { look ->
                    Sprite("kitchen/filter_$look.webp", Modifier.size(170.dp).testTag("filter_$look"))
                }
                Crossfade(state.tumblerLook, animationSpec = if (reduced) snap() else tween(400), label = "tumbler") { look ->
                    Sprite("kitchen/tumbler_$look.webp", Modifier.size(170.dp).testTag("tumbler_$look"))
                }
            }
        }
        val tools: @Composable () -> Unit = {
            if (state.isDone) {
                BigButton(stringResource(R.string.cook_again), onReset, Modifier.fillMaxWidth().testTag("cook_again"), SmritiIcons.Replay, color = SmritiColors.Leaf)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 2) {
                    CookTool.entries.forEach { tool ->
                        PictureCard(
                            toolLabel(tool), toolSprite(tool, state), { onTool(tool) },
                            Modifier.weight(1f).testTag("tool_${tool.name.lowercase()}"),
                            highlighted = highlight == tool.name, imageSize = 84.dp,
                        )
                    }
                }
            }
        }
        val toolbar: @Composable () -> Unit = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolButton(stringResource(R.string.act_repeat), SmritiIcons.Speaker, onRepeat, Modifier.testTag("cook_repeat"))
                ToolButton(stringResource(R.string.act_show_me), SmritiIcons.Eye, onShowMe, Modifier.testTag("cook_show_me"), enabled = !state.isDone)
                ToolButton(stringResource(R.string.act_do_it), SmritiIcons.Help, onDoIt, Modifier.testTag("cook_do_it"), enabled = !state.isDone)
                ToolButton(stringResource(R.string.act_undo), SmritiIcons.Back, onUndo, Modifier.testTag("cook_undo"), enabled = state.step != CookStep.POWDER_IN_FILTER)
                ToolButton(stringResource(R.string.act_start_over), SmritiIcons.Replay, onReset, Modifier.testTag("cook_reset"))
            }
        }
        if (landscape) {
            Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    stepCard()
                    work(Modifier.weight(1f).fillMaxWidth())
                }
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    tools()
                    toolbar()
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                stepCard()
                work(Modifier.fillMaxWidth().height(200.dp))
                tools()
                toolbar()
            }
        }
    }
}

@Composable
private fun dishName(kind: DishKind) = stringResource(
    when (kind) {
        DishKind.PLATE -> R.string.dish_plate
        DishKind.CUP -> R.string.dish_cup
        DishKind.BOWL -> R.string.dish_bowl
    }
)

private fun dishSprite(d: Dish) = "kitchen/${d.kind.name.lowercase()}_${if (d.condition == DishCondition.DIRTY) "dirty" else "clean"}.webp"

/** Washing up by tapping. Any order and any rack position are fine; guided mode only points. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DishesScreen(
    state: DishwashState,
    onTap: (String) -> Unit,
    onGuided: (Boolean) -> Unit,
    onDoAll: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = LocalSmritiPrefs.current.reducedMotion
    val suggested = if (state.guided) state.suggested else null
    Column(
        modifier.fillMaxSize().background(SmritiColors.Cream).padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeChip(stringResource(R.string.dishes_free), !state.guided, Modifier.weight(1f).testTag("dishes_free")) { onGuided(false) }
            ModeChip(stringResource(R.string.dishes_guided), state.guided, Modifier.weight(1f).testTag("dishes_guided")) { onGuided(true) }
        }
        Panel(Modifier.fillMaxWidth()) {
            val text = when {
                state.isDone -> stringResource(R.string.dishes_done)
                suggested != null -> stringResource(R.string.dish_next, dishName(suggested.kind)) + " " +
                    stringResource(if (suggested.condition == DishCondition.DIRTY) R.string.dishes_tap_dirty else R.string.dishes_tap_clean)
                else -> stringResource(R.string.dishes_tap_dirty) + " " + stringResource(R.string.dishes_tap_clean)
            }
            Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("dishes_instruction"))
        }
        // Sink: dishes still to wash or to put away
        Box(Modifier.fillMaxWidth().background(Color(0xFF9AA3A8), RoundedCornerShape(24.dp)).border(3.dp, SmritiColors.Outline, RoundedCornerShape(24.dp)).padding(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.dishes.filter { it.condition != DishCondition.ON_RACK }.forEach { d ->
                    Crossfade(d.condition, animationSpec = if (reduced) snap() else tween(500), label = d.id) { cond ->
                        PictureCard(
                            dishName(d.kind), dishSprite(d.copy(condition = cond)), { onTap(d.id) },
                            Modifier.widthIn(min = 130.dp).testTag("dish_${d.id}"),
                            highlighted = suggested?.id == d.id,
                            note = stringResource(if (cond == DishCondition.DIRTY) R.string.dish_dirty else R.string.dish_clean),
                        )
                    }
                }
            }
        }
        Text(stringResource(R.string.dishes_rack), style = MaterialTheme.typography.titleMedium)
        Box(Modifier.fillMaxWidth().heightIn(min = 120.dp).background(Color(0xFFE9D1A6), RoundedCornerShape(20.dp)).border(3.dp, SmritiColors.Wood, RoundedCornerShape(20.dp)).padding(10.dp).testTag("dish_rack")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.onRack.forEach { d -> Sprite(dishSprite(d), Modifier.size(96.dp).testTag("rack_${d.id}")) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolButton(stringResource(R.string.act_do_it), SmritiIcons.Help, onDoAll, Modifier.testTag("dishes_do_all"), enabled = !state.isDone)
            ToolButton(stringResource(R.string.act_start_over), SmritiIcons.Replay, onReset, Modifier.testTag("dishes_reset"))
        }
        Spacer(Modifier.width(1.dp))
    }
}

@Composable
fun ModeChip(label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp).semantics { stateDescription = if (on) "✓" else "" },
        shape = RoundedCornerShape(16.dp),
        color = if (on) SmritiColors.Leaf else SmritiColors.Paper,
        contentColor = if (on) Color.White else SmritiColors.Ink,
        border = BorderStroke(2.dp, SmritiColors.Outline),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
            Text((if (on) "✓ " else "") + label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}
