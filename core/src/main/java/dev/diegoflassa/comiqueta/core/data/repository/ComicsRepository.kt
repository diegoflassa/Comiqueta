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
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicsRepository @Inject constructor(
    private val comicsDao: ComicsDao
) : IComicsRepository {

    companion object {
        private val tag = ComicsRepository::class.simpleName
        private const val DAYS_CONSIDERED_NEW = 70
    }

    override fun getComicsPaginated(
        categoryId: Long?,
        flags: Set<ComicFlags>,
        pageSize: Int
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
                TimberLogger.logI(
                    tag,
                    "CategoryId: $categoryId, Favorite: $filterByFavorite, CreatedAfter: $createdAfterTimestamp, Flags: $flags"
                )

                comicsDao.getComicsPagingSource(
                    categoryId = categoryId,
                    filterByFavorite = filterByFavorite,
                    createdAfterTimestamp = createdAfterTimestamp,
                    filterByRead = filterByRead
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
