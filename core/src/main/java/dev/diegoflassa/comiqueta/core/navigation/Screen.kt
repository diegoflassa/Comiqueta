package dev.diegoflassa.comiqueta.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Screen : NavKey {
    @Serializable
    data object Home : Screen

    @Serializable
    data object Catalog : Screen

    @Serializable
    data object Bookmark : Screen

    @Serializable
    data object Favorites : Screen

    @Serializable
    data object Settings : Screen
}
