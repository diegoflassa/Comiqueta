package dev.diegoflassa.comiqueta.core.data.enums

enum class ComicsExtensions(val extension: String) {
    CBZ(".cbz"),
    CBR(".cbr"),
    CBT(".cbt"),
    CB7(".cb7"),
    PDF(".pdf");

    companion object {
        private val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".webp")

        fun list(includeImages: Boolean = false): List<String> {
            return if (includeImages) {
                entries.map { it.extension } + imageExtensions
            } else {
                entries.map { it.extension }
            }
        }
    }
}