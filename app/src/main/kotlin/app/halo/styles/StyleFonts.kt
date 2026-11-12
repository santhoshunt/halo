package app.halo.styles

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import app.halo.R

/** Bundled typefaces, all under the SIL Open Font License 1.1 (texts in assets/licenses). */
object StyleFonts {
    val Pixel = FontFamily(Font(R.font.silkscreen))
    val Typewriter = FontFamily(Font(R.font.courier_prime))
    val Hand = FontFamily(Font(R.font.patrick_hand))
    val Condensed = FontFamily(Font(R.font.oswald))
    val Block = FontFamily(Font(R.font.bungee))
    val Display = FontFamily(Font(R.font.abril_fatface))
    val Terminal = FontFamily(Font(R.font.vt323))

    val names = listOf("Silkscreen", "Courier Prime", "Patrick Hand", "Oswald", "Bungee", "Abril Fatface", "VT323")
}
