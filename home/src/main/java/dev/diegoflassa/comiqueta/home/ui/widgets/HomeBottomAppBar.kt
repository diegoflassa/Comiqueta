package dev.diegoflassa.comiqueta.home.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.BottomAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.theme.transparent
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.home.R
import dev.diegoflassa.comiqueta.home.ui.enums.BottomNavItems
import dev.diegoflassa.comiqueta.home.ui.home.HomeIntent


@Composable
fun HomeBottomAppBar(
    modifier: Modifier = Modifier,
    bottomBarHeight: Dp = ComiquetaTheme.dimen.bottomBarHeight.scaled(),
    onIntent: ((HomeIntent) -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(bottomBarHeight)
            .background(ComiquetaTheme.colorScheme.transparent)
            .zIndex(1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Group
        Row(
            modifier = Modifier
                .weight(1f)
                .background(ComiquetaTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Home,
                contentDescription = "home",
                label = stringResource(R.string.bottom_nav_home),
                type = BottomNavItems.HOME,
                isSelected = true,
            ) { onIntent?.invoke(HomeIntent.ShowAllComics) }
            BottomNavItem(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Outlined.LibraryBooks,
                contentDescription = "library",
                label = stringResource(R.string.bottom_nav_catalog),
                type = BottomNavItems.CATALOG,
                // For preview, uiState might not have currentBottomNavItem,
                // so set isSelected directly or adapt uiState for preview
                isSelected = false // Example: Catalog is not selected
            ) { onIntent?.invoke(HomeIntent.ShowFavoriteComics) }
        }
        BottomAppBar(
            modifier = modifier
                .width(90.dp.scaled())
                .height(bottomBarHeight)
                .graphicsLayer(
                    shape = ComiquetaTheme.shapes.bottomBarShape, clip = true
                ),
            containerColor = ComiquetaTheme.colorScheme.surface,
            tonalElevation = 4.dp.scaled(),
        ) {}
        // Right Group
        Row(
            modifier = Modifier
                .weight(1f)
                .background(ComiquetaTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Bookmark,
                contentDescription = "bookmarks",
                label = stringResource(R.string.bottom_nav_bookmarks),
                type = BottomNavItems.BOOKMARKS,
                isSelected = false // Example
            ) { onIntent?.invoke(HomeIntent.ShowNewComics) }
            BottomNavItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Favorite,
                contentDescription = "favorite",
                label = stringResource(R.string.bottom_nav_favorites),
                type = BottomNavItems.FAVORITES,
                isSelected = false // Example
            ) { onIntent?.invoke(HomeIntent.ShowFavoriteComics) }
        }
    }
}
