package dev.diegoflassa.comiqueta.core.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.diegoflassa.comiqueta.core.data.timber.LogRedaction
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
            contentResolver.persistedUriPermissions.map { it.uri }.also {
                TimberLogger.logI(
                    "ComicsFolderRepository",
                    "[Comiqueta][ComicsFolder] persisted permissions read count=${it.size}",
                )
            }
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            TimberLogger.logE(
                "ComicsFolderRepository",
                "[Comiqueta][ComicsFolder] Error retrieving persisted URI permissions",
                ex
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
            _persistedFoldersFlow.value = fetchCurrentPersistedPermissions()
            TimberLogger.logI(
                "ComicsFolderRepository",
                "[Comiqueta][ComicsFolder] took persistable permission uri=${LogRedaction.uri(uri)} " +
                    "flags=$flags folders=${_persistedFoldersFlow.value.size}",
            )
            true
        } catch (se: SecurityException) {
            FirebaseCrashlytics.getInstance().recordException(se)
            TimberLogger.logE(
                "ComicsFolderRepository",
                "[Comiqueta][ComicsFolder] failed to take persistable permission " +
                    "uri=${LogRedaction.uri(uri)} flags=$flags",
                se
            )
            false
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            TimberLogger.logE(
                "ComicsFolderRepository",
                "[Comiqueta][ComicsFolder] error taking permission uri=${LogRedaction.uri(uri)}",
                ex,
            )
            false
        }
    }

    override fun releasePersistablePermission(
        uri: Uri,
        flags: Int
    ): Boolean {
        return try {
            contentResolver.releasePersistableUriPermission(uri, flags)
            _persistedFoldersFlow.value = fetchCurrentPersistedPermissions()
            TimberLogger.logI(
                "ComicsFolderRepository",
                "[Comiqueta][ComicsFolder] released persistable permission uri=${LogRedaction.uri(uri)} " +
                    "flags=$flags folders=${_persistedFoldersFlow.value.size}",
            )
            true
        } catch (se: SecurityException) {
            FirebaseCrashlytics.getInstance().recordException(se)
            TimberLogger.logE(
                "ComicsFolderRepository",
                "[Comiqueta][ComicsFolder] failed to release persistable permission " +
                    "uri=${LogRedaction.uri(uri)} flags=$flags",
                se
            )
            // If the permission was not granted or already released.
            false
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            TimberLogger.logE(
                "ComicsFolderRepository",
                "[Comiqueta][ComicsFolder] error releasing permission uri=${LogRedaction.uri(uri)}",
                ex,
            )
            false
        }
    }
}
