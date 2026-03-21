package dev.diegoflassa.comiqueta.core.domain.usecase.comic

import dev.diegoflassa.comiqueta.core.domain.model.Comic

interface IGetComicUseCase {
    suspend operator fun invoke(filePath: String): Comic?
}
