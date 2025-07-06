package dev.diegoflassa.comiqueta.home.ui.home

import android.net.Uri

sealed interface HomeIntent {
    data object LoadInitialData : HomeIntent
    data class SearchComics(val query: String) : HomeIntent
    data class SelectCategory(val category: String) : HomeIntent
    data object AddFolderClicked : HomeIntent
    data class FolderSelected(val uri: Uri) : HomeIntent
    data class FolderPermissionResult(val isGranted: Boolean) : HomeIntent
    data object CheckInitialFolderPermission : HomeIntent
}