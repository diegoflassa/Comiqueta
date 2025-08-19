package dev.diegoflassa.comiqueta.viewer.ui

import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val decodeComicPage: IDecodeComicPageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUIState())
    open val uiState: StateFlow<ViewerUIState> = _uiState.asStateFlow()

    private val _effect = Channel<ViewerEffect>(Channel.BUFFERED)
    open val effect: Flow<ViewerEffect> = _effect.receiveAsFlow()

    private var comicPageIdentifiers: List<String> = emptyList()
    private var currentComicUri: Uri? = null
    private var currentComicFileType: ComicFileType? = null

    private val pageBitmapCache = LruCache<Int, ImageBitmap>(5)

    private var focusedPageJob: Job? = null
    private var prevPageJob: Job? = null
    private var nextPageJob: Job? = null

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
                    isLoadingFocused = true,
                    error = null,
                    comicTitle = "",
                    focusedBitmap = null,
                    neighborBitmaps = emptyMap(),
                    currentPage = 0,
                    pageCount = 0
                )
            }
            currentComicUri = uri
            pageBitmapCache.evictAll()
            comicPageIdentifiers = emptyList()

            focusedPageJob?.cancelAndJoinSilently()
            prevPageJob?.cancelAndJoinSilently()
            nextPageJob?.cancelAndJoinSilently()

            try {
                TimberLogger.logI("ViewerViewModel", "LoadComic: Starting for $uri")
                val comicInfo = getComicInfo(currentComicUri!!)
                comicPageIdentifiers = comicInfo.pageIdentifiers
                currentComicFileType = comicInfo.fileType

                _uiState.update {
                    it.copy(
                        comicTitle = comicInfo.title,
                        pageCount = comicInfo.pageCount,
                        fileType = comicInfo.fileType,
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
                TimberLogger.logI("ViewerViewModel", "LoadComic cancelled: ${cex.message}")
                _uiState.update { it.copy(isLoadingFocused = false) }
            } catch (ex: Exception) {
                ex.printStackTrace()
                TimberLogger.logE("ViewerViewModel", "LoadComic: Error loading comic $uri", ex)
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
                    "handleGoToPage: Already on page $pageNumber with bitmap. Skipping redundant load."
                )
                // If we are already on the page and bitmap is loaded, ensure neighbors are considered
                loadNeighborPagesAsync(pageNumber)
                return
            }
            loadFocusedAndNeighborPages(pageNumber)
        } else {
            TimberLogger.logW("ViewerViewModel", "handleGoToPage: Invalid pageNumber $pageNumber.")
            // Avoid sending error if it's just a slight overscroll in pager that self-corrects
            if (pageNumber < 0 || pageNumber >= uiState.value.pageCount) {
                viewModelScope.launch { _effect.send(ViewerEffect.ShowError("Invalid page number: ${pageNumber + 1}")) }
            }
        }
    }

    private fun loadFocusedAndNeighborPages(targetPageIndex: Int) {
        TimberLogger.logD(
            "ViewerViewModel",
            "loadFocusedAndNeighborPages for index: $targetPageIndex"
        )

        focusedPageJob?.cancelAndJoinSilently()
        // We might want to be more selective about cancelling neighbor jobs
        // if the target page is one of the existing neighbors.
        // For simplicity now, cancel all.
        prevPageJob?.cancelAndJoinSilently()
        nextPageJob?.cancelAndJoinSilently()

        val cachedFocusedBitmap = pageBitmapCache.get(targetPageIndex)

        _uiState.update {
            it.copy(
                currentPage = targetPageIndex,
                isLoadingFocused = cachedFocusedBitmap == null,
                focusedBitmap = cachedFocusedBitmap,
                // Initialize/clear neighborBitmaps for the new context
                neighborBitmaps = buildMap {
                    val prev = targetPageIndex - 1
                    val next = targetPageIndex + 1
                    if (prev >= 0) pageBitmapCache.get(prev)?.let { bmp -> put(prev, bmp) }
                    if (next < it.pageCount) pageBitmapCache.get(next)
                        ?.let { bmp -> put(next, bmp) }
                }
            )
        }

        // Load focused page if not from cache
        if (cachedFocusedBitmap == null) {
            focusedPageJob = viewModelScope.launch {
                try {
                    TimberLogger.logD("ViewerViewModel", "Loading focused page: $targetPageIndex")
                    val bitmap =
                        loadPageBitmapInternal(targetPageIndex)
                    if (isActive) {
                        _uiState.update {
                            if (it.currentPage == targetPageIndex) {
                                it.copy(
                                    focusedBitmap = bitmap,
                                    isLoadingFocused = false,
                                    error = if (bitmap == null) "Failed to load page ${targetPageIndex + 1}" else null
                                )
                            } else {
                                it
                            }
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
                    // isLoadingFocused might need to be reset if cancellation wasn't due to page change
                    if (isActive && uiState.value.currentPage == targetPageIndex) {
                        _uiState.update { it.copy(isLoadingFocused = false) }
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
        // After focused page is handled (or started), trigger neighbor loading
        loadNeighborPagesAsync(targetPageIndex)
    }

    private fun loadNeighborPagesAsync(focusedPageIndex: Int) {
        // Load previous page (neighbor)
        val prevPageIndex = focusedPageIndex - 1
        if (prevPageIndex >= 0 && prevPageIndex < comicPageIdentifiers.size && pageBitmapCache.get(
                prevPageIndex
            ) == null
        ) {
            prevPageJob?.cancelAndJoinSilently()
            prevPageJob = viewModelScope.launch {
                TimberLogger.logD("ViewerViewModel", "Pre-loading previous page: $prevPageIndex")
                try {
                    val bitmap = loadPageBitmapInternal(prevPageIndex)
                    if (isActive && bitmap != null) {
                        _uiState.update {
                            // Only update if it's still a relevant neighbor
                            if (abs(it.currentPage - prevPageIndex) == 1) {
                                it.copy(neighborBitmaps = it.neighborBitmaps + (prevPageIndex to bitmap))
                            } else it
                        }
                    }
                } catch (cex: CancellationException) {
                    cex.printStackTrace()
                    TimberLogger.logE(
                        "ViewerViewModel",
                        "Prev page $prevPageIndex loading cancelled.",
                        cex
                    )
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    TimberLogger.logE(
                        "ViewerViewModel",
                        "Error pre-loading prev page $prevPageIndex",
                        ex
                    )
                }
            }
        }

        // Load next page (neighbor)
        val nextPageIndex = focusedPageIndex + 1
        if (nextPageIndex >= 0 && nextPageIndex < comicPageIdentifiers.size && pageBitmapCache.get(
                nextPageIndex
            ) == null
        ) {
            nextPageJob?.cancelAndJoinSilently()
            nextPageJob = viewModelScope.launch {
                TimberLogger.logD("ViewerViewModel", "Pre-loading next page: $nextPageIndex")
                try {
                    val bitmap = loadPageBitmapInternal(nextPageIndex)
                    if (isActive && bitmap != null) {
                        _uiState.update {
                            // Only update if it's still a relevant neighbor
                            if (abs(it.currentPage - nextPageIndex) == 1) {
                                it.copy(neighborBitmaps = it.neighborBitmaps + (nextPageIndex to bitmap))
                            } else it
                        }
                    }
                } catch (cex: CancellationException) {
                    cex.printStackTrace()
                    TimberLogger.logE(
                        "ViewerViewModel",
                        "Next page $nextPageIndex loading cancelled.",
                        cex
                    )
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    TimberLogger.logE(
                        "ViewerViewModel",
                        "Error pre-loading next page $nextPageIndex",
                        ex
                    )
                }
            }
        }
    }

    private suspend fun loadPageBitmapInternal(pageIndex: Int): ImageBitmap? {
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
                pageBitmapCache.put(pageIndex, it)
            } ?: run {
                TimberLogger.logW(
                    "ViewerViewModel",
                    "decodeComicPageUseCase returned null for page $pageIndex."
                )
                null
            }
        } catch (cex: CancellationException) {
            cex.printStackTrace()
            TimberLogger.logE("ViewerViewModel", "Decoding cancelled for page $pageIndex", cex)
            throw cex
        } catch (ex: Exception) {
            ex.printStackTrace()
            TimberLogger.logE("ViewerViewModel", "Error during decodeComicPage for $pageIndex", ex)
            throw ex
        }
    }

    private fun Job.cancelAndJoinSilently() {
        viewModelScope.launch {
            try {
                this@cancelAndJoinSilently.cancel()
            } catch (ex: Exception) {
                ex.printStackTrace()
                TimberLogger.logE(
                    "ViewerViewModel",
                    "Exception during job cancel/join: ${ex.message}",
                    ex
                )
            }
        }
    }
}
