package dev.diegoflassa.comiqueta.viewer.ui

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import dev.diegoflassa.comiqueta.core.model.ComicFileType

/**
 * Represents the state of the Comic Viewer screen.
 */
data class ViewerUIState(
    val comicTitle: String = "",
    val comicPath: Uri = Uri.EMPTY,
    val loadedPages: Map<Int, ImageBitmap?> = emptyMap(),
    val currentPage: Int = 0,
    val pageCount: Int = 0,
    val fileType: ComicFileType? = null,
    val isUiVisible: Boolean = false,
    val error: String? = null,
    val pagesToPreloadLogic: Int = DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD,
    val isLoadingPage: Set<Int> = emptySet(),
    val isMangaMode: Boolean = false,
    val isWebtoonMode: Boolean = false,
    val isDoublePageMode: Boolean = false,
    val isPageFlipSoundEnabled: Boolean = false,
    val loadedThumbnails: Map<Int, ImageBitmap?> = emptyMap(),
    val isLoadingThumbnail: Set<Int> = emptySet()
){
    companion object {
        const val DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD = 1
    }
}
