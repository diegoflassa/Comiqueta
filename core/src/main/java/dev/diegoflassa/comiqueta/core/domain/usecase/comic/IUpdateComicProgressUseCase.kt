package dev.diegoflassa.comiqueta.core.domain.usecase.comic

import android.net.Uri

interface IUpdateComicProgressUseCase {
    suspend operator fun invoke(filePath: Uri, lastPageRead: Int, isCompleted: Boolean)
}
