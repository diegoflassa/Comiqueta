package dev.diegoflassa.comiqueta.ui.widgets

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.theme.bottomAppBarSelectedIcon
import dev.diegoflassa.comiqueta.core.theme.bottomAppBarSelectedText
import dev.diegoflassa.comiqueta.core.theme.bottomAppBarUnselectedIcon
import dev.diegoflassa.comiqueta.core.theme.bottomAppBarUnselectedText
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.ui.enums.BottomNavItems

@Composable
fun BottomNavItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    label: String,
    type: BottomNavItems = BottomNavItems.UNKNOWN,
    isSelected: Boolean,
    onClick: ((BottomNavItems) -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = { onClick?.invoke(type) }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(ComiquetaTheme.dimen.bottomAppBarIconSize.scaled()),
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) ComiquetaTheme.colorScheme.bottomAppBarSelectedIcon else ComiquetaTheme.colorScheme.bottomAppBarUnselectedIcon
        )
        Text(
            text = label,
            color = if (isSelected) ComiquetaTheme.colorScheme.bottomAppBarSelectedText else ComiquetaTheme.colorScheme.bottomAppBarUnselectedText,
            style = ComiquetaTheme.typography.bottomAppBarText
        )
    }
}

// --- BottomNavItem Previews ---

@Preview(name = "BottomNavItem · Home Selected · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "BottomNavItem · Home Selected · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun BottomNavItemHomeSelectedPreview() {
    ComiquetaThemeContent {
        Surface {
            BottomNavItem(
                label = "Home",
                icon = Icons.Filled.Home,
                contentDescription = "Home",
                isSelected = true,
                onClick = {}
            )
        }
    }
}

@Preview(name = "BottomNavItem · Home Selected · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BottomNavItemHomeSelectedDarkPreview() {
    ComiquetaThemeContent {
        Surface {
            BottomNavItem(
                label = "Home",
                icon = Icons.Filled.Home,
                contentDescription = "Home",
                isSelected = true,
                onClick = {}
            )
        }
    }
}

@Preview(name = "BottomNavItem · Favorites Unselected · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "BottomNavItem · Favorites Unselected · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun BottomNavItemFavoritesUnselectedPreview() {
    ComiquetaThemeContent {
        Surface {
            BottomNavItem(
                label = "Favorites",
                icon = Icons.Filled.Favorite,
                contentDescription = "Favorites",
                isSelected = false,
                onClick = {}
            )
        }
    }
}

@Preview(name = "BottomNavItem · Favorites Unselected · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BottomNavItemFavoritesUnselectedDarkPreview() {
    ComiquetaThemeContent {
        Surface {
            BottomNavItem(
                label = "Favorites",
                icon = Icons.Filled.Favorite,
                contentDescription = "Favorites",
                isSelected = false,
                onClick = {}
            )
        }
    }
}

@Preview(name = "BottomNavItem · Settings Unselected · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "BottomNavItem · Settings Unselected · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun BottomNavItemSettingsUnselectedPreview() {
    ComiquetaThemeContent {
        Surface {
            BottomNavItem(
                label = "Settings",
                icon = Icons.Filled.Settings,
                contentDescription = "Settings",
                isSelected = false,
                onClick = {}
            )
        }
    }
}

@Preview(name = "BottomNavItem · Settings Unselected · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BottomNavItemSettingsUnselectedDarkPreview() {
    ComiquetaThemeContent {
        Surface {
            BottomNavItem(
                label = "Settings",
                icon = Icons.Filled.Settings,
                contentDescription = "Settings",
                isSelected = false,
                onClick = {}
            )
        }
    }
}