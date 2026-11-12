package app.halo.notify

import android.app.PendingIntent
import android.app.RemoteInput
import android.graphics.drawable.Drawable

data class BannerAction(
    val title: CharSequence,
    val intent: PendingIntent,
    /** Set when the action accepts free-form text, which makes it a reply action. */
    val remoteInput: RemoteInput?,
)

data class Banner(
    val key: String,
    val packageName: String,
    val appLabel: CharSequence,
    val title: CharSequence,
    val text: CharSequence,
    val icon: Drawable,
    /** At most two, reply action first. */
    val actions: List<BannerAction>,
    val contentIntent: PendingIntent?,
    val autoCancel: Boolean,
    /** Stays on screen until the original notification goes away. True for incoming calls. */
    val sticky: Boolean,
    /** Notification.color, 0 when the app set none. */
    val accentColor: Int,
)
