package dev.diegoflassa.comiqueta.categories.ui

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity

sealed interface CategoriesIntent {
    data object LoadCategories : CategoriesIntent
    data object NavigateBack : CategoriesIntent
    data object CategoryAdd : CategoriesIntent
    data class CategoryEdit(val category: CategoryEntity) : CategoriesIntent
    data class CategoryDelete(val category: CategoryEntity) : CategoriesIntent
    data object DismissDialog : CategoriesIntent
    data class SetNewCategoryName(val name: String) : CategoriesIntent
    data object SaveCategory : CategoriesIntent
    data class DeleteCategoryById(val categoryId: Long) : CategoriesIntent
}
