package dev.diegoflassa.comiqueta.core.domain.usecase.folder

import android.net.Uri
import dev.diegoflassa.comiqueta.core.data.repository.IComicsFolderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve the list of monitored comic folder URIs.
 */
open class GetMonitoredFoldersUseCase @Inject constructor(
    private val comicsFolderRepository: IComicsFolderRepository
) : IGetMonitoredFoldersUseCase {
    //operator fun invoke(): List<Uri> {
    // Assuming getPersistedPermissions() is the correct suspend function
    // If it returns a Flow, adjust accordingly (e.g., repository.getPersistedPermissions().firstOrNull() ?: emptyList())
    // Or change the invoke operator to return the Flow directly if the ViewModel will collect it.
    // For simplicity with current ViewModel structure, making it return List<Uri> directly.
    //return comicsFolderRepository.getPersistedPermissions()
    //}

    // If your repository provides a Flow and you want the UseCase to return a Flow:
    override operator fun invoke(): Flow<List<Uri>> {
        return comicsFolderRepository.getPersistedPermissionsFlow() // Assuming such a method exists
    }
}
