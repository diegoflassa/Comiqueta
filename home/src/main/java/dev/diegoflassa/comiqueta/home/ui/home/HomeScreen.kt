package dev.diegoflassa.comiqueta.home.ui.home

import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.PrimaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
import kotlinx.coroutines.flow.collectLatest
import androidx.core.net.toUri
import dev.diegoflassa.comiqueta.core.data.preferences.UserPreferencesKeys
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.theme.bottomAppBarSelectedIcon
import dev.diegoflassa.comiqueta.core.theme.bottomAppBarUnselectedIcon
import dev.diegoflassa.comiqueta.core.theme.getOutlinedTextFieldDefaultsColors
import dev.diegoflassa.comiqueta.core.theme.settingIconTint
import dev.diegoflassa.comiqueta.core.theme.tabSelectedText
import dev.diegoflassa.comiqueta.core.theme.tabUnselectedText

// Assuming these are defined in your project:
// - HomeViewModel, HomeIntent, HomeEffect, HomeUIState
// - ComiquetaTheme, ComiquetaThemeContent, ComiquetaTheme.dimen, ComiquetaTheme.shapes
// - dev.diegoflassa.comiqueta.core.ui.extensions.scaled
// - R.string.home, R.string.catalog, R.string.bookmarks, R.string.favorites, etc.
// - R.drawable.ic_placeholder_comic (or similar placeholder drawable)

private const val tag = "HomeScreen"

private const val COMIC_COVER_ASPECT_RATIO = 2f / 3f

@Composable
fun HomeScreen(
    navigationViewModel: NavigationViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            uri?.let {
                homeViewModel.processIntent(HomeIntent.FolderSelected(it))
            }
        }
    )

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            // You might need a way to know which permission was requested if you handle multiple
            // For now, assuming the ViewModel can infer or has stored this.
            homeViewModel.processIntent(HomeIntent.FolderPermissionResult(isGranted))
        }
    )

    LaunchedEffect(Unit) {
        homeViewModel.processIntent(HomeIntent.LoadInitialData)
    }

    LaunchedEffect(key1 = homeViewModel.effect) {
        homeViewModel.effect.collectLatest { effect ->
            when (effect) {
                is HomeEffect.LaunchFolderPicker -> {
                    folderPickerLauncher.launch(null) // Uri can be null for initial directory
                }

                is HomeEffect.NavigateToComicDetail -> {
                    // Assuming your Screen.Viewer can take the ComicEntity's filePath (Uri)
                    // You might need to encode the Uri if passing as a string argument
                    // For example: val encodedPath = Uri.encode(effect.comic.filePath.toString())
                    // navController.navigate(Screen.Viewer(encodedPath))
                    // If Screen.Viewer is set up to take Uri directly via Kotlinx Serialization, it's cleaner:
                    navigationViewModel.navigateTo(Screen.Viewer(effect.comicPath))
                }

                is HomeEffect.RequestStoragePermission -> {
                    requestPermissionLauncher.launch(effect.permission)
                }

                is HomeEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
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
    val isEmpty =
        uiState.allComics.isEmpty() && uiState.searchQuery.isBlank() && uiState.selectedCategory == null && !uiState.isLoading

    val topSystemBarInsetDp = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    Scaffold(
        modifier = modifier.background(ComiquetaTheme.colorScheme.background),
        topBar = {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topSystemBarInsetDp)
            )
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = ComiquetaTheme.dimen.appBarHorizontalPadding),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            "Comiqueta",
                            style = ComiquetaTheme.typography.comiquetaTitleText.scaled()
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = ComiquetaTheme.dimen.appBarHorizontalPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { navigationViewModel?.navigateTo(Screen.Settings) }) {
                            Icon(
                                modifier = Modifier.size(ComiquetaTheme.dimen.iconSettings.scaled()),
                                imageVector = Icons.Outlined.Settings,
                                tint = ComiquetaTheme.colorScheme.settingIconTint,
                                contentDescription = "Settings"
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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

            BottomAppBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(bottomBarHeight)
                    .graphicsLayer(
                        shape = ComiquetaTheme.shapes.bottomBarShape,
                        clip = true
                    ),
                tonalElevation = 4.dp.scaled(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp, bottom = 6.dp, start = 16.dp, end = 16.dp),
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
            }

            ExtendedFloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(1F)
                    .size(fabDiameter)
                    .offset(y = (-17).dp.scaled()),
                onClick = { onIntent?.invoke(HomeIntent.AddFolderClicked) },
                shape = CircleShape,
            ) {
                Icon(
                    modifier = Modifier.size(ComiquetaTheme.dimen.fabIconSize.scaled()),
                    imageVector = Icons.Filled.Add,
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
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp.scaled()))
            Text(
                text = stringResource(R.string.empty_state_message),
                textAlign = TextAlign.Center,
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
    if (categories.isEmpty()) {
        return
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val textWidths = remember { mutableStateMapOf<Int, Dp>() }
    val density = LocalDensity.current

    TabRow(
        modifier = Modifier.padding(horizontal = ComiquetaTheme.dimen.tabHorizontalPadding),
        containerColor = ComiquetaTheme.colorScheme.background,
        selectedTabIndex = selectedTabIndex,
        indicator = { tabPositions ->
            selectedTabIndex = categories.indexOf(selectedCategory).let {
                if (it == -1) 0 else it
            }
            if (tabPositions.isNotEmpty() && selectedTabIndex >= 0 && selectedTabIndex < tabPositions.size) {
                val currentTabPosition = tabPositions[selectedTabIndex]
                val currentTextWidth = textWidths[selectedTabIndex]?.scaled() ?: 0.dp
                TimberLogger.logI(tag, "Got size[$selectedTabIndex]: $currentTextWidth")

                if (currentTextWidth > 0.dp) {
                    val mainIndicatorHeight = 2.dp.scaled()
                    val mainIndicatorColor = ComiquetaTheme.colorScheme.tabSelectedText

                    PrimaryIndicator(
                        modifier = Modifier
                            .tabIndicatorOffset(currentTabPosition)
                            .width(currentTextWidth.scaled())
                            .wrapContentSize(Alignment.BottomStart)
                            .padding(start = 16.dp),
                        height = mainIndicatorHeight,
                        color = mainIndicatorColor
                    )
                }
            } else {
                Box(Modifier)
            }
        }
    ) {
        categories.forEachIndexed { index, category ->
            val categoryText = if (category.name.equals(
                    UserPreferencesKeys.DEFAULT_CATEGORY_ALL,
                    ignoreCase = true
                )
            ) {
                stringResource(id = dev.diegoflassa.comiqueta.core.R.string.all)
            } else {
                category.name
            }

            Tab(
                //modifier = Modifier.background(ComiquetaTheme.colorScheme.transparent),
                selected = selectedTabIndex == index,
                onClick = { onCategoryClicked(category) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier
                                .wrapContentWidth()
                                .onSizeChanged { intSize ->
                                    TimberLogger.logI(
                                        tag,
                                        "Setted size[$index]: ${intSize.width}"
                                    )
                                    textWidths[index] =
                                        with(density) { intSize.width.toDp() }
                                }
                                .align(Alignment.CenterStart),
                            text = categoryText,
                            style = ComiquetaTheme.typography.tabText,
                            color = if (selectedTabIndex == index) ComiquetaTheme.colorScheme.tabSelectedText else ComiquetaTheme.colorScheme.tabUnselectedText,
                            textAlign = TextAlign.Start,
                            maxLines = 1
                        )
                    }
                }
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
                top = 16.dp.scaled(),
                bottom = 8.dp.scaled()
            ),
        text = title,
        textAlign = TextAlign.Start,
        fontSize = 18.sp,
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
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp.scaled())
    ) {
        items(comics, key = { it.filePath.toString() }) { comic ->
            ComicCoverItem(comic = comic, onIntent = onIntent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicsContent(
    modifier: Modifier = Modifier,
    uiState: HomeUIState,
    onIntent: ((HomeIntent) -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Search Bar
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onIntent?.invoke(HomeIntent.SearchComics(it)) },
                placeholder = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = ComiquetaTheme.typography.searchText.scaled()
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(ComiquetaTheme.dimen.iconSize.scaled()),
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.search_icon_description),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                    .clip(RoundedCornerShape(8.dp.scaled())),
                colors = getOutlinedTextFieldDefaultsColors(),
                singleLine = true
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
                SectionHeader(
                    title = if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) {
                        stringResource(R.string.results_section_title)
                    } else {
                        stringResource(R.string.all_comics_section_title)
                    }
                )
            }
            // Add Grid or List view based on uiState.viewMode (if implemented)
            // For now, using ComicListItem for all.
            items(comicsToDisplay, key = { it.filePath.toString() }) { comic ->
                ComicListItem(comic = comic, onIntent = onIntent)
                Spacer(modifier = Modifier.height(8.dp.scaled()))
            }
        } else if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) {
            // Show "no results" only if a search or category filter is active and yields no results
            item {
                Text(
                    text = stringResource(R.string.no_comics_found_for_search),
                    modifier = Modifier
                        .padding(16.dp.scaled())
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    //color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Spacer for bottom content not to be hidden by FAB/BottomBar
        item {
            Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.bottomBarHeight.scaled() + ComiquetaTheme.dimen.fabDiameter.scaled() / 2))
        }
    }
}


@Composable
fun ComicCoverItem(
    modifier: Modifier = Modifier,
    comic: ComicEntity, // Uses ComicEntity
    onIntent: ((HomeIntent) -> Unit)?
) {
    Card(
        modifier = modifier
            .height(150.dp.scaled())
            .aspectRatio(COMIC_COVER_ASPECT_RATIO)
            .clickable { onIntent?.invoke(HomeIntent.ComicSelected(comic)) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp.scaled()),
        shape = RoundedCornerShape(8.dp.scaled())
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = comic.coverPath
                    ?: comic.filePath.takeIf { it != Uri.EMPTY },
                error = painterResource(id = R.drawable.ic_placeholder_comic),
                placeholder = painterResource(id = R.drawable.ic_placeholder_comic)
            ),
            contentDescription = comic.title
                ?: stringResource(id = R.string.comic_cover_image_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ComicListItem(
    comic: ComicEntity,
    onIntent: ((HomeIntent) -> Unit)?
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
                    model = comic.coverPath ?: comic.filePath.takeIf { it != Uri.EMPTY },
                    error = painterResource(id = R.drawable.ic_placeholder_comic),
                    placeholder = painterResource(id = R.drawable.ic_placeholder_comic)
                ),
                contentDescription = comic.title
                    ?: stringResource(id = R.string.comic_cover_image_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(90.dp.scaled())
                    .aspectRatio(COMIC_COVER_ASPECT_RATIO)
                    .clip(RoundedCornerShape(4.dp.scaled()))
            )
            Spacer(modifier = Modifier.width(16.dp.scaled()))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comic.title ?: stringResource(id = R.string.unknown_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                comic.author?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                    )
                }
            }
            if (comic.isFavorite) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.favorite_icon_description),
                    modifier = Modifier.padding(start = 8.dp.scaled())
                )
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(ComiquetaTheme.dimen.bottomAppBarIconSize.scaled()),
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) ComiquetaTheme.colorScheme.bottomAppBarSelectedIcon else ComiquetaTheme.colorScheme.bottomAppBarUnselectedIcon
        )
        Text(
            text = label,
            color = if (isSelected) ComiquetaTheme.colorScheme.tabSelectedText else ComiquetaTheme.colorScheme.tabUnselectedText,
            style = ComiquetaTheme.typography.bottomAppBarText
        )
    }
}

@Preview(
    showBackground = true,
    name = "Light Mode - 1080x2560px",
    device = "spec:width=1080px,height=2560px,dpi=440"
)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark Mode - 1080x2560px",
    device = "spec:width=1080px,height=2560px,dpi=440"
)
@Composable
fun HomeScreenContentLoadingPreview() {
    ComiquetaThemeContent {
        HomeScreenContent(
            uiState = HomeUIState(isLoading = true),
            onIntent = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "Light Mode - Empty - 1080x2560px",
    device = "spec:width=1080px,height=2560px,dpi=440"
)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark Mode - Empty - 1080x2560px",
    device = "spec:width=1080px,height=2560px,dpi=440"
)
@Composable
fun HomeScreenContentEmptyPreview() {
    ComiquetaThemeContent {
        HomeScreenContent(
            uiState = HomeUIState(
                isLoading = false,
                allComics = emptyList(),
                latestComics = emptyList(),
                favoriteComics = emptyList(),
                categories = listOf(CategoryEntity(id = 1L, name = "All")),
            ),
            onIntent = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "Light Mode - With Comics - 1080x2560px",
    device = "spec:width=1080px,height=2560px,dpi=440"
)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark Mode - With Comics - 1080x2560px",
    device = "spec:width=1080px,height=2560px,dpi=440"
)
@Composable
fun HomeScreenContentWithComicsPreview() {
    val sampleComics = listOf(
        ComicEntity(
            filePath = "file:///comic1".toUri(),
            title = "Comic Adventure 1",
            // author = "Author A", // ComicEntity doesn't have author, removed for consistency
            isFavorite = true,
            isNew = true,
            coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+1".toUri() // Using a placeholder URL
        ),
        ComicEntity(
            filePath = "file:///comic2".toUri(),
            title = "Mystery of the Void",
            // author = "Author B",
            isNew = true,
            coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+2".toUri()
        ),
        ComicEntity(
            filePath = "file:///comic3".toUri(),
            title = "Chronicles of Code",
            // author = "Author C",
            isFavorite = false,
            coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+3".toUri()
        ),
        ComicEntity(
            filePath = "file:///comic4".toUri(),
            title = "Epic Tales",
            // author = "Author D",
            isFavorite = true,
            coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+4".toUri()
        )
    )
    val sampleCategories = listOf(
        CategoryEntity(id = 1, name = "All"), // Assuming CategoryEntity structure
        CategoryEntity(id = 2, name = "Sci-Fi"),
        CategoryEntity(id = 3, name = "Fantasy")
    )
    ComiquetaThemeContent {
        HomeScreenContent(
            uiState = HomeUIState(
                isLoading = false,
                allComics = sampleComics,
                latestComics = sampleComics.filter { it.isNew },
                favoriteComics = sampleComics.filter { it.isFavorite },
                categories = sampleCategories,
                selectedCategory = sampleCategories.first()
            ),
            onIntent = {}
        )
    }
}
