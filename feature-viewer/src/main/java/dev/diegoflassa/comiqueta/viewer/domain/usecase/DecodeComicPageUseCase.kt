package dev.diegoflassa.comiqueta.viewer.domain.usecase

import android.app.Application
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.documentfile.provider.DocumentFile
import com.github.junrar.Archive as JunrarArchive
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.model.ComicFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
        fileType: ComicFileType
    ): ImageBitmap? {
        return withContext(Dispatchers.IO) {
            TimberLogger.logI(
                "DecodeComicPageUseCase",
                "Decoding page index: $pageIndex, identifier: '$pageIdentifier' for $fileType from $comicUri"
            )
            val context = application.applicationContext
            var pfd: ParcelFileDescriptor? = null

            try {
                val bitmapOptions = BitmapFactory.Options().apply {
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    // Consider adding inJustDecodeBounds for checking dimensions first if needed elsewhere
                    // Consider adding inSampleSize for loading scaled down versions to save memory
                }

                val loadedBitmap: android.graphics.Bitmap? = when (fileType) {
                    ComicFileType.PDF -> {
                        pfd = context.contentResolver.openFileDescriptor(comicUri, "r")
                            ?: throw IOException("PFD null for PDF page rendering.")
                        PdfRenderer(pfd).use { renderer ->
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
                                bitmap
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
                                            return@withContext BitmapFactory.decodeStream(
                                                ais,
                                                null,
                                                bitmapOptions
                                            )?.asImageBitmap()
                                        }
                                        entry = ais.nextEntry
                                    }
                                }
                            }
                        }
                        null
                    }

                    ComicFileType.CBR -> {
                        val tempFile = File(
                            context.cacheDir,
                            "temp_cbr_decode_${System.currentTimeMillis()}.cbr"
                        )
                        try {
                            context.contentResolver.openInputStream(comicUri)?.use { inputStream ->
                                FileOutputStream(tempFile).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                                ?: throw IOException("Could not open InputStream for CBR page decoding.")

                            JunrarArchive(tempFile).use { archive ->
                                archive.fileHeaders.firstOrNull {
                                    it.fileName == pageIdentifier && isImageFile(
                                        it.fileName
                                    ) && !it.isDirectory
                                }?.let { header ->
                                    ByteArrayOutputStream().use { baos ->
                                        archive.getInputStream(header).use { entryStream ->
                                            entryStream.copyTo(baos)
                                        }
                                        val imageBytes = baos.toByteArray()
                                        BitmapFactory.decodeByteArray(
                                            imageBytes,
                                            0,
                                            imageBytes.size,
                                            bitmapOptions
                                        )
                                    }
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
                            context.contentResolver.openInputStream(comicUri)?.use { inputStream ->
                                FileOutputStream(tempFile).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                                ?: throw IOException("Could not open InputStream for CB7 page decoding.")

                            SevenZFile.Builder().setFile(tempFile).get().use { sevenZFile ->
                                var entry = sevenZFile.nextEntry
                                while (entry != null) {
                                    if (entry.name == pageIdentifier && isImageFile(entry.name) && !entry.isDirectory) {
                                        val contentBytes = ByteArray(entry.size.toInt())
                                        var currentOffset = 0
                                        while (currentOffset < contentBytes.size) {
                                            val read = sevenZFile.read(
                                                contentBytes,
                                                currentOffset,
                                                contentBytes.size - currentOffset
                                            )
                                            if (read == -1) break
                                            currentOffset += read
                                        }
                                        return@withContext BitmapFactory.decodeByteArray(
                                            contentBytes,
                                            0,
                                            contentBytes.size,
                                            bitmapOptions
                                        )?.asImageBitmap()
                                    }
                                    entry = sevenZFile.nextEntry
                                }
                            }
                        } finally {
                            if (tempFile.exists()) tempFile.delete()
                        }
                        null
                    }

                    ComicFileType.CBT -> {
                        context.contentResolver.openInputStream(comicUri)?.use { fis ->
                            BufferedInputStream(fis).use { bis ->
                                val fileName = DocumentFile.fromSingleUri(context, comicUri)?.name
                                    ?: "unknown.tar"
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
                                    var entry = ais.nextEntry
                                    while (entry != null) {
                                        if (entry.name == pageIdentifier && isImageFile(entry.name) && !entry.isDirectory) {
                                            return@withContext BitmapFactory.decodeStream(
                                                ais,
                                                null,
                                                bitmapOptions
                                            )?.asImageBitmap()
                                        }
                                        entry = ais.nextEntry
                                    }
                                }
                            }
                        }
                        null
                    }

                    ComicFileType.JPG, ComicFileType.JPEG, ComicFileType.PNG, ComicFileType.GIF, ComicFileType.WEBP -> {
                        // For single images, pageIdentifier is the filename, but we decode directly from the URI
                        context.contentResolver.openInputStream(comicUri)?.use { inputStream ->
                            if (fileType != ComicFileType.GIF) {
                                val source =
                                    ImageDecoder.createSource(context.contentResolver, comicUri)
                                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                    decoder.isMutableRequired = true
                                }
                            } else {
                                BitmapFactory.decodeStream(inputStream, null, bitmapOptions)
                            }
                        }
                    }
                }
                TimberLogger.logD(
                    "DecodeComicPageUseCase",
                    "Successfully decoded page index: $pageIndex, identifier: $pageIdentifier"
                )
                loadedBitmap?.asImageBitmap()
            } catch (ex: Exception) {
                ex.printStackTrace()
                TimberLogger.logE(
                    "DecodeComicPageUseCase",
                    "Error decoding page index: $pageIndex, identifier: '$pageIdentifier' for $comicUri",
                    ex
                )
                null
            } finally {
                try {
                    pfd?.close()
                } catch (ioe: IOException) {
                    ioe.printStackTrace()
                    TimberLogger.logE(
                        "DecodeComicPageUseCase",
                        "Error closing PFD for $comicUri in decodePage",
                        ioe
                    )
                }
            }
        }
    }
}
