package app.halo.ui

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.halo.data.HaloSettings
import app.halo.data.UnlockMode
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun FocusScreen(settings: HaloSettings, edit: EditGlobal) {
    val context = LocalContext.current
    val g = settings.global
    val perms = rememberPermissionState()

    fun pickTime(current: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            current / 60,
            current % 60,
            DateFormat.is24HourFormat(context),
        ).show()
    }

    ScreenList("Focus", "When Halo stays quiet.") {
        item {
            PaperCard {
                ToggleRow("Show Halo banners", "Turn off to pause every banner without losing your settings.", g.enabled) { on ->
                    edit { it.copy(enabled = on) }
                }
            }
        }
        item {
            PaperCard {
                ToggleRow("Quiet hours", "No banners during a daily window. Ringing calls still show.", g.quietHours) { on ->
                    edit { it.copy(quietHours = on) }
                }
                if (g.quietHours) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pickTime(g.quietStartMin) { m -> edit { it.copy(quietStartMin = m) } } }) {
                            Text("From ${clock(g.quietStartMin)}")
                        }
                        OutlinedButton(onClick = { pickTime(g.quietEndMin) { m -> edit { it.copy(quietEndMin = m) } } }) {
                            Text("To ${clock(g.quietEndMin)}")
                        }
                    }
                }
            }
        }
        item {
            PaperCard {
                CardTitle("While the phone is locked")
                CardBody(
                    when (g.whileLocked) {
                        UnlockMode.SUMMARY -> "On unlock, one banner lists what arrived."
                        UnlockMode.LATEST -> "On unlock, only the latest notification shows."
                        UnlockMode.ALL -> "On unlock, every notification shows, up to three at a time."
                        UnlockMode.NONE -> "Notifications that arrive while locked never show as banners."
                    },
                )
                ChoiceRow(UnlockMode.entries, g.whileLocked, { it.label }) { mode -> edit { it.copy(whileLocked = mode) } }
            }
        }
        if (settings.anyQuietApps() && !perms.usageAccess) {
            item {
                PaperCard {
                    CardTitle("Quiet apps need Usage access")
                    CardBody("Halo needs Usage access to see which app is open. Without it, quiet apps still get banners.")
                    OutlinedButton(onClick = { context.openUsageAccessSettings() }) { Text("Open settings") }
                }
            }
        }
        item {
            PaperCard {
                CardBody("Choose quiet apps, such as games and video players, in the Apps tab.")
            }
        }
        item {
            PaperCard {
                ToggleRow("Respect Do Not Disturb", "No banners for notifications that Do Not Disturb would silence.", g.respectDnd) { on ->
                    edit { it.copy(respectDnd = on) }
                }
            }
        }
        item {
            PaperCard {
                ToggleRow("Skip silent notifications", "No banners for low-priority notifications that don't make a sound.", g.skipSilent) { on ->
                    edit { it.copy(skipSilent = on) }
                }
            }
        }
        item {
            PaperCard {
                ToggleRow("Clear original on swipe", "Swiping a banner away also clears the notification from the shade.", g.dismissOriginalOnSwipe) { on ->
                    edit { it.copy(dismissOriginalOnSwipe = on) }
                }
            }
        }
    }
}

private fun clock(minuteOfDay: Int): String =
    LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
