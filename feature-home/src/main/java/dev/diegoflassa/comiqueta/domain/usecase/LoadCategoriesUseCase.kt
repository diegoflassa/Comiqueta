package dev.diegoflassa.comiqueta.domain.usecase

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.domain.repository.ICategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to load all categories from the repository.
 */
class LoadCategoriesUseCase @Inject constructor(
    private val categoryRepository: ICategoryRepository
) : ILoadCategoriesUseCase {
    override operator fun invoke(): Flow<List<CategoryEntity>> {
        return categoryRepository.getAllCategories()
    }
}
