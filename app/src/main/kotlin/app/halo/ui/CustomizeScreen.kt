package app.halo.ui

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.halo.data.BannerPosition
import app.halo.data.HaloSettings
import app.halo.overlay.BannerUi
import app.halo.overlay.OverlayController
import app.halo.styles.Sample
import app.halo.styles.StyledBanner
import app.halo.styles.sampleBanner
import kotlin.math.roundToInt

@Composable
fun CustomizeScreen(settings: HaloSettings, edit: EditGlobal, overlay: OverlayController) {
    val context = LocalContext.current
    val g = settings.global
    var sample by rememberSaveable { mutableStateOf(Sample.MESSAGE) }
    val preview = remember(sample) { sampleBanner(context, sample) }
    // Sliders keep a local value while dragging and save once on release.
    var duration by remember(g.durationMs) { mutableFloatStateOf(g.durationMs.toFloat()) }
    var fontSize by remember(g.fontSizeSp) { mutableFloatStateOf(g.fontSizeSp.toFloat()) }

    ScreenList("Customize", "Where banners sit, how long they stay and how much they show.") {
        item {
            PaperCard {
                CardTitle("Preview")
                ChoiceRow(Sample.entries, sample, { it.label }) { sample = it }
                StyledBanner(
                    style = g.style,
                    banner = preview,
                    ui = BannerUi(g.maxLines, fontSize.roundToInt(), g.hideContent),
                )
                OutlinedButton(onClick = { overlay.show(sampleBanner(context, sample)) }) { Text("Show on screen") }
            }
        }
        item {
            PaperCard {
                CardTitle("Position on screen")
                ChoiceRow(BannerPosition.entries, g.position, { it.label }) { p -> edit { it.copy(position = p) } }
            }
        }
        item {
            PaperCard {
                CardTitle("Time on screen")
                CardBody("%.1f seconds".format(duration / 1000f))
                Slider(
                    value = duration,
                    onValueChange = { duration = it },
                    valueRange = 2000f..10000f,
                    steps = 15,
                    onValueChangeFinished = { edit { it.copy(durationMs = duration.roundToInt()) } },
                )
            }
        }
        item {
            PaperCard {
                CardTitle("Message lines")
                ChoiceRow(listOf(1, 2, 3, 4), g.maxLines, { it.toString() }) { n -> edit { it.copy(maxLines = n) } }
            }
        }
        item {
            PaperCard {
                CardTitle("Text size")
                CardBody("${fontSize.roundToInt()} sp")
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 13f..18f,
                    steps = 4,
                    onValueChangeFinished = { edit { it.copy(fontSizeSp = fontSize.roundToInt()) } },
                )
            }
        }
        item {
            PaperCard {
                ToggleRow(
                    title = "Hide message content",
                    subtitle = "Shows only the app name and \"New notification\".",
                    checked = g.hideContent,
                ) { on -> edit { it.copy(hideContent = on) } }
            }
        }
    }
}
