package dev.diegoflassa.comiqueta.core.domain.usecase.category

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.domain.repository.ICategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: ICategoryRepository
) : IDeleteCategoryUseCase {
    override suspend operator fun invoke(category: CategoryEntity) {
        categoryRepository.deleteCategory(category)
    }

    override suspend fun byId(categoryId: Long) {
        categoryRepository.deleteCategoryById(categoryId)
    }
}
