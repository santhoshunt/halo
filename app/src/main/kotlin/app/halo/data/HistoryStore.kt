package app.halo.data

import android.util.AtomicFile
import android.util.Log
import app.halo.notify.Banner
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val time: Long,
    val packageName: String,
    val appLabel: String,
    val title: String,
    /** Null unless "Keep message text" was on when the entry was recorded. */
    val text: String?,
)

/**
 * The last [MAX_ENTRIES] notifications Halo handled, newest first, kept in one JSON file in
 * Halo's private storage. Nothing leaves the phone.
 */
class HistoryStore(file: File, private val scope: CoroutineScope) {
    private val atomic = AtomicFile(file)
    // One writer at a time; each write saves a snapshot of the whole list.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val io = Dispatchers.IO.limitedParallelism(1)
    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    init {
        scope.launch(io) {
            val loaded = read()
            // A record may land before loading finishes; merge both and keep the newest.
            _entries.update { current -> (current + loaded).sortedByDescending { it.time }.take(MAX_ENTRIES) }
        }
    }

    fun record(banner: Banner, keepText: Boolean) {
        val entry = HistoryEntry(
            time = System.currentTimeMillis(),
            packageName = banner.packageName,
            appLabel = banner.appLabel.toString(),
            title = banner.title.toString(),
            text = if (keepText) banner.text.toString() else null,
        )
        _entries.update { (listOf(entry) + it).take(MAX_ENTRIES) }
        persist()
    }

    fun clear() {
        _entries.value = emptyList()
        persist()
    }

    private fun persist() {
        val snapshot = _entries.value
        scope.launch(io) { write(snapshot) }
    }

    private fun read(): List<HistoryEntry> = try {
        if (!atomic.baseFile.exists()) emptyList() else {
            val array = JSONArray(String(atomic.readFully()))
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                HistoryEntry(
                    time = o.getLong("time"),
                    packageName = o.getString("pkg"),
                    appLabel = o.getString("app"),
                    title = o.getString("title"),
                    text = if (o.has("text")) o.getString("text") else null,
                )
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "History file unreadable, starting empty", e)
        emptyList()
    }

    private fun write(list: List<HistoryEntry>) {
        val array = JSONArray()
        list.forEach { e ->
            array.put(
                JSONObject()
                    .put("time", e.time)
                    .put("pkg", e.packageName)
                    .put("app", e.appLabel)
                    .put("title", e.title)
                    .apply { if (e.text != null) put("text", e.text) },
            )
        }
        val out = atomic.startWrite()
        try {
            out.write(array.toString().toByteArray())
            atomic.finishWrite(out)
        } catch (e: Exception) {
            atomic.failWrite(out)
            Log.w(TAG, "History write failed", e)
        }
    }

    companion object {
        const val MAX_ENTRIES = 200
        private const val TAG = "HaloHistory"
    }
}
