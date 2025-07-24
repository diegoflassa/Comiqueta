package dev.diegoflassa.comiqueta.viewer.ui.viewer

import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import android.graphics.Color as AndroidColor
import androidx.core.graphics.createBitmap
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    modifier: Modifier = Modifier,
    comicPath: Uri? = null,
    navigationViewModel: NavigationViewModel? = null,
    viewerViewModel: ViewerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(comicPath) {
        if (comicPath != null) {
            viewerViewModel.processIntent(ViewerIntent.LoadComic(comicPath))
        }
    }
    val viewerUIState: ViewerUIState by viewerViewModel.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        viewerViewModel.effect.collect { effect ->
            when (effect) {
                is ViewerEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ViewerScreenContent(
        modifier = modifier,
        navigationViewModel = navigationViewModel,
        uiState = viewerUIState
    ) { intent ->
        viewerViewModel.processIntent(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ViewerScreenContent(
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel? = null,
    uiState: ViewerUIState = ViewerUIState(),
    onIntent: ((ViewerIntent) -> Unit)? = null
) {
    BackHandler {
        navigationViewModel?.goBack()
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) {
        uiState.totalPageCount.coerceAtLeast(0)
    }

    LaunchedEffect(uiState.currentPageNumber, uiState.totalPageCount) {
        if (uiState.totalPageCount > 0) {
            val targetPage = (uiState.currentPageNumber - 1).coerceIn(0, uiState.totalPageCount - 1)
            if (pagerState.currentPage != targetPage) {
                pagerState.scrollToPage(targetPage)
            }
        }
    }
    var scale by remember { mutableFloatStateOf(1f) } // Hoisted scale

    LaunchedEffect(pagerState, uiState.currentPageNumber, uiState.totalPageCount) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            TimberLogger.logD("ViewerScreen", "SnapshotFlow: Pager's currentPage is now: $page. uiState.currentPageNumber (1-indexed) is: ${uiState.currentPageNumber}, totalPageCount: ${uiState.totalPageCount}.")
            if (uiState.totalPageCount > 0 && (page + 1) != uiState.currentPageNumber) {
                TimberLogger.logD("ViewerScreen", "SnapshotFlow: Condition MET. Pager current page (0-indexed): $page, uiState.currentPageNumber (1-indexed): ${uiState.currentPageNumber}. Sending NavigateToPage($page).")
                onIntent?.invoke(ViewerIntent.NavigateToPage(page))
            } else {
                TimberLogger.logD("ViewerScreen", "SnapshotFlow: Condition NOT MET. Pager current page (0-indexed): $page, uiState.currentPageNumber (1-indexed): ${uiState.currentPageNumber}, totalPageCount: ${uiState.totalPageCount}.")
            }
        }
    }


    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AnimatedVisibility(
                visible = uiState.showViewerControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TopAppBar(
                    title = { Text(uiState.comicTitle) },
                    navigationIcon = {
                        IconButton(onClick = { navigationViewModel?.goBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = uiState.showViewerControls && uiState.totalPageCount > 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "${uiState.currentPageNumber} / ${uiState.totalPageCount}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .clickable(
                    enabled = !uiState.isLoadingComic && uiState.viewerError == null, // Disable click if loading or error
                    onClick = { onIntent?.invoke(ViewerIntent.ToggleViewerControls) }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoadingComic && uiState.comicPages.isEmpty() && uiState.viewerError == null -> {
                    CircularProgressIndicator()
                }

                uiState.viewerError != null -> {
                    Text(
                        text = "Error: ${uiState.viewerError}\nTap to dismiss.",
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable { // This clickable is only for dismissing the error
                                onIntent?.invoke(ViewerIntent.ClearError)
                            },
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                uiState.totalPageCount > 0 -> {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !uiState.isLoadingComic && scale == 1f // Updated: only allow scroll if not loading AND scale is 1f
                    ) { pageIndexInPager ->
                        val pageBitmapToDisplay = if (
                            (uiState.currentPageNumber -1) == pageIndexInPager &&
                            uiState.comicPages.isNotEmpty()
                        ) {
                            uiState.comicPages.firstOrNull()
                        } else {
                            null
                        }

                        //var scale by remember { mutableFloatStateOf(1f) } // Moved scale to be hoisted
                        var offsetX by remember { mutableFloatStateOf(0f) }
                        var offsetY by remember { mutableFloatStateOf(0f) }

                        LaunchedEffect(pageBitmapToDisplay, pagerState.currentPage) {
                            // Reset zoom/pan only for the CURRENT active page in pager if bitmap/page changes
                            if (pagerState.currentPage == pageIndexInPager) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val imageModifier = Modifier
                                .fillMaxSize()
                                .pointerInput(pageBitmapToDisplay) {
                                    if (pageBitmapToDisplay != null) {
                                        detectTransformGestures(
                                            panZoomLock = true,
                                            onGesture = { centroid, pan, zoom, _ ->
                                                val oldScale = scale
                                                val newScale = (scale * zoom).coerceIn(1f, 5f)

                                                // Calculate image dimensions based on ContentScale.Fit
                                                val containerWidthPx = constraints.maxWidth
                                                val containerHeightPx = constraints.maxHeight
                                                val imageAspectRatio = pageBitmapToDisplay.width.toFloat() / pageBitmapToDisplay.height.toFloat()
                                                val containerAspectRatio = containerWidthPx.toFloat() / containerHeightPx.toFloat()

                                                val fittedImageWidth: Float
                                                val fittedImageHeight: Float

                                                if (imageAspectRatio > containerAspectRatio) {
                                                    fittedImageWidth = containerWidthPx.toFloat()
                                                    fittedImageHeight = fittedImageWidth / imageAspectRatio
                                                } else { // Image is taller
                                                    fittedImageHeight = containerHeightPx.toFloat()
                                                    fittedImageWidth = fittedImageHeight * imageAspectRatio
                                                }

                                                // Calculate maximum allowed offsets
                                                val maxOffsetX = (fittedImageWidth * newScale - containerWidthPx).coerceAtLeast(0f) / 2f
                                                val maxOffsetY = (fittedImageHeight * newScale - containerHeightPx).coerceAtLeast(0f) / 2f


                                                // Update offset: zoom around the centroid and apply pan
                                                offsetX = (offsetX + centroid.x * (1 - newScale / oldScale) + pan.x).let {
                                                    if (maxOffsetX > 0) it.coerceIn(-maxOffsetX, maxOffsetX) else 0f
                                                }
                                                offsetY = (offsetY + centroid.y * (1 - newScale / oldScale) + pan.y).let {
                                                    if (maxOffsetY > 0) it.coerceIn(-maxOffsetY, maxOffsetY) else 0f
                                                }
                                                scale = newScale
                                            }
                                        )
                                    }
                                }
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )

                            if (pageBitmapToDisplay != null) {
                                Image(
                                    bitmap = pageBitmapToDisplay,
                                    contentDescription = "Page ${pageIndexInPager + 1}",
                                    modifier = imageModifier.background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Fit
                                )
                            } else if ((uiState.currentPageNumber - 1) == pageIndexInPager && uiState.comicPages.isEmpty() && uiState.isLoadingComic) {
                                // Show loader for the current page if its bitmap is loading
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                // Empty placeholder for non-active/non-loaded pages
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                        }
                    }
                }

                else -> { // No comic loaded, not loading, no error
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "No comic loaded.",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Please select a comic to start viewing.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun createDummyBitmapsForPreview(
    count: Int,
    width: Int = 600,
    height: Int = 800
): List<ImageBitmap> {
    val list = mutableListOf<ImageBitmap>()
    for (i in 1..count) {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = if (i % 2 == 0) AndroidColor.LTGRAY else AndroidColor.DKGRAY
            style = Paint.Style.FILL
        }
        canvas.drawPaint(paint)
        paint.color = AndroidColor.WHITE
        paint.textSize = 50f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Page $i (Preview)", canvas.width / 2f, canvas.height / 2f, paint)
        list.add(bitmap.asImageBitmap())
    }
    return list
}

// --- Previews Start ---

// Previews with Data
@Preview(name = "Phone - Light - With Comic", group = "Screen - With Comic", showBackground = true, device = "spec:width=1080px,height=2560px,dpi=440")
@Preview(name = "Phone - Dark - With Comic", group = "Screen - With Comic", showBackground = true, device = "spec:width=1080px,height=2560px,dpi=440", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Foldable - Light - With Comic", group = "Screen - With Comic", showBackground = true, device = Devices.FOLDABLE)
@Preview(name = "Foldable - Dark - With Comic", group = "Screen - With Comic", showBackground = true, device = Devices.FOLDABLE, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet - Light - With Comic", group = "Screen - With Comic", showBackground = true, device = Devices.TABLET)
@Preview(name = "Tablet - Dark - With Comic", group = "Screen - With Comic", showBackground = true, device = Devices.TABLET, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ViewerScreenWithComicPreview() {
    ComiquetaThemeContent {
        ViewerScreenContent(
            uiState = ViewerUIState(
                isLoadingComic = false,
                comicPages = createDummyBitmapsForPreview(1),
                currentPageNumber = 1,
                totalPageCount = 3,
                comicTitle = "My Awesome Comic Book",
                showViewerControls = true,
                viewerError = null
            ),
            onIntent = {}
        )
    }
}

// Previews for Other States (Empty, Loading, Error)
@Preview(name = "Phone - Light Mode - Empty", group = "Screen - Other States", showBackground = true, device = "spec:width=1080px,height=2560px,dpi=440")
@Preview(name = "Phone - Dark Mode - Empty", group = "Screen - Other States", showBackground = true, device = "spec:width=1080px,height=2560px,dpi=440", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ViewerScreenEmptyPreview() {
    ComiquetaThemeContent {
        ViewerScreenContent(
            uiState = ViewerUIState(
                isLoadingComic = false,
                comicPages = emptyList(),
                comicTitle = "",
                viewerError = null
            ),
            onIntent = {}
        )
    }
}

@Preview(name = "Phone - Light Mode - Loading", group = "Screen - Other States", showBackground = true, device = "spec:width=1080px,height=2560px,dpi=440")
@Preview(name = "Phone - Dark Mode - Loading", group = "Screen - Other States", showBackground = true, device = "spec:width=1080px,height=2560px,dpi=440", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ViewerScreenLoadingPreview() {
    ComiquetaThemeContent {
        ViewerScreenContent(
            uiState = ViewerUIState(
                isLoadingComic = true,
                comicPages = emptyList(),
                currentPageNumber = 1,
                totalPageCount = 1,
                comicTitle = "Loading Comic...",
                viewerError = null
            ),
            onIntent = {}
        )
    }
}

@Preview(name = "Phone - Light Mode - Error", group = "Screen - Other States", showBackground = true, device = "spec:width=1080px,height=2560px,dpi=440")
@Preview(name = "Phone - Dark Mode - Error", group = "Screen - Other States", showBackground = true, device = "spec:width=1080px,height=2560px,dpi=440", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ViewerScreenErrorPreview() {
    ComiquetaThemeContent {
        ViewerScreenContent(
            uiState = ViewerUIState(
                isLoadingComic = false,
                comicPages = emptyList(),
                comicTitle = "Problem Child",
                viewerError = "Failed to load pages. File might be corrupted or unsupported."
            ),
            onIntent = {}
        )
    }
}
