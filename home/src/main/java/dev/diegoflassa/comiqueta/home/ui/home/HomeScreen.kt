@file:OptIn(ExperimentalMaterial3Api::class)

package dev.diegoflassa.comiqueta.home.ui.home

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import dev.diegoflassa.comiqueta.home.R
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.navigation.Screen
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import coil.compose.AsyncImage
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent

private const val COMIC_COVERT_ASPECT_RATIO = 2f / 3f

private const val tag = "HomeScreen"

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        treeUri?.let {
            try {
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
                Log.d("HomeScreen", "Persistable URI permission taken by UI for: $it")
                viewModel.processIntent(HomeIntent.FolderSelected(it))
            } catch (e: SecurityException) {
                Log.e("HomeScreen", "Failed to take persistable URI permission by UI for $it", e)
                Toast.makeText(
                    context, "Failed to get long-term access to the folder.", Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        viewModel.processIntent(HomeIntent.FolderPermissionResult(isGranted))
    }
    LaunchedEffect(key1 = viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HomeEffect.ShowToast -> Toast.makeText(
                    context, effect.message, Toast.LENGTH_SHORT
                ).show()

                is HomeEffect.LaunchFolderPicker -> folderPickerLauncher.launch(null)
                is HomeEffect.RequestStoragePermission -> runtimePermissionLauncher.launch(effect.permission)
            }
        }
    }
    val homeUIState = viewModel.uiState.collectAsState().value
    HomeScreenContent(
        modifier = modifier, navigationViewModel = navigationViewModel, uiState = homeUIState
    ) {
        viewModel.processIntent(it)
    }

}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel? = null,
    uiState: HomeUIState = HomeUIState(),
    onIntent: ((HomeIntent) -> Unit)? = null,
) {
    val isEmpty = uiState.allComics.isEmpty() && uiState.isLoading.not()
    val fabDiameter = 48.dp.scaled()
    val bottomBarHeight = 50.dp.scaled()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Comiqueta", fontWeight = FontWeight.Bold) },
                actions = {
                    if (isEmpty.not()) {
                        IconButton(onClick = { navigationViewModel?.navigateTo(Screen.Settings) }) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ComiquetaTheme.colorScheme.surface)
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .background(ComiquetaTheme.colorScheme.surfaceContainer)
            ) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (isEmpty) {
                    EmptyStateContent()
                } else {
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
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }
    }
}

@Composable
fun EmptyStateContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp.scaled()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "No comics found yet.",
            fontSize = 18.sp.scaled(),
            textAlign = TextAlign.Center,
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 8.dp.scaled())
        )
        Text(
            "Click the '+' button to select a folder with your comics.",
            fontSize = 16.sp.scaled(),
            textAlign = TextAlign.Center,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp.scaled())
        )
        Icon(
            Icons.Filled.Add,
            "Add book icon",
            modifier = Modifier
                .size(128.dp.scaled())
                .padding(bottom = 16.dp.scaled()),
            tint = Color.Gray
        )
    }
}

@Composable
fun ComicsContent(
    modifier: Modifier = Modifier,
    uiState: HomeUIState,
    onIntent: ((HomeIntent) -> Unit)? = null,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF222222)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Search Bar
            OutlinedTextField(
                value = "",
                onValueChange = { /* TODO: Handle search input */ },
                placeholder = { Text("Search", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = Color.Gray
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF444444)),
                colors = OutlinedTextFieldDefaults.colors().copy(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White
                )
            )
        }
        val categories = uiState.categories
        item {
            // Categories
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                categories.forEach { category ->
                    Text(
                        text = category.name ?: "",
                        color = if (category.name == "All") MaterialTheme.colorScheme.primary else Color.Gray,
                        fontWeight = if (category.name == "All") FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
        val latestComics = uiState.latestComics
        item {
            // Latest Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = "Latest",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(latestComics) { comic ->
                        ComicCoverItem(comic = comic)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
        val favoriteComics = uiState.favoriteComics
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Favorites",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(favoriteComics) { comic ->
                        ComicCoverItem(comic = comic)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Favorites",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row {
                        IconButton(onClick = { /* TODO: Handle list view */ }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "List View",
                                tint = Color.Gray
                            )
                        }
                        IconButton(onClick = { /* TODO: Handle grid view */ }) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = "Grid View",
                                tint = Color.Gray
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        val continueReadingComics = uiState.continueReadingComics
        items(continueReadingComics) { comic ->
            ContinueReadingItem(comic = comic)
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
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(bottomAppBarIconSize),
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) ComiquetaTheme.colorScheme.primary else Color.Gray,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = ComiquetaTheme.typography.bottomAppBar.scaled(),
            color = if (isSelected) ComiquetaTheme.colorScheme.primary else Color.Gray,
            maxLines = 2
        )
    }
}

@Preview(
    name = "${tag}Empty:360x640",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    showSystemUi = true
)
@Preview(
    name = "${tag}Empty:540x1260",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260,
    showSystemUi = true
)
@Preview(
    name = "${tag}Empty:540x1260 Dark",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "${tag}Empty:540x1260",
    locale = "en-rUS",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260
)
@Preview(
    name = "${tag}Empty:540x1260",
    locale = "de",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260
)
@Composable
fun HomeScreenPreviewEmptyMVI() {
    ComiquetaThemeContent {
        val homeUIState = HomeUIState()
        HomeScreenContent(uiState = homeUIState)
    }
}

@Preview(
    name = "$tag:360x640",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Preview(
    name = "$tag:540x1260",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260
)
@Preview(
    name = "$tag:540x1260 Dark",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "$tag:540x1260",
    locale = "en-rUS",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260
)
@Preview(
    name = "$tag:540x1260",
    locale = "de",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260
)
@Composable
fun HomeScreenPreviewContentMVI() {
    ComiquetaThemeContent {
        val sampleComics = listOf(
            ComicEntity(
                filePath = "file:///preview/1".toUri(),
                title = "The Hero's Journey",
                coverPath = "https://placehold.co/150x220/E0E0E0/333333?text=Comic+1".toUri(),
                isFavorite = true,
                genre = "Action",
                isNew = true,
                hasBeenRead = false
            ), ComicEntity(
                filePath = "file:///preview/2".toUri(),
                title = "Cosmic Saga",
                coverPath = "https://placehold.co/150x220/D0D0D0/333333?text=Comic+2".toUri(),
                isFavorite = false,
                genre = "Sci-Fi",
                isNew = true,
                hasBeenRead = false
            )
        )
        val homeUIState = HomeUIState(
            allComics = sampleComics,
            categories = categoriesMock,
            latestComics = latestComicsMock,
            favoriteComics = favoriteComicsMock,
            //unreadComics = unreadComicsMock,
            continueReadingComics = continueReadingComicsMock
        )
        HomeScreenContent(uiState = homeUIState)
    }
}

// Sample Data updated to use ComicEntity
val latestComicsMock = listOf(
    ComicEntity(
        filePath = "file:///comic1.cbr".toUri(),
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+1".toUri(),
        title = "Titulo",
        genre = "Action",
        isNew = true
    ),
    ComicEntity(
        filePath = "file:///comic2.cbr".toUri(),
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+2".toUri(),
        title = "Titulo",
        genre = "Action",
        isNew = true
    ),
    ComicEntity(
        filePath = "file:///comic3.cbr".toUri(),
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+3".toUri(),
        title = "Titulo",
        genre = "Horror",
        isNew = true
    ),
    ComicEntity(
        filePath = "file:///comic4.cbr".toUri(),
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+4".toUri(),
        title = "Titulo",
        genre = "Fantasy",
        isNew = true
    ),
    ComicEntity(
        filePath = "file:///comic5.cbr".toUri(),
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+5".toUri(),
        title = "Titulo",
        genre = "Action",
        isNew = true
    )
)

val favoriteComicsMock = listOf(
    ComicEntity(
        filePath = "file:///comic6.cbr".toUri(),
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+6".toUri(),
        title = "Titulo",
        genre = "Action",
        isFavorite = true
    ),
    ComicEntity(
        filePath = "file:///comic7.cbr".toUri(),
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+7".toUri(),
        title = "Titulo",
        genre = "Action",
        isFavorite = true
    ),
    ComicEntity(
        filePath = "file:///comic8.cbr".toUri(),
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+8".toUri(),
        title = "Titulo",
        genre = "Horror",
        isFavorite = true
    ),
    ComicEntity(
        filePath = "file:///comic9.cbr".toUri(),
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+9".toUri(),
        title = "Titulo",
        genre = "Fantasy",
        isFavorite = true
    ),
    ComicEntity(
        filePath = "file:///comic10.cbr".toUri(),
        coverPath = "https://placehold.co/100x150/cccccc/333333?text=Comic+10".toUri(),
        title = "Titulo",
        genre = "Action",
        isFavorite = true
    )
)

val categoriesMock = listOf(
    CategoryEntity(1, "All"),
    CategoryEntity(2, "Action"),
    CategoryEntity(3, "Horror"),
    CategoryEntity(4, "Fantasy")
)

val continueReadingComicsMock = listOf(
    ComicEntity(
        filePath = "file:///comic11.cbr".toUri(),
        coverPath = "https://placehold.co/60x90/cccccc/333333?text=Comic+11".toUri(),
        title = "Titulo",
        lastPage = 23,
        hasBeenRead = true
    ),
    ComicEntity(
        filePath = "file:///comic12.cbr".toUri(),
        coverPath = "https://placehold.co/60x90/cccccc/333333?text=Comic+12".toUri(),
        title = "Titulo",
        lastPage = 23,
        hasBeenRead = true
    )
)

@Composable
fun ComicCoverItem(comic: ComicEntity) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val imageModifier = Modifier
            .size(150.dp)
            .aspectRatio(COMIC_COVERT_ASPECT_RATIO)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.DarkGray)

        if (comic.coverPath != null && comic.coverPath != Uri.EMPTY) {
            AsyncImage(
                model = comic.coverPath,
                contentDescription = comic.title,
                modifier = imageModifier,
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(Icons.AutoMirrored.Filled.ViewList),
                error = rememberVectorPainter(Icons.AutoMirrored.Filled.ViewList)
            )
        } else {
            Box(
                modifier = imageModifier,
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ViewList,
                    contentDescription = "No Cover Available",
                    tint = Color.Gray,
                    modifier = Modifier.size(50.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = comic.title ?: "No Title", color = Color.White, fontSize = 14.sp)
        Text(text = "Chapter 1", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun ContinueReadingItem(comic: ComicEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFF333333), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val imageModifier = Modifier
                .size(60.dp, 90.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.DarkGray)

            if (comic.coverPath != null && comic.coverPath != Uri.EMPTY) {
                AsyncImage(
                    model = comic.coverPath,
                    contentDescription = comic.title,
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop,
                    placeholder = rememberVectorPainter(Icons.AutoMirrored.Filled.ViewList),
                    error = rememberVectorPainter(Icons.AutoMirrored.Filled.ViewList)
                )
            } else {
                Box(
                    modifier = imageModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = "No Cover Available",
                        tint = Color.Gray,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = comic.title ?: "No Title",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Chapter X - Page ${comic.lastPage}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = "Go to comic",
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}
