package dev.diegoflassa.comiqueta.core.data.repository

import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsDao
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags
import dev.diegoflassa.comiqueta.core.data.mappers.asEntity
import dev.diegoflassa.comiqueta.core.data.mappers.asExternalModel
import dev.diegoflassa.comiqueta.core.domain.model.Comic
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicsRepository @Inject constructor(
    private val comicsDao: ComicsDao
) : IComicsRepository {

    companion object{
        private val tag = ComicsRepository::class.simpleName
        private const val DAYS_CONSIDERED_NEW = 7
    }

    override fun getComicsPaginated(
        categoryId: Long?,
        flags: Set<ComicFlags>,
        pageSize: Int,
        searchQuery: String? // Added to match interface
    ): Flow<PagingData<Comic>> {
        val pagerFlow: Flow<PagingData<ComicEntity>> = Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                prefetchDistance = pageSize / 2,
                initialLoadSize = pageSize * 3
            ), pagingSourceFactory = {
                val filterByFavorite: Boolean? =
                    if (flags.contains(ComicFlags.FAVORITE)) true else null
                val filterByRead: Boolean? = if (flags.contains(ComicFlags.READ)) true else null

                val createdAfterTimestamp: Long? = if (flags.contains(ComicFlags.NEW)) {
                    System.currentTimeMillis() - TimeUnit.DAYS.toMillis(DAYS_CONSIDERED_NEW.toLong())
                } else {
                    null
                }

                // Sanitize search query: use null if blank, otherwise add wildcards for LIKE operator
                val effectiveSearchQuery = if (searchQuery.isNullOrBlank()) null else "%${searchQuery.trim()}%"

                // Launch a coroutine to perform the count and log in the background
                CoroutineScope(Dispatchers.IO).launch {
                    val count = comicsDao.getComicsCountByCriteria(
                        categoryId = categoryId,
                        filterByFavorite = filterByFavorite,
                        createdAfterTimestamp = createdAfterTimestamp,
                        filterByRead = filterByRead,
                        searchQuery = effectiveSearchQuery
                    )
                    TimberLogger.logI(tag, "Count: $count, CategoryId: $categoryId, Favorite: $filterByFavorite, CreatedAfter: $createdAfterTimestamp, Read: $filterByRead, Search: '$effectiveSearchQuery', Flags: $flags")
                }

                comicsDao.getComicsPagingSource(
                    categoryId = categoryId,
                    filterByFavorite = filterByFavorite,
                    createdAfterTimestamp = createdAfterTimestamp,
                    filterByRead = filterByRead,
                    searchQuery = effectiveSearchQuery // Pass sanitized search query for paging source
                )
            }).flow

        return pagerFlow.map { pagingData: PagingData<ComicEntity> ->
            pagingData.map { comicEntity: ComicEntity ->
                // The asExternalModel() already uses the isNew(daysConsideredNew) logic for the Comic model's isNew property
                comicEntity.asExternalModel()
            }
        }
    }

    override suspend fun getComicByFilePath(filePath: Uri): Comic? {
        return comicsDao.getComicByFilePath(filePath)?.asExternalModel()
    }

    override suspend fun insertComic(comic: Comic) {
        comicsDao.insertComic(comic.asEntity())
    }

    override suspend fun insertComics(comics: List<ComicEntity>) {
        comicsDao.insertComics(comics)
    }

    override suspend fun updateComic(comic: Comic) {
        comicsDao.updateComic(comic.asEntity())
    }

    override suspend fun deleteComicByFilePath(filePath: Uri) {
        comicsDao.deleteComicByFilePath(filePath)
    }

    override suspend fun clearAllComics() {
        comicsDao.clearAllComics()
    }
}
