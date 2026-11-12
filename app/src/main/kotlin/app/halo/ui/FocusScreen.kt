package app.halo.ui

import androidx.compose.runtime.Composable
import app.halo.data.HaloSettings

@Composable
fun FocusScreen(settings: HaloSettings, edit: EditGlobal) {
    val g = settings.global
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
