package dev.diegoflassa.comiqueta.core.data.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile

class SafFolderScanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val folderUriString = inputData.getString("folderUri") ?: return Result.failure()
        val folderUri = folderUriString.toUri()
        val comicFiles = mutableListOf<Uri>()
        val rootDoc = DocumentFile.fromTreeUri(applicationContext, folderUri)
        scanDocumentFileForComics(rootDoc, comicFiles)

        // TODO: Save or notify ViewModel/DB about new comic files
        // You can use WorkManager's output or other IPC mechanism (e.g. Broadcast, LiveData, etc.)

        return Result.success()
    }

    private fun scanDocumentFileForComics(dir: DocumentFile?, outList: MutableList<Uri>) {
        if (dir == null || !dir.isDirectory) return
        val comicExtensions = listOf("cbz", "cbr", "cbt")
        dir.listFiles().forEach { file ->
            if (file.isDirectory) {
                scanDocumentFileForComics(file, outList)
            } else {
                val name = file.name ?: ""
                if (comicExtensions.any { name.endsWith(".$it", ignoreCase = true) }) {
                    outList.add(file.uri)
                }
            }
        }
    }
}
