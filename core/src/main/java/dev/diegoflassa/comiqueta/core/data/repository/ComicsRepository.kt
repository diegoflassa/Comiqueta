package dev.diegoflassa.comiqueta.core.data.repository

import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsDao
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.database.entity.asEntity
import dev.diegoflassa.comiqueta.core.data.database.entity.asExternalModel
import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags
import dev.diegoflassa.comiqueta.core.data.model.Comic
import dev.diegoflassa.comiqueta.core.data.paging.ComicPagingSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class ComicsRepository @Inject constructor(
    private val comicsDao: ComicsDao // Using the updated ComicsDao
) {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
    }

    /**
     * Get a stream of paginated comics, filterable by category and flags.
     *
     * @param categoryId Optional Long to filter comics by a specific category.
     * @param flags A Set of ComicFlags to filter comics by (e.g., FAVORITE, NEW, READ).
     *              An empty set means no flag-based filtering beyond category.
     * @param pageSize The number of items to load per page.
     * @return A Flow of PagingData containing Comic objects.
     */
    open fun getComicsPaginated(
        categoryId: Long? = 0,
        flags: Set<ComicFlags> = emptySet(),
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): Flow<PagingData<Comic>> {
        val pagerFlow: Flow<PagingData<ComicEntity>> = Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                prefetchDistance = pageSize / 2,
                initialLoadSize = pageSize * 3
            ), pagingSourceFactory = {
                ComicPagingSource(
                    comicsDao = comicsDao, categoryId = categoryId, flags = flags
                )
            }).flow

        return pagerFlow.map { pagingData: PagingData<ComicEntity> ->
            pagingData.map { comicEntity: ComicEntity ->
                comicEntity.asExternalModel()
            }
        }
    }

    /**
     * Retrieves a single comic by its file path (Primary Key).
     *
     * @param filePath The Uri of the comic file.
     * @return The [Comic] if found, otherwise null.
     */
    open suspend fun getComicByFilePath(filePath: Uri): Comic? {
        // Assuming comicsDao.getComicByFilePath(filePath: Uri) now exists and is correct
        return comicsDao.getComicByFilePath(filePath)?.asExternalModel()
    }

    /**
     * Inserts a single comic into the database.
     *
     * @param comic The [Comic] object to insert.
     */
    open suspend fun insertComic(comic: Comic) {
        comicsDao.insertComic(comic.asEntity())
    }

    /**
     * Inserts a list of comics into the database.
     *
     * @param comics The list of [Comic] objects to insert.
     */
    open suspend fun insertComics(comics: List<ComicEntity>) {
        comicsDao.insertComics(comics)
    }

    /**
     * Updates an existing comic in the database.
     *
     * @param comic The [Comic] object with updated information.
     */
    open suspend fun updateComic(comic: Comic) {
        comicsDao.updateComic(comic.asEntity())
    }

    /**
     * Deletes a comic by its file path (Primary Key).
     *
     * @param filePath The Uri of the comic file to delete.
     */
    open suspend fun deleteComicByFilePath(filePath: Uri) {
        // Assuming comicsDao.deleteComicByFilePath(filePath: Uri) now exists
        comicsDao.deleteComicByFilePath(filePath)
    }

    /**
     * Deletes all comics from the database. Use with caution.
     */
    open suspend fun clearAllComics() {
        comicsDao.clearAllComics()
    }
}
