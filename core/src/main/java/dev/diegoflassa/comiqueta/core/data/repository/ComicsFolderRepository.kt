package dev.diegoflassa.comiqueta.core.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation for managing persistable URI permissions.
 */
@Singleton
class ComicsFolderRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : IComicsFolderRepository {

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

    override fun getPersistedPermissionsFlow(): StateFlow<List<Uri>> {
        return _persistedFoldersFlow.asStateFlow()
    }

    override fun getPersistedPermissions(): List<Uri> {
        return _persistedFoldersFlow.value
    }

    override fun takePersistablePermission(
        uri: Uri,
        flags: Int
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

    override fun releasePersistablePermission(
        uri: Uri,
        flags: Int
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
