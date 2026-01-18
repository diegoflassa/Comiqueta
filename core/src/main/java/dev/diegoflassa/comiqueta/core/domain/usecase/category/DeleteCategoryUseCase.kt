package dev.diegoflassa.comiqueta.core.domain.usecase.category

import dev.diegoflassa.comiqueta.core.domain.model.Category
import dev.diegoflassa.comiqueta.core.domain.repository.ICategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: ICategoryRepository
) : IDeleteCategoryUseCase {
    override suspend operator fun invoke(category: Category) {
        categoryRepository.deleteCategory(category)
    }

    override suspend fun byId(categoryId: Long) {
        categoryRepository.deleteCategoryById(categoryId)
    }
}
