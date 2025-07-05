package dev.diegoflassa.comiqueta.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker // Changed from Worker
import androidx.work.WorkerParameters
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
// Removed unused import: import androidx.work.Worker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsDao
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.model.ComicFileType

@HiltWorker
class SafFolderScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val comicsDao: ComicsDao
) : CoroutineWorker(appContext, workerParams) { // Changed to CoroutineWorker

    override suspend fun doWork(): Result { // Added suspend
        val folderUriString = inputData.getString("folderUri") ?: return Result.failure()
        TimberLogger.logD("SafFolderScanWorker", "Starting scan for folder URI: $folderUriString")
        val folderUri = folderUriString.toUri()
        val applicationContext =
            applicationContext // Use the applicationContext from CoroutineWorker

        // It's crucial that applicationContext is used for DocumentFile.fromTreeUri
        // if this worker might live longer than an Activity/Service context.
        val rootDoc = DocumentFile.fromTreeUri(applicationContext, folderUri)

        if (rootDoc == null || !rootDoc.isDirectory) {
            TimberLogger.logE(
                "SafFolderScanWorker",
                "Root document is null or not a directory. URI: $folderUri"
            )
            return Result.failure()
        }

        TimberLogger.logD("SafFolderScanWorker", "Scanning document tree for: ${rootDoc.name}")
        scanDocumentFileForComics(rootDoc) // Now calling a suspend fun

        TimberLogger.logD("SafFolderScanWorker", "Scan finished for URI: $folderUriString")
        return Result.success()
    }

    private suspend fun scanDocumentFileForComics(dir: DocumentFile?) { // Added suspend
        // The dir.isDirectory.not() was problematic, simplified to !dir.isDirectory
        if (dir == null || !dir.isDirectory) {
            if (dir == null) TimberLogger.logW(
                "SafFolderScanWorker",
                "dir is null in scanDocumentFileForComics"
            )
            else TimberLogger.logW(
                "SafFolderScanWorker",
                "dir is not a directory: ${dir.name}, type: ${dir.type}"
            )
            return
        }

        TimberLogger.logD("SafFolderScanWorker", "Scanning directory: ${dir.name}")
        val comicExtensions = ComicFileType.getValuesAsString(includeImages = false)

        // listFiles() can return null, so handle that with a null check or elvis operator
        dir.listFiles().forEach { file ->
            if (file.isDirectory) {
                TimberLogger.logD(
                    "SafFolderScanWorker",
                    "Found subdirectory: ${file.name}, recursing..."
                )
                scanDocumentFileForComics(file) // Recursive call to suspend fun
            } else {
                val name = file.name ?: ""
                val fileUriString = file.uri.toString()
                TimberLogger.logD(
                    "SafFolderScanWorker",
                    "Checking file: $name, URI: $fileUriString"
                )
                if (comicExtensions.any { extension ->
                        name.endsWith(
                            ".$extension",
                            ignoreCase = true
                        )
                    }) {
                    TimberLogger.logI("SafFolderScanWorker", "Found comic: $name. Saving to DB.")
                    val comicEntity = ComicEntity(
                        filePath = file.uri,
                        title = file.name?.let { it.substringBeforeLast('.', it) }
                            ?: "Unknown Title",
                        // Other fields will use defaults: coverPath=null, genre=null, isFavorite=false, isNew=true, hasBeenRead=false
                    )
                    try {
                        comicsDao.insertComic(comicEntity) // This is a suspend function
                        TimberLogger.logD("SafFolderScanWorker", "Successfully inserted: $name")
                    } catch (e: Exception) {
                        TimberLogger.logE(
                            "SafFolderScanWorker",
                            "Error inserting comic $name: ${e.message}",
                            e
                        )
                    }
                } else {
                    TimberLogger.logD("SafFolderScanWorker", "Skipping non-comic file: $name")
                }
            }
        }
    }
}