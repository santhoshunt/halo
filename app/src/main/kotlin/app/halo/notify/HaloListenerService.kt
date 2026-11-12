package app.halo.notify

import android.app.KeyguardManager
import android.content.ComponentName
import android.os.PowerManager
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.halo.HaloApp

/**
 * Android binds this service once the user grants Notification access, and calls it for every
 * notification posted on the phone. Callbacks arrive on the main thread.
 */
class HaloListenerService : NotificationListenerService() {
    /** Notification key to (hash of title and text, time seen), for skipping identical reposts. */
    private val recent = HashMap<String, Pair<Int, Long>>()

    private val app get() = application as HaloApp

    override fun onListenerConnected() {
        instance = this
    }

    override fun onListenerDisconnected() {
        instance = null
        // Battery management can unbind the listener; ask Android to bind it again.
        requestRebind(ComponentName(this, HaloListenerService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        val ranking = Ranking()
        if (!rankingMap.getRanking(sbn.key, ranking)) return
        val power = getSystemService(PowerManager::class.java)
        val keyguard = getSystemService(KeyguardManager::class.java)
        val show = RulesEngine.shouldShow(
            sbn = sbn,
            ranking = ranking,
            settings = app.settings.value,
            interruptionFilter = currentInterruptionFilter,
            screenOn = power.isInteractive,
            locked = keyguard.isKeyguardLocked,
        )
        // An update that no longer qualifies (a ringing call that was answered, for example)
        // takes its banner off the screen.
        val banner = if (show) sbn.toBanner(this) else null
        if (banner == null) {
            app.overlay.onRemoved(sbn.key)
            return
        }

        val now = SystemClock.elapsedRealtime()
        val hash = "${banner.title}\u0000${banner.text}".hashCode()
        recent.entries.removeIf { now - it.value.second > REPOST_WINDOW_MS }
        if (recent[sbn.key]?.first == hash) return
        recent[sbn.key] = hash to now

        app.overlay.show(banner)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        app.overlay.onRemoved(sbn.key)
    }

    companion object {
        /** Set while Android has the listener bound; used to clear original notifications. */
        @Volatile
        var instance: HaloListenerService? = null
            private set

        private const val REPOST_WINDOW_MS = 60_000L
    }
}
