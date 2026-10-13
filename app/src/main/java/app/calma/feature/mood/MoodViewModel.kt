package app.calma.feature.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.calma.core.common.TimeProvider
import app.calma.core.common.TimeUtils
import app.calma.data.repository.MoodRepository
import app.calma.domain.model.MoodEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class MoodUiState(
    val entries: List<MoodEntry> = emptyList(),
    val draft: MoodEntry = MoodEntry(),
    val isEditingExisting: Boolean = false,
    val savedToday: Boolean = false,
    val loadFailed: Boolean = false,
) {
    val canSave: Boolean get() = draft.isValid()
}

/**
 * Daily check-in screen state.
 *
 * Notes are held only in memory and in the local database; they are never logged and never
 * leave the device.
 */
class MoodViewModel(
    private val repository: MoodRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoodUiState())
    val uiState: StateFlow<MoodUiState> = _uiState.asStateFlow()

    init {
        repository.observeRecent(120)
            .onEach { entries ->
                val today = TimeUtils.startOfDay(timeProvider.currentTimeMillis())
                val todayEntry = entries.firstOrNull {
                    TimeUtils.startOfDay(it.recordedAtEpochMillis) == today
                }
                _uiState.value = _uiState.value.copy(
                    entries = entries,
                    savedToday = todayEntry != null,
                    loadFailed = false,
                )
            }
            .launchIn(viewModelScope)

        viewModelScope.launch { loadTodayDraft() }
    }

    private suspend fun loadTodayDraft() {
        val today = TimeUtils.startOfDay(timeProvider.currentTimeMillis())
        val existing = repository.latestOnDay(today)
        _uiState.value = _uiState.value.copy(
            draft = existing ?: MoodEntry(recordedAtEpochMillis = timeProvider.currentTimeMillis()),
            isEditingExisting = existing != null,
            savedToday = existing != null,
        )
    }

    fun setMood(value: Int) = updateDraft { it.copy(mood = value) }

    fun setStress(value: Int) = updateDraft { it.copy(stress = value) }

    fun setEnergy(value: Int) = updateDraft { it.copy(energy = value) }

    fun setConcentration(value: Int) = updateDraft { it.copy(concentration = value) }

    fun setNote(note: String) = updateDraft { it.copy(note = note.take(MAX_NOTE_LENGTH)) }

    private fun updateDraft(transform: (MoodEntry) -> MoodEntry) {
        _uiState.value = _uiState.value.copy(draft = transform(_uiState.value.draft))
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            repository.save(state.draft, timeProvider.currentTimeMillis())
            loadTodayDraft()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            loadTodayDraft()
        }
    }

    /** Re-opens a past entry for editing. */
    fun edit(entry: MoodEntry) {
        _uiState.value = _uiState.value.copy(draft = entry, isEditingExisting = true)
    }

    fun startNew() {
        _uiState.value = _uiState.value.copy(
            draft = MoodEntry(recordedAtEpochMillis = timeProvider.currentTimeMillis()),
            isEditingExisting = false,
        )
    }

    companion object {
        const val MAX_NOTE_LENGTH = 500

        fun factory(
            repository: MoodRepository,
            timeProvider: TimeProvider,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MoodViewModel(repository, timeProvider) as T
        }
    }
}