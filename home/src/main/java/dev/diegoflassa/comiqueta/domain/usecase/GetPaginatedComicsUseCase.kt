package dev.diegoflassa.comiqueta.domain.usecase

import androidx.paging.PagingData
import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags
import dev.diegoflassa.comiqueta.core.data.model.Comic
import dev.diegoflassa.comiqueta.core.data.repository.IComicsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get paginated comics from the repository.
 */
class GetPaginatedComicsUseCase @Inject constructor(
    private val comicsRepository: IComicsRepository
) {
    operator fun invoke(params: Params): Flow<PagingData<Comic>> {
        return comicsRepository.getComicsPaginated(
            categoryId = params.categoryId,
            flags = params.flags
        )
    }

    data class Params(
        val categoryId: Long? = null,
        val flags: Set<ComicFlags> = emptySet()
    )
}
