package app.calma.core.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.calma.R
import app.calma.audio.AmbientPlaybackService
import app.calma.core.ui.theme.CalmaSpacing
import app.calma.feature.about.AboutScreen
import app.calma.feature.about.PermissionsScreen
import app.calma.feature.about.PrivacyScreen
import app.calma.feature.about.UserGuideScreen
import app.calma.feature.breathing.BreathingScreen
import app.calma.feature.calm.CalmScreen
import app.calma.feature.focus.FocusScreen
import app.calma.feature.home.HomeScreen
import app.calma.feature.mood.MoodScreen
import app.calma.feature.settings.SettingsScreen
import app.calma.feature.sounds.SoundsScreen
import app.calma.feature.statistics.StatisticsScreen

/** Root scaffold: a bottom bar with the five primary destinations. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalmaApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val isTopLevel = TopLevelDestination.entries.any { it.route == currentRoute }
    val titleRes = TopLevelDestination.entries
        .firstOrNull { it.route == currentRoute }
        ?.labelRes

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedVisibility(
                visible = isTopLevel,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (titleRes != null) {
                                stringResource(titleRes)
                            } else {
                                stringResource(R.string.app_name)
                            },
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.cd_open_settings),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = isTopLevel) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            CalmaNavHost(navController = navController, context = context)
        }
    }
}

@Composable
private fun CalmaNavHost(
    navController: NavHostController,
    context: android.content.Context,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.HOME.route,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(TopLevelDestination.HOME.route) {
            HomeScreen(
                contentPadding = PaddingValues(CalmaSpacing.lg),
                onOpenCalm = { navController.navigate(Routes.CALM) },
                onOpenFocus = { navController.navigate(TopLevelDestination.FOCUS.route) },
                onOpenBreathe = { navController.navigate(TopLevelDestination.BREATHE.route) },
                onOpenSounds = { navController.navigate(TopLevelDestination.SOUNDS.route) },
                onOpenMood = { navController.navigate(Routes.MOOD) },
            )
        }

        composable(TopLevelDestination.FOCUS.route) {
            FocusScreen(onBack = { navController.popBackStack() })
        }

        composable(TopLevelDestination.BREATHE.route) {
            BreathingScreen()
        }

        composable(TopLevelDestination.SOUNDS.route) {
            SoundsScreen(
                onBackgroundPlayback = { AmbientPlaybackService.start(context) },
                onBackgroundPlaybackStop = { AmbientPlaybackService.stop(context) },
            )
        }

        composable(TopLevelDestination.STATS.route) {
            StatisticsScreen()
        }

        composable(Routes.MOOD) {
            MoodScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                onOpenHelp = { navController.navigate(Routes.HELP) },
            )
        }

        composable(Routes.PERMISSIONS) {
            PermissionsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PRIVACY) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.HELP) {
            UserGuideScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CALM) {
            CalmScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.CALM_PATTERN,
            arguments = listOf(navArgument(Routes.CALM_ARG) { type = NavType.StringType }),
        ) {
            CalmScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** Shared horizontal padding token used by list-style screens. */
val ScreenHorizontalPadding: PaddingValues
    @Composable get() = PaddingValues(horizontal = CalmaSpacing.lg, vertical = CalmaSpacing.md)

internal val ScreenTopSpacer = 8.dp