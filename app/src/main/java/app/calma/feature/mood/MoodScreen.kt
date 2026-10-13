package app.calma.feature.mood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.calma.R
import app.calma.core.common.Format
import app.calma.core.di.appContainer
import app.calma.core.ui.component.EmptyState
import app.calma.core.ui.component.SectionHeader
import app.calma.core.ui.theme.CalmaSpacing
import app.calma.domain.model.MoodEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodScreen(onBack: () -> Unit) {
    val container = appContainer()
    val viewModel: MoodViewModel = viewModel(
        factory = MoodViewModel.factory(
            repository = container.moodRepository,
            timeProvider = container.timeProvider,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<MoodEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mood_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(CalmaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(CalmaSpacing.lg),
        ) {
            CheckInForm(
                draft = state.draft,
                isEditing = state.isEditingExisting,
                onMood = viewModel::setMood,
                onStress = viewModel::setStress,
                onEnergy = viewModel::setEnergy,
                onConcentration = viewModel::setConcentration,
                onNote = viewModel::setNote,
                onSave = viewModel::save,
            )

            SectionHeader(title = stringResource(R.string.mood_history))

            if (state.entries.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.mood_empty),
                    description = stringResource(R.string.mood_empty_hint),
                    icon = Icons.Outlined.EditNote,
                )
            } else {
                state.entries.forEach { entry ->
                    HistoryRow(
                        entry = entry,
                        onEdit = { viewModel.edit(entry) },
                        onDelete = { pendingDelete = entry },
                    )
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.mood_delete_confirm_title)) },
            text = { Text(stringResource(R.string.mood_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(entry.id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.mood_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.mood_cancel))
                }
            },
        )
    }
}

@Composable
private fun CheckInForm(
    draft: MoodEntry,
    isEditing: Boolean,
    onMood: (Int) -> Unit,
    onStress: (Int) -> Unit,
    onEnergy: (Int) -> Unit,
    onConcentration: (Int) -> Unit,
    onNote: (String) -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(CalmaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(CalmaSpacing.md),
        ) {
            Text(
                text = stringResource(
                    if (isEditing) R.string.mood_update else R.string.mood_new,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            ScaleSelector(
                label = stringResource(R.string.mood_mood),
                levelLabels = moodLabelRes.map { stringResource(it) },
                selected = draft.mood,
                onSelect = onMood,
            )
            ScaleSelector(
                label = stringResource(R.string.mood_stress),
                levelLabels = stressLabelRes.map { stringResource(it) },
                selected = draft.stress,
                onSelect = onStress,
            )
            ScaleSelector(
                label = stringResource(R.string.mood_energy),
                levelLabels = energyLabelRes.map { stringResource(it) },
                selected = draft.energy,
                onSelect = onEnergy,
            )
            ScaleSelector(
                label = stringResource(R.string.mood_concentration),
                levelLabels = focusLabelRes.map { stringResource(it) },
                selected = draft.concentration,
                onSelect = onConcentration,
            )
            OutlinedTextField(
                value = draft.note.orEmpty(),
                onValueChange = onNote,
                label = { Text(stringResource(R.string.mood_note)) },
                placeholder = { Text(stringResource(R.string.mood_note_hint)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text(stringResource(if (isEditing) R.string.mood_update else R.string.mood_save)) }
        }
    }
}

@Composable
private fun ScaleSelector(
    label: String,
    levelLabels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CalmaSpacing.xs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.xs)) {
            (1..5).forEach { level ->
                FilterChip(
                    selected = level == selected,
                    onClick = { onSelect(level) },
                    label = {
                        Text(
                            text = levelLabels.getOrElse(level - 1) { level.toString() },
                            maxLines = 1,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "$label ${levelLabels.getOrElse(level - 1) { "" }}" },
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: MoodEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CalmaSpacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = Format.dateTime(entry.recordedAtEpochMillis),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onEdit) { Text(stringResource(R.string.mood_update)) }
            TextButton(onClick = onDelete) { Text(stringResource(R.string.mood_delete)) }
        }
        Text(
            text = listOf(
                stringResource(R.string.mood_mood) + " " + stringResource(levelRes(moodLabelRes, entry.mood)),
                stringResource(R.string.mood_stress) + " " + stringResource(levelRes(stressLabelRes, entry.stress)),
                stringResource(R.string.mood_energy) + " " + stringResource(levelRes(energyLabelRes, entry.energy)),
                stringResource(R.string.mood_concentration) + " " + stringResource(levelRes(focusLabelRes, entry.concentration)),
            ).joinToString("  ·  "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!entry.note.isNullOrBlank()) {
            Text(text = entry.note, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private val moodLabelRes = intArrayOf(
    R.string.mood_mood_1, R.string.mood_mood_2, R.string.mood_mood_3,
    R.string.mood_mood_4, R.string.mood_mood_5,
)
private val stressLabelRes = intArrayOf(
    R.string.mood_stress_1, R.string.mood_stress_2, R.string.mood_stress_3,
    R.string.mood_stress_4, R.string.mood_stress_5,
)
private val energyLabelRes = intArrayOf(
    R.string.mood_energy_1, R.string.mood_energy_2, R.string.mood_energy_3,
    R.string.mood_energy_4, R.string.mood_energy_5,
)
private val focusLabelRes = intArrayOf(
    R.string.mood_focus_1, R.string.mood_focus_2, R.string.mood_focus_3,
    R.string.mood_focus_4, R.string.mood_focus_5,
)

private fun levelRes(table: IntArray, level: Int): Int =
    table.getOrElse(level - 1) { R.string.level_none }