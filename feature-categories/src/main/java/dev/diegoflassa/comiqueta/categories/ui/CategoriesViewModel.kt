package dev.diegoflassa.comiqueta.categories.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.diegoflassa.comiqueta.categories.R
import dev.diegoflassa.comiqueta.core.domain.model.Category
import dev.diegoflassa.comiqueta.core.domain.usecase.category.IAddCategoryUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.category.IDeleteCategoryUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.category.IGetCategoriesUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.category.IUpdateCategoryUseCase
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
    private val getCategoriesUseCase: IGetCategoriesUseCase,
    private val addCategoryUseCase: IAddCategoryUseCase,
    private val updateCategoryUseCase: IUpdateCategoryUseCase,
    private val deleteCategoryUseCase: IDeleteCategoryUseCase,
    @ApplicationContext private val context: Context
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
                    FirebaseCrashlytics.getInstance().recordException(e)
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                    _effect.send(CategoriesEffect.ShowToast(context.getString(R.string.error_loading_categories, e.message)))
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
            _effect.send(CategoriesEffect.ShowToast(context.getString(R.string.category_name_empty)))
            return
        }

        _uiState.update { it.copy(showDialog = false, isLoading = true) }
        try {
            val categoryToEdit = uiState.value.categoryToEdit
            if (categoryToEdit == null) {
                addCategoryUseCase(currentName)
                _effect.send(CategoriesEffect.ShowToast(context.getString(R.string.category_added, currentName)))
            } else {
                updateCategoryUseCase(categoryToEdit.copy(name = currentName))
                _effect.send(CategoriesEffect.ShowToast(context.getString(R.string.category_updated, currentName)))
            }
            _effect.send(CategoriesEffect.NavigateBack)
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            _effect.send(CategoriesEffect.ShowToast(context.getString(R.string.error_saving_category, ex.message)))
        } finally {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    categoryToEdit = null,
                    newCategoryName = ""
                )
            }
        }
    }

    private suspend fun deleteCategory(category: Category) {
        _uiState.update { it.copy(isLoading = true) }
        try {
            deleteCategoryUseCase(category)
            _effect.send(CategoriesEffect.ShowToast(context.getString(R.string.category_deleted_named, category.name)))
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            _effect.send(CategoriesEffect.ShowToast(context.getString(R.string.error_deleting_category, ex.message)))
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun deleteCategoryById(categoryId: Long) {
        _uiState.update { it.copy(isLoading = true) }
        try {
            deleteCategoryUseCase.byId(categoryId)
            _effect.send(CategoriesEffect.ShowToast(context.getString(R.string.category_deleted_generic)))
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            _effect.send(CategoriesEffect.ShowToast(context.getString(R.string.error_deleting_category, ex.message)))
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
