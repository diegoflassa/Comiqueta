package dev.diegoflassa.comiqueta.viewer.ui

import androidx.compose.ui.graphics.ImageBitmap
import dev.diegoflassa.comiqueta.core.model.ComicFileType

/**
 * Represents the state of the Comic Viewer screen.
 */
data class ViewerUIState(
    val isLoading: Boolean = false,
    val comicTitle: String = "",
    val currentBitmap: ImageBitmap? = null,
    val currentPage: Int = 0,
    val pageCount: Int = 0,
    val fileType: ComicFileType? = null,
    val isUiVisible: Boolean = true,
    val error: String? = null
)
