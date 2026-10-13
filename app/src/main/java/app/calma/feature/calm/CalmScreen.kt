package app.calma.feature.calm

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.calma.R
import app.calma.core.di.appContainer
import app.calma.core.ui.component.BreathingRing
import app.calma.core.ui.component.CalmaCard
import app.calma.core.ui.component.CalmaCard
import app.calma.core.ui.component.SectionHeader
import app.calma.core.ui.theme.CalmaSpacing
import app.calma.domain.exercise.CalmExercise

/**
 * "Calm now".
 *
 * Offers the bundled short exercises and runs one step at a time. Content ships with the
 * app: no network call, no model, no account.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalmScreen(onBack: () -> Unit) {
    val container = appContainer()
    val viewModel: CalmViewModel = viewModel(
        factory = CalmViewModel.factory(
            catalog = container.calmExercises,
            calmRepository = container.calmRepository,
            settingsStore = container.settingsStore,
            timeProvider = container.timeProvider,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calm_title)) },
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
        val exercise = state.exercise
        if (exercise == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(CalmaSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(CalmaSpacing.md),
            ) {
                Text(
                    text = stringResource(R.string.calm_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                viewModel.exercises.forEach { item ->
                    CalmaCard(
                        title = stringResource(titleOf(item)),
                        subtitle = stringResource(subtitleOf(item)),
                        icon = iconOf(item),
                        onClick = { viewModel.start(item.id) },
                    )
                }
                Text(
                    text = stringResource(R.string.calm_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            RunningCalm(
                exercise = exercise,
                stepIndex = state.stepIndex,
                finished = state.finished,
                onNext = viewModel::next,
                onPrevious = viewModel::previous,
                onStop = viewModel::stop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

@Composable
private fun RunningCalm(
    exercise: CalmExercise,
    stepIndex: Int,
    finished: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(CalmaSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CalmaSpacing.xl),
    ) {
        Text(
            text = stringResource(titleOf(exercise)),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        if (finished) {
            Text(
                text = stringResource(R.string.calm_finish),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onStop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text(stringResource(R.string.calm_finish)) }
        } else {
            if (exercise.isBreathing) {
                BreathingRing(
                    scale = 0.75f,
                    diameter = 200.dp,
                    progress = 1f,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(stringResOf(exercise.steps[stepIndex])),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = stringResource(
                    R.string.calm_step_of,
                    stepIndex + 1,
                    exercise.steps.size,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.md),
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = stepIndex > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) { Text(stringResource(R.string.action_no)) }
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text(
                        stringResource(
                            if (stepIndex + 1 >= exercise.steps.size) {
                                R.string.calm_finish
                            } else {
                                R.string.calm_next
                            },
                        ),
                    )
                }
            }
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.breathe_stop)) }
        }
    }
}

private fun titleOf(exercise: CalmExercise): Int = when (exercise.id) {
    "breathe_slow" -> R.string.calm_breathe_title
    "five_senses" -> R.string.calm_senses_title
    "look_around" -> R.string.calm_surroundings_title
    "be_here_now" -> R.string.calm_mindful_title
    else -> R.string.calm_release_title
}

private fun subtitleOf(exercise: CalmExercise): Int = when (exercise.id) {
    "breathe_slow" -> R.string.calm_breathe_sub
    "five_senses" -> R.string.calm_senses_sub
    "look_around" -> R.string.calm_surroundings_sub
    "be_here_now" -> R.string.calm_mindful_sub
    else -> R.string.calm_release_sub
}

private fun iconOf(exercise: CalmExercise): ImageVector = when (exercise.id) {
    "breathe_slow" -> Icons.Outlined.Air
    "five_senses" -> Icons.Outlined.Visibility
    "look_around" -> Icons.Outlined.WbSunny
    "be_here_now" -> Icons.Outlined.Spa
    else -> Icons.Outlined.Waves
}

/** Maps a catalog step key to its string resource. */
private fun stringResOf(stepKey: String): Int = when (stepKey) {
    "calm_step_senses_1" -> R.string.calm_step_senses_1
    "calm_step_senses_2" -> R.string.calm_step_senses_2
    "calm_step_senses_3" -> R.string.calm_step_senses_3
    "calm_step_senses_4" -> R.string.calm_step_senses_4
    "calm_step_senses_5" -> R.string.calm_step_senses_5
    "calm_step_senses_6" -> R.string.calm_step_senses_6
    "calm_step_surround_1" -> R.string.calm_step_surround_1
    "calm_step_surround_2" -> R.string.calm_step_surround_2
    "calm_step_surround_3" -> R.string.calm_step_surround_3
    "calm_step_surround_4" -> R.string.calm_step_surround_4
    "calm_step_surround_5" -> R.string.calm_step_surround_5
    "calm_step_mindful_1" -> R.string.calm_step_mindful_1
    "calm_step_mindful_2" -> R.string.calm_step_mindful_2
    "calm_step_mindful_3" -> R.string.calm_step_mindful_3
    "calm_step_mindful_4" -> R.string.calm_step_mindful_4
    "calm_step_mindful_5" -> R.string.calm_step_mindful_5
    "calm_step_release_1" -> R.string.calm_step_release_1
    "calm_step_release_2" -> R.string.calm_step_release_2
    "calm_step_release_3" -> R.string.calm_step_release_3
    "calm_step_release_4" -> R.string.calm_step_release_4
    "calm_step_release_5" -> R.string.calm_step_release_5
    else -> R.string.calm_step_mindful_1
}