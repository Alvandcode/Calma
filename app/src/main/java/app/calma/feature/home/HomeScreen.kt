package app.calma.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.calma.R
import app.calma.core.common.Format
import app.calma.core.di.appContainer
import app.calma.core.ui.component.CalmaCard
import app.calma.core.ui.component.SectionHeader
import app.calma.core.ui.component.StatTile
import app.calma.core.ui.theme.CalmaSpacing
import java.util.Calendar

/**
 * Home.
 *
 * Warm ember hero ("Recommended" card), quick-entry chips and a compact summary of
 * today. Nothing that needs a network, an account or an ad.
 */
@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onOpenCalm: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenBreathe: () -> Unit,
    onOpenSounds: () -> Unit,
    onOpenMood: () -> Unit,
) {
    val container = appContainer()
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(
            focusRepository = container.focusRepository,
            calmRepository = container.calmRepository,
            moodRepository = container.moodRepository,
            calmExercises = container.calmExercises,
            timeProvider = container.timeProvider,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(CalmaSpacing.xl),
    ) {
        Greeting()

        HeroCard(onClick = onOpenCalm)

        EntryChips(
            onOpenFocus = onOpenFocus,
            onOpenBreathe = onOpenBreathe,
            onOpenSounds = onOpenSounds,
        )

        TodaySection(
            state = state,
            onOpenMood = onOpenMood,
        )
    }
}

@Composable
private fun Greeting() {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> R.string.home_greeting_morning
        in 12..16 -> R.string.home_greeting_afternoon
        else -> R.string.home_greeting_evening
    }
    Text(
        text = stringResource(greeting),
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun HeroCard(onClick: () -> Unit) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
        ),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(CalmaSpacing.xl),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CalmaSpacing.sm)) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text(
                    text = stringResource(R.string.home_recommended),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(
                        horizontal = CalmaSpacing.md,
                        vertical = CalmaSpacing.xs,
                    ),
                )
            }
            Text(
                text = stringResource(R.string.home_quick_calm),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.home_quick_calm_caption),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = CalmaSpacing.sm)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(
                    text = stringResource(R.string.calm_start),
                    modifier = Modifier.padding(start = CalmaSpacing.sm),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun EntryChips(
    onOpenFocus: () -> Unit,
    onOpenBreathe: () -> Unit,
    onOpenSounds: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.sm)) {
        AssistChip(
            onClick = onOpenFocus,
            label = { Text(stringResource(R.string.home_start_focus)) },
            leadingIcon = {
                Icon(Icons.Outlined.Timer, contentDescription = null)
            },
            colors = AssistChipDefaults.assistChipColors(),
        )
        AssistChip(
            onClick = onOpenBreathe,
            label = { Text(stringResource(R.string.home_breathe_now)) },
            leadingIcon = {
                Icon(Icons.Outlined.SelfImprovement, contentDescription = null)
            },
            colors = AssistChipDefaults.assistChipColors(),
        )
        AssistChip(
            onClick = onOpenSounds,
            label = { Text(stringResource(R.string.home_sound_mixer)) },
            colors = AssistChipDefaults.assistChipColors(),
        )
    }
}

@Composable
private fun TodaySection(
    state: HomeUiState,
    onOpenMood: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CalmaSpacing.md)) {
        SectionHeader(title = stringResource(R.string.home_daily_summary))
        Row(horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.sm)) {
            StatTile(
                value = Format.duration(state.todayFocusMillis),
                label = stringResource(R.string.home_stat_focus),
                icon = Icons.Outlined.Timer,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = state.todaySessionCount.toString(),
                label = stringResource(R.string.home_stat_sessions),
                icon = Icons.Outlined.BarChart,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.sm)) {
            StatTile(
                value = state.todayCalmCount.toString(),
                label = stringResource(R.string.home_stat_calm),
                icon = Icons.Outlined.SelfImprovement,
                modifier = Modifier.weight(1f),
            )
            CalmaCard(
                title = stringResource(R.string.home_daily_checkin),
                icon = Icons.Outlined.EditNote,
                onClick = onOpenMood,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = state.todayCheckIns.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.End,
                )
            }
        }
        if (!state.isLoading && state.todaySessionCount == 0 &&
            state.todayCalmCount == 0 && state.todayCheckIns == 0
        ) {
            Text(
                text = stringResource(R.string.home_no_activity_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
