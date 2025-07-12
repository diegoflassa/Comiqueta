package dev.diegoflassa.comiqueta.viewer.ui.viewer

import android.content.Context
import android.net.Uri
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
open class ViewerViewModel @Inject constructor(
    @param:ApplicationContext private val applicationContext: Context,
    private val getMonitoredFoldersUseCase: GetMonitoredFoldersUseCase,
    private val addMonitoredFolderUseCase: AddMonitoredFolderUseCase,
    private val removeMonitoredFolderUseCase: RemoveMonitoredFolderUseCase,
    private val getRelevantOsPermissionsUseCase: GetRelevantOsPermissionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUIState(isLoading = true))
    open val uiState: StateFlow<ViewerUIState> = _uiState.asStateFlow()

    private val _effect = Channel<ViewerEffect>(Channel.BUFFERED)
    open val effect: Flow<ViewerEffect> = _effect.receiveAsFlow()

    init {
        // Initial data load and permission status setup
        processIntent(ViewerIntent.LoadInitialData)
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
                viewModelScope.launch { _effect.send(ViewerEffect.ShowToast("Error loading folders: ${e.message}")) }
            }
        }
    }

    // THIS IS THE UPDATED FUNCTION
    open fun processIntent(intent: ViewerIntent) {
        viewModelScope.launch {
            when (intent) {
                is ViewerIntent.LoadInitialData -> {
                    loadPersistedFolders()
                    // Optionally, if an activity context were available here without an explicit Refresh intent,
                    // one could call refreshOsPermissionDisplayStatuses. However, it's usually triggered
                    // by the UI when it becomes active.
                }

                is ViewerIntent.RefreshPermissionStatuses -> {
                    //refreshOsPermissionDisplayStatuses(intent.activity)
                }

                is ViewerIntent.RequestPermission -> {
                    _effect.send(ViewerEffect.LaunchPermissionRequest(listOf(intent.permission)))
                }

                is ViewerIntent.PermissionResults -> {
                    //handleOsPermissionResults(intent.results)
                }

                is ViewerIntent.RemoveFolderClicked -> {
                    removeFolder(intent.folderUri)
                }

                is ViewerIntent.OpenAppViewerClicked -> {
                    _effect.send(ViewerEffect.NavigateToAppViewerScreen)
                }

                // --- ADDED CASES ---
                is ViewerIntent.RequestAddFolder -> {
                    _effect.send(ViewerEffect.LaunchFolderPicker)
                }

                is ViewerIntent.FolderSelected -> {
                    addFolder(intent.uri)
                }
                // --- END OF ADDED CASES ---
            }
        }
    }

    private suspend fun removeFolder(folderUri: Uri) {
        try {
            val success = removeMonitoredFolderUseCase(folderUri)
            if (success) {
                TimberLogger.logD("SettingsViewModel", "Successfully removed folder via UseCase: $folderUri")
                _effect.send(ViewerEffect.ShowToast("Folder '${Uri.decode(folderUri.toString())}' access removed."))
            } else {
                TimberLogger.logW("SettingsViewModel", "Failed to remove folder via UseCase: $folderUri.")
                _effect.send(
                    ViewerEffect.ShowToast(
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
            _effect.send(ViewerEffect.ShowToast("Error removing folder: ${e.message}"))
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
                _effect.send(ViewerEffect.ShowToast("Folder '${Uri.decode(uri.toString())}' added."))
            } else {
                TimberLogger.logW("SettingsViewModel", "Failed to add folder via UseCase: $uri.")
                _effect.send(
                    ViewerEffect.ShowToast(
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
            _effect.send(ViewerEffect.ShowToast("Error adding folder: ${e.message}"))
        } finally {
            loadPersistedFolders() // Refresh the list from the source of truth
        }
    }
}
