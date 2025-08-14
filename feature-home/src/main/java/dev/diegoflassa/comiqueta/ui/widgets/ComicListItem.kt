package dev.diegoflassa.comiqueta.ui.widgets

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import dev.diegoflassa.comiqueta.core.domain.model.Comic
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.home.R
import dev.diegoflassa.comiqueta.ui.home.HomeIntent

@Composable
fun ComicListItem(
    comic: Comic?,
    aspectRatio: Float = 2f / 3f,
    onIntent: ((HomeIntent) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp.scaled(), vertical = 4.dp.scaled())
            .clickable { onIntent?.invoke(HomeIntent.ComicSelected(comic)) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp.scaled()),
        shape = RoundedCornerShape(8.dp.scaled())
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp.scaled()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = comic?.coverPath?.takeIf { it != Uri.EMPTY }
                        ?: comic?.filePath?.takeIf { it != Uri.EMPTY },
                    error = painterResource(id = R.drawable.ic_placeholder_comic),
                    placeholder = painterResource(id = R.drawable.ic_placeholder_comic)
                ),
                contentDescription = comic?.title
                    ?: stringResource(id = R.string.comic_cover_image_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(40.dp.scaled())
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(4.dp.scaled()))
            )

            Spacer(modifier = Modifier.width(8.dp.scaled()))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = comic?.title ?: stringResource(id = R.string.unknown_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Chapter ${comic?.chapter ?: "?"} - Page ${comic?.page ?: "?"}",
                    fontSize = 12.sp,
                    color = ComiquetaTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Icon(
                painter = rememberVectorPainter(Icons.Default.ChevronRight),
                contentDescription = stringResource(id = R.string.more_options),
                tint = ComiquetaTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .width(25.dp.scaled())
                    .aspectRatio(0.5f)
            )
        }
    }
}

// --- ComicCoverItem Previews ---
private val sampleComicForCoverPreview = Comic(
    filePath = Uri.EMPTY,
    title = "The Amazing Adventures of Preview Man",
    coverPath = Uri.EMPTY,
    author = "AI Author",
    categoryId = 1L,
    isFavorite = false,
    isNew = true,
    hasBeenRead = false,
    lastPageRead = 0,
    lastModified = System.currentTimeMillis(),
    created = System.currentTimeMillis() - 86400000L, // approx 1 day ago
    pageCount = 22
)

// --- ComicListItem Previews ---
private val sampleComicForListItemDefault = sampleComicForCoverPreview.copy(
    title = "Journey to the Preview Zone",
    author = "A. Writer",
    isNew = false,
    hasBeenRead = true,
    lastPageRead = 10
)

private val sampleComicForListItemFavorite = sampleComicForCoverPreview.copy(
    title = "The Favored One",
    author = "B. Author",
    isFavorite = true,
    isNew = false
)

private val sampleComicForListItemLongTitleNoAuthor = sampleComicForCoverPreview.copy(
    title = "The Incredibly Long and Winding Title of a Comic Book That Goes On And On And On",
    author = null,
    pageCount = 150
)

@PreviewScreenSizes
@Preview(
    name = "ComicListItem - Default - Dark",
    group = "ComicListItem",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ComicListItemDefaultPreview() {
    ComiquetaThemeContent {
        Surface {
            ComicListItem(
                comic = sampleComicForListItemDefault,
                onIntent = {}
            )
        }
    }
}

@PreviewScreenSizes
@Preview(
    name = "ComicListItem - Favorite - Dark",
    group = "ComicListItem",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ComicListItemFavoritePreview() {
    ComiquetaThemeContent {
        Surface {
            ComicListItem(
                comic = sampleComicForListItemFavorite,
                onIntent = {}
            )
        }
    }
}

@PreviewScreenSizes
@Preview(
    name = "ComicListItem - Long Title, No Author - Dark",
    group = "ComicListItem",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ComicListItemLongTitleNoAuthorPreview() {
    ComiquetaThemeContent {
        Surface {
            ComicListItem(
                comic = sampleComicForListItemLongTitleNoAuthor,
                onIntent = {}
            )
        }
    }
}

@PreviewScreenSizes
@Preview(
    name = "ComicListItem - Null Comic (Placeholder) - Dark",
    group = "ComicListItem",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ComicListItemNullPreview() {
    ComiquetaThemeContent {
        Surface {
            ComicListItem(
                comic = null,
                onIntent = {}
            )
        }
    }
}
