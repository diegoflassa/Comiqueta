package dev.diegoflassa.comiqueta.viewer.ui

/**
 * Represents user actions or events for the Comic Viewer screen,
 * to be processed by the ViewerViewModel.
 */
sealed interface ViewerIntent {
    /**
     * Intent to load a comic from the given URI string.
     * The ViewModel will handle conversion to Uri.
     * @param uriString The string representation of the comic file's URI.
     */
    data class LoadComic(val uriString: String) : ViewerIntent

    /**
     * Intent to navigate to a specific page (0-indexed).
     * Corresponds to ViewerViewModel's GoToPage event.
     * @param pageNumber The 0-based index of the page to navigate to.
     */
    data class GoToPage(val pageNumber: Int) : ViewerIntent

    /**
     * Intent to navigate to the next page.
     */
    data object NavigateNextPage : ViewerIntent

    /**
     * Intent to navigate to the previous page.
     */
    data object NavigatePreviousPage : ViewerIntent

    /**
     * Intent to toggle the visibility of the viewer's UI controls.
     * Corresponds to ViewerViewModel's ToggleUiVisibility event.
     */
    data object ToggleUiVisibility : ViewerIntent

    /**
     * Intent to signify that an error message has been shown/handled by the UI,
     * allowing the ViewModel to clear its error state.
     * Corresponds to ViewerViewModel's ErrorShown event.
     */
    data object ErrorShown : ViewerIntent
}
