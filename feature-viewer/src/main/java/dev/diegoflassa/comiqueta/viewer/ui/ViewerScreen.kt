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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel
import dev.diegoflassa.comiqueta.viewer.R
// import dev.diegoflassa.comiqueta.viewer.ui.page.PageCurl // Removed for now
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

    var isPagerScrollLocked by remember { mutableStateOf(false) }

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
                } catch (ex: Exception) {
                    TimberLogger.logE(tag, "Error scrolling to page: $targetPage", ex)
                }
            }
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        val settledPage = pagerState.settledPage
        // Check pageCount > 0 to avoid issues during initial load or if comic has no pages.
        if (uiState.pageCount > 0 && settledPage != uiState.currentPage) {
            TimberLogger.logI(tag, "Pager settled on $settledPage, informing ViewModel.")
            onIntent?.invoke(ViewerIntent.GoToPage(settledPage))
        }
        isPagerScrollLocked = false
        TimberLogger.logD(tag, "Pager settled on $settledPage. isPagerScrollLocked set to false.")
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
                    enabled = !isPagerScrollLocked &&
                            // Disable click to toggle UI only if it's the very initial load (no pages yet)
                            // and something is loading. Otherwise, allow UI toggle.
                            (uiState.pageCount > 0 || uiState.isLoadingPage.isEmpty()) &&
                            uiState.error == null,
                    onClick = { onIntent?.invoke(ViewerIntent.ToggleUiVisibility) }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                // Initial loading: No pages counted yet, but some page (likely page 0) is loading.
                uiState.pageCount == 0 && uiState.isLoadingPage.isNotEmpty() -> {
                    TimberLogger.logI(tag, "Displaying global loading indicator (initial comic load or page count not yet set), isLoadingPage: ${uiState.isLoadingPage}")
                    CircularProgressIndicator()
                }

                // Error state: Show error message.
                uiState.error != null -> {
                    TimberLogger.logI(tag, "Displaying error message: ${uiState.error}")
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable { onIntent?.invoke(ViewerIntent.ErrorShown) }, // Allow dismissing error
                        textAlign = TextAlign.Center
                    )
                }

                // Pages available: Show the HorizontalPager.
                uiState.pageCount > 0 -> {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = !isPagerScrollLocked,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = uiState.pagesToPreloadLogic.coerceAtLeast(1),
                        key = { pageIndex -> pageIndex } // Key for pager items stability
                    ) { pageIndexInPager ->

                        // val density = LocalDensity.current.density // For page curl, not used now
                        
                        var itemScale by remember(pageIndexInPager) { mutableFloatStateOf(1f) }
                        var itemOffsetX by remember(pageIndexInPager) { mutableFloatStateOf(0f) }
                        var itemOffsetY by remember(pageIndexInPager) { mutableFloatStateOf(0f) }

                        BoxWithConstraints(
                            modifier = Modifier.fillMaxSize()
                            // GraphicsLayer for page turning effect can be re-added here if needed
                        ) {
                            val currentBitmap: ImageBitmap? = uiState.loadedPages[pageIndexInPager]
                            val isThisPageActuallyLoading: Boolean = uiState.isLoadingPage.contains(pageIndexInPager)

                            TimberLogger.logD(tag, "Page $pageIndexInPager evaluating: " +
                                    "isCurrent=${pageIndexInPager == uiState.currentPage}, " +
                                    "bitmapForThisPageIsNull=${currentBitmap == null}, " +
                                    "isThisPageActuallyLoading=$isThisPageActuallyLoading, " +
                                    "completeIsLoadingPageSet=${uiState.isLoadingPage}")
                            
                            val viewConfiguration = LocalViewConfiguration.current
                            val touchSlop = viewConfiguration.touchSlop

                            val imageDisplayModifier = Modifier
                                .fillMaxSize()
                                .pointerInput(pageIndexInPager, currentBitmap) { 
                                    if (currentBitmap != null) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val changes = event.changes
                                                val hasTwoPointers = changes.size >= 2

                                                val rawZoom = event.calculateZoom()
                                                val rawPan = event.calculatePan()
                                                val rawCentroid = event.calculateCentroid(useCurrent = true)

                                                val zoom = if (rawZoom.isNaN() || rawZoom.isInfinite()) 1f else rawZoom
                                                val pan = if (rawPan == Offset.Unspecified) Offset.Zero else rawPan
                                                val centroid = if (rawCentroid == Offset.Unspecified) Offset.Zero else rawCentroid

                                                if (hasTwoPointers || itemScale > 1f) {
                                                    val oldScale = itemScale
                                                    val newScale = (itemScale * zoom).coerceIn(1f, 5f)

                                                    if (pageIndexInPager == pagerState.currentPage) {
                                                        isPagerScrollLocked = newScale > 1f
                                                    }

                                                    if (newScale > 1f) {
                                                        val containerWidthPx = constraints.maxWidth.toFloat()
                                                        val containerHeightPx = constraints.maxHeight.toFloat()
                                                        val imageAspectRatio = currentBitmap.width.toFloat() / currentBitmap.height.toFloat()
                                                        val containerAspectRatio = containerWidthPx / containerHeightPx

                                                        val fittedImageWidth: Float
                                                        val fittedImageHeight: Float

                                                        if (imageAspectRatio > containerAspectRatio) {
                                                            fittedImageWidth = containerWidthPx
                                                            fittedImageHeight = fittedImageWidth / imageAspectRatio
                                                        } else {
                                                            fittedImageHeight = containerHeightPx
                                                            fittedImageWidth = fittedImageHeight * imageAspectRatio
                                                        }

                                                        val scaledImageWidth = fittedImageWidth * newScale
                                                        val scaledImageHeight = fittedImageHeight * newScale

                                                        val maxTranslateX = (scaledImageWidth - containerWidthPx).coerceAtLeast(0f) / 2f
                                                        val maxTranslateY = (scaledImageHeight - containerHeightPx).coerceAtLeast(0f) / 2f

                                                        itemOffsetX = (itemOffsetX + centroid.x * (1 - newScale / oldScale) + pan.x).coerceIn(-maxTranslateX, maxTranslateX)
                                                        itemOffsetY = (itemOffsetY + centroid.y * (1 - newScale / oldScale) + pan.y).coerceIn(-maxTranslateY, maxTranslateY)
                                                    } else {
                                                        itemOffsetX = 0f
                                                        itemOffsetY = 0f
                                                        if (pageIndexInPager == pagerState.currentPage) {
                                                           isPagerScrollLocked = false
                                                        }
                                                    }
                                                    itemScale = newScale
                                                    changes.forEach { it.consume() }
                                                } else if (changes.isNotEmpty()) {
                                                    val firstChange = changes.first()
                                                    if (firstChange.pressed && firstChange.previousPressed && firstChange.positionChanged()) {
                                                        val delta = firstChange.position - firstChange.previousPosition
                                                        if (abs(delta.x) <= touchSlop && abs(delta.y) > touchSlop) {
                                                             TimberLogger.logD(tag, "Vertical gesture on unzoomed page $pageIndexInPager. Consuming.")
                                                            firstChange.consume()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                .graphicsLayer { 
                                    scaleX = itemScale
                                    scaleY = itemScale
                                    translationX = itemOffsetX
                                    translationY = itemOffsetY
                                }

                            if (currentBitmap != null) {
                                Image(
                                    bitmap = currentBitmap,
                                    contentDescription = stringResource(R.string.comic_page_description, pageIndexInPager + 1),
                                    contentScale = ContentScale.Fit,
                                    modifier = imageDisplayModifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            } else {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isThisPageActuallyLoading) {
                                        TimberLogger.logD(tag, "Page $pageIndexInPager: Displaying loading indicator. (isThisPageActuallyLoading=$isThisPageActuallyLoading from uiState.isLoadingPage)")
                                        CircularProgressIndicator()
                                    } else {
                                        TimberLogger.logD(tag, "Page $pageIndexInPager: No bitmap, not loading. Displaying placeholder. (isThisPageActuallyLoading=$isThisPageActuallyLoading from uiState.isLoadingPage)")
                                        Text(
                                            "Page ${pageIndexInPager + 1}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // No comic loaded or comic has zero pages, and not currently loading/error.
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            stringResource(R.string.no_comic_loaded_title),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            stringResource(R.string.no_comic_loaded_subtitle),
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
                comicTitle = "",
                loadedPages = emptyMap(),
                currentPage = 0,
                pageCount = 0,
                isLoadingPage = emptySet(),
                error = null
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
                comicTitle = "Loading Comic...",
                loadedPages = emptyMap(),
                currentPage = 0,
                pageCount = 0,
                isLoadingPage = setOf(0), // Show loading for the first page
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
                comicTitle = "Sample Comic Title",
                loadedPages = emptyMap(), // No pages loaded yet
                currentPage = 2, // Imagine we are on page 2
                pageCount = 5,
                isLoadingPage = setOf(2), // Page 2 itself is loading
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
                comicTitle = "Sample Comic Title",
                loadedPages = mapOf(
                    0 to createDummyBitmapForPreview(pageNumber = 1),
                    1 to createDummyBitmapForPreview(pageNumber = 2) 
                ),
                currentPage = 0,
                pageCount = 5,
                isLoadingPage = emptySet(), // Page 2 might be loading if preload > 0
                error = null,
                pagesToPreloadLogic = 1
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
                comicTitle = "Error Comic",
                loadedPages = emptyMap(),
                currentPage = 0,
                pageCount = 0, 
                isLoadingPage = emptySet(),
                error = "Failed to load this amazing comic book. Please try again!"
            ),
            onIntent = {}
        )
    }
}
