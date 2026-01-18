package dev.diegoflassa.comiqueta.domain.usecase

import dev.diegoflassa.comiqueta.core.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface ILoadCategoriesUseCase {
    operator fun invoke(): Flow<List<Category>>
}
