package dev.diegoflassa.comiqueta.core.domain.usecase.comic

import dev.diegoflassa.comiqueta.core.data.repository.IComicsRepository
import dev.diegoflassa.comiqueta.core.domain.model.CollectionStats
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCollectionStatsUseCase @Inject constructor(
    private val comicsRepository: IComicsRepository
) : IGetCollectionStatsUseCase {
    override fun invoke(): Flow<CollectionStats> {
        return comicsRepository.getCollectionStats()
    }
}
