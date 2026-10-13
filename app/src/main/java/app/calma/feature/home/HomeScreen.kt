package app.calma.feature.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Waves
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.calma.R
import app.calma.core.common.Format
import app.calma.core.di.appContainer
import app.calma.core.ui.component.CalmaCard
import app.calma.core.ui.component.CalmaCard
import app.calma.core.ui.component.SectionHeader
import app.calma.core.ui.component.StatTile
import app.calma.core.ui.theme.CalmaSpacing
import java.util.Calendar

/**
 * Home.
 *
 * Deliberately short: one prominent action ("Calm now"), a compact summary of today and a
 * handful of direct entry points. Nothing that needs a network, an account or an ad.
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

        QuickCalmCard(onClick = onOpenCalm)

        TodaySection(
            state = state,
            onOpenMood = onOpenMood,
        )

        SectionHeader(title = stringResource(R.string.home_tagline))
        ShortcutGrid(
            onOpenFocus = onOpenFocus,
            onOpenBreathe = onOpenBreathe,
            onOpenSounds = onOpenSounds,
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
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun QuickCalmCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(CalmaSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(CalmaSpacing.sm),
        ) {
            Text(
                text = stringResource(R.string.home_quick_calm),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.home_quick_calm_caption),
                style = MaterialTheme.typography.bodyMedium,
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

@Composable
private fun ShortcutGrid(
    onOpenFocus: () -> Unit,
    onOpenBreathe: () -> Unit,
    onOpenSounds: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CalmaSpacing.sm)) {
        CalmaCard(
            title = stringResource(R.string.home_start_focus),
            icon = Icons.Outlined.Timer,
            onClick = onOpenFocus,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.sm)) {
            CalmaCard(
                title = stringResource(R.string.home_breathe_now),
                icon = Icons.Outlined.SelfImprovement,
                onClick = onOpenBreathe,
                modifier = Modifier.weight(1f),
            )
            CalmaCard(
                title = stringResource(R.string.home_sound_mixer),
                icon = Icons.Outlined.Waves,
                onClick = onOpenSounds,
                modifier = Modifier.weight(1f),
            )
        }
    }
}