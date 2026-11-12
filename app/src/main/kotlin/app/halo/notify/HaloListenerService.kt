package app.halo.notify

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import app.halo.HaloApp
import app.halo.R
import app.halo.data.UnlockMode
import java.time.LocalTime

/**
 * Android binds this service once the user grants Notification access, and calls it for every
 * notification posted on the phone. Callbacks arrive on the main thread.
 */
class HaloListenerService : NotificationListenerService() {
    /** Notification key to (hash of title and text, time seen), for skipping identical reposts. */
    private val recent = HashMap<String, Pair<Int, Long>>()
    /** Banners that arrived while the phone was locked, one per key, oldest first. */
    private val held = LinkedHashMap<String, Banner>()
    private val foreground by lazy { ForegroundTracker(this) }
    private var unlockReceiverRegistered = false

    private val app get() = application as HaloApp

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val keyguard = getSystemService(KeyguardManager::class.java)
            // SCREEN_ON also covers phones with no lock screen, where USER_PRESENT may not follow.
            if (intent.action == Intent.ACTION_USER_PRESENT || !keyguard.isKeyguardLocked) flushHeld()
        }
    }

    override fun onListenerConnected() {
        instance = this
        if (!unlockReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            ContextCompat.registerReceiver(this, unlockReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            unlockReceiverRegistered = true
        }
    }

    override fun onListenerDisconnected() {
        instance = null
        unregisterUnlockReceiver()
        // Battery management can unbind the listener; ask Android to bind it again.
        requestRebind(ComponentName(this, HaloListenerService::class.java))
    }

    override fun onDestroy() {
        unregisterUnlockReceiver()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        val ranking = Ranking()
        if (!rankingMap.getRanking(sbn.key, ranking)) return
        val settings = app.settings.value
        val power = getSystemService(PowerManager::class.java)
        val keyguard = getSystemService(KeyguardManager::class.java)
        val now = LocalTime.now()
        val verdict = RulesEngine.decide(
            sbn = sbn,
            ranking = ranking,
            settings = settings,
            interruptionFilter = currentInterruptionFilter,
            screenOn = power.isInteractive,
            locked = keyguard.isKeyguardLocked,
            minuteOfDay = now.hour * 60 + now.minute,
            foregroundPackage = { foreground.current() },
        )
        // An update that no longer qualifies (a ringing call that was answered, for example)
        // takes its banner off the screen.
        val banner = if (verdict == Verdict.SKIP) null else sbn.toBanner(this)
        if (banner == null) {
            app.overlay.onRemoved(sbn.key)
            held.remove(sbn.key)
            return
        }

        val nowMs = SystemClock.elapsedRealtime()
        val hash = "${banner.title}\u0000${banner.text}".hashCode()
        recent.entries.removeIf { nowMs - it.value.second > REPOST_WINDOW_MS }
        if (recent[sbn.key]?.first == hash) return
        recent[sbn.key] = hash to nowMs

        if (settings.global.historyEnabled) app.history.record(banner, settings.global.historyKeepText)
        if (verdict == Verdict.HOLD) {
            held.remove(sbn.key)
            held[sbn.key] = banner
        } else {
            app.overlay.show(banner)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        held.remove(sbn.key)
        app.overlay.onRemoved(sbn.key)
    }

    private fun flushHeld() {
        if (held.isEmpty()) return
        val banners = held.values.toList()
        held.clear()
        when (app.settings.value.global.whileLocked) {
            UnlockMode.SUMMARY -> app.overlay.show(if (banners.size == 1) banners[0] else summaryOf(banners))
            UnlockMode.LATEST -> app.overlay.show(banners.last())
            UnlockMode.ALL -> banners.forEach { app.overlay.show(it) }
            UnlockMode.NONE -> Unit
        }
    }

    /** One banner listing what arrived while locked, such as "WhatsApp 3, Gmail 1, and 2 more". */
    private fun summaryOf(banners: List<Banner>): Banner {
        val perApp = banners.groupingBy { it.appLabel.toString() }.eachCount()
            .entries.sortedByDescending { it.value }
        val named = perApp.take(3).joinToString(", ") { "${it.key} ${it.value}" }
        val rest = perApp.size - 3
        return Banner(
            key = SUMMARY_KEY,
            packageName = packageName,
            appLabel = "Halo",
            title = "While your phone was locked",
            text = if (rest > 0) "$named, and $rest more" else named,
            icon = getDrawable(R.mipmap.ic_launcher)!!,
            actions = emptyList(),
            contentIntent = null,
            autoCancel = false,
            sticky = false,
            accentColor = 0,
        )
    }

    private fun unregisterUnlockReceiver() {
        if (!unlockReceiverRegistered) return
        runCatching { unregisterReceiver(unlockReceiver) }
        unlockReceiverRegistered = false
    }

    companion object {
        /** Set while Android has the listener bound; used to clear original notifications. */
        @Volatile
        var instance: HaloListenerService? = null
            private set

        private const val REPOST_WINDOW_MS = 60_000L
        private const val SUMMARY_KEY = "halo.unlock.summary"
    }
}
