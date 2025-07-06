package dev.diegoflassa.comiqueta.core.domain.usecase.folder

import android.net.Uri
import dev.diegoflassa.comiqueta.core.data.repository.ComicsFolderRepository
import javax.inject.Inject

/**
 * Use case to remove a monitored comic folder URI.
 * This UseCase only tells the repository to forget the URI.
 * The actual call to contentResolver.releasePersistableUriPermission() should be handled by the UI layer.
 */
open class RemoveMonitoredFolderUseCase @Inject constructor(
    private val comicsFolderRepository: ComicsFolderRepository
) {
    open operator fun invoke(uri: Uri): Boolean {
        // Similar to AddMonitoredFolderUseCase, the repository method should ideally just remove the URI from storage.
        // If comicsFolderRepository.releasePersistablePermission also tries to release from ContentResolver,
        // it might be okay, or you might separate it to have the UI layer always handle ContentResolver interaction.
        // For now, aligning with existing repository method name.
        return comicsFolderRepository.releasePersistablePermission(uri, 0) // Flags might be irrelevant
    }
}
