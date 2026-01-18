package dev.diegoflassa.comiqueta.core.domain.usecase.comic

import dev.diegoflassa.comiqueta.core.domain.model.CollectionStats
import kotlinx.coroutines.flow.Flow

interface IGetCollectionStatsUseCase {
    operator fun invoke(): Flow<CollectionStats>
}
