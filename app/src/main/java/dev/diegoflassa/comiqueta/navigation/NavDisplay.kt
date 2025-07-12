package dev.diegoflassa.comiqueta.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import dev.diegoflassa.comiqueta.home.ui.home.HomeScreen
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.navigation.Screen
import dev.diegoflassa.comiqueta.settings.ui.settings.SettingsScreen
import dev.diegoflassa.comiqueta.categories.ui.categories.CategoriesScreen
import dev.diegoflassa.comiqueta.viewer.ui.viewer.ViewerScreen

@Composable
fun NavDisplay(modifier: Modifier, navigationViewModel: NavigationViewModel) {
    val backstack = navigationViewModel.state.collectAsStateWithLifecycle().value.backStack
    NavDisplay(
        backStack = backstack,
        modifier = modifier,
        transitionSpec = {
            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
        },
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Screen.Home> {
                HomeScreen(navigationViewModel = navigationViewModel)
            }
            entry<Screen.Settings> {
                SettingsScreen(navigationViewModel = navigationViewModel)
            }
            entry<Screen.Categories> {
                CategoriesScreen(navigationViewModel = navigationViewModel)
            }
            entry<Screen.Viewer> {
                ViewerScreen(navigationViewModel = navigationViewModel)
            }
        }
    )
}
