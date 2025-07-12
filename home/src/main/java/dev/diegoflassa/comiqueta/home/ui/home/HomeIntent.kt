package dev.diegoflassa.comiqueta.home.ui.home

import android.net.Uri
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.home.ui.enums.ViewMode

sealed interface HomeIntent {
    data object LoadInitialData : HomeIntent
    data class SearchComics(val query: String) : HomeIntent
    data class CategorySelected(val category: CategoryEntity) : HomeIntent
    data class ViewModeChanged(val viewMode: ViewMode) : HomeIntent
    data object AddFolderClicked : HomeIntent
    data class FolderSelected(val uri: Uri) : HomeIntent
    data class ComicSelected(val comic: ComicEntity) : HomeIntent
    data class FolderPermissionResult(val isGranted: Boolean) : HomeIntent
    data object CheckInitialFolderPermission : HomeIntent
}