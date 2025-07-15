package dev.diegoflassa.comiqueta.viewer.ui.viewer

import android.net.Uri

/**
 * Represents user actions or events for the Comic Viewer screen.
 */
sealed interface ViewerIntent {
    /**
     * Intent to load a comic from the given URI.
     * @param uri The URI of the comic file to load.
     */
    data class LoadComic(val uri: Uri) : ViewerIntent

    /**
     * Intent to navigate to a specific page index (0-indexed).
     * @param pageIndex The 0-based index of the page to navigate to.
     */
    data class NavigateToPage(val pageIndex: Int) : ViewerIntent

    /**
     * Intent to toggle the visibility of the viewer's UI controls (e.g., app bars).
     */
    data object ToggleViewerControls : ViewerIntent

    /**
     * Intent to clear any currently displayed error message.
     */
    data object ClearError : ViewerIntent
}