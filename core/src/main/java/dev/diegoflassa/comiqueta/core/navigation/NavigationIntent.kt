package dev.diegoflassa.comiqueta.core.navigation

sealed interface NavigationIntent {
    data class NavigateTo(val screen: Screen) : NavigationIntent
    data object GoToHome : NavigationIntent
    data object GoBack : NavigationIntent
    data class ReplaceAll(val screen: Screen) : NavigationIntent
}