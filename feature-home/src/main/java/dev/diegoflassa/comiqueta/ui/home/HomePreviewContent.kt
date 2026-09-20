package dev.diegoflassa.comiqueta.ui.home

import android.content.res.Configuration
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import dev.diegoflassa.comiqueta.ads.BannerAdView
import dev.diegoflassa.comiqueta.core.R as CoreR
import dev.diegoflassa.comiqueta.core.data.config.IConfig
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.mappers.asExternalModel
import dev.diegoflassa.comiqueta.core.domain.model.Category
import dev.diegoflassa.comiqueta.core.domain.model.Comic
import dev.diegoflassa.comiqueta.core.navigation.Screen
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.theme.settingIconTint
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.home.R
import dev.diegoflassa.comiqueta.ui.enums.ViewMode
import dev.diegoflassa.comiqueta.ui.widgets.ComicsContentForPreview
import dev.diegoflassa.comiqueta.ui.widgets.EmptyStateContent
import dev.diegoflassa.comiqueta.ui.widgets.HomeBottomAppBar
import kotlinx.coroutines.flow.flowOf

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
    val isEmpty =
        (comics.isEmpty()) && uiState.searchQuery.isBlank() && uiState.selectedCategory == null && uiState.isLoading.not()

    val topSystemBarInsetDp = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    val showAds by remember { mutableStateOf(true) }
    Column(modifier = modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier
            .weight(1f)
            .background(ComiquetaTheme.colorScheme.background),
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
                                painter = painterResource(id = CoreR.drawable.ic_settings),
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
                onIntent = onIntent
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
                        EmptyStateContent(
                            addFolderInFlight = uiState.isAddFolderInFlight,
                            onIntent = onIntent
                        )
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
            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.isScanningFolders || uiState.scanFinished,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
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
                                TextButton(
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
                            LinearProgressIndicator(
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
        if (showAds && config != null) {
            BannerAdView(
                adUnitId = config.addBannerId
            )
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


// Previews With Data
@Preview(name = "HomeScreen · With Data - Grid · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "HomeScreen · With Data - Grid · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
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

@Preview(name = "HomeScreen · With Data - Grid · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenContentWithComicsGridDarkPreview() {
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

@Preview(name = "HomeScreen · With Data - List · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "HomeScreen · With Data - List · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
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

@Preview(name = "HomeScreen · With Data - List · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenContentWithComicsListDarkPreview() {
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
@Preview(name = "HomeScreen · Loading · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "HomeScreen · Loading · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
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

@Preview(name = "HomeScreen · Loading · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenContentLoadingDarkPreview() {
    ComiquetaThemeContent {
        HomeScreenContentForPreview(
            comics = emptyList(),
            latestComics = emptyList(),
            favoriteComics = emptyList(),
            uiState = HomeUIState(isLoading = true), onIntent = {})
    }
}

@Preview(name = "HomeScreen · Empty · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "HomeScreen · Empty · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
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

@Preview(name = "HomeScreen · Empty · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenContentEmptyDarkPreview() {
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

@Preview(name = "HomeScreen · Scanning · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "HomeScreen · Scanning · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
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

@Preview(name = "HomeScreen · Scanning · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenContentScanningDarkPreview() {
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

@Preview(name = "HomeScreen · Scan Finished · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "HomeScreen · Scan Finished · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
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

@Preview(name = "HomeScreen · Scan Finished · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenContentScanFinishedDarkPreview() {
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
