package dev.diegoflassa.comiqueta.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.diegoflassa.comiqueta.core.R as CoreR
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.extensions.toDp
import dev.diegoflassa.comiqueta.core.data.mappers.asExternalModel
import dev.diegoflassa.comiqueta.core.domain.model.Comic
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.theme.getOutlinedTextFieldDefaultsColors
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.home.R
import dev.diegoflassa.comiqueta.ui.enums.ViewMode
import dev.diegoflassa.comiqueta.ui.home.COMIC_COVER_ASPECT_RATIO
import dev.diegoflassa.comiqueta.ui.home.HomeIntent
import dev.diegoflassa.comiqueta.ui.home.HomeUIState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicsContentForPreview(
    modifier: Modifier = Modifier,
    comics: List<Comic>,
    latestComics: List<Comic>,
    favoriteComics: List<Comic>,
    uiState: HomeUIState,
    onIntent: ((HomeIntent) -> Unit)? = null,
) {
    var sectionHeaderLatestViewMode by remember { mutableStateOf(ViewMode.GRID) }
    var sectionHeaderFavoritesViewMode by remember { mutableStateOf(ViewMode.GRID) }
    var sectionHeaderAllViewMode by remember { mutableStateOf(ViewMode.LIST) }

    var latestComicsExpanded by remember { mutableStateOf(true) }
    var favoriteComicsExpanded by remember { mutableStateOf(true) }
    var allComicsExpanded by remember { mutableStateOf(true) }

    val screenWidthDp = LocalWindowInfo.current.containerSize.width.dp
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onIntent?.invoke(HomeIntent.SearchComics(it)) },
                placeholder = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = stringResource(R.string.search_comics_placeholder),
                            style = ComiquetaTheme.typography.searchText.scaled()
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(ComiquetaTheme.dimen.iconSize.scaled()),
                        painter = painterResource(id = CoreR.drawable.ic_search),
                        contentDescription = stringResource(R.string.search_icon_description),
                        tint = ComiquetaTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = ComiquetaTheme.dimen.searchTopPadding,
                        bottom = ComiquetaTheme.dimen.searchBottomPadding,
                        start = ComiquetaTheme.dimen.searchHorizontalPadding,
                        end = ComiquetaTheme.dimen.searchHorizontalPadding
                    )
                    .height(ComiquetaTheme.dimen.inputHeight.scaled())
                    .clip(RoundedCornerShape(ComiquetaTheme.dimen.searchRoundedCorner.scaled())),
                colors = getOutlinedTextFieldDefaultsColors(),
                singleLine = true
            )
        }

        if (latestComics.isNotEmpty()) {
            item { // Latest Comics Header
                SectionHeader(
                    title = stringResource(R.string.latest_comics_section_title),
                    isExpanded = latestComicsExpanded,
                    onHeaderClick = { latestComicsExpanded = !latestComicsExpanded },
                    currentViewMode = sectionHeaderLatestViewMode
                ) { newViewMode -> sectionHeaderLatestViewMode = newViewMode }
            }
            if (latestComicsExpanded) {
                item {
                    HorizontalComicsRowForPreview(comics = latestComics, onIntent = onIntent)
                    Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerMedium.scaled()))
                }
            }
        }

        if (favoriteComics.isNotEmpty()) {
            item { // Favorite Comics Header
                SectionHeader(
                    title = stringResource(R.string.favorite_comics_section_title),
                    isExpanded = favoriteComicsExpanded,
                    onHeaderClick = { favoriteComicsExpanded = !favoriteComicsExpanded },
                    currentViewMode = sectionHeaderFavoritesViewMode
                ) { newViewMode -> sectionHeaderFavoritesViewMode = newViewMode }
            }
            if (favoriteComicsExpanded) {
                item {
                    HorizontalComicsRowForPreview(comics = favoriteComics, onIntent = onIntent)
                    Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerMedium.scaled()))
                }
            }
        }

        if (uiState.categories.isNotEmpty()) {
            item { // Categories Section
                CategoriesSection(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategoryClicked = { category ->
                        onIntent?.invoke(HomeIntent.CategorySelected(category))
                    }
                )
            }
        }

        // "All Comics" / "Results" Section
        if (comics.isNotEmpty() || uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) {
            item { // All Comics / Results Header
                SectionHeader(
                    title = if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) uiState.selectedCategory?.name
                        ?: stringResource(
                            R.string.results_section_title
                        ) else stringResource(R.string.all_comics_section_title),
                    isExpanded = allComicsExpanded,
                    showGridListOption = true,
                    onHeaderClick = { allComicsExpanded = !allComicsExpanded },
                    currentViewMode = sectionHeaderAllViewMode
                ) { newViewMode -> sectionHeaderAllViewMode = newViewMode }
            }
            if (allComicsExpanded) {
                if (comics.isNotEmpty()) {
                    when (sectionHeaderAllViewMode) {
                        ViewMode.LIST -> {
                            items(
                                comics.size,
                                key = { index -> comics[index].filePath }) { index ->
                                ComicListItem(
                                    comic = comics[index],
                                    aspectRatio = COMIC_COVER_ASPECT_RATIO,
                                    onIntent = onIntent
                                )
                                Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.paddingSmall.scaled()))
                            }
                        }

                        ViewMode.GRID -> {
                            item {
                                val configuration = LocalWindowInfo.current
                                val screenHeight = configuration.containerSize.height.toDp()
                                val gridHeight = (screenHeight * 0.6f).coerceAtLeast(200.dp)
                                val currentGridColumnCount = when {
                                    screenWidthDp < 600.dp -> 3
                                    screenWidthDp < 840.dp -> 4
                                    else -> 5
                                }
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(currentGridColumnCount),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(gridHeight)
                                        .padding(horizontal = ComiquetaTheme.dimen.paddingLarge.scaled()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp.scaled()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp.scaled()),
                                    contentPadding = PaddingValues(vertical = 8.dp.scaled())
                                ) {
                                    items(
                                        count = comics.size,
                                        key = { index -> comics[index].filePath }
                                    ) { index ->
                                        ComicCoverItem(
                                            comic = comics[index],
                                            aspectRatio = COMIC_COVER_ASPECT_RATIO,
                                            onIntent = onIntent
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerMedium.scaled()))
                            }
                        }
                    }
                } else if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) {
                    item { // No Results
                        Text(
                            text = stringResource(R.string.no_comics_found_for_search),
                            modifier = Modifier
                                .padding(ComiquetaTheme.dimen.paddingLarge.scaled())
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerMedium.scaled()))
        }
    }
}

private val sampleComicsForPreview = listOf(
    ComicEntity(
        filePath = "file:///comic1".toUri(),
        title = "Comic Adventure 1",
        isFavorite = true,
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+1".toUri()
    ).asExternalModel(), ComicEntity(
        filePath = "file:///comic2".toUri(),
        title = "Mystery of the Void",
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+2".toUri()
    ).asExternalModel(), ComicEntity(
        filePath = "file:///comic3".toUri(),
        title = "Chronicles of Code",
        isFavorite = false,
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+3".toUri()
    ).asExternalModel(), ComicEntity(
        filePath = "file:///comic4".toUri(),
        title = "Epic Tales",
        isFavorite = true,
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+4".toUri()
    ).asExternalModel()
)

// region Previews
@Preview(name = "ComicsContentForPreview · Default · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "ComicsContentForPreview · Default · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun ComicsContentForPreviewDefaultPreview() {
    ComiquetaThemeContent {
        ComicsContentForPreview(
            comics = sampleComicsForPreview,
            latestComics = sampleComicsForPreview,
            favoriteComics = sampleComicsForPreview.filter { it.isFavorite },
            uiState = HomeUIState(isLoading = false),
            onIntent = {}
        )
    }
}
// endregion
