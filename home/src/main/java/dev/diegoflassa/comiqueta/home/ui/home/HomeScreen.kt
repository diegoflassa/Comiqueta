package dev.diegoflassa.comiqueta.home.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.ui.navigation.BottomNavItem
import dev.diegoflassa.comiqueta.core.ui.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.ui.navigation.Screen
import dev.diegoflassa.comiqueta.core.ui.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.ui.theme.scaled
import dev.diegoflassa.comiqueta.home.R
import dev.diegoflassa.comiqueta.core.data.preferences.UserPreferencesKeys

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigationViewModel: NavigationViewModel? = null,
    onIntent: ((HomeIntent) -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEmpty =
        uiState.allComics.isEmpty() && uiState.latestComics.isEmpty() && uiState.favoriteComics.isEmpty()

    val bottomBarHeight = 56.dp.scaled()
    val fabDiameter = 56.dp.scaled()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = { /* Handle navigation icon click */ }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button_description)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navigationViewModel?.navigateTo(Screen.Settings) }) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_button_description)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ComiquetaTheme.colorScheme.primaryContainer)
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply scaffold padding
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                isEmpty -> {
                    EmptyStateContent(onIntent = onIntent)
                }

                else -> {
                    ComicsContent(
                        uiState = uiState,
                        onIntent = onIntent,
                    )
                }
            }

            // Custom BottomAppBar and FAB
            BottomAppBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(bottomBarHeight)
                    .graphicsLayer(
                        shape = ComiquetaTheme.shapes.bottomBarShape, // Ensure this shape is defined in your theme
                        clip = true
                    ),
                containerColor = ComiquetaTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp.scaled(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp.scaled()),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Example BottomNavItems - replace with your actual items/logic
                    BottomNavItem(
                        Icons.Default.Home,
                        stringResource(R.string.home),
                        true,
                        { navigationViewModel?.navigateTo(Screen.Home) },
                        Modifier.weight(1f)
                    )
                    BottomNavItem(
                        Icons.Default.Star, // Changed from Catalog to Star to match common icon usage
                        stringResource(R.string.catalog),
                        false,
                        { navigationViewModel?.navigateTo(Screen.Catalog) },
                        Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(fabDiameter + 16.dp.scaled())) // Space for FAB
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
            }

            ExtendedFloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(1F) // Ensure FAB is above BottomAppBar
                    .size(fabDiameter)
                    .offset(y = (-17).dp.scaled()), // Adjust offset as needed
                onClick = { onIntent?.invoke(HomeIntent.AddFolderClicked) },
                shape = CircleShape,
                containerColor = ComiquetaTheme.colorScheme.primaryContainer, // Or secondary
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
fun EmptyStateContent(
    modifier: Modifier = Modifier,
    onIntent: ((HomeIntent) -> Unit)?
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp.scaled()),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.empty_state_title),
                fontSize = 20.sp.scaled(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp.scaled()))
            Text(
                text = stringResource(R.string.empty_state_message),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp.scaled()))
            ExtendedFloatingActionButton(
                onClick = { onIntent?.invoke(HomeIntent.AddFolderClicked) },
                icon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_folder_button)
                    )
                },
                text = { Text(stringResource(R.string.add_folder_button)) }
            )
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
        horizontalArrangement = Arrangement.Start
    ) {
        categories.forEach { category ->
            val categoryText = if (category.name.equals(UserPreferencesKeys.DEFAULT_CATEGORY_ALL, ignoreCase = true)) {
                stringResource(id = dev.diegoflassa.comiqueta.core.R.string.all)
            } else {
                category.name
            }
            Text(
                text = categoryText,
                color = if (category.id == selectedCategory?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (category.id == selectedCategory?.id) FontWeight.Bold else FontWeight.Normal,
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
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp.scaled(),
                end = 16.dp.scaled(),
                top = 16.dp.scaled(), // Added top padding for better spacing from search bar or previous section
                bottom = 8.dp.scaled()
            ),
        text = title,
        textAlign = TextAlign.Start,
        color = MaterialTheme.colorScheme.onSurface, // Use theme color
        fontSize = 18.sp.scaled(),
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun HorizontalComicsRow(
    comics: List<ComicEntity>, // Uses ComicEntity
    onIntent: ((HomeIntent) -> Unit)?
) {
    LazyRow(
        contentPadding = PaddingValues(
            horizontal = 16.dp.scaled(),
            vertical = 8.dp.scaled()
        ), // Added vertical padding
        horizontalArrangement = Arrangement.spacedBy(16.dp.scaled())
    ) {
        items(comics, key = { it.filePath.toString() }) { comic -> // Use filePath as key
            ComicCoverItem(comic = comic, onIntent = onIntent)
        }
    }
}


@Composable
fun ComicsContent(
    modifier: Modifier = Modifier,
    uiState: HomeUIState, // Uses ComicEntity within
    onIntent: ((HomeIntent) -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface), // Use theme background
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Search Bar
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onIntent?.invoke(HomeIntent.SearchComics(it)) },
                placeholder = {
                    Text(
                        stringResource(R.string.search_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_icon_description),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp.scaled(), vertical = 8.dp.scaled())
                    .clip(RoundedCornerShape(8.dp.scaled()))
                    .background(MaterialTheme.colorScheme.surfaceVariant), // Contrasting background for search
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent, // No border when focused
                    unfocusedBorderColor = Color.Transparent, // No border when unfocused
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, // Keep bg color
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Categories Section
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

        // Latest Comics Section
        // The ViewModel should ideally provide these filtered lists.
        // If not, this filtering logic can be kept here.
        val latestComics = uiState.latestComics // Assuming ViewModel populates this
        if (latestComics.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.latest_comics_section_title))
                HorizontalComicsRow(comics = latestComics, onIntent = onIntent)
                Spacer(modifier = Modifier.height(16.dp.scaled())) // Reduced spacer
            }
        }

        // Favorite Comics Section
        val favoriteComics = uiState.favoriteComics // Assuming ViewModel populates this
        if (favoriteComics.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.favorite_comics_section_title))
                HorizontalComicsRow(comics = favoriteComics, onIntent = onIntent)
                Spacer(modifier = Modifier.height(16.dp.scaled())) // Reduced spacer
            }
        }

        // All Comics / Results Section
        // The ViewModel should provide the primary list of comics to display (allComics)
        // This list should already be filtered by category if a category is selected.
        // Search filtering is applied on top of that.
        val comicsToDisplay = if (uiState.searchQuery.isBlank()) {
            uiState.allComics // Already category-filtered by ViewModel
        } else {
            uiState.allComics.filter { comic ->
                comic.title?.contains(uiState.searchQuery, ignoreCase = true) == true
            }
        }

        if (comicsToDisplay.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.all_comics_section_title))
            }
            items(comicsToDisplay, key = { it.filePath.toString() }) { comic -> // Use filePath as key
                ComicListItem(comic = comic, onIntent = onIntent)
            }
        }
    }
}

@Composable
fun ComicCoverItem(
    comic: ComicEntity, // Uses ComicEntity
    onIntent: ((HomeIntent) -> Unit)?
) {
    Column(
        modifier = Modifier
            .width(120.dp.scaled()) // Fixed width for consistent sizing
            .clickable { onIntent?.invoke(HomeIntent.ComicClicked(comic)) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Placeholder for a comic cover image - replace with actual image loading
        Box(
            modifier = Modifier
                .height(160.dp.scaled())
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp.scaled())),
            contentAlignment = Alignment.Center
        ) {
            //Icon(Icons.Default.Book, contentDescription = "Comic Cover", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            // It's better to use an AsyncImage or similar here
            Text("Cover", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp.scaled()))
        Text(
            text = comic.title ?: stringResource(R.string.unknown_title),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ComicListItem(
    comic: ComicEntity, // Uses ComicEntity
    onIntent: ((HomeIntent) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onIntent?.invoke(HomeIntent.ComicClicked(comic)) }
            .padding(horizontal = 16.dp.scaled(), vertical = 8.dp.scaled()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder for a smaller comic cover image or icon
        Box(
            modifier = Modifier
                .size(60.dp.scaled())
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp.scaled())),
            contentAlignment = Alignment.Center
        ) {
            //Icon(Icons.Default.Book, contentDescription = "Comic Thumbnail", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Img", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(16.dp.scaled()))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comic.title ?: stringResource(R.string.unknown_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Add more details like author, series, etc., if available in ComicEntity
            // Text(text = "Author Name", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Example action: Favorite button
        IconButton(onClick = { /* Handle favorite toggle */ }) {
            Icon(
                Icons.Default.Favorite, // Use FavoriteBorder for unselected state
                contentDescription = stringResource(R.string.favorite_button_description),
                tint = if (comic.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
