package dev.diegoflassa.comiqueta.settings.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
// import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsFoldersDao // REMOVED
// import dev.diegoflassa.comiqueta.core.data.database.entity.ComicsFolderEntity // REMOVED or not used directly
import dev.diegoflassa.comiqueta.core.data.repository.ComicsFolderRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Definitions for State, Intent, and Effect (co-located or in separate files)

data class PermissionDisplayStatus(
    val isGranted: Boolean,
    val shouldShowRationale: Boolean
)

data class SettingsUIState(
    val comicsFolders: List<Uri> = emptyList(), // Changed to List<Uri>
    val permissionDisplayStatuses: Map<String, PermissionDisplayStatus> = emptyMap(),
    val isLoading: Boolean = false
)

sealed interface SettingsEffect {
    data class LaunchPermissionRequest(val permissionsToRequest: List<String>) : SettingsEffect
    data object NavigateToAppSettingsScreen : SettingsEffect
    data class ShowToast(val message: String) : SettingsEffect
}

sealed interface SettingsIntent {
    data object LoadInitialData : SettingsIntent // To explicitly load/refresh folders
    data class RefreshPermissionStatuses(val activity: Activity) : SettingsIntent
    data class RequestPermission(val permission: String) : SettingsIntent
    data class PermissionResults(val results: Map<String, Boolean>) : SettingsIntent
    data class RemoveFolderClicked(val folderUri: Uri) : SettingsIntent // Changed to Uri
    data object OpenAppSettingsClicked : SettingsIntent
}

// Helper functions (can be here or in a separate utility file within the module)
fun getPermissionFriendlyNameSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE -> "Read External Storage"
        else -> permission.substringAfterLast(".").replace("_", " ")
    }
}

fun getPermissionDescriptionSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE -> "Allows access to files on your device's storage to read comic files (for older Android versions)."
        else -> "This permission is required for certain app features to work correctly."
    }
}

fun getPermissionRationaleSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE -> "To load your comics on this Android version, the app needs to access files. Please grant access."
        else -> "This permission is important for the feature you are trying to use."
    }
}


@HiltViewModel
open class SettingsViewModel @Inject constructor(
    private val comicsFolderRepository: ComicsFolderRepository, // For URI permissions
    // private val comicsFoldersDao: ComicsFoldersDao, // REMOVED
    private val applicationContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUIState(isLoading = true)) // Start with loading true
    open val uiState: StateFlow<SettingsUIState> = _uiState.asStateFlow()

    private val _effect = Channel<SettingsEffect>(Channel.BUFFERED)
    open val effect: Flow<SettingsEffect> = _effect.receiveAsFlow()

    init {
        // Initial load of data
        processIntent(SettingsIntent.LoadInitialData)

        // Initial permission status check (for non-folder related permissions if any)
        val initialPermissions = getRelevantOsPermissions() // Renamed for clarity
        val initialGrantStatuses = initialPermissions.associateWith { permission ->
            PermissionDisplayStatus(
                isGranted = ContextCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED,
                shouldShowRationale = false // Rationale needs an Activity context, refresh later
            )
        }
        _uiState.update { it.copy(permissionDisplayStatuses = initialGrantStatuses) }
    }

    private fun loadPersistedFolders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val persistedUris = comicsFolderRepository.getPersistedPermissions()
                _uiState.update { currentState ->
                    currentState.copy(comicsFolders = persistedUris, isLoading = false)
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error loading persisted folders", e)
                _uiState.update { it.copy(isLoading = false) }
                _effect.send(SettingsEffect.ShowToast("Error loading folders: ${e.message}"))
            }
        }
    }


    open fun processIntent(intent: SettingsIntent) {
        viewModelScope.launch {
            when (intent) {
                is SettingsIntent.LoadInitialData -> {
                    loadPersistedFolders()
                    // Optionally, also refresh OS permission statuses if activity context isn't available yet
                    // For instance, if an activity is passed with LoadInitialData, use it.
                    // Otherwise, they will be refreshed when RefreshPermissionStatuses is called.
                }
                is SettingsIntent.RefreshPermissionStatuses -> {
                    refreshOsPermissionDisplayStatuses(intent.activity) // Renamed for clarity
                }
                is SettingsIntent.RequestPermission -> {
                    _effect.send(SettingsEffect.LaunchPermissionRequest(listOf(intent.permission)))
                }
                is SettingsIntent.PermissionResults -> {
                    handleOsPermissionResults(intent.results) // Renamed for clarity
                }
                is SettingsIntent.RemoveFolderClicked -> {
                    removeFolder(intent.folderUri)
                }
                is SettingsIntent.OpenAppSettingsClicked -> {
                    _effect.send(SettingsEffect.NavigateToAppSettingsScreen)
                }
            }
        }
    }

    // Renamed to avoid confusion with folder/URI permissions
    private fun getRelevantOsPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            emptyList() // No specific OS-level file permissions needed for SAF access on Android T+
        }
    }

    // Renamed to avoid confusion
    private fun refreshOsPermissionDisplayStatuses(activity: Activity) {
        val relevantPermissions = getRelevantOsPermissions()
        if (relevantPermissions.isEmpty()) {
            _uiState.update { it.copy(permissionDisplayStatuses = emptyMap()) } // Removed isLoading update here
            return
        }

        val newStatuses = relevantPermissions.associateWith { permission ->
            val isGranted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            val shouldShowRationale = !isGranted && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            PermissionDisplayStatus(isGranted, shouldShowRationale)
        }
        _uiState.update { currentState ->
            currentState.copy(permissionDisplayStatuses = newStatuses) // Removed isLoading update
        }
    }

    // Renamed to avoid confusion
    private fun handleOsPermissionResults(results: Map<String, Boolean>) {
        _uiState.update { currentState ->
            val updatedStatuses = currentState.permissionDisplayStatuses.toMutableMap()
            results.forEach { (permission, isGranted) ->
                updatedStatuses[permission] = currentState.permissionDisplayStatuses[permission]?.copy(
                    isGranted = isGranted,
                    // Rationale should be re-checked with ActivityCompat after a denial if needed
                    // For simplicity, keeping it false here, rely on next RefreshPermissionStatuses call
                    shouldShowRationale = false
                ) ?: PermissionDisplayStatus(isGranted = isGranted, shouldShowRationale = false)
            }
            currentState.copy(permissionDisplayStatuses = updatedStatuses)
        }
    }

    private suspend fun removeFolder(folderUri: Uri) {
        // 1. Release persistable URI permission using ComicsFolderRepository
        val permissionReleased = comicsFolderRepository.releasePersistablePermission(
            uri = folderUri,
            // Ensure these are the flags used when the permission was taken.
            // Typically READ for folders. Add WRITE if it was also taken.
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        if (permissionReleased) {
            Log.d("SettingsViewModel", "Successfully released permission for $folderUri")
            // 2. Refresh the list of folders from the repository
            loadPersistedFolders() // This will update the UI state
            _effect.send(SettingsEffect.ShowToast("Folder '${Uri.decode(folderUri.toString())}' access released."))

        } else {
            Log.w("SettingsViewModel", "Failed to release permission for $folderUri.")
            // Optionally, inform the user more specifically if the folder wasn't in the persisted list.
            _effect.send(SettingsEffect.ShowToast("Could not release access for folder '${Uri.decode(folderUri.toString())}'."))
            // Refresh the list anyway, in case the internal state of persisted permissions changed for other reasons.
            loadPersistedFolders()
        }
    }
}
