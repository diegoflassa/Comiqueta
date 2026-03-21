package dev.diegoflassa.comiqueta.core.domain.usecase.comic

interface IUpdateComicProgressUseCase {
    suspend operator fun invoke(filePath: String, lastPageRead: Int, isCompleted: Boolean)
}
