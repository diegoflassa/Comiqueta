package dev.diegoflassa.comiqueta.home.ui.home

import android.net.Uri

sealed interface HomeEffect {
    data class ShowToast(val message: String) : HomeEffect
    data object LaunchFolderPicker : HomeEffect
    data class RequestStoragePermission(val permission: String) : HomeEffect
    data class NavigateToComicDetail(val comicPath: Uri) : HomeEffect
}