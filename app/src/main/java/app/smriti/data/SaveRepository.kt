package app.smriti.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import app.smriti.domain.SaveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.InputStream
import java.io.OutputStream

interface SaveRepository {
    val data: Flow<SaveData>
    suspend fun load(): SaveData
    suspend fun update(transform: (SaveData) -> SaveData)
}

private object SaveDataSerializer : Serializer<SaveData> {
    override val defaultValue: SaveData = SaveData()

    override suspend fun readFrom(input: InputStream): SaveData =
        try {
            SaveCodec.decode(input.readBytes().decodeToString())
        } catch (e: Exception) {
            throw CorruptionException("Unreadable SMRITI save", e)
        }

    override suspend fun writeTo(t: SaveData, output: OutputStream) {
        output.write(SaveCodec.encode(t).encodeToByteArray())
    }
}

/** DataStore-backed local save in app-private storage. Nothing leaves the device. */
class DataStoreSaveRepository(context: Context, scope: CoroutineScope) : SaveRepository {
    private val store: DataStore<SaveData> = DataStoreFactory.create(
        serializer = SaveDataSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { SaveData() },
        scope = scope,
        produceFile = { File(context.filesDir, "datastore/smriti_save.json") },
    )

    override val data: Flow<SaveData> = store.data
    override suspend fun load(): SaveData = store.data.first()
    override suspend fun update(transform: (SaveData) -> SaveData) {
        store.updateData(transform)
    }
}

/** In-memory save used by tests. */
class InMemorySaveRepository(initial: SaveData = SaveData()) : SaveRepository {
    private val state = MutableStateFlow(initial)
    override val data: Flow<SaveData> = state
    override suspend fun load(): SaveData = state.value
    override suspend fun update(transform: (SaveData) -> SaveData) {
        state.value = transform(state.value)
    }
    val current: SaveData get() = state.value
}
