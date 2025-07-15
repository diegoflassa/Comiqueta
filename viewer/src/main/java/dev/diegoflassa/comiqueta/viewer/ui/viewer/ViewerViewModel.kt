package dev.diegoflassa.comiqueta.viewer.ui.viewer

import android.app.Application
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache // Added import
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.junrar.Archive as JunrarArchive
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.model.ComicFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
open class ViewerViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUIState())
    open val uiState: StateFlow<ViewerUIState> = _uiState.asStateFlow()

    private val _effect = Channel<ViewerEffect>(Channel.BUFFERED)
    open val effect: Flow<ViewerEffect> = _effect.receiveAsFlow()

    private var comicPageIdentifiers: List<String> = emptyList()

    // Changed from MutableMap to LruCache with a size of 5 pages
    private val pageBitmapCache = LruCache<Int, ImageBitmap>(5)
    private var currentLoadingJob: Job? = null
    private var preloadingJob: Job? = null

    private val alphanumComparator = Comparator<String> { s1, s2 ->
        val regex = Regex("([0-9]+)|([^0-9]+)")
        val parts1 = regex.findAll(s1).map { it.value }.toList()
        val parts2 = regex.findAll(s2).map { it.value }.toList()

        val maxParts = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxParts) {
            val p1 = parts1.getOrNull(i)
            val p2 = parts2.getOrNull(i)

            if (p1 == null) return@Comparator -1 // s1 is shorter
            if (p2 == null) return@Comparator 1  // s2 is shorter

            val isNum1 = p1.matches(Regex("[0-9]+"))
            val isNum2 = p2.matches(Regex("[0-9]+"))

            if (isNum1 && isNum2) {
                val num1 = p1.toLong()
                val num2 = p2.toLong()
                if (num1 != num2) return@Comparator num1.compareTo(num2)
            } else {
                val cmp = p1.compareTo(p2, ignoreCase = true)
                if (cmp != 0) return@Comparator cmp
            }
        }
        return@Comparator 0
    }

    private fun isImageFile(fileName: String): Boolean {
        val lowerName = fileName.lowercase(Locale.ROOT)
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".gif") ||
                lowerName.endsWith(".webp") || lowerName.endsWith(".bmp")
    }

    private data class ComicInfo(
        val title: String,
        val pageCount: Int,
        val pageIdentifiers: List<String>,
        val fileType: ComicFileType
    )

    private suspend fun getComicInfo(uri: Uri): ComicInfo {
        return withContext(Dispatchers.IO) {
            val context = application.applicationContext
            val docFile = DocumentFile.fromSingleUri(context, uri)
                ?: throw IOException("Could not access DocumentFile for URI: $uri")
            val fileName = docFile.name ?: "Unknown"
            val title = fileName.substringBeforeLast(".")

            var pfd: ParcelFileDescriptor? = null
            val pageIdentifiers = mutableListOf<String>()
            var pageCount = 0
            var determinedFileType: ComicFileType?

            try {
                val mimeType = context.contentResolver.getType(uri)
                val fileExtension =
                    fileName.substringAfterLast(".", "").takeIf { it.isNotEmpty() }
                TimberLogger.logI(
                    "ViewerViewModel",
                    "Attempting to determine file type for URI: $uri, MimeType: $mimeType, FileName: $fileName, Extension: $fileExtension"
                )

                determinedFileType = ComicFileType.fromMimeTypeOrExtension(mimeType, fileExtension)

                if (determinedFileType == null) {
                    throw IOException("Unsupported file type or could not determine type for: $fileName (MIME: $mimeType, Ext: $fileExtension)")
                }
                TimberLogger.logI(
                    "ViewerViewModel",
                    "Determined file type using ComicFileType enum: $determinedFileType"
                )

                @Suppress("UNUSED_VARIABLE")
                when (determinedFileType) {
                    ComicFileType.PDF -> {
                        pfd = context.contentResolver.openFileDescriptor(uri, "r")
                            ?: throw IOException("PFD null for PDF.")
                        val renderer = PdfRenderer(pfd)
                        pageCount = renderer.pageCount
                        pageIdentifiers.addAll(List(pageCount) { it.toString() })
                        renderer.close()
                    }

                    ComicFileType.CBZ -> {
                        context.contentResolver.openInputStream(uri)?.use { fis ->
                            BufferedInputStream(fis).use { bis ->
                                val archiveStreamForListing =
                                    ArchiveStreamFactory().createArchiveInputStream(
                                        ArchiveStreamFactory.ZIP,
                                        bis
                                    ) as ArchiveInputStream<out ArchiveEntry>
                                archiveStreamForListing.use { ais ->
                                    generateSequence { ais.nextEntry }
                                        .filter { entry -> !entry.isDirectory && isImageFile(entry.name) }
                                        .map { entry -> entry.name }
                                        .toList()
                                        .sortedWith(alphanumComparator)
                                        .let {
                                            pageIdentifiers.addAll(it)
                                            pageCount = it.size
                                        }
                                }
                            }
                        } ?: throw IOException("Could not open InputStream for CBZ.")
                    }

                    ComicFileType.CBR -> {
                        val tempFile = File(
                            context.cacheDir,
                            "temp_cbr_viewer_${System.currentTimeMillis()}.cbr"
                        )
                        try {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                FileOutputStream(tempFile).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            } ?: throw IOException("Could not open InputStream for CBR.")

                            JunrarArchive(tempFile).use { archive ->
                                archive.fileHeaders
                                    .filter { !it.isDirectory && isImageFile(it.fileName) }
                                    .map { it.fileName }
                                    .toList()
                                    .sortedWith(alphanumComparator)
                                    .let {
                                        pageIdentifiers.addAll(it)
                                        pageCount = it.size
                                    }
                            }
                        } finally {
                            if (tempFile.exists()) tempFile.delete()
                        }
                    }

                    ComicFileType.CB7 -> {
                        val tempFile = File(
                            context.cacheDir,
                            "temp_cb7_viewer_${System.currentTimeMillis()}.cb7"
                        )
                        try {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                FileOutputStream(tempFile).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            } ?: throw IOException("Could not open InputStream for CB7.")

                            SevenZFile.Builder().setFile(tempFile).get().use { sevenZFile ->
                                generateSequence { sevenZFile.nextEntry }
                                    .filter { !it.isDirectory && isImageFile(it.name) }
                                    .map { it.name }
                                    .toList()
                                    .sortedWith(alphanumComparator)
                                    .let {
                                        pageIdentifiers.addAll(it)
                                        pageCount = it.size
                                    }
                            }
                        } finally {
                            if (tempFile.exists()) tempFile.delete()
                        }
                    }

                    ComicFileType.CBT -> {
                        context.contentResolver.openInputStream(uri)?.use { fis ->
                            BufferedInputStream(fis).use { bis ->
                                val tarInput: TarArchiveInputStream = when {
                                    fileName.endsWith(".tar.gz", true) || fileName.endsWith(
                                        ".tgz",
                                        true
                                    ) ->
                                        TarArchiveInputStream(GzipCompressorInputStream(bis))

                                    fileName.endsWith(
                                        ".tar.bz2",
                                        true
                                    ) || fileName.endsWith(".tbz2", true) ->
                                        TarArchiveInputStream(BZip2CompressorInputStream(bis))

                                    else -> TarArchiveInputStream(bis)
                                }
                                tarInput.use { ais ->
                                    generateSequence { ais.nextEntry }
                                        .filter { !it.isDirectory && isImageFile(it.name) }
                                        .map { it.name }
                                        .toList()
                                        .sortedWith(alphanumComparator)
                                        .let {
                                            pageIdentifiers.addAll(it)
                                            pageCount = it.size
                                        }
                                }
                            }
                        } ?: throw IOException("Could not open InputStream for CBT.")
                    }

                    ComicFileType.JPG, ComicFileType.JPEG, ComicFileType.PNG, ComicFileType.GIF, ComicFileType.WEBP -> {
                        pageIdentifiers.add(fileName)
                        pageCount = 1
                    }
                }

                TimberLogger.logD(
                    "ViewerViewModel",
                    "Finished processing file type in getComicInfo. Page count: $pageCount"
                )

            } catch (e: Exception) {
                TimberLogger.logE("ViewerViewModel", "Error getting comic info for $uri", e)
                throw IOException(
                    "Failed to parse comic: ${e.message}",
                    e
                )
            } finally {
                try {
                    pfd?.close()
                } catch (e: IOException) {
                    TimberLogger.logE(
                        "ViewerViewModel",
                        "Error closing PFD for $uri in getComicInfo",
                        e
                    )
                }
            }
            ComicInfo(
                title,
                pageCount,
                Collections.unmodifiableList(pageIdentifiers.toList()),
                determinedFileType
            )
        }
    }

    private suspend fun decodePage(
        pageIndex: Int, // for logging and PDF (0-based)
        pageIdentifier: String, // for archives (actual filename/entry name)
        comicUriStr: String,
        fileType: ComicFileType
    ): ImageBitmap? { // Return nullable ImageBitmap
        return withContext(Dispatchers.IO) {
            TimberLogger.logI(
                "ViewerViewModel",
                "Decoding page index: $pageIndex, identifier: '$pageIdentifier' for $fileType"
            )
            val context = application.applicationContext
            val comicUri = comicUriStr.toUri()
            var pfd: ParcelFileDescriptor? = null

            try {
                val bitmapOptions = BitmapFactory.Options().apply {
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                }

                val loadedBitmap: ImageBitmap? = when (fileType) { // Can be null
                    ComicFileType.PDF -> {
                        pfd = context.contentResolver.openFileDescriptor(comicUri, "r")
                            ?: throw IOException("PFD null for PDF page.")
                        PdfRenderer(pfd).use { renderer ->
                            // For PDF, pageIdentifier is the string representation of pageIndex
                            val actualPageIndex = pageIdentifier.toIntOrNull() ?: pageIndex
                            if (actualPageIndex < 0 || actualPageIndex >= renderer.pageCount) {
                                throw IOException("Page index out of bounds for PDF. Index: $actualPageIndex, Count: ${renderer.pageCount}")
                            }
                            renderer.openPage(actualPageIndex).use { page ->
                                val bitmap = createBitmap(
                                    page.width,
                                    page.height,
                                    android.graphics.Bitmap.Config.ARGB_8888
                                )
                                page.render(
                                    bitmap,
                                    null,
                                    null,
                                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                )
                                bitmap.asImageBitmap()
                            }
                        }
                    }

                    ComicFileType.CBZ -> {
                        context.contentResolver.openInputStream(comicUri)?.use { fis ->
                            BufferedInputStream(fis).use { bufferedInputStream ->
                                val archiveStreamForExtraction =
                                    ArchiveStreamFactory().createArchiveInputStream(
                                        ArchiveStreamFactory.ZIP,
                                        bufferedInputStream
                                    ) as ArchiveInputStream<out ArchiveEntry>
                                archiveStreamForExtraction.use { ais ->
                                    var entry: ArchiveEntry? = ais.nextEntry
                                    while (entry != null) {
                                        if (entry.name == pageIdentifier) {
                                            return@use BitmapFactory.decodeStream(
                                                ais,
                                                null,
                                                bitmapOptions
                                            )?.asImageBitmap()
                                            // ?: throw IOException("Failed to decode image from CBZ entry: $pageIdentifier")
                                        }
                                        entry = ais.nextEntry
                                    }
                                    // throw IOException("Page not found in CBZ: $pageIdentifier")
                                    TimberLogger.logW(
                                        "ViewerViewModel",
                                        "Page not found in CBZ: $pageIdentifier"
                                    )
                                    null // Return null if page not found
                                }
                            }
                        } // ?: throw IOException("Could not open InputStream for CBZ page decoding.")
                            ?: run {
                                TimberLogger.logW(
                                    "ViewerViewModel",
                                    "Could not open InputStream for CBZ page decoding."
                                )
                                null
                            }
                    }

                    ComicFileType.CBR -> {
                        val tempFile = File(
                            context.cacheDir,
                            "temp_cbr_decode_${System.currentTimeMillis()}.cbr"
                        )
                        try {
                            context.contentResolver.openInputStream(comicUri)?.use { input ->
                                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                            } ?: run {
                                TimberLogger.logW(
                                    "ViewerViewModel",
                                    "Could not open input stream for CBR page decoding."
                                )
                                return@withContext null
                            }

                            JunrarArchive(tempFile).use { archive ->
                                val header =
                                    archive.fileHeaders.find { it.fileName == pageIdentifier }
                                //?: throw IOException("Page not found in CBR: $pageIdentifier")
                                if (header == null) {
                                    TimberLogger.logW(
                                        "ViewerViewModel",
                                        "Page not found in CBR: $pageIdentifier"
                                    )
                                    return@withContext null
                                }
                                if (header.isDirectory) {
                                    TimberLogger.logW(
                                        "ViewerViewModel",
                                        "Attempted to decode directory in CBR: $pageIdentifier"
                                    )
                                    return@withContext null
                                }
                                archive.getInputStream(header).use { entryStream ->
                                    BitmapFactory.decodeStream(entryStream, null, bitmapOptions)
                                        ?.asImageBitmap()
                                }
                            }
                        } finally {
                            if (tempFile.exists()) tempFile.delete()
                        }
                    }

                    ComicFileType.CB7 -> {
                        val tempFile = File(
                            context.cacheDir,
                            "temp_cb7_decode_${System.currentTimeMillis()}.cb7"
                        )
                        try {
                            context.contentResolver.openInputStream(comicUri)?.use { input ->
                                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                            } ?: run {
                                TimberLogger.logW(
                                    "ViewerViewModel",
                                    "Could not open input stream for CB7 page decoding."
                                )
                                return@withContext null
                            }

                            SevenZFile.Builder().setFile(tempFile).get().use { sevenZFile ->
                                var entry: SevenZArchiveEntry? =
                                    sevenZFile.nextEntry
                                while (entry != null) {
                                    if (entry.name == pageIdentifier) {
                                        val entryInputStream = sevenZFile.getInputStream(entry)
                                        return@use entryInputStream.use { eis ->
                                            BitmapFactory.decodeStream(eis, null, bitmapOptions)
                                                ?.asImageBitmap()
                                        }
                                    }
                                    entry = sevenZFile.nextEntry
                                }
                                TimberLogger.logW(
                                    "ViewerViewModel",
                                    "Page not found in CB7: $pageIdentifier"
                                )
                                null
                            }
                        } finally {
                            if (tempFile.exists()) tempFile.delete()
                        }
                    }

                    ComicFileType.CBT -> {
                        context.contentResolver.openInputStream(comicUri)?.use { fis ->
                            BufferedInputStream(fis).use { bis ->
                                val docFile = DocumentFile.fromSingleUri(context, comicUri)
                                val fileName = docFile?.name ?: ""
                                val tarInput: TarArchiveInputStream = when {
                                    fileName.endsWith(".tar.gz", true) || fileName.endsWith(
                                        ".tgz",
                                        true
                                    ) ->
                                        TarArchiveInputStream(GzipCompressorInputStream(bis))

                                    fileName.endsWith(
                                        ".tar.bz2",
                                        true
                                    ) || fileName.endsWith(".tbz2", true) ->
                                        TarArchiveInputStream(BZip2CompressorInputStream(bis))

                                    else -> TarArchiveInputStream(bis)
                                }
                                tarInput.use { ais ->
                                    var entry: ArchiveEntry? = ais.nextEntry
                                    while (entry != null) {
                                        if (entry.name == pageIdentifier) {
                                            return@use BitmapFactory.decodeStream(
                                                ais,
                                                null,
                                                bitmapOptions
                                            )?.asImageBitmap()
                                        }
                                        entry = ais.nextEntry
                                    }
                                    TimberLogger.logW(
                                        "ViewerViewModel",
                                        "Page not found in CBT: $pageIdentifier"
                                    )
                                    null
                                }
                            }
                        } ?: run {
                            TimberLogger.logW(
                                "ViewerViewModel",
                                "Could not open InputStream for CBT page decoding."
                            )
                            null
                        }
                    }

                    ComicFileType.JPG, ComicFileType.JPEG, ComicFileType.PNG, ComicFileType.GIF, ComicFileType.WEBP -> {
                        context.contentResolver.openInputStream(comicUri)?.use { imageStream ->
                            BitmapFactory.decodeStream(imageStream, null, bitmapOptions)
                                ?.asImageBitmap()
                        } ?: run {
                            TimberLogger.logW(
                                "ViewerViewModel",
                                "Could not open InputStream for image file: $pageIdentifier"
                            )
                            null
                        }
                    }
                }
                loadedBitmap
            } catch (e: Exception) {
                TimberLogger.logE(
                    "ViewerViewModel",
                    "Error decoding page '$pageIdentifier' ($fileType), pageIndex $pageIndex",
                    e
                )
                // Do not return an error bitmap from here, let the caller handle null
                null
            } finally {
                try {
                    pfd?.close()
                } catch (e: IOException) {
                    TimberLogger.logE("ViewerViewModel", "Error closing PFD for page decoding", e)
                }
            }
        }
    }

    open fun processIntent(intent: ViewerIntent) {
        if (intent is ViewerIntent.LoadComic) {
            currentLoadingJob?.cancel()
            preloadingJob?.cancel()
            pageBitmapCache.evictAll() // Changed from clear() to evictAll() for LruCache
            comicPageIdentifiers = emptyList()
        }

        viewModelScope.launch {
            when (intent) {
                is ViewerIntent.LoadComic -> loadComicFromFile(intent.uri)
                is ViewerIntent.NavigateToPage -> navigateToPage(intent.pageIndex)
                is ViewerIntent.ToggleViewerControls -> _uiState.update { it.copy(showViewerControls = !it.showViewerControls) }
                is ViewerIntent.ClearError -> _uiState.update { it.copy(viewerError = null) }
            }
        }
    }

    private fun loadComicFromFile(uri: Uri) {
        currentLoadingJob = viewModelScope.launch {
            _uiState.update { currentState ->
                ViewerUIState( // Reset to a new state for a new comic
                    comicIdentifierUri = uri.toString(),
                    isLoadingComic = true,
                    comicTitle = "",
                    comicPages = emptyList(),
                    currentPageNumber = 0,
                    totalPageCount = 0,
                    comicFileType = null,
                    showViewerControls = currentState.showViewerControls, // Preserve control visibility
                    viewerError = null
                )
            }
            try {
                TimberLogger.logI("ViewerViewModel", "Loading comic from URI: $uri")
                val comicInfo = getComicInfo(uri)
                this@ViewerViewModel.comicPageIdentifiers = comicInfo.pageIdentifiers

                _uiState.update {
                    it.copy(
                        // isLoadingComic remains true until the first page attempt
                        comicTitle = comicInfo.title,
                        totalPageCount = comicInfo.pageCount,
                        comicFileType = comicInfo.fileType
                    )
                }

                if (comicInfo.pageCount > 0) {
                    navigateToPage(
                        0,
                        isInitialLoad = true
                    ) // This will set isLoadingComic to false on completion/error
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingComic = false, // No pages, so loading is done
                            viewerError = "No pages found in comic."
                        )
                    }
                }
            } catch (e: Exception) {
                TimberLogger.logE("ViewerViewModel", "Error loading comic info", e)
                _uiState.update {
                    it.copy(
                        isLoadingComic = false,
                        viewerError = "Failed to load comic: ${e.message ?: "Unknown error"}"
                    )
                }
                _effect.send(ViewerEffect.ShowToast("Error: ${e.localizedMessage ?: "Could not load comic."}"))
            }
        }
    }

    private fun navigateToPage(pageIndex: Int, isInitialLoad: Boolean = false) {
        TimberLogger.logD(
            "ViewerViewModel",
            "navigateToPage: Called with pageIndex: $pageIndex, isInitialLoad: $isInitialLoad"
        )
        val currentTotalPages = _uiState.value.totalPageCount

        TimberLogger.logD(
            "ViewerViewModel",
            "navigateToPage: currentTotalPages: $currentTotalPages"
        )

        if (currentTotalPages == 0 && isInitialLoad) {
            TimberLogger.logW(
                "ViewerViewModel",
                "navigateToPage: Initial load with 0 total pages for pageIndex: $pageIndex."
            )
            _uiState.update {
                it.copy(
                    isLoadingComic = false,
                    viewerError = it.viewerError ?: "Comic has no pages."
                )
            }
            return
        }

        if (pageIndex < 0 || pageIndex >= currentTotalPages) {
            TimberLogger.logW(
                "ViewerViewModel",
                "navigateToPage: Attempted to navigate to invalid page index: $pageIndex (Total: $currentTotalPages)"
            )
            if (isInitialLoad) {
                _uiState.update {
                    it.copy(
                        isLoadingComic = false,
                        viewerError = "Invalid initial page index."
                    )
                }
            }
            return
        }
        TimberLogger.logD(
            "ViewerViewModel",
            "navigateToPage: Passed initial boundary checks for pageIndex: $pageIndex"
        )

        if (currentLoadingJob?.isActive == true) {
            TimberLogger.logD(
                "ViewerViewModel",
                "navigateToPage: Cancelling active currentLoadingJob for pageIndex: $pageIndex"
            )
            currentLoadingJob?.cancel()
        }

        currentLoadingJob = viewModelScope.launch {
            TimberLogger.logD(
                "ViewerViewModel",
                "navigateToPage: Launched coroutine for pageIndex: $pageIndex"
            )
            _uiState.update {
                TimberLogger.logD(
                    "ViewerViewModel",
                    "navigateToPage: Updating UI state - currentPageNumber: ${pageIndex + 1}, clearing comicPages for pageIndex: $pageIndex"
                )
                it.copy(
                    currentPageNumber = pageIndex + 1,
                    comicPages = emptyList(), // Clear current page, new one is loading
                    viewerError = null       // Clear previous page-specific error
                )
            }

            try {
                val comicUri = _uiState.value.comicIdentifierUri
                val fileType = _uiState.value.comicFileType
                TimberLogger.logD(
                    "ViewerViewModel",
                    "navigateToPage: Retrieved comicUri: $comicUri, fileType: $fileType for pageIndex: $pageIndex"
                )

                if (comicUri == null) {
                    TimberLogger.logE(
                        "ViewerViewModel",
                        "navigateToPage: Comic URI is null for pageIndex: $pageIndex."
                    )
                    throw IllegalStateException("Comic URI not set for navigation.")
                }
                if (fileType == null) {
                    TimberLogger.logE(
                        "ViewerViewModel",
                        "navigateToPage: Comic file type is null for pageIndex: $pageIndex."
                    )
                    throw IllegalStateException("Comic file type not set for navigation.")
                }

                TimberLogger.logD(
                    "ViewerViewModel",
                    "navigateToPage: Accessing comicPageIdentifiers (size: ${comicPageIdentifiers.size}) at pageIndex: $pageIndex"
                )
                if (pageIndex < 0 || pageIndex >= comicPageIdentifiers.size) {
                    TimberLogger.logE(
                        "ViewerViewModel",
                        "navigateToPage: pageIndex $pageIndex is out of bounds for comicPageIdentifiers size ${comicPageIdentifiers.size}"
                    )
                    throw IndexOutOfBoundsException("pageIndex $pageIndex is out of bounds for comicPageIdentifiers size ${comicPageIdentifiers.size}")
                }
                val pageIdentifier = comicPageIdentifiers[pageIndex]
                TimberLogger.logD(
                    "ViewerViewModel",
                    "navigateToPage: Retrieved pageIdentifier: '$pageIdentifier' for pageIndex: $pageIndex"
                )

                var pageBitmap: ImageBitmap?
                TimberLogger.logD(
                    "ViewerViewModel",
                    "navigateToPage: Checking cache for pageIndex: $pageIndex"
                )
                val cachedBitmap = pageBitmapCache.get(pageIndex)
                if (cachedBitmap != null) {
                    pageBitmap = cachedBitmap
                    TimberLogger.logI(
                        "ViewerViewModel",
                        "navigateToPage: Page ${pageIndex + 1} ('$pageIdentifier') loaded from cache for pageIndex: $pageIndex. Bitmap: $pageBitmap"
                    )
                } else {
                    TimberLogger.logI(
                        "ViewerViewModel",
                        "navigateToPage: Page ${pageIndex + 1} ('$pageIdentifier') not in cache, attempting to decode for pageIndex: $pageIndex"
                    )
                    val decodedBitmap = decodePage(
                        pageIndex,
                        pageIdentifier,
                        comicUri,
                        fileType
                    )
                    TimberLogger.logD(
                        "ViewerViewModel",
                        "navigateToPage: decodePage returned: $decodedBitmap for pageIdentifier: '$pageIdentifier', pageIndex: $pageIndex"
                    )
                    if (decodedBitmap != null) {
                        TimberLogger.logD(
                            "ViewerViewModel",
                            "navigateToPage: Putting decoded bitmap into cache for pageIndex: $pageIndex, pageIdentifier: '$pageIdentifier'. Bitmap: $decodedBitmap"
                        )
                        pageBitmapCache.put(pageIndex, decodedBitmap)
                        TimberLogger.logI(
                            "ViewerViewModel",
                            "navigateToPage: Page ${pageIndex + 1} ('$pageIdentifier') decoded successfully and added to cache for pageIndex: $pageIndex."
                        )
                    } else {
                        TimberLogger.logW(
                            "ViewerViewModel",
                            "navigateToPage: Failed to decode page ${pageIndex + 1} ('$pageIdentifier'). decodePage returned null for pageIndex: $pageIndex."
                        )
                    }
                    pageBitmap = decodedBitmap
                }

                TimberLogger.logD(
                    "ViewerViewModel",
                    "navigateToPage: Final pageBitmap is ${if (pageBitmap == null) "null" else "not null"} for pageIndex: $pageIndex, pageIdentifier: '$pageIdentifier'"
                )

                if (pageBitmap != null) {
                    TimberLogger.logD(
                        "ViewerViewModel",
                        "navigateToPage: Successfully obtained bitmap for pageIndex: $pageIndex. Updating UI. Bitmap: $pageBitmap"
                    )
                    _uiState.update {
                        it.copy(
                            isLoadingComic = false,
                            comicPages = listOf(pageBitmap),
                            viewerError = null
                        )
                    }
                    TimberLogger.logD(
                        "ViewerViewModel",
                        "navigateToPage: Calling preloadAdjacentPages for currentPageIndex: $pageIndex"
                    )
                    preloadAdjacentPages(pageIndex)
                } else {
                    TimberLogger.logE(
                        "ViewerViewModel",
                        "navigateToPage: Error navigating to page $pageIndex: pageBitmap is null after cache check and decode attempt for '$pageIdentifier'."
                    )
                    val errorMessage =
                        "Failed to load page ${pageIndex + 1}: Could not decode or retrieve."
                    _uiState.update {
                        it.copy(
                            isLoadingComic = false,
                            comicPages = emptyList(),
                            viewerError = errorMessage
                        )
                    }
                    _effect.send(ViewerEffect.ShowToast("Error loading page ${pageIndex + 1}"))
                }
            } catch (e: Exception) {
                TimberLogger.logE(
                    "ViewerViewModel",
                    "navigateToPage: Error navigating to page $pageIndex. Exception: ${e.message}",
                    e
                )
                val errorMessage = "Failed to load page ${pageIndex + 1}: ${e.message}"
                _uiState.update {
                    it.copy(
                        isLoadingComic = false,
                        comicPages = emptyList(),
                        viewerError = errorMessage
                    )
                }
                _effect.send(ViewerEffect.ShowToast("Error loading page ${pageIndex + 1}"))
            }
        }
    }

    private fun preloadAdjacentPages(currentPageIndex: Int) {
        preloadingJob?.cancel()
        preloadingJob = viewModelScope.launch(Dispatchers.IO) {
            val comicUri = _uiState.value.comicIdentifierUri ?: return@launch
            val fileType = _uiState.value.comicFileType ?: return@launch
            val totalPages = _uiState.value.totalPageCount

            val pagesToPreload = listOf(currentPageIndex + 1, currentPageIndex - 1)

            for (pageIdx in pagesToPreload) {
                if (pageIdx >= 0 && pageIdx < totalPages && pageBitmapCache.get(pageIdx) == null) {
                    try {
                        if (pageIdx < comicPageIdentifiers.size) {
                            val pageIdentifier = comicPageIdentifiers[pageIdx]
                            TimberLogger.logI(
                                "ViewerViewModel",
                                "Preloading page: ${pageIdx + 1} ($pageIdentifier)"
                            )
                            val bitmap = decodePage(pageIdx, pageIdentifier, comicUri, fileType)
                            if (bitmap != null) { // Check if bitmap is not null before putting into cache
                                pageBitmapCache.put(pageIdx, bitmap)
                                TimberLogger.logI(
                                    "ViewerViewModel",
                                    "Successfully preloaded page: ${pageIdx + 1} ($pageIdentifier)"
                                )
                            } else {
                                TimberLogger.logW(
                                    "ViewerViewModel",
                                    "Failed to decode for preload page ${pageIdx + 1} ($pageIdentifier). decodePage returned null."
                                )
                            }
                        } else {
                            TimberLogger.logW(
                                "ViewerViewModel",
                                "Preload SKIPPED: pageIdx $pageIdx out of bounds for comicPageIdentifiers size ${comicPageIdentifiers.size}"
                            )
                        }
                    } catch (e: Exception) {
                        TimberLogger.logW(
                            "ViewerViewModel",
                            "Failed to preload page ${pageIdx + 1}",
                            e
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        TimberLogger.logI("ViewerViewModel", "onCleared called.")

        currentLoadingJob?.cancel()
        preloadingJob?.cancel()
        TimberLogger.logD("ViewerViewModel", "Ongoing jobs cancelled if active.")

        pageBitmapCache.evictAll()
        TimberLogger.logI("ViewerViewModel", "Page bitmap cache (LruCache) evicted.")

        TimberLogger.logI("ViewerViewModel", "onCleared finished.")
    }
}
