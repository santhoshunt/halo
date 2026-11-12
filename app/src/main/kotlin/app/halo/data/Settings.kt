package app.halo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class StyleId(val label: String, val description: String) {
    QUILL("Quill", "Ivory card with an ink outline"),
    SLATE("Slate", "Solid dark card with soft light text"),
    THREAD("Thread", "One slim line with icon and message"),
    LANTERN("Lantern", "Dark card edged in the app's own colour"),
}

enum class BannerPosition(val label: String) { TOP("Top"), MIDDLE("Middle"), BOTTOM("Bottom") }

data class GlobalSettings(
    val enabled: Boolean = true,
    val style: StyleId = StyleId.QUILL,
    val position: BannerPosition = BannerPosition.TOP,
    val durationMs: Int = 4000,
    val maxLines: Int = 2,
    val fontSizeSp: Int = 15,
    val hideContent: Boolean = false,
    val respectDnd: Boolean = true,
    val skipSilent: Boolean = true,
    val dismissOriginalOnSwipe: Boolean = false,
    val setupDone: Boolean = false,
)

data class AppRule(val packageName: String, val enabled: Boolean = true, val style: StyleId? = null)

data class HaloSettings(
    val global: GlobalSettings = GlobalSettings(),
    val rules: Map<String, AppRule> = emptyMap(),
) {
    fun ruleFor(pkg: String): AppRule = rules[pkg] ?: AppRule(pkg)
    fun styleFor(pkg: String): StyleId = ruleFor(pkg).style ?: global.style
}

private val Context.haloStore: DataStore<Preferences> by preferencesDataStore(name = "halo_settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.haloStore

    val settings: Flow<HaloSettings> = store.data.map { prefs ->
        HaloSettings(global = readGlobal(prefs), rules = readRules(prefs))
    }

    suspend fun updateGlobal(transform: (GlobalSettings) -> GlobalSettings) {
        store.edit { prefs -> writeGlobal(prefs, transform(readGlobal(prefs))) }
    }

    suspend fun setAppEnabled(pkg: String, enabled: Boolean) {
        store.edit { it[booleanPreferencesKey(APP_ENABLED + pkg)] = enabled }
    }

    /** A null style clears the override, so the app follows the global style again. */
    suspend fun setAppStyle(pkg: String, style: StyleId?) {
        store.edit {
            val key = stringPreferencesKey(APP_STYLE + pkg)
            if (style == null) it.remove(key) else it[key] = style.name
        }
    }

    private fun readGlobal(p: Preferences): GlobalSettings {
        val d = GlobalSettings()
        return GlobalSettings(
            enabled = p[ENABLED] ?: d.enabled,
            style = enumOr(p[STYLE], d.style),
            position = enumOr(p[POSITION], d.position),
            durationMs = p[DURATION_MS] ?: d.durationMs,
            maxLines = p[MAX_LINES] ?: d.maxLines,
            fontSizeSp = p[FONT_SP] ?: d.fontSizeSp,
            hideContent = p[HIDE_CONTENT] ?: d.hideContent,
            respectDnd = p[RESPECT_DND] ?: d.respectDnd,
            skipSilent = p[SKIP_SILENT] ?: d.skipSilent,
            dismissOriginalOnSwipe = p[DISMISS_ON_SWIPE] ?: d.dismissOriginalOnSwipe,
            setupDone = p[SETUP_DONE] ?: d.setupDone,
        )
    }

    private fun writeGlobal(p: MutablePreferences, g: GlobalSettings) {
        p[ENABLED] = g.enabled
        p[STYLE] = g.style.name
        p[POSITION] = g.position.name
        p[DURATION_MS] = g.durationMs
        p[MAX_LINES] = g.maxLines
        p[FONT_SP] = g.fontSizeSp
        p[HIDE_CONTENT] = g.hideContent
        p[RESPECT_DND] = g.respectDnd
        p[SKIP_SILENT] = g.skipSilent
        p[DISMISS_ON_SWIPE] = g.dismissOriginalOnSwipe
        p[SETUP_DONE] = g.setupDone
    }

    private fun readRules(p: Preferences): Map<String, AppRule> {
        val rules = HashMap<String, AppRule>()
        for ((key, value) in p.asMap()) {
            val name = key.name
            when {
                name.startsWith(APP_ENABLED) -> {
                    val pkg = name.removePrefix(APP_ENABLED)
                    rules[pkg] = (rules[pkg] ?: AppRule(pkg)).copy(enabled = value as Boolean)
                }
                name.startsWith(APP_STYLE) -> {
                    val pkg = name.removePrefix(APP_STYLE)
                    val style = StyleId.entries.firstOrNull { it.name == value } ?: continue
                    rules[pkg] = (rules[pkg] ?: AppRule(pkg)).copy(style = style)
                }
            }
        }
        return rules
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private companion object {
        const val APP_ENABLED = "app_enabled:"
        const val APP_STYLE = "app_style:"
        val ENABLED = booleanPreferencesKey("enabled")
        val STYLE = stringPreferencesKey("style")
        val POSITION = stringPreferencesKey("position")
        val DURATION_MS = intPreferencesKey("duration_ms")
        val MAX_LINES = intPreferencesKey("max_lines")
        val FONT_SP = intPreferencesKey("font_sp")
        val HIDE_CONTENT = booleanPreferencesKey("hide_content")
        val RESPECT_DND = booleanPreferencesKey("respect_dnd")
        val SKIP_SILENT = booleanPreferencesKey("skip_silent")
        val DISMISS_ON_SWIPE = booleanPreferencesKey("dismiss_on_swipe")
        val SETUP_DONE = booleanPreferencesKey("setup_done")
    }
}
