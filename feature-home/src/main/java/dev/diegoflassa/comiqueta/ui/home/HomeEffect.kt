package dev.diegoflassa.comiqueta.ui.home

import android.net.Uri
import dev.diegoflassa.comiqueta.core.navigation.Screen

sealed interface HomeEffect {
    data class ShowToast(val message: String) : HomeEffect
    data object OpenFolderPicker : HomeEffect
    data class NavigateTo(val screen: Screen) : HomeEffect
    data class RequestStoragePermission(val permission: String) : HomeEffect
    data object RequestGeneralStoragePermission : HomeEffect
    data class NavigateToComicDetail(val comicPath: Uri?) : HomeEffect
}
