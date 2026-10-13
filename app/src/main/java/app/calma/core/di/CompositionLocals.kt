package app.calma.core.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import app.calma.AppContainer
import app.calma.calmaContainer

/**
 * The application container, provided once by [MainActivity] and read by screens that need
 * to build a ViewModel with manual wiring.
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer was not provided. Wrap the content in CompositionLocalProvider.")
}

/** Container for the current composition; always non-null inside Calma. */
@Composable
fun appContainer(): AppContainer = LocalAppContainer.current

/** Android context for the current composition. */
@Composable
fun localContext() = LocalContext.current

/** Container resolved directly from the application context, for non-composable call sites. */
fun containerOf(context: android.content.Context): AppContainer = context.calmaContainer