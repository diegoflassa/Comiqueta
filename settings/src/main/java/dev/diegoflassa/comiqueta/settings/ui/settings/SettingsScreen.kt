package dev.diegoflassa.comiqueta.settings.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme

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
            settingsViewModel.processIntent(SettingsIntent.FolderSelected(uri))
        }
    }

    LaunchedEffect(key1 = Unit) { // Or key1 = settingsViewModel
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
        settingsViewModel = settingsViewModel,
        settingsUIState = settingsUIState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel? = null,
    settingsViewModel: SettingsViewModel? = null,
    settingsUIState: SettingsUIState = SettingsUIState()
) {
    val uiState = settingsViewModel?.uiState?.collectAsState()?.value ?: settingsUIState
    BackHandler {
        navigationViewModel?.goBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navigationViewModel?.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go back")
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
                .padding(horizontal = 16.dp),
        ) {
            if (uiState.isLoading && uiState.permissionDisplayStatuses.isEmpty() && uiState.comicsFolders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Permissions Section
                Text(
                    text = "App Permissions",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                if (uiState.permissionDisplayStatuses.isEmpty()) {
                    Text(
                        "No special permissions from this list are required for this version of Android.",
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    uiState.permissionDisplayStatuses.forEach { (permission, status) ->
                        PermissionItem(
                            permission = permission,
                            status = status,
                            onRequestPermissionClick = {
                                settingsViewModel?.processIntent(
                                    SettingsIntent.RequestPermission(
                                        permission
                                    )
                                )
                            },
                            onOpenSettingsClick = {
                                settingsViewModel?.processIntent(SettingsIntent.OpenAppSettingsClicked)
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    if (uiState.permissionDisplayStatuses.keys.any { it == Manifest.permission.READ_EXTERNAL_STORAGE }) {
                        Text(
                            "The 'Read External Storage' permission is required on older Android versions for the app to select your comic library folder " +
                                    "and scan its contents. If you deny the permission, you won't be able to select a folder. " +
                                    "You can always manage permissions from the App Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                        )
                    }
                }

                // Monitored Folders Section
                Text(
                    text = "Monitored Comic Folders",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                if (uiState.comicsFolders.isEmpty()) {
                    Text(
                        "No comic folders are currently being monitored. Add folders from the main screen.",
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(
                            uiState.comicsFolders.size,
                            key = { it.toString() }) { index ->
                            val folderUri = uiState.comicsFolders[index]
                            ComicsFolderUriItem(
                                folderUri = folderUri,
                                onRemoveClick = {
                                    settingsViewModel?.processIntent(
                                        SettingsIntent.RemoveFolderClicked(
                                            folderUri
                                        )
                                    )
                                }
                            )
                            HorizontalDivider()
                        }
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

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = getPermissionFriendlyNameSettings(permission),
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = getPermissionDescriptionSettings(permission),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (status.isGranted) {
                Text(
                    "Granted",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Button(onClick = onOpenSettingsClick) { Text("Settings") }
            } else {
                Button(onClick = onRequestPermissionClick) {
                    Text("Grant")
                }
            }
        }

        if (status.shouldShowRationale) {
            Text(
                text = getPermissionRationaleSettings(permission),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
            )
        } else if (isEffectivelyPermanentlyDenied) {
            Text(
                text = "Permission denied. To enable this feature, please grant the permission in App Settings.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
            )
        }
    }
}

@Composable
fun ComicsFolderUriItem(
    folderUri: Uri,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val decodedPath = remember(folderUri) {
            try {
                Uri.decode(folderUri.toString())
            } catch (ex: Exception) {
                ex.printStackTrace()
                folderUri.toString() // Fallback to raw string
            }
        }
        Text(
            text = decodedPath,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
        IconButton(onClick = onRemoveClick) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Remove folder",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

fun openAppSettings(context: Context) {
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        context.startActivity(this)
    }
}

@Preview(showBackground = true, name = "Settings Screen Content - Mock State API 29")
@Composable
fun SettingsScreenContentWithMockStateApi29Preview() { // Renamed
    ComiquetaTheme {
        val mockUiState = SettingsUIState(
            comicsFolders = listOf(
                "content://com.android.externalstorage.documents/tree/primary%3ADCIM".toUri(),
                "content://com.android.externalstorage.documents/tree/primary%3ADownload%2FComics".toUri()
            ),
            permissionDisplayStatuses = mapOf(
                Manifest.permission.READ_EXTERNAL_STORAGE to PermissionDisplayStatus(
                    isGranted = true,
                    shouldShowRationale = false
                )
                // You can add more permission statuses here if needed for the preview
                // e.g., Manifest.permission.POST_NOTIFICATIONS to PermissionDisplayStatus(...)
            ),
            isLoading = false,
            // Initialize any other fields of SettingsUIState as needed for your preview
        )

        SettingsScreenContent(
            settingsUIState = mockUiState,
            settingsViewModel = null, // No ViewModel interactions in this specific preview
            navigationViewModel = null
        )
    }
}

@Preview(showBackground = true, name = "Settings Screen Content - Mock State API 33+", apiLevel = 33)
@Composable
fun SettingsScreenContentWithMockStateApi33Preview() { // Added API 33+ version
    ComiquetaTheme {
        val mockUiState = SettingsUIState(
            comicsFolders = listOf(
                "content://com.android.externalstorage.documents/tree/primary%3AMovies".toUri(),
                "content://com.android.externalstorage.documents/tree/primary%3APictures%2FVacation".toUri()
            ),
            permissionDisplayStatuses = mapOf(
                // For API 33+, READ_EXTERNAL_STORAGE might not be relevant if using SAF exclusively
                // Add POST_NOTIFICATIONS if your app targets it and it's relevant for this screen
                // Manifest.permission.POST_NOTIFICATIONS to PermissionDisplayStatus(isGranted = false, shouldShowRationale = true)
            ),
            isLoading = false
        )

        SettingsScreenContent(
            settingsUIState = mockUiState,
            settingsViewModel = null,
            navigationViewModel = null
        )
    }
}


