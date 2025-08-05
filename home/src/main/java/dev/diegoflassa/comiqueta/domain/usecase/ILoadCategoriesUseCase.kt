package dev.diegoflassa.comiqueta.domain.usecase

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface ILoadCategoriesUseCase {
    operator fun invoke(): Flow<List<CategoryEntity>>
}
