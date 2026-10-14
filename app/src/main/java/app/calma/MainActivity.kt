package app.calma

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.calma.core.di.LocalAppContainer
import app.calma.core.navigation.CalmaApp
import app.calma.core.ui.theme.CalmaTheme
import app.calma.feature.settings.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * The single Activity of Calma.
 *
 * Everything above it is Compose. The Activity owns no business logic; it observes the
 * settings that affect theming and language and hands them to [CalmaApp].
 */
class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    private val container: AppContainer
        get() = (application as CalmaApplication).container

    override fun attachBaseContext(newBase: Context) {
        val tag = LocaleController.getCurrentTag(newBase)
        super.attachBaseContext(LocaleController.wrapContext(newBase, tag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            settingsViewModel.applyLanguagePreference()
        }

        setContent {
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalAppContainer provides container) {
                CalmaTheme(
                    themeMode = settings.themeMode,
                    languageTag = settings.language.languageTag,
                ) {
                    CalmaApp()
                }
            }
        }
    }
}