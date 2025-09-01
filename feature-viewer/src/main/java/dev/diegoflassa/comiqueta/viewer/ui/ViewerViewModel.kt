package dev.diegoflassa.comiqueta.viewer.ui

import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.comiqueta.core.data.preferences.PreferencesKeys
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.model.ComicFileType
import dev.diegoflassa.comiqueta.viewer.domain.usecase.IDecodeComicPageUseCase
import dev.diegoflassa.comiqueta.viewer.domain.usecase.IGetComicInfoUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

@HiltViewModel
open class ViewerViewModel @Inject constructor(
    private val getComicInfoUseCase: IGetComicInfoUseCase,
    private val decodeComicPageUseCase: IDecodeComicPageUseCase,
    dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUIState())
    open val uiState: StateFlow<ViewerUIState> = _uiState.asStateFlow()

    private val _effect = Channel<ViewerEffect>(Channel.BUFFERED)
    open val effect: Flow<ViewerEffect> = _effect.receiveAsFlow()

    private var comicPageIdentifiers: List<String> = emptyList()
    private var currentComicUri: Uri? = null
    private var currentComicFileType: ComicFileType? = null

    private lateinit var pageBitmapCache: LruCache<Int, ImageBitmap>

    private var focusedPageJob: Job? = null
    private val neighborPageJobs = mutableMapOf<Int, Job>()

    private val _pagesToPreloadLogic =
        MutableStateFlow(ViewerUIState.DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD)

    companion object {
        private const val TAG = "ViewerViewModel"
        private const val MIN_PRELOAD_COUNT_LOGIC = 0
        private const val MAX_PRELOAD_COUNT_LOGIC = 5
        private const val MAX_SETTING_FOR_CACHE_INIT =
            10 // Max setting value considered for initial cache sizing
    }

    init {
        viewModelScope.launch {
            val viewerPagesToPreloadAheadFlow: Flow<Int> = dataStore.data
                .map {
                    it[PreferencesKeys.VIEWER_PAGES_TO_PRELOAD_AHEAD]
                        ?: ViewerUIState.DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD
                }

            val initialSettingValue = try {
                viewerPagesToPreloadAheadFlow.first()
            } catch (ex: Exception) {
                TimberLogger.logE(TAG, "Failed to get initial preload count, using default.", ex)
                ViewerUIState.DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD
            }

            val cacheInitPreloadCount = initialSettingValue
                .coerceAtLeast(MIN_PRELOAD_COUNT_LOGIC)
                .coerceAtMost(MAX_SETTING_FOR_CACHE_INIT)
            pageBitmapCache = LruCache(1 + 2 * cacheInitPreloadCount) // Focused + 2 sides * preload
            TimberLogger.logI(
                TAG,
                "Cache initialized. Capacity based on setting value: $initialSettingValue (used for cache calc: $cacheInitPreloadCount)"
            )

            val initialLogicPreload = initialSettingValue
                .coerceAtLeast(MIN_PRELOAD_COUNT_LOGIC)
                .coerceAtMost(MAX_PRELOAD_COUNT_LOGIC)
            _pagesToPreloadLogic.value = initialLogicPreload
            _uiState.update { it.copy(pagesToPreloadLogic = initialLogicPreload, loadingNeighborIndices = emptySet()) }
            TimberLogger.logI(TAG, "Initial logic preload count set to: $initialLogicPreload.")

            viewerPagesToPreloadAheadFlow
                .catch { e ->
                    TimberLogger.logE(TAG, "Error observing viewerPagesToPreloadAhead", e)
                    val safeDefault = ViewerUIState.DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD
                        .coerceAtLeast(MIN_PRELOAD_COUNT_LOGIC)
                        .coerceAtMost(MAX_PRELOAD_COUNT_LOGIC)
                    if (_pagesToPreloadLogic.value != safeDefault) {
                        _pagesToPreloadLogic.value = safeDefault
                        _uiState.update { it.copy(pagesToPreloadLogic = safeDefault) }
                        dispatchLoadFocusedAndNeighbors(uiState.value.currentPage)
                    }
                }
                .collect { newSettingValue ->
                    val newLogicPreload = newSettingValue
                        .coerceAtLeast(MIN_PRELOAD_COUNT_LOGIC)
                        .coerceAtMost(MAX_PRELOAD_COUNT_LOGIC)
                    if (_pagesToPreloadLogic.value != newLogicPreload) {
                        TimberLogger.logI(
                            TAG,
                            "Preload setting changed. From DataStore: $newSettingValue, Applied for Logic: $newLogicPreload"
                        )
                        _pagesToPreloadLogic.value = newLogicPreload
                        _uiState.update { it.copy(pagesToPreloadLogic = newLogicPreload) }
                        dispatchLoadFocusedAndNeighbors(uiState.value.currentPage)
                    }
                }
        }
    }

    open fun reduce(intent: ViewerIntent) {
        TimberLogger.logI(TAG, "Reducing intent: $intent")
        when (intent) {
            is ViewerIntent.LoadComic -> handleLoadComic(intent.uriString.toUri())
            is ViewerIntent.GoToPage -> dispatchLoadFocusedAndNeighbors(intent.pageNumber)
            is ViewerIntent.NavigateNextPage -> {
                val nextPage = uiState.value.currentPage + 1
                if (nextPage < uiState.value.pageCount) {
                    dispatchLoadFocusedAndNeighbors(nextPage)
                }
            }

            is ViewerIntent.NavigatePreviousPage -> {
                val prevPage = uiState.value.currentPage - 1
                if (prevPage >= 0) {
                    dispatchLoadFocusedAndNeighbors(prevPage)
                }
            }

            is ViewerIntent.ToggleUiVisibility -> _uiState.update { it.copy(isUiVisible = !it.isUiVisible) }
            is ViewerIntent.ErrorShown -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun handleLoadComic(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingFocused = true, error = null, comicTitle = "",
                    focusedBitmap = null, neighborBitmaps = emptyMap(),
                    currentPage = 0, pageCount = 0,
                    pagesToPreloadLogic = _pagesToPreloadLogic.value,
                    loadingNeighborIndices = emptySet()
                )
            }
            currentComicUri = uri
            if (::pageBitmapCache.isInitialized) {
                pageBitmapCache.evictAll()
            } else {
                TimberLogger.logE(
                    TAG,
                    "CRITICAL: Cache accessed in handleLoadComic before init completed!"
                )
                val fallbackPreload =
                    _pagesToPreloadLogic.value.coerceAtMost(MAX_SETTING_FOR_CACHE_INIT)
                pageBitmapCache = LruCache(1 + 2 * fallbackPreload)
            }
            comicPageIdentifiers = emptyList()

            focusedPageJob?.cancelJob("New comic load requested")
            neighborPageJobs.values.forEach { it.cancelJob("New comic load requested") }
            neighborPageJobs.clear()
            _uiState.update { it.copy(loadingNeighborIndices = emptySet()) }


            try {
                TimberLogger.logI(TAG, "LoadComic: Starting for $uri")
                val comicInfo = getComicInfoUseCase(currentComicUri!!)
                comicPageIdentifiers = comicInfo.pageIdentifiers
                currentComicFileType = comicInfo.fileType

                _uiState.update {
                    it.copy(
                        comicTitle = comicInfo.title, pageCount = comicInfo.pageCount,
                        fileType = comicInfo.fileType
                    )
                }

                if (comicInfo.pageCount > 0 && comicInfo.pageIdentifiers.isNotEmpty()) {
                    val initialPage = 0 // Or potentially a saved last read page for this comic
                    dispatchLoadFocusedAndNeighbors(initialPage)
                } else {
                    TimberLogger.logW(TAG, "LoadComic: Comic has no pages.")
                    _effect.send(ViewerEffect.ShowError("Comic has no pages or is empty."))
                    _uiState.update { it.copy(isLoadingFocused = false) }
                }
            } catch (cex: CancellationException) {
                TimberLogger.logI(TAG, "LoadComic (getComicInfo) cancelled: ${cex.message}")
                _uiState.update { it.copy(isLoadingFocused = false) }
            } catch (ex: Exception) {
                TimberLogger.logE(TAG, "LoadComic (getComicInfo): Error loading comic $uri", ex)
                val errorMessage = ex.localizedMessage ?: "Failed to load comic"
                _uiState.update { it.copy(isLoadingFocused = false, error = errorMessage) }
                _effect.send(ViewerEffect.ShowError(errorMessage))
            }
        }
    }

    private fun dispatchLoadFocusedAndNeighbors(targetPageIndex: Int) {
        TimberLogger.logD(TAG, "dispatchLoadFocusedAndNeighbors for page: $targetPageIndex")
        if (!::pageBitmapCache.isInitialized) {
            TimberLogger.logE(TAG, "dispatchLoadFocusedAndNeighbors: Cache not ready. Aborting.")
            _uiState.update {
                it.copy(
                    isLoadingFocused = false,
                    error = "Internal error: Viewer not ready."
                )
            }
            return
        }
        if (currentComicUri == null || currentComicFileType == null || comicPageIdentifiers.isEmpty()) {
            TimberLogger.logW(
                TAG,
                "dispatchLoadFocusedAndNeighbors: Comic data not ready for page $targetPageIndex. URI: $currentComicUri, FileType: $currentComicFileType, Identifiers Empty: ${comicPageIdentifiers.isEmpty()}"
            )
            if (uiState.value.pageCount == 0) {
                _uiState.update {
                    it.copy(
                        isLoadingFocused = false,
                        error = "Comic data not fully loaded."
                    )
                }
            }
            return
        }
        if (targetPageIndex < 0 || targetPageIndex >= uiState.value.pageCount) {
            TimberLogger.logW(
                TAG,
                "dispatchLoadFocusedAndNeighbors: Invalid targetPageIndex $targetPageIndex for pageCount ${uiState.value.pageCount}"
            )
            viewModelScope.launch { _effect.send(ViewerEffect.ShowError("Invalid page number: ${targetPageIndex + 1}")) }
            return
        }

        focusedPageJob?.cancelJob("New target page: $targetPageIndex")
        val cachedFocusedBitmap = pageBitmapCache.get(targetPageIndex)

        _uiState.update {
            it.copy(
                currentPage = targetPageIndex,
                focusedBitmap = cachedFocusedBitmap,
                isLoadingFocused = cachedFocusedBitmap == null,
                error = null
            )
        }

        if (cachedFocusedBitmap == null) {
            focusedPageJob = viewModelScope.launch {
                TimberLogger.logD(TAG, "Focused page $targetPageIndex: Not in cache, launching load job.")
                try {
                    val bitmap = loadPageBitmapInternal(targetPageIndex)
                    if (isActive) {
                        _uiState.update { state ->
                            if (state.currentPage == targetPageIndex) {
                                state.copy(
                                    focusedBitmap = bitmap,
                                    isLoadingFocused = false,
                                    error = if (bitmap == null && state.error == null) "Failed to load page ${targetPageIndex + 1}" else state.error
                                )
                            } else state
                        }
                        if (bitmap == null && isActive && uiState.value.currentPage == targetPageIndex) {
                            _effect.send(ViewerEffect.ShowError("Failed to load page ${targetPageIndex + 1}"))
                        }
                    }
                } catch (cex: CancellationException) {
                    TimberLogger.logE(TAG, "Focused page $targetPageIndex loading cancelled.", cex)
                    if (isActive && uiState.value.currentPage == targetPageIndex) {
                        _uiState.update { it.copy(isLoadingFocused = false) }
                    }
                } catch (ex: Exception) {
                    TimberLogger.logE(TAG, "Error loading focused page $targetPageIndex", ex)
                    if (isActive && uiState.value.currentPage == targetPageIndex) {
                        val errorMsg = ex.localizedMessage ?: "Error loading page ${targetPageIndex + 1}"
                        _uiState.update { it.copy(isLoadingFocused = false, error = errorMsg) }
                        _effect.send(ViewerEffect.ShowError(errorMsg))
                    }
                }
            }
        }

        // --- Neighbor Pages Logic ---
        val currentLogicPreload = _pagesToPreloadLogic.value
        val pageCount = uiState.value.pageCount
        val validPreloadIndices = mutableSetOf<Int>()

        if (currentLogicPreload > MIN_PRELOAD_COUNT_LOGIC) {
            for (offset in 1..currentLogicPreload) {
                if (targetPageIndex - offset >= 0) validPreloadIndices.add(targetPageIndex - offset)
                if (targetPageIndex + offset < pageCount) validPreloadIndices.add(targetPageIndex + offset)
            }
        }
        TimberLogger.logI(TAG, "Neighbors for $targetPageIndex (preload $currentLogicPreload): Valid indices: $validPreloadIndices")

        // 1. Cancel jobs for neighbors no longer in the preload range
        val jobsToCancel = neighborPageJobs.filterKeys { !validPreloadIndices.contains(it) }
        jobsToCancel.forEach { (idx, job) ->
            TimberLogger.logD(TAG, "Neighbor $idx: No longer in preload range. Cancelling job.")
            job.cancelJob("No longer in preload range for $targetPageIndex")
            neighborPageJobs.remove(idx)
        }

        // 2. Determine current state of valid neighbors and prepare for UI update
        val newNeighborBitmapsForUI = mutableMapOf<Int, ImageBitmap>()
        val newLoadingNeighborIndicesForUI = mutableSetOf<Int>()
        val neighborsThatNeedNewLoadJob = mutableSetOf<Int>()

        validPreloadIndices.forEach { index ->
            pageBitmapCache.get(index)?.let { cachedBitmap ->
                newNeighborBitmapsForUI[index] = cachedBitmap // Already cached
            } ?: run {
                // Not cached
                if (neighborPageJobs[index]?.isActive == true) {
                    newLoadingNeighborIndicesForUI.add(index) // Job already active
                } else {
                    newLoadingNeighborIndicesForUI.add(index) // Will start a new job
                    neighborsThatNeedNewLoadJob.add(index)
                }
            }
        }

        // 3. Update UI state in one go for all valid neighbors (cached, already loading, or will be loading)
        _uiState.update { currentState ->
            // Filter out any loading indices or bitmaps from old neighbors that are no longer valid
            val relevantOldLoadingIndices = currentState.loadingNeighborIndices.filter { validPreloadIndices.contains(it) }.toMutableSet()
            val relevantOldNeighborBitmaps = currentState.neighborBitmaps.filterKeys { validPreloadIndices.contains(it) }.toMutableMap()

            // Combine with new information
            relevantOldLoadingIndices.addAll(newLoadingNeighborIndicesForUI)
            relevantOldNeighborBitmaps.putAll(newNeighborBitmapsForUI)
            
            // Ensure that if a bitmap is now present, it's not also in loading state
            relevantOldLoadingIndices.removeAll(relevantOldNeighborBitmaps.keys)

            TimberLogger.logD(TAG, "Syncing UI state: Final Neighbors in UI: ${relevantOldNeighborBitmaps.keys}, Final Loading Indices: $relevantOldLoadingIndices")
            currentState.copy(
                neighborBitmaps = relevantOldNeighborBitmaps,
                loadingNeighborIndices = relevantOldLoadingIndices
            )
        }

        // 4. Launch jobs for neighbors that were identified as needing a new load operation
        neighborsThatNeedNewLoadJob.forEach { neighborIdx ->
            TimberLogger.logD(TAG, "Neighbor $neighborIdx: Not cached, no prior active job. Launching new load job.")

            neighborPageJobs[neighborIdx] = viewModelScope.launch {
                try {
                    val bitmap = loadPageBitmapInternal(neighborIdx)
                    if (isActive && bitmap != null) {
                        _uiState.update { state ->
                            // Check if still a valid neighbor before updating
                            if (abs(state.currentPage - neighborIdx) <= state.pagesToPreloadLogic) {
                                TimberLogger.logD(TAG, "Neighbor $neighborIdx loaded, adding to neighborBitmaps and removing from loading.")
                                state.copy(
                                    neighborBitmaps = state.neighborBitmaps + (neighborIdx to bitmap),
                                    loadingNeighborIndices = state.loadingNeighborIndices - neighborIdx
                                )
                            } else {
                                TimberLogger.logD(TAG, "Neighbor $neighborIdx loaded, but no longer relevant. Removing from loading.")
                                state.copy(loadingNeighborIndices = state.loadingNeighborIndices - neighborIdx)
                            }
                        }
                    } else if (isActive && bitmap == null) { // Load failed or returned null
                        TimberLogger.logW(TAG, "Neighbor $neighborIdx: Load returned null. Removing from loading.")
                         _uiState.update { it.copy(loadingNeighborIndices = it.loadingNeighborIndices - neighborIdx) }
                    }
                } catch (cex: CancellationException) {
                    TimberLogger.logI(TAG, "Neighbor page $neighborIdx loading cancelled: ${cex.message}")
                } catch (ex: Exception) {
                    TimberLogger.logE(TAG, "Error loading neighbor page $neighborIdx", ex)
                } finally {
                    if (isActive) { // Only update state and jobs map if the scope is still active
                        TimberLogger.logD(TAG, "Neighbor $neighborIdx: Job finished. Removing from loadingNeighborIndices and neighborPageJobs map.")
                        _uiState.update { it.copy(loadingNeighborIndices = it.loadingNeighborIndices - neighborIdx) }
                    }
                    neighborPageJobs.remove(neighborIdx) // Always remove from job map
                }
            }
        }
    }

    private suspend fun loadPageBitmapInternal(pageIndex: Int): ImageBitmap? {
        if (!::pageBitmapCache.isInitialized) {
            TimberLogger.logE(TAG, "loadPageBitmapInternal: Cache not ready for page $pageIndex!")
            throw IllegalStateException("Cache not initialized when trying to load page $pageIndex")
        }
        val localCurrentComicUri = currentComicUri
        val localCurrentComicFileType = currentComicFileType
        if (localCurrentComicUri == null || localCurrentComicFileType == null) {
            TimberLogger.logW(TAG, "loadPageBitmapInternal($pageIndex): Aborted. Comic data not ready.")
            throw IllegalStateException("Comic data (URI or FileType) not ready for page $pageIndex")
        }
        if (comicPageIdentifiers.isEmpty() || pageIndex < 0 || pageIndex >= comicPageIdentifiers.size) {
            TimberLogger.logE(TAG, "loadPageBitmapInternal($pageIndex): Aborted. PageIndex out of bounds (0-${comicPageIdentifiers.size - 1}).")
            throw IndexOutOfBoundsException("Page index $pageIndex out of bounds for ${comicPageIdentifiers.size} pages")
        }

        pageBitmapCache.get(pageIndex)?.let {
            TimberLogger.logI(TAG, "Page $pageIndex found in cache.")
            return it
        }

        TimberLogger.logD(TAG, "Page $pageIndex not in cache. Decoding.")
        val pageIdentifier = comicPageIdentifiers[pageIndex]
        return try {
            val bitmap = decodeComicPageUseCase(
                pageIndex,
                pageIdentifier,
                localCurrentComicUri,
                localCurrentComicFileType
            )
            bitmap?.also {
                TimberLogger.logD(TAG, "Successfully decoded page $pageIndex. Caching.")
                if (::pageBitmapCache.isInitialized) pageBitmapCache.put(pageIndex, it)
            } ?: run {
                TimberLogger.logW(TAG, "decodeComicPageUseCase returned null for page $pageIndex.")
                null
            }
        } catch (cex: CancellationException) {
            TimberLogger.logI(TAG, "Decoding cancelled for page $pageIndex: ${cex.message}")
            throw cex
        } catch (ex: Exception) {
            TimberLogger.logE(TAG, "Error during decodeComicPage for $pageIndex", ex)
            throw ex
        }
    }

    private fun Job.cancelJob(message: String) {
        try {
            if (this.isActive) this.cancel(CancellationException(message))
        } catch (ex: Exception) {
            TimberLogger.logE(TAG, "Exception during job cancellation: $message - ${ex.message}", ex)
        }
    }

    override fun onCleared() {
        super.onCleared()
        TimberLogger.logI(TAG, "ViewModel cleared. Cancelling focused and neighbor jobs.")
        focusedPageJob?.cancelJob("ViewModel cleared")
        neighborPageJobs.values.forEach { it.cancelJob("ViewModel cleared") }
        neighborPageJobs.clear()
    }
}
