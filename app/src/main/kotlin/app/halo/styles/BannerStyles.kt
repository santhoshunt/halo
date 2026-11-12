package app.halo.styles

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import app.halo.data.StyleId
import app.halo.notify.Banner
import app.halo.notify.BannerAction
import app.halo.overlay.BannerUi
import kotlinx.coroutines.delay

/** Colours and shape of one banner style. Text is always light on dark or dark on light. */
private data class Palette(
    val container: Color,
    val border: Color,
    val borderWidth: Dp,
    val title: Color,
    val body: Color,
    val chipBorder: Color,
    val corner: Dp,
    /** Thread puts title and message on one line. */
    val compact: Boolean,
)

private val Ink = Color(0xFF222222)
private val InkMuted = Color(0xFF5F5E5A)
private val InkLine = Color(0xFF444441)
private val Paper = Color(0xFFFFFDF8)
private val Light = Color(0xFFF0F4FF)
private val LightMuted = Color(0xFFAAB8D4)

private fun paletteFor(style: StyleId, accent: Int): Palette = when (style) {
    StyleId.QUILL -> Palette(Paper, InkLine, 1.dp, Ink, InkMuted, InkLine, 14.dp, compact = false)
    StyleId.SLATE -> Palette(Color(0xFF1E2227), Color(0xFF2E343B), 1.dp, Light, LightMuted, Color(0xFF4A525C), 18.dp, compact = false)
    StyleId.THREAD -> Palette(Color.White, Color(0xFFD9D4C7), 1.dp, Ink, InkMuted, Color(0xFFD9D4C7), 28.dp, compact = true)
    StyleId.LANTERN -> {
        val edge = if (accent == 0) LightMuted else Color(accent).copy(alpha = 1f)
        Palette(Color(0xFF16181C), edge, 2.dp, Light, LightMuted, edge, 18.dp, compact = false)
    }
}

/**
 * Draws one banner in the given style. Used by the overlay and by the in-app previews, so a
 * preview looks exactly like the real banner.
 */
@Composable
fun StyledBanner(
    style: StyleId,
    banner: Banner,
    ui: BannerUi,
    modifier: Modifier = Modifier,
    replyTo: BannerAction? = null,
    onAction: (BannerAction) -> Unit = {},
    onReplySend: (BannerAction, String) -> Unit = { _, _ -> },
) {
    val p = paletteFor(style, banner.accentColor)
    val shape = RoundedCornerShape(p.corner)
    val title = (if (ui.hideContent) banner.appLabel else banner.title).toString()
    val text = if (ui.hideContent) "New notification" else banner.text.toString()
    val icon = remember(banner.icon) { banner.icon.toBitmap(96, 96).asImageBitmap() }

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(p.container)
            .border(p.borderWidth, p.border, shape)
            .padding(horizontal = 14.dp, vertical = if (p.compact) 8.dp else 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(icon, contentDescription = null, modifier = Modifier.size(if (p.compact) 24.dp else 36.dp).clip(CircleShape))
            Spacer(Modifier.width(10.dp))
            if (p.compact) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = p.title, fontWeight = FontWeight.Medium)) { append(title) }
                        append("  ")
                        append(text)
                    },
                    color = p.body,
                    fontSize = ui.fontSizeSp.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            color = p.title,
                            fontWeight = FontWeight.Medium,
                            fontSize = ui.fontSizeSp.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(banner.appLabel.toString(), color = p.body, fontSize = (ui.fontSizeSp - 3).sp, maxLines = 1)
                    }
                    Text(
                        text,
                        color = p.body,
                        fontSize = (ui.fontSizeSp - 1).sp,
                        maxLines = ui.maxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (replyTo != null) {
            ReplyRow(p, ui) { onReplySend(replyTo, it) }
        } else if (banner.actions.isNotEmpty()) {
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                banner.actions.forEach { action -> Chip(action.title.toString(), p) { onAction(action) } }
            }
        }
    }
}

@Composable
private fun Chip(label: String, p: Palette, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .clip(shape)
            .border(1.dp, p.chipBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, color = p.title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ReplyRow(p: Palette, ui: BannerUi, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // The window turns focusable through an async WindowManager update; give it a moment
        // before asking for focus so the keyboard opens.
        delay(120)
        focus.requestFocus()
    }
    val shape = RoundedCornerShape(10.dp)
    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focus)
                .border(1.dp, p.chipBorder, shape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            textStyle = TextStyle(color = p.title, fontSize = ui.fontSizeSp.sp),
            cursorBrush = SolidColor(p.title),
            maxLines = 4,
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) Text("Reply", color = p.body, fontSize = ui.fontSizeSp.sp)
                    inner()
                }
            },
        )
        Spacer(Modifier.width(8.dp))
        Chip("Send", p) { if (text.isNotBlank()) onSend(text.trim()) }
    }
}
