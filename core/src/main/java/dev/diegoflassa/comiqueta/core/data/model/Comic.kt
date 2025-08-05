package dev.diegoflassa.comiqueta.core.data.model

import android.net.Uri

data class Comic(
    val filePath: Uri = Uri.EMPTY,
    val title: String? = null,
    val coverPath: Uri = Uri.EMPTY,
    val author: String? = null,
    val categoryId: Long? = null,
    val isFavorite: Boolean = false,
    val isNew: Boolean = true,
    val hasBeenRead: Boolean = false,
    val lastPageRead: Int = 0,
    val lastModified: Long = 0,
    val created: Long = 0,
    val pageCount: Int = 0
)

