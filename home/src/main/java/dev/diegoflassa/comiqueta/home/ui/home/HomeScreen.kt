package dev.diegoflassa.comiqueta.home.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import java.util.UUID
import androidx.core.net.toUri
import dev.diegoflassa.comiqueta.core.navigation.Screen

@Composable
fun HomeScreen(modifier: Modifier = Modifier, navigationViewModel: NavigationViewModel? = null) {
    val comics = remember { mutableStateListOf<ComicEntity>() }
    LaunchedEffect(Unit) {
        comics.addAll(
            listOf(
                ComicEntity(
                    filePath = "file:///sample/${UUID.randomUUID()}".toUri(),
                    title = "Titulo 1",
                    coverPath = "https://placehold.co/150x220/E0E0E0/333333?text=Comic+1".toUri(),
                    isFavorite = true,
                    genre = "Action",
                    isNew = true,
                    hasBeenRead = false
                ),
                ComicEntity(
                    filePath = "file:///sample/${UUID.randomUUID()}".toUri(),
                    title = "Titulo 2",
                    coverPath = "https://placehold.co/150x220/D0D0D0/333333?text=Comic+2".toUri(),
                    isFavorite = false,
                    genre = "Sci-Fi",
                    isNew = true,
                    hasBeenRead = false
                ),
                // Add more sample comics if needed
            )
        )
    }

    var currentScreen by remember { mutableStateOf("home") }

    val onAddComic: () -> Unit = {
        val newComic = ComicEntity(
            filePath = "file:///sample/new_${comics.size + 1}".toUri(),
            title = "New Comic ${comics.size + 1}",
            coverPath = "https://placehold.co/150x220/808080/FFFFFF?text=New+${comics.size + 1}".toUri(),
            isFavorite = false,
            genre = "Mystery",
            isNew = true,
            hasBeenRead = false
        )
        comics.add(newComic)
    }

    ComiquetaScreen(
        comics = comics,
        currentScreen = currentScreen,
        onNavigate = { screen -> navigationViewModel?.navigateTo(screen = screen) },
        onAddComic = onAddComic
    )
}

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
                    top = -cradleDepthFromTop, // Rect top for arc calculation to make it dip downwards
                    right = cradleEndX,
                    bottom = cradleDepthFromTop // Rect bottom for arc calculation
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComiquetaScreen(
    comics: List<ComicEntity>,
    currentScreen: String,
    onNavigate: (Screen) -> Unit,
    onAddComic: () -> Unit
) {
    val isEmpty = comics.isEmpty()
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Action", "Sci-Fi", "Fantasy", "Horror", "Adventure", "Mystery")

    val fabDiameter = 56.dp
    val bottomBarHeight = 64.dp // Height of the Surface used as BottomBar

    // Offset to pull the FAB's center UP to the top edge of the bottomBar Surface
    // FabPosition.Center places the FAB roughly centered in the bottom bar area.
    // So we offset by -(half the bottomBarHeight).
    val fabVerticalOffset = -(bottomBarHeight / 2)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comiqueta", fontWeight = FontWeight.Bold) },
                actions = {
                    if (!isEmpty) {
                        IconButton(onClick = { onNavigate(Screen.Settings) }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddComic,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(fabDiameter)
                    .shadow(10.dp, CircleShape)
                    .offset(y = fabVerticalOffset) // Apply calculated offset
            ) {
                Icon(
                    modifier = Modifier
                        .size(fabDiameter * 0.6f),
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
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
                        shape = BottomBarArcShape(
                            fabDiameter = fabDiameter,
                            fabCradleRoundedCornerRadius = 24.dp, // Smoothness of cradle corners
                            fabMargin = 8.dp  // Space between FAB edge and cradle curve start
                        ),
                        clip = true // Essential for the shape to cut out the Surface
                    ),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = BottomAppBarDefaults.ContainerElevation // Or your preferred elevation
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp), // Padding for nav items from screen edges
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(
                        icon = Icons.Default.Home, label = "Home",
                        isSelected = currentScreen == "home", onClick = { onNavigate(Screen.Home) },
                        modifier = Modifier.weight(1f)
                    )
                    BottomNavItem(
                        icon = Icons.Default.Star,
                        label = "Catalog",
                        isSelected = currentScreen == "catalog",
                        onClick = { onNavigate(Screen.Catalog) },
                        modifier = Modifier.weight(1f)
                    )
                    // Spacer for the FAB. Width should accommodate FAB diameter + margins used in shape
                    Spacer(modifier = Modifier.width(fabDiameter + (8.dp * 2)))
                    BottomNavItem(
                        icon = Icons.AutoMirrored.Filled.List,
                        label = "Bookmark",
                        isSelected = currentScreen == "bookmark",
                        onClick = { onNavigate(Screen.Bookmark) },
                        modifier = Modifier.weight(1f)
                    )
                    BottomNavItem(
                        icon = Icons.Default.Favorite,
                        label = "Favorites",
                        isSelected = currentScreen == "favorites",
                        onClick = { onNavigate(Screen.Favorites) },
                        modifier = Modifier.weight(1f)
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
            if (isEmpty) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "You still don't have any books added to your catalog.",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Click the '+' button below to add.",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add book icon",
                        modifier = Modifier
                            .size(128.dp)
                            .padding(bottom = 16.dp),
                        tint = Color.Gray
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text("Search Comics") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        categories.forEach { category ->
                            Button(
                                onClick = { selectedCategory = category },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedCategory == category) MaterialTheme.colorScheme.primary else Color(
                                        0xFFE0E0E0
                                    ),
                                    contentColor = if (selectedCategory == category) Color.White else Color.DarkGray
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(text = category, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Latest",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(comics.filter { it.isNew }.take(4)) { comic ->
                            ComicCoverCard(comic = comic)
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Favorites",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(comics.filter { it.isFavorite }.take(2)) { comic ->
                            ComicCoverCard(comic = comic)
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All Comics",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filteredComics = comics.filter {
                            (searchText.isEmpty() || it.title?.contains(
                                searchText,
                                ignoreCase = true
                            ) == true) &&
                                    (selectedCategory == "All" || it.genre == selectedCategory)
                        }
                        items(filteredComics) { comic ->
                            ComicListItem(comic = comic)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun ComiquetaScreenPreviewEmpty() {
    ComiquetaTheme {
        HomeScreen()
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun ComiquetaScreenPreviewContent() {
    ComiquetaTheme {
        ComiquetaScreen(
            comics = remember {
                mutableStateListOf(
                    ComicEntity(
                        filePath = "file:///preview/1".toUri(),
                        title = "The Hero's Journey",
                        coverPath = "https://placehold.co/150x220/E0E0E0/333333?text=Comic+1".toUri(),
                        isFavorite = true, genre = "Action", isNew = true, hasBeenRead = false
                    ),
                    ComicEntity(
                        filePath = "file:///preview/2".toUri(),
                        title = "Cosmic Saga",
                        coverPath = "https://placehold.co/150x220/D0D0D0/333333?text=Comic+2".toUri(),
                        isFavorite = false, genre = "Sci-Fi", isNew = true, hasBeenRead = false
                    )
                )
            },
            currentScreen = "home",
            onNavigate = {},
            onAddComic = {}
        )
    }
}
