package dev.diegoflassa.comiqueta.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.diegoflassa.comiqueta.categories.ui.CategoriesScreen
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.navigation.Screen
import dev.diegoflassa.comiqueta.ui.SettingsScreen
import dev.diegoflassa.comiqueta.ui.home.HomeScreen
import dev.diegoflassa.comiqueta.ui.stats.StatisticsScreen
import dev.diegoflassa.comiqueta.viewer.ui.ViewerScreen

private const val TWEEN_DURATION = 300

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NavDisplay(modifier: Modifier, navigationViewModel: NavigationViewModel) {
    val backstack = navigationViewModel.state.collectAsStateWithLifecycle().value.backStack
    val adaptiveScaffoldNavigator = rememberListDetailPaneScaffoldNavigator<String>()

    NavDisplay(
        backStack = backstack,
        modifier = modifier,
        transitionSpec = {
            fadeIn(tween(TWEEN_DURATION)) togetherWith fadeOut(tween(TWEEN_DURATION))
        },
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
            entry<Screen.Viewer> { viewerScreenInstance ->
                val comicPath = viewerScreenInstance.comicPath
                ViewerScreen(
                    navigationViewModel = navigationViewModel,
                    comicPath = comicPath
                )
            }
            entry<Screen.Statistics> {
                StatisticsScreen(navigationViewModel = navigationViewModel)
            }
        }
    )
}
