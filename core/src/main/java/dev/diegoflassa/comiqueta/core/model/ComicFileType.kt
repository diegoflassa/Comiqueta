package dev.diegoflassa.comiqueta.core.model

/**
 * Represents supported comic file types and, optionally, image types.
 *
 * @param extension The file extension (e.g., "cbz", "pdf").
 * @param mimeType The primary MIME type associated with the file type (e.g., "application/vnd.comicbook+zip").
 *                 Can be null if not strictly defined or if multiple apply.
 * @param alternativeMimeTypes A list of other common MIME types.
 * @param isImageType True if this file type represents a standalone image format.
 */
enum class ComicFileType(
    val extension: String,
    val mimeType: String?,
    val alternativeMimeTypes: List<String> = emptyList(),
    val isImageType: Boolean = false
) {
    // Comic specific formats
    CBZ("cbz", "application/vnd.comicbook+zip", listOf("application/x-cbz", "application/zip")),
    CBR("cbr", "application/vnd.comicbook-rar", listOf("application/x-cbr", "application/x-rar-compressed")),
    CB7("cb7", "application/x-7z-compressed", listOf("application/x-cb7")), // Added CB7
    CBT("cbt", "application/x-tar", listOf("application/x-cbt", "application/vnd.comicbook-tar")), // Order for tar can be tricky with .tar.gz etc.
    PDF("pdf", "application/pdf"),

    // Image formats - typically these are not comic archives but can be viewed.
    JPG("jpg", "image/jpeg", isImageType = true),
    JPEG("jpeg", "image/jpeg", isImageType = true),
    PNG("png", "image/png", isImageType = true),
    GIF("gif", "image/gif", isImageType = true),
    WEBP("webp", "image/webp", isImageType = true);

    companion object {
        fun fromExtension(extension: String?): ComicFileType? {
            if (extension == null) return null
            return entries.find { it.extension.equals(extension, ignoreCase = true) }
        }

        fun fromMimeType(mimeType: String?): ComicFileType? {
            if (mimeType == null) return null
            return entries.find {
                it.mimeType.equals(mimeType, ignoreCase = true) ||
                        it.alternativeMimeTypes.any { alt -> alt.equals(mimeType, ignoreCase = true) }
            }
        }

        fun fromMimeTypeOrExtension(mimeType: String?, extension: String?): ComicFileType? {
            if (mimeType != null) {
                fromMimeType(mimeType)?.let { return it }
            }
            if (extension != null) {
                fromExtension(extension)?.let { return it }
            }
            // Fallback for generic archive mime types if extension matches a comic type
            if (mimeType != null && extension != null) {
                val extType = fromExtension(extension)
                if (extType != null && !extType.isImageType) { // Only for comic archive types
                    when (mimeType.lowercase()) {
                        "application/zip", "application/x-zip-compressed" -> if (extType == CBZ) return CBZ
                        "application/x-rar-compressed", "application/rar" -> if (extType == CBR) return CBR
                        "application/x-7z-compressed" -> if (extType == CB7) return CB7
                        "application/x-tar", "application/x-gtar" -> if (extType == CBT) return CBT
                        // Potentially add GzipCompressorInputStream, BZip2CompressorInputStream if needed for .cbt directly from mime
                    }
                }
            }
            return null
        }

        /**
         * Returns a list of [ComicFileType] values.
         * @param includeImages If true, image types are included. Otherwise, only comic archive types are returned.
         */
        fun getValues(includeImages: Boolean = true): List<ComicFileType> {
            return if (includeImages) {
                entries.toList()
            } else {
                entries.filter { !it.isImageType }
            }
        }

        /**
         * Returns a list of comic file type extensions as strings.
         * @param includeImages If true, image type extensions are included. Otherwise, only comic archive type extensions are returned.
         */
        fun getValuesAsString(includeImages: Boolean = true): List<String> {
            return if (includeImages) {
                entries.map { it.extension }
            } else {
                entries.filter { !it.isImageType }.map { it.extension }
            }
        }
    }
}
