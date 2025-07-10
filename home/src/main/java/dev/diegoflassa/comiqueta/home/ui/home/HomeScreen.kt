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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp.scaled())
            .verticalScroll(rememberScrollState()) // Consider if LazyColumn is better for overall screen
    ) {
        Spacer(modifier = Modifier.height(16.dp.scaled()))
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { onIntent?.invoke(HomeIntent.SearchComics(it)) },
            label = { Text("Search Comics") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp.scaled()),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ComiquetaTheme.colorScheme.primary,
                unfocusedBorderColor = Color.LightGray
            )
        )
        Spacer(modifier = Modifier.height(16.dp.scaled()))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp.scaled())
        ) {
            items(uiState.categories) { category ->
                Button(
                    onClick = { onIntent?.invoke(HomeIntent.SelectCategory(category)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.selectedCategory == category) ComiquetaTheme.colorScheme.primary else Color(
                            0xFFE0E0E0
                        ),
                        contentColor = if (uiState.selectedCategory == category) Color.White else Color.DarkGray
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp.scaled(),
                        vertical = 8.dp.scaled()
                    )
                ) {
                    Text(text = category, fontSize = 12.sp.scaled())
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp.scaled())) // Reduced from 32dp.scaled()

        if (uiState.latestComics.isNotEmpty()) {
            Text(
                "Latest",
                fontSize = 18.sp.scaled(),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp.scaled())
            ) // Reduced bottom from 16dp.scaled()
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp.scaled()),
                contentPadding = PaddingValues(horizontal = 4.dp.scaled())
            ) {
                items(uiState.latestComics) { comic -> ComicCoverCard(comic = comic) }
            }
            Spacer(modifier = Modifier.height(24.dp.scaled()))
        }

        if (uiState.favoriteComics.isNotEmpty()) {
            Text(
                "Favorites",
                fontSize = 18.sp.scaled(),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp.scaled())
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp.scaled()),
                contentPadding = PaddingValues(horizontal = 4.dp.scaled())
            ) {
                items(uiState.favoriteComics) { comic -> ComicCoverCard(comic = comic) }
            }
            Spacer(modifier = Modifier.height(24.dp.scaled()))
        }

        Text("All Comics", fontSize = 18.sp.scaled(), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp.scaled()))

        // Filtered list for "All Comics" section
        val filteredComics =
            remember(uiState.allComics, uiState.searchQuery, uiState.selectedCategory) {
                uiState.allComics.filter { comic ->
                    (uiState.searchQuery.isEmpty() || comic.title?.contains(
                        uiState.searchQuery, ignoreCase = true
                    ) == true) && (uiState.selectedCategory == "All" || comic.genre == uiState.selectedCategory)
                }
            }

        if (filteredComics.isEmpty() && (uiState.searchQuery.isNotEmpty() || uiState.selectedCategory != "All")) {
            Text(
                "No comics match your current filter.",
                modifier = Modifier
                    .padding(vertical = 16.dp.scaled())
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        } else {
            // This LazyColumn should not be nested in a verticalScroll if it's meant to scroll independently.
            // For now, giving it a fixed height to prevent crashes in a verticalScroll.
            // A better approach is to make the whole screen a LazyColumn if possible.
            Column(modifier = Modifier.fillMaxWidth()) { // No fixed height, let it expand in verticalScroll
                filteredComics.forEach { comic ->
                    ComicListItem(comic = comic)
                    Spacer(modifier = Modifier.height(8.dp.scaled())) // Spacing between items
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp.scaled())) // Footer spacing
    }
}

@Composable
fun ComicCoverCard(comic: ComicEntity, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(200.dp.scaled())
            .aspectRatio(COMIC_COVERT_ASPECT_RATIO)
            .padding(4.dp.scaled()),
        shape = RoundedCornerShape(8.dp.scaled()),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp.scaled())
    ) {
        Image(
            painter = rememberAsyncImagePainter(comic.coverPath),
            contentDescription = comic.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp.scaled())
                .aspectRatio(COMIC_COVERT_ASPECT_RATIO)
                .clip(RoundedCornerShape(topStart = 8.dp.scaled(), topEnd = 8.dp.scaled()))
        )
    }
}

@Composable
fun ComicListItem(comic: ComicEntity, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp.scaled(), vertical = 4.dp.scaled()),
        shape = RoundedCornerShape(8.dp.scaled()),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp.scaled())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp.scaled()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = rememberAsyncImagePainter(comic.coverPath),
                    contentDescription = comic.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp.scaled())
                        .clip(RoundedCornerShape(8.dp.scaled()))
                        .background(Color.LightGray)
                )
                Spacer(modifier = Modifier.width(16.dp.scaled()))
                Column {
                    Text(
                        text = comic.title ?: "No Title",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp.scaled()
                    )
                    Text(
                        text = comic.genre ?: "No Genre",
                        fontSize = 14.sp.scaled(),
                        color = Color.Gray
                    )
                }
            }
            Icon(
                imageVector = if (comic.isFavorite) Icons.Filled.Favorite else Icons.Default.Star,
                contentDescription = if (comic.isFavorite) "Favorite" else "Not Favorite",
                tint = if (comic.isFavorite) ComiquetaTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(20.dp.scaled())
            )
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
        val homeUIState = HomeUIState(allComics = sampleComics)
        HomeScreenContent(uiState = homeUIState)
    }
}
