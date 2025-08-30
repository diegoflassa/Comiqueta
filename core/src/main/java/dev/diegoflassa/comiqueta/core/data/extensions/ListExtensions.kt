@file:Suppress("unused", "DEPRECATION")

package dev.diegoflassa.comiqueta.core.data.extensions

import android.net.Uri
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.mappers.asExternalModel
import dev.diegoflassa.comiqueta.core.domain.model.Category
import dev.diegoflassa.comiqueta.core.domain.model.Comic

fun List<Uri>.toStringList(): List<String> {
    return map { it.toString() }
}

fun List<ComicEntity>.toComicList(): List<Comic> {
    return map { it.asExternalModel() }
}

fun List<CategoryEntity>.toCategoryList(): List<Category> {
    return map { it.asExternalModel() }
}
