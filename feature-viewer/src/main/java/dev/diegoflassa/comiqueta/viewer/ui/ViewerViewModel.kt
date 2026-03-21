package dev.diegoflassa.comiqueta.viewer.ui

import android.app.Application
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.comiqueta.core.data.preferences.PreferencesKeys
import dev.diegoflassa.comiqueta.core.data.repository.IComicsRepository
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.data.util.CoverUtils
import dev.diegoflassa.comiqueta.core.domain.usecase.comic.IGetComicUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.comic.IUpdateComicProgressUseCase
import dev.diegoflassa.comiqueta.core.model.ComicFileType
import dev.diegoflassa.comiqueta.viewer.R
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
class ViewerViewModel @Inject constructor(
    private val getComicInfoUseCase: IGetComicInfoUseCase,
    private val decodeComicPageUseCase: IDecodeComicPageUseCase,
    private val getComicUseCase: IGetComicUseCase,
    private val updateComicProgressUseCase: IUpdateComicProgressUseCase,
    private val comicsRepository: IComicsRepository,
    private val application: Application,
    private val dataStore: DataStore<Preferences>
) : ViewModel(), IViewerViewModel {

    private val _uiState = MutableStateFlow(ViewerUIState())
    override val uiState: StateFlow<ViewerUIState> = _uiState.asStateFlow()

    private val _effect = Channel<ViewerEffect>(Channel.BUFFERED)
    override val effect: Flow<ViewerEffect> = _effect.receiveAsFlow()

    private var comicPageIdentifiers: List<String> = emptyList()
    private var currentComicUri: Uri? = null
    private var currentComicFileType: ComicFileType? = null

    private val pageBitmapCache: LruCache<Int, ImageBitmap> by lazy {
        val initialPreload = _pagesToPreloadLogic.value
            .coerceAtLeast(MIN_PRELOAD_COUNT_LOGIC)
            .coerceAtMost(MAX_SETTING_FOR_CACHE_INIT)
        val cacheSize = 1 + 2 * initialPreload
        LruCache(cacheSize.coerceAtLeast(1))
    }
    private val thumbnailBitmapCache: LruCache<Int, ImageBitmap> by lazy {
        LruCache(200)
    }
    private val pageLoadJobs = mutableMapOf<Int, Job>()
    private val thumbnailLoadJobs = mutableMapOf<Int, Job>()

    private val _pagesToPreloadLogic =
        MutableStateFlow(ViewerUIState.DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD)

    companion object {
        private const val TAG = "ViewerViewModel"
        private const val MIN_PRELOAD_COUNT_LOGIC = 0
        private const val MAX_PRELOAD_COUNT_LOGIC = 5
        private const val MAX_SETTING_FOR_CACHE_INIT = 10
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
                FirebaseCrashlytics.getInstance().recordException(ex)
                TimberLogger.logE(TAG, "Failed to get initial preload count, using default.", ex)
                ViewerUIState.DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD
            }

            val initialLogicPreload = initialSettingValue
                .coerceAtLeast(MIN_PRELOAD_COUNT_LOGIC)
                .coerceAtMost(MAX_PRELOAD_COUNT_LOGIC)
            _pagesToPreloadLogic.value = initialLogicPreload
            _uiState.update {
                it.copy(
                    pagesToPreloadLogic = initialLogicPreload,
                    isLoadingPage = emptySet(),
                    isLoadingThumbnail = emptySet()
                )
            }
            TimberLogger.logI(TAG, "Initial logic preload count set to: $initialLogicPreload.")

            val isMangaModeFlow: Flow<Boolean> = dataStore.data
                .map {
                    it[PreferencesKeys.MANGA_MODE] ?: false
                }

            launch {
                isMangaModeFlow.collect { isMangaMode ->
                    _uiState.update { it.copy(isMangaMode = isMangaMode) }
                }
            }

            launch {
                dataStore.data.map { it[PreferencesKeys.WEBTOON_MODE] ?: false }.collect { enabled ->
                    _uiState.update { it.copy(isWebtoonMode = enabled) }
                }
            }

            launch {
                dataStore.data.map { it[PreferencesKeys.DOUBLE_PAGE_MODE] ?: false }.collect { enabled ->
                    _uiState.update { it.copy(isDoublePageMode = enabled) }
                }
            }

            launch {
                dataStore.data.map { it[PreferencesKeys.PAGE_FLIP_SOUND_ENABLED] ?: false }.collect { enabled ->
                    _uiState.update { it.copy(isPageFlipSoundEnabled = enabled) }
                }
            }

            viewerPagesToPreloadAheadFlow
                .catch { e ->
                    FirebaseCrashlytics.getInstance().recordException(e)
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

    override fun reduce(intent: ViewerIntent) {
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

            is ViewerIntent.UpdateZoom -> {
                _uiState.update {
                    it.copy(
                        zoomScale = intent.scale,
                        zoomOffsetX = intent.offsetX,
                        zoomOffsetY = intent.offsetY
                    )
                }
            }

            is ViewerIntent.ToggleUiVisibility -> _uiState.update { it.copy(isUiVisible = !it.isUiVisible) }
            is ViewerIntent.ErrorShown -> _uiState.update { it.copy(error = null) }
            is ViewerIntent.LoadThumbnail -> handleLoadThumbnail(intent.pageNumber)
            is ViewerIntent.SetAsCover -> handleSetAsCover()
        }
    }

    private fun handleLoadThumbnail(pageIndex: Int) {
        if (uiState.value.loadedThumbnails.containsKey(pageIndex)) return
        if (uiState.value.isLoadingThumbnail.contains(pageIndex)) return
        if (thumbnailLoadJobs.containsKey(pageIndex)) return

        thumbnailLoadJobs[pageIndex] = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingThumbnail = it.isLoadingThumbnail + pageIndex) }
                val bitmap = loadPageBitmapInternal(pageIndex, thumbnailWidth = 300)
                if (isActive && bitmap != null) {
                    thumbnailBitmapCache.put(pageIndex, bitmap)
                    _uiState.update { state ->
                        state.copy(
                            loadedThumbnails = state.loadedThumbnails + (pageIndex to bitmap),
                            isLoadingThumbnail = state.isLoadingThumbnail - pageIndex
                        )
                    }
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                TimberLogger.logE(TAG, "Error loading thumbnail for page $pageIndex", e)
            } finally {
                _uiState.update { it.copy(isLoadingThumbnail = it.isLoadingThumbnail - pageIndex) }
                thumbnailLoadJobs.remove(pageIndex)
            }
        }
    }

    private fun handleSetAsCover() {
        val currentPage = uiState.value.currentPage
        val comicUri = currentComicUri ?: return
        val fileType = currentComicFileType ?: return
        val pageIdentifier = if (currentPage < comicPageIdentifiers.size) {
            comicPageIdentifiers[currentPage]
        } else {
            currentPage.toString()
        }

        viewModelScope.launch {
            try {
                // We want a thumbnail size for the cover
                val bitmap = decodeComicPageUseCase(
                    pageIndex = currentPage,
                    pageIdentifier = pageIdentifier,
                    comicUri = comicUri,
                    fileType = fileType,
                    thumbnailWidth = CoverUtils.THUMBNAIL_WIDTH
                )?.asAndroidBitmap()

                if (bitmap != null) {
                    val comic = getComicUseCase(comicUri.toString())
                    if (comic != null) {
                        // Delete old cover if it exists in our covers directory
                        CoverUtils.deleteOldCover(application, comic.coverPath.toUri())

                        val newCoverUri = CoverUtils.saveBitmapToCache(
                            application,
                            bitmap,
                            comic.title ?: "custom_cover"
                        )
                        if (newCoverUri != null) {
                            comicsRepository.updateComicCover(comicUri.toString(), newCoverUri.toString())
                            TimberLogger.logI(
                                TAG,
                                "Successfully set page $currentPage as cover for ${comic.title}"
                            )
                            _effect.send(ViewerEffect.ShowMessage(application.getString(R.string.cover_updated_success)))
                        }
                    }
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                TimberLogger.logE(TAG, "Error setting page as cover", e)
                _effect.send(ViewerEffect.ShowError(application.getString(R.string.cover_updated_error)))
            }
        }
    }

    private fun handleLoadComic(uri: Uri) {
        viewModelScope.launch {
            if (currentComicUri == uri) {
                TimberLogger.logI(TAG, "LoadComic: Comic $uri already loaded. Skipping.")
                return@launch
            }
            try {
                _uiState.update {
                    it.copy(
                        comicTitle = "",
                        comicPath = uri,
                        loadedPages = emptyMap(),
                        currentPage = 0, pageCount = 0,
                        fileType = null,
                        isLoadingPage = emptySet(),
                        error = null,
                        pagesToPreloadLogic = _pagesToPreloadLogic.value
                    )
                }

                val comic = getComicUseCase(uri.toString())
                val initialPage = comic?.lastPageRead ?: 0

                currentComicUri = uri
                pageBitmapCache.evictAll()
                thumbnailBitmapCache.evictAll()

                comicPageIdentifiers = emptyList()

                pageLoadJobs.values.forEach { it.cancelJob("New comic load requested") }
                pageLoadJobs.clear()

                TimberLogger.logI(TAG, "LoadComic: Starting for $uri")
                val comicInfo = getComicInfoUseCase(currentComicUri!!)
                comicPageIdentifiers = comicInfo.pageIdentifiers
                currentComicFileType = comicInfo.fileType

                _uiState.update {
                    it.copy(
                        comicTitle = comicInfo.title,
                        comicPath = uri,
                        pageCount = comicInfo.pageCount,
                        fileType = comicInfo.fileType
                    )
                }

                if (comicInfo.pageCount > 0 && comicInfo.pageIdentifiers.isNotEmpty()) {
                    dispatchLoadPages(initialPage.coerceIn(0, comicInfo.pageCount - 1))
                } else {
                    TimberLogger.logW(TAG, "LoadComic: Comic has no pages.")
                    _effect.send(ViewerEffect.ShowError("Comic has no pages or is empty."))
                }
            } catch (cex: CancellationException) {
                TimberLogger.logI(TAG, "LoadComic (getComicInfo) cancelled: ${cex.message}")
                _uiState.update { it.copy(comicPath = Uri.EMPTY) }
            } catch (ex: Exception) {
                FirebaseCrashlytics.getInstance().recordException(ex)
                TimberLogger.logE(TAG, "LoadComic (getComicInfo): Error loading comic $uri", ex)
                val errorMessage = ex.localizedMessage ?: "Failed to load comic"
                _uiState.update { it.copy(error = errorMessage, comicPath = Uri.EMPTY) }
                _effect.send(ViewerEffect.ShowError(errorMessage))
            }
        }
    }

    private fun dispatchLoadPages(targetPageIndex: Int) {
        TimberLogger.logD(TAG, "dispatchLoadPages for page: $targetPageIndex")
        if (currentComicUri == null || currentComicFileType == null || comicPageIdentifiers.isEmpty()) {
            TimberLogger.logW(
                TAG,
                "dispatchLoadPages: Comic data not ready for page $targetPageIndex."
            )
            if (uiState.value.pageCount == 0) {
                _uiState.update { it.copy(error = "Comic data not fully loaded.") }
            }
            return
        }
        val pageCount = uiState.value.pageCount
        if (targetPageIndex !in 0..<pageCount) {
            TimberLogger.logW(
                TAG,
                "dispatchLoadPages: Invalid targetPageIndex $targetPageIndex for pageCount $pageCount"
            )
            viewModelScope.launch { _effect.send(ViewerEffect.ShowError("Invalid page number: ${targetPageIndex + 1}")) }
            return
        }

        val currentLogicPreload = _pagesToPreloadLogic.value
        val relevantPageIndices = mutableSetOf(targetPageIndex)
        for (offset in 1..currentLogicPreload) {
            if (targetPageIndex - offset >= 0) relevantPageIndices.add(targetPageIndex - offset)
            if (targetPageIndex + offset < pageCount) relevantPageIndices.add(targetPageIndex + offset)
        }

        // Cancel jobs for pages no longer relevant
        val jobsToCancel = pageLoadJobs.filterKeys { !relevantPageIndices.contains(it) }
        jobsToCancel.forEach { (idx, job) ->
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
                } else if (pagesThatNeedNewLoadJob.contains(idx) || activeRelevantLoadJobIndices.contains(
                        idx
                    )
                ) {
                    finalIsLoadingPage.add(idx)
                }
            }
            currentState.copy(
                currentPage = targetPageIndex,
                loadedPages = finalLoadedPages,
                isLoadingPage = finalIsLoadingPage,
                error = null
            )
        }

        // Persist reading progress
        viewModelScope.launch {
            try {
                updateComicProgressUseCase(
                    filePath = currentComicUri?.toString() ?: return@launch,
                    lastPageRead = targetPageIndex,
                    isCompleted = targetPageIndex >= pageCount - 1
                )
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                TimberLogger.logE(TAG, "Failed to update comic progress", e)
            }
        }

        pagesThatNeedNewLoadJob.forEach { pageToLoadIdx ->
            pageLoadJobs[pageToLoadIdx] = viewModelScope.launch {
                try {
                    val bitmap = loadPageBitmapInternal(pageToLoadIdx)
                    if (isActive && bitmap != null) {
                        _uiState.update { state ->
                            val stillRelevant = (state.currentPage - pageToLoadIdx).let { diff ->
                                diff == 0 || (kotlin.math.abs(diff) <= state.pagesToPreloadLogic)
                            } && pageToLoadIdx < state.pageCount
                            if (stillRelevant) {
                                state.copy(
                                    loadedPages = state.loadedPages + (pageToLoadIdx to bitmap),
                                    isLoadingPage = state.isLoadingPage - pageToLoadIdx
                                )
                            } else {
                                state.copy(isLoadingPage = state.isLoadingPage - pageToLoadIdx)
                            }
                        }
                    } else if (isActive && bitmap == null) {
                        _uiState.update { it.copy(isLoadingPage = it.isLoadingPage - pageToLoadIdx) }
                    }
                } catch (cex: CancellationException) {
                    TimberLogger.logE(TAG, "Error loading page $pageToLoadIdx", cex)
                } catch (ex: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(ex)
                    TimberLogger.logE(TAG, "Error loading page $pageToLoadIdx", ex)
                } finally {
                    if (isActive) {
                        _uiState.update { it.copy(isLoadingPage = it.isLoadingPage - pageToLoadIdx) }
                    }
                    pageLoadJobs.remove(pageToLoadIdx)
                }
            }
        }
    }

    private suspend fun loadPageBitmapInternal(pageIndex: Int, thumbnailWidth: Int? = null): ImageBitmap? {
        val localCurrentComicUri = currentComicUri
        val localCurrentComicFileType = currentComicFileType
        if (localCurrentComicUri == null || localCurrentComicFileType == null) {
            throw IllegalStateException("Comic data (URI or FileType) not ready for page $pageIndex")
        }
        if (comicPageIdentifiers.isEmpty() || pageIndex < 0 || pageIndex >= comicPageIdentifiers.size) {
            throw IndexOutOfBoundsException("Page index $pageIndex out of bounds for ${comicPageIdentifiers.size} pages")
        }

        if (thumbnailWidth == null) {
            pageBitmapCache.get(pageIndex)?.let { return it }
        } else {
            thumbnailBitmapCache.get(pageIndex)?.let { return it }
        }

        val pageIdentifier = comicPageIdentifiers[pageIndex]
        return try {
            val bitmap = decodeComicPageUseCase(
                pageIndex,
                pageIdentifier,
                localCurrentComicUri,
                localCurrentComicFileType,
                thumbnailWidth
            )
            bitmap?.also {
                if (thumbnailWidth == null) {
                    pageBitmapCache.put(pageIndex, it)
                } else {
                    thumbnailBitmapCache.put(pageIndex, it)
                }
            }
        } catch (cex: CancellationException) {
            throw cex
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            TimberLogger.logE(TAG, "Error during decodeComicPage for $pageIndex", ex)
            throw ex
        }
    }

    private fun Job.cancelJob(message: String) {
        try {
            if (this.isActive) this.cancel(CancellationException(message))
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            TimberLogger.logE(
                TAG,
                "Exception during job cancellation: $message - ${ex.message}",
                ex
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        TimberLogger.logI(TAG, "ViewModel cleared. Cancelling all page load jobs.")
        pageLoadJobs.values.forEach { it.cancelJob("ViewModel cleared") }
        pageLoadJobs.clear()
    }
}
