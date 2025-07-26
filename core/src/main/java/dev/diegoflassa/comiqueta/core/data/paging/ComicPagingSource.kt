package dev.diegoflassa.comiqueta.core.data.paging // Or your actual package

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsDao
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags

class ComicPagingSource(
    private val comicsDao: ComicsDao,
    private val categoryId: Long?,
    private val flags: Set<ComicFlags> = emptySet()
) : PagingSource<Int, ComicEntity>() {

    companion object {
        private const val STARTING_PAGE_INDEX = 0
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ComicEntity> {
        val page = params.key ?: STARTING_PAGE_INDEX
        return try {
            val filterByFavorite: Boolean? = if (flags.contains(ComicFlags.FAVORITE)) true else null
            val filterByNew: Boolean? = if (flags.contains(ComicFlags.NEW)) true else null
            val filterByRead: Boolean? = if (flags.contains(ComicFlags.READ)) true else null

            val entities = comicsDao.getComicsPaginated(
                limit = params.loadSize,
                offset = page * params.loadSize,
                categoryId = categoryId,
                filterByFavorite = filterByFavorite,
                filterByNew = filterByNew,
                filterByRead = filterByRead
            )

            LoadResult.Page(
                data = entities,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (entities.isEmpty()) null else page + 1
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            LoadResult.Error(ex)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ComicEntity>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
