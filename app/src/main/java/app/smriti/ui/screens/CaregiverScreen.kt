package app.smriti.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.smriti.R
import app.smriti.domain.DefaultPlaylist
import app.smriti.domain.MemoryBookState
import app.smriti.domain.MemoryPage
import app.smriti.domain.RadioTrack
import app.smriti.ui.components.BigButton
import app.smriti.ui.components.Panel
import app.smriti.ui.components.QuietButton
import app.smriti.ui.components.SmritiIcons
import app.smriti.ui.theme.SmritiColors

/** Everything the caregiver screen can ask the app to do. */
interface CaregiverActions {
    fun importPhoto(uri: Uri): String?
    fun importAudio(uri: Uri): Pair<String, String>?
    fun addPage(caption: String, photoFile: String?)
    fun editCaption(id: String, caption: String)
    fun deletePage(id: String)
    fun removeSamples()
    fun restoreSamples()
    fun addTrack(fileName: String, title: String)
    fun removeTrack(id: String)
    fun restoreDemoTracks()
}

/**
 * Separate from casual play and opened deliberately from Settings, so the person cannot change
 * the playlist or Memory Book by accident.
 */
@Composable
fun CaregiverScreen(book: MemoryBookState, playlist: List<RadioTrack>, actions: CaregiverActions, modifier: Modifier = Modifier) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (!unlocked) {
        Column(modifier.fillMaxSize().background(SmritiColors.Cream).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Panel(Modifier.widthIn(max = 600.dp)) {
                Text(stringResource(R.string.scene_caregiver), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.caregiver_gate_body), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.caregiver_privacy), style = MaterialTheme.typography.bodyMedium, color = SmritiColors.InkSoft)
                Spacer(Modifier.height(16.dp))
                BigButton(stringResource(R.string.caregiver_gate_continue), { unlocked = true }, Modifier.fillMaxWidth().testTag("caregiver_continue"), color = SmritiColors.Indigo)
            }
        }
        return
    }

    var caption by rememberSaveable { mutableStateOf("") }
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var importFailed by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var editText by rememberSaveable { mutableStateOf("") }
    var confirmDeletePage by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmRemoveTrack by rememberSaveable { mutableStateOf<String?>(null) }

    // Photo Picker and the document picker need no storage permission.
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val stored = actions.importPhoto(uri)
            importFailed = stored == null
            if (stored != null) pendingPhoto = stored
        }
    }
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val stored = actions.importAudio(uri)
            importFailed = stored == null
            stored?.let { (file, title) -> actions.addTrack(file, title) }
        }
    }

    LazyColumn(
        modifier.fillMaxSize().background(SmritiColors.Cream).testTag("caregiver_tools"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(stringResource(R.string.caregiver_privacy), style = MaterialTheme.typography.bodyMedium, color = SmritiColors.InkSoft, modifier = Modifier.widthIn(max = 760.dp))
            if (importFailed) Text(stringResource(R.string.caregiver_import_failed), style = MaterialTheme.typography.bodyLarge, color = SmritiColors.Maroon)
        }
        item {
            Panel(Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
                Text(stringResource(R.string.caregiver_memory_header), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.caregiver_add_page), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = caption, onValueChange = { caption = it },
                    label = { Text(stringResource(R.string.caregiver_caption)) },
                    modifier = Modifier.fillMaxWidth().testTag("caregiver_caption"),
                    textStyle = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuietButton(
                        stringResource(if (pendingPhoto == null) R.string.caregiver_choose_photo else R.string.caregiver_photo_chosen),
                        { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        Modifier.weight(1f), SmritiIcons.Add,
                    )
                    BigButton(
                        stringResource(R.string.caregiver_save),
                        {
                            actions.addPage(caption, pendingPhoto)
                            caption = ""
                            pendingPhoto = null
                        },
                        Modifier.weight(1f).testTag("caregiver_save_page"),
                        enabled = caption.isNotBlank() || pendingPhoto != null,
                    )
                }
            }
        }
        if (book.pages.isEmpty()) item { Text(stringResource(R.string.caregiver_no_pages), style = MaterialTheme.typography.bodyLarge) }
        items(book.pages, key = { it.id }) { page ->
            PageRow(
                page = page,
                isEditing = editing == page.id,
                editText = editText,
                onEditText = { editText = it },
                onStartEdit = { editing = page.id; editText = page.caption.orEmpty() },
                onSaveEdit = { actions.editCaption(page.id, editText); editing = null },
                onDelete = { confirmDeletePage = page.id },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
                if (book.pages.any { it.isSample }) QuietButton(stringResource(R.string.caregiver_remove_samples), actions::removeSamples, Modifier.weight(1f).testTag("remove_samples"))
                if (MemoryBookState.DEFAULT_SAMPLE_PAGES.any { s -> book.pages.none { it.id == s.id } }) {
                    QuietButton(stringResource(R.string.caregiver_restore_samples), actions::restoreSamples, Modifier.weight(1f))
                }
            }
        }
        item {
            Panel(Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
                Text(stringResource(R.string.caregiver_radio_header), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.caregiver_song_rights), style = MaterialTheme.typography.bodyMedium, color = SmritiColors.InkSoft)
                Spacer(Modifier.height(10.dp))
                BigButton(stringResource(R.string.caregiver_add_song), { pickAudio.launch(arrayOf("audio/*")) }, Modifier.fillMaxWidth(), SmritiIcons.Add, color = SmritiColors.Indigo)
                Spacer(Modifier.height(10.dp))
                playlist.forEach { track ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(trackTitle(track), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        QuietButton(stringResource(R.string.caregiver_remove_song), { confirmRemoveTrack = track.id }, Modifier.testTag("remove_track_${track.id}"), SmritiIcons.Delete)
                    }
                }
                if (DefaultPlaylist.tracks.any { d -> playlist.none { it.id == d.id } }) {
                    QuietButton(stringResource(R.string.caregiver_restore_demo), actions::restoreDemoTracks, Modifier.fillMaxWidth())
                }
            }
        }
    }

    confirmDeletePage?.let { id ->
        ConfirmDialog(R.string.caregiver_delete_page_title, R.string.caregiver_delete_page_body, R.string.caregiver_delete,
            onConfirm = { actions.deletePage(id); confirmDeletePage = null }, onDismiss = { confirmDeletePage = null })
    }
    confirmRemoveTrack?.let { id ->
        ConfirmDialog(R.string.caregiver_remove_song_title, R.string.caregiver_remove_song_body, R.string.caregiver_remove_song,
            onConfirm = { actions.removeTrack(id); confirmRemoveTrack = null }, onDismiss = { confirmRemoveTrack = null })
    }
}

@Composable
private fun PageRow(
    page: MemoryPage,
    isEditing: Boolean,
    editText: String,
    onEditText: (String) -> Unit,
    onStartEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SmritiColors.Paper,
        border = BorderStroke(2.dp, SmritiColors.Outline),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val caption = memoryCaption(page)
            Text(if (page.isSample) stringResource(R.string.caregiver_sample_label, caption) else caption, style = MaterialTheme.typography.bodyLarge)
            if (isEditing) {
                OutlinedTextField(editText, onEditText, modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton(stringResource(R.string.caregiver_save), onSaveEdit)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!page.isSample) QuietButton(stringResource(R.string.caregiver_edit), onStartEdit, icon = SmritiIcons.Edit)
                    QuietButton(stringResource(R.string.caregiver_delete), onDelete, Modifier.testTag("delete_page_${page.id}"), SmritiIcons.Delete)
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialog(title: Int, body: Int, confirm: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title), style = MaterialTheme.typography.titleLarge) },
        text = { Text(stringResource(body), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { TextButton(onClick = onConfirm, modifier = Modifier.testTag("confirm")) { Text(stringResource(confirm), style = MaterialTheme.typography.labelLarge, color = SmritiColors.Maroon) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.caregiver_cancel), style = MaterialTheme.typography.labelLarge) } },
        containerColor = SmritiColors.Paper,
    )
}
