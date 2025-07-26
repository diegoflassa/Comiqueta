package dev.diegoflassa.comiqueta.core.domain.usecase.category

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.domain.repository.ICategoryRepository
import javax.inject.Inject

class UpdateCategoryUseCase @Inject constructor(
    private val ICategoryRepository: ICategoryRepository
) {
    suspend operator fun invoke(category: CategoryEntity) {
        // Basic validation, more complex validation can be added
        if (category.name.isBlank()) {
            throw IllegalArgumentException("Category name cannot be blank.")
        }
        ICategoryRepository.updateCategory(category)
    }
}
