package app.halo.notify

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

/**
 * Finds the app in the foreground from usage events. Needs Usage access; without it, [current]
 * returns null and no app counts as quiet.
 */
class ForegroundTracker(private val context: Context) {
    private val usage = context.getSystemService(UsageStatsManager::class.java)
    private var lastQueryAt = 0L
    private var lastForeground: String? = null

    fun hasAccess(): Boolean = hasUsageAccess(context)

    fun current(): String? {
        if (!hasAccess()) return null
        val now = System.currentTimeMillis()
        // The first query looks back 12 hours; later ones only read events since the last query.
        val from = if (lastQueryAt == 0L) now - FIRST_WINDOW_MS else lastQueryAt
        val events = usage.queryEvents(from, now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) lastForeground = event.packageName
        }
        lastQueryAt = now
        return lastForeground
    }

    private companion object {
        const val FIRST_WINDOW_MS = 12 * 60 * 60 * 1000L
    }
}

fun hasUsageAccess(context: Context): Boolean {
    val ops = context.getSystemService(AppOpsManager::class.java)
    val mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}
