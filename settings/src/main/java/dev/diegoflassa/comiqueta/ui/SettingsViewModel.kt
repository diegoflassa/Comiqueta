package dev.diegoflassa.comiqueta.ui

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.IAddMonitoredFolderUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.IGetMonitoredFoldersUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.IRemoveMonitoredFolderUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.permission.IGetRelevantOsPermissionsUseCase
import dev.diegoflassa.comiqueta.data.model.PermissionDisplayStatus
import dev.diegoflassa.comiqueta.domain.usecase.IRefreshPermissionDisplayStatusUseCase
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
    private val getMonitoredFoldersUseCase: IGetMonitoredFoldersUseCase,
    private val addMonitoredFolderUseCase: IAddMonitoredFolderUseCase,
    private val removeMonitoredFolderUseCase: IRemoveMonitoredFolderUseCase,
    getRelevantOsPermissionsUseCase: IGetRelevantOsPermissionsUseCase,
    private val refreshPermissionDisplayStatusUseCase: IRefreshPermissionDisplayStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUIState(isLoading = true))
    open val uiState: StateFlow<SettingsUIState> = _uiState.asStateFlow()

    private val _effect = Channel<SettingsEffect>(Channel.BUFFERED)
    open val effect: Flow<SettingsEffect> = _effect.receiveAsFlow()

    init {
        processIntent(SettingsIntent.LoadInitialData)
        // Initial permission status setup without activity (no rationale check here)
        val initialPermissions = getRelevantOsPermissionsUseCase()
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

    open fun processIntent(intent: SettingsIntent) {
        viewModelScope.launch {
            when (intent) {
                is SettingsIntent.LoadInitialData -> {
                    loadPersistedFolders()
                }

                is SettingsIntent.RefreshPermissionStatuses -> {
                    refreshOsPermissionDisplayStatuses(intent.activity)
                }

                is SettingsIntent.RequestPermission -> {
                    _effect.send(SettingsEffect.LaunchPermissionRequest(listOf(intent.permission)))
                }

                is SettingsIntent.PermissionResults -> {
                    handleOsPermissionResults(intent.results, intent.activity)
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

    // Removed private fun getRelevantOsPermissions() as it's directly called from UseCase or its functionality is within Refresh UseCase

    private fun refreshOsPermissionDisplayStatuses(activity: Activity) {
        val newStatuses = refreshPermissionDisplayStatusUseCase(activity)
        _uiState.update { currentState ->
            currentState.copy(permissionDisplayStatuses = newStatuses)
        }
    }

    private fun handleOsPermissionResults(results: Map<String, PermissionDisplayStatus>, activity: Activity) {
        _uiState.update { currentState ->
            // After permission results, it's best to refresh all statuses using the use case,
            // as shouldShowRationale might have changed for denied permissions.
            val refreshedStatuses = refreshPermissionDisplayStatusUseCase(activity)
            currentState.copy(permissionDisplayStatuses = refreshedStatuses)
        }
        // Optionally, could still iterate through 'results' to show specific toasts for granted/denied,
        // but the UI state is now fully updated by the use case.
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
                        "Could not remove access for folder '${
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
