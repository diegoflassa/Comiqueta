package dev.diegoflassa.comiqueta.ui.widgets

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import dev.diegoflassa.comiqueta.core.data.model.Comic
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
