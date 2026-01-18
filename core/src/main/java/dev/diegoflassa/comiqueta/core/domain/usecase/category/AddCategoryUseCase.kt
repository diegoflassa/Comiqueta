package dev.diegoflassa.comiqueta.core.domain.usecase.category

import dev.diegoflassa.comiqueta.core.domain.model.Category
import dev.diegoflassa.comiqueta.core.domain.repository.ICategoryRepository // Verified correct import
import javax.inject.Inject

class AddCategoryUseCase @Inject constructor(
    private val categoryRepository: ICategoryRepository
) : IAddCategoryUseCase {
    override suspend operator fun invoke(categoryName: String): Long {
        if (categoryName.isBlank()) {
            throw IllegalArgumentException("Category name cannot be blank.")
        }
        val newCategory = Category(id = 0, name = categoryName, createdAt = System.currentTimeMillis())
        return categoryRepository.insertCategory(newCategory)
    }
}
