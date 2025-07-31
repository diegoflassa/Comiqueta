package dev.diegoflassa.comiqueta.viewer.ui.viewer

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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel
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
    TimberLogger.logI(tag, "ViewerScreen")
    val context = LocalContext.current
    LaunchedEffect(comicPath) {
        if (comicPath != null) {
            viewerViewModel.processIntent(ViewerIntent.LoadComic(comicPath))
        }
    }
    val viewerUIState: ViewerUIState by viewerViewModel.uiState.collectAsStateWithLifecycle()

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
    BackHandler { navigationViewModel?.goBack() }

    val initialPagerPage = (uiState.currentPageNumber - 1).coerceAtLeast(0)
    val totalPagerPageCount = uiState.totalPageCount.coerceAtLeast(0)

    val pagerState = rememberPagerState(
        initialPage = initialPagerPage,
        pageCount = { totalPagerPageCount }
    )

    LaunchedEffect(uiState.currentPageNumber, uiState.totalPageCount) {
        val maxPageIndex = (uiState.totalPageCount - 1).coerceAtLeast(0)
        val targetPage = (uiState.currentPageNumber - 1).coerceIn(0, maxPageIndex)

        if (uiState.totalPageCount > 0 && pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                if (uiState.totalPageCount > 0 && (page + 1) != uiState.currentPageNumber) {
                    onIntent?.invoke(ViewerIntent.NavigateToPage(page))
                }
            }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(uiState.showViewerControls, enter = fadeIn(), exit = fadeOut()) {
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
                uiState.showViewerControls && uiState.totalPageCount > 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = "${uiState.currentPageNumber} / ${uiState.totalPageCount}",
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
                    enabled = !uiState.isLoadingComic && uiState.viewerError == null,
                    onClick = { onIntent?.invoke(ViewerIntent.ToggleViewerControls) }
                ),
            contentAlignment = Alignment.Center
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }

            var isImageZoomed by remember { mutableStateOf(false) }

            // Reset zoom and pan when the pager page changes or if current values are invalid
            LaunchedEffect(pagerState.currentPage) {
                if (scale.isNaN() || scale.isInfinite() || scale != 1f ||
                    offsetX.isNaN() || offsetX.isInfinite() || offsetX != 0f ||
                    offsetY.isNaN() || offsetY.isInfinite() || offsetY != 0f
                ) {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                    isImageZoomed = false
                }
            }

            when {
                uiState.isLoadingComic -> {
                    CircularProgressIndicator()
                }

                uiState.viewerError != null -> {
                    Text(
                        text = uiState.viewerError,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable { onIntent?.invoke(ViewerIntent.ClearError) },
                        textAlign = TextAlign.Center
                    )
                }

                uiState.totalPageCount > 0 -> {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = !isImageZoomed,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndexInPager ->
                        val pageBitmapToDisplay = uiState.comicPages.getOrNull(pageIndexInPager)

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val viewConfiguration = LocalViewConfiguration.current
                            val touchSlop = viewConfiguration.touchSlop

                            val imageModifier = Modifier
                                .fillMaxSize()
                                .pointerInput(pageBitmapToDisplay) {
                                    if (pageBitmapToDisplay != null) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val changes = event.changes

                                                val hasTwoPointers = changes.size >= 2
                                                val firstChange = changes.firstOrNull()

                                                // Calculate raw values
                                                val rawZoom = event.calculateZoom()
                                                val rawPan = event.calculatePan()
                                                val rawCentroid = event.calculateCentroid(useCurrent = true)

                                                // Guard against Unspecified/NaN/Infinity
                                                // Default zoom to 1f if invalid, pan/centroid to Offset(0f,0f)
                                                val zoom = if (rawZoom.isNaN() || rawZoom.isInfinite()) 1f else rawZoom
                                                val pan = if (rawPan == Offset.Unspecified) Offset(0f, 0f) else rawPan
                                                val centroid = if (rawCentroid == Offset.Unspecified) Offset(0f, 0f) else rawCentroid

                                                // If multi-touch (for initial zoom) OR already zoomed in (for pan/zoom)
                                                if (hasTwoPointers || scale > 1f) {
                                                    val oldScale = scale
                                                    val newScale = (scale * zoom).coerceIn(1f, 5f)

                                                    isImageZoomed = newScale > 1f

                                                    if (newScale > 1f) {
                                                        val containerWidthPx = constraints.maxWidth.toFloat()
                                                        val containerHeightPx = constraints.maxHeight.toFloat()

                                                        val imageAspectRatio = pageBitmapToDisplay.width.toFloat() / pageBitmapToDisplay.height.toFloat()
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

                                                        // Ensure maxTranslateX/Y are finite before coercing (existing fix)
                                                        val maxTranslateX = (scaledImageWidth - containerWidthPx) / 2f
                                                        val maxTranslateY = (scaledImageHeight - containerHeightPx) / 2f

                                                        val safeMaxTranslateX = if (maxTranslateX.isFinite()) maxTranslateX else 0f
                                                        val safeMaxTranslateY = if (maxTranslateY.isFinite()) maxTranslateY else 0f

                                                        // Use guarded 'centroid' and 'pan' here
                                                        offsetX = (offsetX + centroid.x * (1 - newScale / oldScale) + pan.x).let {
                                                            if (safeMaxTranslateX > 0f) {
                                                                it.coerceIn(-safeMaxTranslateX, safeMaxTranslateX)
                                                            } else {
                                                                0f
                                                            }
                                                        }
                                                        offsetY = (offsetY + centroid.y * (1 - newScale / oldScale) + pan.y).let {
                                                            if (safeMaxTranslateY > 0f) {
                                                                it.coerceIn(-safeMaxTranslateY, safeMaxTranslateY)
                                                            } else {
                                                                0f
                                                            }
                                                        }
                                                    } else {
                                                        // If newScale becomes 1f, reset offsets to center
                                                        offsetX = 0f
                                                        offsetY = 0f
                                                    }
                                                    scale = newScale

                                                    // Consume all changes because we handled the gesture (pan/zoom)
                                                    changes.forEach { it.consume() }
                                                } else if (firstChange != null && firstChange.positionChanged()) {
                                                    val delta = firstChange.position - firstChange.previousPosition

                                                    if (abs(delta.x) > touchSlop || abs(delta.y) > touchSlop) {
                                                        if (abs(delta.x) > abs(delta.y)) {
                                                            // Dominantly horizontal movement at 1x scale: DO NOT consume.
                                                            // This allows HorizontalPager to handle the swipe.
                                                        } else {
                                                            // Dominantly vertical movement at 1x scale:
                                                            // Can consume if you want to prevent vertical scrolling at 1x
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

                            if (pageBitmapToDisplay != null) {
                                Image(
                                    bitmap = pageBitmapToDisplay,
                                    contentDescription = "Page ${pageIndexInPager + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = imageModifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            } else {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .align(Alignment.Center)
                                ) {
                                    if (uiState.isLoadingComic) {
                                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                                    } else {
                                        Text(
                                            "Page ${pageIndexInPager + 1} not loaded.",
                                            Modifier.align(Alignment.Center),
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
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
                            "Please select a comic.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
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

@Preview(name = "ViewerScreen Empty", locale = "en", showBackground = true, showSystemUi = true)
@Composable
fun ViewerScreenPreviewEmpty() {
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

@Preview(name = "ViewerScreen Loading", locale = "en", showBackground = true, showSystemUi = true)
@Composable
fun ViewerScreenPreviewLoading() {
    ComiquetaThemeContent {
        ViewerScreenContent(
            uiState = ViewerUIState(
                isLoadingComic = true,
                comicPages = emptyList(),
                currentPageNumber = 1,
                totalPageCount = 5,
                comicTitle = "Loading Comic...",
                viewerError = null
            ),
            onIntent = {}
        )
    }
}

@Preview(
    name = "ViewerScreen With Comic",
    locale = "en",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun ViewerScreenPreviewWithComic() {
    ComiquetaThemeContent {
        ViewerScreenContent(
            uiState = ViewerUIState(
                isLoadingComic = false,
                comicPages = createDummyBitmapsForPreview(5),
                currentPageNumber = 1,
                totalPageCount = 5,
                comicTitle = "Sample Comic",
                viewerError = null
            ),
            onIntent = {}
        )
    }
}
