package dev.diegoflassa.comiqueta.viewer.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel
import dev.diegoflassa.comiqueta.viewer.R
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.PageFlip
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.rememberPageCurlState
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.config.rememberPageCurlConfig
import kotlinx.coroutines.CancellationException
import android.graphics.Color as AndroidColor

private const val tag = "ViewerScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    modifier: Modifier = Modifier,
    comicPath: Uri? = null,
    navigationViewModel: NavigationViewModel? = hiltActivityViewModel(),
    viewerViewModel: ViewerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val viewerUIState: ViewerUIState by viewerViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(comicPath) {
        if (comicPath != null) {
            viewerViewModel.reduce(ViewerIntent.LoadComic(comicPath.toString()))
        }
    }

    LaunchedEffect(key1 = Unit) {
        viewerViewModel.effect.collect { effect ->
            when (effect) {
                is ViewerEffect.ShowError -> {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreenContent(
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel? = null,
    uiState: ViewerUIState = ViewerUIState(),
    onIntent: ((ViewerIntent) -> Unit)? = null
) {
    BackHandler { navigationViewModel?.goBack() }

    var globalIsPinchZoomActive by retain { mutableStateOf(false) }
    val pageCurlState = rememberPageCurlState(initialCurrent = uiState.currentPage)

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
                    enabled = !globalIsPinchZoomActive &&
                            (uiState.pageCount > 0 || uiState.isLoadingPage.isEmpty()) &&
                            uiState.error == null,
                    onClick = { onIntent?.invoke(ViewerIntent.ToggleUiVisibility) }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.pageCount == 0 && uiState.isLoadingPage.isNotEmpty() -> {
                    CircularProgressIndicator()
                }

                uiState.error != null -> {
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
                    LaunchedEffect(uiState.currentPage) {
                        if (pageCurlState.current != uiState.currentPage && uiState.currentPage < uiState.pageCount) {
                            pageCurlState.current = uiState.currentPage
                        }
                    }

                    LaunchedEffect(pageCurlState.current) {
                        val curlPageCurrent = pageCurlState.current
                        if (curlPageCurrent != uiState.currentPage) {
                            onIntent?.invoke(ViewerIntent.GoToPage(curlPageCurrent))
                        }
                    }

                    val pageCurlConfig = rememberPageCurlConfig(
                        dragForwardEnabled = !globalIsPinchZoomActive,
                        dragBackwardEnabled = !globalIsPinchZoomActive,
                        tapForwardEnabled = !globalIsPinchZoomActive,
                        tapBackwardEnabled = !globalIsPinchZoomActive
                    )

                    PageFlip(
                        modifier = Modifier.fillMaxSize(),
                        count = uiState.pageCount,
                        key = { uiState.comicPath.hashCode() },
                        state = pageCurlState,
                        config = pageCurlConfig,
                    ) { pageIndexInCurl ->
                        TimberLogger.logI(
                            tag,
                            "PZ_DEBUG: PageCurl Item recomposing for page $pageIndexInCurl, CUR=${pageCurlState.current}"
                        )
                        var itemScale by retain(pageIndexInCurl) { mutableFloatStateOf(1f) }
                        var itemOffsetX by retain(pageIndexInCurl) { mutableFloatStateOf(0f) }
                        var itemOffsetY by retain(pageIndexInCurl) { mutableFloatStateOf(0f) }

                        LaunchedEffect(pageCurlState.current, pageIndexInCurl, itemScale) {
                            if (pageIndexInCurl == pageCurlState.current) {
                                val newGlobalZoomState = itemScale > 1f
                                if (globalIsPinchZoomActive != newGlobalZoomState) {
                                    globalIsPinchZoomActive = newGlobalZoomState
                                }
                            } else {
                                if (itemScale > 1f) {
                                    itemScale = 1f
                                    itemOffsetX = 0f
                                    itemOffsetY = 0f
                                }
                            }
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val currentBitmap: ImageBitmap? = uiState.loadedPages[pageIndexInCurl]
                            val isThisPageActuallyLoading: Boolean =
                                uiState.isLoadingPage.contains(pageIndexInCurl)

                            val imageDisplayModifier = retain(
                                pageIndexInCurl,
                                currentBitmap != null,
                                pageCurlState.current
                            ) {
                                Modifier
                                    .fillMaxSize()
                                    .pointerInput(
                                        pageIndexInCurl,
                                        (currentBitmap != null),
                                        pageCurlState.current
                                    ) {
                                        if (pageIndexInCurl == pageCurlState.current) {
                                            if (currentBitmap != null) {
                                                awaitPointerEventScope {
                                                    try {
                                                        while (true) {
                                                            val event =
                                                                awaitPointerEvent(PointerEventPass.Initial)

                                                            val changes = event.changes
                                                            if (changes.isEmpty()) {
                                                                continue
                                                            }

                                                            val oldLocalItemScale = itemScale
                                                            if (changes.size >= 2 || oldLocalItemScale > 1f) {
                                                                val zoomFactor =
                                                                    if (changes.size >= 2) event.calculateZoom() else 1f
                                                                val panDelta = event.calculatePan()
                                                                val newLocalItemScale =
                                                                    (oldLocalItemScale * zoomFactor).coerceIn(
                                                                        1f,
                                                                        5f
                                                                    )

                                                                itemScale = newLocalItemScale

                                                                if (itemScale > 1f) {
                                                                    val containerWidthPx =
                                                                        constraints.maxWidth.toFloat()
                                                                    val containerHeightPx =
                                                                        constraints.maxHeight.toFloat()
                                                                    val imageAspectRatio =
                                                                        currentBitmap.width.toFloat() / currentBitmap.height.toFloat()
                                                                    val containerAspectRatio =
                                                                        containerWidthPx / containerHeightPx
                                                                    val fittedImageWidth: Float
                                                                    val fittedImageHeight: Float
                                                                    if (imageAspectRatio > containerAspectRatio) {
                                                                        fittedImageWidth =
                                                                            containerWidthPx
                                                                        fittedImageHeight =
                                                                            fittedImageWidth / imageAspectRatio
                                                                    } else {
                                                                        fittedImageHeight =
                                                                            containerHeightPx
                                                                        fittedImageWidth =
                                                                            fittedImageHeight * imageAspectRatio
                                                                    }
                                                                    val scaledImageWidth =
                                                                        fittedImageWidth * itemScale
                                                                    val scaledImageHeight =
                                                                        fittedImageHeight * itemScale
                                                                    val maxTranslateX =
                                                                        (scaledImageWidth - containerWidthPx).coerceAtLeast(
                                                                            0f
                                                                        ) / 2f
                                                                    val maxTranslateY =
                                                                        (scaledImageHeight - containerHeightPx).coerceAtLeast(
                                                                            0f
                                                                        ) / 2f

                                                                    val centroid =
                                                                        event.calculateCentroid(
                                                                            useCurrent = true
                                                                        )
                                                                    itemOffsetX =
                                                                        (itemOffsetX - (centroid.x - itemOffsetX) * (itemScale / oldLocalItemScale - 1))
                                                                    itemOffsetY =
                                                                        (itemOffsetY - (centroid.y - itemOffsetY) * (itemScale / oldLocalItemScale - 1))
                                                                    itemOffsetX += panDelta.x
                                                                    itemOffsetY += panDelta.y
                                                                    itemOffsetX =
                                                                        itemOffsetX.coerceIn(
                                                                            -maxTranslateX,
                                                                            maxTranslateX
                                                                        )
                                                                    itemOffsetY =
                                                                        itemOffsetY.coerceIn(
                                                                            -maxTranslateY,
                                                                            maxTranslateY
                                                                        )
                                                                } else {
                                                                    itemOffsetX = 0f
                                                                    itemOffsetY = 0f
                                                                }
                                                                changes.forEach { it.consume() }
                                                            } else {
                                                                TimberLogger.logI(
                                                                    tag,
                                                                    "PZ_DEBUG: Page $pageIndexInCurl (CUR=${pageCurlState.current}) In zoom loop. Not enough changes (${changes.size}) and not zoomed (oldScale=$oldLocalItemScale)."
                                                                )
                                                            }
                                                        }
                                                    } catch (e: CancellationException) {
                                                        TimberLogger.logE(
                                                            tag,
                                                            "PZ_DEBUG: Page $pageIndexInCurl (CUR=${pageCurlState.current}) CANCELLATION in awaitPointerEventScope's while loop",
                                                            e
                                                        )
                                                        throw e
                                                    } catch (e: Throwable) {
                                                        TimberLogger.logE(
                                                            tag,
                                                            "PZ_DEBUG: Page $pageIndexInCurl (CUR=${pageCurlState.current}) EXCEPTION in awaitPointerEventScope's while loop",
                                                            e
                                                        )
                                                    } finally {
                                                        TimberLogger.logI(
                                                            tag,
                                                            "PZ_DEBUG: Page $pageIndexInCurl (CUR=${pageCurlState.current}) FINALLY block of awaitPointerEventScope's try."
                                                        )
                                                    }
                                                }
                                            } else {
                                                TimberLogger.logI(
                                                    tag,
                                                    "PZ_DEBUG: Page $pageIndexInCurl (CUR=${pageCurlState.current}) currentBitmap IS NULL. SKIPPING awaitPointerEventScope."
                                                )
                                            }
                                        } else {
                                            if (itemScale > 1f) {
                                                itemScale = 1f
                                                itemOffsetX = 0f
                                                itemOffsetY = 0f
                                            }
                                        }
                                    }
                                    .graphicsLayer {
                                        scaleX = itemScale
                                        scaleY = itemScale
                                        translationX = itemOffsetX
                                        translationY = itemOffsetY
                                    }
                            }

                            if (currentBitmap != null) {
                                Image(
                                    modifier = if (pageIndexInCurl == pageCurlState.current) {
                                        imageDisplayModifier
                                            .align(Alignment.Center)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    } else {
                                        Modifier
                                            .align(Alignment.Center)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    },
                                    bitmap = currentBitmap,
                                    contentDescription = stringResource(
                                        R.string.comic_page_description,
                                        pageIndexInCurl + 1
                                    ),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isThisPageActuallyLoading) {
                                        CircularProgressIndicator()
                                    } else {
                                        Text(
                                            "Page ${pageIndexInCurl + 1}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.7f
                                            )
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
                isLoadingPage = setOf(0),
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
                loadedPages = emptyMap(),
                currentPage = 2,
                pageCount = 5,
                isLoadingPage = setOf(2),
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
                isLoadingPage = emptySet(),
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
