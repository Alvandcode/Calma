package app.calma

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Applies the user's explicit language choice without pulling in AppCompat.
 *
 * * API 33+ uses the platform [android.app.LocaleManager], which persists the choice in
 *   system settings and surfaces the per-app language entry.
 * * Below API 33 the Activity overrides the configuration locale in
 *   [wrapContext], which is the long-standing approach for per-app languages.
 *
 * Because `attachBaseContext` runs before any coroutine can start, the chosen tag is also
 * mirrored into a tiny SharedPreferences file ([rememberTag]) so the locale can be applied
 * synchronously on the very first frame.
 *
 * Both translations are always compiled into the APK, so Persian stays available on every
 * supported API level and the app remains fully functional offline.
 */
object LocaleController {

    private const val BOOT_PREFS = "calma_locale_bootstrap"
    private const val KEY_TAG = "language_tag"

    val usesPlatformLocaleManager: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Reads the mirrored tag; called once from [CalmaApplication.onCreate]. */
    fun loadPersistedTag(context: Context): String? = runCatching {
        context.applicationContext
            .getSharedPreferences(BOOT_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TAG, null)
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

    /** Mirrors [tag] so the next cold start can apply it synchronously. */
    fun rememberTag(context: Context, tag: String?) {
        runCatching {
            context.applicationContext
                .getSharedPreferences(BOOT_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TAG, tag)
                .apply()
        }
    }

    /** Current effective language tag (reads from DataStore via SettingsStore). */
    fun getCurrentTag(context: Context): String? {
        val prefs = context.applicationContext
            .getSharedPreferences(BOOT_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TAG, null)?.takeIf { it.isNotBlank() }
    }

    fun apply(context: Context, tag: String?) {
        if (!usesPlatformLocaleManager) return
        val manager = context.getSystemService(android.app.LocaleManager::class.java) ?: return
        val target = if (tag.isNullOrBlank()) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(tag)
        }
        if (manager.applicationLocales != target) {
            manager.applicationLocales = target
        }
    }

    /** Wraps [base] with the locale [tag], or returns it unchanged for the system default. */
    fun wrapContext(base: Context, tag: String?): Context {
        if (tag.isNullOrBlank()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }
}