package dev.diegoflassa.comiqueta.ui.widgets

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.home.R
import dev.diegoflassa.comiqueta.ui.enums.BottomNavItems
import dev.diegoflassa.comiqueta.ui.home.HomeIntent
import dev.diegoflassa.comiqueta.ui.home.HomeUIState

object HomeBottomAppBarTestTags {
    const val BODY = "home_bottom_bar_body"
    const val ACTION = "home_bottom_bar_action"
    const val HOME = "home_bottom_bar_home"
    const val CATALOG = "home_bottom_bar_catalog"
    const val BOOKMARKS = "home_bottom_bar_bookmarks"
    const val FAVORITES = "home_bottom_bar_favorites"
}

@Composable
fun HomeBottomAppBar(
    modifier: Modifier = Modifier,
    uiState: HomeUIState? = null,
    onIntent: ((HomeIntent) -> Unit)? = null
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (!maxWidth.value.isFinite() || maxWidth <= 0.dp) {
            return@BoxWithConstraints
        }
        val geometry = remember(maxWidth) { HomeBottomBarGeometry.fromWidth(maxWidth) }
        val navTextStyle = ComiquetaTheme.typography.bottomAppBarText.let { style ->
            style.copy(
                fontSize = style.fontSize * geometry.scale,
                lineHeight = style.lineHeight * geometry.scale,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(geometry.occupiedHeight)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(geometry.bodyHeight)
                    .testTag(HomeBottomAppBarTestTags.BODY)
                    .background(
                        color = ComiquetaTheme.colorScheme.surface,
                        shape = ComiquetaTheme.shapes.bottomBarShape
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(geometry.bodyHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag(HomeBottomAppBarTestTags.HOME),
                        icon = Icons.Outlined.Home,
                        contentDescription = stringResource(R.string.bottom_nav_home),
                        label = stringResource(R.string.bottom_nav_home),
                        type = BottomNavItems.HOME,
                        isSelected = if (uiState != null) {
                            uiState.currentBottomNavItem == BottomNavItems.HOME
                        } else {
                            true
                        },
                        iconSize = geometry.navIconSize,
                        textStyle = navTextStyle,
                    ) { onIntent?.invoke(HomeIntent.ShowAllComics) }
                    BottomNavItem(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag(HomeBottomAppBarTestTags.CATALOG),
                        icon = Icons.AutoMirrored.Outlined.LibraryBooks,
                        contentDescription = stringResource(R.string.bottom_nav_catalog),
                        label = stringResource(R.string.bottom_nav_catalog),
                        type = BottomNavItems.CATALOG,
                        isSelected = uiState?.currentBottomNavItem == BottomNavItems.CATALOG,
                        iconSize = geometry.navIconSize,
                        textStyle = navTextStyle,
                    ) { onIntent?.invoke(HomeIntent.ShowReadComics) }
                }
                Spacer(modifier = Modifier.width(geometry.cutoutWidth))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag(HomeBottomAppBarTestTags.BOOKMARKS),
                        icon = Icons.Outlined.Bookmark,
                        contentDescription = stringResource(R.string.bottom_nav_bookmarks),
                        label = stringResource(R.string.bottom_nav_bookmarks),
                        type = BottomNavItems.BOOKMARKS,
                        isSelected = uiState?.currentBottomNavItem == BottomNavItems.BOOKMARKS,
                        iconSize = geometry.navIconSize,
                        textStyle = navTextStyle,
                    ) { onIntent?.invoke(HomeIntent.ShowNewComics) }
                    BottomNavItem(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag(HomeBottomAppBarTestTags.FAVORITES),
                        icon = Icons.Outlined.Favorite,
                        contentDescription = stringResource(R.string.bottom_nav_favorites),
                        label = stringResource(R.string.bottom_nav_favorites),
                        type = BottomNavItems.FAVORITES,
                        isSelected = uiState?.currentBottomNavItem == BottomNavItems.FAVORITES,
                        iconSize = geometry.navIconSize,
                        textStyle = navTextStyle,
                    ) { onIntent?.invoke(HomeIntent.ShowFavoriteComics) }
                }
            }
            FloatingActionButton(
                onClick = {
                    if (uiState?.isAddFolderInFlight == true) return@FloatingActionButton
                    onIntent?.invoke(HomeIntent.AddFolderClicked)
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(geometry.circleDiameter)
                    .zIndex(1f)
                    .testTag(HomeBottomAppBarTestTags.ACTION),
                shape = CircleShape,
                containerColor = ComiquetaTheme.colorScheme.primary,
                contentColor = ComiquetaTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                ),
            ) {
                Icon(
                    modifier = Modifier.size(geometry.actionIconSize),
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_fab_description)
                )
            }
        }
    }
}

// --- HomeBottomAppBar Previews ---
@Preview(name = "HomeBottomAppBar · Default · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "HomeBottomAppBar · Default · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun HomeBottomAppBarDefaultPreview() {
    ComiquetaThemeContent {
        Surface {
            HomeBottomAppBar(
                onIntent = {}
            )
        }
    }
}

@Preview(name = "HomeBottomAppBar · Default · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeBottomAppBarDefaultDarkPreview() {
    ComiquetaThemeContent {
        Surface {
            HomeBottomAppBar(
                onIntent = {}
            )
        }
    }
}

// --- EmptyStateContent Previews ---
@Preview(name = "EmptyStateContent · Default · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "EmptyStateContent · Default · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun EmptyStateContentDefaultPreview() {
    ComiquetaThemeContent {
        Surface {
            EmptyStateContent(
                onIntent = {}
            )
        }
    }
}

@Preview(name = "EmptyStateContent · Default · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyStateContentDefaultDarkPreview() {
    ComiquetaThemeContent {
        Surface {
            EmptyStateContent(
                onIntent = {}
            )
        }
    }
}
