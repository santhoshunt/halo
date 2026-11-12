package app.halo.ui

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun AboutScreen(edit: EditGlobal) {
    val context = LocalContext.current
    val version = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    }
    ScreenList("About", "Halo $version. Works offline, collects nothing.") {
        item {
            PaperCard {
                CardTitle("Updates")
                CardBody("New versions are published as GitHub releases. Download the APK there and install it over this one.")
                OutlinedButton(onClick = { context.openUrl(RELEASES_URL) }) { Text("Check for updates") }
            }
        }
        item {
            PaperCard {
                CardTitle("Setup")
                CardBody("Go back through the permission checks and the system pop-up guide.")
                OutlinedButton(onClick = { edit { it.copy(setupDone = false) } }) { Text("Replay setup") }
            }
        }
    }
}
