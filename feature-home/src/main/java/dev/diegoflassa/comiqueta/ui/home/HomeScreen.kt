package dev.diegoflassa.comiqueta.ui.home

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.domain.model.Comic
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.navigation.Screen.Viewer
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel
import dev.diegoflassa.comiqueta.home.R
import kotlinx.coroutines.flow.collectLatest

const val COMIC_COVER_ASPECT_RATIO = 2f / 3f

private const val tag = "HomeScreen"

@Composable
fun HomeScreen(
    navigationViewModel: NavigationViewModel = hiltActivityViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    TimberLogger.logI(tag, "[Comiqueta][Home] HomeScreen")

    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(), onResult = { uri: Uri? ->
            if (uri != null) {
                homeViewModel.reduce(HomeIntent.FolderSelected(uri))
            } else {
                homeViewModel.reduce(HomeIntent.FolderPickerCancelled)
            }
        })

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(), onResult = { isGranted: Boolean ->
            homeViewModel.reduce(HomeIntent.FolderPermissionResult(isGranted))
        })

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        homeViewModel.reduce(HomeIntent.CheckInitialFolderPermission)
        homeViewModel.reduce(HomeIntent.LoadComics)
    }
    val actionLabel = stringResource(R.string.retry)
    LaunchedEffect(key1 = homeViewModel.effect) {
        homeViewModel.effect.collectLatest { effect ->
            when (effect) {
                is HomeEffect.NavigateTo -> {
                    navigationViewModel.navigateTo(effect.screen)
                }

                is HomeEffect.NavigateToComicDetail -> {
                    navigationViewModel.navigateTo(Viewer(effect.comicPath ?: Uri.EMPTY))
                }

                is HomeEffect.RequestStoragePermission -> {
                    // This specific effect with a parameter is still here as per HomeEffect.kt
                    // If it's not used, it can be removed from HomeEffect.kt
                    requestPermissionLauncher.launch(effect.permission)
                }

                is HomeEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                is HomeEffect.ShowErrorWithRetry -> {
                    val result = snackbarHostState.showSnackbar(
                        message = effect.message,
                        actionLabel = actionLabel,
                        duration = SnackbarDuration.Indefinite
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        effect.onRetry()
                    }
                }

                HomeEffect.OpenFolderPicker -> {
                    folderPickerLauncher.launch(null)
                }

                HomeEffect.RequestGeneralStoragePermission -> {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    requestPermissionLauncher.launch(permission)
                }
            }
        }
    }
    val comics: LazyPagingItems<Comic> = homeViewModel.comicsFlow.collectAsLazyPagingItems()
    val latestComics: LazyPagingItems<Comic> =
        homeViewModel.latestComicsFlow.collectAsLazyPagingItems()
    val favoriteComics: LazyPagingItems<Comic> =
        homeViewModel.favoriteComicsFlow.collectAsLazyPagingItems()

    LaunchedEffect(comics.loadState) {
        TimberLogger.logD("Comics LoadState", "[Comiqueta][Home] ${comics.loadState}")
    }
    LaunchedEffect(latestComics.loadState) {
        TimberLogger.logD("Latest Comics LoadState", "[Comiqueta][Home] ${latestComics.loadState}")
    }
    LaunchedEffect(favoriteComics.loadState) {
        TimberLogger.logD("Favorite Comics LoadState", "[Comiqueta][Home] ${favoriteComics.loadState}")
    }
    HomeScreenContent(
        config = homeViewModel.config,
        comics = comics,
        latestComics = latestComics,
        favoriteComics = favoriteComics,
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onIntent = homeViewModel::reduce
    )
}
