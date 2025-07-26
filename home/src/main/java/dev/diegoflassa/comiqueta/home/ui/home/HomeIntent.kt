package dev.diegoflassa.comiqueta.home.ui.home

import android.net.Uri
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags
import dev.diegoflassa.comiqueta.core.data.model.Comic
import dev.diegoflassa.comiqueta.core.navigation.Screen
import dev.diegoflassa.comiqueta.home.ui.enums.ViewMode

sealed interface HomeIntent {
    // Intent to load comics with current filters (e.g., on initial load or refresh)
    data object LoadComics : HomeIntent

    // Intent for navigation 
    data class NavigateTo(val screen: Screen) : HomeIntent

    // Intent for search a particular comic
    data class SearchComics(val query: String) : HomeIntent

    // Intent to load comics, explicitly setting or replacing all flags
    data class SetComicFilters(val flags: Set<ComicFlags>) : HomeIntent

    // --- More granular intents for toggling individual flags ---

    // Toggle a specific flag ON or OFF
    data class ToggleFlag(val flag: ComicFlags) : HomeIntent

    // Specifically add a flag to the current set of filters
    data class AddFlag(val clearAndSet: Boolean = true, val flag: ComicFlags) : HomeIntent

    // Specifically remove a flag from the current set of filters
    data class RemoveFlag(val flag: ComicFlags) : HomeIntent

    // --- Intents for common predefined filter sets ---

    // Effectively SetComicFilters(emptySet())
    data object ShowAllComics : HomeIntent
    // SetComicFilters(setOf(ComicFlags.FAVORITE))
    data object ShowFavoriteComics : HomeIntent
    // SetComicFilters(setOf(ComicFlags.NEW))
    data object ShowNewComics : HomeIntent
    // SetComicFilters(setOf(ComicFlags.READ))
    data object ShowReadComics : HomeIntent

    // --- Intents for view mode handling ---
    data class ViewModeChanged(val viewMode: ViewMode) : HomeIntent

    // --- Intents for selection handling ---
    data class CategorySelected(val category: CategoryEntity?) : HomeIntent
    data class ComicSelected(val comic: Comic?) : HomeIntent
    data class FlagSelected(val flag: ComicFlags) : HomeIntent

    // --- Intents for folder handling ---
    data object AddFolderClicked : HomeIntent
    data class FolderSelected(val uri: Uri) : HomeIntent

    // --- Intents for permissions handling ---
    data class FolderPermissionResult(val isGranted: Boolean) : HomeIntent
    data object CheckInitialFolderPermission : HomeIntent
}
