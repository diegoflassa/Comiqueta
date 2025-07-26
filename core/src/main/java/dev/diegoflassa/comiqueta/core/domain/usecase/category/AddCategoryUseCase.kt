package dev.diegoflassa.comiqueta.core.domain.usecase.category

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.domain.repository.ICategoryRepository // Verified correct import
import javax.inject.Inject

class AddCategoryUseCase @Inject constructor(
    private val ICategoryRepository: ICategoryRepository
) {
    suspend operator fun invoke(categoryName: String): Long { // Changed to return Long
        // Basic validation, more complex validation can be added
        if (categoryName.isBlank()) {
            throw IllegalArgumentException("Category name cannot be blank.")
        }
        val newCategory = CategoryEntity(name = categoryName)
        return ICategoryRepository.insertCategory(newCategory)
    }
}
