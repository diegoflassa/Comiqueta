package dev.diegoflassa.comiqueta.domain.usecase

import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags

data class PaginatedComicsParams(
    val categoryId: Long? = null,
    val flags: Set<ComicFlags> = emptySet()
)
