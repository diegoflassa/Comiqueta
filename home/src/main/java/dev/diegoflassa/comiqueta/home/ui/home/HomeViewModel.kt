package dev.diegoflassa.comiqueta.home.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.repository.ComicsFolderRepository
import dev.diegoflassa.comiqueta.core.data.repository.ComicsRepository
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.domain.usecase.EnqueueSafFolderScanWorkerUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
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
                is HomeIntent.LoadInitialData -> loadAllComicData()
                is HomeIntent.SearchComics -> _uiState.update { it.copy(searchQuery = intent.query) }
                is HomeIntent.SelectCategory -> _uiState.update { it.copy(selectedCategory = intent.category) }
                is HomeIntent.AddFolderClicked -> handleAddFolderClicked()
                is HomeIntent.FolderSelected -> handleFolderSelected(intent.uri)
                is HomeIntent.FolderPermissionResult -> handleFolderPermissionResult(intent.isGranted)
                is HomeIntent.CheckInitialFolderPermission -> checkFolderPermissionStatus()
            }
        }
    }

    private fun loadAllComicData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            comicsRepository.getAllComics()
                .catch { e ->
                    Log.e("HomeViewModel", "Error loading all comics", e)
                    _effect.send(HomeEffect.ShowToast("Error loading comics: ${e.message}"))
                    _uiState.update { it.copy(isLoading = false) }
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
                            isLoading = false
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
            _uiState.update { it.copy(isFolderPermissionGranted = isGranted) }
        } else {
            _uiState.update { it.copy(isFolderPermissionGranted = true) }
        }
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
        _uiState.update { it.copy(isFolderPermissionGranted = isGranted) }
        if (isGranted) {
            _effect.send(HomeEffect.LaunchFolderPicker)
        } else {
            _effect.send(HomeEffect.ShowToast("Storage permission is needed to select folders."))
        }
    }

    private suspend fun handleFolderSelected(uri: Uri) {
        Log.d("HomeViewModel", "Folder selected and permission should be taken by UI: $uri")
        _effect.send(HomeEffect.ShowToast("Folder selected: ${uri.path}. Scanning should start."))
        triggerFolderScan()
        val success = comicsFolderRepository.takePersistablePermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        if (success) {
            // Trigger scan worker
            _effect.send(HomeEffect.ShowToast("Background scan started."))
            triggerFolderScan()
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
                TimberLogger.logE("YourViewModel", "Failed to enqueue folder scan worker", ex)
                _effect.send(HomeEffect.ShowToast("Error starting scan: ${ex.message}"))
            }
        }
    }
}
