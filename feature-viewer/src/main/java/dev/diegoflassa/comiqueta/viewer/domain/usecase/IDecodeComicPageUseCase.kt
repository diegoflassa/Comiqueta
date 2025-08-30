package dev.diegoflassa.comiqueta.viewer.domain.usecase

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import dev.diegoflassa.comiqueta.core.model.ComicFileType


/**
 * Interface for the use case to decode a specific page of a comic file into an ImageBitmap.
 * Defines the public contract of the use case.
 */
interface IDecodeComicPageUseCase {
    /**
     * Decodes a specific page from a comic file into an ImageBitmap.
     *
     * @param pageIndex The 0-based index of the page to decode (used for PDFs).
     * @param pageIdentifier A unique identifier for the page (e.g., filename within archives).
     * @param comicUri The URI of the comic file.
     * @param fileType The determined [ComicFileType] of the comic.
     * @return An [ImageBitmap] representing the decoded page, or null if decoding fails.
     * @throws java.io.IOException if there's an error accessing the file or decoding.
     */
    suspend operator fun invoke(
        pageIndex: Int,
        pageIdentifier: String,
        comicUri: Uri,
        fileType: ComicFileType
    ): ImageBitmap?
}