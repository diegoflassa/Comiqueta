package dev.diegoflassa.comiqueta.ui.widgets

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.paging.compose.LazyPagingItems
import dev.diegoflassa.comiqueta.core.domain.model.Comic
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.theme.trackBarThumbColor
import dev.diegoflassa.comiqueta.core.theme.trackBarTrackColor
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.ui.home.HomeIntent
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random


@Composable
fun HorizontalComicsRow(
    modifier: Modifier = Modifier,
    comics: LazyPagingItems<Comic>,
    itemsPerPage: Int = 4,
    onIntent: ((HomeIntent) -> Unit)? = null
) {
    val lazyListState = rememberLazyListState()
    val itemCount = comics.itemCount
    val pageCount by remember(itemCount, itemsPerPage) {
        derivedStateOf {
            if (itemCount > 0) ceil(itemCount.toFloat() / itemsPerPage).toInt() else 0
        }
    }
    val currentPage by remember(itemCount, pageCount, itemsPerPage) {
        derivedStateOf {
            if (itemCount > 0 && pageCount > 0 && lazyListState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
                val firstVisible = lazyListState.firstVisibleItemIndex
                val lastVisible =
                    lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: firstVisible
                val centerVisibleIndex = firstVisible + (lastVisible - firstVisible) / 2
                (centerVisibleIndex / itemsPerPage).coerceIn(0, pageCount - 1)
            } else 0
        }
    }

    if (itemCount == 0) {
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyRow(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                count = comics.itemCount,
                key = { index ->
                    comics[index]?.filePath?.toString() ?: index
                }
            ) { index ->
                val comic = comics[index]
                if (comic != null) {
                    ComicCoverItem(comic = comic, onIntent = onIntent)
                }
            }
        }

        if (pageCount > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            ScrollTrackIndicator(
                pageCount = pageCount,
                currentPage = currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun HorizontalComicsRowForPreview(
    modifier: Modifier = Modifier,
    comics: List<Comic?>,
    itemsPerPage: Int = 4,
    onIntent: ((HomeIntent) -> Unit)? = null
) {
    val lazyListState = rememberLazyListState()
    val itemCount = comics.size
    val pageCount by remember(itemCount, itemsPerPage) {
        derivedStateOf {
            if (itemCount > 0) ceil(itemCount.toFloat() / itemsPerPage).toInt() else 0
        }
    }
    val currentPage by remember(itemCount, pageCount, itemsPerPage) {
        derivedStateOf {
            if (itemCount > 0 && pageCount > 0 && lazyListState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
                val firstVisible = lazyListState.firstVisibleItemIndex
                val lastVisible =
                    lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: firstVisible
                val centerVisibleIndex = firstVisible + (lastVisible - firstVisible) / 2
                (centerVisibleIndex / itemsPerPage).coerceIn(0, pageCount - 1)
            } else 0
        }
    }

    if (itemCount == 0) {
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyRow(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                count = comics.size,
                key = { index ->
                    comics[index]?.filePath?.toString() ?: Random.nextInt()
                } // Preview can use Random for simplicity if keys aren't critical for preview stability
            ) { index ->
                val comic = comics[index]
                if (comic != null) {
                    ComicCoverItem(comic = comic, onIntent = onIntent)
                }
            }
        }
        if (pageCount > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            ScrollTrackIndicator(
                pageCount = pageCount,
                currentPage = currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ScrollTrackIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.trackBarTrackColor,
    thumbColor: Color = MaterialTheme.colorScheme.trackBarThumbColor,
    barHeight: Dp = 2.dp.scaled(),
    thumbIndicatorWidth: Dp = 23.dp.scaled()
) {
    if (pageCount <= 1) {
        // To maintain consistent layout height if needed, add a Spacer here:
        // Spacer(modifier = modifier.height(barHeight))
        return
    }

    BoxWithConstraints(
        modifier = modifier.height(barHeight)
    ) {
        val trackShape = RoundedCornerShape(percent = 50)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(trackColor, trackShape)
        )

        // Movable Thumb
        val thumbWidthActual = thumbIndicatorWidth.coerceAtMost(this.maxWidth)
        val thumbWidthPx = with(LocalDensity.current) { thumbWidthActual.toPx() }
        val trackWidthPx = this.constraints.maxWidth.toFloat()

        val draggableWidthPx = (trackWidthPx - thumbWidthPx).coerceAtLeast(0f)

        val currentOffsetRatio = if (pageCount > 1) {
            currentPage.toFloat() / (pageCount - 1).coerceAtLeast(1)
        } else {
            0f
        }
        val currentOffsetPx = (currentOffsetRatio * draggableWidthPx).coerceIn(0f, draggableWidthPx)

        Box(
            modifier = Modifier
                .offset { IntOffset(currentOffsetPx.roundToInt(), 0) }
                .width(thumbWidthActual)
                .fillMaxHeight()
                .background(thumbColor, trackShape)
        )
    }
}


// --- HorizontalComicsRow Previews (using HorizontalComicsRowForPreview) ---

// Dummy data for previews using your Comic class
private val sampleComicsPreviewData: List<Comic> = List(5) { index ->
    Comic(
        filePath = "file:///preview/comic_$index.cbz".toUri(),
        title = "Awesome Comic Adventure Vol. ${index + 1}",
        coverPath = if (index % 2 == 0) "file:///preview/cover_$index.jpg".toUri() else Uri.EMPTY,
        author = "Writer ${index + 1}",
        pageCount = 22 + index,
        isNew = index < 2,
        isFavorite = index == 1
    )
}

private val fewComicsPreviewData: List<Comic> = List(2) { index ->
    Comic(
        filePath = "file:///preview/few_comic_$index.cbz".toUri(),
        title = "Short Story Collection ${index + 1}",
        coverPath = "file:///preview/few_cover_$index.jpg".toUri(),
        author = "Author ${index + 1}",
        pageCount = 10 + index,
        hasBeenRead = index == 0
    )
}

private val manyComicsPreviewData: List<Comic> = List(15) { index ->
    Comic(
        filePath = "file:///preview/many_comic_$index.cbz".toUri(),
        title = "The Epic Saga of The Universe Part ${index + 1}",
        coverPath = if (index % 3 == 0) Uri.EMPTY else "file:///preview/many_cover_$index.jpg".toUri(),
        author = "Various Writers",
        pageCount = 100 + index * 5,
        lastModified = System.currentTimeMillis() - (index * 100000)
    )
}

// Data with nulls to test nullable item handling
private val comicsWithNullsPreviewData: List<Comic?> =
    listOf(
        sampleComicsPreviewData[0],
        null, // Placeholder for a loading item
        sampleComicsPreviewData[1],
        null,
        sampleComicsPreviewData[2]
    )


@PreviewScreenSizes
@Preview(
    name = "HorizontalComicsRow - Light - Default",
    group = "HorizontalComicsRow",
    showBackground = true
)
@Composable
private fun HorizontalComicsRowPreviewLightDefault() {
    ComiquetaThemeContent(darkTheme = false) {
        Surface {
            HorizontalComicsRowForPreview(
                comics = sampleComicsPreviewData,
                onIntent = {}
            )
        }
    }
}

@PreviewScreenSizes
@Preview(
    name = "HorizontalComicsRow - Dark - Default",
    group = "HorizontalComicsRow",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HorizontalComicsRowPreviewDarkDefault() {
    ComiquetaThemeContent(darkTheme = true) {
        Surface {
            HorizontalComicsRowForPreview(
                comics = sampleComicsPreviewData,
                onIntent = {}
            )
        }
    }
}

@PreviewScreenSizes
@Preview(
    name = "HorizontalComicsRow - Light - Few Items",
    group = "HorizontalComicsRow",
    showBackground = true
)
@Composable
private fun HorizontalComicsRowPreviewLightFewItems() {
    ComiquetaThemeContent(darkTheme = false) {
        Surface {
            HorizontalComicsRowForPreview(
                comics = fewComicsPreviewData,
                onIntent = {}
            )
        }
    }
}

@PreviewScreenSizes
@Preview(
    name = "HorizontalComicsRow - Dark - Many Items",
    group = "HorizontalComicsRow",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HorizontalComicsRowPreviewDarkManyItems() {
    ComiquetaThemeContent(darkTheme = true) {
        Surface {
            HorizontalComicsRowForPreview(
                comics = manyComicsPreviewData,
                onIntent = {}
            )
        }
    }
}

@PreviewScreenSizes
@Preview(
    name = "HorizontalComicsRow - Light - Empty List",
    group = "HorizontalComicsRow",
    showBackground = true
)
@Composable
private fun HorizontalComicsRowPreviewLightEmpty() {
    ComiquetaThemeContent(darkTheme = false) {
        Surface {
            HorizontalComicsRowForPreview(
                comics = emptyList(),
                onIntent = {}
            )
        }
    }
}

@PreviewScreenSizes
@Preview(
    name = "HorizontalComicsRow - Light - With Nulls (Loading)",
    group = "HorizontalComicsRow",
    showBackground = true
)
@Composable
private fun HorizontalComicsRowPreviewLightWithNulls() {
    ComiquetaThemeContent(darkTheme = false) {
        Surface {
            HorizontalComicsRowForPreview(
                comics = comicsWithNullsPreviewData,
                onIntent = {}
            )
        }
    }
}
