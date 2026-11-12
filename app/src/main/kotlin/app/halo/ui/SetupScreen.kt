package app.halo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
fun SetupScreen(edit: EditGlobal) {
    val context = LocalContext.current
    val perms = rememberPermissionState()
    // A permission still off after the user visited its screen usually means restricted settings.
    var triedListener by rememberSaveable { mutableStateOf(false) }
    var triedOverlay by rememberSaveable { mutableStateOf(false) }
    val blocked = (triedListener && !perms.listener) || (triedOverlay && !perms.overlay)
    val buildLabel = context.applicationInfo.loadLabel(context.packageManager).toString()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        ScreenList("Set up $buildLabel", "Two permissions turn notifications into Halo banners.") {
            item {
                PermissionCard(
                    title = "Notification access",
                    body = "Lets Halo read incoming notifications and use their Reply and Mark as read buttons.",
                    granted = perms.listener,
                ) { triedListener = true; context.openListenerSettings() }
            }
            item {
                PermissionCard(
                    title = "Display over other apps",
                    body = "Lets Halo draw its banner on top of whatever app is open.",
                    granted = perms.overlay,
                ) { triedOverlay = true; context.openOverlaySettings() }
            }
            if (blocked) {
                item {
                    PaperCard {
                        CardTitle("Blocked by restricted settings")
                        CardBody(
                            "Android blocks these permissions for apps installed from a download. Open App info, " +
                                "tap the three-dot menu, choose Allow restricted settings, then come back and try again.",
                        )
                        OutlinedButton(onClick = { context.openAppInfo() }) { Text("Open App info") }
                    }
                }
            }
            item {
                PermissionCard(
                    title = "Keep Halo running",
                    body = "Battery savers can stop Halo in the background, and banners then stop appearing.",
                    granted = perms.batteryExempt,
                ) { context.openBatterySettings() }
            }
            item {
                PaperCard {
                    CardTitle("Turn off system pop-ups")
                    CardBody(
                        "Android still shows its own pop-up banner next to Halo's. In each messaging app's " +
                            "notification settings, turn off Pop on screen. The Apps tab has a shortcut for every app.",
                    )
                }
            }
            item {
                Button(onClick = { edit { it.copy(setupDone = true) } }, modifier = Modifier.fillMaxWidth()) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(title: String, body: String, granted: Boolean, onOpen: () -> Unit) {
    PaperCard {
        CardTitle(title)
        CardBody(body)
        if (granted) {
            Text("Allowed", style = MaterialTheme.typography.labelLarge)
        } else {
            OutlinedButton(onClick = onOpen) { Text("Open settings") }
        }
    }
}
