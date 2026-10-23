package app.smriti.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID

/**
 * Copies caregiver-chosen photos and songs into app-private storage so they keep working offline
 * and are never re-shared. Only system pickers are used, so no storage permission is requested.
 */
class MediaStorage(private val context: Context) {
    val photoDir: File get() = File(context.filesDir, "memory_photos").apply { mkdirs() }
    val radioDir: File get() = File(context.filesDir, "radio_tracks").apply { mkdirs() }

    fun photoFile(name: String): File = File(photoDir, name)
    fun radioFile(name: String): File = File(radioDir, name)

    /** Returns the stored file name, or null if the copy failed. */
    fun importPhoto(uri: Uri): String? = copyInto(uri, photoDir, ".img")

    /** Returns (stored file name, display title) or null if the copy failed. */
    fun importAudio(uri: Uri): Pair<String, String>? {
        val name = copyInto(uri, radioDir, ".audio") ?: return null
        val title = displayName(uri)?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: name
        return name to title
    }

    fun deletePhoto(name: String) {
        File(photoDir, name).delete()
    }

    fun deleteAudio(name: String) {
        File(radioDir, name).delete()
    }

    private fun copyInto(uri: Uri, dir: File, suffix: String): String? = runCatching {
        val name = UUID.randomUUID().toString() + suffix
        val target = File(dir, name)
        context.contentResolver.openInputStream(uri)!!.use { input -> target.outputStream().use { input.copyTo(it) } }
        name
    }.getOrNull()

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()
}
