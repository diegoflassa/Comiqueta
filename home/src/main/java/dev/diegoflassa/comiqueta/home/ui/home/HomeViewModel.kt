package dev.diegoflassa.comiqueta.home.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags
import dev.diegoflassa.comiqueta.core.data.model.Comic
import dev.diegoflassa.comiqueta.core.data.preferences.UserPreferencesKeys
import dev.diegoflassa.comiqueta.core.data.repository.ComicsFolderRepository
import dev.diegoflassa.comiqueta.core.data.repository.ComicsRepository
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.domain.repository.ICategoryRepository
import dev.diegoflassa.comiqueta.core.domain.usecase.EnqueueSafFolderScanWorkerUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
open class HomeViewModel @Inject constructor(
    private val comicsRepository: ComicsRepository,
    private val categoryRepository: ICategoryRepository,
    private val comicsFolderRepository: ComicsFolderRepository,
    @param:ApplicationContext private val applicationContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUIState())
    val uiState: StateFlow<HomeUIState> = _uiState.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    private val _comicsFlow: MutableStateFlow<PagingData<Comic>> =
        MutableStateFlow(PagingData.empty())
    val comicsFlow = _comicsFlow.asStateFlow()

    private val _latestComicsFlow: MutableStateFlow<PagingData<Comic>> =
        MutableStateFlow(PagingData.empty())
    val latestComicsFlow = _latestComicsFlow.asStateFlow()

    private val _favoriteComicsFlow: MutableStateFlow<PagingData<Comic>> =
        MutableStateFlow(PagingData.empty())
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
            if (categoryId == null) {
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
                    comicsRepository.getComicsPaginated(
                        categoryId = sanitizedCategory,
                        flags = flags
                    )
                        .catch { e ->
                            TimberLogger.logE("HomeViewModel", "Error loading main comics", e)
                            _effect.send(HomeEffect.ShowToast("Error loading comics: ${e.message}"))
                            emit(PagingData.empty())
                        }
                        .collectLatest {
                            _comicsFlow.value = it
                        }
                }

                async {
                    comicsRepository.getComicsPaginated(flags = setOf(ComicFlags.NEW))
                        .catch { e ->
                            TimberLogger.logE("HomeViewModel", "Error loading latest comics", e)
                            _effect.send(HomeEffect.ShowToast("Error loading latest comics: ${e.message}"))
                            emit(PagingData.empty())
                        }
                        .collectLatest {
                            _latestComicsFlow.value = it
                        }
                }

                async {
                    comicsRepository.getComicsPaginated(flags = setOf(ComicFlags.FAVORITE))
                        .catch { e ->
                            TimberLogger.logE("HomeViewModel", "Error loading favorite comics", e)
                            _effect.send(HomeEffect.ShowToast("Error loading favorite comics: ${e.message}"))
                            emit(PagingData.empty())
                        }
                        .collectLatest {
                            _favoriteComicsFlow.value = it
                        }
                }

                // Wait for all of them to complete
                //awaitAll(mainComicsDeferred)//, latestComicsDeferred, favoriteComicsDeferred)

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
                delay(250)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }
            categoryRepository.getAllCategories()
                .catch { e ->
                    TimberLogger.logE("HomeViewModel", "Error loading categories", e)
                    _effect.send(HomeEffect.ShowToast("Error loading categories: ${e.message}"))
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load categories: ${e.message}"
                        )
                    }
                }
                .collectLatest { fetchedCategories ->
                    _uiState.update {
                        it.copy(
                            categories = fetchedCategories,
                            isLoading = false
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
                    // Update search query in UIState.
                    // The PagingData flow should ideally react to this searchQuery as well,
                    // by passing it to the PagingSource.
                    _uiState.update { it.copy(searchQuery = intent.query) }
                    // Example: loadPaginatedComics(searchQuery = intent.query)
                    // You'll need to adjust how PagingSource receives this query.
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
                    loadPaginatedComics(flags = _uiState.value.flags)
                    _uiState.update { it.copy(flags = emptySet()) }
                }

                is HomeIntent.ShowFavoriteComics -> {
                    _uiState.update { it.copy(flags = setOf(ComicFlags.FAVORITE)) }
                    loadPaginatedComics(flags = _uiState.value.flags)
                }

                is HomeIntent.ShowNewComics -> {
                    _uiState.update { it.copy(flags = setOf(ComicFlags.NEW)) }
                    loadPaginatedComics(flags = _uiState.value.flags)
                }

                is HomeIntent.ShowReadComics -> {
                    _uiState.update { it.copy(flags = setOf(ComicFlags.READ)) }
                    loadPaginatedComics(flags = _uiState.value.flags)
                }

                // --- Intents for view mode handling ---
                is HomeIntent.ViewModeChanged -> {
                    _uiState.update { it.copy(viewMode = intent.viewMode) }
                }

                // --- Intents for selection handling ---
                is HomeIntent.CategorySelected -> {
                    _uiState.update { it.copy(selectedCategory = intent.category) }
                    loadPaginatedComics(categoryId = intent.category?.id)
                }

                is HomeIntent.ComicSelected -> {
                    _effect.send(HomeEffect.NavigateToComicDetail(intent.comic?.filePath))
                }

                is HomeIntent.FlagSelected -> {
                    _uiState.update { it.copy(flags = setOf(intent.flag)) }
                    loadPaginatedComics(flags = _uiState.value.flags)
                }

                // --- Intents for folder handling ---
                is HomeIntent.AddFolderClicked -> handleAddFolderClicked()
                is HomeIntent.FolderSelected -> handleFolderSelected(intent.uri)

                // --- Intents for permissions handling ---
                is HomeIntent.FolderPermissionResult -> handleFolderPermissionResult(intent.isGranted)
                is HomeIntent.CheckInitialFolderPermission -> checkFolderPermissionStatus()
            }
        }
    }

    // loadAllComicData is replaced by loadPaginatedComics and loadAuxiliaryComicLists
    // private fun loadAllComicData() { ... OLD CODE REMOVED ... }


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
            triggerFolderScan() // Make sure this uses the specific URI or a general scan
        } else {
            _effect.send(HomeEffect.ShowToast("Failed to secure access to folder."))
        }
    }

    private fun triggerFolderScan() { // Consider if this needs to scan a specific URI or all managed folders
        viewModelScope.launch {
            try {
                // If EnqueueSafFolderScanWorkerUseCase can take a specific Uri, pass it.
                EnqueueSafFolderScanWorkerUseCase(applicationContext).invoke()
                _effect.send(HomeEffect.ShowToast("Folder scan enqueued."))
            } catch (ex: Exception) {
                TimberLogger.logE("HomeViewModel", "Failed to enqueue folder scan worker", ex)
                _effect.send(HomeEffect.ShowToast("Error starting scan: ${ex.message}"))
            }
        }
    }
}

