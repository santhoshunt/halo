package app.halo.styles

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.halo.data.StyleId
import kotlin.math.sin

enum class StyleLayout {
    /** Icon, title and app name on one row, message below. */
    CARD,
    /** Title and message on one slim line. */
    LINE,
    /** Like CARD, with a "> " prompt before the title. */
    TERMINAL,
    /** Like CARD, with a large capitalised title. */
    POSTER,
}

/** A decoration painted with the card. Cut-out decorations clear pixels so the app shows through. */
enum class Deco {
    NONE, SHADOW, FOLD, ZIGZAG, TORN, RULED, STAMP, HOLES, DOTS, BAND, ACCENT_BAND,
    SCANLINES, STRIPES, RAILS, BRACKETS, GRID, WAVES, WASH, SKETCH, BLOTS, UNDERLINE,
}

/**
 * Everything that makes one banner style. Text is always dark on a light container or light on
 * a dark one; colour goes on borders and decorations.
 */
data class StyleSpec(
    val container: Color,
    /** Set for a vertical gradient from [container] to this colour. */
    val container2: Color? = null,
    val border: Color = Color.Transparent,
    val borderWidth: Dp = 0.dp,
    val dashed: Boolean = false,
    val corner: Dp = 14.dp,
    val title: Color,
    val body: Color,
    val chip: Color,
    val font: FontFamily = FontFamily.Default,
    val bodyFont: FontFamily = FontFamily.Default,
    /** Some display fonts run small or large; these scale the user's text size. */
    val titleScale: Float = 1f,
    val bodyScale: Float = 1f,
    val uppercase: Boolean = false,
    val layout: StyleLayout = StyleLayout.CARD,
    val deco: Deco = Deco.NONE,
    val decoColor: Color = Color.Unspecified,
    /** Border and chips take the notifying app's own colour. */
    val accentBorder: Boolean = false,
)

private val Ink = Color(0xFF222222)
private val InkMuted = Color(0xFF5F5E5A)
private val InkLine = Color(0xFF444441)
private val Paper = Color(0xFFFFFDF8)
private val Stone = Color(0xFFF1EFE8)
private val Rule = Color(0xFFD9D4C7)
private val Light = Color(0xFFF0F4FF)
private val LightMuted = Color(0xFFAAB8D4)
private val Grey = Color(0xFF888780)
private val GreyLight = Color(0xFFB4B2A9)

fun specFor(style: StyleId): StyleSpec = when (style) {
    StyleId.QUILL -> StyleSpec(Paper, border = InkLine, borderWidth = 1.dp, title = Ink, body = InkMuted, chip = InkLine)
    StyleId.THREAD -> StyleSpec(Color.White, border = Rule, borderWidth = 1.dp, corner = 28.dp, title = Ink, body = InkMuted, chip = Rule, layout = StyleLayout.LINE)
    StyleId.HAIRLINE -> StyleSpec(Color.White, corner = 0.dp, title = Ink, body = InkMuted, chip = InkLine, deco = Deco.UNDERLINE, decoColor = Ink)
    StyleId.MONO_SLAB -> StyleSpec(Stone, corner = 6.dp, title = Ink, body = InkLine, chip = Grey, font = FontFamily.Monospace, bodyFont = FontFamily.Monospace)
    StyleId.GHOST -> StyleSpec(Color.White, border = Color(0xFFD3D1C7), borderWidth = 1.dp, corner = 20.dp, title = InkLine, body = Grey, chip = Color(0xFFD3D1C7))

    StyleId.STICKY -> StyleSpec(
        Color(0xFFFFF4B8), corner = 2.dp, title = Color(0xFF412402), body = Color(0xFF633806), chip = Color(0xFFBA7517),
        font = StyleFonts.Hand, bodyFont = StyleFonts.Hand, titleScale = 1.15f, bodyScale = 1.15f,
        deco = Deco.FOLD, decoColor = Color(0xFFF2D675),
    )
    StyleId.LETTERPRESS -> StyleSpec(Color(0xFFF4F1EA), border = GreyLight, borderWidth = 1.dp, corner = 4.dp, title = Ink, body = InkMuted, chip = Grey, font = StyleFonts.Display, bodyFont = FontFamily.Serif)
    StyleId.RECEIPT -> StyleSpec(
        Color(0xFFFBFAF6), corner = 0.dp, title = Ink, body = InkLine, chip = Grey,
        font = StyleFonts.Typewriter, bodyFont = StyleFonts.Typewriter, uppercase = true, deco = Deco.ZIGZAG,
    )
    StyleId.INDEX_CARD -> StyleSpec(
        Color.White, border = Rule, borderWidth = 1.dp, corner = 4.dp, title = Ink, body = InkLine, chip = GreyLight,
        font = StyleFonts.Typewriter, bodyFont = StyleFonts.Typewriter, deco = Deco.RULED,
    )
    StyleId.STAMP -> StyleSpec(
        Paper, border = Color(0xFF993C1D), borderWidth = 1.dp, corner = 0.dp, title = Color(0xFF712B13), body = Color(0xFF4A1B0C),
        chip = Color(0xFF993C1D), font = StyleFonts.Display, bodyFont = FontFamily.Serif, deco = Deco.STAMP,
    )

    StyleId.POP_BLOCK -> StyleSpec(
        Color(0xFFFAC775), border = Ink, borderWidth = 2.dp, corner = 0.dp, title = Ink, body = Ink, chip = Ink,
        font = StyleFonts.Block, titleScale = 0.9f, uppercase = true, deco = Deco.SHADOW,
    )
    StyleId.HALFTONE -> StyleSpec(
        Color.White, border = Ink, borderWidth = 2.dp, corner = 6.dp, title = Ink, body = InkLine, chip = Ink,
        font = StyleFonts.Condensed, deco = Deco.DOTS, decoColor = Color(0xFFED93B1),
    )
    StyleId.DUOTONE -> StyleSpec(
        Color(0xFFEEEDFE), corner = 12.dp, title = Color(0xFF26215C), body = Color(0xFF3C3489), chip = Color(0xFF534AB7),
        deco = Deco.BAND, decoColor = Color(0xFF534AB7),
    )
    StyleId.TORN_POSTER -> StyleSpec(Stone, corner = 0.dp, title = Ink, body = InkLine, chip = InkLine, font = StyleFonts.Condensed, uppercase = true, deco = Deco.TORN)
    StyleId.COMIC -> StyleSpec(Color.White, border = Ink, borderWidth = 3.dp, corner = 14.dp, title = Ink, body = InkLine, chip = Ink, font = StyleFonts.Block, titleScale = 0.9f, deco = Deco.SHADOW)

    StyleId.PHOSPHOR -> StyleSpec(
        Color(0xFF0B0F0B), border = Color(0xFF27500A), borderWidth = 1.dp, corner = 4.dp, title = Color(0xFF97C459), body = Color(0xFFC0DD97),
        chip = Color(0xFF3B6D11), font = StyleFonts.Terminal, bodyFont = StyleFonts.Terminal, titleScale = 1.3f, bodyScale = 1.3f,
        layout = StyleLayout.TERMINAL, deco = Deco.SCANLINES, decoColor = Color.Black,
    )
    StyleId.ARCADE -> StyleSpec(
        Color(0xFF1A1033), border = Color(0xFFD4537E), borderWidth = 2.dp, corner = 0.dp, title = Color(0xFFF4C0D1), body = Light,
        chip = Color(0xFFD4537E), font = StyleFonts.Pixel, titleScale = 0.85f,
    )
    StyleId.TELETEXT -> StyleSpec(
        Color.Black, corner = 0.dp, title = Color(0xFFFAC775), body = Light, chip = Color(0xFFFAC775),
        font = StyleFonts.Terminal, bodyFont = StyleFonts.Terminal, titleScale = 1.3f, bodyScale = 1.3f, uppercase = true,
    )
    StyleId.CASSETTE -> StyleSpec(
        Stone, border = GreyLight, borderWidth = 1.dp, corner = 8.dp, title = Ink, body = InkLine, chip = Grey,
        font = StyleFonts.Condensed, uppercase = true, deco = Deco.STRIPES,
    )
    StyleId.DOT_MATRIX -> StyleSpec(
        Color(0xFFFBFAF6), border = GreyLight, borderWidth = 1.dp, corner = 0.dp, title = Ink, body = InkLine, chip = Grey,
        font = StyleFonts.Typewriter, bodyFont = StyleFonts.Typewriter, deco = Deco.HOLES,
    )

    StyleId.LANTERN -> StyleSpec(Color(0xFF16181C), borderWidth = 2.dp, corner = 18.dp, title = Light, body = LightMuted, chip = LightMuted, accentBorder = true)
    StyleId.NEON_RAIL -> StyleSpec(
        Color(0xFF0F0F14), corner = 4.dp, title = Light, body = LightMuted, chip = Color(0xFF7F77DD),
        font = StyleFonts.Condensed, deco = Deco.RAILS, decoColor = Color(0xFF7F77DD),
    )
    StyleId.VISOR -> StyleSpec(
        Color(0xFF0F1420), corner = 2.dp, title = Color(0xFFB5D4F4), body = Light, chip = Color(0xFF378ADD),
        font = StyleFonts.Condensed, uppercase = true, deco = Deco.BRACKETS, decoColor = Color(0xFF378ADD),
    )
    StyleId.HOLO -> StyleSpec(
        Color(0xFF1B1F3A), container2 = Color(0xFF0F3A36), border = Color(0xFF5DCAA5), borderWidth = 1.dp, corner = 18.dp,
        title = Color(0xFFE1F5EE), body = Color(0xFF9FE1CB), chip = Color(0xFF5DCAA5),
    )
    StyleId.CIRCUIT -> StyleSpec(
        Color(0xFF04342C), border = Color(0xFF1D9E75), borderWidth = 1.dp, corner = 6.dp, title = Color(0xFFE1F5EE), body = Color(0xFF9FE1CB),
        chip = Color(0xFF1D9E75), font = FontFamily.Monospace, bodyFont = FontFamily.Monospace, deco = Deco.GRID, decoColor = Color(0xFF1D9E75),
    )

    StyleId.MOSS -> StyleSpec(Color(0xFFEAF3DE), border = Color(0xFF639922), borderWidth = 1.dp, corner = 18.dp, title = Color(0xFF173404), body = Color(0xFF27500A), chip = Color(0xFF639922))
    StyleId.DUSK -> StyleSpec(Color(0xFF3C3489), container2 = Color(0xFF72243E), corner = 18.dp, title = Color(0xFFFBEAF0), body = Color(0xFFF4C0D1), chip = Color(0xFFED93B1))
    StyleId.TIDE -> StyleSpec(
        Color(0xFFE6F1FB), corner = 16.dp, title = Color(0xFF042C53), body = Color(0xFF0C447C), chip = Color(0xFF378ADD),
        deco = Deco.WAVES, decoColor = Color(0xFF85B7EB),
    )
    StyleId.CAMPFIRE -> StyleSpec(
        Color(0xFF2C2C2A), container2 = Color(0xFF4A1B0C), border = Color(0xFFD85A30), borderWidth = 1.dp, corner = 16.dp,
        title = Color(0xFFFAECE7), body = Color(0xFFF5C4B3), chip = Color(0xFFD85A30),
    )

    StyleId.INK_WASH -> StyleSpec(
        Paper, corner = 8.dp, title = Ink, body = InkMuted, chip = InkLine, font = StyleFonts.Display, bodyFont = FontFamily.Serif,
        deco = Deco.WASH, decoColor = InkLine,
    )
    StyleId.GRAPHITE -> StyleSpec(
        Stone, border = InkLine, borderWidth = 1.dp, corner = 10.dp, title = Ink, body = InkLine, chip = InkLine,
        font = StyleFonts.Hand, bodyFont = StyleFonts.Hand, titleScale = 1.15f, bodyScale = 1.15f, deco = Deco.SKETCH, decoColor = InkLine,
    )
    StyleId.WATERCOLOR -> StyleSpec(Color.White, corner = 14.dp, title = Ink, body = InkMuted, chip = GreyLight, font = FontFamily.Serif, deco = Deco.BLOTS)
    StyleId.STITCH -> StyleSpec(
        Paper, border = Color(0xFF993C1D), borderWidth = 2.dp, dashed = true, corner = 10.dp, title = Color(0xFF4A1B0C), body = Color(0xFF712B13),
        chip = Color(0xFF993C1D), font = FontFamily.Serif, bodyFont = FontFamily.Serif,
    )

    StyleId.SLATE -> StyleSpec(Color(0xFF1E2227), border = Color(0xFF2E343B), borderWidth = 1.dp, corner = 18.dp, title = Light, body = LightMuted, chip = Color(0xFF4A525C))
    StyleId.MARQUEE -> StyleSpec(Ink, corner = 4.dp, title = Color.White, body = GreyLight, chip = Grey, font = StyleFonts.Block, layout = StyleLayout.POSTER)
    StyleId.SIGNAL -> StyleSpec(Color.White, border = Rule, borderWidth = 1.dp, corner = 10.dp, title = Ink, body = InkMuted, chip = GreyLight, deco = Deco.ACCENT_BAND)
}

/** Extra content padding a decoration needs so it never sits under text. */
fun StyleSpec.decoPadding(): PaddingValues = when (deco) {
    Deco.SHADOW -> PaddingValues(end = 4.dp, bottom = 4.dp)
    Deco.ZIGZAG, Deco.TORN, Deco.WAVES -> PaddingValues(bottom = 8.dp)
    Deco.STRIPES, Deco.RAILS -> PaddingValues(top = 8.dp)
    Deco.BAND, Deco.ACCENT_BAND -> PaddingValues(start = 8.dp)
    Deco.HOLES -> PaddingValues(start = 12.dp, end = 12.dp)
    Deco.STAMP -> PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
    Deco.RULED -> PaddingValues(start = 28.dp)
    else -> PaddingValues(0.dp)
}

/**
 * Paints the card, its decoration, the content, the border and any cut-outs. The offscreen
 * layer is what lets BlendMode.Clear punch real holes instead of painting a guessed colour.
 */
fun Modifier.styleSurface(spec: StyleSpec, accent: Color): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        val shadow = if (spec.deco == Deco.SHADOW) 4.dp.toPx() else 0f
        val w = size.width - shadow
        val h = size.height - shadow
        val r = spec.corner.toPx()
        val card = Size(w, h)
        if (shadow > 0f) {
            drawRoundRect(Ink, topLeft = Offset(shadow, shadow), size = card, cornerRadius = CornerRadius(r))
        }
        val fill = spec.container2?.let { Brush.verticalGradient(listOf(spec.container, it), endY = h) }
            ?: SolidColor(spec.container)
        drawRoundRect(fill, size = card, cornerRadius = CornerRadius(r))
        val outline = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(r))) }
        clipPath(outline) { paintBehind(spec, accent, w, h) }
        drawContent()
        if (spec.borderWidth > 0.dp) {
            val bw = spec.borderWidth.toPx()
            val dash = if (spec.dashed) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())) else null
            drawRoundRect(
                color = if (spec.accentBorder) accent else spec.border,
                topLeft = Offset(bw / 2, bw / 2),
                size = Size(w - bw, h - bw),
                cornerRadius = CornerRadius((r - bw / 2).coerceAtLeast(0f)),
                style = Stroke(bw, pathEffect = dash),
            )
        }
        paintCutouts(spec, w, h)
    }

private fun DrawScope.paintBehind(spec: StyleSpec, accent: Color, w: Float, h: Float) {
    val c = spec.decoColor
    when (spec.deco) {
        Deco.FOLD -> {
            val s = 18.dp.toPx()
            drawPath(Path().apply { moveTo(w - s, 0f); lineTo(w - s, s); lineTo(w, s); close() }, c)
        }
        Deco.RULED -> {
            val step = 20.dp.toPx()
            var y = 26.dp.toPx()
            while (y < h) {
                drawLine(Color(0xFFB5D4F4), Offset(0f, y), Offset(w, y), 1.dp.toPx())
                y += step
            }
            drawLine(Color(0xFFF09595), Offset(22.dp.toPx(), 0f), Offset(22.dp.toPx(), h), 1.dp.toPx())
        }
        Deco.DOTS -> {
            val step = 9.dp.toPx()
            var x = w
            while (x > w * 0.55f) {
                var y = 0f
                while (y < h * 0.6f) {
                    // Dots shrink away from the top-right corner.
                    val fade = 1f - ((w - x) / (w * 0.45f) + y / (h * 0.6f)) / 2f
                    if (fade > 0.1f) drawCircle(c, radius = 3.dp.toPx() * fade, center = Offset(x, y))
                    y += step
                }
                x -= step
            }
        }
        Deco.BAND -> drawRect(c, size = Size(8.dp.toPx(), h))
        Deco.ACCENT_BAND -> drawRect(accent, size = Size(6.dp.toPx(), h))
        Deco.SCANLINES -> {
            var y = 0f
            while (y < h) {
                drawLine(c.copy(alpha = 0.35f), Offset(0f, y), Offset(w, y), 1.dp.toPx())
                y += 3.dp.toPx()
            }
        }
        Deco.STRIPES -> {
            val band = 3.dp.toPx()
            listOf(Color(0xFFEF9F27), Color(0xFFD85A30), Color(0xFF993C1D)).forEachIndexed { i, stripe ->
                drawRect(stripe, topLeft = Offset(0f, i * band), size = Size(w, band))
            }
        }
        Deco.RAILS -> {
            listOf(3.dp.toPx(), h - 3.dp.toPx()).forEach { y ->
                drawLine(c.copy(alpha = 0.3f), Offset(0f, y), Offset(w, y), 6.dp.toPx())
                drawLine(c, Offset(0f, y), Offset(w, y), 2.dp.toPx())
            }
        }
        Deco.BRACKETS -> {
            val inset = 5.dp.toPx()
            val len = 12.dp.toPx()
            val stroke = 2.dp.toPx()
            listOf(
                Triple(Offset(inset, inset), 1f, 1f), Triple(Offset(w - inset, inset), -1f, 1f),
                Triple(Offset(inset, h - inset), 1f, -1f), Triple(Offset(w - inset, h - inset), -1f, -1f),
            ).forEach { (corner, dx, dy) ->
                drawLine(c, corner, corner + Offset(len * dx, 0f), stroke)
                drawLine(c, corner, corner + Offset(0f, len * dy), stroke)
            }
        }
        Deco.GRID -> {
            val step = 14.dp.toPx()
            var x = 0f
            while (x < w) { drawLine(c.copy(alpha = 0.18f), Offset(x, 0f), Offset(x, h), 1f); x += step }
            var y = 0f
            while (y < h) { drawLine(c.copy(alpha = 0.18f), Offset(0f, y), Offset(w, y), 1f); y += step }
        }
        Deco.WAVES -> {
            val wave = Path()
            val base = h - 6.dp.toPx()
            var x = 0f
            wave.moveTo(0f, base)
            while (x <= w) {
                wave.lineTo(x, base + sin(x / 10.dp.toPx()) * 3.dp.toPx())
                x += 2f
            }
            drawPath(wave, c, style = Stroke(2.dp.toPx()))
        }
        Deco.WASH -> {
            drawCircle(c.copy(alpha = 0.10f), radius = h * 0.75f, center = Offset(w * 0.12f, h * 0.65f))
            drawCircle(c.copy(alpha = 0.07f), radius = h * 0.45f, center = Offset(w * 0.3f, h * 0.2f))
        }
        Deco.SKETCH -> {
            val r = spec.corner.toPx()
            listOf(Offset(1.5.dp.toPx(), -1.dp.toPx()), Offset(-1.dp.toPx(), 1.5.dp.toPx())).forEach { shift ->
                drawRoundRect(
                    c.copy(alpha = 0.55f), topLeft = shift + Offset(2.dp.toPx(), 2.dp.toPx()),
                    size = Size(w - 4.dp.toPx(), h - 4.dp.toPx()), cornerRadius = CornerRadius(r), style = Stroke(1.dp.toPx()),
                )
            }
        }
        Deco.BLOTS -> {
            drawCircle(Color(0xFFF4C0D1).copy(alpha = 0.35f), radius = h * 0.55f, center = Offset(w * 0.85f, h * 0.2f))
            drawCircle(Color(0xFF9FE1CB).copy(alpha = 0.30f), radius = h * 0.45f, center = Offset(w * 0.65f, h * 0.95f))
            drawCircle(Color(0xFFFAC775).copy(alpha = 0.25f), radius = h * 0.35f, center = Offset(w * 0.05f, h * 0.1f))
        }
        Deco.UNDERLINE -> drawLine(c, Offset(0f, h - 1.dp.toPx()), Offset(w, h - 1.dp.toPx()), 1.5.dp.toPx())
        Deco.NONE, Deco.SHADOW, Deco.ZIGZAG, Deco.TORN, Deco.STAMP, Deco.HOLES -> Unit
    }
}

private fun DrawScope.paintCutouts(spec: StyleSpec, w: Float, h: Float) {
    when (spec.deco) {
        Deco.FOLD -> {
            val s = 18.dp.toPx()
            clear(Path().apply { moveTo(w - s, 0f); lineTo(w, 0f); lineTo(w, s); close() })
        }
        Deco.ZIGZAG -> clear(teeth(w, h, width = 8.dp.toPx()) { 5.dp.toPx() })
        // Uneven teeth from a fixed pattern, so the tear looks the same on every banner.
        Deco.TORN -> clear(teeth(w, h, width = 11.dp.toPx()) { i -> (3 + (i * 7 % 5) * 1.5f).dp.toPx() })
        Deco.STAMP -> {
            val step = 10.dp.toPx()
            val r = 3.dp.toPx()
            var x = step / 2
            while (x < w) { punch(Offset(x, 0f), r); punch(Offset(x, h), r); x += step }
            var y = step / 2
            while (y < h) { punch(Offset(0f, y), r); punch(Offset(w, y), r); y += step }
        }
        Deco.HOLES -> {
            val step = 14.dp.toPx()
            var y = step / 2
            while (y < h) {
                punch(Offset(7.dp.toPx(), y), 3.dp.toPx())
                punch(Offset(w - 7.dp.toPx(), y), 3.dp.toPx())
                y += step
            }
        }
        else -> Unit
    }
}

/** Triangles pointing up from the bottom edge; [depth] gives each tooth's height. */
private fun teeth(w: Float, h: Float, width: Float, depth: (Int) -> Float): Path = Path().apply {
    var x = 0f
    var i = 0
    while (x < w) {
        moveTo(x, h)
        lineTo(x + width / 2, h - depth(i))
        lineTo(x + width, h)
        close()
        x += width
        i++
    }
}

private fun DrawScope.clear(path: Path) = drawPath(path, Color.Black, blendMode = BlendMode.Clear)

private fun DrawScope.punch(center: Offset, radius: Float) =
    drawCircle(Color.Black, radius = radius, center = center, blendMode = BlendMode.Clear)
