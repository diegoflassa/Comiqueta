package dev.diegoflassa.comiqueta.viewer.ui

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
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key // Added for HorizontalPager key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
// import androidx.compose.ui.res.stringResource // If you add R.string.comic_page_description
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel
// import dev.diegoflassa.comiqueta.viewer.R // If you add string resources like comic_page_description
import android.graphics.Color as AndroidColor
import kotlin.math.abs

private const val tag = "ViewerScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    modifier: Modifier = Modifier,
    comicPath: Uri? = null,
    navigationViewModel: NavigationViewModel? = hiltActivityViewModel(),
    viewerViewModel: ViewerViewModel = hiltViewModel()
) {
    TimberLogger.logI(tag, "ViewerScreen composable")
    val context = LocalContext.current

    val viewerUIState: ViewerUIState by viewerViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(comicPath) {
        if (comicPath != null) {
            TimberLogger.logI(tag, "New comicPath received: $comicPath")
            viewerViewModel.reduce(ViewerIntent.LoadComic(comicPath.toString()))
        } else {
            TimberLogger.logI(tag, "comicPath is null")
        }
    }

    LaunchedEffect(key1 = Unit) {
        viewerViewModel.effect.collect { effect ->
            when (effect) {
                is ViewerEffect.ShowError -> {
                    TimberLogger.logI(tag, "Showing error toast: ${effect.message}")
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    viewerViewModel.reduce(ViewerIntent.ErrorShown)
                }
            }
        }
    }

    ViewerScreenContent(
        modifier = modifier,
        navigationViewModel = navigationViewModel,
        uiState = viewerUIState,
        onIntent = { intent -> viewerViewModel.reduce(intent) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ViewerScreenContent(
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel? = null,
    uiState: ViewerUIState = ViewerUIState(),
    onIntent: ((ViewerIntent) -> Unit)? = null
) {
    BackHandler { navigationViewModel?.goBack() }

    val pagerState = rememberPagerState(
        initialPage = uiState.currentPage,
        pageCount = { uiState.pageCount.coerceAtLeast(0) }
    )

    LaunchedEffect(uiState.currentPage, uiState.pageCount) {
        if (uiState.pageCount > 0) {
            val targetPage =
                uiState.currentPage.coerceIn(0, (uiState.pageCount - 1).coerceAtLeast(0))
            if (pagerState.currentPage != targetPage) {
                TimberLogger.logI(
                    tag,
                    "Scrolling pager to target page: $targetPage (ViewModel state: ${uiState.currentPage})"
                )
                try {
                    pagerState.scrollToPage(targetPage)
                } catch (e: Exception) {
                    TimberLogger.logE(tag, "Error scrolling to page: $targetPage", e)
                }
            }
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        val settledPage = pagerState.settledPage
        // Ensure that we only send GoToPage if the settled page is different from the ViewModel's current page
        // and also ensure pageCount is positive to avoid issues with empty comics.
        if (uiState.pageCount > 0 && settledPage != uiState.currentPage) {
            TimberLogger.logI(tag, "Pager settled on $settledPage, informing ViewModel.")
            onIntent?.invoke(ViewerIntent.GoToPage(settledPage))
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(uiState.isUiVisible, enter = fadeIn(), exit = fadeOut()) {
                TopAppBar(
                    title = { Text(uiState.comicTitle) },
                    navigationIcon = {
                        IconButton(onClick = { navigationViewModel?.goBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = uiState.isUiVisible && uiState.pageCount > 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = "${uiState.currentPage + 1} / ${uiState.pageCount}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .clickable(
                    // Use isLoadingFocused and check if pageCount is > 0 before enabling toggle
                    enabled = !uiState.isLoadingFocused && uiState.error == null && uiState.pageCount > 0,
                    onClick = { onIntent?.invoke(ViewerIntent.ToggleUiVisibility) }
                ),
            contentAlignment = Alignment.Center
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }
            var isImageZoomed by remember { mutableStateOf(false) }

            // Reset zoom/pan when the focused bitmap for the settled page changes,
            // or when the settled page itself changes.
            LaunchedEffect(
                pagerState.settledPage,
                uiState.focusedBitmap
            ) {
                TimberLogger.logI(
                    tag,
                    "Resetting zoom/pan for pager settledPage: ${pagerState.settledPage}"
                )
                scale = 1f
                offsetX = 0f
                offsetY = 0f
                isImageZoomed = false
            }

            when {
                // Global loading: when comic is first loading and no focused bitmap is available yet.
                uiState.isLoadingFocused && uiState.focusedBitmap == null && uiState.pageCount == 0 -> {
                    TimberLogger.logI(
                        tag,
                        "Displaying global loading indicator (initial comic load)"
                    )
                    CircularProgressIndicator()
                }

                uiState.error != null -> {
                    TimberLogger.logI(tag, "Displaying error message: ${uiState.error}")
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable { onIntent?.invoke(ViewerIntent.ErrorShown) },
                        textAlign = TextAlign.Center
                    )
                }

                uiState.pageCount > 0 -> {
                    TimberLogger.logI(
                        tag,
                        "Displaying HorizontalPager. VMPage: ${uiState.currentPage}, PagerCurrent: ${pagerState.currentPage}, PagerSettled: ${pagerState.settledPage}"
                    )
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = !isImageZoomed,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                        key = { pageIndex -> pageIndex } // Added key
                    ) { pageIndexInPager ->

                        val bitmapToDisplay: ImageBitmap? = key(
                            uiState.currentPage,
                            uiState.focusedBitmap,
                            uiState.neighborBitmaps
                        ) {
                            // This key helps recompose this specific logic when relevant parts of uiState change for this pageIndexInPager
                            if (pageIndexInPager == uiState.currentPage) {
                                uiState.focusedBitmap
                            } else {
                                uiState.neighborBitmaps[pageIndexInPager]
                            }
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val viewConfiguration = LocalViewConfiguration.current
                            val touchSlop = viewConfiguration.touchSlop

                            // This imageModifier is now more general, applied if bitmapToDisplay is not null
                            val imageDisplayModifier = Modifier
                                .fillMaxSize()
                                .pointerInput(
                                    bitmapToDisplay, // Key on bitmapToDisplay
                                    pagerState.currentPage // And current page from pager
                                ) {
                                    if (bitmapToDisplay != null) { // Only enable gestures if bitmap is present
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val changes = event.changes
                                                val hasTwoPointers = changes.size >= 2

                                                val rawZoom = event.calculateZoom()
                                                val rawPan = event.calculatePan()
                                                val rawCentroid =
                                                    event.calculateCentroid(useCurrent = true)

                                                val zoom =
                                                    if (rawZoom.isNaN() || rawZoom.isInfinite()) 1f else rawZoom
                                                val pan =
                                                    if (rawPan == Offset.Unspecified) Offset.Zero else rawPan
                                                val centroid =
                                                    if (rawCentroid == Offset.Unspecified) Offset.Zero else rawCentroid

                                                if (hasTwoPointers || scale > 1f) {
                                                    val oldScale = scale
                                                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                                                    isImageZoomed = newScale > 1f

                                                    if (newScale > 1f) {
                                                        val containerWidthPx =
                                                            constraints.maxWidth.toFloat()
                                                        val containerHeightPx =
                                                            constraints.maxHeight.toFloat()
                                                        val imageAspectRatio =
                                                            bitmapToDisplay.width.toFloat() / bitmapToDisplay.height.toFloat()
                                                        val containerAspectRatio =
                                                            containerWidthPx / containerHeightPx

                                                        val fittedImageWidth: Float
                                                        val fittedImageHeight: Float

                                                        if (imageAspectRatio > containerAspectRatio) {
                                                            fittedImageWidth = containerWidthPx
                                                            fittedImageHeight =
                                                                fittedImageWidth / imageAspectRatio
                                                        } else {
                                                            fittedImageHeight = containerHeightPx
                                                            fittedImageWidth =
                                                                fittedImageHeight * imageAspectRatio
                                                        }

                                                        val scaledImageWidth =
                                                            fittedImageWidth * newScale
                                                        val scaledImageHeight =
                                                            fittedImageHeight * newScale

                                                        val maxTranslateX =
                                                            (scaledImageWidth - containerWidthPx).coerceAtLeast(
                                                                0f
                                                            ) / 2f
                                                        val maxTranslateY =
                                                            (scaledImageHeight - containerHeightPx).coerceAtLeast(
                                                                0f
                                                            ) / 2f

                                                        offsetX =
                                                            (offsetX + centroid.x * (1 - newScale / oldScale) + pan.x).coerceIn(
                                                                -maxTranslateX,
                                                                maxTranslateX
                                                            )
                                                        offsetY =
                                                            (offsetY + centroid.y * (1 - newScale / oldScale) + pan.y).coerceIn(
                                                                -maxTranslateY,
                                                                maxTranslateY
                                                            )
                                                    } else {
                                                        offsetX = 0f
                                                        offsetY = 0f
                                                    }
                                                    scale = newScale
                                                    changes.forEach { it.consume() }
                                                } else if (changes.isNotEmpty()) {
                                                    val firstChange = changes.first()
                                                    if (firstChange.pressed && firstChange.previousPressed && firstChange.positionChanged()) {
                                                        val delta =
                                                            firstChange.position - firstChange.previousPosition
                                                        if (abs(delta.x) > touchSlop && abs(delta.x) > abs(
                                                                delta.y
                                                            )
                                                        ) {
                                                            // Horizontal swipe detected, do nothing to allow pager to scroll
                                                            TimberLogger.logD(
                                                                tag,
                                                                "Horizontal gesture detected, not consuming for zoom/pan."
                                                            )
                                                        } else if (abs(delta.y) > touchSlop) {
                                                            // Vertical swipe detected, consume if not zooming (though this path might be less common with current logic)
                                                            firstChange.consume()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )

                            if (bitmapToDisplay != null) {
                                Image(
                                    bitmap = bitmapToDisplay, // It's already an ImageBitmap
                                    contentDescription = "Page ${pageIndexInPager + 1}", // Consider stringResource(R.string.comic_page_description, pageIndexInPager + 1)
                                    contentScale = ContentScale.Fit,
                                    modifier = imageDisplayModifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            } else {
                                // Per-page loading indicator or placeholder
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.3f
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val isLoadingThisPage =
                                        (pageIndexInPager == uiState.currentPage && uiState.isLoadingFocused) ||
                                                (abs(uiState.currentPage - pageIndexInPager) == 1 && uiState.neighborBitmaps[pageIndexInPager] == null && uiState.pageCount > 0)

                                    if (isLoadingThisPage) {
                                        TimberLogger.logD(
                                            tag,
                                            "Showing loading indicator for page $pageIndexInPager (focused: ${uiState.currentPage}, isLoadingFocused: ${uiState.isLoadingFocused})"
                                        )
                                        CircularProgressIndicator()
                                    } else {
                                        // Optionally, show page number if it's the current page and somehow bitmap is null but not loading (e.g. error state for specific page)
                                        // For now, keep it blank if not loading and no bitmap for non-focused pages
                                        if (pageIndexInPager == uiState.currentPage) {
                                            Text(
                                                "Page ${pageIndexInPager + 1}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.7f
                                                )
                                            )
                                        }
                                        TimberLogger.logD(
                                            tag,
                                            "No bitmap and not actively loading page $pageIndexInPager"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> { // No comic loaded or pageCount is 0 after initial load attempt
                    TimberLogger.logI(tag, "Displaying 'No comic loaded or empty comic' message")
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
                            "Please select a comic from the library.",
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

/**
 * Creates a single dummy ImageBitmap for preview purposes.
 */
private fun createDummyBitmapForPreview(
    pageNumber: Int = 1,
    width: Int = 600,
    height: Int = 800
): ImageBitmap {
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        color = if (pageNumber % 2 == 0) AndroidColor.LTGRAY else AndroidColor.DKGRAY
        style = Paint.Style.FILL
    }
    canvas.drawPaint(paint)
    paint.color = AndroidColor.WHITE
    paint.textSize = 50f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("Page $pageNumber (Preview)", canvas.width / 2f, canvas.height / 2f, paint)
    return bitmap.asImageBitmap()
}

@PreviewScreenSizes
@Composable
private fun ViewerScreenPreviewEmpty() {
    ComiquetaThemeContent {
        ViewerScreenContent(
            uiState = ViewerUIState(
                isLoadingFocused = false,
                comicTitle = "",
                error = null,
                pageCount = 0,
                currentPage = 0,
                focusedBitmap = null,
                neighborBitmaps = emptyMap()
            ),
            onIntent = {}
        )
    }
}

@PreviewScreenSizes
@Composable
private fun ViewerScreenPreviewLoadingInitial() {
    ComiquetaThemeContent {
        ViewerScreenContent(
            uiState = ViewerUIState(
                isLoadingFocused = true,
                comicTitle = "Loading Comic...",
                pageCount = 0,
                currentPage = 0,
                focusedBitmap = null,
                neighborBitmaps = emptyMap(),
                error = null
            ),
            onIntent = {}
        )
    }
}

@PreviewScreenSizes
@Composable
private fun ViewerScreenPreviewLoadingPage() {
    ComiquetaThemeContent {
        ViewerScreenContent(
            uiState = ViewerUIState(
                isLoadingFocused = true,
                comicTitle = "Sample Comic Title",
                pageCount = 5,
                currentPage = 0,
                focusedBitmap = null,
                neighborBitmaps = emptyMap(),
                error = null
            ),
            onIntent = {}
        )
    }
}


@PreviewScreenSizes
@Composable
private fun ViewerScreenPreviewWithComic() {
    ComiquetaThemeContent {
        ViewerScreenContent(
            uiState = ViewerUIState(
                isLoadingFocused = false,
                focusedBitmap = createDummyBitmapForPreview(pageNumber = 1),
                neighborBitmaps = mapOf(
                    1 to createDummyBitmapForPreview(pageNumber = 2)
                ),
                currentPage = 0,
                pageCount = 5,
                comicTitle = "Sample Comic Title",
                error = null
            ),
            onIntent = {}
        )
    }
}

@PreviewScreenSizes
@Composable
private fun ViewerScreenPreviewWithError() {
    ComiquetaThemeContent {
        ViewerScreenContent(
            uiState = ViewerUIState(
                isLoadingFocused = false,
                error = "Failed to load this amazing comic book. Please try again!",
                comicTitle = "Error Comic",
                pageCount = 0,
                currentPage = 0,
                focusedBitmap = null,
                neighborBitmaps = emptyMap()
            ),
            onIntent = {}
        )
    }
}
