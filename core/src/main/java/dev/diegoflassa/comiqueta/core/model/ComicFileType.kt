package dev.diegoflassa.comiqueta.core.model

/**
 * Represents supported comic file types and, optionally, image types.
 */
enum class ComicFileType(val extension: String, val isImageType: Boolean = false) {
    // Comic specific formats
    CBZ("cbz"),
    CBR("cbr"),
    CBT("cbt"),
    PDF("pdf"),

    // Image formats
    JPG("jpg", true),
    JPEG("jpeg", true),
    PNG("png", true),
    GIF("gif", true),
    WEBP("webp", true);

    companion object {
        fun fromExtension(extension: String?): ComicFileType? {
            if (extension == null) return null
            return entries.find { it.extension.equals(extension, ignoreCase = true) }
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
