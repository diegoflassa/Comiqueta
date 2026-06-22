package dev.diegoflassa.comiqueta.viewer.domain.usecase

import android.app.Application
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Xml
import androidx.documentfile.provider.DocumentFile
import com.github.junrar.Archive as JunrarArchive
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.model.ComicFileType
import dev.diegoflassa.comiqueta.viewer.model.ComicInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Collections
import java.util.Locale
import javax.inject.Inject

class GetComicInfoUseCase @Inject constructor(
    private val application: Application
) : IGetComicInfoUseCase {

    private val alphanumComparator = Comparator<String> { s1, s2 ->
        val regex = Regex("([0-9]+)|([^0-9]+)")
        val parts1 = regex.findAll(s1).map { it.value }.toList()
        val parts2 = regex.findAll(s2).map { it.value }.toList()

        val maxParts = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxParts) {
            val p1 = parts1.getOrNull(i)
            val p2 = parts2.getOrNull(i)

            if (p1 == null) return@Comparator -1
            if (p2 == null) return@Comparator 1

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

    private fun parseComicInfoXml(inputStream: InputStream): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            var currentTag = ""
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    currentTag = parser.name
                } else if (eventType == XmlPullParser.TEXT) {
                    val text = parser.text.trim()
                    if (text.isNotEmpty()) {
                        metadata[currentTag] = text
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            TimberLogger.logE("GetComicInfoUseCase", "[Comiqueta][GetComicInfo] Error parsing ComicInfo.xml", e)
        }
        return metadata
    }

    override suspend operator fun invoke(uri: Uri): ComicInfo {
        return withContext(Dispatchers.IO) {
            val context = application.applicationContext
            val docFile = DocumentFile.fromSingleUri(context, uri)
                ?: throw IOException("Could not access DocumentFile for URI: $uri")
            val fileName = docFile.name ?: "Unknown"
            var title = fileName.substringBeforeLast(".")

            var pfd: ParcelFileDescriptor? = null
            val pageIdentifiers = mutableListOf<String>()
            var pageCount = 0
            var determinedFileType: ComicFileType?

            try {
                val mimeType = context.contentResolver.getType(uri)
                val fileExtension =
                    fileName.substringAfterLast(".", "").takeIf { it.isNotEmpty() }
                
                determinedFileType = ComicFileType.fromMimeTypeOrExtension(mimeType, fileExtension)

                if (determinedFileType == null) {
                    throw IOException("Unsupported file type or could not determine type for: $fileName")
                }

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
                                    val entries = mutableListOf<ArchiveEntry>()
                                    var entry = ais.nextEntry
                                    while (entry != null) {
                                        if (!entry.isDirectory) {
                                            if (isImageFile(entry.name)) {
                                                entries.add(entry)
                                            } else if (entry.name.equals("ComicInfo.xml", ignoreCase = true)) {
                                                val metadata = parseComicInfoXml(ais)
                                                metadata["Title"]?.let { title = it }
                                            }
                                        }
                                        entry = ais.nextEntry
                                    }
                                    val sortedNames = entries.map { it.name }.sortedWith(alphanumComparator)
                                    pageIdentifiers.addAll(sortedNames)
                                    pageCount = sortedNames.size
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
                                val headers = archive.fileHeaders
                                headers.forEach { header ->
                                    if (!header.isDirectory) {
                                        if (isImageFile(header.fileName)) {
                                            pageIdentifiers.add(header.fileName)
                                        } else if (header.fileName.equals("ComicInfo.xml", ignoreCase = true)) {
                                            archive.getInputStream(header).use { ais ->
                                                val metadata = parseComicInfoXml(ais)
                                                metadata["Title"]?.let { title = it }
                                            }
                                        }
                                    }
                                }
                                pageIdentifiers.sortWith(alphanumComparator)
                                pageCount = pageIdentifiers.size
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
                                var entry = sevenZFile.nextEntry
                                while (entry != null) {
                                    if (!entry.isDirectory) {
                                        if (isImageFile(entry.name)) {
                                            pageIdentifiers.add(entry.name)
                                        } else if (entry.name.equals("ComicInfo.xml", ignoreCase = true)) {
                                            // SevenZFile.getInputStream(entry) might be tricky depending on version
                                            // For now we just skip metadata for 7z if not easily accessible
                                        }
                                    }
                                    entry = sevenZFile.nextEntry
                                }
                                pageIdentifiers.sortWith(alphanumComparator)
                                pageCount = pageIdentifiers.size
                            }
                        } finally {
                            if (tempFile.exists()) tempFile.delete()
                        }
                    }

                    ComicFileType.CBT -> {
                        context.contentResolver.openInputStream(uri)?.use { fis ->
                            BufferedInputStream(fis).use { bis ->
                                val tarInput: TarArchiveInputStream = when {
                                    fileName.endsWith(".tar.gz", true) || fileName.endsWith(".tgz", true) ->
                                        TarArchiveInputStream(GzipCompressorInputStream(bis))
                                    fileName.endsWith(".tar.bz2", true) || fileName.endsWith(".tbz2", true) ->
                                        TarArchiveInputStream(BZip2CompressorInputStream(bis))
                                    else -> TarArchiveInputStream(bis)
                                }
                                tarInput.use { ais ->
                                    var entry = ais.nextEntry
                                    while (entry != null) {
                                        if (!entry.isDirectory) {
                                            if (isImageFile(entry.name)) {
                                                pageIdentifiers.add(entry.name)
                                            } else if (entry.name.equals("ComicInfo.xml", ignoreCase = true)) {
                                                val metadata = parseComicInfoXml(ais)
                                                metadata["Title"]?.let { title = it }
                                            }
                                        }
                                        entry = ais.nextEntry
                                    }
                                    pageIdentifiers.sortWith(alphanumComparator)
                                    pageCount = pageIdentifiers.size
                                }
                            }
                        } ?: throw IOException("Could not open InputStream for CBT.")
                    }

                    ComicFileType.JPG, ComicFileType.JPEG, ComicFileType.PNG, ComicFileType.GIF, ComicFileType.WEBP -> {
                        pageIdentifiers.add(fileName)
                        pageCount = 1
                    }
                }

            } catch (ex: Exception) {
                TimberLogger.logE("GetComicInfoUseCase", "[Comiqueta][GetComicInfo] Error getting comic info for $uri", ex)
                throw IOException("Failed to parse comic: ${ex.message}", ex)
            } finally {
                try {
                    pfd?.close()
                } catch (ioe: IOException) {
                    TimberLogger.logE("GetComicInfoUseCase", "[Comiqueta][GetComicInfo] Error closing PFD for $uri", ioe)
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
}
