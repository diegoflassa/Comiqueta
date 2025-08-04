package dev.diegoflassa.comiqueta.domain.usecase

import androidx.paging.PagingData
// ComicFlags import was here, but GetPaginatedComicsParams now handles its own import for it.
import dev.diegoflassa.comiqueta.core.data.model.Comic
import dev.diegoflassa.comiqueta.core.data.repository.IComicsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get paginated comics from the repository.
 */
class GetPaginatedComicsUseCase @Inject constructor(
    private val comicsRepository: IComicsRepository
) : IGetPaginatedComicsUseCase {
    override operator fun invoke(params: GetPaginatedComicsParams): Flow<PagingData<Comic>> { // Changed here
        return comicsRepository.getComicsPaginated(
            categoryId = params.categoryId,
            flags = params.flags
        )
    }
}
