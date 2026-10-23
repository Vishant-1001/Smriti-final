package app.smriti.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.smriti.R
import app.smriti.domain.MemoryBookState
import app.smriti.domain.MemoryPage
import app.smriti.ui.components.Badge
import app.smriti.ui.components.BigButton
import app.smriti.ui.components.QuietButton
import app.smriti.ui.components.SmritiIcons
import app.smriti.ui.components.loadFileBitmap
import app.smriti.ui.components.rememberAssetBitmap
import app.smriti.ui.theme.SmritiColors
import java.io.File

private val BookShape = RoundedCornerShape(22.dp)

/** Caption shown for a page: localised for samples, exactly as typed for personal pages. */
@Composable
fun memoryCaption(page: MemoryPage): String = when {
    page.isSample -> when (page.sampleKey) {
        "kolam" -> stringResource(R.string.memory_sample_kolam)
        "coffee" -> stringResource(R.string.memory_sample_coffee)
        "jasmine" -> stringResource(R.string.memory_sample_jasmine)
        else -> ""
    }
    else -> page.caption.orEmpty()
}

/**
 * A book of large pages. It is for looking and remembering together; it never asks the person
 * to name anyone or recall anything to continue.
 */
@Composable
fun MemoryBookScreen(
    book: MemoryBookState,
    photoFile: (String) -> File,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReadAloud: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(SmritiColors.Cream)) {
        val landscape = maxWidth > maxHeight
        val stacked = maxWidth < 600.dp
        val page = book.currentPage
        // Short landscape screens: page-turn buttons beside the book so the page keeps its height.
        val sideButtons = landscape && maxHeight < 520.dp
        val bookView: @Composable (Modifier) -> Unit = { m ->
            Box(
                    m
                        .shadow(12.dp, BookShape)
                        .clip(BookShape)
                        .background(SmritiColors.Maroon)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SmritiColors.Paper)
                        .testTag("memory_book"),
                ) {
                    if (page == null) {
                        Text(
                            stringResource(R.string.memory_empty),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        )
                    } else {
                        val caption = memoryCaption(page)
                        val image: ImageBitmap? = if (page.isSample) rememberAssetBitmap("memory/memory_sample_${page.sampleKey}.webp")
                        else page.photoFile?.let { name -> remember(name) { loadFileBitmap(photoFile(name)) } }
                        val missingPhoto = !page.isSample && page.photoFile != null && image == null
                        if (landscape) {
                            Row(Modifier.fillMaxSize()) {
                                PagePicture(image, missingPhoto, Modifier.weight(1f).fillMaxHeight().padding(20.dp))
                                Box(Modifier.width(3.dp).fillMaxHeight().background(SmritiColors.PaperDim))
                                PageText(page, caption, book, onReadAloud, Modifier.weight(1f).fillMaxHeight().padding(24.dp))
                            }
                        } else {
                            Column(Modifier.fillMaxSize()) {
                                PagePicture(image, missingPhoto, Modifier.fillMaxWidth().weight(1f).padding(16.dp))
                                PageText(page, caption, book, onReadAloud, Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp, vertical = 8.dp))
                            }
                        }
                    }
                }
        }
        val previous: @Composable (Modifier, Boolean) -> Unit = { m, st ->
            BigButton(stringResource(R.string.memory_previous), onPrevious, m.testTag("memory_previous"), SmritiIcons.Back, enabled = book.hasPrevious, stacked = st)
        }
        val next: @Composable (Modifier, Boolean) -> Unit = { m, st ->
            BigButton(stringResource(R.string.memory_next), onNext, m.testTag("memory_next"), SmritiIcons.Next, enabled = book.hasNext, stacked = st)
        }
        if (sideButtons) {
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                previous(Modifier.width(132.dp).fillMaxHeight(0.6f), true)
                bookView(Modifier.weight(1f).fillMaxHeight())
                next(Modifier.width(132.dp).fillMaxHeight(0.6f), true)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                bookView(Modifier.weight(1f).fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    previous(Modifier.weight(1f), stacked)
                    next(Modifier.weight(1f), stacked)
                }
            }
        }
    }
}

@Composable
private fun PagePicture(image: ImageBitmap?, missing: Boolean, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(
                image, contentDescription = null, contentScale = ContentScale.Fit,
                // Grow to the largest size that fits the page while keeping the picture's proportions.
                modifier = Modifier
                    .aspectRatio(image.width.toFloat() / image.height)
                    .clip(RoundedCornerShape(10.dp))
                    .border(3.dp, SmritiColors.Outline, RoundedCornerShape(10.dp)),
            )
        } else if (missing) {
            Text(stringResource(R.string.memory_photo_missing), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        } else {
            Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(10.dp)).background(SmritiColors.PaperDim))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PageText(page: MemoryPage, caption: String, book: MemoryBookState, onReadAloud: (String) -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (page.isSample) Badge(stringResource(R.string.memory_sample_badge), SmritiColors.Indigo, Modifier.testTag("sample_badge"))
            else Badge(stringResource(R.string.memory_personal_badge), SmritiColors.Leaf)
            Text(
                stringResource(R.string.memory_page_of, book.currentIndex + 1, book.pages.size),
                style = MaterialTheme.typography.labelMedium, color = SmritiColors.InkSoft,
            )
        }
        Text(caption, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("memory_caption"))
        if (page.isSample) {
            Text(stringResource(R.string.memory_sample_note), style = MaterialTheme.typography.bodyMedium, color = SmritiColors.InkSoft)
        }
        if (caption.isNotBlank()) {
            QuietButton(stringResource(R.string.memory_read_aloud), { onReadAloud(caption) }, icon = SmritiIcons.Speaker)
        }
    }
}
