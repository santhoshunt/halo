package app.halo.notify

import android.app.Notification
import android.content.Context
import android.graphics.drawable.Drawable
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

/** Turns a posted notification into a [Banner], or null when it has nothing to show. */
fun StatusBarNotification.toBanner(context: Context): Banner? {
    val n = notification
    val extras = n.extras
    val messaging = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
    val last = messaging?.messages?.lastOrNull()

    val sender: CharSequence? = last?.person?.name
    val group: CharSequence? = messaging?.takeIf { it.isGroupConversation }?.conversationTitle
    val title: CharSequence = when {
        sender != null && group != null -> "$group · $sender"
        sender != null -> sender
        else -> extras.getCharSequence(Notification.EXTRA_TITLE) ?: ""
    }
    val text: CharSequence = last?.text
        ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        ?: extras.getCharSequence(Notification.EXTRA_TEXT)
        ?: ""
    if (title.isBlank() && text.isBlank()) return null

    val pm = context.packageManager
    val appInfo = runCatching { pm.getApplicationInfo(packageName, 0) }.getOrNull()
    val appLabel: CharSequence = appInfo?.let { pm.getApplicationLabel(it) } ?: packageName

    // Large icon first (a contact photo), then the app icon. The small icon comes last because
    // it is a one-colour silhouette that disappears on light banner styles.
    val icon: Drawable = runCatching { n.getLargeIcon()?.loadDrawable(context) }.getOrNull()
        ?: appInfo?.let { runCatching { pm.getApplicationIcon(it) }.getOrNull() }
        ?: runCatching { n.smallIcon?.loadDrawable(context) }.getOrNull()
        ?: pm.defaultActivityIcon

    val actions = n.actions.orEmpty()
        .mapNotNull { a ->
            val intent = a.actionIntent ?: return@mapNotNull null
            BannerAction(a.title ?: "", intent, a.remoteInputs?.firstOrNull { it.allowFreeFormInput })
        }
        .sortedByDescending { it.remoteInput != null }
        .take(2)

    return Banner(
        key = key,
        packageName = packageName,
        appLabel = appLabel,
        title = title,
        text = text,
        icon = icon,
        actions = actions,
        contentIntent = n.contentIntent,
        autoCancel = n.flags and Notification.FLAG_AUTO_CANCEL != 0,
        sticky = n.isIncomingCall(),
        accentColor = n.color,
    )
}

/**
 * True while a call is ringing. Once answered, the dialer reposts the same notification as an
 * ongoing call, which no longer qualifies, so the banner goes away.
 */
fun Notification.isIncomingCall(): Boolean {
    if (category != Notification.CATEGORY_CALL) return false
    val type = extras.getInt(Notification.EXTRA_CALL_TYPE, Notification.CallStyle.CALL_TYPE_UNKNOWN)
    // Dialers that don't set a call type still give a ringing call a full-screen intent.
    return if (type != Notification.CallStyle.CALL_TYPE_UNKNOWN) {
        type == Notification.CallStyle.CALL_TYPE_INCOMING
    } else {
        fullScreenIntent != null
    }
}
