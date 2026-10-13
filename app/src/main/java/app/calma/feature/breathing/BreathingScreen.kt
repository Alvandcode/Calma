package app.calma.feature.breathing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.calma.R
import app.calma.core.common.BreathPhase
import app.calma.core.common.Format
import app.calma.core.di.appContainer
import app.calma.core.ui.component.BreathingRing
import app.calma.core.ui.component.SectionHeader
import app.calma.core.ui.theme.CalmaSpacing
import app.calma.domain.breathing.BreathSessionState
import app.calma.domain.breathing.BreathingPattern
import app.calma.domain.breathing.BreathingScheduler

@Composable
fun BreathingScreen() {
    val container = appContainer()
    val haptics = LocalHapticFeedback.current

    val viewModel: BreathingViewModel = viewModel(
        factory = BreathingViewModel.factory(
            calmRepository = container.calmRepository,
            settingsStore = container.settingsStore,
            timeProvider = container.timeProvider,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by container.settingsStore.settings.collectAsStateWithLifecycle(initialValue = null)
    val phaseChange by viewModel.phaseChangeCount.collectAsStateWithLifecycle()

    // Exactly one short pulse per phase change, and never when haptics are off.
    LaunchedEffect(phaseChange, settings?.hapticsEnabled) {
        if (phaseChange > 0 && settings?.hapticsEnabled == true) {
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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
        if (state.isIdle) {
            SetupSection(
                state = state,
                onSelectPattern = viewModel::selectPattern,
                onSelectDuration = viewModel::selectDurationSeconds,
                onStart = viewModel::start,
            )
        } else {
            RunningSection(
                state = state,
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onStop = viewModel::stop,
            )
        }
    }
}

@Composable
private fun SetupSection(
    state: BreathingUiState,
    onSelectPattern: (String) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CalmaSpacing.xl),
    ) {
        Text(
            text = stringResource(R.string.breathe_pattern),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BreathingPattern.BUILT_IN.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.sm)) {
                pair.forEach { pattern ->
                    FilterChip(
                        selected = pattern.id == state.selectedPatternId,
                        onClick = { onSelectPattern(pattern.id) },
                        label = { Text(patternLabel(pattern)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }

        SectionHeader(title = stringResource(R.string.breathe_duration))
        Row(horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.sm)) {
            BreathingPattern.DURATION_OPTIONS_SECONDS.forEach { seconds ->
                FilterChip(
                    selected = seconds == state.selectedDurationSeconds,
                    onClick = { onSelectDuration(seconds) },
                    label = { Text(Format.minutes(seconds / 60)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("breathing_start_button"),
            shape = MaterialTheme.shapes.large,
        ) { Text(stringResource(R.string.breathe_start), style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun patternLabel(pattern: BreathingPattern): String {
    val inhale = pattern.inhaleMillis / 1000
    val exhale = pattern.exhaleMillis / 1000
    return "$inhale · $exhale"
}

@Composable
private fun RunningSection(
    state: BreathingUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    val session = state.session
    val phaseLabel = stringResource(
        when (session.phase) {
            BreathPhase.INHALE -> R.string.breathe_phase_inhale
            BreathPhase.HOLD_AFTER_INHALE -> R.string.breathe_phase_hold_after_inhale
            BreathPhase.EXHALE -> R.string.breathe_phase_exhale
            BreathPhase.HOLD_AFTER_EXHALE -> R.string.breathe_phase_hold_after_exhale
        },
    )
    val scale = BreathingScheduler.circleScale(
        phase = session.phase,
        phaseProgress = session.phaseProgress,
        pattern = state.pattern,
    )
    val remaining = Format.clock(session.remainingMillis)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CalmaSpacing.xl),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(300.dp),
            contentAlignment = Alignment.Center,
        ) {
            BreathingRing(
                scale = scale,
                diameter = 280.dp,
                progress = session.phaseProgress,
                contentDescription = "$phaseLabel, ${session.phaseRemainingSeconds}",
                modifier = Modifier.testTag("breathing_ring"),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = phaseLabel,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = session.phaseRemainingSeconds.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.testTag("breathing_phase_count"),
                )
            }
        }

        Text(
            text = stringResource(R.string.breathe_remaining, remaining),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (session.totalCycles > 0) {
            Text(
                text = stringResource(
                    R.string.breathe_round,
                    (session.cycleIndex + 1).coerceAtMost(session.totalCycles),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.md),
        ) {
            when (session.runState) {
                BreathSessionState.RunState.RUNNING -> FilledTonalButton(
                    onClick = onPause,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) { Text(stringResource(R.string.breathe_pause)) }

                BreathSessionState.RunState.PAUSED -> FilledTonalButton(
                    onClick = onResume,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) { Text(stringResource(R.string.breathe_resume)) }

                BreathSessionState.RunState.FINISHED -> Button(
                    onClick = {},
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = false,
                ) { Text(stringResource(R.string.breathe_done_title)) }

                BreathSessionState.RunState.IDLE -> Unit
            }
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) { Text(stringResource(R.string.breathe_stop)) }
        }
    }
}