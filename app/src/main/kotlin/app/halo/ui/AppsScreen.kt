package app.halo.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import app.halo.data.AppRule
import app.halo.data.HaloSettings
import app.halo.data.SettingsRepository
import app.halo.data.StyleId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class AppEntry(val pkg: String, val label: String, val icon: ImageBitmap)

@Composable
fun AppsScreen(settings: HaloSettings, repo: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apps by produceState<List<AppEntry>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadApps(context) }
    }

    ScreenList("Apps", "Turn banners on or off per app, or give an app its own style.") {
        val list = apps
        if (list == null) {
            item { CardBody("Loading apps") }
        } else {
            items(list, key = { it.pkg }) { app ->
                AppRow(
                    app = app,
                    rule = settings.ruleFor(app.pkg),
                    globalStyle = settings.global.style,
                    onEnabled = { on -> scope.launch { repo.setAppEnabled(app.pkg, on) } },
                    onQuiet = { on -> scope.launch { repo.setAppQuiet(app.pkg, on) } },
                    onStyle = { style -> scope.launch { repo.setAppStyle(app.pkg, style) } },
                    onSystemSettings = { context.openAppNotificationSettings(app.pkg) },
                )
            }
        }
    }
}

@Composable
private fun AppRow(
    app: AppEntry,
    rule: AppRule,
    globalStyle: StyleId,
    onEnabled: (Boolean) -> Unit,
    onQuiet: (Boolean) -> Unit,
    onStyle: (StyleId?) -> Unit,
    onSystemSettings: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    PaperCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(app.icon, contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.bodyLarge)
                CardBody(rule.style?.let { "${it.label} style" } ?: "Global style, ${globalStyle.label}")
            }
            Switch(checked = rule.enabled, onCheckedChange = onEnabled)
        }
        ToggleRow("Quiet while open", "No banners while this app is on screen.", rule.quietWhenOpen, onQuiet)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                OutlinedButton(onClick = { menuOpen = true }) { Text("Style") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Global style") }, onClick = { menuOpen = false; onStyle(null) })
                    StyleId.entries.forEach { style ->
                        DropdownMenuItem(text = { Text(style.label) }, onClick = { menuOpen = false; onStyle(style) })
                    }
                }
            }
            TextButton(onClick = onSystemSettings) { Text("System settings") }
        }
    }
}

private fun loadApps(context: Context): List<AppEntry> {
    val pm = context.packageManager
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(launcher, PackageManager.ResolveInfoFlags.of(0))
        .distinctBy { it.activityInfo.packageName }
        .filter { it.activityInfo.packageName != context.packageName }
        .map { info ->
            AppEntry(
                pkg = info.activityInfo.packageName,
                label = info.loadLabel(pm).toString(),
                icon = info.loadIcon(pm).toBitmap(72, 72).asImageBitmap(),
            )
        }
        .sortedBy { it.label.lowercase() }
}
