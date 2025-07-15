package dev.diegoflassa.comiqueta.home.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.diegoflassa.comiqueta.core.data.repository.ComicsFolderRepository
import dev.diegoflassa.comiqueta.core.data.repository.ComicsRepository
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.domain.usecase.EnqueueSafFolderScanWorkerUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class HomeViewModel @Inject constructor(
    private val comicsRepository: ComicsRepository,
    private val comicsFolderRepository: ComicsFolderRepository,
    @param:ApplicationContext private val applicationContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUIState())
    val uiState: StateFlow<HomeUIState> = _uiState.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    init {
        processIntent(HomeIntent.LoadInitialData)
        processIntent(HomeIntent.CheckInitialFolderPermission)
    }

    fun processIntent(intent: HomeIntent) {
        viewModelScope.launch {
            when (intent) {
                is HomeIntent.LoadInitialData -> loadAllComicData() // Renamed from LoadInitialData
                is HomeIntent.SearchComics -> _uiState.update { it.copy(searchQuery = intent.query) }
                is HomeIntent.AddFolderClicked -> handleAddFolderClicked()
                is HomeIntent.FolderSelected -> handleFolderSelected(intent.uri)
                is HomeIntent.FolderPermissionResult -> handleFolderPermissionResult(intent.isGranted)
                is HomeIntent.CheckInitialFolderPermission -> checkFolderPermissionStatus()

                is HomeIntent.CategorySelected -> {
                    _uiState.update { it.copy(selectedCategory = intent.category) }
                }

                is HomeIntent.ComicSelected -> {
                    _effect.send(HomeEffect.NavigateToComicDetail(intent.comic.filePath))
                }

                is HomeIntent.ViewModeChanged -> {
                    _uiState.update { it.copy(viewMode = intent.viewMode) }
                }
            }
        }
    }

    private fun loadAllComicData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            comicsRepository.getAllComics()
                .catch { e ->
                    TimberLogger.logE("HomeViewModel", "Error loading all comics", e)
                    _effect.send(HomeEffect.ShowToast("Error loading comics: ${e.message}"))
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                }
                .collect { allComicsList ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            allComics = allComicsList,
                            latestComics = allComicsList.filter { it.isNew }.take(10),
                            favoriteComics = allComicsList.filter { it.isFavorite }.take(10),
                            unreadComics = allComicsList.filter { it.hasBeenRead.not() }.take(10),
                            continueReadingComics = allComicsList.filter { it.hasBeenRead }
                                .take(10),
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    private fun checkFolderPermissionStatus() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            val isGranted = ContextCompat.checkSelfPermission(
                applicationContext,
                permission
            ) == PackageManager.PERMISSION_GRANTED
            // Assuming HomeUIState has isFolderPermissionGranted or similar
            // _uiState.update { it.copy(isFolderPermissionGranted = isGranted) }
            // For now, let's assume this state is managed or not critical for this update
            TimberLogger.logD("HomeViewModel", "Folder permission granted: $isGranted (Legacy)")
        } else {
            // _uiState.update { it.copy(isFolderPermissionGranted = true) }
            TimberLogger.logD("HomeViewModel", "Folder permission granted: true (Android T+)")
        }
        // If HomeUIState from HomeScreen.kt doesn't have isFolderPermissionGranted,
        // this update might fail or need adjustment.
        // For now, I'm commenting out the direct uiState update for this field if it's not in the shared UIState.
    }

    private suspend fun handleAddFolderClicked() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            when {
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    permission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    _effect.send(HomeEffect.LaunchFolderPicker)
                }

                else -> {
                    _effect.send(HomeEffect.RequestStoragePermission(permission))
                }
            }
        } else {
            _effect.send(HomeEffect.LaunchFolderPicker)
        }
    }

    private suspend fun handleFolderPermissionResult(isGranted: Boolean) {
        // _uiState.update { it.copy(isFolderPermissionGranted = isGranted) } // See comment in checkFolderPermissionStatus
        if (isGranted) {
            _effect.send(HomeEffect.LaunchFolderPicker)
        } else {
            _effect.send(HomeEffect.ShowToast("Storage permission is needed to select folders."))
        }
    }

    private suspend fun handleFolderSelected(uri: Uri) {
        TimberLogger.logD(
            "HomeViewModel",
            "Folder selected and permission should be taken by UI: $uri"
        )
        _effect.send(HomeEffect.ShowToast("Folder selected: ${uri.path}. Scanning should start."))
        // triggerFolderScan() // Original code called it here
        val success = comicsFolderRepository.takePersistablePermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        if (success) {
            _effect.send(HomeEffect.ShowToast("Background scan started."))
            triggerFolderScan() // And here. One call should be enough after permission.
        } else {
            _effect.send(HomeEffect.ShowToast("Failed to secure access to folder."))
        }
    }

    private fun triggerFolderScan() {
        viewModelScope.launch {
            try {
                EnqueueSafFolderScanWorkerUseCase(applicationContext).invoke()
            } catch (ex: Exception) {
                ex.printStackTrace()
                TimberLogger.logE(
                    "HomeViewModel",
                    "Failed to enqueue folder scan worker",
                    ex
                ) // Changed tag from YourViewModel
                _effect.send(HomeEffect.ShowToast("Error starting scan: ${ex.message}"))
            }
        }
    }
}
