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
import com.github.junrar.Archive as JunrarArchive
import com.github.junrar.rarfile.FileHeader as JunrarFileHeader
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsDao
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.repository.ComicsFolderRepository
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
    private val comicsFolderRepository: ComicsFolderRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val THUMBNAIL_WIDTH = 300
        private const val THUMBNAIL_HEIGHT = 450
        private const val COVERS_DIR_NAME = "covers"
        private val tag = SafFolderScanWorker::class.simpleName
    }

    override suspend fun doWork(): Result {
        TimberLogger.logD(tag, "Starting scheduled scan of persisted folders.")

        val folderUrisToScan = try {
            comicsFolderRepository.getPersistedPermissions()
        } catch (e: Exception) {
            TimberLogger.logE(
                tag,
                "Error fetching persisted folders from repository.",
                e
            )
            return Result.failure()
        }

        if (folderUrisToScan.isEmpty()) {
            TimberLogger.logI(tag, "No persisted folders found to scan.")
            return Result.success()
        }

        TimberLogger.logI(tag, "Found ${folderUrisToScan.size} folders to scan.")
        var anyFolderScanFailed = false

        for (folderUri in folderUrisToScan) {
            TimberLogger.logD(tag, "Starting scan for folder URI: $folderUri")
            val rootDoc = try {
                DocumentFile.fromTreeUri(appContext, folderUri)
            } catch (e: Exception) {
                TimberLogger.logE(
                    tag,
                    "Error creating DocumentFile from URI: $folderUri",
                    e
                )
                anyFolderScanFailed = true
                continue
            }

            if (rootDoc == null || !rootDoc.isDirectory) {
                TimberLogger.logW(
                    tag,
                    "Root document is null or not a directory. URI: $folderUri"
                )
                anyFolderScanFailed = true
                continue
            }

            try {
                TimberLogger.logD(
                    tag,
                    "Scanning document tree for: ${rootDoc.name} (URI: $folderUri)"
                )
                scanDocumentFileForComics(rootDoc)
                TimberLogger.logD(tag, "Scan finished for URI: $folderUri")
            } catch (e: Exception) {
                TimberLogger.logE(
                    tag,
                    "Exception during scan of folder: ${rootDoc.name} (URI: $folderUri)",
                    e
                )
                anyFolderScanFailed = true
            }
        }

        TimberLogger.logI(
            tag,
            "Finished processing all folders. Any folder scan failed: $anyFolderScanFailed"
        )
        return if (anyFolderScanFailed) Result.retry() else Result.success() // Consider retry if specific failures occurred
    }

    private suspend fun scanDocumentFileForComics(dir: DocumentFile) {
        TimberLogger.logD(tag, "Scanning directory: ${dir.name}")
        val supportedComicTypes = listOf(
            ComicFileType.PDF.extension.lowercase(Locale.ROOT),
            ComicFileType.CBZ.extension.lowercase(Locale.ROOT),
            ComicFileType.CBR.extension.lowercase(Locale.ROOT)
        )

        for (file in dir.listFiles()) {
            if (file.isDirectory) {
                TimberLogger.logD(
                    tag,
                    "Found subdirectory: ${file.name}, recursing..."
                )
                scanDocumentFileForComics(file)
            } else {
                val fileName = file.name ?: ""
                val fileUri = file.uri
                val fileExtension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)

                TimberLogger.logD(
                    tag,
                    "Checking file: $fileName, Ext: $fileExtension, URI: $fileUri"
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
                            coverPath = coverImageUri ?: existingComic.coverPath,
                            isNew = if (coverImageUri != null && existingComic.coverPath != coverImageUri) true else existingComic.isNew,
                            lastModified = file.lastModified() // Update last modified
                        )
                        TimberLogger.logD(
                            tag,
                            "Updating existing comic: ${comicToSave.title}"
                        )
                    } else {
                        comicToSave = ComicEntity(
                            filePath = fileUri,
                            title = comicTitle,
                            coverPath = coverImageUri,
                            isNew = true,
                            lastModified = file.lastModified()
                        )
                        TimberLogger.logI(
                            tag,
                            "Found new comic: ${comicToSave.title}. Saving to DB."
                        )
                    }

                    try {
                        comicsDao.insertComic(comicToSave)
                        TimberLogger.logD(
                            tag,
                            "Successfully saved/updated comic: ${comicToSave.title}"
                        )
                    } catch (e: Exception) {
                        TimberLogger.logE(
                            tag,
                            "Error saving/updating comic ${comicToSave.title}: ${e.message}",
                            e
                        )
                    }
                } else {
                    TimberLogger.logD(
                        tag,
                        "Skipping non-comic file: $fileName (ext: $fileExtension)"
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
                                                THUMBNAIL_WIDTH,
                                                THUMBNAIL_HEIGHT
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
                                                archiverNameString,
                                                bufferedInputStream
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
                            TimberLogger.logE(
                                tag,
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
                                                    archiverNameString,
                                                    newBufferedStream
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
                                TimberLogger.logE(
                                    tag,
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
                            TimberLogger.logE(
                                tag,
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
                                // Re-open stream for extraction if needed, or if junrar Archive allows re-access
                                // For safety, re-opening is shown if the stream was consumed
                                appContext.contentResolver.openInputStream(comicFile.uri)
                                    ?.use { extractionInputStream ->
                                        JunrarArchive(extractionInputStream).use { archiveForExtract ->
                                            // Find the header again in this new archive instance, as header instances might not be reusable across Archive instances
                                            val headerToExtract =
                                                archiveForExtract.fileHeaders.find { it.fileName == firstImageHeader.fileName }
                                            if (headerToExtract != null) {
                                                ByteArrayOutputStream().use { baos ->
                                                    archiveForExtract.extractFile(
                                                        headerToExtract,
                                                        baos
                                                    )
                                                    val imageBytes = baos.toByteArray()
                                                    val originalBitmap =
                                                        BitmapFactory.decodeByteArray(
                                                            imageBytes,
                                                            0,
                                                            imageBytes.size
                                                        )
                                                    if (originalBitmap != null) {
                                                        localScaledBitmap = originalBitmap.scale(
                                                            THUMBNAIL_WIDTH,
                                                            THUMBNAIL_HEIGHT
                                                        )
                                                        originalBitmap.recycle()
                                                    }
                                                }
                                            } else {
                                                TimberLogger.logW(
                                                    tag,
                                                    "Could not find header '${firstImageHeader.fileName}' again in CBR for extraction."
                                                )
                                            }
                                        }
                                    }
                            } catch (e: Exception) {
                                TimberLogger.logE(
                                    tag,
                                    "Error extracting first image from CBR (junrar) ${comicFile.name}: ${e.message}",
                                    e
                                )
                            }
                        }
                    }
                } // End of when

                localScaledBitmap?.let { bmp ->
                    coverFile = saveBitmapToCache(
                        bmp,
                        comicFile.name ?: "unknown_comic_${System.currentTimeMillis()}"
                    )
                }

            } catch (e: Exception) {
                TimberLogger.logE(
                    tag,
                    "General error extracting cover for ${comicFile.name}: ${e.message}",
                    e
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
        // Added .gif as it's sometimes used in comic archives
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".webp") ||
                lowerName.endsWith(".gif")
    }

    private fun saveBitmapToCache(bitmap: Bitmap, originalFileName: String): File? {
        val coversDir = File(appContext.cacheDir, COVERS_DIR_NAME)
        if (!coversDir.exists() && !coversDir.mkdirs()) {
            TimberLogger.logE(
                tag,
                "Failed to create covers directory: ${coversDir.absolutePath}"
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
            TimberLogger.logD(tag, "Saved cover to: ${imageFile.absolutePath}")
            return imageFile
        } catch (e: IOException) {
            TimberLogger.logE(
                tag,
                "Error saving bitmap to cache: ${e.message}",
                e
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
            if (s1 == null) return -1 // nulls first
            if (s2 == null) return 1  // nulls first

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
                    // Numerical comparison for digit chunks
                    try {
                        val thisNum = BigInteger(thisChunk)
                        val thatNum = BigInteger(thatChunk)
                        result = thisNum.compareTo(thatNum)
                    } catch (ex: NumberFormatException) {
                        TimberLogger.logE(
                            tag,
                            " Error comparing chunks : $thisChunk, $thatChunk",
                            ex
                        )
                        // Fallback to string comparison if not a valid number (shouldn't happen with isDigit check)
                        result = thisChunk.compareTo(thatChunk)
                    }
                } else {
                    // Lexicographical comparison for non-digit chunks
                    result = thisChunk.compareTo(thatChunk)
                }

                if (result != 0) return result
            }
            return s1Length - s2Length
        }
    }
}
