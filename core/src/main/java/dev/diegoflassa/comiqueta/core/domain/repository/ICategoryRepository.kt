package dev.diegoflassa.comiqueta.core.domain.repository

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface ICategoryRepository {
    fun getAllCategories(): Flow<List<CategoryEntity>>
    fun getCategoryById(categoryId: Long): Flow<CategoryEntity?>
    suspend fun insertCategory(category: CategoryEntity): Long
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun deleteCategory(category: CategoryEntity)
    suspend fun deleteCategoryById(categoryId: Long)
    fun getEditableCategories(): Flow<List<CategoryEntity>>
}
