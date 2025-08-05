package dev.diegoflassa.comiqueta.core.data.repository

import android.net.Uri
import androidx.paging.PagingData
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags
import dev.diegoflassa.comiqueta.core.data.model.Comic
import kotlinx.coroutines.flow.Flow

interface IComicsRepository {
    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }

    fun getComicsPaginated(
        categoryId: Long? = 0,
        flags: Set<ComicFlags> = emptySet(),
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): Flow<PagingData<Comic>>

    suspend fun getComicByFilePath(filePath: Uri): Comic?

    suspend fun insertComic(comic: Comic)

    suspend fun insertComics(comics: List<ComicEntity>)

    suspend fun updateComic(comic: Comic)

    suspend fun deleteComicByFilePath(filePath: Uri)

    suspend fun clearAllComics()
}
