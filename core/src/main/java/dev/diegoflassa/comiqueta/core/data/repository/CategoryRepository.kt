package dev.diegoflassa.comiqueta.core.data.repository

import dev.diegoflassa.comiqueta.core.data.database.dao.CategoryDao
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.data.preferences.UserPreferencesKeys
import dev.diegoflassa.comiqueta.core.domain.repository.ICategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) : ICategoryRepository {
    override fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAll()
    }

    override fun getCategoryById(categoryId: Long): Flow<CategoryEntity?> {
        return categoryDao.getById(categoryId)
    }

    override suspend fun insertCategory(category: CategoryEntity): Long {
        // Prevent adding "All" category
        if (category.name.equals(UserPreferencesKeys.DEFAULT_CATEGORY_ALL, ignoreCase = true)) {
            // Optionally, throw an exception or return a specific value indicating failure
            // For now, let's assume we just don't insert it and return a non-positive value or handle error
            return -1L // Or throw IllegalArgumentException("Cannot add 'All' category manually")
        }
        return categoryDao.insert(category)
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        // Prevent renaming a category to "All" if it's not already the "All" category,
        // or prevent changing the name of the "All" category.
        // This logic might need refinement based on how you identify the "All" category (e.g., by a fixed ID).
        if (category.name.equals(UserPreferencesKeys.DEFAULT_CATEGORY_ALL, ignoreCase = true)) {
            // Potentially check if the original category was also "All"
            // If we assume "All" category name should not be changed or assigned:
             throw IllegalArgumentException("Cannot update category to or from 'All' name via this method")
        }
        return categoryDao.update(category)
    }

    override suspend fun deleteCategory(category: CategoryEntity) {
        if (category.name.equals(UserPreferencesKeys.DEFAULT_CATEGORY_ALL, ignoreCase = true)) {
            // Prevent deleting "All" category
            throw IllegalArgumentException("Cannot delete 'All' category")
        }
        return categoryDao.delete(category)
    }

    override suspend fun deleteCategoryById(categoryId: Long) {
        // To prevent deleting "All" by ID, you would first need to fetch the category,
        // check its name, and then decide. This requires making this function non-suspending
        // if called from a context that cannot make another suspend call, or more complex logic.
        // For simplicity, direct ID deletion might bypass name check unless handled in DAO or DB trigger.
        // A safer approach is to fetch then delete, or ensure "All" has a known unexposed ID.
        val category = categoryDao.getById(categoryId).firstOrNull()
        if (category?.name.equals(UserPreferencesKeys.DEFAULT_CATEGORY_ALL, ignoreCase = true)) {
             throw IllegalArgumentException("Cannot delete 'All' category by ID")
        }
        return categoryDao.deleteById(categoryId)
    }

    override fun getEditableCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAll().map { entities ->
            entities.filterNot { it.name.equals(UserPreferencesKeys.DEFAULT_CATEGORY_ALL, ignoreCase = true) }
                .sortedBy { it.name }
        }
    }
}
