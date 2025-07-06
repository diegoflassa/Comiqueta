package dev.diegoflassa.comiqueta.core.domain.usecase.folder

import android.net.Uri
import dev.diegoflassa.comiqueta.core.data.repository.ComicsFolderRepository
import javax.inject.Inject

/**
 * Use case to add a new monitored comic folder URI.
 * Assumes that persistable URI permission has already been taken by the UI layer.
 */
open class AddMonitoredFolderUseCase @Inject constructor(
    private val comicsFolderRepository: ComicsFolderRepository
) {
    open operator fun invoke(uri: Uri): Boolean {
        // The repository's takePersistablePermission might be a misnomer if the UI is taking it.
        // It should ideally just be an "addFolder" or "saveFolder" type of method.
        // For now, I'll assume comicsFolderRepository.takePersistablePermission is the method that stores the URI.
        // If it also tries to take permission, this might be redundant or problematic if the UI already did.
        // Consider renaming repository method to e.g., "storeMonitoredFolderUri"
        return comicsFolderRepository.takePersistablePermission(uri, 0) // Flags might be irrelevant if UI handles it
    }
}
