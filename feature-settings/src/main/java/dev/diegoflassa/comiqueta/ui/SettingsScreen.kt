package dev.diegoflassa.comiqueta.ui

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.ump.UserMessagingPlatform
import dev.diegoflassa.comiqueta.settings.R
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel
import dev.diegoflassa.comiqueta.data.model.PermissionDisplayStatus

private const val tag = "SettingsScreen"
private const val MAX_PRELOAD_PAGES = 5

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
                val shouldShowRationale =
                    !isGranted && ActivityCompat.shouldShowRequestPermissionRationale(
                        currentActivity,
                        permission
                    )
                PermissionDisplayStatus(
                    isGranted = isGranted,
                    shouldShowRationale = shouldShowRationale
                )
            }
            settingsViewModel.processIntent(
                SettingsIntent.PermissionResults(
                    results = permissionDisplayStatusMap,
                    activity = currentActivity
                )
            )
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
            } catch (se: SecurityException) {
                se.printStackTrace()
                TimberLogger.logE(tag, "Failed to take persistable URI permission for $uri", se)
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
                    folderPickerLauncher.launch(null) // Initial URI can be null for SAF
                }

                is SettingsEffect.LaunchViewFolderIntent -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(effect.folderUri, DocumentsContract.Document.MIME_TYPE_DIR)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (anfe: ActivityNotFoundException) {
                        anfe.printStackTrace()
                        Toast.makeText(
                            context,
                            noAppToOpenFolder,
                            Toast.LENGTH_SHORT
                        ).show()
                        TimberLogger.logE(
                            tag,
                            "No activity found to handle folder URI: ${effect.folderUri}",
                            anfe
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
        uiState = settingsUIState,
        onIntent = { settingsViewModel.processIntent(it) }
    )
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
        LazyColumn( // Changed to LazyColumn to accommodate more settings
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp.scaled()),
        ) {
            item { // Permissions Section Title
                Text(
                    text = stringResource(R.string.settings_section_permissions_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp.scaled(), bottom = 8.dp.scaled())
                )
            }
            if (uiState.permissionDisplayStatuses.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.settings_permissions_none_required),
                        modifier = Modifier.padding(vertical = 8.dp.scaled()),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                uiState.permissionDisplayStatuses.forEach { (permission, status) ->
                    item {
                        PermissionItem(
                            permission = permission,
                            status = status,
                            onRequestPermissionClick = {
                                onIntent?.invoke(
                                    SettingsIntent.RequestPermission(permission)
                                )
                            },
                            onOpenSettingsClick = { onIntent?.invoke(SettingsIntent.OpenAppSettingsClicked) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp.scaled()))
                    }
                }
                if (uiState.permissionDisplayStatuses.keys.any { it == Manifest.permission.READ_EXTERNAL_STORAGE }) {
                    item {
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
            }

            item { // Monitored Folders Section Title
                Text(
                    text = stringResource(R.string.settings_section_monitored_folders_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp.scaled(), bottom = 8.dp.scaled())
                )
            }
            if (uiState.comicsFolders.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.settings_monitored_folders_empty),
                        modifier = Modifier.padding(vertical = 8.dp.scaled()),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
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
            item { Spacer(modifier = Modifier.height(16.dp.scaled())) }


            // Viewer Settings Section - Added
            item {
                Text(
                    text = "Viewer Settings", // Consider adding to strings.xml
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp.scaled(), bottom = 8.dp.scaled())
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Comic Page Pre-loading") }, // Consider strings.xml
                        supportingContent = { Text("Pages to load ahead/behind current page (0-${MAX_PRELOAD_PAGES}).") }, // Consider strings.xml
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val currentCount = uiState.viewerPagesToPreloadAhead
                                        if (currentCount > 0) {
                                            onIntent?.invoke(SettingsIntent.UpdateViewerPagesToPreloadAhead(currentCount - 1))
                                        }
                                    },
                                    enabled = uiState.viewerPagesToPreloadAhead > 0
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease pre-load count") // Consider strings.xml
                                }
                                Text(
                                    text = uiState.viewerPagesToPreloadAhead.toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(horizontal = 8.dp.scaled())
                                )
                                IconButton(
                                    onClick = {
                                        val currentCount = uiState.viewerPagesToPreloadAhead
                                        if (currentCount < MAX_PRELOAD_PAGES) {
                                            onIntent?.invoke(SettingsIntent.UpdateViewerPagesToPreloadAhead(currentCount + 1))
                                        }
                                    },
                                    enabled = uiState.viewerPagesToPreloadAhead < MAX_PRELOAD_PAGES
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase pre-load count") // Consider strings.xml
                                }
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp.scaled()))
            }

            // Manage Categories Section
            item {
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


            // Privacy Settings / Ad Consent Section
            item {
                val context = LocalContext.current
                val activity = context as? Activity
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activity?.let { currentActivity ->
                                TimberLogger.logD(tag, "Showing privacy options form.")
                                UserMessagingPlatform.showPrivacyOptionsForm(currentActivity) { formError ->
                                    if (formError != null) {
                                        TimberLogger.logE(
                                            tag,
                                            "Error showing privacy options form: ${formError.message}",
                                            Exception("${formError.errorCode}-${formError.message}")
                                        )
                                        Toast.makeText(
                                            currentActivity,
                                            "Error loading privacy settings: ${formError.errorCode} - ${formError.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } ?: run {
                                TimberLogger.logW(
                                    tag,
                                    "Activity context not available for showing privacy options form."
                                )
                                Toast.makeText(
                                    context,
                                    "Could not open privacy settings.",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        },
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_privacy_title)) },
                        supportingContent = { Text(stringResource(R.string.settings_privacy_description)) },
                        leadingContent = {
                            Icon(
                                Icons.Default.Policy,
                                contentDescription = stringResource(R.string.settings_privacy_icon_desc)
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp.scaled())) // Space at the end
            }

            // Handle initial loading state for the whole screen
            if (uiState.isLoading && uiState.permissionDisplayStatuses.isEmpty() && uiState.comicsFolders.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { // fillParentMaxSize for LazyColumn item
                        CircularProgressIndicator()
                    }
                }
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
            .clickable { // Consider what happens on click for the whole item
                onIntent?.invoke(SettingsIntent.OpenFolder(folderUri)) // Changed to OpenFolder
            }
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
private fun SettingsScreenPreview() {
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
                comicsFolders = listOf("content://com.android.externalstorage.documents/tree/primary%3ADCIM".toUri()),
                viewerPagesToPreloadAhead = 1 // Added for preview
            )
        )
    }
}

@PreviewScreenSizes
@Preview(
    name = "Phone - Dark",
    group = "Screen - With Data",
    showBackground = true,
    device = "spec:width=1080px,height=2560px,dpi=440",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SettingsScreenPreviewDark() {
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
                ),
                viewerPagesToPreloadAhead = 2 // Added for preview
            )
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SettingsScreenPreviewEmpty() {
    ComiquetaThemeContent {
        SettingsScreenContent(
            uiState = SettingsUIState(
                isLoading = false,
                permissionDisplayStatuses = emptyMap(),
                comicsFolders = emptyList(),
                viewerPagesToPreloadAhead = 0 // Added for preview
            )
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SettingsScreenPreviewLoading() {
    ComiquetaThemeContent {
        SettingsScreenContent(
            uiState = SettingsUIState(
                isLoading = true,
                permissionDisplayStatuses = emptyMap(),
                comicsFolders = emptyList(),
                viewerPagesToPreloadAhead = 1 // Added for preview
            )
        )
    }
}
