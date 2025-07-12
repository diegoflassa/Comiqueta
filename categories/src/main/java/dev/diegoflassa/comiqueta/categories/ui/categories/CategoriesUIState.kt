package dev.diegoflassa.comiqueta.categories.ui.categories

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity

data class CategoriesUIState(
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDialog: Boolean = false,
    val categoryToEdit: CategoryEntity? = null,
    val newCategoryName: String = ""
)
