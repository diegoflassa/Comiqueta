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
import dev.diegoflassa.comiqueta.core.domain.model.Comic
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.data.worker.SafFolderScanWorker
import dev.diegoflassa.comiqueta.core.domain.usecase.EnqueueSafFolderScanWorkerUseCase
import dev.diegoflassa.comiqueta.domain.usecase.IGetPaginatedComicsUseCase
import dev.diegoflassa.comiqueta.domain.usecase.ILoadCategoriesUseCase
import dev.diegoflassa.comiqueta.domain.usecase.PaginatedComicsParams
import dev.diegoflassa.comiqueta.ui.enums.BottomNavItems
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
    private val enqueueSafFolderScanWorkerUseCase: EnqueueSafFolderScanWorkerUseCase
) : ViewModel() {

    companion object {
        private val tag = HomeViewModel::class.simpleName
    }

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
        loadPaginatedComics()
        reduce(HomeIntent.CheckInitialFolderPermission)
    }

    private fun hasGeneralStoragePermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES // Or READ_MEDIA_VIDEO / READ_MEDIA_AUDIO as needed
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(
            applicationContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
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
                    if (currentQuery != intent.query) {
                        _uiState.update { it.copy(searchQuery = intent.query) }
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
                            selectedCategory = null,
                            currentBottomNavItem = BottomNavItems.HOME,
                            searchQuery = ""
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
                            searchQuery = ""
                        )
                    }
                    loadPaginatedComics(
                        flags = setOf(ComicFlags.FAVORITE),
                        categoryId = null,
                        searchQuery = ""
                    )
                }

                is HomeIntent.ShowNewComics -> {
                    _uiState.update {
                        it.copy(
                            flags = setOf(ComicFlags.NEW),
                            selectedCategory = null,
                            currentBottomNavItem = BottomNavItems.BOOKMARKS,
                            searchQuery = ""
                        )
                    }
                    loadPaginatedComics(
                        flags = setOf(ComicFlags.NEW),
                        categoryId = null,
                        searchQuery = ""
                    )
                }

                is HomeIntent.ShowReadComics -> {
                    _uiState.update {
                        it.copy(
                            flags = setOf(ComicFlags.READ),
                            selectedCategory = null,
                            currentBottomNavItem = BottomNavItems.CATALOG,
                            searchQuery = ""
                        )
                    }
                    loadPaginatedComics(
                        flags = setOf(ComicFlags.READ),
                        categoryId = null,
                        searchQuery = ""
                    )
                }

                is HomeIntent.ViewModeChanged -> {
                    _uiState.update { it.copy(viewMode = intent.viewMode) }
                }

                is HomeIntent.CategorySelected -> {
                    _uiState.update {
                        it.copy(
                            selectedCategory = intent.category,
                            flags = emptySet(),
                            searchQuery = ""
                        )
                    }
                    loadPaginatedComics(
                        categoryId = intent.category?.id,
                        flags = _uiState.value.flags,
                        searchQuery = _uiState.value.searchQuery
                    )
                }

                is HomeIntent.ComicSelected -> {
                    _effect.send(HomeEffect.NavigateToComicDetail(intent.comic?.filePath))
                }

                is HomeIntent.ClearSearch -> {
                    if (_uiState.value.searchQuery.isNotEmpty()) {
                        _uiState.update { it.copy(searchQuery = "") }
                        loadPaginatedComics()
                    }
                }

                is HomeIntent.RequestStoragePermission -> {
                    TimberLogger.logD(tag, "Intent: RequestStoragePermission received.")
                    _effect.send(HomeEffect.RequestGeneralStoragePermission)
                }

                is HomeIntent.ScanComicsFolders -> {
                    triggerGeneralScan()
                }

                is HomeIntent.AddFolderClicked -> {
                    TimberLogger.logD(tag, "Intent: AddFolderClicked received.")
                    if (!hasGeneralStoragePermission()) {
                        _effect.send(HomeEffect.ShowToast("Storage permission needed to add folders."))
                        _effect.send(HomeEffect.RequestGeneralStoragePermission)
                        return@launch
                    }
                    _effect.send(HomeEffect.OpenFolderPicker)
                }

                is HomeIntent.CheckInitialFolderPermission -> {
                    TimberLogger.logD(tag, "Intent: CheckInitialFolderPermission received.")
                    val isGranted = hasGeneralStoragePermission()
                    _uiState.update { it.copy(generalStoragePermissionGranted = isGranted) }
                    if (!isGranted) {
                        TimberLogger.logI(tag, "Initial storage permission check: NOT granted.")
                        _effect.send(HomeEffect.ShowToast("Storage permission is recommended for full functionality."))
                        // Optionally, prompt for permission immediately:
                        // _effect.send(HomeEffect.RequestGeneralStoragePermission)
                    } else {
                        TimberLogger.logI(tag, "Initial storage permission check: GRANTED.")
                    }
                }

                is HomeIntent.FlagSelected -> {
                    TimberLogger.logD(
                        tag,
                        "Intent: FlagSelected received with flag: ${intent.flag}"
                    )
                    _uiState.update { currentState ->
                        currentState.copy(flags = setOf(intent.flag))
                    }
                    loadPaginatedComics(flags = _uiState.value.flags)
                }

                is HomeIntent.FolderPermissionResult -> {
                    TimberLogger.logD(
                        tag,
                        "Intent: FolderPermissionResult received. Granted: ${intent.isGranted}"
                    )
                    _uiState.update { it.copy(generalStoragePermissionGranted = intent.isGranted) }
                    if (intent.isGranted) {
                        _effect.send(HomeEffect.ShowToast("Storage permission granted!"))
                        // Optionally trigger a scan or other actions, e.g., if a blocked action can now proceed
                        // reduce(HomeIntent.ScanComicsFolders) // Example: Trigger scan if this was the purpose
                    } else {
                        _effect.send(HomeEffect.ShowToast("Storage permission denied. Some features might be limited."))
                    }
                }

                is HomeIntent.FolderSelected -> {
                    handleFolderSelected(intent.uri)
                }
            }
        }
    }

    private fun loadPaginatedComics(
        categoryId: Long? = _uiState.value.selectedCategory?.id,
        flags: Set<ComicFlags> = _uiState.value.flags,
        searchQuery: String? = _uiState.value.searchQuery
    ) {
        viewModelScope.launch {
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

                async {
                    getPaginatedComicsUseCase(
                        PaginatedComicsParams(
                            categoryId = sanitizedCategory,
                            flags = flags,
                            searchQuery = searchQuery
                        )
                    )
                        .cachedIn(viewModelScope)
                        .catch { e ->
                            TimberLogger.logE(tag, "Error loading main comics", e)
                            _effect.send(HomeEffect.ShowToast("Error loading comics: ${e.message}"))
                            emit(PagingData.empty())
                        }.collectLatest { _comicsFlow.value = it }
                }

                async {
                    getPaginatedComicsUseCase(
                        PaginatedComicsParams(flags = setOf(ComicFlags.NEW))
                    )
                        .cachedIn(viewModelScope)
                        .catch { e ->
                            TimberLogger.logE(tag, "Error loading latest comics", e)
                            _effect.send(HomeEffect.ShowToast("Error loading latest comics: ${e.message}"))
                            emit(PagingData.empty())
                        }.collectLatest { _latestComicsFlow.value = it }
                }

                async {
                    getPaginatedComicsUseCase(
                        PaginatedComicsParams(flags = setOf(ComicFlags.FAVORITE))
                    )
                        .cachedIn(viewModelScope)
                        .catch { e ->
                            TimberLogger.logE(tag, "Error loading favorite comics", e)
                            _effect.send(HomeEffect.ShowToast("Error loading favorite comics: ${e.message}"))
                            emit(PagingData.empty())
                        }.collectLatest { _favoriteComicsFlow.value = it }
                }

            } catch (e: CancellationException) {
                TimberLogger.logD(tag, "Comics loading cancelled", e)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                TimberLogger.logE(tag, "Unexpected error during combined comics loading", e)
                _effect.send(HomeEffect.ShowToast("An unexpected error occurred: ${e.message}"))
                _uiState.update { it.copy(error = e.message, isLoading = false) }
                _comicsFlow.value = PagingData.empty()
                _latestComicsFlow.value = PagingData.empty()
                _favoriteComicsFlow.value = PagingData.empty()
            } finally {
                if (_uiState.value.isLoading) {
                    delay(250)
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            //isLoading for categories is handled within loadPaginatedComics or a separate state variable
            loadCategoriesUseCase()
                .catch { e ->
                    TimberLogger.logE(tag, "Error loading categories", e)
                    _effect.send(HomeEffect.ShowToast("Error loading categories: ${e.message}"))
                    _uiState.update { it.copy(error = "Failed to load categories: ${e.message}") }
                }
                .collectLatest { fetchedCategories ->
                    _uiState.update { it.copy(categories = fetchedCategories) }
                }
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
            triggerGeneralScan()
        } else {
            _effect.send(HomeEffect.ShowToast("Failed to secure access to folder."))
        }
    }

    private fun triggerGeneralScan() {
        viewModelScope.launch {
            try {
                val workRequestId =
                    enqueueSafFolderScanWorkerUseCase.invoke(null)
                _effect.send(HomeEffect.ShowToast("General folder scan enqueued."))
                observeScanWorker(workRequestId)
            } catch (ex: Exception) {
                TimberLogger.logE(tag, "Failed to enqueue general folder scan worker", ex)
                _effect.send(HomeEffect.ShowToast("Error starting general scan: ${ex.message}"))
            }
        }
    }

    private fun observeScanWorker(workRequestId: UUID) {
        viewModelScope.launch {
            WorkManager.getInstance(applicationContext)
                .getWorkInfoByIdFlow(workRequestId)
                .collectLatest { workInfo ->
                    _uiState.update { it.copy(isScanningFolders = workInfo?.state == WorkInfo.State.RUNNING) }
                    if (workInfo != null) {
                        TimberLogger.logD(
                            tag,
                            "Scan Worker ($workRequestId) State: ${workInfo.state}"
                        )
                        when (workInfo.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                TimberLogger.logD(
                                    tag,
                                    "Folder scan SUCCEEDED for $workRequestId. Refreshing comics."
                                )
                                _effect.send(HomeEffect.ShowToast("Scan complete. Refreshing..."))
                                loadPaginatedComics()
                            }

                            WorkInfo.State.FAILED -> {
                                val errorMessage =
                                    workInfo.outputData.getString(SafFolderScanWorker.KEY_ERROR_MESSAGE)
                                TimberLogger.logW(
                                    tag,
                                    "Folder scan FAILED for $workRequestId. Worker message: $errorMessage"
                                )
                                FirebaseCrashlytics.getInstance()
                                    .recordException(Exception("Worker FAILED ($workRequestId): ${errorMessage ?: "No message"}"))
                                _effect.send(HomeEffect.ShowToast(errorMessage ?: "Scan failed."))
                            }

                            WorkInfo.State.CANCELLED -> {
                                TimberLogger.logI(tag, "Folder scan CANCELLED for $workRequestId.")
                                _effect.send(HomeEffect.ShowToast("Scan cancelled."))
                            }

                            else -> {
                                /* ENQUEUED, RUNNING (handled by uiState update), BLOCKED */
                            }
                        }
                    }
                }
        }
    }
}
