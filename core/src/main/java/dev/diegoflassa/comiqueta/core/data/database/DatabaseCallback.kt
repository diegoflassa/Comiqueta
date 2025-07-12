package dev.diegoflassa.comiqueta.core.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.diegoflassa.comiqueta.core.data.database.dao.CategoryDao
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.data.preferences.UserPreferencesKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class DatabaseCallback @Inject constructor(
    private val categoryDaoProvider: Provider<CategoryDao>
) : RoomDatabase.Callback() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        applicationScope.launch {
            populateInitialData()
        }
    }

    suspend fun populateInitialData() {
        val categoryDao = categoryDaoProvider.get()
        categoryDao.insert(CategoryEntity(name = UserPreferencesKeys.DEFAULT_CATEGORY_ALL))
        // You could add other default categories here if needed
        // For example:
        // categoryDao.insertCategory(CategoryEntity(name = "Favorites"))
    }
}
