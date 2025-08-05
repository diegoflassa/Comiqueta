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
// Import the INTERFACES
import dev.diegoflassa.comiqueta.viewer.domain.usecase.IDecodeComicPageUseCase
import dev.diegoflassa.comiqueta.viewer.domain.usecase.IGetComicInfoUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
open class ViewerViewModel @Inject constructor(
    // Use INTERFACES in the constructor
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
    private var currentLoadingJob: Job? = null
    private var preloadingPagesJob: Job? = null

    open fun reduce(intent: ViewerIntent) {
        when (intent) {
            is ViewerIntent.LoadComic -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true, error = null, comicTitle = "") }
                    currentComicUri = intent.uriString.toUri()
                    pageBitmapCache.evictAll()
                    comicPageIdentifiers = emptyList()
                    _uiState.update { it.copy(currentPage = 0, pageCount = 0, currentBitmap = null) }

                    currentLoadingJob?.cancel()
                    preloadingPagesJob?.cancel()

                    try {
                        TimberLogger.logI("ViewerViewModel", "LoadComic: Starting for ${intent.uriString}")
                        // Call the interface method
                        val comicInfo = getComicInfo(currentComicUri!!)

                        comicPageIdentifiers = comicInfo.pageIdentifiers
                        currentComicFileType = comicInfo.fileType

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                comicTitle = comicInfo.title,
                                pageCount = comicInfo.pageCount,
                                fileType = comicInfo.fileType
                            )
                        }
                        if (comicInfo.pageCount > 0 && comicInfo.pageIdentifiers.isNotEmpty()) {
                            loadPageBitmap(0, isCurrentPage = true)
                        } else {
                            TimberLogger.logW("ViewerViewModel", "LoadComic: Comic has no pages or identifiers. PageCount: ${comicInfo.pageCount}, IdentifiersEmpty: ${comicInfo.pageIdentifiers.isEmpty()}")
                            _effect.send(ViewerEffect.ShowError("Comic has no pages or is empty."))
                            _uiState.update { it.copy(isLoading = false) }
                        }
                    } catch (e: Exception) {
                        TimberLogger.logE("ViewerViewModel", "LoadComic: Error loading comic ${intent.uriString}", e)
                        _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to load comic") }
                        _effect.send(ViewerEffect.ShowError(e.localizedMessage ?: "Failed to load comic"))
                    }
                }
            }

            is ViewerIntent.GoToPage -> {
                TimberLogger.logI("ViewerViewModel", "GoToPage intent: targetPage=${intent.pageNumber}, currentViewModelPage=${uiState.value.currentPage}, pageCount=${uiState.value.pageCount}, loadedIdentifiersCount=${comicPageIdentifiers.size}")
                if (intent.pageNumber >= 0 && intent.pageNumber < uiState.value.pageCount && intent.pageNumber < comicPageIdentifiers.size) {
                    _uiState.update { it.copy(currentPage = intent.pageNumber) }
                    loadPageBitmap(intent.pageNumber, isCurrentPage = true)
                } else {
                    TimberLogger.logW("ViewerViewModel", "GoToPage intent: Invalid pageNumber ${intent.pageNumber}. ViewModel PageCount: ${uiState.value.pageCount}, Loaded IdentifiersCount: ${comicPageIdentifiers.size}")
                    viewModelScope.launch { _effect.send(ViewerEffect.ShowError("Invalid page number: ${intent.pageNumber + 1}"))}
                }
            }

            is ViewerIntent.NavigateNextPage -> {
                val nextPage = uiState.value.currentPage + 1
                TimberLogger.logI("ViewerViewModel", "NavigateNextPage intent: trying to go to $nextPage from ${uiState.value.currentPage}")
                reduce(ViewerIntent.GoToPage(nextPage))
            }

            is ViewerIntent.NavigatePreviousPage -> {
                val prevPage = uiState.value.currentPage - 1
                TimberLogger.logI("ViewerViewModel", "NavigatePreviousPage intent: trying to go to $prevPage from ${uiState.value.currentPage}")
                reduce(ViewerIntent.GoToPage(prevPage))
            }

            is ViewerIntent.ToggleUiVisibility -> {
                _uiState.update { it.copy(isUiVisible = !it.isUiVisible) }
            }
            is ViewerIntent.ErrorShown -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun loadPageBitmap(pageIndex: Int, isCurrentPage: Boolean) {
        if (currentComicUri == null || currentComicFileType == null || comicPageIdentifiers.isEmpty()) {
            TimberLogger.logW("ViewerViewModel", "loadPageBitmap: Aborted. Comic data not ready. PageIndex: $pageIndex, HasIdentifiers: ${!comicPageIdentifiers.isEmpty()}")
            if(isCurrentPage){
                _uiState.update { it.copy(isLoading = false, currentBitmap = null, error = "Cannot load page: Comic data not ready") }
            }
            return
        }
        if (pageIndex < 0 || pageIndex >= comicPageIdentifiers.size) {
            TimberLogger.logE("ViewerViewModel", "loadPageBitmap: Aborted. PageIndex $pageIndex is out of bounds for comicPageIdentifiers size ${comicPageIdentifiers.size}.")
            if(isCurrentPage){
                _uiState.update { it.copy(isLoading = false, currentBitmap = null, error = "Page number out of bounds") }
                viewModelScope.launch { _effect.send(ViewerEffect.ShowError("Page number out of bounds: ${pageIndex + 1}")) }
            }
            return
        }

        val cachedBitmap = pageBitmapCache.get(pageIndex)
        if (cachedBitmap != null) {
            TimberLogger.logD("ViewerViewModel", "loadPageBitmap: Page $pageIndex found in cache.")
            if (isCurrentPage) {
                _uiState.update { it.copy(currentBitmap = cachedBitmap, isLoading = false, error = null) }
                preloadNextPages(pageIndex)
            }
            return
        }

        if (isCurrentPage) {
            currentLoadingJob?.cancel()
            currentLoadingJob = viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val pageIdentifier = comicPageIdentifiers[pageIndex]
                TimberLogger.logI("ViewerViewModel", "CurrentPageLoad: Launching for pageIndex=$pageIndex, identifier='$pageIdentifier'")
                try {
                    // Call the interface method
                    val bitmap = decodeComicPage(pageIndex, pageIdentifier, currentComicUri!!, currentComicFileType!!)
                    if (isActive) {
                        if (bitmap != null) {
                            TimberLogger.logI("ViewerViewModel", "CurrentPageLoad: Successfully decoded page $pageIndex.")
                            pageBitmapCache.put(pageIndex, bitmap)
                            if (uiState.value.currentPage == pageIndex) {
                                _uiState.update { it.copy(currentBitmap = bitmap, isLoading = false) }
                            }
                        } else {
                            TimberLogger.logW("ViewerViewModel", "CurrentPageLoad: decodeComicPageUseCase returned null for page $pageIndex ('$pageIdentifier').")
                            throw IOException("Failed to decode page $pageIndex ('$pageIdentifier'). Use case returned null.")
                        }
                    }
                    preloadNextPages(pageIndex)
                } catch (e: CancellationException) {
                    TimberLogger.logI("ViewerViewModel", "CurrentPageLoad: Job cancelled for page $pageIndex: ${e.message}")
                } catch (e: Exception) {
                    TimberLogger.logE("ViewerViewModel", "CurrentPageLoad: Error loading page $pageIndex (Identifier: '$pageIdentifier')", e)
                    if (isActive && uiState.value.currentPage == pageIndex) {
                        val errorMessage = e.localizedMessage ?: "Failed to load page ${pageIndex + 1}"
                        _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                        _effect.send(ViewerEffect.ShowError(errorMessage))
                    }
                }
            }
        } else { // Preloading
            viewModelScope.launch {
                val pageIdentifier = comicPageIdentifiers[pageIndex]
                TimberLogger.logI("ViewerViewModel", "PreloadPage-Launch: pageIndex=$pageIndex, identifier='$pageIdentifier'")
                try {
                    // Call the interface method
                    val bitmap = decodeComicPage(pageIndex, pageIdentifier, currentComicUri!!, currentComicFileType!!)
                    if (isActive && bitmap != null) {
                        pageBitmapCache.put(pageIndex, bitmap)
                        TimberLogger.logD("ViewerViewModel", "PreloadPage-Launch: Successfully preloaded/cached page $pageIndex")
                    } else if (isActive && bitmap == null) {
                        TimberLogger.logW("ViewerViewModel", "PreloadPage-Launch: decodeComicPageUseCase returned null for page $pageIndex")
                    }
                } catch (e: CancellationException) {
                    TimberLogger.logI("ViewerViewModel", "PreloadPage-Launch: Job cancelled for page $pageIndex: ${e.message}")
                } catch (e: Exception) {
                    TimberLogger.logE("ViewerViewModel", "PreloadPage-Launch: Error preloading page $pageIndex (Identifier: '$pageIdentifier')", e)
                }
            }
        }
    }

    private fun preloadNextPages(currentPageIndex: Int) {
        if (preloadingPagesJob != null && preloadingPagesJob!!.isActive) {
            TimberLogger.logD("ViewerViewModel", "preloadNextPages: Preloading cycle already active. Skipping.")
            return
        }

        val nextPageIndexToStart = currentPageIndex + 1
        val preloadUpTo = minOf(currentPageIndex + 3, comicPageIdentifiers.size - 1)

        if (nextPageIndexToStart <= preloadUpTo && nextPageIndexToStart < comicPageIdentifiers.size) {
            preloadingPagesJob = viewModelScope.launch {
                TimberLogger.logD("ViewerViewModel", "preloadNextPages: Starting cycle from $nextPageIndexToStart to $preloadUpTo")
                for (i in nextPageIndexToStart..preloadUpTo) {
                    if (!isActive) {
                        TimberLogger.logD("ViewerViewModel", "preloadNextPages: Cycle cancelled at page $i.")
                        break
                    }
                    if (pageBitmapCache.get(i) == null) {
                        TimberLogger.logD("ViewerViewModel", "preloadNextPages: Requesting preload for page $i via loadPageBitmap.")
                        loadPageBitmap(i, isCurrentPage = false)
                        delay(150) // Small delay to allow current page loading/rendering to take priority
                    } else {
                        TimberLogger.logD("ViewerViewModel", "preloadNextPages: Page $i already in cache. Skipping.")
                    }
                }
                TimberLogger.logD("ViewerViewModel", "preloadNextPages: Cycle finished.")
            }
        }
    }
}
