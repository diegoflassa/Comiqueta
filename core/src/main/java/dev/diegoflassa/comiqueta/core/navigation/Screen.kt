package dev.diegoflassa.comiqueta.core.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import dev.diegoflassa.comiqueta.core.data.serialization.UriSerializer
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

    @Serializable
    data object Categories : Screen

    @Serializable
    data class Viewer(
        @Serializable(with = UriSerializer::class)
        val comicPath: Uri
    ) : Screen
}
