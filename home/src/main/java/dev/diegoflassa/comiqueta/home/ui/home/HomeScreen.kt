package dev.diegoflassa.comiqueta.home.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.repository.ComicsFolderRepository
import dev.diegoflassa.comiqueta.core.data.repository.ComicsRepository
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.navigation.Screen
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsDao
import androidx.compose.runtime.collectAsState


@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        treeUri?.let {
            try {
                // IMPORTANT: UI layer takes persistable URI permission
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
                Log.d("HomeScreen", "Persistable URI permission taken by UI for: $it")
                viewModel.processIntent(HomeIntent.FolderSelected(it))
            } catch (e: SecurityException) {
                Log.e("HomeScreen", "Failed to take persistable URI permission by UI for $it", e)
                Toast.makeText(
                    context,
                    "Failed to get long-term access to the folder.",
                    Toast.LENGTH_LONG
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
                    context,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()

                is HomeEffect.LaunchFolderPicker -> folderPickerLauncher.launch(null) // Launch without specific URI for tree selection
                is HomeEffect.RequestStoragePermission -> runtimePermissionLauncher.launch(effect.permission)
            }
        }
    }

    ComiquetaScreenContent( // Renamed to avoid conflict, and to pass state/intents
        modifier = modifier,
        uiState = uiState,
        onNavigate = { screen -> navigationViewModel?.navigateTo(screen = screen) },
        onAddFolderClicked = { viewModel.processIntent(HomeIntent.AddFolderClicked) },
        onSearchQueryChanged = { query -> viewModel.processIntent(HomeIntent.SearchComics(query)) },
        onCategorySelected = { category ->
            viewModel.processIntent(
                HomeIntent.SelectCategory(
                    category
                )
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComiquetaScreenContent( // This is the main UI structure, previously ComiquetaScreen
    modifier: Modifier = Modifier,
    uiState: HomeUIState,
    onNavigate: (Screen) -> Unit,
    onAddFolderClicked: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit
) {
    val isEmpty = uiState.allComics.isEmpty() && !uiState.isLoading // Consider loading state
    val fabDiameter = 56.dp
    val bottomBarHeight = 64.dp

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Comiqueta", fontWeight = FontWeight.Bold) },
                actions = {
                    if (!isEmpty) { // Show settings only if there's content, or always if preferred
                        IconButton(onClick = { onNavigate(Screen.Settings) }) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddFolderClicked, // Changed to onAddFolderClicked
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(fabDiameter)
                    .shadow(10.dp, CircleShape)
            ) {
                Icon(
                    modifier = Modifier.size(fabDiameter * 0.6f),
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Folder", // Changed description
                    tint = Color.White,
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bottomBarHeight)
                    .graphicsLayer(
                        shape = BottomBarArcShape(fabDiameter, 24.dp, 8.dp),
                        clip = true
                    ),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = BottomAppBarDefaults.ContainerElevation
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(
                        Icons.Default.Home,
                        "Home",
                        true,
                        { onNavigate(Screen.Home) },
                        Modifier.weight(1f)
                    )
                    BottomNavItem(
                        Icons.Default.Star,
                        "Catalog",
                        false,
                        { onNavigate(Screen.Catalog) },
                        Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(fabDiameter + 16.dp))
                    BottomNavItem(
                        Icons.AutoMirrored.Filled.List,
                        "Bookmark",
                        false,
                        { onNavigate(Screen.Bookmark) },
                        Modifier.weight(1f)
                    )
                    BottomNavItem(
                        Icons.Default.Favorite,
                        "Favorites",
                        false,
                        { onNavigate(Screen.Favorites) },
                        Modifier.weight(1f)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF0F0F0))
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (isEmpty) {
                EmptyStateContent() // Extracted empty state
            } else {
                ComicsContent( // Extracted comics display
                    uiState = uiState,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onCategorySelected = onCategorySelected
                )
            }
        }
    }
}

@Composable
fun EmptyStateContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "No comics found yet.",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "Click the '+' button to select a folder with your comics.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Icon(
            Icons.Filled.Add, "Add book icon", modifier = Modifier
                .size(128.dp)
                .padding(bottom = 16.dp), tint = Color.Gray
        )
    }
}

@Composable
fun ComicsContent(
    uiState: HomeUIState,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()) // Consider if LazyColumn is better for overall screen
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            label = { Text("Search Comics") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.LightGray
            )
        )
        Spacer(modifier = Modifier.height(16.dp)) // Reduced from 24dp
        LazyRow( // For categories
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp) // Spacing between buttons
        ) {
            items(uiState.categories) { category ->
                Button(
                    onClick = { onCategorySelected(category) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.selectedCategory == category) MaterialTheme.colorScheme.primary else Color(
                            0xFFE0E0E0
                        ),
                        contentColor = if (uiState.selectedCategory == category) Color.White else Color.DarkGray
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(text = category, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp)) // Reduced from 32dp

        if (uiState.latestComics.isNotEmpty()) {
            Text(
                "Latest",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            ) // Reduced bottom from 16dp
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(uiState.latestComics) { comic -> ComicCoverCard(comic = comic) }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (uiState.favoriteComics.isNotEmpty()) {
            Text(
                "Favorites",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(uiState.favoriteComics) { comic -> ComicCoverCard(comic = comic) }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text("All Comics", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // Filtered list for "All Comics" section
        val filteredComics =
            remember(uiState.allComics, uiState.searchQuery, uiState.selectedCategory) {
                uiState.allComics.filter { comic ->
                    (uiState.searchQuery.isEmpty() || comic.title?.contains(
                        uiState.searchQuery,
                        ignoreCase = true
                    ) == true) &&
                            (uiState.selectedCategory == "All" || comic.genre == uiState.selectedCategory)
                }
            }

        if (filteredComics.isEmpty() && (uiState.searchQuery.isNotEmpty() || uiState.selectedCategory != "All")) {
            Text(
                "No comics match your current filter.",
                modifier = Modifier
                    .padding(vertical = 16.dp)
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
                    Spacer(modifier = Modifier.height(8.dp)) // Spacing between items
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp)) // Footer spacing
    }
}


// ComicCoverCard, ComicListItem, BottomNavItem, BottomBarArcShape remain the same
// ... (paste your existing ComicCoverCard, ComicListItem, BottomNavItem, BottomBarArcShape here)
@Composable
fun ComicCoverCard(comic: ComicEntity, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .width(120.dp)
            .height(200.dp)
            .padding(4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Image(
                painter = rememberAsyncImagePainter(comic.coverPath),
                contentDescription = comic.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = comic.title ?: "No Title",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ComicListItem(comic: ComicEntity, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = rememberAsyncImagePainter(comic.coverPath),
                    contentDescription = comic.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = comic.title ?: "No Title",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = comic.genre ?: "No Genre",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            Icon(
                imageVector = if (comic.isFavorite) Icons.Filled.Favorite else Icons.Default.Star,
                contentDescription = if (comic.isFavorite) "Favorite" else "Not Favorite",
                tint = if (comic.isFavorite) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun RowScope.BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
        )
    }
}

class BottomBarArcShape(
    private val fabDiameter: Dp,
    private val fabCradleRoundedCornerRadius: Dp = 16.dp,
    private val fabMargin: Dp = 8.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(Path().apply {
            val fabDiameterPx = with(density) { fabDiameter.toPx() }
            val cornerRadiusPx = with(density) { fabCradleRoundedCornerRadius.toPx() }
            val fabMarginPx = with(density) { fabMargin.toPx() }

            val barWidth = size.width
            val barHeight = size.height

            val cradleDepthFromTop = (fabDiameterPx / 2f) + fabMarginPx
            val cradleWidth = fabDiameterPx + (fabMarginPx * 2f)

            val cradleStartX = (barWidth - cradleWidth) / 2f
            val cradleEndX = cradleStartX + cradleWidth

            moveTo(0f, 0f)
            lineTo(cradleStartX - cornerRadiusPx, 0f)

            quadraticTo(
                x1 = cradleStartX, y1 = 0f,
                x2 = cradleStartX, y2 = cornerRadiusPx
            )

            arcTo(
                rect = Rect(
                    left = cradleStartX,
                    top = -cradleDepthFromTop,
                    right = cradleEndX,
                    bottom = cradleDepthFromTop
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )

            quadraticTo(
                x1 = cradleEndX, y1 = 0f,
                x2 = cradleEndX + cornerRadiusPx, y2 = 0f
            )

            lineTo(barWidth, 0f)
            lineTo(barWidth, barHeight)
            lineTo(0f, barHeight)
            close()
        })
    }
}

@Preview(showBackground = true, name = "Home Screen Empty MVI")
@Composable
fun HomeScreenPreviewEmptyMVI() {
    ComiquetaTheme {
        val mockViewModel = HomeViewModel(
            comicsRepository = MockComicsRepository(
                allComicsToReturn = emptyList(),
                favoriteComicsToReturn = emptyList(),
                newComicsToReturn = emptyList()
            ),
            comicsFolderRepository = MockComicsFolderRepository(LocalContext.current),
            applicationContext = LocalContext.current
        )

        HomeScreen(viewModel = mockViewModel)
    }
}

@Preview(showBackground = true, name = "Home Screen Content MVI")
@Composable
fun HomeScreenPreviewContentMVI() {
    ComiquetaTheme {
        val context = LocalContext.current
        val sampleComics = listOf(
            ComicEntity(
                filePath = Uri.parse("file:///preview/1"),
                title = "The Hero's Journey",
                coverPath = Uri.parse("https://placehold.co/150x220/E0E0E0/333333?text=Comic+1"),
                isFavorite = true,
                genre = "Action",
                isNew = true,
                hasBeenRead = false
            ),
            ComicEntity(
                filePath = Uri.parse("file:///preview/2"),
                title = "Cosmic Saga",
                coverPath = Uri.parse("https://placehold.co/150x220/D0D0D0/333333?text=Comic+2"),
                isFavorite = false,
                genre = "Sci-Fi",
                isNew = true,
                hasBeenRead = false
            )
        )

        val mockViewModel = HomeViewModel(
            // Provide the mock repository configured with sample data
            comicsRepository = MockComicsRepository(
                allComicsToReturn = sampleComics
                // favoriteComicsToReturn and newComicsToReturn will be derived if not specified
            ),
            comicsFolderRepository = MockComicsFolderRepository(context),
            applicationContext = context
        )

        HomeScreen(viewModel = mockViewModel)
    }
}

class MockComicsRepository(
    private val allComicsToReturn: List<ComicEntity> = emptyList(),
    // Allow specifying separate lists for favorites and new,
    // or let them be derived from allComicsToReturn if null.
    private val favoriteComicsToReturn: List<ComicEntity>? = null,
    private val newComicsToReturn: List<ComicEntity>? = null
) : ComicsRepository(FakeComicDao()) { // Assuming ComicsRepository takes a Dao

    override fun getAllComics(): Flow<List<ComicEntity>> = flowOf(allComicsToReturn)

    override fun getFavoriteComics(): Flow<List<ComicEntity>> = flowOf(
        favoriteComicsToReturn ?: allComicsToReturn.filter { it.isFavorite }
    )

    override fun getNewComics(): Flow<List<ComicEntity>> = flowOf(
        newComicsToReturn ?: allComicsToReturn.filter { it.isNew }
    )
}


class MockComicsFolderRepository(context: Context) : ComicsFolderRepository(context) {
    override fun takePersistablePermission(uri: Uri, flags: Int): Boolean = true
    override fun getPersistedPermissionsFlow(): StateFlow<List<Uri>> =
        MutableStateFlow<List<Uri>>(emptyList()).asStateFlow()
}

// Fake Dao for MockComicsRepository (if ComicsRepository depends on a Dao)
// You'll need to define FakeComicDao based on your actual ComicDao interface
class FakeComicDao : ComicsDao {
    override suspend fun insertComic(comic: ComicEntity) {
        TODO("Not yet implemented")
    }

    override suspend fun insertComics(comics: List<ComicEntity>) {
        TODO("Not yet implemented")
    }

    override suspend fun updateComic(comic: ComicEntity) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteComic(comic: ComicEntity) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllComics() {
        TODO("Not yet implemented")
    }

    override fun getComicByPath(filePath: String): Flow<ComicEntity?> {
        TODO("Not yet implemented")
    }

    override suspend fun getComicByFilePath(filePath: Uri): ComicEntity? {
        TODO("Not yet implemented")
    }

    override fun getAllComics(): Flow<List<ComicEntity>> = flowOf(emptyList())
    override fun getFavoriteComics(): Flow<List<ComicEntity>> {
        TODO("Not yet implemented")
    }

    override fun getNewComics(): Flow<List<ComicEntity>> {
        TODO("Not yet implemented")
    }

    override fun getUnreadComics(): Flow<List<ComicEntity>> {
        TODO("Not yet implemented")
    }
    // ... implement other methods if ComicsRepository uses them
}
