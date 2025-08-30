package dev.diegoflassa.comiqueta.domain.usecase

import androidx.paging.PagingData
import dev.diegoflassa.comiqueta.core.domain.model.Comic
import dev.diegoflassa.comiqueta.core.data.repository.IComicsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get paginated comics from the repository.
 */
class GetPaginatedComicsUseCase @Inject constructor(
    private val comicsRepository: IComicsRepository
) : IGetPaginatedComicsUseCase {
    override operator fun invoke(params: PaginatedComicsParams): Flow<PagingData<Comic>> {
        return comicsRepository.getComicsPaginated(
            categoryId = params.categoryId,
            flags = params.flags,
            searchQuery = params.searchQuery
        )
    }
}
