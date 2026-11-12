package app.halo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ivory = Color(0xFFF6F3EC)
private val Paper = Color(0xFFFFFDF8)
private val IvoryDeep = Color(0xFFE9E4D8)
private val IvoryMid = Color(0xFFEFEBE2)
private val Ink = Color(0xFF222222)
private val InkMuted = Color(0xFF5F5E5A)
private val Rule = Color(0xFFD9D4C7)
private val InkAccent = Color(0xFF444441)

// Every role is set: Material 3 fills unset roles with its baseline purple, which switches,
// chips and the navigation bar would pick up.
private val MurmurColors = lightColorScheme(
    primary = InkAccent,
    onPrimary = Paper,
    primaryContainer = IvoryDeep,
    onPrimaryContainer = Ink,
    secondary = InkAccent,
    onSecondary = Paper,
    secondaryContainer = IvoryDeep,
    onSecondaryContainer = Ink,
    tertiary = InkAccent,
    onTertiary = Paper,
    tertiaryContainer = IvoryDeep,
    onTertiaryContainer = Ink,
    background = Ivory,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = IvoryMid,
    onSurfaceVariant = InkMuted,
    surfaceTint = Color.Transparent,
    surfaceBright = Paper,
    surfaceDim = IvoryMid,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Paper,
    surfaceContainer = Ivory,
    surfaceContainerHigh = IvoryMid,
    surfaceContainerHighest = IvoryDeep,
    outline = Rule,
    outlineVariant = Rule,
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    inversePrimary = Rule,
)

/** Halo's app look: ivory paper and ink, always light, whatever the system dark mode says. */
@Composable
fun MurmurTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MurmurColors, content = content)
}
