package dev.diegoflassa.comiqueta.core.data.mappers

import android.net.Uri
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.domain.model.Comic
import kotlin.text.ifEmpty

/**
 * Converts a database [ComicEntity] to a domain [Comic] model.
 */
fun ComicEntity.asExternalModel(): Comic = Comic(
    filePath = this.filePath,
    categoryId = this.comicCategoryId,
    coverPath = this.coverPath ?: Uri.EMPTY,
    title = this.title ?: "Untitled",
    author = this.author,
    isFavorite = this.isFavorite,
    isNew = isNew(daysConsideredNew = 7), // Example: comic is new if created in the last 7 days
    hasBeenRead = this.read,
    lastPageRead = this.lastPage,
    lastModified = this.lastModified,
    created = this.created,
)

/**
 * Converts a domain [Comic] model to a database [ComicEntity].
 */
fun Comic.asEntity(): ComicEntity = ComicEntity(
    // Fields from your Comic model that map back to ComicEntity
    filePath = this.filePath, // filePath in Comic model should be non-null Uri
    comicCategoryId = this.categoryId,
    coverPath = this.coverPath, // coverPath in Comic model is nullable Uri
    title = this.title?.ifEmpty { null }, // Store as null in DB if UI sends empty string
    author = this.author,
    isFavorite = this.isFavorite,
    read = this.hasBeenRead,
    lastPage = this.lastPageRead, // Map 'lastPageRead' from model to 'lastPage' in entity
    lastModified = this.lastModified,
    created = if (this.created == 0L) System.currentTimeMillis() else this.created, // Ensure 'created' is set, either from model or new

    // Regarding your original mapping attempt and ComicEntity structure:
    // folderPath = this.folderUri ?: Uri.EMPTY, // Map folderUri from Comic model to folderPath in entity.
    // Uri.EMPTY might not be ideal for DB, null might be better if the column is nullable.
    // Depends on your Room TypeConverter for Uri to String.
    // fileName = this.filePath.lastPathSegment, // Example if you still want fileName. Be cautious if filePath can be non-standard.

    // pageCount: Your ComicEntity doesn't have a direct 'pageCount' field.
    // If 'lastPage' actually represents the total number of pages, then:
    // lastPage = this.pageCount, // if Comic model had pageCount and it maps to entity's lastPage
)
