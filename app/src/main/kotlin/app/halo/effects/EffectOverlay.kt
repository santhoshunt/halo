package app.halo.effects

import android.app.Application
import android.graphics.PixelFormat
import android.hardware.input.InputManager
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.halo.data.EffectId
import app.halo.overlay.OverlayLifecycleOwner

/**
 * Full-screen window for screen effects. Taps pass through it: it is FLAG_NOT_TOUCHABLE and its
 * opacity stays at or below the device's limit for a pass-through overlay (0.8 by default).
 * OverlayController adds it before the banner window so it sits underneath the banners.
 */
class EffectOverlay(private val app: Application) {
    private data class Run(val effect: EffectId, val accent: Color, val intensity: Int, val runMs: Long, val id: Int)

    private val wm = app.getSystemService(WindowManager::class.java)
    private val run = mutableStateOf<Run?>(null)
    private var counter = 0
    private var view: FrameLayout? = null
    private var owner: OverlayLifecycleOwner? = null

    val isShowing: Boolean get() = view != null

    /** False for no effect, or when the system "Remove animations" setting is on. */
    fun canPlay(effect: EffectId): Boolean = effect != EffectId.NONE && !animationsOff()

    fun play(effect: EffectId, accent: Int, intensity: Int, runMs: Long) {
        if (!canPlay(effect)) {
            hide()
            return
        }
        val color = if (accent == 0) DEFAULT_ACCENT else Color(accent).copy(alpha = 1f)
        run.value = Run(effect, color, intensity, runMs, ++counter)
        if (view == null) attach()
    }

    fun hide() {
        view?.let { runCatching { wm.removeView(it) } }
        owner?.stop()
        view = null
        owner = null
        run.value = null
    }

    private fun attach() {
        val lifecycle = OverlayLifecycleOwner().also { it.start() }
        val frame = FrameLayout(app)
        frame.setViewTreeLifecycleOwner(lifecycle)
        frame.setViewTreeSavedStateRegistryOwner(lifecycle)
        frame.addView(
            ComposeView(app).apply {
                setContent {
                    val r = run.value
                    if (r != null) ScreenEffect(r.effect, r.accent, r.intensity, r.id, r.runMs)
                }
            },
        )
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            title = "Halo effect"
            // The limit is a system setting a device can lower; above it, Android blocks every
            // tap that passes through, so read it rather than assume the 0.8 default.
            val limit = app.getSystemService(InputManager::class.java).maximumObscuringOpacityForTouch
            alpha = minOf(DEFAULT_PASS_THROUGH_ALPHA, limit)
            fitInsetsTypes = 0
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        try {
            wm.addView(frame, params)
            view = frame
            owner = lifecycle
        } catch (e: RuntimeException) {
            Log.w(TAG, "Effect window addView failed", e)
            lifecycle.stop()
        }
    }

    /** The system "Remove animations" setting sets the animator scale to 0. */
    private fun animationsOff(): Boolean =
        Settings.Global.getFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

    private companion object {
        const val TAG = "HaloEffect"
        const val DEFAULT_PASS_THROUGH_ALPHA = 0.8f
        val DEFAULT_ACCENT = Color(0xFF7F77DD)
    }
}
