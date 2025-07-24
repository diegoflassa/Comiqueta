package dev.diegoflassa.comiqueta.categories.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.domain.usecase.category.AddCategoryUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.category.DeleteCategoryUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.category.GetCategoriesUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.category.UpdateCategoryUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUIState())
    val uiState: StateFlow<CategoriesUIState> = _uiState.asStateFlow()

    private val _effect = Channel<CategoriesEffect>(Channel.BUFFERED)
    val effect: Flow<CategoriesEffect> = _effect.receiveAsFlow()

    init {
        processIntent(CategoriesIntent.LoadCategories)
    }

    fun processIntent(intent: CategoriesIntent) {
        viewModelScope.launch {
            when (intent) {
                is CategoriesIntent.LoadCategories -> loadCategories()
                is CategoriesIntent.NavigateBack -> {
                    _effect.send(CategoriesEffect.NavigateBack)
                }

                is CategoriesIntent.CategoryAdd -> {
                    _uiState.update {
                        it.copy(
                            showDialog = true,
                            categoryToEdit = null,
                            newCategoryName = ""
                        )
                    }
                }

                is CategoriesIntent.CategoryDelete -> deleteCategory(intent.category)

                is CategoriesIntent.CategoryEdit -> {
                    _uiState.update {
                        it.copy(
                            showDialog = true,
                            categoryToEdit = intent.category,
                            newCategoryName = intent.category.name
                        )
                    }
                }

                is CategoriesIntent.DismissDialog -> {
                    _uiState.update {
                        it.copy(
                            showDialog = false,
                            categoryToEdit = null,
                            newCategoryName = ""
                        )
                    }
                    _effect.send(CategoriesEffect.NavigateBack)
                }

                is CategoriesIntent.SetNewCategoryName -> {
                    _uiState.update { it.copy(newCategoryName = intent.name) }
                }

                is CategoriesIntent.SaveCategory -> saveCategory()
                is CategoriesIntent.DeleteCategoryById -> deleteCategoryById(intent.categoryId)
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getCategoriesUseCase()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                    _effect.send(CategoriesEffect.ShowToast("Error loading categories: ${e.message}"))
                }
                .collect { categories ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            categories = categories,
                            error = null
                        )
                    }
                }
        }
    }

    private suspend fun saveCategory() {
        val currentName = uiState.value.newCategoryName.trim()
        if (currentName.isEmpty()) {
            _effect.send(CategoriesEffect.ShowToast("Category name cannot be empty."))
            return
        }

        _uiState.update { it.copy(showDialog = false, isLoading = true) }
        try {
            val categoryToEdit = uiState.value.categoryToEdit
            if (categoryToEdit == null) { // Add new category
                addCategoryUseCase(currentName)
                _effect.send(CategoriesEffect.ShowToast("Category '$currentName' added."))
            } else { // Update existing category
                updateCategoryUseCase(categoryToEdit.copy(name = currentName))
                _effect.send(CategoriesEffect.ShowToast("Category '$currentName' updated."))
            }
            _effect.send(CategoriesEffect.NavigateBack)
        } catch (e: Exception) {
            _effect.send(CategoriesEffect.ShowToast("Error saving category: ${e.message}"))
        }
        // No finally block to set isLoading to false, as loadCategories will be called by SaveCategory success/failure observation in UI if needed
        // or rely on the loadCategories flow to update the list and loading state.
        // For simplicity here, we assume the list will refresh or the operation is atomic enough.
        // Consider explicit refresh or better state management if operations are slow.
        _uiState.update {
            it.copy(
                isLoading = false,
                categoryToEdit = null,
                newCategoryName = ""
            )
        } // Reset dialog state
    }

    private suspend fun deleteCategory(category: CategoryEntity) {
        _uiState.update { it.copy(isLoading = true) }
        try {
            deleteCategoryUseCase(category)
            _effect.send(CategoriesEffect.ShowToast("Category '${category.name}' deleted."))
        } catch (e: Exception) {
            _effect.send(CategoriesEffect.ShowToast("Error deleting category: ${e.message}"))
        }
        _uiState.update { it.copy(isLoading = false) }
    }

    private suspend fun deleteCategoryById(categoryId: Long) {
        _uiState.update { it.copy(isLoading = true) }
        try {
            deleteCategoryUseCase.byId(categoryId)
            _effect.send(CategoriesEffect.ShowToast("Category deleted."))
        } catch (e: Exception) {
            _effect.send(CategoriesEffect.ShowToast("Error deleting category: ${e.message}"))
        }
        _uiState.update { it.copy(isLoading = false) }
    }
}
