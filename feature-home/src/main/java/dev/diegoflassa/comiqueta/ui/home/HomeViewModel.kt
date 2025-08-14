package dev.diegoflassa.comiqueta.ui.home

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.comiqueta.core.data.config.IConfig
import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags
import dev.diegoflassa.comiqueta.core.data.preferences.UserPreferencesKeys
import dev.diegoflassa.comiqueta.core.data.repository.IComicsFolderRepository
import dev.diegoflassa.comiqueta.core.domain.usecase.EnqueueSafFolderScanWorkerUseCase
import dev.diegoflassa.comiqueta.domain.usecase.IGetPaginatedComicsUseCase
import dev.diegoflassa.comiqueta.domain.usecase.PaginatedComicsParams
import dev.diegoflassa.comiqueta.ui.enums.BottomNavItems
import dev.diegoflassa.comiqueta.core.domain.model.Comic
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.data.worker.SafFolderScanWorker
import dev.diegoflassa.comiqueta.domain.usecase.ILoadCategoriesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val applicationContext: Application,
    val config: IConfig,
    private val getPaginatedComicsUseCase: IGetPaginatedComicsUseCase,
    private val loadCategoriesUseCase: ILoadCategoriesUseCase,
    private val comicsFolderRepository: IComicsFolderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUIState())
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val _comicsFlow = MutableStateFlow<PagingData<Comic>>(PagingData.empty())
    val comicsFlow = _comicsFlow.asStateFlow()

    private val _latestComicsFlow = MutableStateFlow<PagingData<Comic>>(PagingData.empty())
    val latestComicsFlow = _latestComicsFlow.asStateFlow()

    private val _favoriteComicsFlow = MutableStateFlow<PagingData<Comic>>(PagingData.empty())
    val favoriteComicsFlow = _favoriteComicsFlow.asStateFlow()

    init {
        loadCategories()
        // Initial load of comics can also consider the initial empty search query
        loadPaginatedComics()
    }

    /**
     * Loads or reloads paginated comics based on current filters,
     * and also refreshes latest and favorite comics lists.
     * Manages a global isLoading state for the entire operation.
     */
    private fun loadPaginatedComics(
        categoryId: Long? = _uiState.value.selectedCategory?.id,
        flags: Set<ComicFlags> = _uiState.value.flags,
        searchQuery: String? = _uiState.value.searchQuery
    ) {
        viewModelScope.launch {
            // Update isLoading only if it's a "full" load (no specific category, flags, or search)
            if (categoryId == null && flags.isEmpty() && searchQuery.isNullOrEmpty()) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            try {
                val sanitizedCategory =
                    if (categoryId == UserPreferencesKeys.DEFAULT_CATEGORY_ID_ALL) {
                        null
                    } else {
                        categoryId
                    }

                // Launch fetching of all three lists concurrently
                async {
                    getPaginatedComicsUseCase(
                        PaginatedComicsParams(
                            categoryId = sanitizedCategory,
                            flags = flags,
                            searchQuery = searchQuery // Pass the search query
                        )
                    )
                        .cachedIn(viewModelScope)
                        .catch { e ->
                            TimberLogger.logE("HomeViewModel", "Error loading main comics", e)
                            _effect.send(HomeEffect.ShowToast("Error loading comics: ${e.message}"))
                            emit(PagingData.empty())
                        }.collectLatest {
                            _comicsFlow.value = it
                        }
                }

                // Search query typically does not apply to "Latest" or "Favorite" sections
                async {
                    getPaginatedComicsUseCase(
                        PaginatedComicsParams(
                            flags = setOf(
                                ComicFlags.NEW
                            )
                            // searchQuery is not passed here, uses default null from PaginatedComicsParams if defined
                        )
                    )
                        .cachedIn(viewModelScope)
                        .catch { e ->
                            TimberLogger.logE("HomeViewModel", "Error loading latest comics", e)
                            _effect.send(HomeEffect.ShowToast("Error loading latest comics: ${e.message}"))
                            emit(PagingData.empty())
                        }.collectLatest {
                            _latestComicsFlow.value = it
                        }
                }

                async {
                    getPaginatedComicsUseCase(
                        PaginatedComicsParams(
                            flags = setOf(
                                ComicFlags.FAVORITE
                            )
                            // searchQuery is not passed here, uses default null from PaginatedComicsParams if defined
                        )
                    )
                        .cachedIn(viewModelScope)
                        .catch { e ->
                            TimberLogger.logE("HomeViewModel", "Error loading favorite comics", e)
                            _effect.send(HomeEffect.ShowToast("Error loading favorite comics: ${e.message}"))
                            emit(PagingData.empty())
                        }.collectLatest {
                            _favoriteComicsFlow.value = it
                        }
                }

            } catch (e: CancellationException) {
                TimberLogger.logD("HomeViewModel", "Comics loading cancelled", e)
            } catch (e: Exception) {
                TimberLogger.logE(
                    "HomeViewModel",
                    "Unexpected error during combined comics loading",
                    e
                )
                _effect.send(HomeEffect.ShowToast("An unexpected error occurred: ${e.message}"))
                _uiState.update { it.copy(error = e.message) }
                _comicsFlow.value = PagingData.empty()
                _latestComicsFlow.value = PagingData.empty()
                _favoriteComicsFlow.value = PagingData.empty()
            } finally {
                // Delay slightly to allow PagingData to propagate before hiding shimmer
                delay(250)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true, // Consider a more specific loading state for categories if needed
                    error = null
                )
            }
            loadCategoriesUseCase()
                .catch { e ->
                    TimberLogger.logE("HomeViewModel", "Error loading categories", e)
                    _effect.send(HomeEffect.ShowToast("Error loading categories: ${e.message}"))
                    _uiState.update {
                        it.copy(
                            isLoading = false, // Reset global loading if categories fail
                            error = "Failed to load categories: ${e.message}"
                        )
                    }
                }
                .collectLatest { fetchedCategories ->
                    _uiState.update {
                        it.copy(
                            categories = fetchedCategories,
                            isLoading = false // Reset global loading after categories are loaded
                        )
                    }
                }
        }
    }

    fun reduce(intent: HomeIntent) {
        viewModelScope.launch {
            when (intent) {
                is HomeIntent.LoadComics -> {
                    loadPaginatedComics()
                }

                is HomeIntent.NavigateTo -> _effect.send(HomeEffect.NavigateTo(intent.screen))

                is HomeIntent.SearchComics -> {
                    val currentQuery = _uiState.value.searchQuery
                    if (currentQuery != intent.query) { // Only reload if query changed
                        _uiState.update { it.copy(searchQuery = intent.query) }
                        // The loadPaginatedComics call will use the updated searchQuery from _uiState by default
                        loadPaginatedComics(
                            categoryId = _uiState.value.selectedCategory?.id,
                            flags = _uiState.value.flags
                        )
                    }
                }

                is HomeIntent.SetComicFilters -> {
                    _uiState.update { it.copy(flags = intent.flags) }
                    loadPaginatedComics(flags = _uiState.value.flags)
                }

                is HomeIntent.ToggleFlag -> {
                    _uiState.update { currentState ->
                        val newFlags = if (currentState.flags.contains(intent.flag)) {
                            currentState.flags - intent.flag
                        } else {
                            currentState.flags + intent.flag
                        }
                        currentState.copy(flags = newFlags)
                    }
                    loadPaginatedComics(flags = _uiState.value.flags)
                }

                is HomeIntent.AddFlag -> {
                    if (intent.clearAndSet) {
                        _uiState.update { it.copy(flags = setOf(intent.flag)) }
                    } else {
                        _uiState.update { it.copy(flags = it.flags + intent.flag) }
                    }
                    loadPaginatedComics(flags = _uiState.value.flags)
                }

                is HomeIntent.RemoveFlag -> {
                    _uiState.update { it.copy(flags = it.flags - intent.flag) }
                    loadPaginatedComics(flags = _uiState.value.flags)
                }

                is HomeIntent.ShowAllComics -> {
                    _uiState.update {
                        it.copy(
                            flags = emptySet(),
                            selectedCategory = null, // Clear selected category
                            currentBottomNavItem = BottomNavItems.HOME,
                            searchQuery = "" // Clear search query
                        )
                    }
                    loadPaginatedComics(flags = emptySet(), categoryId = null, searchQuery = "")
                }

                is HomeIntent.ShowFavoriteComics -> {
                    _uiState.update {
                        it.copy(
                            flags = setOf(ComicFlags.FAVORITE),
                            selectedCategory = null, 
                            currentBottomNavItem = BottomNavItems.FAVORITES,
                            searchQuery = "" // Clear search query when changing main tabs
                        )
                    }
                    loadPaginatedComics(flags = setOf(ComicFlags.FAVORITE), categoryId = null, searchQuery = "")
                }

                is HomeIntent.ShowNewComics -> {
                    _uiState.update {
                        it.copy(
                            flags = setOf(ComicFlags.NEW),
                            selectedCategory = null, 
                            currentBottomNavItem = BottomNavItems.BOOKMARKS, // Assuming 'New' maps to Bookmarks for now
                            searchQuery = "" // Clear search query
                        )
                    }
                    loadPaginatedComics(flags = setOf(ComicFlags.NEW), categoryId = null, searchQuery = "")
                }

                is HomeIntent.ShowReadComics -> {
                    _uiState.update {
                        it.copy(
                            flags = setOf(ComicFlags.READ),
                            selectedCategory = null, 
                            currentBottomNavItem = BottomNavItems.CATALOG, // Assuming 'Read' maps to Catalog for now
                            searchQuery = "" // Clear search query
                        )
                    }
                    loadPaginatedComics(flags = setOf(ComicFlags.READ), categoryId = null, searchQuery = "")
                }

                is HomeIntent.ViewModeChanged -> {
                    _uiState.update { it.copy(viewMode = intent.viewMode) }
                }

                is HomeIntent.CategorySelected -> {
                    _uiState.update {
                        it.copy(
                            selectedCategory = intent.category,
                            flags = emptySet(), // Optionally clear flags when a category is selected
                            searchQuery = "" // Optionally clear search when category changes
                        )
                    }
                    loadPaginatedComics(categoryId = intent.category?.id, flags = _uiState.value.flags, searchQuery = _uiState.value.searchQuery)
                }

                is HomeIntent.ComicSelected -> {
                    // Handled by navigation effect or other logic, no reload needed here
                }

                is HomeIntent.ClearSearch -> {
                    if (_uiState.value.searchQuery.isNotEmpty()) {
                        _uiState.update { it.copy(searchQuery = "") }
                        loadPaginatedComics() // Reload with cleared search query
                    }
                }

                is HomeIntent.RequestStoragePermission -> {
                    // TODO: Implement permission request logic if needed here or in View
                }

                is HomeIntent.ScanComicsFolders -> {
                    // TODO: Trigger comics folder scanning
                }
            }
        }
    }
}
