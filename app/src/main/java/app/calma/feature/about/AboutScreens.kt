package app.calma.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.calma.R
import app.calma.core.ui.component.CalmaCard
import app.calma.core.ui.component.CalmaCard
import app.calma.core.ui.component.SectionHeader
import app.calma.core.ui.theme.CalmaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
            verticalArrangement = Arrangement.spacedBy(CalmaSpacing.md),
            content = { content() },
        )
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    DetailScaffold(title = stringResource(R.string.settings_about), onBack = onBack) {
        Text(
            text = stringResource(R.string.settings_about_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        SectionHeader(title = stringResource(R.string.settings_licenses))
        Text(
            text = stringResource(R.string.settings_licenses_none),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    DetailScaffold(title = stringResource(R.string.settings_privacy), onBack = onBack) {
        Text(
            text = stringResource(R.string.permissions_none_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        SectionHeader(title = stringResource(R.string.settings_section_privacy))
        Text(
            text = stringResource(R.string.settings_erase_all_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun UserGuideScreen(onBack: () -> Unit) {
    DetailScaffold(title = stringResource(R.string.settings_help_title), onBack = onBack) {
        SectionHeader(title = stringResource(R.string.settings_help_focus_title))
        Text(
            text = stringResource(R.string.settings_help_focus_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        SectionHeader(title = stringResource(R.string.settings_help_breathe_title))
        Text(
            text = stringResource(R.string.settings_help_breathe_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        SectionHeader(title = stringResource(R.string.settings_help_sounds_title))
        Text(
            text = stringResource(R.string.settings_help_sounds_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        SectionHeader(title = stringResource(R.string.settings_help_mood_title))
        Text(
            text = stringResource(R.string.settings_help_mood_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        SectionHeader(title = stringResource(R.string.settings_help_stats_title))
        Text(
            text = stringResource(R.string.settings_help_stats_body),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/**
 * Explains every permission Calma declares and why it needs one.
 *
 * Documentation lives in the app as well as in `docs/permissions.md`, because a user
 * should never have to leave Calma to find out what it asks for and why.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    DetailScaffold(title = stringResource(R.string.permissions_title), onBack = onBack) {
        Text(
            text = stringResource(R.string.permissions_intro),
            style = MaterialTheme.typography.bodyLarge,
        )
        CalmaCard(
            title = stringResource(R.string.permissions_notifications_title),
            subtitle = stringResource(R.string.permissions_notifications_body),
        )
        CalmaCard(
            title = stringResource(R.string.permissions_vibrate_title),
            subtitle = stringResource(R.string.permissions_vibrate_body),
        )
        CalmaCard(
            title = stringResource(R.string.permissions_foreground_title),
            subtitle = stringResource(R.string.permissions_foreground_body),
        )
        CalmaCard(
            title = stringResource(R.string.permissions_wakelock_title),
            subtitle = stringResource(R.string.permissions_wakelock_body),
        )
        SectionHeader(title = stringResource(R.string.permissions_none_title))
        Text(
            text = stringResource(R.string.permissions_none_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}