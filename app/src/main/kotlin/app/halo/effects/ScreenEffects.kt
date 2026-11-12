package app.halo.effects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import app.halo.data.EffectId
import kotlin.math.sin
import kotlin.random.Random

private const val FADE_MS = 400L

/** One particle. Its position is a pure function of elapsed time, so no physics state is kept. */
private data class Particle(
    val x: Float,          // start, as a fraction of the screen width
    val offset: Float,     // start offset along the fall, as a fraction of the height
    val speed: Float,      // screen heights per second (petals) or initial vertical speed (confetti)
    val vx: Float,         // confetti: screen widths per second
    val sway: Float,       // petals: sideways swing, fraction of width
    val phase: Float,
    val spin: Float,       // degrees per second
    val size: Float,       // dp
    val color: Color,
)

private val PetalColors = listOf(Color(0xFFF4C0D1), Color(0xFFFBEAF0), Color(0xFFED93B1), Color(0xFFFFFDF8))
private val ConfettiColors = listOf(
    Color(0xFFE24B4A), Color(0xFFEF9F27), Color(0xFF639922),
    Color(0xFF378ADD), Color(0xFF7F77DD), Color(0xFFD4537E),
)

private fun particlesFor(effect: EffectId, intensity: Int, seed: Int): List<Particle> {
    val random = Random(seed)
    val base = when (effect) {
        EffectId.PETALS -> 40
        EffectId.CONFETTI -> 90
        else -> 0
    }
    val colors = if (effect == EffectId.PETALS) PetalColors else ConfettiColors
    return List(base * intensity.coerceIn(30, 100) / 100) {
        Particle(
            x = random.nextFloat(),
            offset = random.nextFloat(),
            speed = if (effect == EffectId.PETALS) 0.08f + random.nextFloat() * 0.10f else 0.15f + random.nextFloat() * 0.6f,
            vx = (random.nextFloat() - 0.5f) * 1.2f,
            sway = 0.02f + random.nextFloat() * 0.05f,
            phase = random.nextFloat() * 6.28f,
            spin = (random.nextFloat() - 0.5f) * 360f,
            size = if (effect == EffectId.PETALS) 10f + random.nextFloat() * 8f else 5f + random.nextFloat() * 5f,
            color = colors[random.nextInt(colors.size)],
        )
    }
}

/**
 * Full-screen animated effect, drawn behind the banners. It plays for [runMs], then fades out.
 * Restarts whenever [runId] changes.
 */
@Composable
fun ScreenEffect(effect: EffectId, accent: Color, intensity: Int, runId: Int, runMs: Long) {
    var elapsed by remember(runId) { mutableLongStateOf(0L) }
    val particles = remember(runId, effect, intensity) { particlesFor(effect, intensity, runId) }
    LaunchedEffect(runId) {
        val start = withFrameNanos { it }
        while (elapsed <= runMs + FADE_MS) {
            elapsed = (withFrameNanos { it } - start) / 1_000_000
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        // Reading elapsed here, in the draw phase, redraws each frame without recomposing.
        val ms = elapsed
        val fade = when {
            ms < FADE_MS -> ms / FADE_MS.toFloat()
            ms > runMs -> 1f - ((ms - runMs) / FADE_MS.toFloat()).coerceIn(0f, 1f)
            else -> 1f
        }
        val t = ms / 1000f
        when (effect) {
            EffectId.EDGE_GLOW -> drawEdgeGlow(accent, t, fade)
            EffectId.PETALS -> drawPetals(particles, t, fade)
            EffectId.CONFETTI -> drawConfetti(particles, t, fade)
            EffectId.NONE -> Unit
        }
    }
}

private fun DrawScope.drawEdgeGlow(accent: Color, t: Float, fade: Float) {
    val pulse = 0.55f + 0.45f * sin(t * 3f)
    val corner = CornerRadius(32.dp.toPx(), 32.dp.toPx())
    val wide = 18.dp.toPx()
    val thin = 4.dp.toPx()
    drawRoundRect(
        color = accent.copy(alpha = 0.35f * pulse * fade),
        topLeft = Offset(wide / 2, wide / 2),
        size = Size(size.width - wide, size.height - wide),
        cornerRadius = corner,
        style = Stroke(wide),
    )
    drawRoundRect(
        color = accent.copy(alpha = pulse * fade),
        topLeft = Offset(thin / 2, thin / 2),
        size = Size(size.width - thin, size.height - thin),
        cornerRadius = corner,
        style = Stroke(thin),
    )
}

private fun DrawScope.drawPetals(particles: List<Particle>, t: Float, fade: Float) {
    particles.forEach { p ->
        // Petals loop from just above the top to just below the bottom.
        val fall = ((p.offset + t * p.speed) % 1.2f) - 0.1f
        val x = (p.x + sin(t * 1.5f + p.phase) * p.sway) * size.width
        val y = fall * size.height
        val w = p.size.dp.toPx()
        val h = w * 0.6f
        rotate(degrees = p.phase * 57f + t * p.spin, pivot = Offset(x, y)) {
            drawOval(p.color.copy(alpha = 0.9f * fade), topLeft = Offset(x - w / 2, y - h / 2), size = Size(w, h))
        }
    }
}

private fun DrawScope.drawConfetti(particles: List<Particle>, t: Float, fade: Float) {
    val gravity = 0.6f * size.height
    particles.forEach { p ->
        // One burst from the top centre, then gravity pulls each piece down.
        val x = size.width * (0.5f + p.vx * t * 0.5f + (p.x - 0.5f) * 0.1f)
        val y = -20f + p.speed * size.height * t + 0.5f * gravity * t * t
        if (y > size.height + 40f) return@forEach
        val w = p.size.dp.toPx()
        rotate(degrees = p.phase * 57f + t * p.spin * 2f, pivot = Offset(x, y)) {
            drawRect(p.color.copy(alpha = fade), topLeft = Offset(x - w / 2, y - w), size = Size(w, w * 2f))
        }
    }
}
