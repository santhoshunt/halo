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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.halo.data.BannerPosition
import app.halo.data.HaloSettings
import app.halo.data.StyleId
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

/**
 * Shows one banner at a time in a TYPE_APPLICATION_OVERLAY window. A newer banner replaces the
 * one on screen. Main thread only.
 */
class OverlayController(private val app: Application, private val settings: StateFlow<HaloSettings>) {
    private val wm = app.getSystemService(WindowManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val banner = mutableStateOf<Banner?>(null)
    private val styleOverride = mutableStateOf<StyleId?>(null)
    private val replyTo = mutableStateOf<BannerAction?>(null)
    private val showId = mutableIntStateOf(0)

    private var frame: BannerFrame? = null
    private var owner: OverlayLifecycleOwner? = null
    private var timer: Job? = null

    /** [style] forces a style for previews; null uses the app's rule or the global style. */
    fun show(next: Banner, style: StyleId? = null) {
        if (!Settings.canDrawOverlays(app)) return
        banner.value = next
        styleOverride.value = style
        replyTo.value = null
        showId.intValue++
        val view = frame ?: attach() ?: return
        runCatching { wm.updateViewLayout(view, layoutParams(focusable = false)) }
        restartTimer()
    }

    fun dismiss(clearOriginal: Boolean) {
        val current = banner.value
        if (clearOriginal && current != null) cancelOriginal(current.key)
        timer?.cancel()
        banner.value = null
        replyTo.value = null
        frame?.let { runCatching { wm.removeView(it) } }
        owner?.stop()
        frame = null
        owner = null
    }

    /** The original notification went away elsewhere, so its banner goes too. */
    fun onRemoved(key: String) {
        if (banner.value?.key == key) dismiss(clearOriginal = false)
    }

    fun enterReply(action: BannerAction) {
        val view = frame ?: return
        timer?.cancel()
        runCatching { wm.updateViewLayout(view, layoutParams(focusable = true)) }
        replyTo.value = action
    }

    fun exitReply() {
        val view = frame ?: return
        replyTo.value = null
        runCatching { wm.updateViewLayout(view, layoutParams(focusable = false)) }
        restartTimer()
    }

    private fun tap(current: Banner) {
        current.contentIntent?.let { send(it, fillIn = null) }
        if (current.autoCancel) cancelOriginal(current.key)
        dismiss(clearOriginal = false)
    }

    private fun runAction(action: BannerAction) {
        send(action.intent, fillIn = null)
        dismiss(clearOriginal = false)
    }

    private fun sendReply(action: BannerAction, text: String) {
        val input = action.remoteInput ?: return
        val fillIn = Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        RemoteInput.addResultsToIntent(
            arrayOf(input), fillIn, Bundle().apply { putCharSequence(input.resultKey, text) },
        )
        RemoteInput.setResultsSource(fillIn, RemoteInput.SOURCE_FREE_FORM_INPUT)
        send(action.intent, fillIn)
        dismiss(clearOriginal = false)
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

    private fun restartTimer() {
        timer?.cancel()
        val current = banner.value ?: return
        if (current.sticky) return
        timer = scope.launch {
            delay(settings.value.global.durationMs.toLong())
            dismiss(clearOriginal = false)
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
            null
        }
    }

    @Composable
    private fun Content() {
        val current = banner.value ?: return
        val s by settings.collectAsState()
        val g = s.global
        key(showId.intValue) {
            BannerHost(
                banner = current,
                style = styleOverride.value ?: s.styleFor(current.packageName),
                ui = BannerUi(g.maxLines, g.fontSizeSp, g.hideContent),
                replyTo = replyTo.value,
                callbacks = BannerCallbacks(
                    onTap = { tap(current) },
                    onAction = ::runAction,
                    onReplyOpen = ::enterReply,
                    onReplySend = ::sendReply,
                    onDismiss = { dismiss(clearOriginal = settings.value.global.dismissOriginalOnSwipe) },
                ),
            )
        }
    }

    private fun layoutParams(focusable: Boolean): WindowManager.LayoutParams {
        val insets = wm.currentWindowMetrics.windowInsets
        val margin = (8 * app.resources.displayMetrics.density).toInt()
        // FLAG_NOT_TOUCH_MODAL passes touches outside the banner to the app underneath.
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
     * Root view of the overlay window. An overlay has no OnBackPressedDispatcher, so Compose
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
    }
}
