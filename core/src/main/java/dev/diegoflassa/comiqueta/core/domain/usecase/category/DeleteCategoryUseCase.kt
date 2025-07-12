package dev.diegoflassa.comiqueta.core.domain.usecase.category

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.domain.repository.CategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(category: CategoryEntity) {
        categoryRepository.deleteCategory(category)
    }

    suspend fun byId(categoryId: Long) {
        categoryRepository.deleteCategoryById(categoryId)
    }
}
