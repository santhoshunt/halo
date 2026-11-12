package app.halo.overlay

import android.app.ActivityOptions
import android.app.Application
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
import android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
import android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.halo.data.BannerPosition
import app.halo.data.HaloSettings
import app.halo.data.StyleId
import app.halo.effects.EffectOverlay
import app.halo.notify.Banner
import app.halo.notify.BannerAction
import app.halo.notify.HaloListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** One card on screen. Grouping merges several notifications from one app into one card. */
data class StackEntry(
    /** "app:<package>" when grouped by app, otherwise the notification key. */
    val id: String,
    /** The latest banner merged into this card. */
    val banner: Banner,
    /** Every notification key merged in; the card goes when all of them are removed. */
    val keys: Set<String>,
    /** Banners merged so far, shown as "<app>, <count> new" above 1. */
    val count: Int,
    /** Fixed when the card first appears, so a random style doesn't change on every merge. */
    val style: StyleId,
    /** Bumps on every merge, restarting the slide-in and swipe state. */
    val showId: Int,
)

/**
 * Shows up to [MAX_STACK] cards in one TYPE_APPLICATION_OVERLAY window, with an optional
 * screen effect window underneath. Main thread only.
 */
class OverlayController(private val app: Application, private val settings: StateFlow<HaloSettings>) {
    private val wm = app.getSystemService(WindowManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val effects = EffectOverlay(app)

    private val stack = mutableStateListOf<StackEntry>()
    /** Entry id and the reply action whose text field is open. */
    private val replyTo = mutableStateOf<Pair<String, BannerAction>?>(null)
    private val timers = HashMap<String, Job>()
    private var nextShowId = 0

    private var frame: BannerFrame? = null
    private var owner: OverlayLifecycleOwner? = null

    /** [style] forces a style for previews; null uses the app's rule, random style or the global style. */
    fun show(next: Banner, style: StyleId? = null) {
        if (!Settings.canDrawOverlays(app)) return
        val s = settings.value
        val g = s.global
        val id = if (g.groupByApp) "app:${next.packageName}" else next.key
        val index = stack.indexOfFirst { it.id == id || next.key in it.keys }
        val entry = if (index >= 0) {
            val old = stack.removeAt(index)
            // Keep the card's identity while its reply box is open, so typed text survives.
            val showId = if (replyTo.value?.first == old.id) old.showId else ++nextShowId
            // A preview's forced style replaces the card's style; otherwise the card keeps its own.
            old.copy(
                banner = next,
                keys = old.keys + next.key,
                count = old.count + 1,
                style = style ?: old.style,
                showId = showId,
            )
        } else {
            val chosen = style ?: s.ruleFor(next.packageName).style
                ?: if (g.randomStyle) StyleId.entries.random() else g.style
            StackEntry(id, next, setOf(next.key), count = 1, style = chosen, showId = ++nextShowId)
        }
        if (!g.stackBanners) stack.toList().forEach { remove(it.id) }
        stack.add(0, entry)
        while (stack.size > MAX_STACK) remove(stack.last().id)

        val playEffect = { effects.play(g.effect, next.accentColor, g.effectIntensity, g.durationMs.toLong()) }
        when {
            frame == null -> {
                // The effect window goes in first so it sits under the banner window.
                playEffect()
                attach() ?: return
            }
            effects.isShowing -> playEffect()
            // The effect was switched on while banners were showing. Re-add the banner window
            // above the new effect window; skipped while a reply is open so typing isn't lost.
            effects.canPlay(g.effect) && replyTo.value == null -> {
                removeBannerWindow()
                playEffect()
                attach() ?: return
            }
        }
        frame?.let { view -> runCatching { wm.updateViewLayout(view, layoutParams(focusable = replyTo.value != null)) } }
        restartTimer(entry)
    }

    /** A notification went away elsewhere; its card goes when none of its notifications remain. */
    fun onRemoved(key: String) {
        val index = stack.indexOfFirst { key in it.keys }
        if (index < 0) return
        val entry = stack[index]
        val keys = entry.keys - key
        if (keys.isEmpty()) dismiss(entry.id, clearOriginal = false) else stack[index] = entry.copy(keys = keys)
    }

    fun dismiss(id: String, clearOriginal: Boolean) {
        val entry = stack.firstOrNull { it.id == id } ?: return
        if (clearOriginal) entry.keys.forEach(::cancelOriginal)
        remove(id)
        if (stack.isEmpty()) detach()
    }

    private fun enterReply(id: String, action: BannerAction) {
        val view = frame ?: return
        timers.remove(id)?.cancel()
        runCatching { wm.updateViewLayout(view, layoutParams(focusable = true)) }
        replyTo.value = id to action
    }

    fun exitReply() {
        val (id, _) = replyTo.value ?: return
        replyTo.value = null
        frame?.let { view -> runCatching { wm.updateViewLayout(view, layoutParams(focusable = false)) } }
        stack.firstOrNull { it.id == id }?.let(::restartTimer)
    }

    private fun tap(entry: StackEntry) {
        entry.banner.contentIntent?.let { send(it, fillIn = null) }
        if (entry.banner.autoCancel) cancelOriginal(entry.banner.key)
        dismiss(entry.id, clearOriginal = false)
    }

    private fun runAction(entry: StackEntry, action: BannerAction) {
        send(action.intent, fillIn = null)
        dismiss(entry.id, clearOriginal = false)
    }

    private fun sendReply(entry: StackEntry, action: BannerAction, text: String) {
        val input = action.remoteInput ?: return
        val fillIn = Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        RemoteInput.addResultsToIntent(
            arrayOf(input), fillIn, Bundle().apply { putCharSequence(input.resultKey, text) },
        )
        RemoteInput.setResultsSource(fillIn, RemoteInput.SOURCE_FREE_FORM_INPUT)
        send(action.intent, fillIn)
        dismiss(entry.id, clearOriginal = false)
    }

    private fun send(intent: PendingIntent, fillIn: Intent?) {
        // Halo holds a visible overlay window, which lets the target app open an activity
        // from the background once the sender opts in here.
        val options = ActivityOptions.makeBasic()
            .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS)
            .toBundle()
        try {
            intent.send(app, 0, fillIn, null, null, null, options)
        } catch (e: PendingIntent.CanceledException) {
            Log.i(TAG, "PendingIntent was cancelled by its app", e)
        }
    }

    private fun cancelOriginal(key: String) {
        runCatching { HaloListenerService.instance?.cancelNotification(key) }
            .onFailure { Log.w(TAG, "cancelNotification failed", it) }
    }

    private fun remove(id: String) {
        timers.remove(id)?.cancel()
        stack.removeAll { it.id == id }
        if (replyTo.value?.first == id) {
            replyTo.value = null
            frame?.let { view -> runCatching { wm.updateViewLayout(view, layoutParams(focusable = false)) } }
        }
    }

    private fun restartTimer(entry: StackEntry) {
        timers.remove(entry.id)?.cancel()
        if (entry.banner.sticky || replyTo.value?.first == entry.id) return
        timers[entry.id] = scope.launch {
            delay(settings.value.global.durationMs.toLong())
            dismiss(entry.id, clearOriginal = false)
        }
    }

    private fun attach(): BannerFrame? {
        val lifecycle = OverlayLifecycleOwner().also { it.start() }
        val view = BannerFrame(
            app,
            onBack = { if (replyTo.value != null) { exitReply(); true } else false },
            onOutside = { if (replyTo.value != null) exitReply() },
        )
        view.setViewTreeLifecycleOwner(lifecycle)
        view.setViewTreeSavedStateRegistryOwner(lifecycle)
        view.addView(ComposeView(app).apply { setContent { Content() } })
        return try {
            wm.addView(view, layoutParams(focusable = false))
            frame = view
            owner = lifecycle
            view
        } catch (e: RuntimeException) {
            // BadTokenException or InvalidDisplayException, for example after the overlay
            // permission was revoked between the check and the call.
            Log.w(TAG, "addView failed", e)
            lifecycle.stop()
            stack.clear()
            effects.hide()
            null
        }
    }

    private fun detach() {
        replyTo.value = null
        timers.values.forEach { it.cancel() }
        timers.clear()
        removeBannerWindow()
        effects.hide()
    }

    private fun removeBannerWindow() {
        frame?.let { runCatching { wm.removeView(it) } }
        owner?.stop()
        frame = null
        owner = null
    }

    @Composable
    private fun Content() {
        val s by settings.collectAsState()
        val g = s.global
        val reply = replyTo.value
        // The newest card sits nearest the screen edge the stack is anchored to.
        val cards = if (g.position == BannerPosition.BOTTOM) stack.asReversed() else stack
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cards.forEach { entry ->
                key(entry.id, entry.showId) {
                    BannerHost(
                        banner = entry.banner,
                        style = entry.style,
                        ui = BannerUi(g.maxLines, g.fontSizeSp, g.hideContent),
                        count = entry.count,
                        replyTo = reply?.takeIf { it.first == entry.id }?.second,
                        callbacks = BannerCallbacks(
                            onTap = { tap(entry) },
                            onAction = { action -> runAction(entry, action) },
                            onReplyOpen = { action -> enterReply(entry.id, action) },
                            onReplySend = { action, text -> sendReply(entry, action, text) },
                            onDismiss = { dismiss(entry.id, clearOriginal = settings.value.global.dismissOriginalOnSwipe) },
                        ),
                    )
                }
            }
        }
    }

    private fun layoutParams(focusable: Boolean): WindowManager.LayoutParams {
        val insets = wm.currentWindowMetrics.windowInsets
        val margin = (8 * app.resources.displayMetrics.density).toInt()
        // FLAG_NOT_TOUCH_MODAL passes touches outside the banners to the app underneath.
        var flags = FLAG_NOT_TOUCH_MODAL or FLAG_LAYOUT_IN_SCREEN
        flags = if (focusable) flags or FLAG_WATCH_OUTSIDE_TOUCH else flags or FLAG_NOT_FOCUSABLE
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            title = "Halo banner"
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            softInputMode = if (focusable) {
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
            } else {
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
            }
            when (settings.value.global.position) {
                BannerPosition.TOP -> {
                    gravity = Gravity.TOP
                    y = insets.getInsets(WindowInsets.Type.statusBars()).top + margin
                }
                BannerPosition.MIDDLE -> gravity = Gravity.CENTER_VERTICAL
                BannerPosition.BOTTOM -> {
                    gravity = Gravity.BOTTOM
                    y = insets.getInsets(WindowInsets.Type.navigationBars()).bottom + margin
                }
            }
        }
    }

    /**
     * Root view of the banner window. An overlay has no OnBackPressedDispatcher, so Compose
     * BackHandler never fires; Back and outside taps are caught here to leave reply mode.
     */
    private class BannerFrame(
        context: Context,
        private val onBack: () -> Boolean,
        private val onOutside: () -> Unit,
    ) : FrameLayout(context) {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP && onBack()) return true
            return super.dispatchKeyEvent(event)
        }

        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                onOutside()
                return true
            }
            return super.dispatchTouchEvent(event)
        }
    }

    private companion object {
        const val TAG = "HaloOverlay"
        const val MAX_STACK = 3
    }
}
