package dev.diegoflassa.comiqueta.core.data.mappers

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.domain.model.Category

/**
 * Converts a database [dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity] to a domain [Category] model.
 */
fun CategoryEntity.asExternalModel(): Category = Category(
    id = this.id,
    name = this.name,
    createdAt = this.createdAt
)

/**
 * Converts a domain [Category] model to a database [CategoryEntity].
 */
fun Category.asEntity(): CategoryEntity = CategoryEntity(
    id = this.id,
    name = this.name,
    createdAt = this.createdAt
)
