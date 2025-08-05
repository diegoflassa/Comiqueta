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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicsRepository @Inject constructor(
    private val comicsDao: ComicsDao
) : IComicsRepository {

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
                val filterByNew: Boolean? = if (flags.contains(ComicFlags.NEW)) true else null
                val filterByRead: Boolean? = if (flags.contains(ComicFlags.READ)) true else null

                comicsDao.getComicsPagingSource(
                    categoryId = categoryId,
                    filterByFavorite = filterByFavorite,
                    filterByNew = filterByNew,
                    filterByRead = filterByRead
                )
            }).flow

        return pagerFlow.map { pagingData: PagingData<ComicEntity> ->
            pagingData.map { comicEntity: ComicEntity ->
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
