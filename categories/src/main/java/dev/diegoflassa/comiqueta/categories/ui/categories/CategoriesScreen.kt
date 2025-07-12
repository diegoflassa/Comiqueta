package dev.diegoflassa.comiqueta.categories.ui.categories

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.navigation.NavigationIntent
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    navigationViewModel: NavigationViewModel,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(key1 = viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CategoriesEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                CategoriesEffect.NavigateBack -> {
                    navigationViewModel.processIntent(NavigationIntent.GoBack)
                }
            }
        }
    }

    // Scaffold has been moved to CategoriesScreenContentPreview to allow for better previewing
    // of CategoriesContent and CategoryEditDialog independently.
    // The main CategoriesScreen will still use the Scaffold.

    CategoriesScreenContent(
        modifier = Modifier,
        uiState = uiState,
        onIntent = viewModel::processIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesScreenContent(
    modifier: Modifier = Modifier,
    uiState: CategoriesUIState,
    onIntent: (CategoriesIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") }
                // Add navigation icon if needed, e.g.,
                // navigationIcon = {
                //    IconButton(onClick = { navController.popBackStack() }) {
                //        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                //    }
                // }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onIntent(CategoriesIntent.ShowAddCategoryDialog) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->
        CategoriesContent(
            modifier = modifier.padding(paddingValues),
            categories = uiState.categories,
            isLoading = uiState.isLoading,
            error = uiState.error,
            onEditClick = { category ->
                onIntent(CategoriesIntent.ShowEditCategoryDialog(category))
            },
            onDeleteClick = { category ->
                onIntent(CategoriesIntent.DeleteCategory(category))
            }
        )

        if (uiState.showDialog) {
            CategoryEditDialog(
                category = uiState.categoryToEdit,
                currentName = uiState.newCategoryName,
                onNameChange = { name ->
                    onIntent(CategoriesIntent.SetNewCategoryName(name))
                },
                onDismiss = { onIntent(CategoriesIntent.DismissDialog) },
                onSave = { onIntent(CategoriesIntent.SaveCategory) }
            )
        }
    }
}


@Composable
fun CategoriesContent(
    modifier: Modifier = Modifier,
    categories: List<CategoryEntity>,
    isLoading: Boolean,
    error: String?,
    onEditClick: (CategoryEntity) -> Unit,
    onDeleteClick: (CategoryEntity) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading && categories.isEmpty()) { // Show loading only if list is empty
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (error != null) {
            Text(
                "Error: $error",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.error
            )
        } else if (categories.isEmpty()) {
            Text(
                "No categories found. Click '+' to add.",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(categories, key = { it.id }) { category ->
                    CategoryItem(
                        category = category,
                        onEditClick = { onEditClick(category) },
                        onDeleteClick = { onDeleteClick(category) }
                    )
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    category: CategoryEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(category.name, modifier = Modifier.weight(1f))
        IconButton(onClick = onEditClick) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit Category")
        }
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete Category")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditDialog(
    category: CategoryEntity?, // null if adding new
    currentName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Add New Category" else "Edit Category") },
        text = {
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                label = { Text("Category Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = currentName.isNotBlank() // Disable save if name is blank
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- Previews ---
private const val tag = "CategoriesScreen"

val sampleCategoriesList = listOf(
    CategoryEntity(id = 1, name = "Action"),
    CategoryEntity(id = 2, name = "Comedy"),
    CategoryEntity(id = 3, name = "Sci-Fi Adventure X"),
    CategoryEntity(id = 4, name = "Drama"),
    CategoryEntity(id = 5, name = "Horror Thriller Z")
)

@Preview(name = "${tag}ContentEmpty", showBackground = true)
@Preview(
    name = "${tag}ContentEmpty Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CategoriesContentPreviewEmpty() {
    ComiquetaThemeContent {
        CategoriesContent(
            categories = emptyList(),
            isLoading = false,
            error = null,
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview(name = "${tag}ContentLoading", showBackground = true)
@Preview(
    name = "${tag}ContentLoading Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CategoriesContentPreviewLoading() {
    ComiquetaThemeContent {
        CategoriesContent(
            categories = emptyList(),
            isLoading = true,
            error = null,
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview(name = "${tag}ContentError", showBackground = true)
@Preview(
    name = "${tag}ContentError Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CategoriesContentPreviewError() {
    ComiquetaThemeContent {
        CategoriesContent(
            categories = emptyList(),
            isLoading = false,
            error = "Failed to load categories. Please try again.",
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview(name = "${tag}ContentWithData", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(
    name = "${tag}ContentWithData Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CategoriesContentPreviewWithData() {
    ComiquetaThemeContent {
        CategoriesContent(
            categories = sampleCategoriesList,
            isLoading = false,
            error = null,
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview(name = "${tag}DialogAdd", showBackground = true)
@Preview(
    name = "${tag}DialogAdd Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CategoryEditDialogPreviewAdd() {
    ComiquetaThemeContent {
        CategoryEditDialog(
            category = null,
            currentName = "",
            onNameChange = {},
            onDismiss = {},
            onSave = {}
        )
    }
}

@Preview(name = "${tag}DialogEdit", showBackground = true)
@Preview(
    name = "${tag}DialogEdit Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CategoryEditDialogPreviewEdit() {
    ComiquetaThemeContent {
        CategoryEditDialog(
            category = sampleCategoriesList.first(),
            currentName = sampleCategoriesList.first().name,
            onNameChange = {},
            onDismiss = {},
            onSave = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "${tag}ScreenWithDialog", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(
    name = "${tag}ScreenWithDialog Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CategoriesScreenPreviewWithDialog() {
    ComiquetaThemeContent {
        CategoriesScreenContent(
            uiState = CategoriesUIState(
                categories = sampleCategoriesList,
                showDialog = true,
                categoryToEdit = sampleCategoriesList[1],
                newCategoryName = sampleCategoriesList[1].name
            ),
            onIntent = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "${tag}ScreenMainState", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(
    name = "${tag}ScreenMainState Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CategoriesScreenPreviewMainState() {
    ComiquetaThemeContent {
        CategoriesScreenContent(
            uiState = CategoriesUIState(categories = sampleCategoriesList.take(3)),
            onIntent = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "${tag}ScreenEmptyState", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(
    name = "${tag}ScreenEmptyState Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CategoriesScreenPreviewEmptyState() {
    ComiquetaThemeContent {
        CategoriesScreenContent(
            uiState = CategoriesUIState(categories = emptyList()),
            onIntent = {},
        )
    }
}

