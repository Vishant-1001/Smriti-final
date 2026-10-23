package app.smriti.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.smriti.R
import app.smriti.interactions.GameAction
import app.smriti.ui.components.BigButton
import app.smriti.ui.components.HudButton
import app.smriti.ui.components.SmritiIcons
import app.smriti.ui.components.rememberAssetBitmap
import app.smriti.ui.theme.SmritiColors

/** Calm start screen over a real illustration: a name, one clear Start action, settings. */
@Composable
fun TitleScreen(onAction: (GameAction) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(SmritiColors.Cream)) {
        val landscape = maxWidth > maxHeight
        val art = rememberAssetBitmap(if (landscape) "scenes/exterior_land.webp" else "scenes/exterior_port.webp")
        if (art != null) {
            Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.45f to Color.Transparent, 1f to SmritiColors.Ink.copy(alpha = 0.78f))),
        )
        Box(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
            Row(Modifier.align(Alignment.TopEnd), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HudButton(stringResource(R.string.action_settings), SmritiIcons.Settings, { onAction(GameAction.OpenSettings) }, Modifier.testTag("hud_settings"))
            }
            Column(
                Modifier.align(Alignment.BottomCenter).widthIn(max = 560.dp).fillMaxWidth().padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.title_name), style = MaterialTheme.typography.displaySmall, color = Color.White, textAlign = TextAlign.Center)
                Text(stringResource(R.string.title_tagline), style = MaterialTheme.typography.titleMedium, color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                BigButton(stringResource(R.string.action_start), { onAction(GameAction.Start) }, Modifier.fillMaxWidth().testTag("start"), SmritiIcons.Play)
            }
        }
    }
}
