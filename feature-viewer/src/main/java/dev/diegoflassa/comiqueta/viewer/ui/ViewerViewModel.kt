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
    private val getComicInfo: IGetComicInfoUseCase,
    private val decodeComicPage: IDecodeComicPageUseCase,
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
    private val _pagesToPreloadLogic =
        MutableStateFlow(DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD)


    private var focusedPageJob: Job? = null
    private val neighborPageJobs = mutableMapOf<Int, Job>()

    private val viewerPagesToPreloadAhead: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.VIEWER_PAGES_TO_PRELOAD_AHEAD]
                ?: DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD
        }

    companion object {
        private const val MIN_PRELOAD_COUNT_LOGIC = 0
        private const val MAX_PRELOAD_COUNT_LOGIC = 5
        private const val MAX_SETTING_FOR_CACHE_INIT = 10
        const val DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD = 1
    }

    init {
        viewModelScope.launch {
            // Step 1: Initialize Cache based on the *initial* setting value
            val initialSettingValue = try {
                viewerPagesToPreloadAhead.first()
            } catch (ex: Exception) {
                ex.printStackTrace()
                TimberLogger.logE(
                    "ViewerViewModel",
                    "Failed to get initial preload count, using default.",
                    ex
                )
                PreferencesKeys.DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD
            }

            val cacheInitPreloadCount = initialSettingValue
                .coerceAtLeast(MIN_PRELOAD_COUNT_LOGIC)
                .coerceAtMost(MAX_SETTING_FOR_CACHE_INIT)
            pageBitmapCache = LruCache(1 + 2 * cacheInitPreloadCount)
            TimberLogger.logI(
                "ViewerViewModel",
                "Cache initialized. Capacity based on setting value: $initialSettingValue (used: $cacheInitPreloadCount)"
            )

            // Step 2: Set initial logic preload and then observe changes
            val initialLogicPreload = initialSettingValue
                .coerceAtLeast(MIN_PRELOAD_COUNT_LOGIC)
                .coerceAtMost(MAX_PRELOAD_COUNT_LOGIC)
            _pagesToPreloadLogic.value = initialLogicPreload
            TimberLogger.logI(
                "ViewerViewModel",
                "Initial logic preload count set to: $initialLogicPreload"
            )

            viewerPagesToPreloadAhead
                .catch { e ->
                    TimberLogger.logE(
                        "ViewerViewModel",
                        "Error observing viewerPagesToPreloadAhead",
                        e
                    )
                    // Revert to a safe default if the flow errors
                    if (_pagesToPreloadLogic.value != PreferencesKeys.DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD) {
                        _pagesToPreloadLogic.value =
                            PreferencesKeys.DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD
                                .coerceAtLeast(MIN_PRELOAD_COUNT_LOGIC)
                                .coerceAtMost(MAX_PRELOAD_COUNT_LOGIC)
                        triggerNeighborReloadIfNeeded()
                    }
                }
                .collect { newSettingValue ->
                    val newLogicPreload = newSettingValue
                        .coerceAtLeast(MIN_PRELOAD_COUNT_LOGIC)
                        .coerceAtMost(MAX_PRELOAD_COUNT_LOGIC)
                    if (_pagesToPreloadLogic.value != newLogicPreload) {
                        TimberLogger.logI(
                            "ViewerViewModel",
                            "Preload setting changed. From Config: $newSettingValue, Applied for Logic: $newLogicPreload"
                        )
                        _pagesToPreloadLogic.value = newLogicPreload
                        triggerNeighborReloadIfNeeded()
                    }
                }
        }
    }

    private fun triggerNeighborReloadIfNeeded() {
        if (!::pageBitmapCache.isInitialized) {
            TimberLogger.logW(
                "ViewerViewModel",
                "triggerNeighborReloadIfNeeded called but cache not ready."
            )
            return
        }
        if (uiState.value.focusedBitmap != null && comicPageIdentifiers.isNotEmpty()) {
            TimberLogger.logD(
                "ViewerViewModel",
                "Preload setting changed, re-evaluating neighbors for page ${uiState.value.currentPage}"
            )
            // No need to cancel all jobs here, loadNeighborPagesAsync will handle out-of-scope ones.
            loadNeighborPagesAsync(uiState.value.currentPage)
        }
    }

    open fun reduce(intent: ViewerIntent) {
        TimberLogger.logI("ViewerViewModel", "Reducing intent: $intent")
        when (intent) {
            is ViewerIntent.LoadComic -> handleLoadComic(intent.uriString.toUri())
            is ViewerIntent.GoToPage -> handleGoToPage(intent.pageNumber)
            is ViewerIntent.NavigateNextPage -> {
                val nextPage = uiState.value.currentPage + 1
                handleGoToPage(nextPage)
            }

            is ViewerIntent.NavigatePreviousPage -> {
                val prevPage = uiState.value.currentPage - 1
                handleGoToPage(prevPage)
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
                    currentPage = 0, pageCount = 0
                )
            }
            currentComicUri = uri
            if (::pageBitmapCache.isInitialized) {
                pageBitmapCache.evictAll()
            } else {
                // This is a fallback. Init block should have initialized it.
                // If hit, log an error, and create a default cache to avoid crashing.
                TimberLogger.logE(
                    "ViewerViewModel",
                    "CRITICAL: Cache accessed in handleLoadComic before init completed!"
                )
                val fallbackPreload =
                    _pagesToPreloadLogic.value.coerceAtMost(MAX_SETTING_FOR_CACHE_INIT)
                pageBitmapCache = LruCache(1 + 2 * fallbackPreload)
            }
            comicPageIdentifiers = emptyList()

            focusedPageJob?.cancelJob()
            cancelAndClearNeighborJobs() // Clear all old jobs

            try {
                TimberLogger.logI("ViewerViewModel", "LoadComic: Starting for $uri")
                val comicInfo = getComicInfo(currentComicUri!!)
                comicPageIdentifiers = comicInfo.pageIdentifiers
                currentComicFileType = comicInfo.fileType

                _uiState.update {
                    it.copy(
                        comicTitle = comicInfo.title, pageCount = comicInfo.pageCount,
                        fileType = comicInfo.fileType
                    )
                }

                if (comicInfo.pageCount > 0 && comicInfo.pageIdentifiers.isNotEmpty()) {
                    loadFocusedAndNeighborPages(0)
                } else {
                    TimberLogger.logW("ViewerViewModel", "LoadComic: Comic has no pages.")
                    _effect.send(ViewerEffect.ShowError("Comic has no pages or is empty."))
                    _uiState.update { it.copy(isLoadingFocused = false) }
                }
            } catch (cex: CancellationException) {
                cex.printStackTrace()
                TimberLogger.logE(
                    "ViewerViewModel",
                    "LoadComic (getComicInfo) cancelled: ${cex.message}",
                    cex
                )
                _uiState.update { it.copy(isLoadingFocused = false) }
            } catch (ex: Exception) {
                ex.printStackTrace()
                TimberLogger.logE(
                    "ViewerViewModel",
                    "LoadComic (getComicInfo): Error loading comic $uri",
                    ex
                )
                val errorMessage = ex.localizedMessage ?: "Failed to load comic"
                _uiState.update { it.copy(isLoadingFocused = false, error = errorMessage) }
                _effect.send(ViewerEffect.ShowError(errorMessage))
            }
        }
    }

    private fun handleGoToPage(pageNumber: Int) {
        TimberLogger.logI(
            "ViewerViewModel",
            "handleGoToPage: targetPage=$pageNumber, currentViewModelPage=${uiState.value.currentPage}, pageCount=${uiState.value.pageCount}"
        )
        if (pageNumber >= 0 && pageNumber < uiState.value.pageCount && pageNumber < comicPageIdentifiers.size) {
            if (uiState.value.currentPage == pageNumber && uiState.value.focusedBitmap != null) {
                TimberLogger.logI(
                    "ViewerViewModel",
                    "handleGoToPage: Already on page $pageNumber. Ensuring neighbors for current setting."
                )
                loadNeighborPagesAsync(pageNumber) // Re-evaluate neighbors if setting changed
                return
            }
            loadFocusedAndNeighborPages(pageNumber)
        } else {
            TimberLogger.logW("ViewerViewModel", "handleGoToPage: Invalid pageNumber $pageNumber.")
            if (pageNumber < 0 || pageNumber >= uiState.value.pageCount) {
                viewModelScope.launch { _effect.send(ViewerEffect.ShowError("Invalid page number: ${pageNumber + 1}")) }
            }
        }
    }

    private fun loadFocusedAndNeighborPages(targetPageIndex: Int) {
        if (!::pageBitmapCache.isInitialized) {
            TimberLogger.logE(
                "ViewerViewModel",
                "loadFocusedAndNeighborPages: Cache not ready. Aborting."
            )
            _uiState.update {
                it.copy(
                    isLoadingFocused = false,
                    error = "Internal error: Viewer not ready."
                )
            }
            return
        }
        val currentLogicPreload = _pagesToPreloadLogic.value
        TimberLogger.logD(
            "ViewerViewModel",
            "loadFocusedAndNeighborPages for index: $targetPageIndex, with logic preload: $currentLogicPreload"
        )

        focusedPageJob?.cancelJob()
        // cancelAndClearNeighborJobs() // Let loadNeighborPagesAsync handle more precise job/UI state cleanup

        val cachedFocusedBitmap = pageBitmapCache.get(targetPageIndex)

        _uiState.update { currentState ->
            currentState.copy(
                currentPage = targetPageIndex,
                isLoadingFocused = cachedFocusedBitmap == null,
                focusedBitmap = cachedFocusedBitmap,
                neighborBitmaps = buildMap { // Populate initially from cache based on current logic
                    if (currentLogicPreload > MIN_PRELOAD_COUNT_LOGIC) {
                        for (i in 1..currentLogicPreload) {
                            val prevIdx = targetPageIndex - i
                            val nextIdx = targetPageIndex + i
                            if (prevIdx >= 0) pageBitmapCache.get(prevIdx)?.let { put(prevIdx, it) }
                            if (nextIdx < currentState.pageCount) pageBitmapCache.get(nextIdx)
                                ?.let { put(nextIdx, it) }
                        }
                    }
                }
            )
        }

        if (cachedFocusedBitmap == null) {
            focusedPageJob = viewModelScope.launch {
                try {
                    TimberLogger.logD("ViewerViewModel", "Loading focused page: $targetPageIndex")
                    val bitmap = loadPageBitmapInternal(targetPageIndex)
                    if (isActive) {
                        _uiState.update { state ->
                            if (state.currentPage == targetPageIndex) {
                                state.copy(
                                    focusedBitmap = bitmap, isLoadingFocused = false,
                                    error = if (bitmap == null) "Failed to load page ${targetPageIndex + 1}" else null
                                )
                            } else state
                        }
                        if (bitmap == null && isActive && uiState.value.currentPage == targetPageIndex) {
                            _effect.send(ViewerEffect.ShowError("Failed to load page ${targetPageIndex + 1}"))
                        }
                    }
                } catch (cex: CancellationException) {
                    cex.printStackTrace()
                    TimberLogger.logE(
                        "ViewerViewModel",
                        "Focused page $targetPageIndex loading cancelled.",
                        cex
                    )
                    if (isActive && uiState.value.currentPage == targetPageIndex) _uiState.update {
                        it.copy(
                            isLoadingFocused = false
                        )
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    TimberLogger.logE(
                        "ViewerViewModel",
                        "Error loading focused page $targetPageIndex",
                        ex
                    )
                    if (isActive && uiState.value.currentPage == targetPageIndex) {
                        val errorMsg =
                            ex.localizedMessage ?: "Error loading page ${targetPageIndex + 1}"
                        _uiState.update { it.copy(isLoadingFocused = false, error = errorMsg) }
                        _effect.send(ViewerEffect.ShowError(errorMsg))
                    }
                }
            }
        }
        loadNeighborPagesAsync(targetPageIndex)
    }

    private fun loadNeighborPagesAsync(focusedPageIndex: Int) {
        if (!::pageBitmapCache.isInitialized) {
            TimberLogger.logE(
                "ViewerViewModel",
                "loadNeighborPagesAsync: Cache not ready. Aborting."
            )
            return
        }
        val currentLogicPreload = _pagesToPreloadLogic.value
        TimberLogger.logD(
            "ViewerViewModel",
            "Loading neighbors for page: $focusedPageIndex, Effective Preload Logic: $currentLogicPreload"
        )

        // Determine valid range for current preload setting
        val validPreloadIndices = mutableSetOf<Int>()
        if (currentLogicPreload > MIN_PRELOAD_COUNT_LOGIC) {
            for (offset in 1..currentLogicPreload) {
                if (focusedPageIndex - offset >= 0) validPreloadIndices.add(focusedPageIndex - offset)
                if (focusedPageIndex + offset < uiState.value.pageCount) validPreloadIndices.add(
                    focusedPageIndex + offset
                )
            }
        }

        // Cancel jobs for neighbors no longer in scope
        val jobsToRemove = mutableListOf<Int>()
        neighborPageJobs.forEach { (index, job) ->
            if (!validPreloadIndices.contains(index)) {
                TimberLogger.logD(
                    "ViewerViewModel",
                    "Neighbor job for page $index is out of scope. Cancelling."
                )
                job.cancelJob()
                jobsToRemove.add(index)
            }
        }
        jobsToRemove.forEach { neighborPageJobs.remove(it) }

        // Remove bitmaps from UI state for neighbors no longer in scope
        _uiState.update { currentState ->
            val currentNeighbors = currentState.neighborBitmaps
            val updatedNeighbors = currentNeighbors.filterKeys { validPreloadIndices.contains(it) }
            if (updatedNeighbors.size != currentNeighbors.size) {
                currentState.copy(neighborBitmaps = updatedNeighbors)
            } else {
                currentState
            }
        }

        if (currentLogicPreload == MIN_PRELOAD_COUNT_LOGIC) {
            TimberLogger.logD("ViewerViewModel", "Preload count is 0. No new neighbor loading.")
            return
        }

        for (offset in 1..currentLogicPreload) {
            listOf(focusedPageIndex - offset, focusedPageIndex + offset).forEach { neighborIndex ->
                if (neighborIndex >= 0 && neighborIndex < comicPageIdentifiers.size &&
                    pageBitmapCache.get(neighborIndex) == null &&
                    neighborPageJobs[neighborIndex]?.isActive != true
                ) {
                    neighborPageJobs[neighborIndex] = viewModelScope.launch {
                        TimberLogger.logD(
                            "ViewerViewModel",
                            "Pre-loading neighbor: $neighborIndex (offset for $focusedPageIndex)"
                        )
                        try {
                            val bitmap = loadPageBitmapInternal(neighborIndex)
                            if (isActive && bitmap != null) {
                                _uiState.update {
                                    // Re-check relevance against current page and *current* preload setting
                                    val latestPreloadLogic = _pagesToPreloadLogic.value
                                    if (abs(it.currentPage - neighborIndex) <= latestPreloadLogic && latestPreloadLogic > 0) {
                                        it.copy(neighborBitmaps = it.neighborBitmaps + (neighborIndex to bitmap))
                                    } else {
                                        TimberLogger.logD(
                                            "ViewerViewModel",
                                            "Loaded neighbor $neighborIndex but it's no longer relevant for current page ${it.currentPage} with preload $latestPreloadLogic"
                                        )
                                        it
                                    }
                                }
                            }
                        } catch (cex: CancellationException) {
                            cex.printStackTrace()
                            TimberLogger.logE(
                                "ViewerViewModel",
                                "Neighbor $neighborIndex loading cancelled.",
                                cex
                            )
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            TimberLogger.logE(
                                "ViewerViewModel",
                                "Error pre-loading neighbor $neighborIndex",
                                ex
                            )
                        } finally {
                            neighborPageJobs.remove(neighborIndex)
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadPageBitmapInternal(pageIndex: Int): ImageBitmap? {
        if (!::pageBitmapCache.isInitialized) {
            TimberLogger.logE(
                "ViewerViewModel",
                "loadPageBitmapInternal: Cache not ready for page $pageIndex!"
            )
            throw IllegalStateException("Cache not initialized when trying to load page $pageIndex")
        }
        if (currentComicUri == null || currentComicFileType == null) {
            TimberLogger.logW(
                "ViewerViewModel",
                "loadPageBitmapInternal($pageIndex): Aborted. Comic data not ready."
            )
            throw IllegalStateException("Comic data (URI or FileType) not ready for page $pageIndex")
        }
        if (comicPageIdentifiers.isEmpty() || pageIndex < 0 || pageIndex >= comicPageIdentifiers.size) {
            TimberLogger.logE(
                "ViewerViewModel",
                "loadPageBitmapInternal($pageIndex): Aborted. PageIndex out of bounds (0-${comicPageIdentifiers.size - 1})."
            )
            throw IndexOutOfBoundsException("Page index $pageIndex out of bounds for ${comicPageIdentifiers.size} pages")
        }

        pageBitmapCache.get(pageIndex)?.let {
            TimberLogger.logD("ViewerViewModel", "Page $pageIndex found in cache.")
            return it
        }

        TimberLogger.logI("ViewerViewModel", "Page $pageIndex not in cache. Decoding.")
        val pageIdentifier = comicPageIdentifiers[pageIndex]
        return try {
            val bitmap = decodeComicPage(
                pageIndex,
                pageIdentifier,
                currentComicUri!!,
                currentComicFileType!!
            )
            bitmap?.also {
                TimberLogger.logI(
                    "ViewerViewModel",
                    "Successfully decoded page $pageIndex. Caching."
                )
                if (::pageBitmapCache.isInitialized) pageBitmapCache.put(pageIndex, it)
            } ?: run {
                TimberLogger.logW(
                    "ViewerViewModel",
                    "decodeComicPageUseCase returned null for page $pageIndex."
                )
                null
            }
        } catch (cex: CancellationException) {
            TimberLogger.logE("ViewerViewModel", "Decoding cancelled for page $pageIndex.", cex)
            throw cex
        } catch (ex: Exception) {
            TimberLogger.logE("ViewerViewModel", "Error during decodeComicPage for $pageIndex", ex)
            throw ex
        }
    }

    private fun Job.cancelJob() {
        try {
            if (this.isActive) this.cancel()
        } catch (ex: Exception) {
            ex.printStackTrace()
            TimberLogger.logE(
                "ViewerViewModel",
                "Exception during job cancellation: ${ex.message}",
                ex
            )
        }
    }

    private fun cancelAndClearNeighborJobs() {
        neighborPageJobs.values.forEach { it.cancelJob() }
        neighborPageJobs.clear()
    }
}
