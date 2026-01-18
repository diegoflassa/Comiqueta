package dev.diegoflassa.comiqueta.core.data.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object CoverUtils {
    const val THUMBNAIL_WIDTH = 300
    const val THUMBNAIL_HEIGHT = 450
    const val COVERS_DIR_NAME = "covers"
    private const val TAG = "CoverUtils"

    fun saveBitmapToCache(context: Context, bitmap: Bitmap, originalFileName: String): Uri? {
        val coversDir = File(context.filesDir, COVERS_DIR_NAME)
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
            return imageFile.toUri()
        } catch (ioe: IOException) {
            TimberLogger.logE(
                TAG, "Error saving bitmap to cache: ${ioe.message}", ioe
            )
        }
        return null
    }

    fun deleteOldCover(context: Context, coverUri: Uri?) {
        if (coverUri == null || coverUri == Uri.EMPTY) return
        try {
            val path = coverUri.path ?: return
            val file = File(path)
            if (file.exists() && file.parentFile?.name == COVERS_DIR_NAME) {
                file.delete()
                TimberLogger.logD(TAG, "Deleted old cover: $path")
            }
        } catch (e: Exception) {
            TimberLogger.logE(TAG, "Error deleting old cover: ${e.message}", e)
        }
    }
}
