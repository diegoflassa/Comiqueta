package dev.diegoflassa.comiqueta.ui.widgets

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import dev.diegoflassa.comiqueta.core.data.model.Comic
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.ui.home.HomeIntent
import androidx.core.net.toUri
import kotlin.random.Random


@Composable
fun HorizontalComicsRow(
    comics: LazyPagingItems<Comic>,
    modifier: Modifier = Modifier,
    onIntent: ((HomeIntent) -> Unit)? = null
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            count = comics.itemCount,
            key = comics.itemKey { comic -> comic.filePath.toString() }
        ) { index ->
            val comic = comics[index]
            if (comic != null) {
                ComicCoverItem(comic = comic, onIntent = onIntent)
            } else {
                // Optional: You can display a placeholder while items are loading
                // PlaceholderComicCoverItem()
            }
        }
    }
}

@Composable
fun HorizontalComicsRowForPreview(
    comics: List<Comic?>,
    modifier: Modifier = Modifier,
    onIntent: ((HomeIntent) -> Unit)? = null
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            count = comics.size,
            key = { index -> comics[index]?.filePath ?: Random.nextInt() }
        ) { index ->
            val comic = comics[index]
            if (comic != null) {
                ComicCoverItem(comic = comic, onIntent = onIntent)
            } else {
                // Optional: You can display a placeholder while items are loading
                // PlaceholderComicCoverItem()
            }
        }
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
            // HorizontalComicsRowForPreview doesn't take a title directly
            // The title would be rendered by a parent composable in a real scenario
            HorizontalComicsRowForPreview(
                comics = sampleComicsPreviewData,
                onIntent = {} // Dummy intent handler for preview
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
                comics = emptyList(), // Test with an empty list
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
            // If you have a PlaceholderComicCoverItem(), your HorizontalComicsRowForPreview
            // would render it for the null items.
        }
    }
}
