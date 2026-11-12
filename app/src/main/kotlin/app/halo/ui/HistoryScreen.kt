package app.halo.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.halo.data.HaloSettings
import app.halo.data.HistoryEntry
import app.halo.data.HistoryStore
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HistoryScreen(settings: HaloSettings, edit: EditGlobal, history: HistoryStore) {
    val g = settings.global
    val entries by history.entries.collectAsState()
    val startOfDay = remember { LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
    val today = entries.filter { it.time >= startOfDay }
    // The history keeps 200 entries; a full list that is all from today may have dropped some.
    val capped = entries.size >= HistoryStore.MAX_ENTRIES && today.size == entries.size
    val perApp = today.groupingBy { it.appLabel }.eachCount().entries.sortedByDescending { it.value }

    ScreenList("History", "Notifications Halo handled, kept only on this phone.") {
        item {
            PaperCard {
                CardTitle("Today")
                Text(
                    if (capped) "${today.size}+ notifications" else "${today.size} notifications",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )
                perApp.take(6).forEach { (app, n) -> CardBody("$app  $n") }
            }
        }
        item {
            PaperCard {
                ToggleRow("Keep history", "Record each notification Halo handles, up to the last 200.", g.historyEnabled) { on ->
                    edit { it.copy(historyEnabled = on) }
                }
                ToggleRow("Keep message text", "Off keeps only the app, sender and time.", g.historyKeepText) { on ->
                    edit { it.copy(historyKeepText = on) }
                }
                OutlinedButton(onClick = { history.clear() }) { Text("Clear history") }
            }
        }
        if (entries.isEmpty()) {
            item { CardBody("Nothing yet. Notifications show up here once Halo handles them.") }
        } else {
            // No item keys: two notifications can share a time, app and title.
            items(entries.take(100)) { entry -> HistoryRow(entry) }
        }
        item { AboutCards(edit) }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry) {
    PaperCard {
        Row {
            Column(Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                CardBody(entry.appLabel)
            }
            Spacer(Modifier.width(8.dp))
            CardBody(DateUtils.getRelativeTimeSpanString(entry.time).toString())
        }
        entry.text?.let { CardBody(it) }
    }
}
