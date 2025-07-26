package dev.diegoflassa.comiqueta.core.domain.usecase.category

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.domain.repository.ICategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val ICategoryRepository: ICategoryRepository
) {
    suspend operator fun invoke(category: CategoryEntity) {
        ICategoryRepository.deleteCategory(category)
    }

    suspend fun byId(categoryId: Long) {
        ICategoryRepository.deleteCategoryById(categoryId)
    }
}
