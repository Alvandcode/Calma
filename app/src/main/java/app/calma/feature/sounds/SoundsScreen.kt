package app.calma.feature.sounds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.calma.R
import app.calma.audio.AmbientSound
import app.calma.core.common.Format
import app.calma.core.di.appContainer
import app.calma.core.ui.component.EmptyState
import app.calma.core.ui.component.SectionHeader
import app.calma.core.ui.theme.CalmaSpacing

/** Sleep-timer choices, in minutes. */
private val SLEEP_TIMER_OPTIONS = listOf(15, 30, 45, 60)

@Composable
fun SoundsScreen(
    onBackgroundPlayback: () -> Unit,
    onBackgroundPlaybackStop: () -> Unit,
) {
    val container = appContainer()
    val lifecycleOwner = LocalLifecycleOwner.current

    val viewModel: SoundsViewModel = viewModel(
        factory = SoundsViewModel.factory(
            engine = container.soundEngine,
            soundMixRepository = container.soundMixRepository,
            settingsStore = container.settingsStore,
            timeProvider = container.timeProvider,
            onBackgroundPlaybackRequested = onBackgroundPlayback,
            onBackgroundPlaybackStopped = onBackgroundPlaybackStop,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onAppResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (state.showSaveDialog) {
        SaveMixDialog(
            onDismiss = { viewModel.showSaveDialog(false) },
            onConfirm = { name -> viewModel.saveMix(name) },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(CalmaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(CalmaSpacing.lg),
    ) {
        TransportRow(
            isPlaying = state.mixer.isPlaying,
            activeCount = state.mixer.activeCount,
            onPlay = viewModel::play,
            onStop = viewModel::stopAll,
        )

        SectionHeader(
            title = stringResource(
                R.string.sounds_layer_count,
                state.mixer.activeCount,
                AmbientSound.MAX_ACTIVE_LAYERS,
            ),
        )

        state.mixer.layers.forEach { layer ->
            SoundLayerRow(
                label = stringResource(layer.sound.labelRes()),
                isPlaying = layer.isPlaying,
                volume = layer.volume,
                enabled = layer.isPlaying || state.canAddMore,
                onToggle = { viewModel.toggleLayer(layer.sound) },
                onVolume = { viewModel.setLayerVolume(layer.sound, it) },
            )
        }

        SleepTimerSection(
            selectedMinutes = state.sleepTimerMinutes,
            remainingMillis = state.sleepTimerRemainingMillis,
            onSelect = viewModel::setSleepTimer,
        )

        BackgroundPlaybackRow(
            enabled = state.mixer.backgroundPlaybackEnabled,
            onChange = viewModel::setBackgroundPlaybackEnabled,
        )

        SectionHeader(
            title = stringResource(R.string.sounds_saved_mixes),
            trailing = {
                TextButton(
                    onClick = { viewModel.showSaveDialog(true) },
                    modifier = Modifier.testTag("save_mix_button"),
                ) { Text(stringResource(R.string.sounds_save_mix)) }
            },
        )

        if (state.savedMixes.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.sounds_no_saved_mixes),
                icon = Icons.Outlined.Pause,
            )
        } else {
            state.savedMixes.forEach { mix ->
                SavedMixRow(
                    name = mix.name,
                    onApply = { viewModel.applyMix(mix) },
                    onDelete = { viewModel.deleteMix(mix) },
                )
            }
        }
    }
}

private fun AmbientSound.labelRes(): Int = when (this) {
    AmbientSound.RAIN -> R.string.sound_rain
    AmbientSound.FOREST -> R.string.sound_forest
    AmbientSound.BIRDS -> R.string.sound_birds
    AmbientSound.SEA -> R.string.sound_sea
    AmbientSound.STREAM -> R.string.sound_stream
    AmbientSound.WIND -> R.string.sound_wind
    AmbientSound.WHITE_NOISE -> R.string.sound_white_noise
    AmbientSound.PINK_NOISE -> R.string.sound_pink_noise
    AmbientSound.BROWN_NOISE -> R.string.sound_brown_noise
}

@Composable
private fun TransportRow(
    isPlaying: Boolean,
    activeCount: Int,
    onPlay: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.md),
    ) {
        Button(
            onClick = onPlay,
            enabled = !isPlaying && activeCount < AmbientSound.MAX_ACTIVE_LAYERS,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Text(
                text = stringResource(R.string.sounds_play_all),
                modifier = Modifier.padding(start = CalmaSpacing.sm),
            )
        }
        OutlinedButton(
            onClick = onStop,
            enabled = isPlaying,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
        ) { Text(stringResource(R.string.sounds_stop_all)) }
    }
}

@Composable
private fun SoundLayerRow(
    label: String,
    isPlaying: Boolean,
    volume: Float,
    enabled: Boolean,
    onToggle: () -> Unit,
    onVolume: (Float) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = CalmaSpacing.md, vertical = CalmaSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(CalmaSpacing.xs),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = isPlaying,
                        enabled = enabled,
                        onValueChange = { onToggle() },
                    )
                    .padding(vertical = CalmaSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = isPlaying,
                    enabled = enabled,
                    onCheckedChange = { onToggle() },
                )
            }
            Slider(
                value = volume,
                onValueChange = onVolume,
                enabled = isPlaying || enabled,
                valueRange = 0f..1f,
                modifier = Modifier.semantics {
                    contentDescription = label
                },
            )
        }
    }
}

@Composable
private fun SleepTimerSection(
    selectedMinutes: Int?,
    remainingMillis: Long,
    onSelect: (Int?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CalmaSpacing.sm)) {
        SectionHeader(title = stringResource(R.string.sounds_sleep_timer))
        Row(horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.sm)) {
            FilterChip(
                selected = selectedMinutes == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.sounds_sleep_timer_off)) },
                modifier = Modifier.weight(1f),
            )
            SLEEP_TIMER_OPTIONS.take(3).forEach { minutes ->
                FilterChip(
                    selected = selectedMinutes == minutes,
                    onClick = { onSelect(minutes) },
                    label = { Text(Format.minutes(minutes)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (selectedMinutes != null && remainingMillis > 0L) {
            Text(
                text = stringResource(
                    R.string.sounds_sleep_timer_remaining,
                    Format.clock(remainingMillis),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BackgroundPlaybackRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CalmaSpacing.xxs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.sounds_continue_in_background),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = enabled, onCheckedChange = onChange)
        }
        Text(
            text = stringResource(R.string.sounds_continue_in_background_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SavedMixRow(
    name: String,
    onApply: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onApply) {
            Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.sounds_play_all))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Pause, contentDescription = stringResource(R.string.sounds_delete_mix))
        }
    }
}

@Composable
private fun SaveMixDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sounds_save_mix)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text(stringResource(R.string.sounds_mix_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.mood_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.mood_cancel)) }
        },
    )
}