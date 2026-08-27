package dev.diegoflassa.comiqueta.core.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.diegoflassa.comiqueta.core.R
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsDao
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.preferences.PreferencesKeys
import dev.diegoflassa.comiqueta.core.data.repository.IComicsFolderRepository
import dev.diegoflassa.comiqueta.core.data.timber.LogRedaction
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.data.util.CoverUtils
import dev.diegoflassa.comiqueta.core.model.ComicFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigInteger
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import com.github.junrar.Archive as JunrarArchive
import com.github.junrar.rarfile.FileHeader as JunrarFileHeader

@HiltWorker
class SafFolderScanWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val comicsDao: ComicsDao,
    private val comicsFolderRepository: IComicsFolderRepository,
    private val dataStore: DataStore<Preferences>
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "comic_scan_channel"
        const val TAG = "SafFolderScanWorker"
        const val KEY_ERROR_MESSAGE = "key_error_message"
        const val KEY_FOLDER_URI = "key_folder_uri"
        const val KEY_PROGRESS = "progress"
        const val KEY_CURRENT_COMIC_NAME = "current_comic_name"
        const val KEY_PROCESSED_COMICS_COUNT = "processed_comics_count"
        const val KEY_TOTAL_FILES_COUNT = "total_files_count"
        const val KEY_PROCESSED_FILES_COUNT = "processed_files_count"

        // Configuration for saving strategy
        private const val UNBATCHED_SAVE_THRESHOLD = 20
        private const val BATCH_SIZE_AFTER_THRESHOLD = 10
    }

    override suspend fun doWork(): Result {
        setForeground(getForegroundInfo(0, null, 0, 0, 0))
        var firstErrorMessage: String? = null
        val specificFolderUriString = inputData.getString(KEY_FOLDER_URI)

        val folderUrisToScan: List<Uri> = if (specificFolderUriString != null) {
            TimberLogger.logD(
                TAG,
                "[Comiqueta][SafFolderScanWorker] Starting specific scan for folder URI: $specificFolderUriString"
            )
            try {
                listOf(specificFolderUriString.toUri())
            } catch (ex: Exception) {
                ex.printStackTrace()
                FirebaseCrashlytics.getInstance().recordException(ex)
                val errorMessage = "Invalid specific folder URI provided: $specificFolderUriString"
                TimberLogger.logE(TAG, errorMessage, ex)
                val outputData = workDataOf(KEY_ERROR_MESSAGE to errorMessage)
                return Result.failure(outputData)
            }
        } else {
            TimberLogger.logD(TAG, "[Comiqueta][SafFolderScanWorker] Starting general scan of all persisted folders.")
            try {
                comicsFolderRepository.getPersistedPermissions()
            } catch (ex: Exception) {
                ex.printStackTrace()
                FirebaseCrashlytics.getInstance().recordException(ex)
                val errorMessage = "Error fetching persisted folders for general scan."
                TimberLogger.logE(TAG, "[Comiqueta][SafFolderScanWorker] $errorMessage Repository error.", ex)
                val outputData = workDataOf(KEY_ERROR_MESSAGE to errorMessage)
                return Result.failure(outputData)
            }
        }

        if (folderUrisToScan.isEmpty()) {
            TimberLogger.logI(TAG, "[Comiqueta][SafFolderScanWorker] Scan finished successfully.")

            // Save scan statistics to DataStore
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.LAST_SCAN_TOTAL_FILES] = 0
                preferences[PreferencesKeys.LAST_SCAN_PROCESSED_COMICS] = 0
            }

            return Result.success()
        }

        TimberLogger.logI(TAG, "[Comiqueta][SafFolderScanWorker] Found ${folderUrisToScan.size} folder(s) to scan.")

        // Initialize state variables
        val totalFiles = AtomicInteger(0)
        val totalComicsProcessedCount = AtomicInteger(0)
        var processedFilesCount = 0
        var lastUpdateMillis = 0L
        var anyFolderScanFailed = false
        var lastKnownComicName: String? = null

        coroutineScope {
            // 1. Launch background counting task (Parallel)
            launch(Dispatchers.IO) {
                TimberLogger.logI(TAG, "[Comiqueta][SafFolderScanWorker] Starting parallel background file counting...")
                for ((index, uri) in folderUrisToScan.withIndex()) {
                    if (!isActive) break

                    val folderDoc = DocumentFile.fromTreeUri(appContext, uri)
                    val folderName = folderDoc?.name ?: "Folder ${index + 1}"

                    // Initial indeterminate progress
                    if (totalFiles.get() == 0) {
                        setProgress(workDataOf(KEY_CURRENT_COMIC_NAME to "Calculating total files in $folderName..."))
                    }

                    val folderCount = countFilesRecursively(uri)
                    val newTotal = totalFiles.addAndGet(folderCount)
                    TimberLogger.logD(
                        TAG,
                        "[Comiqueta][SafFolderScanWorker] Counted $folderCount files in $folderName. New total: $newTotal"
                    )
                }
                TimberLogger.logI(
                    TAG,
                    "[Comiqueta][SafFolderScanWorker] Background counting finished. Final total files: ${totalFiles.get()}"
                )
            }

            // 2. Start Scanning Immediately (Parallel)
            for (folderUri in folderUrisToScan) {
                if (!isActive) break

                TimberLogger.logD(TAG, "[Comiqueta][SafFolderScanWorker] processing folder uri=${LogRedaction.uri(folderUri)}")
                val rootDoc = try {
                    DocumentFile.fromTreeUri(appContext, folderUri)
                } catch (ex: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(ex)
                    val errorMessage = "Error accessing folder: $folderUri"
                    TimberLogger.logE(TAG, errorMessage, ex)
                    if (firstErrorMessage == null) firstErrorMessage =
                        "Error accessing folder. Please check permissions."
                    anyFolderScanFailed = true
                    continue
                }

                if (rootDoc == null || !rootDoc.isDirectory) {
                    val errorMessage = "Folder not valid or not a directory: $folderUri"
                    FirebaseCrashlytics.getInstance().recordException(Exception(errorMessage))
                    TimberLogger.logW(TAG, errorMessage)
                    if (firstErrorMessage == null) firstErrorMessage =
                        "Invalid directory: $folderUri"
                    anyFolderScanFailed = true
                    continue
                }

                try {
                    TimberLogger.logD(
                        TAG,
                        "[Comiqueta][SafFolderScanWorker] Scanning document tree for: ${rootDoc.name} (URI: $folderUri)"
                    )
                    processedFilesCount = scanDocumentFileForComics(
                        rootDoc,
                        processedFilesCount,
                        totalFiles,
                        totalComicsProcessedCount
                    ) { progress, comicName, processed, totalCount, comicsCount ->
                        if (comicName != null) {
                            lastKnownComicName = comicName
                        }
                        val currentTime = System.currentTimeMillis()
                        // Update progress every 500ms OR if we have a new comic name to show
                        if (currentTime - lastUpdateMillis > 500 || comicName != null) {
                            lastUpdateMillis = currentTime
                            // If totalCount is 0 (still counting), we can pass 0 or a placeholder. 
                            // The UI should handle totalCount=0 gracefully (e.g. infinite progress).
                            setProgress(
                                workDataOf(
                                    KEY_PROGRESS to progress,
                                    KEY_CURRENT_COMIC_NAME to lastKnownComicName,
                                    KEY_PROCESSED_FILES_COUNT to processed,
                                    KEY_TOTAL_FILES_COUNT to totalCount,
                                    KEY_PROCESSED_COMICS_COUNT to comicsCount
                                )
                            )
                            setForeground(
                                getForegroundInfo(
                                    progress,
                                    lastKnownComicName,
                                    processed,
                                    totalCount,
                                    comicsCount
                                )
                            )
                        }
                    }
                    TimberLogger.logD(TAG, "[Comiqueta][SafFolderScanWorker] scan finished uri=${LogRedaction.uri(folderUri)}")
                } catch (ex: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(ex)
                    val scanErrorMessage = "Error scanning folder: ${rootDoc.name}"
                    TimberLogger.logE(TAG, "[Comiqueta][SafFolderScanWorker] $scanErrorMessage uri=${LogRedaction.uri(folderUri)}", ex)
                    if (firstErrorMessage == null) firstErrorMessage = scanErrorMessage
                    anyFolderScanFailed = true
                }
            }
        }

        TimberLogger.logI(
            TAG, "[Comiqueta][SafFolderScanWorker] Finished processing all folder(s). Any folder scan failed: $anyFolderScanFailed"
        )

        return if (anyFolderScanFailed) {
            val finalErrorMessage = firstErrorMessage ?: "One or more folders failed to scan."
            TimberLogger.logW(TAG, "[Comiqueta][SafFolderScanWorker] Scan failed. Reporting message: $finalErrorMessage")
            val outputData = workDataOf(KEY_ERROR_MESSAGE to finalErrorMessage)
            Result.failure(outputData)
        } else {
            TimberLogger.logI(
                TAG,
                "[Comiqueta][SafFolderScanWorker] Scan completed successfully for all processed folder(s).${if (specificFolderUriString != null) " URI: $specificFolderUriString" else ""}"
            )

            // Save scan statistics to DataStore
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.LAST_SCAN_TOTAL_FILES] = totalFiles.get()
                preferences[PreferencesKeys.LAST_SCAN_PROCESSED_COMICS] =
                    totalComicsProcessedCount.get()
            }

            Result.success()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return getForegroundInfo(0, null, 0, 0, 0)
    }

    private fun getForegroundInfo(
        progress: Int,
        comicName: String?,
        processed: Int,
        total: Int,
        comicsCount: Int
    ): ForegroundInfo {
        val title = appContext.getString(R.string.scan_notification_title)
        val content = if (total > 0) {
            if (comicName != null) {
                appContext.getString(
                    R.string.scanning_progress_detail,
                    comicName,
                    processed,
                    total,
                    comicsCount
                )
            } else {
                appContext.getString(
                    R.string.scanning_progress_no_name,
                    processed,
                    total,
                    comicsCount
                )
            }
        } else {
            if (comicName != null) {
                appContext.getString(R.string.scanning_current_comic, comicName)
            } else {
                appContext.getString(R.string.scan_notification_content)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = appContext.getString(R.string.scan_notification_channel_name)
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun countFilesRecursively(parentUri: Uri): Int {
        var count = 0
        try {
            val parentDoc = DocumentFile.fromTreeUri(appContext, parentUri)
            if (parentDoc != null && parentDoc.isDirectory) {
                // Using a stack for iterative traversal to avoid potential StackOverflowError on deep trees
                // although DocumentFile operations are slow, this is safer than the previous crashing implementation
                val stack = java.util.Stack<DocumentFile>()
                stack.push(parentDoc)

                while (stack.isNotEmpty()) {
                    val current = stack.pop()
                    val files = current.listFiles()
                    for (file in files) {
                        if (file.isDirectory) {
                            stack.push(file)
                        } else {
                            count++
                        }
                    }
                }
            } else {
                TimberLogger.logW(TAG, "[Comiqueta][SafFolderScanWorker] parent uri is not a directory or invalid uri=${LogRedaction.uri(parentUri)}")
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            TimberLogger.logE(TAG, "[Comiqueta][SafFolderScanWorker] error counting files uri=${LogRedaction.uri(parentUri)}", e)
        }
        return count
    }

    private suspend fun scanDocumentFileForComics(
        dir: DocumentFile,
        currentProcessed: Int,
        totalFiles: AtomicInteger,
        totalComicsProcessedCount: AtomicInteger,
        onProgressUpdate: suspend (Int, String?, Int, Int, Int) -> Unit
    ): Int {
        var processed = currentProcessed
        val batch = mutableListOf<ComicEntity>()

        processed = scanDocumentFileRecursively(
            dir,
            processed,
            totalFiles,
            totalComicsProcessedCount,
            batch,
            onProgressUpdate
        )

        // Final flush for remaining comics in batch
        if (batch.isNotEmpty()) {
            try {
                comicsDao.insertComicsAndFts(batch)
                TimberLogger.logD(TAG, "[Comiqueta][SafFolderScanWorker] Final batch insert of ${batch.size} comics.")
            } catch (ex: Exception) {
                FirebaseCrashlytics.getInstance().recordException(ex)
                TimberLogger.logE(TAG, "[Comiqueta][SafFolderScanWorker] Error in final batch insert: ${ex.message}", ex)
            }
        }

        return processed
    }

    private suspend fun scanDocumentFileRecursively(
        dir: DocumentFile,
        currentProcessed: Int,
        totalFiles: AtomicInteger,
        totalComicsProcessedCount: AtomicInteger,
        batch: MutableList<ComicEntity>,
        onProgressUpdate: suspend (Int, String?, Int, Int, Int) -> Unit
    ): Int {
        var processed = currentProcessed
        val supportedComicTypes = listOf(
            ComicFileType.PDF.extension.lowercase(Locale.ROOT),
            ComicFileType.CBZ.extension.lowercase(Locale.ROOT),
            ComicFileType.CBR.extension.lowercase(Locale.ROOT)
        )

        val files = dir.listFiles()
        TimberLogger.logD(
            TAG,
            "[Comiqueta][SafFolderScanWorker] Starting recursive scan for directory: ${dir.name} (Contains ${files.size} files/dirs)"
        )

        var comicsFoundInDir = 0
        for (file in files) {
            if (file.isDirectory) {
                TimberLogger.logD(
                    TAG, "[Comiqueta][SafFolderScanWorker] Found subdirectory: ${file.name}, recursing..."
                )
                processed = scanDocumentFileRecursively(
                    file,
                    processed,
                    totalFiles,
                    totalComicsProcessedCount,
                    batch,
                    onProgressUpdate
                )
            } else {
                processed++
                val total = totalFiles.get()
                if (total > 0) {
                    val progressValue = (processed * 100 / total).coerceAtMost(100)
                    if (progressValue % 10 == 0 && (processed * 100 / total) != ((processed - 1).coerceAtLeast(
                            0
                        ) * 100 / total)
                    ) {
                        TimberLogger.logI(
                            TAG,
                            "[Comiqueta][SafFolderScanWorker] Scanning progress: $progressValue% ($processed/$total files)"
                        )
                    }
                    onProgressUpdate(
                        progressValue,
                        null,
                        processed,
                        total,
                        totalComicsProcessedCount.get()
                    )
                } else {
                    onProgressUpdate(0, null, processed, 0, totalComicsProcessedCount.get())
                }

                val fileName = file.name ?: ""
                val fileUri = file.uri
                val fileExtension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)

                TimberLogger.logD(
                    TAG, "[Comiqueta][SafFolderScanWorker] Checking file: $fileName, Ext: $fileExtension, URI: $fileUri"
                )

                if (supportedComicTypes.contains(fileExtension)) {
                    comicsFoundInDir++
                    val comicTitle = fileName.substringBeforeLast('.', fileName)
                    val currentTotal = totalFiles.get()
                    val comicProgress =
                        if (currentTotal > 0) (processed * 100 / currentTotal).coerceAtMost(100) else 0
                    onProgressUpdate(
                        comicProgress,
                        comicTitle,
                        processed,
                        currentTotal,
                        totalComicsProcessedCount.get() + 1
                    ) // +1 because we are about to increment it
                    var coverImageUri: Uri? = null
                    var metadata = ComicMetadata()

                    val existingComic = comicsDao.getComicByFilePath(fileUri)
                    val fileLastModified = file.lastModified()

                    var needsExtraction = true
                    if (existingComic != null && existingComic.lastModified == fileLastModified) {
                        val currentCoverPath = existingComic.coverPath
                        if (currentCoverPath != null) {
                            val coverFile = File(currentCoverPath.path ?: "")
                            if (coverFile.exists()) {
                                coverImageUri = currentCoverPath
                                needsExtraction = false
                                TimberLogger.logD(
                                    TAG,
                                    "[Comiqueta][SafFolderScanWorker] Skipping thumbnail extraction for $fileName (already exists and valid)"
                                )
                            }
                        }
                    }

                    if (needsExtraction) {
                        TimberLogger.logD(TAG, "[Comiqueta][SafFolderScanWorker] extracting cover/metadata file=${LogRedaction.fileName(fileName)}")
                        if (fileExtension == "pdf" || fileExtension == "cbz" || fileExtension == "cbr") {
                            coverImageUri = extractAndSaveCoverImage(file, fileExtension)
                        }
                        if (fileExtension == "cbz") {
                            metadata = extractMetadataFromArchive(file, fileExtension)
                        }
                    }

                    val comicToSave: ComicEntity = existingComic?.copy(
                        title = comicTitle,
                        fileName = fileName,
                        folderPath = dir.uri,
                        coverPath = coverImageUri ?: existingComic.coverPath,
                        author = metadata.author ?: existingComic.author,
                        volume = metadata.volume ?: existingComic.volume,
                        number = metadata.number ?: existingComic.number,
                        year = metadata.year ?: existingComic.year,
                        lastModified = fileLastModified
                    )
                        ?: ComicEntity(
                            filePath = fileUri,
                            title = comicTitle,
                            fileName = fileName,
                            folderPath = dir.uri,
                            coverPath = coverImageUri,
                            author = metadata.author,
                            volume = metadata.volume,
                            number = metadata.number,
                            year = metadata.year,
                            lastModified = fileLastModified
                        )

                    if (totalComicsProcessedCount.incrementAndGet() <= UNBATCHED_SAVE_THRESHOLD) {
                        try {
                            comicsDao.insertComicsAndFts(listOf(comicToSave))
                            TimberLogger.logD(
                                TAG,
                                "[Comiqueta][SafFolderScanWorker] Immediate insert for comic #${totalComicsProcessedCount.get()}: $fileName"
                            )
                        } catch (ex: Exception) {
                            FirebaseCrashlytics.getInstance().recordException(ex)
                            TimberLogger.logE(TAG, "[Comiqueta][SafFolderScanWorker] Error in immediate insert: ${ex.message}", ex)
                        }
                    } else {
                        batch.add(comicToSave)

                        if (batch.size >= BATCH_SIZE_AFTER_THRESHOLD) {
                            try {
                                comicsDao.insertComicsAndFts(batch)
                                TimberLogger.logD(
                                    TAG,
                                    "[Comiqueta][SafFolderScanWorker] Batch insert of ${batch.size} comics (Total processed: ${totalComicsProcessedCount.get()})."
                                )
                                batch.clear()
                            } catch (ex: Exception) {
                                FirebaseCrashlytics.getInstance().recordException(ex)
                                TimberLogger.logE(TAG, "[Comiqueta][SafFolderScanWorker] Error in batch insert: ${ex.message}", ex)
                            }
                        }
                    }
                } else {
                    TimberLogger.logD(
                        TAG, "[Comiqueta][SafFolderScanWorker] Skipping non-comic file: $fileName (ext: $fileExtension)"
                    )
                }
            }
        }
        if (comicsFoundInDir > 0) {
            TimberLogger.logI(TAG, "[Comiqueta][SafFolderScanWorker] found $comicsFoundInDir comic(s) in dir=${LogRedaction.fileName(dir.name)}")
        }
        return processed
    }

    private suspend fun extractAndSaveCoverImage(comicFile: DocumentFile, extension: String): Uri? {
        return withContext(Dispatchers.IO) {
            var localScaledBitmap: Bitmap? = null
            var coverUri: Uri? = null

            try {
                when (extension) {
                    "pdf" -> {
                        appContext.contentResolver.openFileDescriptor(comicFile.uri, "r")
                            ?.use { pfd ->
                                PdfRenderer(pfd).use { renderer ->
                                    if (renderer.pageCount > 0) {
                                        renderer.openPage(0).use { page ->
                                            // Render directly at thumbnail size to save memory
                                            val bitmap = createBitmap(
                                                CoverUtils.THUMBNAIL_WIDTH,
                                                CoverUtils.THUMBNAIL_HEIGHT
                                            )
                                            page.render(
                                                bitmap,
                                                null,
                                                null,
                                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                            )
                                            localScaledBitmap = bitmap
                                        }
                                    }
                                }
                            }
                    }

                    "cbz" -> {
                        val archiverNameString = ArchiveStreamFactory.ZIP
                        val imageEntries = mutableListOf<String>()
                        var errorInListing = false
                        try {
                            appContext.contentResolver.openInputStream(comicFile.uri)
                                ?.use { inputStream ->
                                    BufferedInputStream(inputStream).use { bufferedInputStream ->
                                        val archiveStreamForListing =
                                            ArchiveStreamFactory().createArchiveInputStream(
                                                archiverNameString, bufferedInputStream
                                            ) as ArchiveInputStream<out ArchiveEntry>
                                        archiveStreamForListing.use { ais ->
                                            var entry: ArchiveEntry? = ais.nextEntry
                                            while (entry != null) {
                                                if (!entry.isDirectory && isImageFile(entry.name)) {
                                                    imageEntries.add(entry.name)
                                                }
                                                entry = ais.nextEntry
                                            }
                                        }
                                    }
                                }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            FirebaseCrashlytics.getInstance().recordException(ex)
                            TimberLogger.logE(
                                TAG,
                                "[Comiqueta][SafFolderScanWorker] Error listing entries in archive ${comicFile.name} (ext: $extension): ${ex.message}",
                                ex
                            )
                            errorInListing = true
                        }

                        if (errorInListing.not() && imageEntries.isNotEmpty()) {
                            imageEntries.sortWith(AlphanumComparator())
                            val firstImageName = imageEntries.first()
                            try {
                                appContext.contentResolver.openInputStream(comicFile.uri)
                                    ?.use { newInputStream ->
                                        BufferedInputStream(newInputStream).use { newBufferedStream ->
                                            val archiveStreamForExtraction =
                                                ArchiveStreamFactory().createArchiveInputStream(
                                                    archiverNameString, newBufferedStream
                                                ) as ArchiveInputStream<out ArchiveEntry>
                                            archiveStreamForExtraction.use { ais ->
                                                var currentEntry: ArchiveEntry? = ais.nextEntry
                                                while (currentEntry != null) {
                                                    if (currentEntry.name == firstImageName) {
                                                        val originalBitmap =
                                                            BitmapFactory.decodeStream(ais)
                                                        if (originalBitmap != null) {
                                                            localScaledBitmap =
                                                                originalBitmap.scale(
                                                                    CoverUtils.THUMBNAIL_WIDTH,
                                                                    CoverUtils.THUMBNAIL_HEIGHT
                                                                )
                                                            originalBitmap.recycle()
                                                        }
                                                        break
                                                    }
                                                    currentEntry = ais.nextEntry
                                                }
                                            }
                                        }
                                    }
                            } catch (ex: Exception) {
                                FirebaseCrashlytics.getInstance().recordException(ex)
                                TimberLogger.logE(
                                    TAG,
                                    "[Comiqueta][SafFolderScanWorker] Error extracting first image from archive ${comicFile.name} (ext: $extension): ${ex.message}",
                                    ex
                                )
                            }
                        }
                    }

                    "cbr" -> {
                        val imageFileHeaders = mutableListOf<JunrarFileHeader>()
                        var errorInListing = false
                        try {
                            appContext.contentResolver.openInputStream(comicFile.uri)
                                ?.use { inputStream ->
                                    BufferedInputStream(inputStream).use { bufferedInputStream ->
                                        JunrarArchive(bufferedInputStream).use { archive ->
                                            for (fileHeader in archive.fileHeaders) {
                                                if (!fileHeader.isDirectory && isImageFile(
                                                        fileHeader.fileName
                                                    )
                                                ) {
                                                    imageFileHeaders.add(fileHeader)
                                                }
                                            }
                                        }
                                    }
                                }
                        } catch (ex: Exception) {
                            FirebaseCrashlytics.getInstance().recordException(ex)
                            TimberLogger.logE(
                                TAG,
                                "[Comiqueta][SafFolderScanWorker] Error listing entries in CBR (junrar) ${comicFile.name}: ${ex.message}",
                                ex
                            )
                            errorInListing = true
                        }

                        if (!errorInListing && imageFileHeaders.isNotEmpty()) {
                            imageFileHeaders.sortWith(Comparator { h1, h2 ->
                                AlphanumComparator().compare(h1.fileName, h2.fileName)
                            })
                            val firstImageHeader = imageFileHeaders.first()

                            try {
                                appContext.contentResolver.openInputStream(comicFile.uri)
                                    ?.use { extractionInputStream ->
                                        BufferedInputStream(extractionInputStream).use { bufferedExtractionStream ->
                                            JunrarArchive(bufferedExtractionStream).use { archiveForExtract ->
                                                val headerToExtract =
                                                    archiveForExtract.fileHeaders.find { it.fileName == firstImageHeader.fileName }
                                                if (headerToExtract != null) {
                                                    ByteArrayOutputStream().use { baos ->
                                                        archiveForExtract.extractFile(
                                                            headerToExtract, baos
                                                        )
                                                        val imageBytes = baos.toByteArray()

                                                        // Use inSampleSize to load a downsampled version
                                                        val options =
                                                            BitmapFactory.Options().apply {
                                                                inJustDecodeBounds = true
                                                            }
                                                        BitmapFactory.decodeByteArray(
                                                            imageBytes,
                                                            0,
                                                            imageBytes.size,
                                                            options
                                                        )

                                                        options.inSampleSize =
                                                            calculateInSampleSize(
                                                                options,
                                                                CoverUtils.THUMBNAIL_WIDTH,
                                                                CoverUtils.THUMBNAIL_HEIGHT
                                                            )
                                                        options.inJustDecodeBounds = false

                                                        val downsampledBitmap =
                                                            BitmapFactory.decodeByteArray(
                                                                imageBytes,
                                                                0,
                                                                imageBytes.size,
                                                                options
                                                            )
                                                        if (downsampledBitmap != null) {
                                                            localScaledBitmap =
                                                                downsampledBitmap.scale(
                                                                    CoverUtils.THUMBNAIL_WIDTH,
                                                                    CoverUtils.THUMBNAIL_HEIGHT
                                                                )
                                                            if (downsampledBitmap != localScaledBitmap) {
                                                                downsampledBitmap.recycle()
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    TimberLogger.logW(
                                                        TAG,
                                                        "[Comiqueta][SafFolderScanWorker] Could not find header '${firstImageHeader.fileName}' again in CBR for extraction."
                                                    )
                                                }
                                            }
                                        }
                                    }
                            } catch (ex: Exception) {
                                FirebaseCrashlytics.getInstance().recordException(ex)
                                TimberLogger.logE(
                                    TAG,
                                    "[Comiqueta][SafFolderScanWorker] Error extracting first image from CBR (junrar) ${comicFile.name}: ${ex.message} (BufferedInputStream fix applied)",
                                    ex
                                )
                            }
                        }
                    }
                }

                localScaledBitmap?.let { bmp ->
                    coverUri = CoverUtils.saveBitmapToCache(
                        appContext,
                        bmp,
                        comicFile.name ?: "unknown_comic_${System.currentTimeMillis()}"
                    )
                }

            } catch (ex: Exception) {
                FirebaseCrashlytics.getInstance().recordException(ex)
                TimberLogger.logE(
                    TAG, "[Comiqueta][SafFolderScanWorker] General error extracting cover for ${comicFile.name}: ${ex.message}", ex
                )
                localScaledBitmap = null
            } finally {
                localScaledBitmap?.recycle()
            }
            return@withContext coverUri
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private data class ComicMetadata(
        val author: String? = null,
        val volume: String? = null,
        val number: String? = null,
        val year: Int? = null
    )

    private fun extractMetadataFromArchive(
        comicFile: DocumentFile,
        extension: String
    ): ComicMetadata {
        if (extension != "cbz") return ComicMetadata()

        try {
            val resolver = appContext.contentResolver
            resolver.openInputStream(comicFile.uri)?.use { inputStream ->
                BufferedInputStream(inputStream).use { bufferedInputStream ->
                    val factory = ArchiveStreamFactory()
                    val ais: ArchiveInputStream<*> = factory.createArchiveInputStream(
                        ArchiveStreamFactory.ZIP, bufferedInputStream
                    )
                    try {
                        var entry: ArchiveEntry? = ais.nextEntry
                        while (entry != null) {
                            if (entry.name.equals("ComicInfo.xml", ignoreCase = true)) {
                                return parseComicInfo(ais)
                            }
                            entry = ais.nextEntry
                        }
                    } finally {
                        try {
                            ais.close()
                        } catch (e: Exception) {
                            FirebaseCrashlytics.getInstance().recordException(e)
                            TimberLogger.logE(
                                SafFolderScanWorker::class.simpleName,
                                e.message ?: "",
                                e
                            )
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            TimberLogger.logE(
                TAG,
                "[Comiqueta][SafFolderScanWorker] Error extracting metadata from ${comicFile.name}: ${ex.message}",
                ex
            )
        }
        return ComicMetadata()
    }

    private fun parseComicInfo(inputStream: java.io.InputStream): ComicMetadata {
        var author: String? = null
        var volume: String? = null
        var number: String? = null
        var year: Int? = null

        try {
            val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "Writer", "Author" -> {
                            try {
                                author = parser.nextText()
                            } catch (e: Exception) {
                                TimberLogger.logE(
                                    SafFolderScanWorker::class.simpleName,
                                    e.message ?: "",
                                    e
                                )
                                author = null
                            }
                        }

                        "Volume" -> {
                            try {
                                volume = parser.nextText()
                            } catch (e: Exception) {
                                TimberLogger.logE(
                                    SafFolderScanWorker::class.simpleName,
                                    e.message ?: "",
                                    e
                                )
                                volume = null
                            }
                        }

                        "Number" -> {
                            try {
                                number = parser.nextText()
                            } catch (e: Exception) {
                                TimberLogger.logE(
                                    SafFolderScanWorker::class.simpleName,
                                    e.message ?: "",
                                    e
                                )
                                number = null
                            }
                        }

                        "Year" -> {
                            try {
                                year = parser.nextText()?.toIntOrNull()
                            } catch (e: Exception) {
                                TimberLogger.logE(
                                    SafFolderScanWorker::class.simpleName,
                                    e.message ?: "",
                                    e
                                )
                                year = null
                            }
                        }
                    }
                }
                try {
                    eventType = parser.next()
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                    TimberLogger.logE(SafFolderScanWorker::class.simpleName, e.message ?: "", e)
                    eventType = org.xmlpull.v1.XmlPullParser.END_DOCUMENT
                }
            }
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            TimberLogger.logE(TAG, "[Comiqueta][SafFolderScanWorker] Error parsing ComicInfo.xml: ${ex.message}", ex)
        }
        return ComicMetadata(author, volume, number, year)
    }

    private fun isImageFile(fileName: String?): Boolean {
        val name = fileName ?: return false
        val lowerName = name.lowercase(Locale.ROOT)
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(
            ".webp"
        ) || lowerName.endsWith(".gif")
    }


    class AlphanumComparator : Comparator<String> {
        private fun isDigit(char: Char): Boolean = char.isDigit()

        private fun getChunk(s: String, marker: Int): String {
            val slength = s.length
            var m = marker
            val chunk = StringBuilder()
            var c = s[m]
            chunk.append(c)
            m++
            if (isDigit(c)) {
                while (m < slength) {
                    c = s[m]
                    if (!isDigit(c)) break
                    chunk.append(c)
                    m++
                }
            } else {
                while (m < slength) {
                    c = s[m]
                    if (isDigit(c)) break
                    chunk.append(c)
                    m++
                }
            }
            return chunk.toString()
        }

        override fun compare(s1: String?, s2: String?): Int {
            val str1 = s1 ?: return if (s2 == null) 0 else -1
            val str2 = s2 ?: return 1

            var thisMarker = 0
            var thatMarker = 0
            val s1Length = str1.length
            val s2Length = str2.length

            while (thisMarker < s1Length && thatMarker < s2Length) {
                val thisChunk = getChunk(str1, thisMarker)
                thisMarker += thisChunk.length

                val thatChunk = getChunk(str2, thatMarker)
                thatMarker += thatChunk.length

                var result: Int
                if (isDigit(thisChunk[0]) && isDigit(thatChunk[0])) {
                    try {
                        val thisNum = BigInteger(thisChunk)
                        val thatNum = BigInteger(thatChunk)
                        result = thisNum.compareTo(thatNum)
                    } catch (nfe: NumberFormatException) {
                        FirebaseCrashlytics.getInstance().recordException(nfe)
                        TimberLogger.logE(
                            TAG, "[Comiqueta][SafFolderScanWorker]  Error comparing chunks : $thisChunk, $thatChunk", nfe
                        )
                        result = thisChunk.compareTo(thatChunk)
                    }
                } else {
                    result = thisChunk.compareTo(thatChunk)
                }

                if (result != 0) return result
            }
            return s1Length - s2Length
        }
    }
}
