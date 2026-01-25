package dev.diegoflassa.comiqueta.ui.home

import android.Manifest
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.diegoflassa.comiqueta.core.data.config.IConfig
import dev.diegoflassa.comiqueta.core.domain.model.Category
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.extensions.toDp
import dev.diegoflassa.comiqueta.core.data.mappers.asExternalModel
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.domain.model.Comic
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.navigation.Screen
import dev.diegoflassa.comiqueta.core.navigation.Screen.Viewer
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.theme.getOutlinedTextFieldDefaultsColors
import dev.diegoflassa.comiqueta.core.theme.settingIconTint
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel
import dev.diegoflassa.comiqueta.core.ui.widgets.BannerAdView
import dev.diegoflassa.comiqueta.home.R
import dev.diegoflassa.comiqueta.ui.enums.ViewMode
import dev.diegoflassa.comiqueta.ui.widgets.CategoriesSection
import dev.diegoflassa.comiqueta.ui.widgets.ComicCoverItem
import dev.diegoflassa.comiqueta.ui.widgets.ComicListItem
import dev.diegoflassa.comiqueta.ui.widgets.EmptyStateContent
import dev.diegoflassa.comiqueta.ui.widgets.HomeBottomAppBar
import dev.diegoflassa.comiqueta.ui.widgets.HorizontalComicsRow
import dev.diegoflassa.comiqueta.ui.widgets.HorizontalComicsRowForPreview
import dev.diegoflassa.comiqueta.ui.widgets.SectionHeader
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf

const val COMIC_COVER_ASPECT_RATIO = 2f / 3f

// Banner ad height handled via ComiquetaTheme.dimen.bannerHeight

private const val tag = "HomeScreen"

@Composable
fun HomeScreen(
    navigationViewModel: NavigationViewModel = hiltActivityViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    TimberLogger.logI(tag, "HomeScreen")

    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(), onResult = { uri: Uri? ->
            uri?.let {
                homeViewModel.reduce(HomeIntent.FolderSelected(it))
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
        TimberLogger.logD("Comics LoadState", "${comics.loadState}")
    }
    LaunchedEffect(latestComics.loadState) {
        TimberLogger.logD("Latest Comics LoadState", "${latestComics.loadState}")
    }
    LaunchedEffect(favoriteComics.loadState) {
        TimberLogger.logD("Favorite Comics LoadState", "${favoriteComics.loadState}")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContentForPreview(
    modifier: Modifier = Modifier,
    config: IConfig? = null,
    comics: List<Comic>,
    latestComics: List<Comic>,
    favoriteComics: List<Comic>,
    uiState: HomeUIState,
    onIntent: ((HomeIntent) -> Unit)? = null,
) {
    val fabDiameter = ComiquetaTheme.dimen.fabDiameter.scaled()
    val bottomBarHeight = ComiquetaTheme.dimen.bottomBarHeight.scaled()
    val isEmpty =
        (comics.isEmpty()) && uiState.searchQuery.isBlank() && uiState.selectedCategory == null && uiState.isLoading.not()

    val topSystemBarInsetDp = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    Scaffold(
        modifier = modifier.background(ComiquetaTheme.colorScheme.background),
        topBar = {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topSystemBarInsetDp)
            )
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = ComiquetaTheme.dimen.appBarHorizontalPadding),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            stringResource(R.string.app_name),
                            style = ComiquetaTheme.typography.comiquetaTitleText.scaled()
                        )
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = ComiquetaTheme.dimen.appBarHorizontalPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onIntent?.invoke(HomeIntent.NavigateTo(Screen.Statistics)) }) {
                            Icon(
                                modifier = Modifier.size(ComiquetaTheme.dimen.iconSettings.scaled()),
                                imageVector = Icons.Outlined.Analytics,
                                tint = ComiquetaTheme.colorScheme.settingIconTint,
                                contentDescription = stringResource(R.string.statistics_title)
                            )
                        }
                        IconButton(onClick = { onIntent?.invoke(HomeIntent.NavigateTo(Screen.Settings)) }) {
                            Icon(
                                modifier = Modifier.size(ComiquetaTheme.dimen.iconSettings.scaled()),
                                painter = painterResource(id = dev.diegoflassa.comiqueta.core.R.drawable.ic_settings),
                                tint = ComiquetaTheme.colorScheme.settingIconTint,
                                contentDescription = stringResource(R.string.top_bar_settings_icon_desc)
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Content
            Column(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    isEmpty -> {
                        EmptyStateContent(onIntent = onIntent)
                    }

                    else -> {
                        ComicsContentForPreview(
                            comics = comics,
                            latestComics = latestComics,
                            favoriteComics = favoriteComics,
                            uiState = uiState,
                            onIntent = onIntent,
                        )
                    }
                }
            }

            // Overlaid Scan Progress
            AnimatedVisibility(
                visible = uiState.isScanningFolders || uiState.scanFinished,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(ComiquetaTheme.dimen.paddingMedium.scaled())
                        .zIndex(2f),
                    shape = RoundedCornerShape(8.dp.scaled()),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 4.dp.scaled(),
                    shadowElevation = 8.dp.scaled()
                ) {
                    Column(
                        modifier = Modifier
                            .clickable { onIntent?.invoke(HomeIntent.ToggleScanProgressMinimization) }
                            .padding(ComiquetaTheme.dimen.paddingSmall.scaled()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        uiState.scanFinished -> uiState.scanResultMessage
                                            ?: stringResource(R.string.scan_completed)

                                        uiState.scanTotalFiles > 0 -> {
                                            stringResource(
                                                R.string.scanning_folders_progress_detail,
                                                uiState.scanProgress,
                                                uiState.scanProcessedFiles,
                                                uiState.scanTotalFiles,
                                                uiState.processedComicsCount
                                            )
                                        }

                                        uiState.scanProgress > 0 -> {
                                            stringResource(
                                                R.string.scanning_folders_progress,
                                                uiState.scanProgress
                                            )
                                        }

                                        else -> {
                                            stringResource(R.string.scanning_folders)
                                        }
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                if (!uiState.isScanProgressMinimized && !uiState.scanFinished && uiState.currentComicName != null) {
                                    Text(
                                        text = stringResource(
                                            R.string.scanning_current_comic,
                                            uiState.currentComicName
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (uiState.scanFinished) {
                                androidx.compose.material3.TextButton(
                                    onClick = { onIntent?.invoke(HomeIntent.DismissScanResult) }
                                ) {
                                    Text(
                                        text = stringResource(R.string.scan_finished_ok),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (uiState.isScanProgressMinimized) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                    contentDescription = if (uiState.isScanProgressMinimized) stringResource(
                                        R.string.expand_progress
                                    ) else stringResource(R.string.minimize_progress),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        if (!uiState.isScanProgressMinimized && !uiState.scanFinished) {
                            Spacer(modifier = Modifier.height(8.dp.scaled()))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { uiState.scanProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp.scaled())
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }

            var showAds by remember { mutableStateOf(true) }
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            ) {
                HomeBottomAppBar(
                    uiState = uiState,
                    bottomBarHeight = bottomBarHeight,
                    onIntent = onIntent
                )
                if (showAds && config != null) {
                    BannerAdView(
                        adUnitId = config.addBannerId
                    )
                }
            }

            ExtendedFloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(1F)
                    .size(fabDiameter)
                    .offset(y = -ComiquetaTheme.dimen.fabOffset.scaled()),
                onClick = { onIntent?.invoke(HomeIntent.AddFolderClicked) },
                shape = CircleShape,
            ) {
                Icon(
                    modifier = Modifier.size(ComiquetaTheme.dimen.fabIconSize.scaled()),
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_fab_description)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    config: IConfig? = null,
    comics: LazyPagingItems<Comic>,
    latestComics: LazyPagingItems<Comic>,
    favoriteComics: LazyPagingItems<Comic>,
    uiState: HomeUIState,
    snackbarHostState: SnackbarHostState,
    onIntent: ((HomeIntent) -> Unit)? = null,
) {
    val fabDiameter = ComiquetaTheme.dimen.fabDiameter.scaled()
    val bottomBarHeight = ComiquetaTheme.dimen.bottomBarHeight.scaled()
    val isEmpty =
        (comics.itemCount == 0) && uiState.searchQuery.isBlank() && uiState.selectedCategory == null && uiState.isLoading.not()

    Column(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .weight(1f)
                .background(ComiquetaTheme.colorScheme.background),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    title = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(start = ComiquetaTheme.dimen.appBarHorizontalPadding),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                stringResource(R.string.app_name),
                                style = ComiquetaTheme.typography.comiquetaTitleText.scaled()
                            )
                        }
                    },
                    actions = {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(end = ComiquetaTheme.dimen.appBarHorizontalPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onIntent?.invoke(HomeIntent.NavigateTo(Screen.Statistics)) }) {
                                Icon(
                                    modifier = Modifier.size(ComiquetaTheme.dimen.iconSettings.scaled()),
                                    imageVector = Icons.Outlined.Analytics,
                                    tint = ComiquetaTheme.colorScheme.settingIconTint,
                                    contentDescription = stringResource(R.string.statistics_title)
                                )
                            }
                            IconButton(onClick = { onIntent?.invoke(HomeIntent.NavigateTo(Screen.Settings)) }) {
                                Icon(
                                    modifier = Modifier.size(ComiquetaTheme.dimen.iconSettings.scaled()),
                                    imageVector = Icons.Outlined.Settings,
                                    tint = ComiquetaTheme.colorScheme.settingIconTint,
                                    contentDescription = stringResource(R.string.top_bar_settings_icon_desc)
                                )
                            }
                        }
                    },
                )
            },
            bottomBar = {
                HomeBottomAppBar(
                    uiState = uiState,
                    bottomBarHeight = bottomBarHeight,
                    onIntent = onIntent
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    modifier = Modifier
                        .size(fabDiameter)
                        .offset(y = (ComiquetaTheme.dimen.fabOffset * 2.2f).scaled()),
                    onClick = { onIntent?.invoke(HomeIntent.AddFolderClicked) },
                    shape = CircleShape,
                    containerColor = ComiquetaTheme.colorScheme.primary,
                    contentColor = ComiquetaTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        modifier = Modifier.size(ComiquetaTheme.dimen.fabIconSize.scaled()),
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_fab_description)
                    )
                }
            },
            floatingActionButtonPosition = androidx.compose.material3.FabPosition.Center
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Main Content
                Column(modifier = Modifier.fillMaxSize()) {
                    when {
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        isEmpty -> {
                            EmptyStateContent(onIntent = onIntent)
                        }

                        else -> {
                            ComicsContent(
                                comics = comics,
                                latestComics = latestComics,
                                favoriteComics = favoriteComics,
                                uiState = uiState,
                                onIntent = onIntent,
                            )
                        }
                    }
                }

                // Overlaid Scan Progress
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.isScanningFolders || uiState.scanFinished,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(2f)
                ) {
                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ComiquetaTheme.dimen.paddingMedium.scaled()),
                        shape = RoundedCornerShape(8.dp.scaled()),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 4.dp.scaled(),
                        shadowElevation = 8.dp.scaled()
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable { onIntent?.invoke(HomeIntent.ToggleScanProgressMinimization) }
                                .padding(ComiquetaTheme.dimen.paddingSmall.scaled()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when {
                                            uiState.scanFinished -> uiState.scanResultMessage
                                                ?: stringResource(R.string.scan_completed)

                                            uiState.scanTotalFiles > 0 -> {
                                                stringResource(
                                                    R.string.scanning_folders_progress_detail,
                                                    uiState.scanProgress,
                                                    uiState.scanProcessedFiles,
                                                    uiState.scanTotalFiles,
                                                    uiState.processedComicsCount
                                                )
                                            }

                                            uiState.scanProgress > 0 -> {
                                                stringResource(
                                                    R.string.scanning_folders_progress,
                                                    uiState.scanProgress
                                                )
                                            }

                                            else -> {
                                                stringResource(R.string.scanning_folders)
                                            }
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    if (!uiState.isScanProgressMinimized && !uiState.scanFinished && uiState.currentComicName != null) {
                                        Text(
                                            text = stringResource(
                                                R.string.scanning_current_comic,
                                                uiState.currentComicName
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (uiState.scanFinished) {
                                    androidx.compose.material3.TextButton(
                                        onClick = { onIntent?.invoke(HomeIntent.DismissScanResult) }
                                    ) {
                                        Text(
                                            text = stringResource(R.string.scan_finished_ok),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (uiState.isScanProgressMinimized) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                        contentDescription = if (uiState.isScanProgressMinimized) stringResource(
                                            R.string.expand_progress
                                        ) else stringResource(R.string.minimize_progress),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            if (!uiState.isScanProgressMinimized && !uiState.scanFinished) {
                                Spacer(modifier = Modifier.height(8.dp.scaled()))
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { uiState.scanProgress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp.scaled())
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        val showAds by remember { mutableStateOf(true) }
        if (showAds && config != null) {
            BannerAdView(
                adUnitId = config.addBannerId
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicsContentForPreview(
    modifier: Modifier = Modifier,
    comics: List<Comic>,
    latestComics: List<Comic>,
    favoriteComics: List<Comic>,
    uiState: HomeUIState,
    onIntent: ((HomeIntent) -> Unit)? = null,
) {
    var sectionHeaderLatestViewMode by remember { mutableStateOf(ViewMode.GRID) }
    var sectionHeaderFavoritesViewMode by remember { mutableStateOf(ViewMode.GRID) }
    var sectionHeaderAllViewMode by remember { mutableStateOf(ViewMode.LIST) }

    var latestComicsExpanded by remember { mutableStateOf(true) }
    var favoriteComicsExpanded by remember { mutableStateOf(true) }
    var allComicsExpanded by remember { mutableStateOf(true) }

    val screenWidthDp = LocalWindowInfo.current.containerSize.width.dp
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onIntent?.invoke(HomeIntent.SearchComics(it)) },
                placeholder = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = stringResource(R.string.search_comics_placeholder),
                            style = ComiquetaTheme.typography.searchText.scaled()
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(ComiquetaTheme.dimen.iconSize.scaled()),
                        painter = painterResource(id = dev.diegoflassa.comiqueta.core.R.drawable.ic_search),
                        contentDescription = stringResource(R.string.search_icon_description),
                        tint = ComiquetaTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = ComiquetaTheme.dimen.searchTopPadding,
                        bottom = ComiquetaTheme.dimen.searchBottomPadding,
                        start = ComiquetaTheme.dimen.searchHorizontalPadding,
                        end = ComiquetaTheme.dimen.searchHorizontalPadding
                    )
                    .height(ComiquetaTheme.dimen.inputHeight.scaled())
                    .clip(RoundedCornerShape(ComiquetaTheme.dimen.searchRoundedCorner.scaled())),
                colors = getOutlinedTextFieldDefaultsColors(),
                singleLine = true
            )
        }

        if (latestComics.isNotEmpty()) {
            item { // Latest Comics Header
                SectionHeader(
                    title = stringResource(R.string.latest_comics_section_title),
                    isExpanded = latestComicsExpanded,
                    onHeaderClick = { latestComicsExpanded = !latestComicsExpanded },
                    currentViewMode = sectionHeaderLatestViewMode
                ) { newViewMode -> sectionHeaderLatestViewMode = newViewMode }
            }
            if (latestComicsExpanded) {
                item {
                    HorizontalComicsRowForPreview(comics = latestComics, onIntent = onIntent)
                    Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerMedium.scaled()))
                }
            }
        }

        if (favoriteComics.isNotEmpty()) {
            item { // Favorite Comics Header
                SectionHeader(
                    title = stringResource(R.string.favorite_comics_section_title),
                    isExpanded = favoriteComicsExpanded,
                    onHeaderClick = { favoriteComicsExpanded = !favoriteComicsExpanded },
                    currentViewMode = sectionHeaderFavoritesViewMode
                ) { newViewMode -> sectionHeaderFavoritesViewMode = newViewMode }
            }
            if (favoriteComicsExpanded) {
                item {
                    HorizontalComicsRowForPreview(comics = favoriteComics, onIntent = onIntent)
                    Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerMedium.scaled()))
                }
            }
        }

        if (uiState.categories.isNotEmpty()) {
            item { // Categories Section
                CategoriesSection(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategoryClicked = { category ->
                        onIntent?.invoke(HomeIntent.CategorySelected(category))
                    }
                )
            }
        }

        // "All Comics" / "Results" Section
        if (comics.isNotEmpty() || uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) {
            item { // All Comics / Results Header
                SectionHeader(
                    title = if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) uiState.selectedCategory?.name
                        ?: stringResource(
                            R.string.results_section_title
                        ) else stringResource(R.string.all_comics_section_title),
                    isExpanded = allComicsExpanded,
                    showGridListOption = true,
                    onHeaderClick = { allComicsExpanded = !allComicsExpanded },
                    currentViewMode = sectionHeaderAllViewMode
                ) { newViewMode -> sectionHeaderAllViewMode = newViewMode }
            }
            if (allComicsExpanded) {
                if (comics.isNotEmpty()) {
                    when (sectionHeaderAllViewMode) {
                        ViewMode.LIST -> {
                            items(
                                comics.size,
                                key = { index -> comics[index].filePath.toString() }) { index ->
                                ComicListItem(
                                    comic = comics[index],
                                    aspectRatio = COMIC_COVER_ASPECT_RATIO,
                                    onIntent = onIntent
                                )
                                Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.paddingSmall.scaled()))
                            }
                        }

                        ViewMode.GRID -> {
                            item {
                                val configuration = LocalWindowInfo.current
                                val screenHeight = configuration.containerSize.height.toDp()
                                val gridHeight = (screenHeight * 0.6f).coerceAtLeast(200.dp)
                                val currentGridColumnCount = when {
                                    screenWidthDp < 600.dp -> 3
                                    screenWidthDp < 840.dp -> 4
                                    else -> 5
                                }
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(currentGridColumnCount),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(gridHeight)
                                        .padding(horizontal = ComiquetaTheme.dimen.paddingLarge.scaled()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp.scaled()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp.scaled()),
                                    contentPadding = PaddingValues(vertical = 8.dp.scaled())
                                ) {
                                    items(
                                        count = comics.size,
                                        key = { index -> comics[index].filePath.toString() }
                                    ) { index ->
                                        ComicCoverItem(
                                            comic = comics[index],
                                            aspectRatio = COMIC_COVER_ASPECT_RATIO,
                                            onIntent = onIntent
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerMedium.scaled()))
                            }
                        }
                    }
                } else if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) {
                    item { // No Results
                        Text(
                            text = stringResource(R.string.no_comics_found_for_search),
                            modifier = Modifier
                                .padding(ComiquetaTheme.dimen.paddingLarge.scaled())
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.bottomBarHeight.scaled() + ComiquetaTheme.dimen.fabDiameter.scaled() / 2))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicsContent(
    // For actual app with LazyPagingItems
    modifier: Modifier = Modifier,
    comics: LazyPagingItems<Comic>,
    latestComics: LazyPagingItems<Comic>,
    favoriteComics: LazyPagingItems<Comic>,
    uiState: HomeUIState,
    onIntent: ((HomeIntent) -> Unit)? = null,
) {
    var sectionHeaderLatestViewMode by remember { mutableStateOf(ViewMode.GRID) }
    var sectionHeaderFavoritesViewMode by remember { mutableStateOf(ViewMode.GRID) }
    var sectionHeaderAllViewMode by remember { mutableStateOf(ViewMode.LIST) }

    var latestComicsExpanded by remember { mutableStateOf(true) }
    var favoriteComicsExpanded by remember { mutableStateOf(true) }
    var allComicsExpanded by remember { mutableStateOf(true) }

    val screenWidthDp = LocalWindowInfo.current.containerSize.width.dp
    val currentGridColumnCount = when {
        screenWidthDp < 600.dp -> 3
        screenWidthDp < 840.dp -> 4
        else -> 5
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onIntent?.invoke(HomeIntent.SearchComics(it)) },
                placeholder = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = stringResource(R.string.search_comics_placeholder),
                            style = ComiquetaTheme.typography.searchText.scaled()
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(ComiquetaTheme.dimen.iconSize.scaled()),
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.search_icon_description),
                        tint = ComiquetaTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = ComiquetaTheme.dimen.searchTopPadding,
                        bottom = ComiquetaTheme.dimen.searchBottomPadding,
                        start = ComiquetaTheme.dimen.searchHorizontalPadding,
                        end = ComiquetaTheme.dimen.searchHorizontalPadding
                    )
                    .height(ComiquetaTheme.dimen.inputHeight.scaled())
                    .clip(RoundedCornerShape(ComiquetaTheme.dimen.searchRoundedCorner.scaled())),
                colors = getOutlinedTextFieldDefaultsColors(),
                singleLine = true
            )
        }

        if (latestComics.itemCount > 0) {
            item { // Latest Comics Header
                SectionHeader(
                    title = stringResource(R.string.latest_comics_section_title),
                    isExpanded = latestComicsExpanded,
                    onHeaderClick = { latestComicsExpanded = !latestComicsExpanded },
                    currentViewMode = sectionHeaderLatestViewMode
                ) { newViewMode -> sectionHeaderLatestViewMode = newViewMode }
            }
            if (latestComicsExpanded) {
                item {
                    HorizontalComicsRow(comics = latestComics, onIntent = onIntent)
                    Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerMedium.scaled()))
                }
            }
        }

        if (favoriteComics.itemCount > 0) {
            item { // Favorite Comics Header
                SectionHeader(
                    title = stringResource(R.string.favorite_comics_section_title),
                    isExpanded = favoriteComicsExpanded,
                    onHeaderClick = { favoriteComicsExpanded = !favoriteComicsExpanded },
                    currentViewMode = sectionHeaderFavoritesViewMode
                ) { newViewMode -> sectionHeaderFavoritesViewMode = newViewMode }
            }
            if (favoriteComicsExpanded) {
                item {
                    HorizontalComicsRow(comics = favoriteComics, onIntent = onIntent)
                    Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerMedium.scaled()))
                }
            }
        }

        if (uiState.categories.isNotEmpty()) {
            item { // Categories Section
                CategoriesSection(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategoryClicked = { category ->
                        onIntent?.invoke(HomeIntent.CategorySelected(category))
                    }
                )
            }
        }

        // "All Comics" / "Results" Section
        if (comics.itemCount > 0 || uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) {
            item { // All Comics / Results Header
                SectionHeader(
                    title = if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) uiState.selectedCategory?.name
                        ?: stringResource(
                            R.string.results_section_title
                        ) else stringResource(R.string.all_comics_section_title),
                    isExpanded = allComicsExpanded,
                    showGridListOption = true,
                    onHeaderClick = { allComicsExpanded = !allComicsExpanded },
                    currentViewMode = sectionHeaderAllViewMode
                ) { newViewMode -> sectionHeaderAllViewMode = newViewMode }
            }

            if (allComicsExpanded) {
                if (comics.itemCount > 0) {
                    when (sectionHeaderAllViewMode) {
                        ViewMode.LIST -> {
                            items(
                                count = comics.itemCount,
                                key = comics.itemKey { it.filePath }
                            ) { index ->
                                val comic = comics[index]
                                ComicListItem(
                                    comic = comic,
                                    aspectRatio = COMIC_COVER_ASPECT_RATIO,
                                    onIntent = onIntent
                                )
                                Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerSmall.scaled()))
                            }
                        }

                        ViewMode.GRID -> {
                            // Chunking the paging items manually to simulate a grid within LazyColumn
                            val rowCount =
                                (comics.itemCount + currentGridColumnCount - 1) / currentGridColumnCount
                            items(rowCount) { rowIndex ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = ComiquetaTheme.dimen.paddingLarge.scaled()),
                                    horizontalArrangement = Arrangement.spacedBy(ComiquetaTheme.dimen.spacerSmall.scaled())
                                ) {
                                    for (columnIndex in 0 until currentGridColumnCount) {
                                        val itemIndex =
                                            rowIndex * currentGridColumnCount + columnIndex
                                        if (itemIndex < comics.itemCount) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                val comic = comics[itemIndex]
                                                ComicCoverItem(
                                                    comic = comic,
                                                    aspectRatio = COMIC_COVER_ASPECT_RATIO,
                                                    onIntent = onIntent
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerSmall.scaled()))
                            }
                        }
                    }
                } else if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) {
                    item {
                        Text(
                            text = stringResource(R.string.no_comics_found_for_search),
                            modifier = Modifier
                                .padding(ComiquetaTheme.dimen.paddingLarge.scaled())
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.bottomBarHeight.scaled() + ComiquetaTheme.dimen.fabDiameter.scaled() / 2))
        }
    }
}

// --- Previews Start ---

/**
 * Helper function to create LazyPagingItems for Composable Previews.
 *
 * @param items The list of items to display in the preview.
 * @return LazyPagingItems<T> ready for preview.
 */
@Composable
fun <T : Any> rememberPreviewLazyPagingItems(
    items: List<T>,
    onItemsLoaded: ((Int) -> Unit)? = null
): LazyPagingItems<T> {
    val pagingData = remember { PagingData.from(items) }
    val flow = remember { flowOf(pagingData) }
    val lazyPagingItems = flow.collectAsLazyPagingItems()

    LaunchedEffect(lazyPagingItems.itemCount) {
        if (lazyPagingItems.itemCount > 0) {
            onItemsLoaded?.invoke(lazyPagingItems.itemCount)
        }
    }

    return lazyPagingItems
}

private val sampleComics = listOf(
    ComicEntity(
        filePath = "file:///comic1".toUri(),
        title = "Comic Adventure 1",
        // author = "Author A", // ComicEntity doesn't have author, removed for consistency
        isFavorite = true,
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+1".toUri()
    ).asExternalModel(), ComicEntity(
        filePath = "file:///comic2".toUri(),
        title = "Mystery of the Void",
        // author = "Author B",
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+2".toUri()
    ).asExternalModel(), ComicEntity(
        filePath = "file:///comic3".toUri(),
        title = "Chronicles of Code",
        // author = "Author C",
        isFavorite = false,
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+3".toUri()
    ).asExternalModel(), ComicEntity(
        filePath = "file:///comic4".toUri(),
        title = "Epic Tales",
        // author = "Author D",
        isFavorite = true,
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+4".toUri()
    ).asExternalModel()
)
private val sampleCategories = listOf(
    Category(id = 1, name = "All", createdAt = 0),
    Category(id = 2, name = "Sci-Fi", createdAt = 0),
    Category(id = 3, name = "Fantasy", createdAt = 0)
)

// BottomAppBar Previews
@PreviewScreenSizes
@Preview(
    name = "Phone - BottomAppBar",
    group = "BottomAppBar - Phone",
    showBackground = true,
    device = "spec:width=1080px,height=2560px,dpi=440",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun BottomAppBarPreviewPhone() {
    ComiquetaThemeContent {
        Box(
            modifier = Modifier
                .height((ComiquetaTheme.dimen.bottomBarHeight * 2).scaled())
                .fillMaxWidth()
        ) {
            HomeBottomAppBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@PreviewScreenSizes
@Preview(
    name = "Foldable - BottomAppBar",
    group = "BottomAppBar - Foldable",
    showBackground = true,
    device = Devices.FOLDABLE,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun BottomAppBarPreviewFoldable() {
    ComiquetaThemeContent {
        Box(
            modifier = Modifier
                .height((ComiquetaTheme.dimen.bottomBarHeight * 2).scaled())
                .fillMaxWidth()
        ) {
            HomeBottomAppBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@PreviewScreenSizes
@Preview(
    name = "Tablet - BottomAppBar",
    group = "BottomAppBar - Tablet",
    showBackground = true,
    device = Devices.TABLET,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun BottomAppBarPreviewTablet() {
    ComiquetaThemeContent {
        Box(
            modifier = Modifier
                .height((ComiquetaTheme.dimen.bottomBarHeight * 2).scaled())
                .fillMaxWidth()
        ) {
            HomeBottomAppBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            )
        }
    }
}


// Previews With Data
@PreviewScreenSizes
@Preview(
    name = "Home - Dark - With Data - Grid",
    group = "Screen - With Data",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HomeScreenContentWithComicsGridPreview() {
    ComiquetaThemeContent {
        HomeScreenContentForPreview(
            comics = sampleComics,
            latestComics = sampleComics.filter { it.isNew },
            favoriteComics = sampleComics.filter { it.isFavorite },
            uiState = HomeUIState(
                isLoading = false,
                categories = ImmutableList(sampleCategories),
                selectedCategory = sampleCategories.first()
            ), onIntent = {})
    }
}

@PreviewScreenSizes
@Preview(
    name = "Home - Dark - With Data - List",
    group = "Screen - With Data",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HomeScreenContentWithComicsListPreview() {
    ComiquetaThemeContent {
        HomeScreenContentForPreview(
            comics = sampleComics,
            latestComics = sampleComics.filter { it.isNew },
            favoriteComics = sampleComics.filter { it.isFavorite },
            uiState = HomeUIState(
                isLoading = false,
                viewMode = ViewMode.LIST,
                categories = ImmutableList(sampleCategories),
                selectedCategory = sampleCategories.first()
            ), onIntent = {})
    }
}

// Previews for Other States (Loading, Empty)
@PreviewScreenSizes
@Preview(
    name = "Home - Dark - Loading",
    group = "Screen - Other States",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HomeScreenContentLoadingPreview() {
    ComiquetaThemeContent {
        HomeScreenContentForPreview(
            comics = emptyList(),
            latestComics = emptyList(),
            favoriteComics = emptyList(),
            uiState = HomeUIState(isLoading = true), onIntent = {})
    }
}

@PreviewScreenSizes
@Preview(
    name = "Home - Dark - Empty",
    group = "Screen - Other States",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HomeScreenContentEmptyPreview() {
    ComiquetaThemeContent {
        HomeScreenContentForPreview(
            comics = emptyList(),
            latestComics = emptyList(),
            favoriteComics = emptyList(),
            uiState = HomeUIState(
                isLoading = false,
                categories = ImmutableList(
                    listOf(
                        Category(
                            id = 1L,
                            name = "All",
                            createdAt = 0
                        )
                    )
                ),
            ), onIntent = {})
    }
}

@PreviewScreenSizes
@Preview(
    name = "Home - Scanning",
    group = "Screen - Scanning States",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HomeScreenContentScanningPreview() {
    ComiquetaThemeContent {
        HomeScreenContentForPreview(
            comics = sampleComics,
            latestComics = sampleComics.filter { it.isNew },
            favoriteComics = sampleComics.filter { it.isFavorite },
            uiState = HomeUIState(
                isLoading = false,
                isScanningFolders = true,
                scanProgress = 45,
                scanTotalFiles = 100,
                scanProcessedFiles = 45,
                currentComicName = "Amazing Spider-Man #300.cbr",
                categories = ImmutableList(sampleCategories),
                selectedCategory = sampleCategories.first()
            ), onIntent = {})
    }
}

@PreviewScreenSizes
@Preview(
    name = "Home - Scan Finished",
    group = "Screen - Scanning States",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HomeScreenContentScanFinishedPreview() {
    ComiquetaThemeContent {
        HomeScreenContentForPreview(
            comics = sampleComics,
            latestComics = sampleComics.filter { it.isNew },
            favoriteComics = sampleComics.filter { it.isFavorite },
            uiState = HomeUIState(
                isLoading = false,
                scanFinished = true,
                scanResultMessage = "Scan completed successfully. Found 15 new comics.",
                categories = ImmutableList(sampleCategories),
                selectedCategory = sampleCategories.first()
            ), onIntent = {})
    }
}
