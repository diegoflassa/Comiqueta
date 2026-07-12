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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.domain.model.Category
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel
import dev.diegoflassa.comiqueta.categories.R

private const val tag = "CategoriesScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    navigationViewModel: NavigationViewModel? = hiltActivityViewModel(),
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    TimberLogger.logI(tag, "[Comiqueta][Categories] CategoriesScreen")
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
                title = { Text(stringResource(R.string.manage_categories_title)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent?.invoke(CategoriesIntent.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onIntent?.invoke(CategoriesIntent.CategoryAdd) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_category_desc))
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
    categories: List<Category>,
    isLoading: Boolean,
    error: String?,
    onIntent: ((CategoriesIntent) -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading && categories.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (error != null) {
            Text(
                stringResource(R.string.error_prefix, error),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(ComiquetaTheme.dimen.paddingMedium.scaled()),
                color = ComiquetaTheme.colorScheme.error
            )
        } else if (categories.isEmpty()) {
            Text(
                stringResource(R.string.no_categories_found),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(ComiquetaTheme.dimen.paddingMedium.scaled())
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
    category: Category,
    onIntent: ((CategoriesIntent) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = ComiquetaTheme.dimen.paddingMedium.scaled(),
                vertical = ComiquetaTheme.dimen.paddingSmall.scaled()
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(category.name, modifier = Modifier.weight(1f))
        IconButton(onClick = { onIntent?.invoke(CategoriesIntent.CategoryEdit(category)) }) {
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_category_desc))
        }
        IconButton(onClick = { onIntent?.invoke(CategoriesIntent.CategoryDelete(category)) }) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_category_desc))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditDialog(
    category: Category? = null,
    currentName: String = "",
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) stringResource(R.string.dialog_add_category_title) else stringResource(R.string.dialog_edit_category_title)) },
        text = {
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.category_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = currentName.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

val sampleCategoriesList = listOf(
    Category(id = 1, name = "Action", createdAt = 0),
    Category(id = 2, name = "Comedy", createdAt = 0),
    Category(id = 3, name = "Sci-Fi Adventure X", createdAt = 0),
    Category(id = 4, name = "Drama", createdAt = 0),
    Category(id = 5, name = "Horror Thriller Z", createdAt = 0)
)


// --- Previews - Main States ---
@Preview(name = "CategoriesScreenContent · Main State · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "CategoriesScreenContent · Main State · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun CategoriesScreenContentMainStatePreview() {
    ComiquetaThemeContent {
        CategoriesScreenContent(
            uiState = CategoriesUIState(categories = sampleCategoriesList.take(3)),
            onIntent = {},
        )
    }
}

@Preview(name = "CategoriesScreenContent · Main State · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoriesScreenContentMainStateDarkPreview() {
    ComiquetaThemeContent {
        CategoriesScreenContent(
            uiState = CategoriesUIState(categories = sampleCategoriesList.take(3)),
            onIntent = {},
        )
    }
}

@Preview(name = "CategoriesScreenContent · With Dialog · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "CategoriesScreenContent · With Dialog · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun CategoriesScreenContentWithDialogPreview() {
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

@Preview(name = "CategoriesScreenContent · With Dialog · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoriesScreenContentWithDialogDarkPreview() {
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

// --- Previews - Other States ---
@Preview(name = "CategoriesScreenContent · Empty · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "CategoriesScreenContent · Empty · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun CategoriesScreenContentEmptyPreview() {
    ComiquetaThemeContent {
        CategoriesScreenContent(
            uiState = CategoriesUIState(categories = emptyList()),
            onIntent = {},
        )
    }
}

@Preview(name = "CategoriesScreenContent · Empty · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoriesScreenContentEmptyDarkPreview() {
    ComiquetaThemeContent {
        CategoriesScreenContent(
            uiState = CategoriesUIState(categories = emptyList()),
            onIntent = {},
        )
    }
}

@Preview(name = "CategoriesContent · Loading · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "CategoriesContent · Loading · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun CategoriesContentLoadingPreview() {
    ComiquetaThemeContent {
        CategoriesContent(
            categories = emptyList(),
            isLoading = true,
            error = null,
            onIntent = {}
        )
    }
}

@Preview(name = "CategoriesContent · Loading · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoriesContentLoadingDarkPreview() {
    ComiquetaThemeContent {
        CategoriesContent(
            categories = emptyList(),
            isLoading = true,
            error = null,
            onIntent = {}
        )
    }
}

@Preview(name = "CategoriesContent · Error · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "CategoriesContent · Error · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun CategoriesContentErrorPreview() {
    ComiquetaThemeContent {
        CategoriesContent(
            categories = emptyList(),
            isLoading = false,
            error = "Failed to load categories. Please try again.",
            onIntent = {}
        )
    }
}

@Preview(name = "CategoriesContent · Error · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoriesContentErrorDarkPreview() {
    ComiquetaThemeContent {
        CategoriesContent(
            categories = emptyList(),
            isLoading = false,
            error = "Failed to load categories. Please try again.",
            onIntent = {}
        )
    }
}

// --- Previews - Dialogs ---
@Preview(name = "CategoryEditDialog · Add · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "CategoryEditDialog · Add · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun CategoryEditDialogAddPreview() {
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

@Preview(name = "CategoryEditDialog · Add · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoryEditDialogAddDarkPreview() {
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

@Preview(name = "CategoryEditDialog · Edit · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "CategoryEditDialog · Edit · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun CategoryEditDialogEditPreview() {
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

@Preview(name = "CategoryEditDialog · Edit · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoryEditDialogEditDarkPreview() {
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
