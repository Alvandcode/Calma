package app.calma.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.calma.R
import app.calma.core.common.AppLanguage
import app.calma.core.common.ThemeMode
import app.calma.core.di.appContainer
import app.calma.core.ui.component.CalmaCard
import app.calma.core.ui.component.SectionHeader
import app.calma.core.ui.theme.CalmaSpacing
import app.calma.domain.model.BreakPresets
import app.calma.domain.model.FocusPresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    val container = appContainer()
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(container, context),
    )
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.messages.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var confirmClear by remember { mutableStateOf(false) }
    var confirmErase by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        when (message) {
            SettingsViewModel.MESSAGE_CLEARED ->
                snackbarHostState.showSnackbar(context.getString(R.string.settings_clear_history_done))
            SettingsViewModel.MESSAGE_ERASED ->
                snackbarHostState.showSnackbar(context.getString(R.string.settings_erase_all_done))
            else -> Unit
        }
        if (message != null) viewModel.consumeMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            // ---- General ---------------------------------------------------------------
            SectionHeader(title = stringResource(R.string.settings_section_general))
            ChoiceRow(
                title = stringResource(R.string.settings_language),
                options = AppLanguage.entries,
                selected = settings.language,
                label = {
                    when (it) {
                        AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                        AppLanguage.ENGLISH -> stringResource(R.string.settings_language_en)
                        AppLanguage.PERSIAN -> stringResource(R.string.settings_language_fa)
                    }
                },
                onSelect = viewModel::setLanguage,
            )
            ChoiceRow(
                title = stringResource(R.string.settings_theme),
                options = ThemeMode.entries,
                selected = settings.themeMode,
                label = {
                    when (it) {
                        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                    }
                },
                onSelect = viewModel::setTheme,
            )

            // ---- Focus -----------------------------------------------------------------
            SectionHeader(title = stringResource(R.string.settings_section_focus))
            Column(verticalArrangement = Arrangement.spacedBy(CalmaSpacing.xs)) {
                Text(
                    text = stringResource(R.string.settings_default_focus),
                    style = MaterialTheme.typography.bodyLarge,
                )
                ChipGroup(
                    options = FocusPresets.minutes,
                    selected = settings.defaultFocusMinutes,
                    label = { stringResource(R.string.focus_minutes, it) },
                    onSelect = viewModel::setDefaultFocusMinutes,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(CalmaSpacing.xs)) {
                Text(
                    text = stringResource(R.string.settings_default_break),
                    style = MaterialTheme.typography.bodyLarge,
                )
                ChipGroup(
                    options = BreakPresets.minutes,
                    selected = settings.defaultBreakMinutes,
                    label = { stringResource(R.string.focus_minutes, it) },
                    onSelect = viewModel::setDefaultBreakMinutes,
                )
            }

            // ---- Feedback --------------------------------------------------------------
            SectionHeader(title = stringResource(R.string.settings_section_feedback))
            SwitchRow(
                title = stringResource(R.string.settings_sound_enabled),
                subtitle = stringResource(R.string.settings_sound_enabled_desc),
                checked = settings.interfaceSoundsEnabled,
                onChange = viewModel::setInterfaceSounds,
            )
            SwitchRow(
                title = stringResource(R.string.settings_haptics),
                subtitle = stringResource(R.string.settings_haptics_desc),
                checked = settings.hapticsEnabled,
                onChange = viewModel::setHaptics,
            )
            SwitchRow(
                title = stringResource(R.string.settings_session_sound),
                subtitle = stringResource(R.string.settings_session_sound_desc),
                checked = settings.sessionChimeEnabled,
                onChange = viewModel::setSessionChime,
            )

            // ---- Sound -----------------------------------------------------------------
            SectionHeader(title = stringResource(R.string.settings_section_sound))
            SwitchRow(
                title = stringResource(R.string.sounds_continue_in_background),
                subtitle = stringResource(R.string.sounds_continue_in_background_desc),
                checked = settings.soundBackgroundPlaybackEnabled,
                onChange = viewModel::setBackgroundPlaybackEnabled,
            )

            // ---- Notifications ----------------------------------------------------------
            SectionHeader(title = stringResource(R.string.settings_section_notifications))
            SwitchRow(
                title = stringResource(R.string.settings_notifications),
                subtitle = stringResource(R.string.settings_notifications_desc),
                checked = settings.notificationsEnabled,
                onChange = viewModel::setNotificationsEnabled,
            )
            CalmaCard(
                title = stringResource(R.string.settings_permissions),
                onClick = onOpenPermissions,
            )

            // ---- Privacy ---------------------------------------------------------------
            SectionHeader(title = stringResource(R.string.settings_section_privacy))
            CalmaCard(
                title = stringResource(R.string.settings_clear_history),
                subtitle = stringResource(R.string.settings_clear_history_desc),
                onClick = { confirmClear = true },
            )
            CalmaCard(
                title = stringResource(R.string.settings_erase_all),
                subtitle = stringResource(R.string.settings_erase_all_desc),
                onClick = { confirmErase = true },
                icon = Icons.Outlined.DeleteForever,
            )
            CalmaCard(
                title = stringResource(R.string.settings_privacy),
                onClick = onOpenPrivacy,
            )

            // ---- About -----------------------------------------------------------------
            SectionHeader(title = stringResource(R.string.settings_section_about))
            CalmaCard(
                title = stringResource(R.string.settings_help),
                onClick = onOpenHelp,
            )
            CalmaCard(
                title = stringResource(R.string.settings_about),
                onClick = onOpenAbout,
            )
            CalmaCard(
                title = stringResource(R.string.settings_source_code),
                onClick = { openUrl(context, SOURCE_URL) },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${stringResource(R.string.settings_version)} ${uiState.versionName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.settings_confirm_clear_title)) },
            text = { Text(stringResource(R.string.settings_confirm_clear_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    confirmClear = false
                }) { Text(stringResource(R.string.action_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.action_no))
                }
            },
        )
    }

    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text(stringResource(R.string.settings_confirm_erase_title)) },
            text = { Text(stringResource(R.string.settings_confirm_erase_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eraseAll()
                    confirmErase = false
                }) { Text(stringResource(R.string.settings_erase_all)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmErase = false }) {
                    Text(stringResource(R.string.action_no))
                }
            },
        )
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url),
            ),
        )
    }
}

const val SOURCE_URL = "https://github.com/calma-app/calma"

@Composable
private fun <T> ChoiceRow(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CalmaSpacing.xs)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.xs)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(label(option)) },
                )
            }
        }
    }
}

@Composable
private fun ChipGroup(
    options: List<Int>,
    selected: Int,
    label: @Composable (Int) -> String,
    onSelect: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(CalmaSpacing.xs)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = CalmaSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}