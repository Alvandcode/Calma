package app.calma.feature.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.calma.R
import app.calma.core.common.Format
import app.calma.core.common.TimeUtils
import app.calma.core.di.appContainer
import app.calma.core.notifications.NotificationChannels
import app.calma.core.ui.component.BreathingRing
import app.calma.core.ui.component.SectionHeader
import app.calma.core.ui.theme.CalmaSpacing
import app.calma.domain.model.BreakPresets
import app.calma.domain.model.FocusTimerState

@Composable
fun FocusScreen(onBack: () -> Unit) {
    val container = appContainer()
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings by container.settingsStore.settings
        .collectAsStateWithLifecycle(initialValue = null)

    val viewModel: FocusViewModel = viewModel(
        factory = FocusViewModel.factory(
            repository = container.focusRepository,
            settingsStore = container.settingsStore,
            timeProvider = container.timeProvider,
        ),
    )
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val selectedFocus by viewModel.selectedFocusMinutes.collectAsStateWithLifecycle()
    val selectedBreak by viewModel.selectedBreakMinutes.collectAsStateWithLifecycle()
    val event by viewModel.events.collectAsStateWithLifecycle()

    LaunchedEffect(event) {
        val current = event ?: return@LaunchedEffect
        when (current) {
            is FocusUiEvent.Completed -> {
                val notificationsAllowed = settings?.notificationsEnabled ?: true
                if (notificationsAllowed) {
                    NotificationChannels.notify(
                        context = context,
                        notification = NotificationChannels.sessionCompletedNotification(
                            context = context,
                            title = context.getString(R.string.focus_completed_title),
                            text = context.getString(
                                R.string.focus_notification_done_text,
                                Format.duration(context, current.focusedMillis),
                            ),
                        ),
                        id = SESSION_NOTIFICATION_ID,
                    )
                }
                viewModel.consumeEvent()
            }

            FocusUiEvent.Discarded -> viewModel.consumeEvent()
            FocusUiEvent.BreakFinished -> viewModel.consumeEvent()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(CalmaSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CalmaSpacing.xl),
    ) {
        if (timerState.isActive) {
            RunningSession(
                state = timerState,
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onFinish = viewModel::finish,
                onCancel = viewModel::cancel,
            )
        } else {
            IdleSetup(
                selectedFocusMinutes = selectedFocus,
                selectedBreakMinutes = selectedBreak,
                onSelectFocus = viewModel::selectFocusPreset,
                onSelectBreak = viewModel::selectBreakPreset,
                onStart = viewModel::start,
            )
        }
    }
}

@Composable
private fun IdleSetup(
    selectedFocusMinutes: Int,
    selectedBreakMinutes: Int,
    onSelectFocus: (Int) -> Unit,
    onSelectBreak: (Int) -> Unit,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CalmaSpacing.xl),
    ) {
        SectionHeader(title = stringResource(R.string.focus_duration_label))
        PresetRow(
            options = app.calma.domain.model.FocusPresets.minutes,
            selected = selectedFocusMinutes,
            onSelect = onSelectFocus,
        )
        CustomMinutesRow(
            value = selectedFocusMinutes,
            onValueChange = onSelectFocus,
            min = 1,
            max = 180,
            label = stringResource(R.string.focus_custom_minutes),
        )

        SectionHeader(title = stringResource(R.string.focus_break_label))
        PresetRow(
            options = BreakPresets.minutes,
            selected = selectedBreakMinutes,
            onSelect = onSelectBreak,
        )

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("focus_start_button"),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                text = stringResource(R.string.focus_start),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun PresetRow(
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.sm),
    ) {
        options.forEach { minutes ->
            FilterChip(
                selected = minutes == selected,
                onClick = { onSelect(minutes) },
                label = { Text(stringResource(R.string.focus_minutes, minutes)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Increment/decrement stepper for durations outside the presets. */
@Composable
private fun CustomMinutesRow(
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int,
    label: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.md),
        ) {
            OutlinedButton(
                onClick = { onValueChange(value - STEP) },
                enabled = value > min,
                modifier = Modifier.size(48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) { Text("−") }
            Text(
                text = stringResource(R.string.focus_minutes, value),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.size(width = 88.dp, height = 24.dp),
            )
            OutlinedButton(
                onClick = { onValueChange(value + STEP) },
                enabled = value < max,
                modifier = Modifier.size(48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) { Text("+") }
        }
    }
}

@Composable
private fun RunningSession(
    state: FocusTimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
) {
    val clock = Format.clock(state.remainingMillis)
    val phaseLabel = when (state.phase) {
        FocusTimerState.Phase.FOCUS -> stringResource(R.string.focus_phase_focus)
        FocusTimerState.Phase.BREAK -> stringResource(R.string.focus_phase_break)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CalmaSpacing.xl),
    ) {
        Text(
            text = phaseLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            BreathingRing(
                scale = 0.85f + 0.15f * state.progress,
                diameter = 240.dp,
                progress = state.progress,
                progressColor = if (state.phase == FocusTimerState.Phase.BREAK) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                contentDescription = "$phaseLabel, $clock",
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = clock,
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.testTag("focus_countdown"),
                )
                if (state.phase == FocusTimerState.Phase.BREAK) {
                    Text(
                        text = Format.duration(state.focusedMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.md),
        ) {
            when (state.runState) {
                FocusTimerState.RunState.RUNNING -> FilledTonalButton(
                    onClick = onPause,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) { Text(stringResource(R.string.focus_pause)) }

                FocusTimerState.RunState.PAUSED -> FilledTonalButton(
                    onClick = onResume,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) { Text(stringResource(R.string.focus_resume)) }

                else -> Unit
            }
            OutlinedButton(
                onClick = onFinish,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(),
            ) { Text(stringResource(R.string.focus_finish)) }
        }

        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.focus_cancel)) }
    }
}

private const val STEP = 5
private const val SESSION_NOTIFICATION_ID = 3001