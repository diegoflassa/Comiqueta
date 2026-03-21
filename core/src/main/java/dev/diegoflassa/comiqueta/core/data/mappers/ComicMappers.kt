package dev.diegoflassa.comiqueta.core.data.mappers

import android.net.Uri
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.domain.model.Comic

/**
 * Converts a database [ComicEntity] to a domain [Comic] model.
 */
fun ComicEntity.asExternalModel(): Comic = Comic(
    filePath = this.filePath.toString(),
    categoryId = this.comicCategoryId,
    coverPath = (this.coverPath ?: Uri.EMPTY).toString(),
    title = this.title ?: "Untitled",
    author = this.author,
    isFavorite = this.isFavorite,
    isNew = isNew(daysConsideredNew = 7),
    hasBeenRead = this.read,
    lastPageRead = this.lastPage,
    lastModified = this.lastModified,
    created = this.created,
)

/**
 * Converts a domain [Comic] model to a database [ComicEntity].
 */
fun Comic.asEntity(): ComicEntity = ComicEntity(
    filePath = Uri.parse(this.filePath),
    comicCategoryId = this.categoryId,
    coverPath = Uri.parse(this.coverPath).takeIf { this.coverPath.isNotEmpty() },
    title = this.title?.ifEmpty { null },
    author = this.author,
    isFavorite = this.isFavorite,
    read = this.hasBeenRead,
    lastPage = this.lastPageRead,
    lastModified = this.lastModified,
    created = if (this.created == 0L) System.currentTimeMillis() else this.created,
)
