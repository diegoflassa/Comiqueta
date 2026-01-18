package dev.diegoflassa.comiqueta.core.domain.usecase.category

import dev.diegoflassa.comiqueta.core.domain.model.Category

interface IDeleteCategoryUseCase {
    /**
     * Deletes the specified category entity.
     * @param category The category entity to delete.
     */
    suspend operator fun invoke(category: Category)

    /**
     * Deletes a category by its unique identifier.
     * @param categoryId The ID of the category to delete.
     */
    suspend fun byId(categoryId: Long)
}