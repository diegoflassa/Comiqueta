package dev.diegoflassa.comiqueta.viewer.ui.viewer

import androidx.compose.ui.graphics.ImageBitmap
import dev.diegoflassa.comiqueta.core.model.ComicFileType

data class ViewerUIState(
    val comicIdentifierUri: String? = null,
    val comicTitle: String = "",
    val comicPages: List<ImageBitmap> = emptyList(),
    val currentPageNumber: Int = 0,
    val totalPageCount: Int = 0,
    val comicFileType: ComicFileType? = null,
    val isLoadingComic: Boolean = false,
    val showViewerControls: Boolean = true,
    val viewerError: String? = null
)
