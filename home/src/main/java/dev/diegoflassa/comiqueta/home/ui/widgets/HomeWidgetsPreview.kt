package dev.diegoflassa.comiqueta.home.ui.widgets

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.data.model.Comic
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.home.ui.enums.ViewMode

// --- BottomNavItem Previews ---

@Preview(name = "BottomNavItem - Home Selected - Light", group = "BottomNavItem", showBackground = true)
@Preview(name = "BottomNavItem - Home Selected - Dark", group = "BottomNavItem", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BottomNavItemHomeSelectedPreview() {
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

@Preview(name = "BottomNavItem - Favorites Unselected - Light", group = "BottomNavItem", showBackground = true)
@Preview(name = "BottomNavItem - Favorites Unselected - Dark", group = "BottomNavItem", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BottomNavItemFavoritesUnselectedPreview() {
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

@Preview(name = "BottomNavItem - Settings Unselected - Light", group = "BottomNavItem", showBackground = true)
@Preview(name = "BottomNavItem - Settings Unselected - Dark", group = "BottomNavItem", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BottomNavItemSettingsUnselectedPreview() {
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

// --- CategoriesSection Previews ---

val sampleCategoriesForPreview = listOf(
    CategoryEntity(id = 0, name = "All"),
    CategoryEntity(id = 1, name = "Action"),
    CategoryEntity(id = 2, name = "Comedy"),
    CategoryEntity(id = 3, name = "Sci-Fi"),
    CategoryEntity(id = 4, name = "Fantasy")
)

@Preview(name = "CategoriesSection - All Selected - Light", group = "CategoriesSection", showBackground = true)
@Preview(name = "CategoriesSection - All Selected - Dark", group = "CategoriesSection", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CategoriesSectionAllSelectedPreview() {
    ComiquetaThemeContent {
        Surface {
            CategoriesSection(
                categories = sampleCategoriesForPreview,
                selectedCategory = sampleCategoriesForPreview.find { it.name == "All" },
                onCategoryClicked = {}
            )
        }
    }
}

@Preview(name = "CategoriesSection - Comedy Selected - Light", group = "CategoriesSection", showBackground = true)
@Preview(name = "CategoriesSection - Comedy Selected - Dark", group = "CategoriesSection", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CategoriesSectionComedySelectedPreview() {
    ComiquetaThemeContent {
        Surface {
            CategoriesSection(
                categories = sampleCategoriesForPreview,
                selectedCategory = sampleCategoriesForPreview.find { it.name == "Comedy" },
                onCategoryClicked = {}
            )
        }
    }
}

@Preview(name = "CategoriesSection - No Selection (Defaults to First) - Light", group = "CategoriesSection", showBackground = true)
@Preview(name = "CategoriesSection - No Selection (Defaults to First) - Dark", group = "CategoriesSection", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CategoriesSectionNoSelectionPreview() {
    ComiquetaThemeContent {
        Surface {
            CategoriesSection(
                categories = sampleCategoriesForPreview,
                selectedCategory = null,
                onCategoryClicked = {}
            )
        }
    }
}

@Preview(name = "CategoriesSection - Empty List - Light", group = "CategoriesSection", showBackground = true)
@Preview(name = "CategoriesSection - Empty List - Dark", group = "CategoriesSection", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CategoriesSectionEmptyPreview() {
    ComiquetaThemeContent {
        Surface {
            CategoriesSection(
                categories = emptyList(),
                selectedCategory = null,
                onCategoryClicked = {}
            )
        }
    }
}

// --- ComicCoverItem Previews ---
val sampleComicForCoverPreview = Comic(
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

@Preview(name = "ComicCoverItem - Light", group = "ComicCoverItem", showBackground = true)
@Preview(name = "ComicCoverItem - Dark", group = "ComicCoverItem", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ComicCoverItemPreview() {
    ComiquetaThemeContent {
        Surface {
            ComicCoverItem(
                comic = sampleComicForCoverPreview,
                onIntent = {}
            )
        }
    }
}

@Preview(name = "ComicCoverItem - Null Comic (Placeholder) - Light", group = "ComicCoverItem", showBackground = true)
@Preview(name = "ComicCoverItem - Null Comic (Placeholder) - Dark", group = "ComicCoverItem", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ComicCoverItemNullPreview() {
    ComiquetaThemeContent {
        Surface {
            ComicCoverItem(
                comic = null,
                onIntent = {}
            )
        }
    }
}

@Preview(name = "ComicCoverItem - Custom Aspect Ratio - Light", group = "ComicCoverItem", showBackground = true)
@Preview(name = "ComicCoverItem - Custom Aspect Ratio - Dark", group = "ComicCoverItem", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ComicCoverItemCustomAspectRatioPreview() {
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

// --- ComicListItem Previews ---
val sampleComicForListItemDefault = sampleComicForCoverPreview.copy(
    title = "Journey to the Preview Zone",
    author = "A. Writer",
    isNew = false,
    hasBeenRead = true,
    lastPageRead = 10
)

val sampleComicForListItemFavorite = sampleComicForCoverPreview.copy(
    title = "The Favored One",
    author = "B. Author",
    isFavorite = true,
    isNew = false
)

val sampleComicForListItemLongTitleNoAuthor = sampleComicForCoverPreview.copy(
    title = "The Incredibly Long and Winding Title of a Comic Book That Goes On And On And On",
    author = null,
    pageCount = 150
)

@Preview(name = "ComicListItem - Default - Light", group = "ComicListItem", showBackground = true)
@Preview(name = "ComicListItem - Default - Dark", group = "ComicListItem", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ComicListItemDefaultPreview() {
    ComiquetaThemeContent {
        Surface {
            ComicListItem(
                comic = sampleComicForListItemDefault,
                onIntent = {}
            )
        }
    }
}

@Preview(name = "ComicListItem - Favorite - Light", group = "ComicListItem", showBackground = true)
@Preview(name = "ComicListItem - Favorite - Dark", group = "ComicListItem", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ComicListItemFavoritePreview() {
    ComiquetaThemeContent {
        Surface {
            ComicListItem(
                comic = sampleComicForListItemFavorite,
                onIntent = {}
            )
        }
    }
}

@Preview(name = "ComicListItem - Long Title, No Author - Light", group = "ComicListItem", showBackground = true)
@Preview(name = "ComicListItem - Long Title, No Author - Dark", group = "ComicListItem", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ComicListItemLongTitleNoAuthorPreview() {
    ComiquetaThemeContent {
        Surface {
            ComicListItem(
                comic = sampleComicForListItemLongTitleNoAuthor,
                onIntent = {}
            )
        }
    }
}

@Preview(name = "ComicListItem - Null Comic (Placeholder) - Light", group = "ComicListItem", showBackground = true)
@Preview(name = "ComicListItem - Null Comic (Placeholder) - Dark", group = "ComicListItem", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ComicListItemNullPreview() {
    ComiquetaThemeContent {
        Surface {
            ComicListItem(
                comic = null,
                onIntent = {}
            )
        }
    }
}

// --- HomeBottomAppBar Previews ---
@Preview(name = "HomeBottomAppBar - Default - Light", group = "HomeBottomAppBar", showBackground = true)
@Preview(name = "HomeBottomAppBar - Default - Dark", group = "HomeBottomAppBar", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeBottomAppBarDefaultPreview() {
    ComiquetaThemeContent {
        Surface { 
            HomeBottomAppBar(
                onIntent = {}
            )
        }
    }
}

@Preview(name = "HomeBottomAppBar - Custom Height - Light", group = "HomeBottomAppBar", showBackground = true)
@Preview(name = "HomeBottomAppBar - Custom Height - Dark", group = "HomeBottomAppBar", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeBottomAppBarCustomHeightPreview() {
    ComiquetaThemeContent {
        Surface {
            HomeBottomAppBar(
                bottomBarHeight = 72.dp, 
                onIntent = {}
            )
        }
    }
}

// --- EmptyStateContent Previews ---
@Preview(name = "EmptyStateContent - Light", group = "EmptyStateContent", showBackground = true)
@Preview(name = "EmptyStateContent - Dark", group = "EmptyStateContent", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmptyStateContentPreview() {
    ComiquetaThemeContent {
        Surface {
            EmptyStateContent(
                onIntent = {}
            )
        }
    }
}

// --- SectionHeader Previews ---
@Preview(name = "SectionHeader - Expanded, Grid, View Options - Light", group = "SectionHeader", showBackground = true)
@Preview(name = "SectionHeader - Expanded, Grid, View Options - Dark", group = "SectionHeader", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SectionHeaderExpandedGridViewOptionsPreview() {
    ComiquetaThemeContent {
        Surface {
            SectionHeader(
                title = "Latest Comics",
                isExpanded = true,
                onHeaderClick = {},
                showGridListOption = true,
                currentViewMode = ViewMode.GRID,
                onViewTypeChange = {}
            )
        }
    }
}

@Preview(name = "SectionHeader - Collapsed, List, View Options - Light", group = "SectionHeader", showBackground = true)
@Preview(name = "SectionHeader - Collapsed, List, View Options - Dark", group = "SectionHeader", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SectionHeaderCollapsedListViewOptionsPreview() {
    ComiquetaThemeContent {
        Surface {
            SectionHeader(
                title = "Favorite Comics",
                isExpanded = false,
                onHeaderClick = {},
                showGridListOption = true,
                currentViewMode = ViewMode.LIST,
                onViewTypeChange = {}
            )
        }
    }
}

@Preview(name = "SectionHeader - Expanded, No View Options - Light", group = "SectionHeader", showBackground = true)
@Preview(name = "SectionHeader - Expanded, No View Options - Dark", group = "SectionHeader", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SectionHeaderExpandedNoViewOptionsPreview() {
    ComiquetaThemeContent {
        Surface {
            SectionHeader(
                title = "All Comics",
                isExpanded = true,
                onHeaderClick = {},
                showGridListOption = false, 
                currentViewMode = ViewMode.LIST,
                onViewTypeChange = {}
            )
        }
    }
}

@Preview(name = "SectionHeader - Collapsed, No View Options - Light", group = "SectionHeader", showBackground = true)
@Preview(name = "SectionHeader - Collapsed, No View Options - Dark", group = "SectionHeader", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SectionHeaderCollapsedNoViewOptionsPreview() {
    ComiquetaThemeContent {
        Surface {
            SectionHeader(
                title = "Search Results",
                isExpanded = false,
                onHeaderClick = {},
                showGridListOption = false,
                currentViewMode = ViewMode.GRID,
                onViewTypeChange = {}
            )
        }
    }
}

// More previews will be appended here for HorizontalComicsRowPreview...

