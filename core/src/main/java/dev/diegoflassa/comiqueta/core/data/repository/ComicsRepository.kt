package dev.diegoflassa.comiqueta.core.data.repository

import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.diegoflassa.comiqueta.core.data.database.dao.CategoryDao
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsDao
import dev.diegoflassa.comiqueta.core.data.database.entity.AuthorCount
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags
import dev.diegoflassa.comiqueta.core.data.mappers.asEntity
import dev.diegoflassa.comiqueta.core.data.mappers.asExternalModel
import dev.diegoflassa.comiqueta.core.data.preferences.PreferencesKeys
import dev.diegoflassa.comiqueta.core.domain.model.AuthorStat
import dev.diegoflassa.comiqueta.core.domain.model.CollectionStats
import dev.diegoflassa.comiqueta.core.domain.model.Comic
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicsRepository @Inject constructor(
    private val comicsDao: ComicsDao,
    private val categoryDao: CategoryDao,
    private val dataStore: DataStore<Preferences>
) : IComicsRepository {

    companion object{
        private val tag = "" + ComicsRepository::class.simpleName
        private const val DAYS_CONSIDERED_NEW = 7
    }

    override fun getComicsPaginated(
        categoryId: Long?,
        flags: Set<ComicFlags>,
        pageSize: Int,
        searchQuery: String?
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

                // Prepare search query for FTS: use null if blank, otherwise add '*' for prefix matching.
                // The FTS query will search across title, author, and fileName in the FTS table.
                val effectiveFtsQuery = if (searchQuery.isNullOrBlank()) {
                    null
                } else {
                    // Ensure the query is not just a wildcard if the search term is empty
                    val trimmedQuery = searchQuery.trim()
                    if (trimmedQuery.isNotEmpty()) "$trimmedQuery*" else null
                }

                // Launch a coroutine to perform the count and log in the background
                CoroutineScope(Dispatchers.IO).launch {
                    val count = comicsDao.getComicsCountByCriteriaFlow(
                        categoryId = categoryId,
                        filterByFavorite = filterByFavorite,
                        createdAfterTimestamp = createdAfterTimestamp,
                        filterByRead = filterByRead,
                        ftsQuery = effectiveFtsQuery
                    ).first()
                    TimberLogger.logI(tag, "[Comiqueta][Comics] Count: $count, CategoryId: $categoryId, Favorite: $filterByFavorite, CreatedAfter: $createdAfterTimestamp, Read: $filterByRead, FTS Search: '$effectiveFtsQuery', Flags: $flags")
                }

                comicsDao.getComicsPagingSource(
                    categoryId = categoryId,
                    filterByFavorite = filterByFavorite,
                    createdAfterTimestamp = createdAfterTimestamp,
                    filterByRead = filterByRead,
                    ftsQuery = effectiveFtsQuery
                )
            }).flow

        return pagerFlow.map { pagingData: PagingData<ComicEntity> ->
            pagingData.map { comicEntity: ComicEntity ->
                comicEntity.asExternalModel()
            }
        }
    }

    override suspend fun getComicByFilePath(filePath: String): Comic? {
        return comicsDao.getComicByFilePath(Uri.parse(filePath))?.asExternalModel()
    }

    override suspend fun insertComic(comic: Comic) {
        comicsDao.insertComicAndFts(comic.asEntity())
    }

    override suspend fun insertComics(comics: List<ComicEntity>) {
        comicsDao.insertComicsAndFts(comics)
    }

    override suspend fun updateComic(comic: Comic) {
        comicsDao.updateComicAndFts(comic.asEntity())
    }

    override suspend fun deleteComicByFilePath(filePath: String) {
        comicsDao.deleteComicByFilePathAndFts(Uri.parse(filePath))
    }

    override suspend fun clearAllComics() {
        comicsDao.clearAllComicsAndFts()
    }
    
    override suspend fun updateComicCover(filePath: String, coverPath: String) {
        comicsDao.updateCoverPath(Uri.parse(filePath), Uri.parse(coverPath))
    }

    override fun getCollectionStats(): Flow<CollectionStats> {
        return combine(
            comicsDao.getComicsCountByCriteriaFlow(null, null, null, null, null)
                .onEach { TimberLogger.logD(tag, "[Comiqueta][Comics] Flow emit: Total Comics = $it") },
            comicsDao.getReadCount()
                .onEach { TimberLogger.logD(tag, "[Comiqueta][Comics] Flow emit: Read Comics = $it") },
            comicsDao.getInProgressCount()
                .onEach { TimberLogger.logD(tag, "[Comiqueta][Comics] Flow emit: In Progress = $it") },
            comicsDao.getComicsCountByCriteriaFlow(null, true, null, null, null)
                .onEach { TimberLogger.logD(tag, "[Comiqueta][Comics] Flow emit: Favorites = $it") },
            categoryDao.getCount()
                .onEach { TimberLogger.logD(tag, "[Comiqueta][Comics] Flow emit: Categories = $it") },
            comicsDao.getCbzCount()
                .onEach { TimberLogger.logD(tag, "[Comiqueta][Comics] Flow emit: CBZ = $it") },
            comicsDao.getCbrCount()
                .onEach { TimberLogger.logD(tag, "[Comiqueta][Comics] Flow emit: CBR = $it") },
            comicsDao.getPdfCount()
                .onEach { TimberLogger.logD(tag, "[Comiqueta][Comics] Flow emit: PDF = $it") },
            dataStore.data
                .catch { 
                    TimberLogger.logE(tag, "[Comiqueta][Comics] Error reading DataStore", it)
                    emit(androidx.datastore.preferences.core.emptyPreferences()) 
                }
                .onEach { TimberLogger.logD(tag, "[Comiqueta][Comics] Flow emit: DataStore Preferences = $it") },
            comicsDao.getTopAuthors(5)
                .onEach { TimberLogger.logD(tag, "[Comiqueta][Comics] Flow emit: Top Authors = ${it.size}") }
        ) { args ->
            val totalComics = args[0] as Int
            val readComics = args[1] as Int
            val inProgressComics = args[2] as Int
            val favoriteComics = args[3] as Int
            val totalCategories = args[4] as Int
            val cbzCount = args[5] as Int
            val cbrCount = args[6] as Int
            val pdfCount = args[7] as Int
            val preferences = args[8] as Preferences
            // combine() hands back Array<Any?>, so element 9 can only be narrowed by
            // inspecting it. filterIsInstance checks each element instead of asserting the
            // whole list, which is what made the plain cast unchecked.
            val topAuthorsList = (args[9] as List<*>).filterIsInstance<AuthorCount>()

            val lastScanTotalFiles = preferences[PreferencesKeys.LAST_SCAN_TOTAL_FILES] ?: 0
            val lastScanProcessedComics = preferences[PreferencesKeys.LAST_SCAN_PROCESSED_COMICS] ?: 0

            val topAuthors = topAuthorsList.map {
                AuthorStat(it.author ?: "Unknown", it.count)
            }

            CollectionStats(
                totalComics = totalComics,
                readComics = readComics,
                inProgressComics = inProgressComics,
                unreadComics = totalComics - readComics - inProgressComics,
                favoriteComics = favoriteComics,
                totalCategories = totalCategories,
                cbzCount = cbzCount,
                cbrCount = cbrCount,
                pdfCount = pdfCount,
                lastScanTotalFiles = lastScanTotalFiles,
                lastScanProcessedComics = lastScanProcessedComics,
                topAuthors = topAuthors
            )
        }
    }
}
