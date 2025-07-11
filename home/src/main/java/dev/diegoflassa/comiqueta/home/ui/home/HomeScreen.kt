package dev.diegoflassa.comiqueta.home.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.navigation.Screen
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.home.R

private const val tag = "HomeScreen"
private const val COMIC_COVER_ASPECT_RATIO = 2f / 3f

// Define ViewMode
enum class ViewMode {
    LIST, GRID
}

@Composable
fun HomeScreen(
    navigationViewModel: NavigationViewModel? = null,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by homeViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.processIntent(HomeIntent.LoadInitialData)
    }

    HomeScreenContent(
        navigationViewModel = navigationViewModel,
        uiState = uiState,
        onIntent = homeViewModel::processIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel? = null,
    uiState: HomeUIState,
    onIntent: ((HomeIntent) -> Unit)? = null,
) {
    val fabDiameter = ComiquetaTheme.dimen.fabDiameter.scaled()
    val bottomBarHeight = ComiquetaTheme.dimen.bottomBarHeight.scaled()
    val isEmpty = uiState.allComics.isEmpty() && uiState.searchQuery.isBlank()

    Scaffold(
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (isEmpty) {
                EmptyStateContent(
                    onAddClicked = { onIntent?.invoke(HomeIntent.AddFolderClicked) }
                )
            } else {
                ComicsContent(
                    uiState = uiState,
                    onIntent = onIntent,
                )
            }

            BottomAppBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(bottomBarHeight)
                    .graphicsLayer(
                        shape = ComiquetaTheme.shapes.bottomBarShape,
                        clip = true
                    ),
                containerColor = ComiquetaTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp.scaled(),
                content = {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp.scaled()),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavItem(
                            Icons.Default.Home,
                            stringResource(R.string.home),
                            true,
                            { navigationViewModel?.navigateTo(Screen.Home) },
                            Modifier.weight(1f)
                        )
                        BottomNavItem(
                            Icons.Default.Star,
                            stringResource(R.string.catalog),
                            false,
                            { navigationViewModel?.navigateTo(Screen.Catalog) },
                            Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(fabDiameter + 16.dp.scaled()))
                        BottomNavItem(
                            Icons.AutoMirrored.Filled.List,
                            stringResource(R.string.bookmarks),
                            false,
                            { navigationViewModel?.navigateTo(Screen.Bookmark) },
                            Modifier.weight(1f)
                        )
                        BottomNavItem(
                            Icons.Default.Favorite,
                            stringResource(R.string.favorites),
                            false,
                            { navigationViewModel?.navigateTo(Screen.Favorites) },
                            Modifier.weight(1f)
                        )
                    }
                })

            ExtendedFloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(1F)
                    .size(fabDiameter)
                    .offset(y = (-17).dp.scaled()),
                onClick = { onIntent?.invoke(HomeIntent.AddFolderClicked) },
                shape = CircleShape,
                containerColor = ComiquetaTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_fab_description)
                )
            }
        }
    }
}

@Composable
fun CategoriesSection(
    categories: List<CategoryEntity>,
    selectedCategory: CategoryEntity?,
    onCategoryClicked: (CategoryEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp.scaled(), vertical = 8.dp.scaled()),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        categories.forEach { category ->
            Text(
                text = category.name
                    ?: stringResource(R.string.unknown_category),
                color = if (category.name == selectedCategory?.name) MaterialTheme.colorScheme.primary else Color.Gray,
                fontWeight = if (category.name == selectedCategory?.name) FontWeight.Bold else FontWeight.Normal,
                fontSize = 16.sp.scaled(),
                modifier = Modifier
                    .padding(horizontal = 8.dp.scaled())
                    .clickable { onCategoryClicked(category) }
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        color = Color.White, // Change to  MaterialTheme.colorScheme.onSurface or similar
        fontSize = 18.sp.scaled(),
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(
            start = 16.dp.scaled(),
            end = 16.dp.scaled(),
            bottom = 8.dp.scaled()
        )
    )
}

@Composable
fun HorizontalComicsRow(
    comics: List<ComicEntity>,
    onIntent: ((HomeIntent) -> Unit)?
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp.scaled()),
        horizontalArrangement = Arrangement.spacedBy(16.dp.scaled())
    ) {
        items(comics) { comic ->
            ComicCoverItem(onIntent = onIntent, comic = comic)
        }
    }
}


@Composable
fun ComicsContent(
    modifier: Modifier = Modifier,
    uiState: HomeUIState,
    onIntent: ((HomeIntent) -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF222222)), // Change to MaterialTheme.colorScheme.surfaceVariant or background
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onIntent?.invoke(HomeIntent.SearchComics(it)) },
                placeholder = {
                    Text(
                        stringResource(R.string.search_placeholder),
                        color = Color.Gray
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_icon_description),
                        tint = Color.Gray
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp.scaled(), vertical = 8.dp.scaled())
                    .clip(RoundedCornerShape(8.dp.scaled()))
                    .background(Color(0xFF444444)), // Change to MaterialTheme.colorScheme.surfaceVariant
                colors = OutlinedTextFieldDefaults.colors().copy(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = Color.White, // MaterialTheme.colorScheme.primary
                    focusedTextColor = Color.White, // MaterialTheme.colorScheme.onSurface
                    unfocusedTextColor = Color.White // MaterialTheme.colorScheme.onSurface
                )
            )
        }

        if (uiState.categories.isNotEmpty()) {
            item {
                CategoriesSection(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategoryClicked = { category ->
                        onIntent?.invoke(HomeIntent.CategorySelected(category))
                    }
                )
            }
        }

        if (uiState.latestComics.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.latest_comics_section_title))
                HorizontalComicsRow(comics = uiState.latestComics, onIntent = onIntent)
                Spacer(modifier = Modifier.height(24.dp.scaled()))
            }
        }

        if (uiState.favoriteComics.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.favorite_comics_section_title))
                HorizontalComicsRow(comics = uiState.favoriteComics, onIntent = onIntent)
                Spacer(modifier = Modifier.height(24.dp.scaled()))
            }
        }

        // Section for "All Comics" / "Continue Reading" with view mode toggles
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp.scaled(),
                        end = 16.dp.scaled(),
                        top = 8.dp.scaled()
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = stringResource(R.string.all_comics_section_title),
                        modifier = Modifier.padding(bottom = 0.dp)
                    )
                    Row {
                        IconButton(onClick = { onIntent?.invoke(HomeIntent.ViewModeChanged(ViewMode.LIST)) }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.list_view_description),
                                tint = if (uiState.viewMode == ViewMode.LIST) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                        IconButton(onClick = { onIntent?.invoke(HomeIntent.ViewModeChanged(ViewMode.GRID)) }) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = stringResource(R.string.grid_view_description),
                                tint = if (uiState.viewMode == ViewMode.GRID) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp.scaled()))
            }
        }

        val displayComics = uiState.allComics

        when (uiState.viewMode) {
            ViewMode.LIST -> {
                items(displayComics) { comic ->
                    ContinueReadingItem(
                        onIntent = onIntent,
                        comic = comic,
                        modifier = Modifier.padding(
                            horizontal = 16.dp.scaled(),
                            vertical = 4.dp.scaled()
                        )
                    )
                }
            }

            ViewMode.GRID -> {
                val itemsPerRow = 3
                items(displayComics.chunked(itemsPerRow)) { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp.scaled()),
                        modifier = Modifier.padding(horizontal = 16.dp.scaled())
                    ) {
                        rowItems.forEach { comic ->
                            ComicCoverItem(
                                comic = comic,
                                onIntent = onIntent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(itemsPerRow - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(8.dp.scaled()))
                }
            }
        }
        item { Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.bottomBarHeight + ComiquetaTheme.dimen.fabDiameter / 2)) }
    }
}


@Composable
fun EmptyStateContent(
    modifier: Modifier = Modifier,
    onAddClicked: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp.scaled()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.no_comics_found_yet),
            fontSize = 18.sp.scaled(),
            textAlign = TextAlign.Center,
            color = Color.DarkGray, // Change to MaterialTheme.colorScheme.onSurfaceVariant
            modifier = Modifier.padding(bottom = 8.dp.scaled())
        )
        Text(
            stringResource(R.string.click_plus_to_select_folder),
            fontSize = 16.sp.scaled(),
            textAlign = TextAlign.Center,
            color = Color.Gray, // Change to MaterialTheme.colorScheme.onSurfaceVariant
            modifier = Modifier.padding(bottom = 32.dp.scaled())
        )
        Icon(
            Icons.Filled.Add,
            stringResource(R.string.add_book_icon_description),
            modifier = Modifier
                .size(128.dp.scaled())
                .padding(bottom = 16.dp.scaled())
                .clickable(onClick = onAddClicked),
            tint = Color.Gray // Change to MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun ComicCoverItem(
    comic: ComicEntity,
    modifier: Modifier = Modifier,
    onIntent: ((HomeIntent) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .width(120.dp.scaled())
            .padding(vertical = 4.dp.scaled())
            .clickable { onIntent?.invoke(HomeIntent.ComicSelected(comic)) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(8.dp.scaled()),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp.scaled())
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = comic.coverPath),
                contentDescription = comic.title
                    ?: stringResource(R.string.comic_cover_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(COMIC_COVER_ASPECT_RATIO)
                    .clip(RoundedCornerShape(topStart = 8.dp.scaled(), topEnd = 8.dp.scaled()))
            )
        }
        Spacer(modifier = Modifier.height(4.dp.scaled()))
        Text(
            text = comic.title ?: stringResource(R.string.no_title),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp.scaled(),
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = Color.White // Change to MaterialTheme.colorScheme.onSurface
        )
    }
}


@Composable
fun ContinueReadingItem(
    comic: ComicEntity,
    modifier: Modifier = Modifier,
    onIntent: ((HomeIntent) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp.scaled())
            .clickable { onIntent?.invoke(HomeIntent.ComicSelected(comic)) },
        shape = RoundedCornerShape(8.dp.scaled()),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp.scaled()),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp.scaled())
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = comic.coverPath),
                contentDescription = comic.title
                    ?: stringResource(R.string.comic_cover_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(90.dp.scaled())
                    .aspectRatio(COMIC_COVER_ASPECT_RATIO)
                    .clip(RoundedCornerShape(4.dp.scaled()))
            )
            Spacer(modifier = Modifier.width(16.dp.scaled()))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comic.title ?: stringResource(R.string.no_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp.scaled(),
                    maxLines = 2
                )
                comic.author?.let {
                    Text(text = it, fontSize = 12.sp.scaled(), color = Color.Gray)
                }
            }
        }
    }
}


@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomAppBarIconSize = ComiquetaTheme.dimen.bottomAppBarIconSize.scaled()
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp.scaled())
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(bottomAppBarIconSize),
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) ComiquetaTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp.scaled()))
        Text(
            text = label,
            style = ComiquetaTheme.typography.bottomAppBar.scaled(),
            color = if (isSelected) ComiquetaTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

// --- Previews ---
@Preview(
    name = "${tag}Empty:360x640",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Preview(
    name = "${tag}Empty:540x1260",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260
)
@Preview(
    name = "${tag}Empty:540x1260 Dark",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun HomeScreenPreviewEmptyMVI() {
    ComiquetaThemeContent {
        val homeUIState = HomeUIState(isLoading = false, allComics = emptyList())
        HomeScreenContent(uiState = homeUIState)
    }
}

@Preview(
    name = "${tag}WithData:360x640",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Preview(
    name = "${tag}WithData:360x640 Dark",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun HomeScreenPreviewWithData() {
    ComiquetaThemeContent {
        val sampleComics = listOf(
            ComicEntity(
                title = "Comic Alpha",
                coverPath = "...".toUri(),
                author = "Author A"
            ),
            ComicEntity(
                title = "The Adventures of Beta Long Title That Wraps",
                coverPath = "...".toUri(),
                author = "Author B"
            ),
            ComicEntity(
                title = "Gamma Stories",
                coverPath = "...".toUri(),
                author = "Author C"
            )
        )
        val sampleCategories = listOf(
            CategoryEntity(id = 1, name = "All"),
            CategoryEntity(id = 2, name = "Action"),
            CategoryEntity(id = 3, name = "Comedy")
        )
        val homeUIState = HomeUIState(
            isLoading = false,
            allComics = sampleComics,
            categories = sampleCategories,
            selectedCategory = sampleCategories.first(),
            latestComics = sampleComics.take(2),
            favoriteComics = sampleComics.takeLast(2),
            continueReadingComics = sampleComics,
            viewMode = ViewMode.LIST
        )
        HomeScreenContent(uiState = homeUIState)
    }
}

@Preview(
    name = "${tag}WithDataGrid:360x640",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Composable
fun HomeScreenPreviewWithDataGrid() {
    ComiquetaThemeContent {
        val sampleComics = listOf(
            ComicEntity(
                title = "Comic Alpha",
                coverPath = "...".toUri(),
                author = "Author A"
            ),
            ComicEntity(
                title = "The Adventures of Beta",
                coverPath = "...".toUri(),
                author = "Author B"
            ),
            ComicEntity(
                title = "Gamma Stories",
                coverPath = "...".toUri(),
                author = "Author C"
            ),
            ComicEntity(
                title = "Delta Force",
                coverPath = "...".toUri(),
                author = "Author D"
            )
        )
        val sampleCategories = listOf(
            CategoryEntity(id = 1, name = "All"),
            CategoryEntity(id = 2, name = "Action")
        )
        val homeUIState = HomeUIState(
            isLoading = false,
            allComics = sampleComics,
            categories = sampleCategories,
            selectedCategory = sampleCategories.first(),
            latestComics = sampleComics.take(2),
            favoriteComics = sampleComics.takeLast(2),
            continueReadingComics = sampleComics,
            viewMode = ViewMode.GRID
        )
        HomeScreenContent(uiState = homeUIState)
    }
}
