package app.halo

import android.app.Application
import app.halo.data.HaloSettings
import app.halo.data.HistoryStore
import java.io.File
import app.halo.data.SettingsRepository
import app.halo.overlay.OverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Holds the process-wide settings and overlay, shared by the listener and the app screens. */
class HaloApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var repo: SettingsRepository
        private set
    lateinit var settings: StateFlow<HaloSettings>
        private set
    lateinit var overlay: OverlayController
        private set
    lateinit var history: HistoryStore
        private set

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        repo = SettingsRepository(this)
        settings = repo.settings.stateIn(appScope, SharingStarted.Eagerly, HaloSettings())
        overlay = OverlayController(this, settings)
        history = HistoryStore(File(filesDir, "history.json"), appScope)
    }
}
