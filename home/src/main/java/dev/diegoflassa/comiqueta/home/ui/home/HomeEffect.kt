package dev.diegoflassa.comiqueta.home.ui.home

import android.net.Uri
import dev.diegoflassa.comiqueta.core.navigation.Screen

sealed interface HomeEffect {
    data class ShowToast(val message: String) : HomeEffect
    data object LaunchFolderPicker : HomeEffect
    data class NavigateTo(val screen: Screen) : HomeEffect
    data class RequestStoragePermission(val permission: String) : HomeEffect
    data class NavigateToComicDetail(val comicPath: Uri?) : HomeEffect
}