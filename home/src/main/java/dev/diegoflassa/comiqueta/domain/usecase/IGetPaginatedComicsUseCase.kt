package dev.diegoflassa.comiqueta.domain.usecase

import androidx.paging.PagingData
import dev.diegoflassa.comiqueta.core.data.model.Comic
import kotlinx.coroutines.flow.Flow

interface IGetPaginatedComicsUseCase {
    operator fun invoke(params: PaginatedComicsParams): Flow<PagingData<Comic>> // Changed here
}
