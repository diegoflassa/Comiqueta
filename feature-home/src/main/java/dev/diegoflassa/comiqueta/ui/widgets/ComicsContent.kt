package dev.diegoflassa.comiqueta.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
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
import dev.diegoflassa.comiqueta.ui.home.rememberPreviewLazyPagingItems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicsContent(
    // For actual app with LazyPagingItems
    modifier: Modifier = Modifier,
    comics: LazyPagingItems<Comic>,
    latestComics: LazyPagingItems<Comic>,
    favoriteComics: LazyPagingItems<Comic>,
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
    val currentGridColumnCount = when {
        screenWidthDp < 600.dp -> 3
        screenWidthDp < 840.dp -> 4
        else -> 5
    }

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
                        imageVector = Icons.Outlined.Search,
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

        if (latestComics.itemCount > 0) {
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
                    HorizontalComicsRow(comics = latestComics, onIntent = onIntent)
                    Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerMedium.scaled()))
                }
            }
        }

        if (favoriteComics.itemCount > 0) {
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
                    HorizontalComicsRow(comics = favoriteComics, onIntent = onIntent)
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
        if (comics.itemCount > 0 || uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) {
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
                if (comics.itemCount > 0) {
                    when (sectionHeaderAllViewMode) {
                        ViewMode.LIST -> {
                            items(
                                count = comics.itemCount,
                                key = comics.itemKey { it.filePath }
                            ) { index ->
                                val comic = comics[index]
                                ComicListItem(
                                    comic = comic,
                                    aspectRatio = COMIC_COVER_ASPECT_RATIO,
                                    onIntent = onIntent
                                )
                                Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerSmall.scaled()))
                            }
                        }

                        ViewMode.GRID -> {
                            // Chunking the paging items manually to simulate a grid within LazyColumn
                            val rowCount =
                                (comics.itemCount + currentGridColumnCount - 1) / currentGridColumnCount
                            items(rowCount) { rowIndex ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = ComiquetaTheme.dimen.paddingLarge.scaled()),
                                    horizontalArrangement = Arrangement.spacedBy(ComiquetaTheme.dimen.spacerSmall.scaled())
                                ) {
                                    for (columnIndex in 0 until currentGridColumnCount) {
                                        val itemIndex =
                                            rowIndex * currentGridColumnCount + columnIndex
                                        if (itemIndex < comics.itemCount) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                val comic = comics[itemIndex]
                                                ComicCoverItem(
                                                    comic = comic,
                                                    aspectRatio = COMIC_COVER_ASPECT_RATIO,
                                                    onIntent = onIntent
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerSmall.scaled()))
                            }
                        }
                    }
                } else if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) {
                    item {
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

private val sampleComicsForPagingPreview = listOf(
    ComicEntity(
        filePath = "file:///comic1".toUri(),
        title = "Comic Adventure 1",
        isFavorite = true,
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+1".toUri()
    ).asExternalModel(),
    ComicEntity(
        filePath = "file:///comic2".toUri(),
        title = "Mystery of the Void",
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+2".toUri()
    ).asExternalModel()
)

// region Previews
@Preview(name = "ComicsContent · Default · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "ComicsContent · Default · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun ComicsContentDefaultPreview() {
    val pagingItems = rememberPreviewLazyPagingItems(sampleComicsForPagingPreview)
    ComiquetaThemeContent {
        ComicsContent(
            comics = pagingItems,
            latestComics = pagingItems,
            favoriteComics = pagingItems,
            uiState = HomeUIState(isLoading = false),
            onIntent = {}
        )
    }
}
// endregion
