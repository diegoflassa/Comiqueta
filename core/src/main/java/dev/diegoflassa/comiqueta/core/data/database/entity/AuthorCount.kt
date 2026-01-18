package dev.diegoflassa.comiqueta.core.data.database.entity

import androidx.room.ColumnInfo

data class AuthorCount(
    @ColumnInfo(name = "author")
    val author: String?,
    @ColumnInfo(name = "count")
    val count: Int
)
