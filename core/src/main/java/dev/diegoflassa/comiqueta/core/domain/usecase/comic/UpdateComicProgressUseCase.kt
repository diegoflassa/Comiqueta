package dev.diegoflassa.comiqueta.core.domain.usecase.comic

import android.net.Uri
import dev.diegoflassa.comiqueta.core.data.repository.IComicsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateComicProgressUseCase @Inject constructor(
    private val comicsRepository: IComicsRepository
) : IUpdateComicProgressUseCase {
    override suspend fun invoke(filePath: Uri, lastPageRead: Int, isCompleted: Boolean) {
        withContext(Dispatchers.IO) {
            val comic = comicsRepository.getComicByFilePath(filePath)
            if (comic != null) {
                val updatedComic = comic.copy(
                    lastPageRead = lastPageRead,
                    hasBeenRead = isCompleted || (lastPageRead >= comic.pageCount - 1 && comic.pageCount > 0)
                )
                comicsRepository.updateComic(updatedComic)
            }
        }
    }
}
