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
import androidx.lifecycle.viewModelScope
// import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsFoldersDao // Removed
import dev.diegoflassa.comiqueta.core.data.repository.ComicsFolderRepository
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

// Definitions for State and Effects - ASSUMING SettingsUIState WILL CHANGE
// data class ComicsFolderEntity(val folderPath: Uri) // Simplified for this example if original is gone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel? = null,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

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
                                settingsViewModel.processIntent(
                                    SettingsIntent.RequestPermission(
                                        permission
                                    )
                                )
                            },
                            onOpenSettingsClick = {
                                settingsViewModel.processIntent(SettingsIntent.OpenAppSettingsClicked)
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
                        items(uiState.comicsFolders.size, key = { it.toString() }) { index ->
                            val folderUri = uiState.comicsFolders[index]
                            ComicsFolderUriItem( // Renamed for clarity, accepts Uri
                                folderUri = folderUri,
                                onRemoveClick = {
                                    // Assuming SettingsIntent.RemoveFolderClicked now takes a Uri
                                    settingsViewModel.processIntent(
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
fun ComicsFolderUriItem( // Renamed and changed to accept Uri
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
    ComiquetaTheme {
        val currentContext = LocalContext.current
        val dummyComicsFolderUris = listOf( // Now List<Uri>
            "content://com.android.externalstorage.documents/tree/primary%3ADCIM".toUri(),
            "content://com.android.externalstorage.documents/tree/primary%3ADownload%2FComics".toUri()
        )
        val initialDummyState = SettingsUIState( // Renamed for clarity for initial state
            comicsFolders = dummyComicsFolderUris, // Using List<Uri>
            permissionDisplayStatuses = mapOf(
                Manifest.permission.READ_EXTERNAL_STORAGE to PermissionDisplayStatus(
                    isGranted = true,
                    shouldShowRationale = false
                )
            ),
            isLoading = false
        )

        val mockComicsFolderRepository = object : ComicsFolderRepository(currentContext) {
            // Using a var to hold the list so releasePersistablePermission can modify it for the preview
            private var currentPersistedUris: MutableList<Uri> =
                dummyComicsFolderUris.toMutableList()

            override fun getPersistedPermissions(): List<Uri> {
                return currentPersistedUris.toList() // Return a copy
            }

            override fun releasePersistablePermission(uri: Uri, flags: Int): Boolean {
                val removed = currentPersistedUris.remove(uri)
                if (removed) {
                    // Simulate the repository successfully releasing permission
                    return true
                }
                return false // URI wasn't in the list or couldn't be removed
            }

            // Add takePersistablePermission mock if your preview adds folders
            override fun takePersistablePermission(uri: Uri, flags: Int): Boolean {
                if (!currentPersistedUris.contains(uri)) {
                    currentPersistedUris.add(uri)
                }
                return true // Simulate success
            }
        }

        val previewViewModel = object : SettingsViewModel(
            comicsFolderRepository = mockComicsFolderRepository,
            // comicsFoldersDao = /* REMOVED */,
            applicationContext = currentContext
        ) {
            // This is the MutableStateFlow that backs the uiState
            private val _previewUiState = MutableStateFlow(initialDummyState)
            override val uiState: StateFlow<SettingsUIState> =
                _previewUiState.asStateFlow() // Expose as StateFlow

            override val effect: Flow<SettingsEffect> =
                flowOf() // No effects for this simple preview

            // Mocking processIntent to update the _previewUiState
            override fun processIntent(intent: SettingsIntent) {
                // We are overriding the ViewModel's processIntent, so no call to super is needed here
                // unless the base ViewModel has some abstract behavior we want to trigger.
                // For this preview, we handle specific intents directly.
                when (intent) {
                    is SettingsIntent.RemoveFolderClicked -> {
                        viewModelScope.launch { // Simulate ViewModel's coroutine scope
                            // Tell the mock repository to release permission
                            val released = mockComicsFolderRepository.releasePersistablePermission(
                                intent.folderUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION // Assuming this flag
                            )
                            if (released) {
                                // If released, update the UI state by getting the new list from the mock repo
                                val updatedFolders =
                                    mockComicsFolderRepository.getPersistedPermissions()
                                _previewUiState.value =
                                    _previewUiState.value.copy(comicsFolders = updatedFolders)
                                // Optionally send a toast effect for the preview
                                // _effect.send(SettingsEffect.ShowToast("Folder removed (Preview)"))
                            } else {
                                // Optionally send a toast effect for failure
                                // _effect.send(SettingsEffect.ShowToast("Failed to remove folder (Preview)"))
                            }
                        }
                    }

                    is SettingsIntent.LoadInitialData -> {
                        viewModelScope.launch {
                            _previewUiState.value = _previewUiState.value.copy(isLoading = true)
                            val folders = mockComicsFolderRepository.getPersistedPermissions()
                            // Also update permissionDisplayStatuses if they can change in preview
                            _previewUiState.value = _previewUiState.value.copy(
                                comicsFolders = folders,
                                isLoading = false
                                // permissionDisplayStatuses = ... // if needed for preview
                            )
                        }
                    }
                    // Handle other intents if necessary for the preview's behavior
                    else -> {
                        // Log.d("PreviewViewModel", "Unhandled intent: $intent")
                    }
                }
            }
        }
        SettingsScreen(settingsViewModel = previewViewModel)
    }
}

@Preview(showBackground = true, name = "Settings Screen - API 33", apiLevel = 33)
@Composable
fun SettingsScreenMviApi33Preview() {
    ComiquetaTheme {
        val currentContext = LocalContext.current
        val initialDummyStateApi33 = SettingsUIState( // Renamed for clarity
            comicsFolders = emptyList(), // List<Uri>
            permissionDisplayStatuses = emptyMap(), // No legacy permissions expected on API 33+
            isLoading = false
        )

        val mockComicsFolderRepositoryApi33 = object : ComicsFolderRepository(currentContext) {
            override fun getPersistedPermissions(): List<Uri> {
                return emptyList() // No folders initially
            }

            // Mocks for take/release if your API 33 preview interacts with adding/removing
            override fun releasePersistablePermission(uri: Uri, flags: Int): Boolean = true
            override fun takePersistablePermission(uri: Uri, flags: Int): Boolean = true
        }

        val previewViewModel = object : SettingsViewModel(
            comicsFolderRepository = mockComicsFolderRepositoryApi33,
            // comicsFoldersDao = /* REMOVED */,
            applicationContext = currentContext
        ) {
            private val _previewUiState = MutableStateFlow(initialDummyStateApi33)
            override val uiState: StateFlow<SettingsUIState> = _previewUiState.asStateFlow()
            override val effect: Flow<SettingsEffect> = flowOf()

            override fun processIntent(intent: SettingsIntent) {
                // Similar to the default preview, handle intents as needed for API 33 scenario
                when (intent) {
                    is SettingsIntent.LoadInitialData -> {
                        viewModelScope.launch {
                            _previewUiState.value = _previewUiState.value.copy(isLoading = true)
                            val folders = mockComicsFolderRepositoryApi33.getPersistedPermissions()
                            _previewUiState.value = _previewUiState.value.copy(
                                comicsFolders = folders,
                                isLoading = false,
                                permissionDisplayStatuses = emptyMap() // Explicitly empty for API 33+
                            )
                        }
                    }
                    // Handle RemoveFolderClicked if applicable to this preview's test case
                    else -> {
                        // Log.d("PreviewViewModelApi33", "Unhandled intent: $intent")
                    }
                }
            }
        }
        SettingsScreen(settingsViewModel = previewViewModel)
    }
}
