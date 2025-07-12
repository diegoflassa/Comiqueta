package dev.diegoflassa.comiqueta.core.data.repository

import dev.diegoflassa.comiqueta.core.data.database.dao.CategoryDao
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAll()
    override fun getCategoryById(categoryId: Long): Flow<CategoryEntity?> = categoryDao.getById(categoryId)
    override suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insert(category)
    override suspend fun updateCategory(category: CategoryEntity) = categoryDao.update(category)
    override suspend fun deleteCategory(category: CategoryEntity) = categoryDao.delete(category)
    override suspend fun deleteCategoryById(categoryId: Long) = categoryDao.deleteById(categoryId)
}
