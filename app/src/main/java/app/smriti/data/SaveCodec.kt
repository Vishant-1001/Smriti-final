package app.smriti.data

import app.smriti.domain.SaveData
import app.smriti.domain.SceneNavigator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

/**
 * Versioned JSON encoding of [SaveData]. Pure Kotlin so migrations are unit-tested.
 *
 * Policy: unknown fields are ignored, missing fields take safe defaults, unreadable files fall back
 * to a fresh save rather than crashing, and the result is always sanitised (valid navigation stack,
 * valid Memory Book index).
 */
object SaveCodec {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun encode(data: SaveData): String = json.encodeToString(SaveData.serializer(), data)

    fun decode(text: String): SaveData {
        if (text.isBlank()) return SaveData()
        val element = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return SaveData()
        val version = runCatching { element["schemaVersion"]?.jsonPrimitive?.int }.getOrNull() ?: 0
        val migrated = migrate(element, version)
        val data = runCatching { json.decodeFromJsonElement(SaveData.serializer(), migrated) }.getOrElse { return SaveData() }
        return sanitize(data)
    }

    /** Applies migrations step by step. Version 1 is the first released schema. */
    internal fun migrate(obj: JsonObject, fromVersion: Int): JsonObject {
        var current = obj
        var v = fromVersion
        while (v < SaveData.CURRENT_SCHEMA) {
            current = when (v) {
                // v0 = pre-release development saves; the shape is identical to v1.
                0 -> current
                else -> current
            }
            v++
        }
        return current
    }

    fun sanitize(data: SaveData): SaveData = data.copy(
        schemaVersion = SaveData.CURRENT_SCHEMA,
        nav = SceneNavigator.sanitize(data.nav),
        memoryBook = data.memoryBook.normalized(),
        selectedTrackId = data.selectedTrackId?.takeIf { id -> data.playlist.any { it.id == id } },
    )
}
