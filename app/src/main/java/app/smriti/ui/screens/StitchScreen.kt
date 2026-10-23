package app.smriti.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.smriti.R
import app.smriti.domain.Catalog
import app.smriti.domain.Creation
import app.smriti.domain.SewKind
import app.smriti.domain.SewPhase
import app.smriti.domain.SewingState
import app.smriti.ui.components.BigButton
import app.smriti.ui.components.Panel
import app.smriti.ui.components.SmritiIcons
import app.smriti.ui.components.Sprite
import app.smriti.ui.components.ToolButton
import app.smriti.ui.theme.LocalSmritiPrefs
import app.smriti.ui.theme.SmritiColors

@Composable
fun sewKindLabel(kind: SewKind) = stringResource(
    when (kind) {
        SewKind.BAG -> R.string.sew_bag
        SewKind.PILLOW -> R.string.sew_pillow
        SewKind.SHIRT -> R.string.sew_shirt
    }
)

/** Stitch & Create. Few, large taps; no timing or grading; leaving keeps the progress. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StitchScreen(
    state: SewingState,
    fabrics: List<String>,
    highlight: String?,
    onFabric: (String) -> Unit,
    onKind: (SewKind) -> Unit,
    onStitch: () -> Unit,
    onShowMe: () -> Unit,
    onDoIt: () -> Unit,
    onUndo: () -> Unit,
    onStartOver: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(SmritiColors.Cream)) {
        val wide = maxWidth > 700.dp
        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (state.phase) {
                SewPhase.CHOOSE_FABRIC -> {
                    Title(stringResource(R.string.sew_choose_fabric))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = if (wide) 4 else 2) {
                        fabrics.forEach { fid ->
                            val item = Catalog.item(fid)
                            PictureCard(
                                item?.let { stringResource(it.label) } ?: fid, "sewing/swatch_$fid.webp", { onFabric(fid) },
                                Modifier.weight(1f).testTag("fabric_$fid"),
                                note = stringResource(if (fid == Catalog.DEFAULT_FABRIC) R.string.sew_at_home else R.string.sew_from_market),
                            )
                        }
                    }
                }
                SewPhase.CHOOSE_ITEM -> {
                    Title(stringResource(R.string.sew_choose_item))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 3) {
                        SewKind.entries.forEach { k ->
                            PictureCard(
                                sewKindLabel(k), "sewing/${k.name.lowercase()}_${state.fabric}.webp", { onKind(k) },
                                Modifier.weight(1f).testTag("kind_${k.name.lowercase()}"), imageSize = 110.dp,
                            )
                        }
                    }
                }
                SewPhase.STITCHING, SewPhase.FINISHED -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                  val picture: @Composable () -> Unit = {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        Machine(running = state.phase == SewPhase.STITCHING)
                        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                            // The outline is the template; the fabric piece appears as the stitching progresses.
                            Sprite("sewing/template_${(state.kind ?: SewKind.BAG).name.lowercase()}.webp", Modifier.size(200.dp))
                            Sprite(
                                "sewing/${(state.kind ?: SewKind.BAG).name.lowercase()}_${state.fabric}.webp",
                                Modifier.size(200.dp).graphicsLayer { alpha = 0.25f + 0.75f * state.progress }.testTag("sew_piece"),
                            )
                        }
                    }
                  }
                  val kind = state.kind ?: SewKind.BAG
                  if (wide) picture()
                  Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Title(if (state.phase == SewPhase.FINISHED) stringResource(R.string.sew_done) else sewKindLabel(kind))
                    if (!wide) picture()
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().height(14.dp),
                        color = SmritiColors.Leaf,
                    )
                    Text(stringResource(R.string.sew_progress, state.stitches, SewingState.STITCHES_NEEDED), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("sew_progress"))
                    if (state.phase == SewPhase.STITCHING) {
                        BigButton(
                            stringResource(R.string.sew_stitch), onStitch,
                            Modifier.fillMaxWidth().testTag("sew_stitch").then(if (highlight == "stitch") Modifier.border(6.dp, SmritiColors.Brass, RoundedCornerShape(18.dp)) else Modifier),
                            SmritiIcons.Next, color = SmritiColors.Leaf,
                        )
                    } else {
                        BigButton(stringResource(R.string.sew_keep), onKeep, Modifier.fillMaxWidth().testTag("sew_keep"), SmritiIcons.Add, color = SmritiColors.Leaf)
                    }
                  }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolButton(stringResource(R.string.act_show_me), SmritiIcons.Eye, onShowMe, Modifier.testTag("sew_show_me"), enabled = state.phase == SewPhase.STITCHING)
                ToolButton(stringResource(R.string.act_do_it), SmritiIcons.Help, onDoIt, Modifier.testTag("sew_do_it"), enabled = state.phase != SewPhase.FINISHED)
                ToolButton(stringResource(R.string.act_undo), SmritiIcons.Back, onUndo, Modifier.testTag("sew_undo"), enabled = state.phase != SewPhase.CHOOSE_FABRIC)
                ToolButton(stringResource(R.string.act_start_over), SmritiIcons.Replay, onStartOver, Modifier.testTag("sew_start_over"))
            }
        }
    }
}

/** The sewing machine with a gently bobbing needle while the person stitches (off with reduced motion). */
@Composable
private fun Machine(running: Boolean) {
    val reduced = LocalSmritiPrefs.current.reducedMotion
    val t = rememberInfiniteTransition(label = "machine")
    val bob by t.animateFloat(0f, 4f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "bob")
    val move = running && !reduced
    Sprite("props/sewing_machine.webp", Modifier.size(150.dp).graphicsLayer { translationY = if (move) bob else 0f })
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("sew_title"))
}

/** The rack of things the person has made, newest first. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreationsGallery(creations: List<Creation>, modifier: Modifier = Modifier) {
    Panel(modifier.widthIn(max = 760.dp).testTag("creations_panel")) {
        Text(stringResource(R.string.creations_title), style = MaterialTheme.typography.headlineSmall)
        if (creations.isEmpty()) {
            Text(stringResource(R.string.creations_empty), style = MaterialTheme.typography.bodyLarge)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                creations.asReversed().forEach { c ->
                    Column(
                        Modifier.background(SmritiColors.PaperDim, RoundedCornerShape(16.dp)).border(2.dp, SmritiColors.Outline, RoundedCornerShape(16.dp)).padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Sprite(c.sprite, Modifier.size(110.dp).testTag("creation_${c.id}"))
                        Text(sewKindLabel(c.kind), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
