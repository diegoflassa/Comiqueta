package dev.diegoflassa.comiqueta.categories.ui

import dev.diegoflassa.comiqueta.core.domain.model.Category

data class CategoriesUIState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDialog: Boolean = false,
    val categoryToEdit: Category? = null,
    val newCategoryName: String = ""
)
