package dev.diegoflassa.comiqueta.core.data.database.entity

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comics")
data class ComicEntity(
    @PrimaryKey
    @ColumnInfo(name = "file_path")
    val filePath: Uri = Uri.EMPTY,

    @ColumnInfo(name = "cover_path")
    val coverPath: Uri? = null,

    @ColumnInfo(name = "title")
    val title: String? = null,

    @ColumnInfo(name = "genre")
    val genre: String? = null,

    @ColumnInfo(name = "author")
    val author: String? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "is_new")
    val isNew: Boolean = true,

    @ColumnInfo(name = "has_been_read")
    val hasBeenRead: Boolean = false,

    @ColumnInfo(name = "last_page")
    val lastPage: Int = 0,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = 0,

    @ColumnInfo(name = "created", defaultValue = "0")
    val created: Long = System.currentTimeMillis()
)