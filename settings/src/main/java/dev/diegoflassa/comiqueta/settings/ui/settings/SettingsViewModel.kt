package dev.diegoflassa.comiqueta.settings.ui.settings

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.AddMonitoredFolderUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.GetMonitoredFoldersUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.RemoveMonitoredFolderUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.permission.GetRelevantOsPermissionsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
open class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val applicationContext: Context,
    private val getMonitoredFoldersUseCase: GetMonitoredFoldersUseCase,
    private val addMonitoredFolderUseCase: AddMonitoredFolderUseCase,
    private val removeMonitoredFolderUseCase: RemoveMonitoredFolderUseCase,
    private val getRelevantOsPermissionsUseCase: GetRelevantOsPermissionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUIState(isLoading = true))
    open val uiState: StateFlow<SettingsUIState> = _uiState.asStateFlow()

    private val _effect = Channel<SettingsEffect>(Channel.BUFFERED)
    open val effect: Flow<SettingsEffect> = _effect.receiveAsFlow()

    init {
        // Initial data load and permission status setup
        processIntent(SettingsIntent.LoadInitialData)
        val initialPermissions = getRelevantOsPermissions()
        val initialGrantStatuses = initialPermissions.associateWith { permission ->
            PermissionDisplayStatus(
                isGranted = ContextCompat.checkSelfPermission(
                    applicationContext,
                    permission
                ) == PackageManager.PERMISSION_GRANTED,
                shouldShowRationale = false
            )
        }
        _uiState.update { it.copy(permissionDisplayStatuses = initialGrantStatuses) }
    }

    private fun loadPersistedFolders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val persistedUris = getMonitoredFoldersUseCase().first()
                _uiState.update { currentState ->
                    currentState.copy(comicsFolders = persistedUris, isLoading = false)
                }
            } catch (e: Exception) {
                TimberLogger.logE("SettingsViewModel", "Error loading persisted folders via UseCase", e)
                _uiState.update { it.copy(isLoading = false) }
                viewModelScope.launch { _effect.send(SettingsEffect.ShowToast("Error loading folders: ${e.message}")) }
            }
        }
    }

    // THIS IS THE UPDATED FUNCTION
    open fun processIntent(intent: SettingsIntent) {
        viewModelScope.launch {
            when (intent) {
                is SettingsIntent.LoadInitialData -> {
                    loadPersistedFolders()
                    // Optionally, if an activity context were available here without an explicit Refresh intent,
                    // one could call refreshOsPermissionDisplayStatuses. However, it's usually triggered
                    // by the UI when it becomes active.
                }

                is SettingsIntent.RefreshPermissionStatuses -> {
                    refreshOsPermissionDisplayStatuses(intent.activity)
                }

                is SettingsIntent.RequestPermission -> {
                    _effect.send(SettingsEffect.LaunchPermissionRequest(listOf(intent.permission)))
                }

                is SettingsIntent.PermissionResults -> {
                    handleOsPermissionResults(intent.results)
                }

                is SettingsIntent.RemoveFolderClicked -> {
                    removeFolder(intent.folderUri)
                }

                is SettingsIntent.OpenAppSettingsClicked -> {
                    _effect.send(SettingsEffect.NavigateToAppSettingsScreen)
                }

                is SettingsIntent.RequestAddFolder -> {
                    _effect.send(SettingsEffect.LaunchFolderPicker)
                }

                is SettingsIntent.FolderSelected -> {
                    addFolder(intent.uri)
                }

                is SettingsIntent.OpenFolder -> {
                    _effect.send(SettingsEffect.LaunchViewFolderIntent(intent.uri))
                }

                is SettingsIntent.NavigateToCategoriesClicked -> {
                    _effect.send(SettingsEffect.NavigateToCategoriesScreen)
                }
            }
        }
    }

    private fun getRelevantOsPermissions(): List<String> {
        return getRelevantOsPermissionsUseCase()
    }

    private fun refreshOsPermissionDisplayStatuses(activity: Activity) {
        val relevantPermissions = getRelevantOsPermissions()
        if (relevantPermissions.isEmpty()) {
            _uiState.update { it.copy(permissionDisplayStatuses = emptyMap()) } // Corrected line
            return
        }
        val newStatuses = relevantPermissions.associateWith { permission ->
            val isGranted = ContextCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED
            val shouldShowRationale =
                !isGranted && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    permission
                )
            PermissionDisplayStatus(isGranted, shouldShowRationale)
        }
        _uiState.update { currentState ->
            currentState.copy(permissionDisplayStatuses = newStatuses)
        }
    }

    private fun handleOsPermissionResults(results: Map<String, Boolean>) {
        _uiState.update { currentState ->
            val updatedStatuses = currentState.permissionDisplayStatuses.toMutableMap()
            results.forEach { (permission, isGranted) ->
                updatedStatuses[permission] =
                    currentState.permissionDisplayStatuses[permission]?.copy(
                        isGranted = isGranted,
                        shouldShowRationale = false // Rationale needs re-check via ActivityCompat if denied
                    ) ?: PermissionDisplayStatus(isGranted = isGranted, shouldShowRationale = false)
            }
            currentState.copy(permissionDisplayStatuses = updatedStatuses)
        }
    }

    private suspend fun removeFolder(folderUri: Uri) {
        try {
            val success = removeMonitoredFolderUseCase(folderUri)
            if (success) {
                TimberLogger.logD("SettingsViewModel", "Successfully removed folder via UseCase: $folderUri")
                _effect.send(SettingsEffect.ShowToast("Folder '${Uri.decode(folderUri.toString())}' access removed."))
            } else {
                TimberLogger.logW("SettingsViewModel", "Failed to remove folder via UseCase: $folderUri.")
                _effect.send(
                    SettingsEffect.ShowToast(
                        "Could not remove access for folder '${ // Corrected line
                            Uri.decode(
                                folderUri.toString()
                            )
                        }'."
                    )
                )
            }
        } catch (e: Exception) {
            TimberLogger.logE("SettingsViewModel", "Error removing folder $folderUri via UseCase", e)
            _effect.send(SettingsEffect.ShowToast("Error removing folder: ${e.message}"))
        } finally {
            loadPersistedFolders()
        }
    }

    private suspend fun addFolder(uri: Uri) {
        // Assumption: The UI layer (SettingsScreen) has already successfully called
        // contentResolver.takePersistableUriPermission() before sending the FolderSelected intent.
        try {
            val success = addMonitoredFolderUseCase(uri)
            if (success) {
                TimberLogger.logD("SettingsViewModel", "Successfully added folder via UseCase: $uri")
                _effect.send(SettingsEffect.ShowToast("Folder '${Uri.decode(uri.toString())}' added."))
            } else {
                TimberLogger.logW("SettingsViewModel", "Failed to add folder via UseCase: $uri.")
                _effect.send(
                    SettingsEffect.ShowToast(
                        "Could not save access for folder '${
                            Uri.decode(
                                uri.toString()
                            )
                        }'."
                    )
                )
            }
        } catch (e: Exception) {
            TimberLogger.logE("SettingsViewModel", "Error adding folder $uri via UseCase", e)
            _effect.send(SettingsEffect.ShowToast("Error adding folder: ${e.message}"))
        } finally {
            loadPersistedFolders() // Refresh the list from the source of truth
        }
    }
}
