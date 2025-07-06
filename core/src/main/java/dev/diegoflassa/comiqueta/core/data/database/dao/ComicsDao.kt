package dev.diegoflassa.comiqueta.core.data.database.dao

import android.net.Uri
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComicsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComic(comic: ComicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComics(comics: List<ComicEntity>)

    @Update
    suspend fun updateComic(comic: ComicEntity)

    @Delete
    suspend fun deleteComic(comic: ComicEntity)

    @Query("DELETE FROM comics")
    suspend fun deleteAllComics()

    @Query("SELECT * FROM comics WHERE file_path = :filePath")
    fun getComicByPath(filePath: String): Flow<ComicEntity?>

    @Query("SELECT * FROM comics WHERE file_path = :filePath LIMIT 1")
    suspend fun getComicByFilePath(filePath: Uri): ComicEntity?

    @Query("SELECT * FROM comics ORDER BY title ASC")
    fun getAllComics(): Flow<List<ComicEntity>>

    @Query("SELECT * FROM comics WHERE is_favorite = 1 ORDER BY title ASC")
    fun getFavoriteComics(): Flow<List<ComicEntity>>

    @Query("SELECT * FROM comics WHERE is_new = 1 ORDER BY title ASC")
    fun getNewComics(): Flow<List<ComicEntity>>

    @Query("SELECT * FROM comics WHERE has_been_read = 0 ORDER BY title ASC")
    fun getUnreadComics(): Flow<List<ComicEntity>>
}