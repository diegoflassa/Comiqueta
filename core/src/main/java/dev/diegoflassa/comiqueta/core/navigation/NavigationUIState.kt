package dev.diegoflassa.comiqueta.core.navigation

data class NavigationUIState(
    val backStack: List<Screen> = listOf(Screen.Home)
)