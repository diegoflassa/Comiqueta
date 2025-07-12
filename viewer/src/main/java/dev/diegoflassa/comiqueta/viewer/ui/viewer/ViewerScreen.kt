package dev.diegoflassa.comiqueta.viewer.ui.viewer

import android.app.Activity
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent

private const val tag = "ViewerScreen"


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel? = null,
    viewerViewModel: ViewerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val viewerUIState: ViewerUIState by viewerViewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResultMap ->
        viewerViewModel.processIntent(ViewerIntent.PermissionResults(permissionsResultMap))
        if (activity != null) {
            viewerViewModel.processIntent(ViewerIntent.RefreshPermissionStatuses(activity))
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewerViewModel.processIntent(ViewerIntent.FolderSelected(uri))
        }
    }

    LaunchedEffect(key1 = Unit) { // Or key1 = settingsViewModel
        viewerViewModel.effect.collect { effect ->
            when (effect) {
                is ViewerEffect.LaunchPermissionRequest -> {
                    permissionLauncher.launch(effect.permissionsToRequest.toTypedArray())
                }

                is ViewerEffect.NavigateToAppViewerScreen -> {
                    //openAppSettings(context)
                }

                is ViewerEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                is ViewerEffect.LaunchFolderPicker -> {
                    folderPickerLauncher.launch(null)
                }
            }
        }
    }


    LaunchedEffect(key1 = activity) {
        if (activity != null) {
            viewerViewModel.processIntent(ViewerIntent.RefreshPermissionStatuses(activity))
        }
    }
    ViewerScreenContent(
        modifier = modifier,
        navigationViewModel = navigationViewModel,
        uiState = viewerUIState
    ) {
        viewerViewModel.processIntent(it)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreenContent(
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel? = null,
    uiState: ViewerUIState = ViewerUIState(),
    onIntent: ((ViewerIntent) -> Unit)? = null
) {
    BackHandler {
        navigationViewModel?.goBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Viewer") },
                navigationIcon = {
                    IconButton(onClick = { navigationViewModel?.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        TimberLogger.logI(tag, paddingValues.toString())
    }
}

@Preview(
    name = "${tag}Empty:360x640",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Preview(
    name = "${tag}Empty:540x1260",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260
)
@Preview(
    name = "${tag}Empty:540x1260 Dark",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "${tag}Empty:540x1260",
    locale = "en-rUS",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260
)
@Preview(
    name = "${tag}Empty:540x1260",
    locale = "de",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260
)
@Composable
fun ViewerScreenContentWithMockStateApi29EmptyPreview() { // Renamed
    ComiquetaThemeContent {
        val mockUiState = ViewerUIState(
            comicsFolders = emptyList(),
            isLoading = true,
        )

        ViewerScreenContent(uiState = mockUiState)
    }
}

@Preview(
    name = "$tag:360x640",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Preview(
    name = "$tag:540x1260",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260
)
@Preview(
    name = "$tag:540x1260 Dark",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "$tag:540x1260",
    locale = "en-rUS",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260
)
@Preview(
    name = "$tag:540x1260",
    locale = "de",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260
)
@Composable
fun SettingsScreenContentWithMockStateApi29Preview() { // Renamed
    ComiquetaThemeContent {
        val mockUiState = ViewerUIState()

        ViewerScreenContent(uiState = mockUiState)
    }
}

@Preview(
    name = "$tag:360x640",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    apiLevel = 33
)
@Preview(
    name = "$tag:540x1260",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260,
    apiLevel = 33
)
@Preview(
    name = "$tag:540x1260 Dark",
    locale = "pt-rBR",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "$tag:540x1260",
    locale = "en-rUS",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260,
    apiLevel = 33
)
@Preview(
    name = "$tag:540x1260",
    locale = "de",
    showBackground = true,
    widthDp = 540,
    heightDp = 1260,
    apiLevel = 33
)
@Composable
fun SettingsScreenContentWithMockStateApi33Preview() {
    ComiquetaThemeContent {
        val mockUiState = ViewerUIState()

        ViewerScreenContent(uiState = mockUiState)
    }
}
