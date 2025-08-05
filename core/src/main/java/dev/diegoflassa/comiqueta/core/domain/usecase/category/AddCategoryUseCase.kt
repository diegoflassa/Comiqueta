package dev.diegoflassa.comiqueta.core.domain.usecase.category

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.domain.repository.ICategoryRepository // Verified correct import
import javax.inject.Inject

class AddCategoryUseCase @Inject constructor(
    private val categoryRepository: ICategoryRepository
) : IAddCategoryUseCase {
    override suspend operator fun invoke(categoryName: String): Long {
        if (categoryName.isBlank()) {
            throw IllegalArgumentException("Category name cannot be blank.")
        }
        val newCategory = CategoryEntity(name = categoryName)
        return categoryRepository.insertCategory(newCategory)
    }
}
