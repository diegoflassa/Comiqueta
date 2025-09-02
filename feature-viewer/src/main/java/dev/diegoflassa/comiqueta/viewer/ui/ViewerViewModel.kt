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
    private val pageLoadJobs = mutableMapOf<Int, Job>()

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
            val cacheSize = 1 + 2 * cacheInitPreloadCount // Current + (preload * 2 sides)
            pageBitmapCache = LruCache(cacheSize.coerceAtLeast(1))
            TimberLogger.logI(
                TAG,
                "Cache initialized. Capacity: $cacheSize (based on setting value: $initialSettingValue, used for cache calc: $cacheInitPreloadCount)"
            )

            val initialLogicPreload = initialSettingValue
                .coerceAtLeast(MIN_PRELOAD_COUNT_LOGIC)
                .coerceAtMost(MAX_PRELOAD_COUNT_LOGIC)
            _pagesToPreloadLogic.value = initialLogicPreload
            _uiState.update { it.copy(pagesToPreloadLogic = initialLogicPreload, isLoadingPage = emptySet()) }
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
                        dispatchLoadPages(uiState.value.currentPage)
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
                        dispatchLoadPages(uiState.value.currentPage)
                    }
                }
        }
    }

    open fun reduce(intent: ViewerIntent) {
        TimberLogger.logI(TAG, "Reducing intent: $intent")
        when (intent) {
            is ViewerIntent.LoadComic -> handleLoadComic(intent.uriString.toUri())
            is ViewerIntent.GoToPage -> dispatchLoadPages(intent.pageNumber)
            is ViewerIntent.NavigateNextPage -> {
                val nextPage = uiState.value.currentPage + 1
                if (nextPage < uiState.value.pageCount) {
                    dispatchLoadPages(nextPage)
                }
            }
            is ViewerIntent.NavigatePreviousPage -> {
                val prevPage = uiState.value.currentPage - 1
                if (prevPage >= 0) {
                    dispatchLoadPages(prevPage)
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
                    comicTitle = "",
                    loadedPages = emptyMap(),
                    currentPage = 0, pageCount = 0,
                    fileType = null,
                    isLoadingPage = emptySet(), // Will be populated by dispatchLoadPages
                    error = null,
                    pagesToPreloadLogic = _pagesToPreloadLogic.value // Ensure this is set early
                )
            }
            currentComicUri = uri
            if (::pageBitmapCache.isInitialized) {
                pageBitmapCache.evictAll()
            } else {
                TimberLogger.logE(TAG, "CRITICAL: Cache accessed in handleLoadComic before init completed!")
                val fallbackPreload = _pagesToPreloadLogic.value.coerceAtMost(MAX_SETTING_FOR_CACHE_INIT)
                pageBitmapCache = LruCache((1 + 2 * fallbackPreload).coerceAtLeast(1))
            }
            comicPageIdentifiers = emptyList()

            pageLoadJobs.values.forEach { it.cancelJob("New comic load requested") }
            pageLoadJobs.clear()

            try {
                TimberLogger.logI(TAG, "LoadComic: Starting for $uri")
                val comicInfo = getComicInfoUseCase(currentComicUri!!)
                comicPageIdentifiers = comicInfo.pageIdentifiers
                currentComicFileType = comicInfo.fileType

                _uiState.update {
                    it.copy(
                        comicTitle = comicInfo.title, pageCount = comicInfo.pageCount,
                        fileType = comicInfo.fileType
                        // isLoadingPage will be handled by dispatchLoadPages
                    )
                }

                if (comicInfo.pageCount > 0 && comicInfo.pageIdentifiers.isNotEmpty()) {
                    dispatchLoadPages(0) // Load initial page
                } else {
                    TimberLogger.logW(TAG, "LoadComic: Comic has no pages.")
                    _effect.send(ViewerEffect.ShowError("Comic has no pages or is empty."))
                }
            } catch (cex: CancellationException) {
                TimberLogger.logI(TAG, "LoadComic (getComicInfo) cancelled: ${cex.message}")
            } catch (ex: Exception) {
                TimberLogger.logE(TAG, "LoadComic (getComicInfo): Error loading comic $uri", ex)
                val errorMessage = ex.localizedMessage ?: "Failed to load comic"
                _uiState.update { it.copy(error = errorMessage) }
                _effect.send(ViewerEffect.ShowError(errorMessage))
            }
        }
    }

    private fun dispatchLoadPages(targetPageIndex: Int) {
        TimberLogger.logD(TAG, "dispatchLoadPages for page: $targetPageIndex")
        if (!::pageBitmapCache.isInitialized) {
            TimberLogger.logE(TAG, "dispatchLoadPages: Cache not ready. Aborting.")
            _uiState.update { it.copy(error = "Internal error: Viewer not ready.") }
            return
        }
        if (currentComicUri == null || currentComicFileType == null || comicPageIdentifiers.isEmpty()) {
            TimberLogger.logW(TAG, "dispatchLoadPages: Comic data not ready for page $targetPageIndex.")
             if (uiState.value.pageCount == 0) { // Only show error if comic hasn't loaded at all
                _uiState.update { it.copy(error = "Comic data not fully loaded.") }
            }
            return
        }
        val pageCount = uiState.value.pageCount
        if (targetPageIndex < 0 || targetPageIndex >= pageCount) {
            TimberLogger.logW(TAG, "dispatchLoadPages: Invalid targetPageIndex $targetPageIndex for pageCount $pageCount")
            viewModelScope.launch { _effect.send(ViewerEffect.ShowError("Invalid page number: ${targetPageIndex + 1}")) }
            return
        }

        val currentLogicPreload = _pagesToPreloadLogic.value
        val relevantPageIndices = mutableSetOf(targetPageIndex)
        for (offset in 1..currentLogicPreload) {
            if (targetPageIndex - offset >= 0) relevantPageIndices.add(targetPageIndex - offset)
            if (targetPageIndex + offset < pageCount) relevantPageIndices.add(targetPageIndex + offset)
        }
        TimberLogger.logD(TAG, "Relevant indices for $targetPageIndex (preload $currentLogicPreload): $relevantPageIndices")

        // Cancel jobs for pages no longer relevant
        val jobsToCancel = pageLoadJobs.filterKeys { !relevantPageIndices.contains(it) }
        jobsToCancel.forEach { (idx, job) ->
            TimberLogger.logD(TAG, "Page $idx: No longer relevant. Cancelling job.")
            job.cancelJob("Page $idx no longer relevant for target $targetPageIndex")
            pageLoadJobs.remove(idx)
        }

        val newLoadedPagesFromCache = mutableMapOf<Int, ImageBitmap>()
        val pagesThatNeedNewLoadJob = mutableSetOf<Int>()
        val activeRelevantLoadJobIndices = mutableSetOf<Int>()

        relevantPageIndices.forEach { idx ->
            pageBitmapCache.get(idx)?.let {
                newLoadedPagesFromCache[idx] = it
            } ?: run {
                if (pageLoadJobs[idx]?.isActive == true) {
                    activeRelevantLoadJobIndices.add(idx)
                } else {
                    pagesThatNeedNewLoadJob.add(idx)
                }
            }
        }

        _uiState.update { currentState ->
            val finalLoadedPages = mutableMapOf<Int, ImageBitmap?>()
            val finalIsLoadingPage = mutableSetOf<Int>()

            relevantPageIndices.forEach { idx ->
                if (newLoadedPagesFromCache.containsKey(idx)) {
                    finalLoadedPages[idx] = newLoadedPagesFromCache[idx]
                } else if (pagesThatNeedNewLoadJob.contains(idx) || activeRelevantLoadJobIndices.contains(idx)) {
                    finalIsLoadingPage.add(idx)
                }
            }
            TimberLogger.logD(TAG, "Updating UI: currentPage=$targetPageIndex, loadedPages=${finalLoadedPages.keys}, isLoadingPage=$finalIsLoadingPage")
            currentState.copy(
                currentPage = targetPageIndex,
                loadedPages = finalLoadedPages,
                isLoadingPage = finalIsLoadingPage,
                error = null
            )
        }

        pagesThatNeedNewLoadJob.forEach { pageToLoadIdx ->
            TimberLogger.logD(TAG, "Page $pageToLoadIdx: Needs new load job. Launching.")
            pageLoadJobs[pageToLoadIdx] = viewModelScope.launch {
                try {
                    val bitmap = loadPageBitmapInternal(pageToLoadIdx)
                    if (isActive && bitmap != null) {
                        _uiState.update { state ->
                            val stillRelevant = (state.currentPage - pageToLoadIdx).let { diff -> diff == 0 || (diff !=0 && kotlin.math.abs(diff) <= state.pagesToPreloadLogic) } && pageToLoadIdx < state.pageCount
                            if (stillRelevant) {
                                TimberLogger.logD(TAG, "Page $pageToLoadIdx loaded, adding to loadedPages and removing from isLoadingPage.")
                                state.copy(
                                    loadedPages = state.loadedPages + (pageToLoadIdx to bitmap),
                                    isLoadingPage = state.isLoadingPage - pageToLoadIdx
                                )
                            } else {
                                TimberLogger.logD(TAG, "Page $pageToLoadIdx loaded, but no longer relevant. Removing from isLoadingPage.")
                                state.copy(isLoadingPage = state.isLoadingPage - pageToLoadIdx)
                            }
                        }
                    } else if (isActive && bitmap == null) {
                        TimberLogger.logW(TAG, "Page $pageToLoadIdx: Load returned null. Removing from isLoadingPage.")
                        _uiState.update { it.copy(isLoadingPage = it.isLoadingPage - pageToLoadIdx) }
                    }
                } catch (cex: CancellationException) {
                    TimberLogger.logI(TAG, "Page $pageToLoadIdx loading cancelled: ${cex.message}")
                } catch (ex: Exception) {
                    TimberLogger.logE(TAG, "Error loading page $pageToLoadIdx", ex)
                } finally {
                    if (isActive) {
                         _uiState.update { it.copy(isLoadingPage = it.isLoadingPage - pageToLoadIdx) }
                    }
                    pageLoadJobs.remove(pageToLoadIdx)
                    TimberLogger.logD(TAG, "Page $pageToLoadIdx: Job finished. Removed from pageLoadJobs and ensured not in isLoadingPage.")
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
        TimberLogger.logI(TAG, "ViewModel cleared. Cancelling all page load jobs.")
        pageLoadJobs.values.forEach { it.cancelJob("ViewModel cleared") }
        pageLoadJobs.clear()
    }
}
