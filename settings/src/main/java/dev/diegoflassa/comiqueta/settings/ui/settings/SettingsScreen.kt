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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    LaunchedEffect(key1 = Unit) {
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

@Preview(showBackground = true, name = "Settings Screen - Default")
@Composable
fun SettingsScreenMviDefaultPreview() {
    val uri1 = "file://storage/emulated/0/DCIM/".toUri()
    val uri2 = "file://storage/emulated/0/Download/Comics/".toUri()

    val dummyComicsFolderUris = listOf(uri1, uri2)

    ComiquetaTheme {
        val initialDummyState = SettingsUIState(
            comicsFolders = dummyComicsFolderUris,
            permissionDisplayStatuses = mapOf(
                Manifest.permission.READ_EXTERNAL_STORAGE to PermissionDisplayStatus(
                    isGranted = true,
                    shouldShowRationale = false
                )
            ),
            isLoading = false
        )
        SettingsScreenContent(settingsUIState = initialDummyState)
    }
}


@Preview(showBackground = true, name = "Settings Screen - API 33", apiLevel = 33)
@Composable
fun SettingsScreenMviApi33Preview() {
    ComiquetaTheme {
        val initialDummyStateApi33 = SettingsUIState(
            comicsFolders = emptyList(),
            permissionDisplayStatuses = emptyMap(),
            isLoading = false
        )
        SettingsScreenContent(settingsUIState = initialDummyStateApi33)
    }
}
