package dev.diegoflassa.comiqueta.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryWithComics
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Transaction
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllWithComics(): Flow<List<CategoryWithComics>>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    fun getById(categoryId: Long): Flow<CategoryEntity?>


    @Transaction
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    fun getByIdWithComics(categoryId: Long): Flow<CategoryWithComics?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteById(categoryId: Long)
}
