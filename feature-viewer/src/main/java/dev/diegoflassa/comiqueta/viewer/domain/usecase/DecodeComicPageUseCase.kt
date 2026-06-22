package dev.diegoflassa.comiqueta.viewer.domain.usecase

import android.app.Application
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import com.github.junrar.Archive as JunrarArchive
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.model.ComicFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

class DecodeComicPageUseCase @Inject constructor(
    private val application: Application
) : IDecodeComicPageUseCase {
    private fun isImageFile(fileName: String?): Boolean {
        if (fileName == null) return false
        val lowerName = fileName.lowercase()
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".gif") ||
                lowerName.endsWith(".webp") || lowerName.endsWith(".bmp")
    }

    override suspend operator fun invoke(
        pageIndex: Int,
        pageIdentifier: String,
        comicUri: Uri,
        fileType: ComicFileType,
        thumbnailWidth: Int?
    ): ImageBitmap? {
        return withContext(Dispatchers.IO) {
            TimberLogger.logI(
                "DecodeComicPageUseCase",
                "[Comiqueta][DecodeComicPage] Decoding page index: $pageIndex, identifier: '$pageIdentifier' for $fileType from $comicUri (thumb: $thumbnailWidth)"
            )
            val context = application.applicationContext

            try {
                fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int): Int {
                    val width = options.outWidth
                    var inSampleSize = 1
                    if (width > reqWidth) {
                        val halfWidth = width / 2
                        while (halfWidth / inSampleSize >= reqWidth) {
                            inSampleSize *= 2
                        }
                    }
                    return inSampleSize
                }

                fun decodeStream(inputStream: InputStream, options: BitmapFactory.Options): android.graphics.Bitmap? {
                    return BitmapFactory.decodeStream(inputStream, null, options)
                }

                val loadedBitmap: android.graphics.Bitmap? = when (fileType) {
                    ComicFileType.PDF -> {
                        context.contentResolver.openFileDescriptor(comicUri, "r")?.use { pfd ->
                            PdfRenderer(pfd).use { renderer ->
                                val actualPageIndex = pageIdentifier.toIntOrNull() ?: pageIndex
                                if (actualPageIndex < 0 || actualPageIndex >= renderer.pageCount) {
                                    throw IOException("Page index out of bounds for PDF. Index: $actualPageIndex, Count: ${renderer.pageCount}")
                                }
                                renderer.openPage(actualPageIndex).use { page ->
                                    val finalWidth: Int
                                    val finalHeight: Int
                                    if (thumbnailWidth != null && page.width > thumbnailWidth) {
                                        finalWidth = thumbnailWidth
                                        finalHeight = (page.height * (thumbnailWidth.toFloat() / page.width)).toInt()
                                    } else {
                                        finalWidth = page.width
                                        finalHeight = page.height
                                    }
                                    val bitmap = createBitmap(
                                        finalWidth,
                                        finalHeight,
                                        android.graphics.Bitmap.Config.ARGB_8888
                                    )
                                    page.render(
                                        bitmap,
                                        null,
                                        null,
                                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                    )
                                    bitmap
                                }
                            }
                        }
                    }

                    ComicFileType.CBZ -> {
                        context.contentResolver.openInputStream(comicUri)?.use { fis ->
                            BufferedInputStream(fis).use { bis ->
                                (ArchiveStreamFactory().createArchiveInputStream(
                                    ArchiveStreamFactory.ZIP, bis
                                ) as ArchiveInputStream<out ArchiveEntry>).use { ais ->
                                    var entry = ais.nextEntry
                                    while (entry != null) {
                                        if (entry.name == pageIdentifier && isImageFile(entry.name) && !entry.isDirectory) {
                                            val options = BitmapFactory.Options().apply {
                                                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                                            }

                                            if (thumbnailWidth != null) {
                                                val tempBuffer = ByteArrayOutputStream()
                                                ais.copyTo(tempBuffer)
                                                val bytes = tempBuffer.toByteArray()
                                                options.inJustDecodeBounds = true
                                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                                                options.inSampleSize = calculateInSampleSize(options, thumbnailWidth)
                                                options.inJustDecodeBounds = false
                                                return@withContext BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
                                            } else {
                                                return@withContext decodeStream(ais, options)?.asImageBitmap()
                                            }
                                        }
                                        entry = ais.nextEntry
                                    }
                                }
                            }
                        }
                        null
                    }

                    ComicFileType.CBR -> {
                        val tempFile = File(context.cacheDir, "temp_cbr_${System.currentTimeMillis()}.cbr")
                        try {
                            context.contentResolver.openInputStream(comicUri)?.use { input ->
                                tempFile.outputStream().use { output -> input.copyTo(output) }
                            } ?: throw IOException("Could not open InputStream for CBR")

                            JunrarArchive(tempFile).use { archive ->
                                val header = archive.fileHeaders.firstOrNull {
                                    it.fileName == pageIdentifier && isImageFile(it.fileName) && !it.isDirectory
                                } ?: return@withContext null

                                archive.getInputStream(header).use { inputStream ->
                                    val options = BitmapFactory.Options().apply {
                                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                                    }
                                    if (thumbnailWidth != null) {
                                        val bytes = inputStream.readBytes()
                                        options.inJustDecodeBounds = true
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                                        options.inSampleSize = calculateInSampleSize(options, thumbnailWidth)
                                        options.inJustDecodeBounds = false
                                        return@withContext BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
                                    } else {
                                        return@withContext decodeStream(inputStream, options)?.asImageBitmap()
                                    }
                                }
                            }
                        } finally {
                            if (tempFile.exists()) tempFile.delete()
                        }
                        null
                    }

                    ComicFileType.CB7 -> {
                        val tempFile = File(context.cacheDir, "temp_cb7_${System.currentTimeMillis()}.cb7")
                        try {
                            context.contentResolver.openInputStream(comicUri)?.use { input ->
                                tempFile.outputStream().use { output -> input.copyTo(output) }
                            } ?: throw IOException("Could not open InputStream for CB7")

                            SevenZFile.Builder().setFile(tempFile).get().use { sevenZFile ->
                                var entry = sevenZFile.nextEntry
                                while (entry != null) {
                                    if (entry.name == pageIdentifier && isImageFile(entry.name) && !entry.isDirectory) {
                                        val options = BitmapFactory.Options().apply {
                                            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                                        }
                                        val entryBytes = ByteArray(entry.size.toInt())
                                        var readSoFar = 0
                                        while (readSoFar < entryBytes.size) {
                                            val read = sevenZFile.read(entryBytes, readSoFar, entryBytes.size - readSoFar)
                                            if (read == -1) break
                                            readSoFar += read
                                        }

                                        if (thumbnailWidth != null) {
                                            options.inJustDecodeBounds = true
                                            BitmapFactory.decodeByteArray(entryBytes, 0, entryBytes.size, options)
                                            options.inSampleSize = calculateInSampleSize(options, thumbnailWidth)
                                            options.inJustDecodeBounds = false
                                        }
                                        return@withContext BitmapFactory.decodeByteArray(entryBytes, 0, entryBytes.size, options)?.asImageBitmap()
                                    }
                                    entry = sevenZFile.nextEntry
                                }
                            }
                        } finally {
                            if (tempFile.exists()) tempFile.delete()
                        }
                        null
                    }

                    else -> null
                }
                loadedBitmap?.asImageBitmap()
            } catch (e: Exception) {
                TimberLogger.logE("DecodeComicPageUseCase", "[Comiqueta][DecodeComicPage] Error decoding page: ${e.message}")
                null
            }
        }
    }
}
