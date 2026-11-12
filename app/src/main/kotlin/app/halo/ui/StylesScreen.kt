package app.halo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.halo.data.StyleFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.halo.data.HaloSettings
import app.halo.data.StyleId
import app.halo.overlay.BannerUi
import app.halo.overlay.OverlayController
import app.halo.styles.Sample
import app.halo.styles.StyledBanner
import app.halo.styles.sampleBanner

@Composable
fun StylesScreen(settings: HaloSettings, edit: EditGlobal, overlay: OverlayController) {
    val context = LocalContext.current
    val sample = remember { sampleBanner(context, Sample.MESSAGE) }
    val g = settings.global
    val ui = BannerUi(g.maxLines, g.fontSizeSp, g.hideContent)
    // Null means every family.
    var family by rememberSaveable { mutableStateOf<StyleFamily?>(null) }
    val shown = StyleId.entries.filter { family == null || it.family == family }

    ScreenList("Styles", "${StyleId.entries.size} looks for every banner, unless an app has its own style.") {
        item {
            PaperCard {
                ToggleRow(
                    title = "Random style for each notification",
                    subtitle = "Picks one of the ${StyleId.entries.size} styles each time. An app's own style still wins.",
                    checked = g.randomStyle,
                ) { on -> edit { it.copy(randomStyle = on) } }
            }
        }
        item {
            ChoiceRow(listOf<StyleFamily?>(null) + StyleFamily.entries, family, { it?.label ?: "All" }) { family = it }
        }
        items(shown, key = { it.name }) { style ->
            PaperCard {
                CardTitle(style.label)
                CardBody("${style.family.label}. ${style.description}")
                StyledBanner(style = style, banner = sample, ui = ui)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (g.style == style) {
                        OutlinedButton(onClick = {}) { Text("In use") }
                    } else {
                        Button(onClick = { edit { it.copy(style = style) } }) { Text("Use") }
                    }
                    OutlinedButton(onClick = { overlay.show(sampleBanner(context, Sample.MESSAGE), style) }) {
                        Text("Show on screen")
                    }
                }
            }
        }
    }
}
