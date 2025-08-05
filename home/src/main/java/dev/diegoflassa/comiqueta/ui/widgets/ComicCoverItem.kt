package dev.diegoflassa.comiqueta.ui.widgets

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import dev.diegoflassa.comiqueta.core.data.model.Comic
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.home.R
import dev.diegoflassa.comiqueta.ui.home.HomeIntent

@Composable
fun ComicCoverItem(
    modifier: Modifier = Modifier,
    comic: Comic?,
    aspectRatio : Float = 2f / 3f,
    onIntent: ((HomeIntent) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .height(150.dp.scaled())
            .aspectRatio(aspectRatio)
            .clickable { onIntent?.invoke(HomeIntent.ComicSelected(comic)) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp.scaled()),
        shape = RoundedCornerShape(8.dp.scaled())
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = comic?.coverPath.takeIf { it != Uri.EMPTY }
                    ?: comic?.filePath.takeIf { it != Uri.EMPTY },
                error = painterResource(id = R.drawable.ic_placeholder_comic),
                placeholder = painterResource(id = R.drawable.ic_placeholder_comic)
            ),
            contentDescription = comic?.title
                ?: stringResource(id = R.string.comic_cover_image_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
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

@PreviewScreenSizes
@Preview(name = "ComicCoverItem - Dark", group = "ComicCoverItem", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ComicCoverItemPreview() {
    ComiquetaThemeContent {
        Surface {
            ComicCoverItem(
                comic = sampleComicForCoverPreview,
                onIntent = {}
            )
        }
    }
}

@PreviewScreenSizes
@Preview(name = "ComicCoverItem - Null Comic (Placeholder) - Dark", group = "ComicCoverItem", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ComicCoverItemNullPreview() {
    ComiquetaThemeContent {
        Surface {
            ComicCoverItem(
                comic = null,
                onIntent = {}
            )
        }
    }
}

@PreviewScreenSizes
@Preview(name = "ComicCoverItem - Custom Aspect Ratio - Dark", group = "ComicCoverItem", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ComicCoverItemCustomAspectRatioPreview() {
    ComiquetaThemeContent {
        Surface {
            ComicCoverItem(
                comic = sampleComicForCoverPreview,
                aspectRatio = 1f, // Square aspect ratio
                onIntent = {}
            )
        }
    }
}
