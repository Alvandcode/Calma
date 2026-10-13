package app.calma.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.calma.core.common.AppLanguage
import app.calma.core.common.FocusMode
import app.calma.core.common.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calma_settings")

/**
 * User settings, persisted with Jetpack DataStore.
 *
 * Everything here is local. There is no sync, no backup and no remote source, and
 * "Erase all local data" simply clears this store alongside the Room database.
 */
data class CalmaSettings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultFocusMinutes: Int = 25,
    val defaultBreakMinutes: Int = 5,
    val focusMode: FocusMode = FocusMode.POMODORO,
    val interfaceSoundsEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val sessionChimeEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val soundBackgroundPlaybackEnabled: Boolean = true,
    /** Last used breathing pattern id, so the screen reopens where the user left it. */
    val lastBreathingPatternId: String = "calm_4_6",
    /** Last used breathing duration in seconds. */
    val lastBreathingDurationSeconds: Int = 180,
    /** Last used sound mixer volumes, encoded as `soundId:volume` pairs. */
    val lastMixVolumes: String = "",
    /** Epoch millis of the last quick-calm session; drives deterministic rotation. */
    val lastCalmExerciseId: String = "",
) {
    val focusMillis: Long get() = defaultFocusMinutes * 60_000L
    val breakMillis: Long get() = defaultBreakMinutes * 60_000L
}

class SettingsStore(private val context: Context) {

    val settings: Flow<CalmaSettings> = context.dataStore.data.map { prefs ->
        CalmaSettings(
            language = AppLanguage.fromTag(prefs[Keys.LANGUAGE]),
            themeMode = ThemeMode.fromName(prefs[Keys.THEME]),
            defaultFocusMinutes = (prefs[Keys.FOCUS_MINUTES] ?: 25).coerceIn(1, 180),
            defaultBreakMinutes = (prefs[Keys.BREAK_MINUTES] ?: 5).coerceIn(0, 60),
            focusMode = FocusMode.fromName(prefs[Keys.FOCUS_MODE]),
            interfaceSoundsEnabled = prefs[Keys.UI_SOUNDS] ?: false,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            sessionChimeEnabled = prefs[Keys.SESSION_CHIME] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            soundBackgroundPlaybackEnabled = prefs[Keys.BACKGROUND_PLAYBACK] ?: true,
            lastBreathingPatternId = prefs[Keys.BREATHING_PATTERN] ?: "calm_4_6",
            lastBreathingDurationSeconds = (prefs[Keys.BREATHING_DURATION] ?: 180)
                .coerceIn(30, 1800),
            lastMixVolumes = prefs[Keys.MIX_VOLUMES] ?: "",
            lastCalmExerciseId = prefs[Keys.LAST_CALM_EXERCISE] ?: "",
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.languageTag.orEmpty() }
    }

    suspend fun setThemeMode(mode: ThemeMode) = put(Keys.THEME, mode.name)

    suspend fun setDefaultFocusMinutes(minutes: Int) =
        put(Keys.FOCUS_MINUTES, minutes.coerceIn(1, 180))

    suspend fun setDefaultBreakMinutes(minutes: Int) =
        put(Keys.BREAK_MINUTES, minutes.coerceIn(0, 60))

    suspend fun setFocusMode(mode: FocusMode) = put(Keys.FOCUS_MODE, mode.name)

    suspend fun setInterfaceSounds(enabled: Boolean) = put(Keys.UI_SOUNDS, enabled)

    suspend fun setHaptics(enabled: Boolean) = put(Keys.HAPTICS, enabled)

    suspend fun setSessionChime(enabled: Boolean) = put(Keys.SESSION_CHIME, enabled)

    suspend fun setNotificationsEnabled(enabled: Boolean) = put(Keys.NOTIFICATIONS, enabled)

    suspend fun setBackgroundPlaybackEnabled(enabled: Boolean) =
        put(Keys.BACKGROUND_PLAYBACK, enabled)

    suspend fun setBreathingPattern(patternId: String) =
        put(Keys.BREATHING_PATTERN, patternId)

    suspend fun setBreathingDuration(seconds: Int) =
        put(Keys.BREATHING_DURATION, seconds.coerceIn(30, 1800))

    suspend fun setMixVolumes(encoded: String) = put(Keys.MIX_VOLUMES, encoded)

    suspend fun setLastCalmExercise(id: String) = put(Keys.LAST_CALM_EXERCISE, id)

    /** Restores every setting to its shipped default, keeping the store alive. */
    suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
    }

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme_mode")
        val FOCUS_MINUTES = intPreferencesKey("default_focus_minutes")
        val BREAK_MINUTES = intPreferencesKey("default_break_minutes")
        val FOCUS_MODE = stringPreferencesKey("focus_mode")
        val UI_SOUNDS = booleanPreferencesKey("ui_sounds")
        val HAPTICS = booleanPreferencesKey("haptics")
        val SESSION_CHIME = booleanPreferencesKey("session_chime")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
        val BREATHING_PATTERN = stringPreferencesKey("breathing_pattern")
        val BREATHING_DURATION = intPreferencesKey("breathing_duration")
        val MIX_VOLUMES = stringPreferencesKey("mix_volumes")
        val LAST_CALM_EXERCISE = stringPreferencesKey("last_calm_exercise")
    }
}