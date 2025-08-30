package dev.diegoflassa.comiqueta.core.data.database.entity

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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

    @ColumnInfo(name = "was_read")
    val read: Boolean = false,

    @ColumnInfo(name = "last_page")
    val lastPage: Int = 0,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = 0,

    @ColumnInfo(name = "created", defaultValue = "0")
    val created: Long = System.currentTimeMillis()
) {
    /**
     * Checks if the comic is considered new based on its creation date.
     * @param daysConsideredNew The number of days within which a comic is considered new. Defaults to 7 days.
     * @return True if the comic was created within the specified number of days, false otherwise.
     */
    fun isNew(daysConsideredNew: Int = 7): Boolean {
        val currentTimeMillis = System.currentTimeMillis()
        // Convert days to milliseconds: days * hours/day * minutes/hour * seconds/minute * milliseconds/second
        val daysInMillis = daysConsideredNew * 24 * 60 * 60 * 1000L
        val thresholdMillis = currentTimeMillis - daysInMillis
        return this.created >= thresholdMillis
    }
}
