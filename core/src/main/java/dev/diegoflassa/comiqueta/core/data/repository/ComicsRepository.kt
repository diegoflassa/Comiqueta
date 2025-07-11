package dev.diegoflassa.comiqueta.core.data.repository

import android.net.Uri
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsDao
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open
class ComicsRepository @Inject constructor(
    private val comicsDao: ComicsDao
) {

    /**
     * Inserts a single comic into the database. If a comic with the same primary key already exists,
     * it will be replaced.
     */
    suspend fun insertComic(comic: ComicEntity) {
        comicsDao.insertComic(comic)
    }

    /**
     * Inserts a list of comics into the database. If any comic with the same primary key already exists,
     * it will be replaced.
     */
    suspend fun insertComics(comics: List<ComicEntity>) {
        comicsDao.insertComics(comics)
    }

    /**
     * Updates an existing comic in the database.
     */
    suspend fun updateComic(comic: ComicEntity) {
        comicsDao.updateComic(comic)
    }

    /**
     * Deletes a specific comic from the database.
     */
    suspend fun deleteComic(comic: ComicEntity) {
        comicsDao.deleteComic(comic)
    }

    /**
     * Deletes all comics from the database. Use with caution.
     */
    suspend fun deleteAllComics() {
        comicsDao.deleteAllComics()
    }

    /**
     * Retrieves a comic by its file path as a Flow.
     * The Flow will emit a new value if the comic data changes or if the comic is added/removed.
     * Emits null if no comic with the given file path is found.
     */
    fun getComicByPath(filePath: String): Flow<ComicEntity?> {
        return comicsDao.getComicByPath(filePath)
    }

    /**
     * Retrieves a comic by its Uri file path directly.
     * Returns null if no comic with the given file path is found.
     * This is a suspend function suitable for one-off checks (e.g., in a Worker).
     */
    suspend fun getComicByFilePath(filePath: Uri): ComicEntity? {
        return comicsDao.getComicByFilePath(filePath)
    }

    /**
     * Retrieves all comics from the database, ordered by title, as a Flow.
     * The Flow will emit a new list whenever the comics data changes.
     */
    open fun getAllComics(): Flow<List<ComicEntity>> {
        return comicsDao.getAllComics()
    }

    /**
     * Retrieves all favorite comics, ordered by title, as a Flow.
     */
    open fun getFavoriteComics(): Flow<List<ComicEntity>> {
        return comicsDao.getFavoriteComics()
    }

    /**
     * Retrieves all new comics, ordered by title, as a Flow.
     */
    open fun getNewComics(): Flow<List<ComicEntity>> {
        return comicsDao.getNewComics()
    }

    /**
     * Retrieves all unread comics, ordered by title, as a Flow.
     */
    fun getUnreadComics(): Flow<List<ComicEntity>> {
        return comicsDao.getUnreadComics()
    }
}
