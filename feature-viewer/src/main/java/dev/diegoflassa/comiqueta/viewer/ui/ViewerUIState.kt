package dev.diegoflassa.comiqueta.viewer.ui

import androidx.compose.ui.graphics.ImageBitmap
import dev.diegoflassa.comiqueta.core.model.ComicFileType

/**
 * Represents the state of the Comic Viewer screen.
 */
data class ViewerUIState(
    val comicTitle: String = "",
    // loadedPages will hold bitmaps for the current page and any loaded/preloaded neighbors
    // as dictated by pagesToPreloadLogic.
    val loadedPages: Map<Int, ImageBitmap?> = emptyMap(),
    val currentPage: Int = 0,
    val pageCount: Int = 0,
    val fileType: ComicFileType? = null,
    val isUiVisible: Boolean = true,
    val error: String? = null,
    val pagesToPreloadLogic: Int = DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD,
    // isLoadingPage will indicate which pages (current or neighbors) are actively being decoded.
    val isLoadingPage: Set<Int> = emptySet()
){
    companion object {
        const val DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD = 1 // Default to current, prev, next
    }
}
