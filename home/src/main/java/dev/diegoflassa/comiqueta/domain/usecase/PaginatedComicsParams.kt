package dev.diegoflassa.comiqueta.domain.usecase

import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags

data class PaginatedComicsParams(
    val categoryId: Long? = 0,
    val flags: Set<ComicFlags> = emptySet()
)
