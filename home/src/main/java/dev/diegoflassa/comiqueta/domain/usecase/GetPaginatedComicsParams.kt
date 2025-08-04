package dev.diegoflassa.comiqueta.domain.usecase

import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags

data class GetPaginatedComicsParams(
    val categoryId: Long? = 0,
    val flags: Set<ComicFlags> = emptySet()
)
