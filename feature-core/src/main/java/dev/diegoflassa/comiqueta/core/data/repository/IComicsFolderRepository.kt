package dev.diegoflassa.comiqueta.core.data.repository

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages persistable URI permissions using Android's ContentResolver.
 * This interface also provides a flow to observe changes to the persisted permissions.
 * It does NOT interact with the application's database.
 * Database operations for comic folders should be handled separately.
 */
interface IComicsFolderRepository {
    /**
     * Provides a Flow that emits the current list of persisted folder URIs
     * and subsequent updates when permissions are taken or released via this repository.
     *
     * @return A StateFlow emitting a list of Uris.
     */
    fun getPersistedPermissionsFlow(): StateFlow<List<Uri>>

    /**
     * Retrieves all persistable URI permissions currently held by the application.
     * This method provides a snapshot and does not update reactively.
     * For reactive updates, use [getPersistedPermissionsFlow].
     *
     * @return A list of Uris for which the app holds persistable permissions.
     */
    fun getPersistedPermissions(): List<Uri>

    /**
     * Takes persistable URI permission for the given URI.
     * Updates the persisted permissions flow on success.
     *
     * @param uri The Uri for which to take permission.
     * @param flags The Intent flags for the permission (e.g., Intent.FLAG_GRANT_READ_URI_PERMISSION).
     * @return True if the permission was successfully taken, false otherwise.
     */
    fun takePersistablePermission(
        uri: Uri,
        flags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
    ): Boolean

    /**
     * Releases persistable URI permission for the given URI.
     * Updates the persisted permissions flow on success.
     *
     * @param uri The Uri for which to release permission.
     * @param flags The Intent flags used when the permission was taken (e.g., Intent.FLAG_GRANT_READ_URI_PERMISSION).
     * @return True if the permission was successfully released, false otherwise.
     */
    fun releasePersistablePermission(
        uri: Uri,
        flags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
    ): Boolean
}
