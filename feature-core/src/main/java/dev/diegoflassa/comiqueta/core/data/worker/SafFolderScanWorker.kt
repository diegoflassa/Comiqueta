package dev.diegoflassa.comiqueta.core.data.worker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.github.junrar.Archive as JunrarArchive
import com.github.junrar.rarfile.FileHeader as JunrarFileHeader
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsDao
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.repository.IComicsFolderRepository
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.model.ComicFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigInteger
import java.util.Locale

@HiltWorker
class SafFolderScanWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val comicsDao: ComicsDao,
    private val comicsFolderRepository: IComicsFolderRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val THUMBNAIL_WIDTH = 300
        private const val THUMBNAIL_HEIGHT = 450
        private const val COVERS_DIR_NAME = "covers"
        const val TAG = "SafFolderScanWorker"
        const val KEY_ERROR_MESSAGE = "key_error_message"
        const val KEY_FOLDER_URI = "key_folder_uri"
    }

    override suspend fun doWork(): Result {
        var firstErrorMessage: String? = null
        val specificFolderUriString = inputData.getString(KEY_FOLDER_URI)

        val folderUrisToScan: List<Uri> = if (specificFolderUriString != null) {
            TimberLogger.logD(
                TAG,
                "Starting specific scan for folder URI: $specificFolderUriString"
            )
            try {
                listOf(specificFolderUriString.toUri())
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                val errorMessage = "Invalid specific folder URI provided: $specificFolderUriString"
                TimberLogger.logE(TAG, errorMessage, e)
                val outputData = workDataOf(KEY_ERROR_MESSAGE to errorMessage)
                return Result.failure(outputData)
            }
        } else {
            TimberLogger.logD(TAG, "Starting general scan of all persisted folders.")
            try {
                comicsFolderRepository.getPersistedPermissions()
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                val errorMessage = "Error fetching persisted folders for general scan."
                TimberLogger.logE(TAG, "$errorMessage Repository error.", e)
                val outputData = workDataOf(KEY_ERROR_MESSAGE to errorMessage)
                return Result.failure(outputData)
            }
        }

        if (folderUrisToScan.isEmpty()) {
            TimberLogger.logI(TAG, "No folders found to scan.")
            return Result.success()
        }

        TimberLogger.logI(TAG, "Found ${folderUrisToScan.size} folder(s) to scan.")
        var anyFolderScanFailed = false

        for (folderUri in folderUrisToScan) {
            TimberLogger.logD(TAG, "Processing folder URI: $folderUri")
            val rootDoc = try {
                DocumentFile.fromTreeUri(appContext, folderUri)
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                val errorMessage = "Error accessing folder: $folderUri"
                TimberLogger.logE(TAG, errorMessage, e)
                if (firstErrorMessage == null) firstErrorMessage =
                    "Error accessing folder. Please check permissions for $folderUri."
                anyFolderScanFailed = true
                continue
            }

            if (rootDoc == null || !rootDoc.isDirectory) {
                val errorMessage = "Folder not valid or not a directory: $folderUri"
                FirebaseCrashlytics.getInstance().recordException(Exception(errorMessage))
                TimberLogger.logW(TAG, errorMessage)
                if (firstErrorMessage == null) firstErrorMessage =
                    "A configured folder is not valid. URI: $folderUri"
                anyFolderScanFailed = true
                continue
            }

            try {
                TimberLogger.logD(
                    TAG, "Scanning document tree for: ${rootDoc.name} (URI: $folderUri)"
                )
                scanDocumentFileForComics(rootDoc)
                TimberLogger.logD(TAG, "Scan finished for URI: $folderUri")
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                val scanErrorMessage = "Error scanning folder: ${rootDoc.name}"
                TimberLogger.logE(TAG, "$scanErrorMessage (URI: $folderUri)", e)
                if (firstErrorMessage == null) firstErrorMessage = scanErrorMessage
                anyFolderScanFailed = true
            }
        }

        TimberLogger.logI(
            TAG, "Finished processing all folder(s). Any folder scan failed: $anyFolderScanFailed"
        )

        return if (anyFolderScanFailed) {
            val finalErrorMessage = firstErrorMessage ?: "One or more folders failed to scan."
            TimberLogger.logW(TAG, "Scan failed. Reporting message: $finalErrorMessage")
            val outputData = workDataOf(KEY_ERROR_MESSAGE to finalErrorMessage)
            Result.failure(outputData)
        } else {
            TimberLogger.logI(
                TAG,
                "Scan completed successfully for all processed folder(s).${if (specificFolderUriString != null) " URI: $specificFolderUriString" else ""}"
            )
            Result.success()
        }
    }

    private suspend fun scanDocumentFileForComics(dir: DocumentFile) {
        TimberLogger.logD(TAG, "Scanning directory: ${dir.name} (URI: ${dir.uri})")
        val supportedComicTypes = listOf(
            ComicFileType.PDF.extension.lowercase(Locale.ROOT),
            ComicFileType.CBZ.extension.lowercase(Locale.ROOT),
            ComicFileType.CBR.extension.lowercase(Locale.ROOT)
        )

        for (file in dir.listFiles()) {
            if (file.isDirectory) {
                TimberLogger.logD(
                    TAG, "Found subdirectory: ${file.name}, recursing..."
                )
                scanDocumentFileForComics(file)
            } else {
                val fileName = file.name ?: ""
                val fileUri = file.uri
                val fileExtension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)

                TimberLogger.logD(
                    TAG, "Checking file: $fileName, Ext: $fileExtension, URI: $fileUri"
                )

                if (supportedComicTypes.contains(fileExtension)) {
                    val comicTitle = fileName.substringBeforeLast('.', fileName)
                    var coverImageUri: Uri? = null

                    if (fileExtension == "pdf" || fileExtension == "cbz" || fileExtension == "cbr") {
                        coverImageUri = extractAndSaveCoverImage(file, fileExtension)
                    }

                    val existingComic = comicsDao.getComicByFilePath(fileUri)
                    val comicToSave: ComicEntity

                    if (existingComic != null) {
                        comicToSave = existingComic.copy(
                            title = comicTitle,
                            fileName = fileName,
                            folderPath = dir.uri,
                            coverPath = coverImageUri ?: existingComic.coverPath,
                            lastModified = file.lastModified()
                        )
                        TimberLogger.logD(
                            TAG,
                            "Updating existing comic: ${comicToSave.title} (URI: ${comicToSave.filePath})"
                        )
                    } else {
                        comicToSave = ComicEntity(
                            filePath = fileUri,
                            title = comicTitle,
                            fileName = fileName,
                            folderPath = dir.uri,
                            coverPath = coverImageUri,
                            lastModified = file.lastModified()
                        )
                        TimberLogger.logI(
                            TAG,
                            "Found new comic: ${comicToSave.title} (URI: ${comicToSave.filePath}). Saving to DB."
                        )
                    }

                    try {
                        comicsDao.insertComicAndFts(comicToSave)
                        TimberLogger.logD(
                            TAG, "Successfully saved/updated comic (and FTS): ${comicToSave.title}"
                        )
                    } catch (e: Exception) {
                        FirebaseCrashlytics.getInstance().recordException(e)
                        TimberLogger.logE(
                            TAG, "Error saving/updating comic ${comicToSave.title}: ${e.message}", e
                        )
                    }
                } else {
                    TimberLogger.logD(
                        TAG, "Skipping non-comic file: $fileName (ext: $fileExtension)"
                    )
                }
            }
        }
    }

    private suspend fun extractAndSaveCoverImage(comicFile: DocumentFile, extension: String): Uri? {
        return withContext(Dispatchers.IO) {
            var localScaledBitmap: Bitmap? = null
            var coverFile: File? = null

            try {
                when (extension) {
                    "pdf" -> {
                        appContext.contentResolver.openFileDescriptor(comicFile.uri, "r")
                            ?.use { pfd ->
                                PdfRenderer(pfd).use { renderer ->
                                    if (renderer.pageCount > 0) {
                                        renderer.openPage(0).use { page ->
                                            val originalBitmap =
                                                createBitmap(page.width, page.height)
                                            page.render(
                                                originalBitmap,
                                                null,
                                                null,
                                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                            )
                                            localScaledBitmap = originalBitmap.scale(
                                                THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT
                                            )
                                            originalBitmap.recycle()
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
                        } catch (e: Exception) {
                            FirebaseCrashlytics.getInstance().recordException(e)
                            TimberLogger.logE(
                                TAG,
                                "Error listing entries in archive ${comicFile.name} (ext: $extension): ${e.message}",
                                e
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
                                                                    THUMBNAIL_WIDTH,
                                                                    THUMBNAIL_HEIGHT
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
                            } catch (e: Exception) {
                                FirebaseCrashlytics.getInstance().recordException(e)
                                TimberLogger.logE(
                                    TAG,
                                    "Error extracting first image from archive ${comicFile.name} (ext: $extension): ${e.message}",
                                    e
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
                                    JunrarArchive(inputStream).use { archive ->
                                        for (fileHeader in archive.fileHeaders) {
                                            if (!fileHeader.isDirectory && isImageFile(fileHeader.fileName)) {
                                                imageFileHeaders.add(fileHeader)
                                            }
                                        }
                                    }
                                }
                        } catch (e: Exception) {
                            FirebaseCrashlytics.getInstance().recordException(e)
                            TimberLogger.logE(
                                TAG,
                                "Error listing entries in CBR (junrar) ${comicFile.name}: ${e.message}",
                                e
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
                                        JunrarArchive(extractionInputStream).use { archiveForExtract ->
                                            val headerToExtract =
                                                archiveForExtract.fileHeaders.find { it.fileName == firstImageHeader.fileName }
                                            if (headerToExtract != null) {
                                                ByteArrayOutputStream().use { baos ->
                                                    archiveForExtract.extractFile(
                                                        headerToExtract, baos
                                                    )
                                                    val imageBytes = baos.toByteArray()
                                                    val originalBitmap =
                                                        BitmapFactory.decodeByteArray(
                                                            imageBytes, 0, imageBytes.size
                                                        )
                                                    if (originalBitmap != null) {
                                                        localScaledBitmap = originalBitmap.scale(
                                                            THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT
                                                        )
                                                        originalBitmap.recycle()
                                                    }
                                                }
                                            } else {
                                                TimberLogger.logW(
                                                    TAG,
                                                    "Could not find header '${firstImageHeader.fileName}' again in CBR for extraction."
                                                )
                                            }
                                        }
                                    }
                            } catch (e: Exception) {
                                FirebaseCrashlytics.getInstance().recordException(e)
                                TimberLogger.logE(
                                    TAG,
                                    "Error extracting first image from CBR (junrar) ${comicFile.name}: ${e.message}",
                                    e
                                )
                            }
                        }
                    }
                }

                localScaledBitmap?.let { bmp ->
                    coverFile = saveBitmapToCache(
                        bmp, comicFile.name ?: "unknown_comic_${System.currentTimeMillis()}"
                    )
                }

            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                TimberLogger.logE(
                    TAG, "General error extracting cover for ${comicFile.name}: ${e.message}", e
                )
                localScaledBitmap = null
                coverFile = null
            } finally {
                localScaledBitmap?.recycle()
            }
            return@withContext coverFile?.toUri()
        }
    }

    private fun isImageFile(fileName: String?): Boolean {
        if (fileName == null) return false
        val lowerName = fileName.lowercase(Locale.ROOT)
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(
            ".webp"
        ) || lowerName.endsWith(".gif")
    }

    private fun saveBitmapToCache(bitmap: Bitmap, originalFileName: String): File? {
        val coversDir = File(appContext.cacheDir, COVERS_DIR_NAME)
        if (!coversDir.exists() && !coversDir.mkdirs()) {
            TimberLogger.logE(
                TAG, "Failed to create covers directory: ${coversDir.absolutePath}"
            )
            return null
        }
        val safeOriginalName = originalFileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val coverFileName = "cover_${safeOriginalName}_${System.currentTimeMillis()}.jpg"
        val imageFile = File(coversDir, coverFileName)

        try {
            FileOutputStream(imageFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }
            TimberLogger.logD(TAG, "Saved cover to: ${imageFile.absolutePath}")
            return imageFile
        } catch (e: IOException) {
            TimberLogger.logE(
                TAG, "Error saving bitmap to cache: ${e.message}", e
            )
        }
        return null
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
            if (s1 == null && s2 == null) return 0
            if (s1 == null) return -1
            if (s2 == null) return 1

            var thisMarker = 0
            var thatMarker = 0
            val s1Length = s1.length
            val s2Length = s2.length

            while (thisMarker < s1Length && thatMarker < s2Length) {
                val thisChunk = getChunk(s1, thisMarker)
                thisMarker += thisChunk.length

                val thatChunk = getChunk(s2, thatMarker)
                thatMarker += thatChunk.length

                var result: Int
                if (isDigit(thisChunk[0]) && isDigit(thatChunk[0])) {
                    try {
                        val thisNum = BigInteger(thisChunk)
                        val thatNum = BigInteger(thatChunk)
                        result = thisNum.compareTo(thatNum)
                    } catch (ex: NumberFormatException) {
                        TimberLogger.logE(
                            TAG, " Error comparing chunks : $thisChunk, $thatChunk", ex
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
