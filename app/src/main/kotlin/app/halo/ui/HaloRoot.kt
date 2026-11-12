package app.halo.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import app.halo.R
import app.halo.data.GlobalSettings
import app.halo.data.HistoryStore
import app.halo.data.SettingsRepository
import app.halo.overlay.OverlayController
import kotlinx.coroutines.launch

/** Applies a change to the global settings. */
typealias EditGlobal = ((GlobalSettings) -> GlobalSettings) -> Unit

enum class Tab(val label: String, @param:DrawableRes val icon: Int) {
    STYLES("Styles", R.drawable.ic_tab_styles),
    CUSTOMIZE("Customize", R.drawable.ic_tab_customize),
    FOCUS("Focus", R.drawable.ic_tab_focus),
    APPS("Apps", R.drawable.ic_tab_apps),
    HISTORY("History", R.drawable.ic_tab_history),
}

@Composable
fun HaloRoot(repo: SettingsRepository, overlay: OverlayController, history: HistoryStore) {
    // Null until DataStore has loaded, so Setup never flashes for someone who finished it.
    val loaded by repo.settings.collectAsState(initial = null)
    val settings = loaded ?: return
    val scope = rememberCoroutineScope()
    val edit: EditGlobal = { transform -> scope.launch { repo.updateGlobal(transform) } }

    if (!settings.global.setupDone) {
        SetupScreen(edit)
        return
    }

    var tab by rememberSaveable { mutableStateOf(Tab.STYLES) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(painterResource(t.icon), contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.STYLES -> StylesScreen(settings, edit, overlay)
                Tab.CUSTOMIZE -> CustomizeScreen(settings, edit, overlay)
                Tab.FOCUS -> FocusScreen(settings, edit)
                Tab.APPS -> AppsScreen(settings, repo)
                Tab.HISTORY -> HistoryScreen(settings, edit, history)
            }
        }
    }
}
