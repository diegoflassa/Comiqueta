package dev.diegoflassa.comiqueta.home.ui.widgets

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import dev.diegoflassa.comiqueta.core.data.model.Comic
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.home.R
import dev.diegoflassa.comiqueta.home.ui.home.HomeIntent

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
            modifier = Modifier.padding(8.dp.scaled()),
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
                    .height(90.dp.scaled())
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(4.dp.scaled()))
            )
            Spacer(modifier = Modifier.width(16.dp.scaled()))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comic?.title ?: stringResource(id = R.string.unknown_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                comic?.author?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                    )
                }
            }
            if (comic?.isFavorite == true) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.favorite_icon_description),
                    modifier = Modifier.padding(start = 8.dp.scaled())
                )
            }
        }
    }
}
