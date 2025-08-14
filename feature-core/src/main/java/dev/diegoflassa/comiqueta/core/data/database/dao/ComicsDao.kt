package dev.diegoflassa.comiqueta.core.data.database.dao

import android.net.Uri
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity

@Dao
interface ComicsDao {

    @Query("SELECT * FROM comics WHERE file_path = :filePath")
    suspend fun getComicByFilePath(filePath: Uri): ComicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComic(comic: ComicEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComics(comics: List<ComicEntity>)

    @Update
    suspend fun updateComic(comic: ComicEntity)

    @Query("DELETE FROM comics WHERE file_path = :filePath")
    suspend fun deleteComicByFilePath(filePath: Uri)

    @Query(
        """
        SELECT * FROM comics
        WHERE
            (:categoryId IS NULL OR comic_category_id = :categoryId) 
            AND (:filterByFavorite IS NULL OR is_favorite = :filterByFavorite)
            AND (:createdAfterTimestamp IS NULL OR created >= :createdAfterTimestamp) /* Replaced is_new check */
            AND (:filterByRead IS NULL OR was_read = :filterByRead) 
        ORDER BY title ASC
        LIMIT :limit OFFSET :offset
    """
    )
    suspend fun getComicsPaginatedManual(
        limit: Int,
        offset: Int,
        categoryId: Long?,
        filterByFavorite: Boolean?,
        createdAfterTimestamp: Long?, // Changed from filterByNew: Boolean?
        filterByRead: Boolean?
    ): List<ComicEntity>

    /**
     * New method for Paging 3. Room will generate a PagingSource that
     * automatically invalidates when the 'comics' table changes.
     */
    @Query(
        """
        SELECT * FROM comics
        WHERE
            (:categoryId IS NULL OR comic_category_id = :categoryId) 
            AND (:filterByFavorite IS NULL OR is_favorite = :filterByFavorite)
            AND (:createdAfterTimestamp IS NULL OR created >= :createdAfterTimestamp) /* Replaced is_new check */
            AND (:filterByRead IS NULL OR was_read = :filterByRead) 
        ORDER BY title ASC
    """
    )
    fun getComicsPagingSource(
        categoryId: Long?,
        filterByFavorite: Boolean?,
        createdAfterTimestamp: Long?, // Changed from filterByNew: Boolean?
        filterByRead: Boolean?
    ): PagingSource<Int, ComicEntity>

    /**
     * Returns the total count of comics matching the given criteria.
     */
    @Query(
        """
        SELECT COUNT(*) FROM comics
        WHERE
            (:categoryId IS NULL OR comic_category_id = :categoryId) 
            AND (:filterByFavorite IS NULL OR is_favorite = :filterByFavorite)
            AND (:createdAfterTimestamp IS NULL OR created >= :createdAfterTimestamp)
            AND (:filterByRead IS NULL OR was_read = :filterByRead) 
    """
    )
    fun getComicsCountByCriteria(
        categoryId: Long?,
        filterByFavorite: Boolean?,
        createdAfterTimestamp: Long?,
        filterByRead: Boolean?
    ): Int

    @Query("SELECT EXISTS(SELECT * FROM comics WHERE file_path = :filePath)")
    suspend fun comicExists(filePath: String): Boolean

    @Query("UPDATE comics SET is_favorite = :isFavorite WHERE file_path = :filePath")
    suspend fun updateFavoriteStatus(filePath: String, isFavorite: Boolean)

    @Query("DELETE FROM comics")
    suspend fun clearAllComics()
}
