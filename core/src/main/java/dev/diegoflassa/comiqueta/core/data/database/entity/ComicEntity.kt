package dev.diegoflassa.comiqueta.core.data.database.entity

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.diegoflassa.comiqueta.core.data.model.Comic

@Entity(
    tableName = "comics",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["comic_category_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["comic_category_id"])]
)
data class ComicEntity(
    @PrimaryKey
    @ColumnInfo(name = "file_path")
    val filePath: Uri = Uri.EMPTY,

    @ColumnInfo(name = "folder_path")
    val folderPath: Uri = Uri.EMPTY,

    @ColumnInfo(name = "file_name")
    val fileName: String? = null,

    @ColumnInfo(name = "comic_category_id")
    val comicCategoryId: Long? = null,

    @ColumnInfo(name = "cover_path")
    val coverPath: Uri? = null,

    @ColumnInfo(name = "title")
    val title: String? = null,

    @ColumnInfo(name = "author")
    val author: String? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "is_new")
    val isNew: Boolean = true,

    @ColumnInfo(name = "was_read")
    val read: Boolean = false,

    @ColumnInfo(name = "last_page")
    val lastPage: Int = 0,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = 0,

    @ColumnInfo(name = "created", defaultValue = "0")
    val created: Long = System.currentTimeMillis()
)

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
    isNew = this.isNew,
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
    isNew = this.isNew,
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
