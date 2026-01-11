package dev.diegoflassa.comiqueta.core.domain.usecase.comic

import android.net.Uri
import dev.diegoflassa.comiqueta.core.data.repository.IComicsRepository
import dev.diegoflassa.comiqueta.core.domain.model.Comic
import javax.inject.Inject

class GetComicUseCase @Inject constructor(
    private val comicsRepository: IComicsRepository
) : IGetComicUseCase {
    override suspend fun invoke(filePath: Uri): Comic? {
        return comicsRepository.getComicByFilePath(filePath)
    }
}
