package dev.diegoflassa.comiqueta.ui.home

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.compose.LazyPagingItems
import dev.diegoflassa.comiqueta.ads.BannerAdView
import dev.diegoflassa.comiqueta.core.data.config.IConfig
import dev.diegoflassa.comiqueta.core.domain.model.Comic
import dev.diegoflassa.comiqueta.core.navigation.Screen
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.theme.settingIconTint
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.home.R
import dev.diegoflassa.comiqueta.ui.widgets.ComicsContent
import dev.diegoflassa.comiqueta.ui.widgets.EmptyStateContent
import dev.diegoflassa.comiqueta.ui.widgets.HomeBottomAppBar

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
                    Surface(
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

        val showAds by remember { mutableStateOf(true) }
        if (showAds && config != null) {
            BannerAdView(
                adUnitId = config.addBannerId
            )
        }
    }
}
