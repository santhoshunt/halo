package app.halo.styles

import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import app.halo.data.StyleId
import app.halo.notify.Banner
import app.halo.notify.BannerAction
import app.halo.overlay.BannerUi
import kotlinx.coroutines.delay

/** Fallback accent for styles that use the app's colour when the app set none. */
private val DefaultAccent = Color(0xFFAAB8D4)

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
    /** Banners merged into this card; above 1 the app label reads "<app>, <count> new". */
    count: Int = 1,
    replyTo: BannerAction? = null,
    onAction: (BannerAction) -> Unit = {},
    onReplySend: (BannerAction, String) -> Unit = { _, _ -> },
) {
    val spec = specFor(style)
    val accent = if (banner.accentColor == 0) DefaultAccent else Color(banner.accentColor).copy(alpha = 1f)
    val chip = if (spec.accentBorder) accent else spec.chip
    val rawTitle = (if (ui.hideContent) banner.appLabel else banner.title).toString()
    val title = when {
        spec.layout == StyleLayout.TERMINAL -> "> $rawTitle"
        spec.uppercase || spec.layout == StyleLayout.POSTER -> rawTitle.uppercase()
        else -> rawTitle
    }
    val text = if (ui.hideContent) "New notification" else banner.text.toString()
    val appLabel = if (count > 1) "${banner.appLabel}, $count new" else banner.appLabel.toString()
    val icon = remember(banner.icon) { banner.icon.toBitmap(96, 96).asImageBitmap() }
    val titleSize = (ui.fontSizeSp * spec.titleScale + if (spec.layout == StyleLayout.POSTER) 6 else 0).sp
    val bodySize = ((ui.fontSizeSp - 1) * spec.bodyScale).sp
    val compact = spec.layout == StyleLayout.LINE

    Column(
        modifier
            .fillMaxWidth()
            .styleSurface(spec, accent)
            .padding(spec.decoPadding())
            .padding(horizontal = 14.dp, vertical = if (compact) 8.dp else 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(icon, contentDescription = null, modifier = Modifier.size(if (compact) 24.dp else 36.dp).clip(CircleShape))
            Spacer(Modifier.width(10.dp))
            if (compact) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = spec.title, fontWeight = FontWeight.Medium, fontFamily = spec.font)) {
                            append(if (count > 1) "$title ($count)" else title)
                        }
                        append("  ")
                        append(text)
                    },
                    color = spec.body,
                    fontFamily = spec.bodyFont,
                    fontSize = bodySize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            color = spec.title,
                            fontWeight = FontWeight.Medium,
                            fontFamily = spec.font,
                            fontSize = titleSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(appLabel, color = spec.body, fontSize = (ui.fontSizeSp - 3).sp, maxLines = 1)
                    }
                    Text(
                        text,
                        color = spec.body,
                        fontFamily = spec.bodyFont,
                        fontSize = bodySize,
                        maxLines = ui.maxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (replyTo != null) {
            ReplyRow(spec.title, spec.body, chip, bodySize) { onReplySend(replyTo, it) }
        } else if (banner.actions.isNotEmpty()) {
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                banner.actions.forEach { action -> Chip(action.title.toString(), spec.title, chip) { onAction(action) } }
            }
        }
    }
}

@Composable
private fun Chip(label: String, textColor: Color, border: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .clip(shape)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, color = textColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ReplyRow(textColor: Color, hintColor: Color, border: Color, size: TextUnit, onSend: (String) -> Unit) {
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
                .border(1.dp, border, shape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            textStyle = TextStyle(color = textColor, fontSize = size),
            cursorBrush = SolidColor(textColor),
            maxLines = 4,
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) Text("Reply", color = hintColor, fontSize = size)
                    inner()
                }
            },
        )
        Spacer(Modifier.width(8.dp))
        Chip("Send", textColor, border) { if (text.isNotBlank()) onSend(text.trim()) }
    }
}
