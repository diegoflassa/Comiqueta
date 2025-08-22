package dev.diegoflassa.comiqueta.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "comics_fts")
@Fts4
data class ComicFtsEntity(
    // This column will store the primary key of the ComicEntity (filePath as String)
    // to link this FTS entry back to the original comic.
    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "title")
    val title: String?,

    @ColumnInfo(name = "author")
    val author: String?,

    @ColumnInfo(name = "file_name")
    val fileName: String?
    // Note: FTS tables have an implicit 'rowid' INTEGER PRIMARY KEY.
    // The fields defined here become the columns of the FTS table.
)
