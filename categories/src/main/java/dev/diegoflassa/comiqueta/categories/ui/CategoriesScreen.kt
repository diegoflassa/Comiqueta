package dev.diegoflassa.comiqueta.categories.ui

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel

private const val tag = "CategoriesScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    navigationViewModel: NavigationViewModel? = hiltActivityViewModel(),
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    TimberLogger.logI(tag, "CategoriesScreen")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(key1 = viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CategoriesEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                CategoriesEffect.NavigateBack -> {
                    navigationViewModel?.goBack()
                }
            }
        }
    }

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
    onIntent: ((CategoriesIntent) -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
                navigationIcon = {
                    IconButton(onClick = { onIntent?.invoke(CategoriesIntent.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onIntent?.invoke(CategoriesIntent.CategoryAdd) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->
        CategoriesContent(
            modifier = modifier.padding(paddingValues),
            categories = uiState.categories,
            isLoading = uiState.isLoading,
            error = uiState.error,
            onIntent = { intent ->
                onIntent?.invoke(intent)
            }
        )

        if (uiState.showDialog) {
            CategoryEditDialog(
                category = uiState.categoryToEdit,
                currentName = uiState.newCategoryName,
                onNameChange = { name ->
                    onIntent?.invoke(CategoriesIntent.SetNewCategoryName(name))
                },
                onDismiss = { onIntent?.invoke(CategoriesIntent.DismissDialog) },
                onSave = { onIntent?.invoke(CategoriesIntent.SaveCategory) }
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
    onIntent: ((CategoriesIntent) -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading && categories.isEmpty()) {
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
                        onIntent = { intent -> onIntent?.invoke(intent) },
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
    onIntent: ((CategoriesIntent) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(category.name, modifier = Modifier.weight(1f))
        IconButton(onClick = { onIntent?.invoke(CategoriesIntent.CategoryEdit(category)) }) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit Category")
        }
        IconButton(onClick = { onIntent?.invoke(CategoriesIntent.CategoryDelete(category)) }) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete Category")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditDialog(
    category: CategoryEntity? = null,
    currentName: String = "",
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
                enabled = currentName.isNotBlank()
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

val sampleCategoriesList = listOf(
    CategoryEntity(id = 1, name = "Action"),
    CategoryEntity(id = 2, name = "Comedy"),
    CategoryEntity(id = 3, name = "Sci-Fi Adventure X"),
    CategoryEntity(id = 4, name = "Drama"),
    CategoryEntity(id = 5, name = "Horror Thriller Z")
)

// --- Previews Start ---

// Previews - With Data
@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Preview(
    name = "ScreenMainState Dark - 1080x2560px",
    group = "Previews - With Data",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=1080px,height=2560px,dpi=440"
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
@PreviewScreenSizes
@Preview(
    name = "ScreenWithDialog Dark - 1080x2560px",
    group = "Previews - With Data",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=1080px,height=2560px,dpi=440"
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

@PreviewScreenSizes
@Preview(
    name = "ContentWithData Dark - 1080x2560px",
    group = "Previews - With Data",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=1080px,height=2560px,dpi=440"
)
@Composable
fun CategoriesContentPreviewWithData() {
    ComiquetaThemeContent {
        CategoriesContent(
            categories = sampleCategoriesList,
            isLoading = false,
            error = null,
            onIntent = {}
        )
    }
}

// Previews - Other States
@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Preview(
    name = "ScreenEmptyState Dark - 1080x2560px",
    group = "Previews - Other States",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=1080px,height=2560px,dpi=440"
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

@PreviewScreenSizes
@Preview(
    name = "ContentEmpty Dark - 1080x2560px",
    group = "Previews - Other States",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=1080px,height=2560px,dpi=440"
)
@Composable
fun CategoriesContentPreviewEmpty() {
    ComiquetaThemeContent {
        CategoriesContent(
            categories = emptyList(),
            isLoading = false,
            error = null,
            onIntent = {},
        )
    }
}

@PreviewScreenSizes
@Preview(
    name = "ContentLoading Dark - 1080x2560px",
    group = "Previews - Other States",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=1080px,height=2560px,dpi=440"
)
@Composable
fun CategoriesContentPreviewLoading() {
    ComiquetaThemeContent {
        CategoriesContent(
            categories = emptyList(),
            isLoading = true,
            error = null,
            onIntent = {}
        )
    }
}

@PreviewScreenSizes
@Preview(
    name = "ContentError Dark - 1080x2560px",
    group = "Previews - Other States",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=1080px,height=2560px,dpi=440"
)
@Composable
fun CategoriesContentPreviewError() {
    ComiquetaThemeContent {
        CategoriesContent(
            categories = emptyList(),
            isLoading = false,
            error = "Failed to load categories. Please try again.",
            onIntent = {}
        )
    }
}

// Previews - Dialogs
@PreviewScreenSizes
@Preview(
    name = "DialogAdd Dark - 1080x2560px",
    group = "Previews - Dialogs",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=1080px,height=2560px,dpi=440"
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

@PreviewScreenSizes
@Preview(
    name = "DialogEdit Dark - 1080x2560px",
    group = "Previews - Dialogs",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=1080px,height=2560px,dpi=440"
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
// --- Previews End ---
