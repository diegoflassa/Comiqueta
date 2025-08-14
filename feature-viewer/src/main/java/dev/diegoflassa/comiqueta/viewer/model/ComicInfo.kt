package dev.diegoflassa.comiqueta.viewer.model

import dev.diegoflassa.comiqueta.core.model.ComicFileType

/**
 * Represents essential information about a comic book.
 *
 * @param title The title of the comic.
 * @param pageCount The total number of pages in the comic.
 * @param pageIdentifiers A list of strings that uniquely identify each page (e.g., filenames or page indices).
 * @param fileType The determined [ComicFileType] of the comic.
 */
data class ComicInfo(
    val title: String,
    val pageCount: Int,
    val pageIdentifiers: List<String>,
    val fileType: ComicFileType
)
