package dev.diegoflassa.comiqueta.core.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages persistable URI permissions using Android's ContentResolver.
 * This class also provides a flow to observe changes to the persisted permissions.
 * It does NOT interact with the application's database.
 * Database operations for comic folders should be handled separately.
 */
@Singleton
open class ComicsFolderRepository @Inject constructor(@ApplicationContext context: Context) {

    private val contentResolver = context.contentResolver

    private val _persistedFoldersFlow = MutableStateFlow<List<Uri>>(emptyList())

    init {
        _persistedFoldersFlow.value = fetchCurrentPersistedPermissions()
    }

    /**
     * Fetches the current list of persisted URIs directly from the ContentResolver.
     */
    private fun fetchCurrentPersistedPermissions(): List<Uri> {
        return try {
            contentResolver.persistedUriPermissions.map { it.uri }
        } catch (e: Exception) {
            TimberLogger.logE(
                "ComicsFolderRepository",
                "Error retrieving persisted URI permissions",
                e
            )
            emptyList()
        }
    }

    /**
     * Provides a Flow that emits the current list of persisted folder URIs
     * and subsequent updates when permissions are taken or released via this repository.
     *
     * @return A StateFlow emitting a list of Uris.
     */
    open fun getPersistedPermissionsFlow(): StateFlow<List<Uri>> {
        return _persistedFoldersFlow.asStateFlow()
    }

    /**
     * Retrieves all persistable URI permissions currently held by the application.
     * This method provides a snapshot and does not update reactively.
     * For reactive updates, use [getPersistedPermissionsFlow].
     *
     * @return A list of Uris for which the app holds persistable permissions.
     */
    open fun getPersistedPermissions(): List<Uri> {
        // Return the current value from the flow, which is kept up-to-date,
        // or fetch directly if a fresh, non-cached read is strictly needed.
        // For simplicity and consistency with the flow, using its current value.
        // Alternatively, call: return fetchCurrentPersistedPermissions()
        return _persistedFoldersFlow.value
    }

    /**
     * Takes persistable URI permission for the given URI.
     * Updates the persisted permissions flow on success.
     *
     * @param uri The Uri for which to take permission.
     * @param flags The Intent flags for the permission (e.g., Intent.FLAG_GRANT_READ_URI_PERMISSION).
     * @return True if the permission was successfully taken, false otherwise.
     */
    open fun takePersistablePermission(
        uri: Uri,
        flags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
    ): Boolean {
        return try {
            contentResolver.takePersistableUriPermission(uri, flags)
            TimberLogger.logD("ComicsFolderRepository", "Successfully took permission for $uri")
            _persistedFoldersFlow.value = fetchCurrentPersistedPermissions()
            true
        } catch (e: SecurityException) {
            TimberLogger.logE(
                "ComicsFolderRepository",
                "Failed to take persistable URI permission for $uri",
                e
            )
            false
        } catch (e: Exception) {
            TimberLogger.logE("ComicsFolderRepository", "Error taking permission for $uri", e)
            false
        }
    }

    /**
     * Releases persistable URI permission for the given URI.
     * Updates the persisted permissions flow on success.
     *
     * @param uri The Uri for which to release permission.
     * @param flags The Intent flags used when the permission was taken (e.g., Intent.FLAG_GRANT_READ_URI_PERMISSION).
     * @return True if the permission was successfully released, false otherwise.
     */
    open fun releasePersistablePermission(
        uri: Uri,
        flags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
    ): Boolean {
        return try {
            contentResolver.releasePersistableUriPermission(uri, flags)
            TimberLogger.logD("ComicsFolderRepository", "Successfully released permission for $uri")
            _persistedFoldersFlow.value = fetchCurrentPersistedPermissions()
            true
        } catch (e: SecurityException) {
            TimberLogger.logE(
                "ComicsFolderRepository",
                "Failed to release persistable URI permission for $uri",
                e
            )
            // If the permission was not granted or already released.
            false
        } catch (e: Exception) {
            TimberLogger.logE("ComicsFolderRepository", "Error releasing permission for $uri", e)
            false
        }
    }
}
