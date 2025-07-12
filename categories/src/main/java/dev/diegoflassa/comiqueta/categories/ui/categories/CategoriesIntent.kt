package dev.diegoflassa.comiqueta.categories.ui.categories

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity

sealed interface CategoriesIntent {
    data object LoadCategories : CategoriesIntent
    data object ShowAddCategoryDialog : CategoriesIntent
    data class ShowEditCategoryDialog(val category: CategoryEntity) : CategoriesIntent
    data object DismissDialog : CategoriesIntent
    data class SetNewCategoryName(val name: String) : CategoriesIntent
    data object SaveCategory : CategoriesIntent
    data class DeleteCategory(val category: CategoryEntity) : CategoriesIntent
    data class DeleteCategoryById(val categoryId: Long) : CategoriesIntent
}
