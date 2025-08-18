package dev.diegoflassa.comiqueta.core.data.database.dao

import android.net.Uri
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicFtsEntity

@Dao
interface ComicsDao {

    // FTS Helper Methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntoFts(comicFts: ComicFtsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListIntoFts(comicsFts: List<ComicFtsEntity>)

    @Query("DELETE FROM comics_fts WHERE file_path = :filePath")
    suspend fun deleteFromFts(filePath: String) // filePath is String matching ComicFtsEntity's PK

    @Query("DELETE FROM comics_fts")
    suspend fun clearAllFts()

    // ComicEntity CUD operations (internal, called by transactional methods)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComicInternal(comic: ComicEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComicsInternal(comics: List<ComicEntity>)

    @Update
    suspend fun updateComicInternal(comic: ComicEntity)

    @Query("DELETE FROM comics WHERE file_path = :filePath")
    suspend fun deleteComicByFilePathInternal(filePath: Uri)

    @Query("DELETE FROM comics")
    suspend fun clearAllComicsInternal()

    // Transactional CUD operations for ComicEntity and ComicFtsEntity
    @Transaction
    suspend fun insertComicAndFts(comic: ComicEntity) {
        insertComicInternal(comic)
        val comicFts = ComicFtsEntity(
            filePath = comic.filePath.toString(),
            title = comic.title,
            author = comic.author,
            fileName = comic.fileName
        )
        insertIntoFts(comicFts)
    }

    @Transaction
    suspend fun insertComicsAndFts(comics: List<ComicEntity>) {
        insertComicsInternal(comics)
        val ftsEntities = comics.map { comic ->
            ComicFtsEntity(
                filePath = comic.filePath.toString(),
                title = comic.title,
                author = comic.author,
                fileName = comic.fileName
            )
        }
        if (ftsEntities.isNotEmpty()) { // Ensure list is not empty before calling insertListIntoFts
            insertListIntoFts(ftsEntities)
        }
    }

    @Transaction
    suspend fun updateComicAndFts(comic: ComicEntity) {
        updateComicInternal(comic)
        // For FTS, delete and re-insert is a common strategy to ensure the index is up-to-date
        deleteFromFts(comic.filePath.toString())
        val comicFts = ComicFtsEntity(
            filePath = comic.filePath.toString(),
            title = comic.title,
            author = comic.author,
            fileName = comic.fileName
        )
        insertIntoFts(comicFts)
    }

    @Transaction
    suspend fun deleteComicByFilePathAndFts(filePath: Uri) {
        deleteComicByFilePathInternal(filePath)
        deleteFromFts(filePath.toString())
    }

    @Transaction
    suspend fun clearAllComicsAndFts() {
        clearAllComicsInternal()
        clearAllFts()
    }

    // Query methods
    @Query("SELECT * FROM comics WHERE file_path = :filePath")
    suspend fun getComicByFilePath(filePath: Uri): ComicEntity?

    /**
     * PagingSource for comics with FTS.
     * @param ftsQuery The FTS-formatted search query (e.g., "searchTerm*").
     *                 If null or empty, FTS search is bypassed.
     */
    @Query(
        """
        SELECT * FROM comics
        WHERE
            (:categoryId IS NULL OR comic_category_id = :categoryId) 
            AND (:filterByFavorite IS NULL OR is_favorite = :filterByFavorite)
            AND (:createdAfterTimestamp IS NULL OR created >= :createdAfterTimestamp)
            AND (:filterByRead IS NULL OR was_read = :filterByRead)
            AND (
                (:ftsQuery IS NULL OR :ftsQuery = '') OR
                file_path IN (
                    SELECT file_path FROM comics_fts WHERE comics_fts MATCH :ftsQuery
                )
            )
        ORDER BY title ASC
    """
    )
    fun getComicsPagingSource(
        categoryId: Long?,
        filterByFavorite: Boolean?,
        createdAfterTimestamp: Long?,
        filterByRead: Boolean?,
        ftsQuery: String? // This should be the FTS-formatted query (e.g., "searchTerm*")
    ): PagingSource<Int, ComicEntity>

    /**
     * Returns the total count of comics matching the given criteria, with FTS.
     * @param ftsQuery The FTS-formatted search query.
     */
    @Query(
        """
        SELECT COUNT(*) FROM comics
        WHERE
            (:categoryId IS NULL OR comic_category_id = :categoryId) 
            AND (:filterByFavorite IS NULL OR is_favorite = :filterByFavorite)
            AND (:createdAfterTimestamp IS NULL OR created >= :createdAfterTimestamp)
            AND (:filterByRead IS NULL OR was_read = :filterByRead)
            AND (
                (:ftsQuery IS NULL OR :ftsQuery = '') OR
                file_path IN (
                    SELECT file_path FROM comics_fts WHERE comics_fts MATCH :ftsQuery
                )
            )
    """
    )
    suspend fun getComicsCountByCriteria(
        categoryId: Long?,
        filterByFavorite: Boolean?,
        createdAfterTimestamp: Long?,
        filterByRead: Boolean?,
        ftsQuery: String?
    ): Int

    @Query("SELECT EXISTS(SELECT * FROM comics WHERE file_path = :filePath)")
    suspend fun comicExists(filePath: Uri): Boolean

    @Transaction
    suspend fun updateFavoriteStatus(filePath: Uri, isFavorite: Boolean) {
        val comic = getComicByFilePath(filePath)
        if (comic != null) {
            val updatedComic = comic.copy(isFavorite = isFavorite)
            // updateComicAndFts will handle updating both ComicEntity and its FTS entry
            updateComicAndFts(updatedComic)
        }
    }
}
