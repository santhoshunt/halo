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

/** What to show on unlock for notifications that arrived while the phone was locked. */
enum class UnlockMode(val label: String) { SUMMARY("Summary"), LATEST("Latest"), ALL("All"), NONE("None") }

enum class EffectId(val label: String) { NONE("None"), EDGE_GLOW("Edge glow"), PETALS("Petals"), CONFETTI("Confetti") }

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
    val stackBanners: Boolean = true,
    val groupByApp: Boolean = true,
    val whileLocked: UnlockMode = UnlockMode.SUMMARY,
    val quietHours: Boolean = false,
    /** Minutes after midnight. */
    val quietStartMin: Int = 22 * 60,
    val quietEndMin: Int = 7 * 60,
    val historyEnabled: Boolean = true,
    val historyKeepText: Boolean = false,
    val effect: EffectId = EffectId.NONE,
    /** 30..100, scales how many particles an effect draws. */
    val effectIntensity: Int = 70,
    val randomStyle: Boolean = false,
)

data class AppRule(
    val packageName: String,
    val enabled: Boolean = true,
    val style: StyleId? = null,
    /** No banners while this app is in the foreground, for games and video players. */
    val quietWhenOpen: Boolean = false,
)

data class HaloSettings(
    val global: GlobalSettings = GlobalSettings(),
    val rules: Map<String, AppRule> = emptyMap(),
) {
    fun ruleFor(pkg: String): AppRule = rules[pkg] ?: AppRule(pkg)
    fun styleFor(pkg: String): StyleId = ruleFor(pkg).style ?: global.style
    fun anyQuietApps(): Boolean = rules.values.any { it.quietWhenOpen }
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

    suspend fun setAppQuiet(pkg: String, quiet: Boolean) {
        store.edit { it[booleanPreferencesKey(APP_QUIET + pkg)] = quiet }
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
            stackBanners = p[STACK_BANNERS] ?: d.stackBanners,
            groupByApp = p[GROUP_BY_APP] ?: d.groupByApp,
            whileLocked = enumOr(p[WHILE_LOCKED], d.whileLocked),
            quietHours = p[QUIET_HOURS] ?: d.quietHours,
            quietStartMin = p[QUIET_START] ?: d.quietStartMin,
            quietEndMin = p[QUIET_END] ?: d.quietEndMin,
            historyEnabled = p[HISTORY_ENABLED] ?: d.historyEnabled,
            historyKeepText = p[HISTORY_KEEP_TEXT] ?: d.historyKeepText,
            effect = enumOr(p[EFFECT], d.effect),
            effectIntensity = p[EFFECT_INTENSITY] ?: d.effectIntensity,
            randomStyle = p[RANDOM_STYLE] ?: d.randomStyle,
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
        p[STACK_BANNERS] = g.stackBanners
        p[GROUP_BY_APP] = g.groupByApp
        p[WHILE_LOCKED] = g.whileLocked.name
        p[QUIET_HOURS] = g.quietHours
        p[QUIET_START] = g.quietStartMin
        p[QUIET_END] = g.quietEndMin
        p[HISTORY_ENABLED] = g.historyEnabled
        p[HISTORY_KEEP_TEXT] = g.historyKeepText
        p[EFFECT] = g.effect.name
        p[EFFECT_INTENSITY] = g.effectIntensity
        p[RANDOM_STYLE] = g.randomStyle
    }

    private fun readRules(p: Preferences): Map<String, AppRule> {
        val rules = HashMap<String, AppRule>()
        fun rule(pkg: String) = rules[pkg] ?: AppRule(pkg)
        for ((key, value) in p.asMap()) {
            val name = key.name
            when {
                name.startsWith(APP_ENABLED) -> {
                    val pkg = name.removePrefix(APP_ENABLED)
                    rules[pkg] = rule(pkg).copy(enabled = value as Boolean)
                }
                name.startsWith(APP_QUIET) -> {
                    val pkg = name.removePrefix(APP_QUIET)
                    rules[pkg] = rule(pkg).copy(quietWhenOpen = value as Boolean)
                }
                name.startsWith(APP_STYLE) -> {
                    val pkg = name.removePrefix(APP_STYLE)
                    val style = StyleId.entries.firstOrNull { it.name == value } ?: continue
                    rules[pkg] = rule(pkg).copy(style = style)
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
        const val APP_QUIET = "app_quiet:"
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
        val STACK_BANNERS = booleanPreferencesKey("stack_banners")
        val GROUP_BY_APP = booleanPreferencesKey("group_by_app")
        val WHILE_LOCKED = stringPreferencesKey("while_locked")
        val QUIET_HOURS = booleanPreferencesKey("quiet_hours")
        val QUIET_START = intPreferencesKey("quiet_start")
        val QUIET_END = intPreferencesKey("quiet_end")
        val HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val HISTORY_KEEP_TEXT = booleanPreferencesKey("history_keep_text")
        val EFFECT = stringPreferencesKey("effect")
        val EFFECT_INTENSITY = intPreferencesKey("effect_intensity")
        val RANDOM_STYLE = booleanPreferencesKey("random_style")
    }
}
