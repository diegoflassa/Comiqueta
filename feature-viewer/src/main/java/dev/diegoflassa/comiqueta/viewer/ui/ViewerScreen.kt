package dev.diegoflassa.comiqueta.viewer.ui

import android.app.Activity
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel
import dev.diegoflassa.comiqueta.viewer.R
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.PageFlip
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.config.PageCurlConfig
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.config.rememberPageCurlConfig
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.rememberPageCurlState
import kotlinx.coroutines.CancellationException
import android.graphics.Rect as AndroidRect

private const val tag = "ViewerScreen"

@Composable
fun ViewerScreen(
    modifier: Modifier = Modifier,
    comicPath: Uri? = null,
    navigationViewModel: NavigationViewModel? = hiltActivityViewModel(),
    viewerViewModel: ViewerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val viewerUIState: ViewerUIState by viewerViewModel.uiState.collectAsStateWithLifecycle()
    TimberLogger.logI(tag, "[PageNavFix] ViewerScreen composed. CurrentPage: ${viewerUIState.currentPage}, PageCount: ${viewerUIState.pageCount}")

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

                is ViewerEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Smart Reading Mode: Handle Immersive Mode
    DisposableEffect(viewerUIState.isUiVisible) {
        val activity = context as? Activity
        val window = activity?.window
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            if (!viewerUIState.isUiVisible) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            // Restore bars when leaving the screen or if needed
            activity?.window?.let { win ->
                WindowInsetsControllerCompat(win, win.decorView).show(WindowInsetsCompat.Type.systemBars())
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
    
    // Zoom state is now managed in ViewModel for persistence
    val globalZoomScale = uiState.zoomScale
    val globalZoomOffsetX = uiState.zoomOffsetX
    val globalZoomOffsetY = uiState.zoomOffsetY
    
    // Track if zoom is active (local state for gesture handling)
    // isPinchGestureInProgress tracks multi-touch contact independently from scale
    var isPinchGestureInProgress by remember { mutableStateOf(false) }
    val globalIsPinchZoomActive = globalZoomScale > 1.01f || isPinchGestureInProgress
    val pageCurlState = rememberPageCurlState(initialCurrent = uiState.currentPage)

    val density = LocalDensity.current
    val view = LocalView.current
    var exclusionRects by retain { mutableStateOf<List<AndroidRect>>(emptyList()) }

    val exclusionWidthPx = with(density) { ComiquetaTheme.dimen.inputHeight.roundToPx() }
    val exclusionHeightPx = with(density) { (ComiquetaTheme.dimen.inputHeight * 5).roundToPx() }

    DisposableEffect(exclusionRects) {
        view.systemGestureExclusionRects = exclusionRects
        onDispose {
            view.systemGestureExclusionRects = emptyList()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Black, // Ensure black background for comic
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()

                    val centerY = (bounds.top + bounds.bottom) / 2
                    val halfHeight = (exclusionHeightPx / 2).toFloat()

                    val top = (centerY - halfHeight).coerceAtLeast(bounds.top).toInt()
                    val bottom = (centerY + halfHeight).coerceAtMost(bounds.bottom).toInt()

                    val leftStrip = AndroidRect(
                        bounds.left.toInt(),
                        top,
                        (bounds.left + exclusionWidthPx).toInt(),
                        bottom
                    )
                    val rightStrip = AndroidRect(
                        (bounds.right - exclusionWidthPx).toInt(),
                        top,
                        bounds.right.toInt(),
                        bottom
                    )
                    exclusionRects = listOf(leftStrip, rightStrip)
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.pageCount == 0 && uiState.isLoadingPage.isNotEmpty() -> {
                    CircularProgressIndicator()
                }

                uiState.error != null -> {
                    Text(
                        text = uiState.error,
                        color = ComiquetaTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(ComiquetaTheme.dimen.paddingLarge.scaled())
                            .clickable { onIntent?.invoke(ViewerIntent.ErrorShown) },
                        textAlign = TextAlign.Center
                    )
                }

                uiState.pageCount > 0 -> {
                    if (uiState.isWebtoonMode) {
                        WebtoonContent(
                            uiState = uiState,
                            onPageSelected = { onIntent?.invoke(ViewerIntent.GoToPage(it)) },
                            onToggleUi = { onIntent?.invoke(ViewerIntent.ToggleUiVisibility) }
                        )
                    } else {
                        LocalContext.current
                        val mediaPlayer: MediaPlayer? = remember {
                            try {
                                // Placeholder: User should add R.raw.page_flip
                                // MediaPlayer.create(context, R.raw.page_flip)
                                null
                            } catch (e: Exception) {
                                null
                            }
                        }

                        val displayPageCount = if (uiState.isDoublePageMode) (uiState.pageCount + 1) / 2 else uiState.pageCount
                        val displayPageIndex = if (uiState.isDoublePageMode) uiState.currentPage / 2 else uiState.currentPage

                        LaunchedEffect(displayPageIndex) {
                            if (pageCurlState.current != displayPageIndex && displayPageIndex < displayPageCount) {
                                TimberLogger.logI(tag, "[PageNavFix] Updating pageCurlState.current from ${pageCurlState.current} to $displayPageIndex")
                                pageCurlState.current = displayPageIndex
                                if (uiState.isPageFlipSoundEnabled) {
                                    mediaPlayer?.start()
                                }
                            }
                        }

                        LaunchedEffect(pageCurlState.current) {
                            val targetPage = if (uiState.isDoublePageMode) pageCurlState.current * 2 else pageCurlState.current
                            if (targetPage != uiState.currentPage) {
                                onIntent?.invoke(ViewerIntent.GoToPage(targetPage))
                            }
                        }

                        val pageCurlConfig = rememberPageCurlConfig(
                            dragForwardEnabled = true,
                            dragBackwardEnabled = true,
                            tapForwardEnabled = true,
                            tapBackwardEnabled = true,
                            dragThreshold = 0.1f, // 10% of screen width (allows micro-swipes)
                            dragInteraction = PageCurlConfig.GestureDragInteraction(
                                forward = PageCurlConfig.GestureDragInteraction.Config(target = androidx.compose.ui.geometry.Rect(0.0f, 0.0f, 1.0f, 1.0f)),
                                backward = PageCurlConfig.GestureDragInteraction.Config(target = androidx.compose.ui.geometry.Rect(0.0f, 0.0f, 1.0f, 1.0f))
                            ),
                            tapInteraction = PageCurlConfig.TargetTapInteraction(
                                forward = PageCurlConfig.TargetTapInteraction.Config(target = androidx.compose.ui.geometry.Rect(0.7f, 0.0f, 1.0f, 1.0f)),
                                backward = PageCurlConfig.TargetTapInteraction.Config(target = androidx.compose.ui.geometry.Rect(0.0f, 0.0f, 0.3f, 1.0f))
                            ),
                            // Handle center taps (dead zone) for UI toggling
                            onCustomTap = { size, offset ->
                                val relativeX = offset.x / size.width
                                if (relativeX > 0.3f && relativeX < 0.7f) {
                                    onIntent?.invoke(ViewerIntent.ToggleUiVisibility)
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        // Disable page navigation when zoomed to allow panning
                        LaunchedEffect(globalIsPinchZoomActive) {
                            val navigationEnabled = !globalIsPinchZoomActive
                            pageCurlConfig.dragForwardEnabled = navigationEnabled
                            pageCurlConfig.dragBackwardEnabled = navigationEnabled
                            pageCurlConfig.tapForwardEnabled = navigationEnabled
                            pageCurlConfig.tapBackwardEnabled = navigationEnabled
                            TimberLogger.logI(tag, "[PageNavFix] Navigation Enabled: $navigationEnabled (ZoomActive: $globalIsPinchZoomActive)")
                        }

                        PageFlip(
                            modifier = Modifier.fillMaxSize(),
                            count = displayPageCount,
                            key = { uiState.comicPath.hashCode() },
                            state = pageCurlState,
                            config = pageCurlConfig,
                        ) { pageIndexInCurl ->
                            val pageIndex1 = if (uiState.isDoublePageMode) pageIndexInCurl * 2 else pageIndexInCurl
                            val pageIndex2 = if (uiState.isDoublePageMode) pageIndex1 + 1 else -1

                            // Use global zoom state if this is the active page
                            var itemScale by remember(pageIndexInCurl) { mutableFloatStateOf(1f) }
                            var itemOffsetX by remember(pageIndexInCurl) { mutableFloatStateOf(0f) }
                            var itemOffsetY by remember(pageIndexInCurl) { mutableFloatStateOf(0f) }
                            
                            // Track restoration state for this specific page instance
                            var hasRestored by remember(pageIndexInCurl) { mutableStateOf(false) }

                            // 1. Restoration & Reset Logic (Triggered by Page Change)
                            LaunchedEffect(pageCurlState.current) {
                                if (pageIndexInCurl == pageCurlState.current) {
                                    // We became current. Restore State.
                                    TimberLogger.logI(tag, "[PageNavFix] Became Current: Page $pageIndexInCurl. GlobalZoom=$globalZoomScale")
                                    if (globalZoomScale > 1.01f) {
                                        itemScale = globalZoomScale
                                        itemOffsetX = globalZoomOffsetX
                                        itemOffsetY = globalZoomOffsetY
                                        TimberLogger.logI(tag, "[PageNavFix] Restored global zoom: $globalZoomScale")
                                    }
                                    hasRestored = true
                                } else {
                                    // We are NOT current. Reset local state if needed.
                                    if (itemScale > 1f) {
                                        TimberLogger.logI(tag, "[PageNavFix] Reseting zoom on non-current page $pageIndexInCurl")
                                        itemScale = 1f
                                        itemOffsetX = 0f
                                        itemOffsetY = 0f
                                    }
                                    hasRestored = false
                                }
                            }

                            // 2. Sync Logic (Triggered by Local Scale Change)
                            // Only runs if we are current AND have restored
                            LaunchedEffect(itemScale, itemOffsetX, itemOffsetY, hasRestored) {
                                if (pageIndexInCurl == pageCurlState.current && hasRestored) {
                                    TimberLogger.logI(tag, "[PageNavFix] Syncing Global: Page $pageIndexInCurl. ItemScale=$itemScale")
                                    // Update Global Zoom State via ViewModel
                                    onIntent?.invoke(ViewerIntent.UpdateZoom(itemScale, itemOffsetX, itemOffsetY))
                                }
                            }

                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val currentBitmap1: ImageBitmap? = uiState.loadedPages[pageIndex1]
                                val currentBitmap2: ImageBitmap? = if (pageIndex2 in 0 until uiState.pageCount) uiState.loadedPages[pageIndex2] else null
                                
                                val isThisPageActuallyLoading: Boolean =
                                    uiState.isLoadingPage.contains(pageIndex1) || (pageIndex2 != -1 && uiState.isLoadingPage.contains(pageIndex2))

                                val imageDisplayModifier = retain(
                                    pageIndexInCurl,
                                    (currentBitmap1 != null),
                                    pageCurlState.current
                                ) {
                                    Modifier
                                        .fillMaxSize()
                                        .pointerInput(
                                            pageIndexInCurl,
                                            (currentBitmap1 != null),
                                            pageCurlState.current
                                        ) {
                                            if (pageIndexInCurl == pageCurlState.current) {
                                                if (currentBitmap1 != null) {
                                                    awaitPointerEventScope {
                                                        var localPinchActive = false
                                                        try {
                                                            while (true) {
                                                                val event =
                                                                    awaitPointerEvent(PointerEventPass.Main)

                                                                val changes = event.changes
                                                                if (changes.isEmpty()) {
                                                                    continue
                                                                }

                                                                val pressedCount = changes.count { it.pressed }

                                                                // Detect start of multi-touch gesture
                                                                if (pressedCount >= 2 && !localPinchActive) {
                                                                    localPinchActive = true
                                                                    isPinchGestureInProgress = true
                                                                    TimberLogger.logI(tag, "[PageNavFix] Pinch gesture started")
                                                                }

                                                                // Detect end of all touches after a pinch
                                                                if (localPinchActive && pressedCount == 0) {
                                                                    localPinchActive = false
                                                                    isPinchGestureInProgress = false
                                                                    TimberLogger.logI(tag, "[PageNavFix] Pinch gesture ended (all fingers lifted)")
                                                                    // Reset zoom state cleanly if back to 1.0
                                                                    if (itemScale <= 1.01f) {
                                                                        itemScale = 1f
                                                                        itemOffsetX = 0f
                                                                        itemOffsetY = 0f
                                                                    }
                                                                    changes.forEach { it.consume() }
                                                                    continue
                                                                }

                                                                val oldLocalItemScale = itemScale
                                                                if (changes.size >= 2 || oldLocalItemScale > 1f || localPinchActive) {
                                                                    val zoomFactor =
                                                                        if (pressedCount >= 2) event.calculateZoom() else 1f
                                                                    val panDelta =
                                                                        if (pressedCount >= 2) event.calculatePan() else Offset.Zero

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

                                                                        val primaryWidth = currentBitmap1.width.toFloat()
                                                                        val primaryHeight = currentBitmap1.height.toFloat()
                                                                        val imageAspectRatio = if (currentBitmap2 != null) {
                                                                            (primaryWidth + currentBitmap2.width.toFloat()) / primaryHeight
                                                                        } else {
                                                                            primaryWidth / primaryHeight
                                                                        }

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

                                                                    // Consume ALL events during a pinch gesture to prevent
                                                                    // the drag handler from interpreting residual finger
                                                                    // movement as a page turn
                                                                    if (localPinchActive || itemScale > 1.01f) {
                                                                        changes.forEach { it.consume() }
                                                                    }
                                                                }
                                                            }
                                                        } catch (e: CancellationException) {
                                                        // Reset pinch state on cancellation to avoid stuck state
                                                        if (localPinchActive) {
                                                            localPinchActive = false
                                                            isPinchGestureInProgress = false
                                                        }
                                                        throw e
                                                    } catch (e: Throwable) {
                                                        if (localPinchActive) {
                                                            localPinchActive = false
                                                            isPinchGestureInProgress = false
                                                        }
                                                    } finally {
                                                        if (localPinchActive) {
                                                            localPinchActive = false
                                                            isPinchGestureInProgress = false
                                                        }
                                                    }
                                                }
                                            } else {

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

                                if (currentBitmap1 != null) {
                                    Row(
                                        modifier = if (pageIndexInCurl == pageCurlState.current) {
                                            imageDisplayModifier
                                                .align(Alignment.Center)
                                                .background(ComiquetaTheme.colorScheme.surfaceVariant)
                                        } else {
                                            Modifier
                                                .align(Alignment.Center)
                                                .background(ComiquetaTheme.colorScheme.surfaceVariant)
                                        },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Image(
                                            bitmap = currentBitmap1,
                                            contentDescription = stringResource(
                                                R.string.comic_page_description,
                                                pageIndex1 + 1
                                            ),
                                            contentScale = ContentScale.Fit,
                                            modifier = if (currentBitmap2 != null) Modifier.weight(1f) else Modifier
                                        )
                                        if (currentBitmap2 != null) {
                                            Image(
                                                bitmap = currentBitmap2,
                                                contentDescription = stringResource(
                                                    R.string.comic_page_description,
                                                    pageIndex2 + 1
                                                ),
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                ComiquetaTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isThisPageActuallyLoading) {
                                            CircularProgressIndicator()
                                        } else {
                                            Text(
                                                stringResource(R.string.comic_page_description, pageIndex1 + 1),
                                                color = ComiquetaTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.7f
                                                )
                                            )
                                        }
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
                            .padding(ComiquetaTheme.dimen.paddingLarge.scaled()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            stringResource(R.string.no_comic_loaded_title),
                            style = ComiquetaTheme.typography.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            stringResource(R.string.no_comic_loaded_subtitle),
                            style = ComiquetaTheme.typography.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = ComiquetaTheme.dimen.paddingSmall.scaled())
                        )
                    }
                }
            }

            // Top Bar Overlay
            AnimatedVisibility(
                visible = uiState.isUiVisible,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TopAppBar(
                    title = { Text(uiState.comicTitle) },
                    navigationIcon = {
                        IconButton(onClick = { navigationViewModel?.goBack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back_icon_description)
                            )
                        }
                    },
                    actions = {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.set_as_cover)) },
                                onClick = {
                                    showMenu = false
                                    onIntent?.invoke(ViewerIntent.SetAsCover)
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ComiquetaTheme.colorScheme.surface.copy(alpha = 0.85f)
                    )
                )
            }

            // Bottom Bar Overlay
            AnimatedVisibility(
                visible = uiState.isUiVisible && uiState.pageCount > 0,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ComiquetaTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .padding(bottom = ComiquetaTheme.dimen.paddingMedium.scaled())
                ) {
                    ThumbnailNavBar(
                        uiState = uiState,
                        onPageSelected = { onIntent?.invoke(ViewerIntent.GoToPage(it)) },
                        onLoadThumbnail = { onIntent?.invoke(ViewerIntent.LoadThumbnail(it)) }
                    )
                    Text(
                        text = stringResource(
                            R.string.page_count_format,
                            uiState.currentPage + 1,
                            uiState.pageCount
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = ComiquetaTheme.dimen.paddingSmall.scaled()),
                        textAlign = TextAlign.Center,
                        style = ComiquetaTheme.typography.typography.bodyMedium,
                        color = ComiquetaTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun WebtoonContent(
    uiState: ViewerUIState,
    onPageSelected: (Int) -> Unit,
    onToggleUi: () -> Unit
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = uiState.currentPage)

    LaunchedEffect(uiState.currentPage) {
        if (listState.firstVisibleItemIndex != uiState.currentPage) {
            listState.scrollToItem(uiState.currentPage)
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex != uiState.currentPage) {
            onPageSelected(listState.firstVisibleItemIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .clickable { onToggleUi() }
    ) {
        items(uiState.pageCount) { index ->
            WebtoonPageItem(
                index = index,
                bitmap = uiState.loadedPages[index],
                isLoading = uiState.isLoadingPage.contains(index)
            )
        }
    }
}

@Composable
fun WebtoonPageItem(
    index: Int,
    bitmap: ImageBitmap?,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ComiquetaTheme.dimen.paddingExtraSmall.scaled())
            .heightIn(min = 200.dp.scaled()),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.comic_page_description, index + 1),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(400.dp.scaled())
                    .background(ComiquetaTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        stringResource(R.string.comic_page_description, index + 1),
                        color = ComiquetaTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun ThumbnailNavBar(
    modifier: Modifier = Modifier,
    uiState: ViewerUIState,
    onPageSelected: (Int) -> Unit,
    onLoadThumbnail: (Int) -> Unit
) {
    val scrollState = rememberLazyListState()

    // Auto-scroll to current page when it changes
    LaunchedEffect(uiState.currentPage) {
        if (uiState.pageCount > 0) {
            scrollState.animateScrollToItem(uiState.currentPage)
        }
    }

    LazyRow(
        state = scrollState,
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp.scaled()),
        contentPadding = PaddingValues(horizontal = ComiquetaTheme.dimen.paddingLarge.scaled()),
        horizontalArrangement = Arrangement.spacedBy(ComiquetaTheme.dimen.spacerSmall.scaled()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(uiState.pageCount) { index ->
            ThumbnailItem(
                index = index,
                bitmap = uiState.loadedThumbnails[index] ?: uiState.loadedPages[index],
                isSelected = uiState.currentPage == index,
                isLoading = uiState.isLoadingThumbnail.contains(index),
                onClick = { onPageSelected(index) },
                onLoadRequest = { onLoadThumbnail(index) }
            )
        }
    }
}

@Composable
fun ThumbnailItem(
    index: Int,
    bitmap: ImageBitmap?,
    isSelected: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    onLoadRequest: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (bitmap == null && !isLoading) {
            onLoadRequest()
        }
    }

    Box(
        modifier = Modifier
            .width(60.dp.scaled())
            .fillMaxHeight()
            .clip(RoundedCornerShape(ComiquetaTheme.dimen.paddingExtraSmall.scaled()))
            .background(ComiquetaTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 2.dp.scaled() else 0.dp,
                color = if (isSelected) ComiquetaTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(ComiquetaTheme.dimen.paddingExtraSmall.scaled())
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(ComiquetaTheme.dimen.bottomAppBarIconSize.scaled()),
                strokeWidth = 2.dp.scaled()
            )
        } else {
            Text(
                text = (index + 1).toString(),
                style = ComiquetaTheme.typography.typography.labelSmall,
                color = ComiquetaTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- Previews ---

@androidx.compose.ui.tooling.preview.PreviewScreenSizes
@Composable
private fun ViewerScreenContentPreview() {
    dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent {
        ViewerScreenContent(
            uiState = ViewerUIState(
                comicTitle = "Sample Comic",
                pageCount = 10,
                currentPage = 2,
                isUiVisible = true
            )
        )
    }
}

@androidx.compose.ui.tooling.preview.PreviewScreenSizes
@androidx.compose.ui.tooling.preview.Preview(
    name = "Viewer - Dark - Immersive",
    group = "Viewer",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ViewerScreenContentImmersivePreview() {
    dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent {
        ViewerScreenContent(
            uiState = ViewerUIState(
                comicTitle = "Sample Comic",
                pageCount = 10,
                currentPage = 5,
                isUiVisible = false
            )
        )
    }
}

@androidx.compose.ui.tooling.preview.PreviewScreenSizes
@Composable
private fun ViewerScreenContentLoadingPreview() {
    dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent {
        ViewerScreenContent(
            uiState = ViewerUIState(
                isLoadingPage = setOf(0),
                pageCount = 0
            )
        )
    }
}

@androidx.compose.ui.tooling.preview.PreviewScreenSizes
@Composable
private fun ViewerScreenContentErrorPreview() {
    dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent {
        ViewerScreenContent(
            uiState = ViewerUIState(
                error = "Failed to load comic file."
            )
        )
    }
}
