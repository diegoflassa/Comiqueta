package dev.diegoflassa.comiqueta.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat // Needed for shouldShowRequestPermissionRationale
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diegoflassa.comiqueta.settings.R
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel
import dev.diegoflassa.comiqueta.settings.model.PermissionDisplayStatus

private const val tag = "SettingsScreen"

private fun getPermissionFriendlyNameSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage Access"
        else -> permission.substringAfterLast('.').replace('_', ' ').let {
            it.lowercase()
                .replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
        }
    }
}

private fun getPermissionDescriptionSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE -> "Allows the app to read your comic files from shared storage."
        else -> "Required for app functionality."
    }
}

private fun getPermissionRationaleSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE -> "This app needs access to your device's storage to find and display your comic book files. Please grant this permission to select your comic library."
        else -> "This permission is important for certain features to work correctly."
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel? = hiltActivityViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    TimberLogger.logI(tag, "SettingsScreen")
    val context = LocalContext.current
    val activity = context as? Activity
    val settingsUIState: SettingsUIState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResultMapFromContract: Map<String, Boolean> ->
        activity?.let { currentActivity ->
            val permissionDisplayStatusMap = permissionsResultMapFromContract.mapValues { entry ->
                val permission = entry.key
                val isGranted = entry.value
                val shouldShowRationale = !isGranted && ActivityCompat.shouldShowRequestPermissionRationale(currentActivity, permission)
                PermissionDisplayStatus(
                    isGranted = isGranted,
                    shouldShowRationale = shouldShowRationale
                )
            }
            settingsViewModel.processIntent(
                SettingsIntent.PermissionResults(
                    results = permissionDisplayStatusMap, // Pass the transformed map
                    activity = currentActivity
                )
            )
            // The explicit call to SettingsIntent.RefreshPermissionStatuses(activity) is no longer needed here,
            // as SettingsViewModel.handleOsPermissionResults now calls refreshPermissionDisplayStatusUseCase.
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                TimberLogger.logD(tag, "Persistable URI permission granted for $uri")
                settingsViewModel.processIntent(SettingsIntent.FolderSelected(uri))
            } catch (e: SecurityException) {
                TimberLogger.logE(tag, "Failed to take persistable URI permission for $uri", e)
                Toast.makeText(
                    context,
                    "Failed to get persistent access to the folder.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    val noAppToOpenFolder = stringResource(R.string.no_app_to_open_folder)
    LaunchedEffect(key1 = settingsViewModel) {
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
                    folderPickerLauncher.launch(null)
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
                        TimberLogger.logE(
                            tag,
                            "No activity found to handle folder URI: ${effect.folderUri}",
                            e
                        )
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.action_back)
                        )
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
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(
                            uiState.comicsFolders.size,
                            key = { index -> uiState.comicsFolders[index].toString() }
                        ) { index ->
                            val folderUri = uiState.comicsFolders[index]
                            ComicsFolderUriItem(
                                folderUri = folderUri,
                                onIntent = { intent -> onIntent?.invoke(intent) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp.scaled()))

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
                color = MaterialTheme.colorScheme.tertiary,
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
    onIntent: ((SettingsIntent) -> Unit)? = null
) {
    val path = remember(folderUri) { folderUri.path ?: "Unknown path" }
    val decodedPath = remember(path) { Uri.decode(path) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onIntent?.invoke(SettingsIntent.FolderSelected(folderUri)) }
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
            maxLines = 2,
            style = MaterialTheme.typography.bodyMedium
        )
        IconButton(onClick = { onIntent?.invoke(SettingsIntent.RemoveFolderClicked(folderUri)) }) {
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

@PreviewScreenSizes
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

@PreviewScreenSizes
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

@PreviewScreenSizes
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

@PreviewScreenSizes
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
