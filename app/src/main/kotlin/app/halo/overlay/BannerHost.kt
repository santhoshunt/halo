package app.halo.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import app.halo.data.StyleId
import app.halo.notify.Banner
import app.halo.notify.BannerAction
import app.halo.styles.StyledBanner
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.launch

data class BannerUi(val maxLines: Int, val fontSizeSp: Int, val hideContent: Boolean)

class BannerCallbacks(
    val onTap: () -> Unit,
    val onAction: (BannerAction) -> Unit,
    val onReplyOpen: (BannerAction) -> Unit,
    val onReplySend: (BannerAction, String) -> Unit,
    val onDismiss: () -> Unit,
)

/** The banner as it sits in the overlay: slide-in, horizontal swipe to dismiss, tap to open. */
@Composable
fun BannerHost(banner: Banner, style: StyleId, ui: BannerUi, replyTo: BannerAction?, callbacks: BannerCallbacks) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val enter = remember { Animatable(0f) }
    var widthPx by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(220)) }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .graphicsLayer {
                val swipe = (abs(offsetX.value) / widthPx).coerceIn(0f, 1f)
                alpha = enter.value * (1f - swipe)
                translationX = offsetX.value
                translationY = (1f - enter.value) * -24.dp.toPx()
            }
            .draggable(
                state = rememberDraggableState { delta -> scope.launch { offsetX.snapTo(offsetX.value + delta) } },
                orientation = Orientation.Horizontal,
                enabled = replyTo == null,
                onDragStopped = { velocity ->
                    if (abs(offsetX.value) > widthPx * 0.35f || abs(velocity) > 2000f) {
                        val direction = if (offsetX.value != 0f) sign(offsetX.value) else sign(velocity)
                        offsetX.animateTo(direction * widthPx, tween(150))
                        callbacks.onDismiss()
                    } else {
                        offsetX.animateTo(0f)
                    }
                },
            )
            .clickable(interactionSource = null, indication = null, enabled = replyTo == null) { callbacks.onTap() },
    ) {
        StyledBanner(
            style = style,
            banner = banner,
            ui = ui,
            replyTo = replyTo,
            onAction = { action ->
                if (action.remoteInput != null) callbacks.onReplyOpen(action) else callbacks.onAction(action)
            },
            onReplySend = callbacks.onReplySend,
        )
    }
}
