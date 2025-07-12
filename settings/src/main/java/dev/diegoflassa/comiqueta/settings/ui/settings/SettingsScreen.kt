package dev.diegoflassa.comiqueta.settings.ui.settings

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.settings.R

private const val tag = "SettingsScreen"

private fun getPermissionFriendlyNameSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage Access"
        // Add other permissions your app might request
        else -> permission.substringAfterLast('.').replace('_', ' ').let {
            it.lowercase()
                .replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
        } // Basic fallback
    }
}

private fun getPermissionDescriptionSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE -> "Allows the app to read your comic files from shared storage."
        // Add other permissions
        else -> "Required for app functionality." // Generic fallback
    }
}

private fun getPermissionRationaleSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE -> "This app needs access to your device's storage to find and display your comic book files. Please grant this permission to select your comic library."
        // Add other permissions
        else -> "This permission is important for certain features to work correctly." // Generic fallback
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel? = null,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val settingsUIState: SettingsUIState by settingsViewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResultMap ->
        settingsViewModel.processIntent(SettingsIntent.PermissionResults(permissionsResultMap))
        if (activity != null) {
            settingsViewModel.processIntent(SettingsIntent.RefreshPermissionStatuses(activity))
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            // Persist permission for the selected folder URI
            val contentResolver = context.contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION // Optional: if you ever need to write
            try {
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                TimberLogger.logD(tag, "Persistable URI permission granted for $uri")
                settingsViewModel.processIntent(SettingsIntent.FolderSelected(uri))
            } catch (e: SecurityException) {
                TimberLogger.logE(tag, "Failed to take persistable URI permission for $uri", e)
                Toast.makeText(context, "Failed to get persistent access to the folder.", Toast.LENGTH_LONG).show()
            }
        }
    }
    val noAppToOpenFolder = stringResource(R.string.no_app_to_open_folder)
    LaunchedEffect(key1 = settingsViewModel) { // Use a key that won't change, or the ViewModel itself
        settingsViewModel.effect.collect { effect ->
            when (effect) {
                is SettingsEffect.LaunchPermissionRequest -> {
                    permissionLauncher.launch(effect.permissionsToRequest.toTypedArray())
                }

                is SettingsEffect.NavigateToAppSettingsScreen -> {
                    openAppSettings(context)
                }

                is SettingsEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                is SettingsEffect.LaunchFolderPicker -> {
                    folderPickerLauncher.launch(null) // Initially, no specific URI is needed for picking
                }

                is SettingsEffect.LaunchViewFolderIntent -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(effect.folderUri, DocumentsContract.Document.MIME_TYPE_DIR)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(
                            context,
                            noAppToOpenFolder,
                            Toast.LENGTH_SHORT
                        ).show()
                        TimberLogger.logE(tag, "No activity found to handle folder URI: ${effect.folderUri}", e)
                    }
                }
                is SettingsEffect.NavigateToCategoriesScreen -> {
                    navigationViewModel?.navigateToCategories()
                }
            }
        }
    }


    LaunchedEffect(key1 = activity) {
        if (activity != null) {
            settingsViewModel.processIntent(SettingsIntent.RefreshPermissionStatuses(activity))
        }
    }
    SettingsScreenContent(
        modifier = modifier,
        navigationViewModel = navigationViewModel,
        uiState = settingsUIState
    ) {
        settingsViewModel.processIntent(it)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel? = null,
    uiState: SettingsUIState = SettingsUIState(),
    onIntent: ((SettingsIntent) -> Unit)? = null
) {
    BackHandler {
        navigationViewModel?.goBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigationViewModel?.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp.scaled()),
        ) {
            if (uiState.isLoading && uiState.permissionDisplayStatuses.isEmpty() && uiState.comicsFolders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Permissions Section
                Text(
                    text = stringResource(R.string.settings_section_permissions_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp.scaled(), bottom = 8.dp.scaled())
                )
                if (uiState.permissionDisplayStatuses.isEmpty()) {
                    Text(
                        stringResource(R.string.settings_permissions_none_required),
                        modifier = Modifier.padding(vertical = 8.dp.scaled()),
                        textAlign = TextAlign.Center
                    )
                } else {
                    uiState.permissionDisplayStatuses.forEach { (permission, status) ->
                        PermissionItem(
                            permission = permission,
                            status = status,
                            onRequestPermissionClick = {
                                onIntent?.invoke(
                                    SettingsIntent.RequestPermission(
                                        permission
                                    )
                                )
                            },
                            onOpenSettingsClick = { onIntent?.invoke(SettingsIntent.OpenAppSettingsClicked) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp.scaled()))
                    }
                    if (uiState.permissionDisplayStatuses.keys.any { it == Manifest.permission.READ_EXTERNAL_STORAGE }) {
                        Text(
                            stringResource(R.string.settings_permission_read_external_storage_rationale_extended),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                top = 8.dp.scaled(),
                                bottom = 16.dp.scaled()
                            )
                        )
                    }
                }

                // Monitored Folders Section
                Text(
                    text = stringResource(R.string.settings_section_monitored_folders_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp.scaled(), bottom = 8.dp.scaled())
                )
                if (uiState.comicsFolders.isEmpty()) {
                    Text(
                        stringResource(R.string.settings_monitored_folders_empty),
                        modifier = Modifier.padding(vertical = 8.dp.scaled()),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) { // Make LazyColumn take available space
                        items(
                            uiState.comicsFolders.size,
                            key = { index -> uiState.comicsFolders[index].toString() } // Use URI as key
                        ) { index ->
                            val folderUri = uiState.comicsFolders[index]
                            ComicsFolderUriItem(
                                folderUri = folderUri,
                                onFolderClick = { uri ->
                                    onIntent?.invoke(
                                        SettingsIntent.OpenFolder(
                                            uri
                                        )
                                    )
                                },
                                onRemoveClick = { uri ->
                                    onIntent?.invoke(
                                        SettingsIntent.RemoveFolderClicked(
                                            uri
                                        )
                                    )
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp.scaled())) // Add some space before the new button

                // Manage Categories Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIntent?.invoke(SettingsIntent.NavigateToCategoriesClicked) },
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_manage_categories_title)) },
                        supportingContent = { Text(stringResource(R.string.settings_manage_categories_description)) },
                        leadingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.ListAlt,
                                contentDescription = stringResource(R.string.settings_manage_categories_icon_desc)
                            )
                        }
                    )
                }
                 Spacer(modifier = Modifier.height(16.dp.scaled()))
            }
        }
    }
}

@Composable
fun PermissionItem(
    permission: String,
    status: PermissionDisplayStatus,
    onRequestPermissionClick: () -> Unit,
    onOpenSettingsClick: () -> Unit
) {
    val isEffectivelyPermanentlyDenied = !status.isGranted && !status.shouldShowRationale

    Column(modifier = Modifier.padding(vertical = 8.dp.scaled())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp.scaled())
            ) {
                Text(
                    text = getPermissionFriendlyNameSettings(permission),
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp.scaled()
                )
                Text(
                    text = getPermissionDescriptionSettings(permission),
                    fontSize = 12.sp.scaled(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (status.isGranted) {
                Text(
                    stringResource(R.string.settings_permission_status_granted),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp.scaled())
                )
                Button(onClick = onOpenSettingsClick) { Text(stringResource(R.string.settings_button_app_settings)) }
            } else {
                Button(onClick = onRequestPermissionClick) {
                    Text(stringResource(R.string.settings_button_grant_permission))
                }
            }
        }

        if (status.shouldShowRationale) {
            Text(
                text = getPermissionRationaleSettings(permission),
                fontSize = 12.sp.scaled(),
                color = MaterialTheme.colorScheme.tertiary, // Using tertiary for rationale
                modifier = Modifier.padding(
                    top = 4.dp.scaled(),
                    start = 8.dp.scaled(),
                    end = 8.dp.scaled()
                )
            )
        } else if (isEffectivelyPermanentlyDenied) {
            Text(
                text = stringResource(R.string.settings_permission_denied_permanently_message),
                fontSize = 12.sp.scaled(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(
                    top = 4.dp.scaled(),
                    start = 8.dp.scaled(),
                    end = 8.dp.scaled()
                )
            )
        }
    }
}

@Composable
fun ComicsFolderUriItem(
    folderUri: Uri,
    onFolderClick: (Uri) -> Unit,
    onRemoveClick: (Uri) -> Unit
) {
    val path = remember(folderUri) { folderUri.path ?: "Unknown path" }
    val decodedPath = remember(path) { Uri.decode(path) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFolderClick(folderUri) }
            .padding(vertical = 12.dp.scaled()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = decodedPath,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp.scaled()),
            overflow = TextOverflow.Ellipsis,
            maxLines = 2, // Allow up to 2 lines for longer paths
            style = MaterialTheme.typography.bodyMedium
        )
        IconButton(onClick = { onRemoveClick(folderUri) }) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.settings_remove_folder_action_desc),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.fromParts("package", context.packageName, null)
    context.startActivity(intent)
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ComiquetaThemeContent {
        SettingsScreenContent(
            uiState = SettingsUIState(
                isLoading = false,
                permissionDisplayStatuses = mapOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE to PermissionDisplayStatus(
                        isGranted = true,
                        shouldShowRationale = false
                    )
                ),
                comicsFolders = listOf("content://com.android.externalstorage.documents/tree/primary%3ADCIM".toUri())
            )
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsScreenPreviewDark() {
    ComiquetaThemeContent {
        SettingsScreenContent(
            uiState = SettingsUIState(
                isLoading = false,
                permissionDisplayStatuses = mapOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE to PermissionDisplayStatus(
                        isGranted = false,
                        shouldShowRationale = true
                    )
                ),
                comicsFolders = listOf(
                    "content://com.android.externalstorage.documents/tree/primary%3ADCIM".toUri(),
                    "content://com.android.externalstorage.documents/tree/primary%3APictures".toUri()
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreviewEmpty() {
    ComiquetaThemeContent {
        SettingsScreenContent(
            uiState = SettingsUIState(
                isLoading = false,
                permissionDisplayStatuses = emptyMap(),
                comicsFolders = emptyList()
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreviewLoading() {
    ComiquetaThemeContent {
        SettingsScreenContent(
            uiState = SettingsUIState(
                isLoading = true,
                permissionDisplayStatuses = emptyMap(),
                comicsFolders = emptyList()
            )
        )
    }
}
