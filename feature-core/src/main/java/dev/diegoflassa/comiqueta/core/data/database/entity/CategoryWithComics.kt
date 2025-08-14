package dev.diegoflassa.comiqueta.core.data.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class CategoryWithComics(
    @Embedded
    val category: CategoryEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "comic_category_id"
    )
    val comics: List<ComicEntity>
)
