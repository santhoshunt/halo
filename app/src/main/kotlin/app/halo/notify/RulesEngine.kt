package app.halo.notify

import android.app.Notification
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.halo.data.HaloSettings
import app.halo.data.UnlockMode

/** SHOW now, HOLD until the phone is unlocked, or SKIP. */
enum class Verdict { SHOW, HOLD, SKIP }

/** Decides what Halo does with a posted notification. */
object RulesEngine {
    fun decide(
        sbn: StatusBarNotification,
        ranking: NotificationListenerService.Ranking,
        settings: HaloSettings,
        interruptionFilter: Int,
        screenOn: Boolean,
        locked: Boolean,
        minuteOfDay: Int,
        foregroundPackage: () -> String?,
    ): Verdict {
        val g = settings.global
        val n = sbn.notification
        val ringing = n.isIncomingCall()
        val filtered = when {
            !g.enabled -> true
            // Halo's own notifications, from either the release or the debug build.
            sbn.packageName.startsWith("app.halo") -> true
            // The child notification carries the message; the summary only repeats it.
            n.flags and Notification.FLAG_GROUP_SUMMARY != 0 -> true
            // Music, downloads, foreground services and answered calls are ongoing.
            // A ringing call is the exception.
            sbn.isOngoing && !ringing -> true
            !settings.ruleFor(sbn.packageName).enabled -> true
            g.skipSilent && ranking.importance < NotificationManager.IMPORTANCE_DEFAULT -> true
            g.respectDnd &&
                interruptionFilter != NotificationListenerService.INTERRUPTION_FILTER_ALL &&
                !ranking.matchesInterruptionFilter() -> true
            else -> false
        }
        if (filtered) return Verdict.SKIP
        // Ringing calls skip both quiet checks: a missed call costs more than an interruption.
        if (!ringing && g.quietHours && inQuietWindow(minuteOfDay, g.quietStartMin, g.quietEndMin)) return Verdict.SKIP
        if (!screenOn || locked) return if (g.whileLocked == UnlockMode.NONE) Verdict.SKIP else Verdict.HOLD
        if (!ringing && settings.anyQuietApps()) {
            val front = foregroundPackage()
            if (front != null && settings.ruleFor(front).quietWhenOpen) return Verdict.SKIP
        }
        return Verdict.SHOW
    }

    /** True when [minute] falls in [start, end), including windows that pass midnight. */
    fun inQuietWindow(minute: Int, start: Int, end: Int): Boolean = when {
        start == end -> false
        start < end -> minute in start until end
        else -> minute >= start || minute < end
    }
}
