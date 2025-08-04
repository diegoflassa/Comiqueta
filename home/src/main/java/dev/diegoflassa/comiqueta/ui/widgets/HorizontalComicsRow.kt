package dev.diegoflassa.comiqueta.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import dev.diegoflassa.comiqueta.core.data.model.Comic
import dev.diegoflassa.comiqueta.ui.home.HomeIntent


@Composable
fun HorizontalComicsRowPreview(
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
            key = { index -> comics[index]?.filePath.toString() }
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