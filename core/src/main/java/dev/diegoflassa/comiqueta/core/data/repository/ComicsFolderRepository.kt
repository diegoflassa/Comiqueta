package dev.diegoflassa.comiqueta.core.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages persistable URI permissions using Android's ContentResolver.
 * This class does NOT interact with the application's database.
 * Database operations for comic folders should be handled separately, typically by injecting ComicsFoldersDao
 * directly into ViewModels or UseCases.
 */
@Singleton
open class ComicsFolderRepository @Inject constructor(context: Context) {

    private val contentResolver = context.contentResolver

    /**
     * Takes persistable URI permission for the given URI.
     *
     * @param uri The Uri for which to take permission.
     * @param flags The Intent flags for the permission (e.g., Intent.FLAG_GRANT_READ_URI_PERMISSION).
     * @return True if the permission was successfully taken, false otherwise.
     */
    open fun takePersistablePermission(
        uri: Uri,
        flags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION // Default to read permission
    ): Boolean {
        return try {
            contentResolver.takePersistableUriPermission(uri, flags)
            TimberLogger.logD("ComicsFolderRepository", "Successfully took permission for $uri")
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
     *
     * @param uri The Uri for which to release permission.
     * @param flags The Intent flags used when the permission was taken (e.g., Intent.FLAG_GRANT_READ_URI_PERMISSION).
     * @return True if the permission was successfully released, false otherwise.
     */
    open fun releasePersistablePermission(
        uri: Uri,
        flags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION // Default to read permission
    ): Boolean {
        return try {
            contentResolver.releasePersistableUriPermission(uri, flags)
            TimberLogger.logD("ComicsFolderRepository", "Successfully released permission for $uri")
            true
        } catch (e: SecurityException) {
            TimberLogger.logE(
                "ComicsFolderRepository",
                "Failed to release persistable URI permission for $uri",
                e
            )
            // This can happen if the permission was not granted or already released.
            false
        } catch (e: Exception) {
            TimberLogger.logE("ComicsFolderRepository", "Error releasing permission for $uri", e)
            false
        }
    }

    /**
     * Retrieves all persistable URI permissions currently held by the application.
     * This can be used for debugging or to get a raw list of all granted folder/document URIs.
     *
     * @return A list of Uris for which the app holds persistable permissions.
     */
    open fun getPersistedPermissions(): List<Uri> {
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
}
