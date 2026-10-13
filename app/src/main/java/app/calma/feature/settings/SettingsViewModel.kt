package app.calma.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.calma.AppContainer
import app.calma.LocaleController
import app.calma.core.common.AppLanguage
import app.calma.core.common.FocusMode
import app.calma.core.common.ThemeMode
import app.calma.data.prefs.CalmaSettings
import app.calma.data.prefs.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: CalmaSettings = CalmaSettings(),
    val versionName: String = "",
    val isErasing: Boolean = false,
)

/** Wraps the settings store for the Settings screen. */
class SettingsViewModel(
    private val container: AppContainer,
    private val appContext: android.content.Context,
) : ViewModel() {

    val settings: StateFlow<CalmaSettings> = container.settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, CalmaSettings())

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _messages = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val messages: StateFlow<String?> = _messages.asStateFlow()

    init {
        viewModelScope.launch {
            settings.collect { value -> _uiState.value = _uiState.value.copy(settings = value) }
        }
        _uiState.value = _uiState.value.copy(versionName = readVersionName())
    }

    // ---- preferences -----------------------------------------------------------------------

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            container.settingsStore.setLanguage(language)
            // Mirror synchronously so the very next cold start applies the locale before
            // the first frame, then let the platform persist it when supported.
            LocaleController.rememberTag(appContext, language.languageTag)
            LocaleController.apply(appContext, language.languageTag)
        }
    }

    /** Called once from the Activity so the platform locale matches the stored preference. */
    fun applyLanguagePreference() {
        viewModelScope.launch {
            val language = container.settingsStore.settings.first().language
            LocaleController.apply(appContext, language.languageTag)
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { container.settingsStore.setThemeMode(mode) }
    }

    fun setDefaultFocusMinutes(minutes: Int) {
        viewModelScope.launch { container.settingsStore.setDefaultFocusMinutes(minutes) }
    }

    fun setDefaultBreakMinutes(minutes: Int) {
        viewModelScope.launch { container.settingsStore.setDefaultBreakMinutes(minutes) }
    }

    fun setFocusMode(mode: FocusMode) {
        viewModelScope.launch { container.settingsStore.setFocusMode(mode) }
    }

    fun setInterfaceSounds(enabled: Boolean) {
        viewModelScope.launch { container.settingsStore.setInterfaceSounds(enabled) }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { container.settingsStore.setHaptics(enabled) }
    }

    fun setSessionChime(enabled: Boolean) {
        viewModelScope.launch { container.settingsStore.setSessionChime(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsStore.setNotificationsEnabled(enabled) }
    }

    fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        container.soundEngine.setBackgroundPlaybackEnabled(enabled)
        viewModelScope.launch {
            container.settingsStore.setBackgroundPlaybackEnabled(enabled)
        }
    }

    // ---- data -------------------------------------------------------------------------------

    /** Removes sessions, calm exercises and check-ins. Settings and mixes are kept. */
    fun clearHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isErasing = true)
            container.wipeActivityData()
            _uiState.value = _uiState.value.copy(isErasing = false)
            _messages.value = MESSAGE_CLEARED
        }
    }

    /** Removes everything, including settings, mixes and notes. */
    fun eraseAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isErasing = true)
            container.soundEngine.stopAll()
            container.wipeActivityData()
            container.settingsStore.resetToDefaults()
            LocaleController.rememberTag(appContext, null)
            LocaleController.apply(appContext, null)
            _uiState.value = _uiState.value.copy(isErasing = false)
            _messages.value = MESSAGE_ERASED
        }
    }

    fun consumeMessage() {
        _messages.value = null
    }

    private fun readVersionName(): String = runCatching {
        val pkg = appContext.packageName
        appContext.packageManager.getPackageInfo(pkg, 0).versionName ?: "1.0.0"
    }.getOrDefault("1.0.0")

    companion object {
        const val MESSAGE_CLEARED = "history_cleared"
        const val MESSAGE_ERASED = "all_erased"

        fun factory(
            container: AppContainer,
            appContext: android.content.Context,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(container, appContext) as T
        }
    }
}