package dev.diegoflassa.comiqueta.core.domain.usecase.category

import dev.diegoflassa.comiqueta.core.domain.model.Category
import dev.diegoflassa.comiqueta.core.domain.repository.ICategoryRepository
import javax.inject.Inject

class UpdateCategoryUseCase @Inject constructor(
    private val categoryRepository: ICategoryRepository
) : IUpdateCategoryUseCase {
    override suspend operator fun invoke(category: Category) {
        // Basic validation, more complex validation can be added
        if (category.name.isBlank()) {
            throw IllegalArgumentException("Category name cannot be blank.")
        }
        categoryRepository.updateCategory(category)
    }
}
