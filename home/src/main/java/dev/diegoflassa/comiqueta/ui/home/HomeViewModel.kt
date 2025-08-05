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
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags
import dev.diegoflassa.comiqueta.core.data.preferences.UserPreferencesKeys
import dev.diegoflassa.comiqueta.core.data.repository.IComicsFolderRepository
import dev.diegoflassa.comiqueta.core.domain.usecase.EnqueueSafFolderScanWorkerUseCase
import dev.diegoflassa.comiqueta.domain.usecase.IGetPaginatedComicsUseCase
import dev.diegoflassa.comiqueta.domain.usecase.PaginatedComicsParams
import dev.diegoflassa.comiqueta.ui.enums.BottomNavItems
import dev.diegoflassa.comiqueta.core.data.model.Comic
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
    }

    /**
     * Loads or reloads paginated comics based on current filters,
     * and also refreshes latest and favorite comics lists.
     * Manages a global isLoading state for the entire operation.
     */
    private fun loadPaginatedComics(
        categoryId: Long? = _uiState.value.selectedCategory?.id,
        flags: Set<ComicFlags> = _uiState.value.flags
    ) {
        viewModelScope.launch {
            if (categoryId == null && flags.isEmpty()) {
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
                        PaginatedComicsParams( // Use the imported top-level Params
                            categoryId = sanitizedCategory,
                            flags = flags
                        )
                    ).catch { e ->
                        TimberLogger.logE("HomeViewModel", "Error loading main comics", e)
                        _effect.send(HomeEffect.ShowToast("Error loading comics: ${e.message}"))
                        emit(PagingData.empty())
                    }.collectLatest {
                        _comicsFlow.value = it
                    }
                }

                async {
                    getPaginatedComicsUseCase(
                        PaginatedComicsParams(
                            flags = setOf(
                                ComicFlags.NEW
                            )
                        )
                    ).catch { e ->
                        TimberLogger.logE("HomeViewModel", "Error loading latest comics", e)
                        _effect.send(HomeEffect.ShowToast("Error loading latest comics: ${e.message}"))
                        emit(PagingData.empty())
                    }.collectLatest {
                        _latestComicsFlow.value = it
                    }
                }

                async {
                    getPaginatedComicsUseCase(
                        PaginatedComicsParams( // Use the imported top-level Params
                            flags = setOf(
                                ComicFlags.FAVORITE
                            )
                        )
                    ).catch { e ->
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
                    _uiState.update { it.copy(searchQuery = intent.query) }
                    // Assuming search triggers a reload of the main comics list with the query
                    // This part would require getPaginatedComicsUseCase to handle search queries
                    // or a separate SearchComicsUseCase. For now, it reloads with current filters.
                    loadPaginatedComics(flags = _uiState.value.flags) // Potentially add searchQuery here
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
                            currentBottomNavItem = BottomNavItems.HOME
                        )
                    }
                    loadPaginatedComics(flags = emptySet(), categoryId = null)
                }

                is HomeIntent.ShowFavoriteComics -> {
                    _uiState.update {
                        it.copy(
                            flags = setOf(ComicFlags.FAVORITE),
                            selectedCategory = null, // Clear selected category if favorites is a global filter
                            currentBottomNavItem = BottomNavItems.FAVORITES
                        )
                    }
                    loadPaginatedComics(flags = setOf(ComicFlags.FAVORITE), categoryId = null)
                }

                is HomeIntent.ShowNewComics -> {
                    _uiState.update {
                        it.copy(
                            flags = setOf(ComicFlags.NEW),
                            selectedCategory = null, // Clear selected category if new is a global filter
                            currentBottomNavItem = BottomNavItems.BOOKMARKS
                        )
                    }
                    loadPaginatedComics(flags = setOf(ComicFlags.NEW), categoryId = null)
                }

                is HomeIntent.ShowReadComics -> {
                    _uiState.update {
                        it.copy(
                            flags = setOf(ComicFlags.READ),
                            selectedCategory = null, // Clear selected category if read is a global filter
                            currentBottomNavItem = BottomNavItems.CATALOG // Assuming 'Read' maps to Catalog for now
                        )
                    }
                    loadPaginatedComics(flags = setOf(ComicFlags.READ), categoryId = null)
                }

                is HomeIntent.ViewModeChanged -> {
                    _uiState.update { it.copy(viewMode = intent.viewMode) }
                }

                is HomeIntent.CategorySelected -> {
                    _uiState.update {
                        it.copy(
                            selectedCategory = intent.category,
                            flags = emptySet()
                        )
                    } // Clear flags when category selected
                    loadPaginatedComics(categoryId = intent.category?.id, flags = emptySet())
                }

                is HomeIntent.ComicSelected -> {
                    _effect.send(HomeEffect.NavigateToComicDetail(intent.comic?.filePath))
                }

                is HomeIntent.FlagSelected -> {
                    _uiState.update {
                        it.copy(
                            flags = setOf(intent.flag),
                            selectedCategory = null
                        )
                    } // Clear category when flag selected
                    loadPaginatedComics(flags = setOf(intent.flag), categoryId = null)
                }

                is HomeIntent.AddFolderClicked -> handleAddFolderClicked()
                is HomeIntent.FolderSelected -> handleFolderSelected(intent.uri)

                is HomeIntent.FolderPermissionResult -> handleFolderPermissionResult(intent.isGranted)
                is HomeIntent.CheckInitialFolderPermission -> checkFolderPermissionStatus()
            }
        }
    }

    private fun checkFolderPermissionStatus() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            val isGranted = ContextCompat.checkSelfPermission(
                applicationContext,
                permission
            ) == PackageManager.PERMISSION_GRANTED
            TimberLogger.logD("HomeViewModel", "Folder permission granted: $isGranted (Legacy)")
            // Update UIState if it has a field for this, e.g., isLegacyPermissionGranted
        } else {
            TimberLogger.logD("HomeViewModel", "Folder permission granted: true (Android T+)")
            // Update UIState if it has a field for this
        }
    }

    private suspend fun handleAddFolderClicked() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            when {
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    permission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    _effect.send(HomeEffect.LaunchFolderPicker)
                }

                else -> {
                    _effect.send(HomeEffect.RequestStoragePermission(permission))
                }
            }
        } else {
            _effect.send(HomeEffect.LaunchFolderPicker)
        }
    }

    private suspend fun handleFolderPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            _effect.send(HomeEffect.LaunchFolderPicker)
        } else {
            _effect.send(HomeEffect.ShowToast("Storage permission is needed to select folders."))
        }
    }

    private suspend fun handleFolderSelected(uri: Uri) {
        TimberLogger.logD(
            "HomeViewModel",
            "Folder selected and permission should be taken by UI: $uri"
        )
        val success = comicsFolderRepository.takePersistablePermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        if (success) {
            _effect.send(HomeEffect.ShowToast("Background scan for folder $uri started."))
            triggerFolderScan()
        } else {
            _effect.send(HomeEffect.ShowToast("Failed to secure access to folder."))
        }
    }

    private fun triggerFolderScan() { // Consider if this needs to scan a specific URI or all managed folders
        viewModelScope.launch {
            try {
                // If EnqueueSafFolderScanWorkerUseCase can take a specific Uri, pass it.
                val workRequestId =
                    EnqueueSafFolderScanWorkerUseCase(WorkManager.getInstance(applicationContext)).invoke()
                _effect.send(HomeEffect.ShowToast("Folder scan enqueued."))

                // Observe the work
                observeScanWorker(workRequestId)
            } catch (ex: Exception) {
                TimberLogger.logE("HomeViewModel", "Failed to enqueue folder scan worker", ex)
                _effect.send(HomeEffect.ShowToast("Error starting scan: ${ex.message}"))
            }
        }
    }

    private fun observeScanWorker(workRequestId: UUID) {
        viewModelScope.launch {
            WorkManager.getInstance(applicationContext)
                .getWorkInfoByIdFlow(workRequestId)
                .collectLatest { workInfo ->
                    if (workInfo != null) {
                        TimberLogger.logD("HomeViewModel", "Scan Worker State: ${workInfo.state}")
                        when (workInfo.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                TimberLogger.logD(
                                    "HomeViewModel",
                                    "Folder scan SUCCEEDED. Refreshing comics."
                                )
                                _effect.send(HomeEffect.ShowToast("Scan complete. Refreshing..."))
                            }

                            WorkInfo.State.RUNNING -> {
                                TimberLogger.logD(
                                    "HomeViewModel",
                                    "Folder scan RUNNING. Refreshing comics."
                                )
                            }

                            WorkInfo.State.FAILED -> {
                                val errorMessage = workInfo.outputData.getString(SafFolderScanWorker.KEY_ERROR_MESSAGE)
                                TimberLogger.logW("HomeViewModel", "Folder scan FAILED. Worker message: $errorMessage")
                                FirebaseCrashlytics.getInstance()
                                    .recordException(Exception("Worker FAILED: ${errorMessage ?: "No specific message."} Raw output: ${workInfo.outputData.keyValueMap}"))
                                _effect.send(HomeEffect.ShowToast(errorMessage ?: "Scan failed."))
                            }

                            WorkInfo.State.CANCELLED -> {
                                TimberLogger.logI("HomeViewModel", "Folder scan CANCELLED.")
                                FirebaseCrashlytics.getInstance()
                                    .recordException(Exception(workInfo.outputData.keyValueMap.toString()))
                                _effect.send(HomeEffect.ShowToast("Scan cancelled."))
                            }

                            else -> {
                                // ENQUEUED, BLOCKED -
                            }
                        }
                    }
                }
        }
    }
}
