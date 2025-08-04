package dev.diegoflassa.comiqueta.categories.ui

sealed interface CategoriesEffect {
    data class ShowToast(val message: String) : CategoriesEffect
    data object NavigateBack : CategoriesEffect
}