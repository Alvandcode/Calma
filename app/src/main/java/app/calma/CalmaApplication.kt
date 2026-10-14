package app.calma

import android.app.Application
import android.content.Context
import app.calma.audio.SoundMixerEngine
import app.calma.core.common.SystemTimeProvider
import app.calma.core.common.TimeProvider
import app.calma.core.notifications.NotificationChannels
import app.calma.data.local.CalmaDatabase
import app.calma.data.prefs.SettingsStore
import app.calma.data.repository.CalmRepository
import app.calma.data.repository.FocusRepository
import app.calma.data.repository.MoodRepository
import app.calma.data.repository.SoundMixRepository
import app.calma.domain.exercise.CalmExerciseCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container.
 *
 * Calma deliberately avoids Hilt: the graph is small, fully known at compile time, and an
 * annotation processor would slow the build for no real benefit. Everything is constructed
 * lazily and lives for the process lifetime.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val timeProvider: TimeProvider = SystemTimeProvider()

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: CalmaDatabase by lazy { CalmaDatabase.get(appContext) }

    val settingsStore: SettingsStore by lazy { SettingsStore(appContext) }

    val soundEngine: SoundMixerEngine by lazy {
        SoundMixerEngine(appContext, timeProvider, applicationScope)
    }

    val focusRepository: FocusRepository by lazy { FocusRepository(database.focusDao()) }

    val moodRepository: MoodRepository by lazy { MoodRepository(database.moodDao()) }

    val calmRepository: CalmRepository by lazy { CalmRepository(database.calmDao()) }

    val soundMixRepository: SoundMixRepository by lazy { SoundMixRepository(database.soundMixDao()) }

    val calmExercises: CalmExerciseCatalog by lazy { CalmExerciseCatalog() }

    /** Removes every user-generated row. Settings are cleared separately by the caller. */
    suspend fun wipeActivityData() {
        database.focusDao().deleteAllSessions()
        database.focusDao().deleteActive()
        database.moodDao().deleteAll()
        database.calmDao().deleteAll()
        database.soundMixDao().clearAll()
    }
}

class CalmaApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        LocaleController.rememberTag(this, LocaleController.loadPersistedTag(this))
        container = runCatching { AppContainer(this) }
            .onFailure { e ->
                android.util.Log.e("CalmaApplication", "Failed to initialize AppContainer, wiping data", e)
                // Wipe corrupted data and retry once
                wipeCorruptedData()
                AppContainer(this)
            }.getOrThrow()
        NotificationChannels.createAll(this)
    }

    private fun wipeCorruptedData() {
        runCatching { deleteDatabase(CalmaDatabase.NAME) }
        runCatching { getSharedPreferences("calma_settings", MODE_PRIVATE).edit().clear().apply() }
        runCatching { getSharedPreferences("calma_locale_bootstrap", MODE_PRIVATE).edit().clear().apply() }
    }
}

/** Convenience accessor used by Activities and Services. */
val Context.calmaContainer: AppContainer
    get() = (applicationContext as CalmaApplication).container