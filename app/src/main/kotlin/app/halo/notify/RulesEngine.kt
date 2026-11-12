package app.halo.notify

import android.app.Notification
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.halo.data.HaloSettings

/** Decides whether a posted notification becomes a Halo banner. */
object RulesEngine {
    fun shouldShow(
        sbn: StatusBarNotification,
        ranking: NotificationListenerService.Ranking,
        settings: HaloSettings,
        interruptionFilter: Int,
        screenOn: Boolean,
        locked: Boolean,
    ): Boolean {
        val g = settings.global
        val n = sbn.notification
        return when {
            !g.enabled -> false
            // Halo's own notifications, from either the release or the debug build.
            sbn.packageName.startsWith("app.halo") -> false
            // The child notification carries the message; the summary only repeats it.
            n.flags and Notification.FLAG_GROUP_SUMMARY != 0 -> false
            // Music, downloads, foreground services and answered calls are ongoing.
            // A ringing call is the exception.
            sbn.isOngoing && !n.isIncomingCall() -> false
            !settings.ruleFor(sbn.packageName).enabled -> false
            !screenOn || locked -> false
            g.skipSilent && ranking.importance < NotificationManager.IMPORTANCE_DEFAULT -> false
            g.respectDnd &&
                interruptionFilter != NotificationListenerService.INTERRUPTION_FILTER_ALL &&
                !ranking.matchesInterruptionFilter() -> false
            else -> true
        }
    }
}
