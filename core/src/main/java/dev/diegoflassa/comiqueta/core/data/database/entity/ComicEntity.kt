package dev.diegoflassa.comiqueta.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comics")
data class ComicEntity(
    @PrimaryKey
    @ColumnInfo(name = "file_path")
    val filePath: String = "", // Storing Uri.toString()

    @ColumnInfo(name = "cover_path")
    val coverPath: String? = null, // Storing Uri.toString(), nullable if cover might not exist initially

    @ColumnInfo(name = "title")
    val title: String? = null,

    @ColumnInfo(name = "genre")
    val genre: String? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "is_new")
    val isNew: Boolean = true,

    @ColumnInfo(name = "has_been_read")
    val hasBeenRead: Boolean = false
)