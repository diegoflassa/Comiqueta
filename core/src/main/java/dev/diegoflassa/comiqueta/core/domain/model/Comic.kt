package dev.diegoflassa.comiqueta.core.domain.model

data class Comic(
    val filePath: String = "",
    val title: String? = null,
    val coverPath: String = "",
    val author: String? = null,
    val categoryId: Long? = null,
    val isFavorite: Boolean = false,
    val isNew: Boolean = true,
    val hasBeenRead: Boolean = false,
    val lastPageRead: Int = 0,
    val lastModified: Long = 0,
    val created: Long = 0,
    val pageCount: Int = 0,
    val chapter: Int = 0,
    val page: Int = 0
)

