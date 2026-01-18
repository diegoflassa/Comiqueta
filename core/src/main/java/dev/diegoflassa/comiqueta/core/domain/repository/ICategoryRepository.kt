package dev.diegoflassa.comiqueta.core.domain.repository

import dev.diegoflassa.comiqueta.core.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface ICategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoryById(categoryId: Long): Flow<Category?>
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun deleteCategoryById(categoryId: Long)
    fun getEditableCategories(): Flow<List<Category>>
}
