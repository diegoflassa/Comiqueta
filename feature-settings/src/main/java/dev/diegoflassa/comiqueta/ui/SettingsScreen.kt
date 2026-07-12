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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items // Added for comicsFolders
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Remove
// import androidx.compose.material.icons.filled.Search // Example for Rescan
// import androidx.compose.material.icons.filled.DeleteSweep // Example for Clear DB
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dev.diegoflassa.comiqueta.settings.R
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel
import dev.diegoflassa.comiqueta.data.model.PermissionDisplayStatus

private const val tag = "SettingsScreen"
private const val MAX_PRELOAD_PAGES = 5

@Composable
private fun getPermissionFriendlyNameSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE -> stringResource(R.string.permission_storage_access_title)
        else -> permission.substringAfterLast('.').replace('_', ' ').let {
            it.lowercase()
                .replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
        }
    }
}

@Composable
private fun getPermissionDescriptionSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE -> stringResource(R.string.permission_storage_access_desc)
        else -> stringResource(R.string.permission_required_desc)
    }
}

@Composable
private fun getPermissionRationaleSettings(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE -> stringResource(R.string.permission_storage_access_rationale)
        else -> stringResource(R.string.permission_important_rationale)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel? = hiltActivityViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    TimberLogger.logI(tag, "[Comiqueta][Settings] SettingsScreen")
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
    
    val folderAddFailedTemplate = stringResource(R.string.folder_add_failed)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                TimberLogger.logD(tag, "[Comiqueta][Settings] Persistable URI permission granted for $uri")
                settingsViewModel.processIntent(SettingsIntent.FolderSelected(uri))
            } catch (se: SecurityException) {
                FirebaseCrashlytics.getInstance().recordException(se)
                TimberLogger.logE(tag, "[Comiqueta][Settings] Failed to take persistable URI permission for $uri", se)
                Toast.makeText(
                    context,
                    folderAddFailedTemplate.format(uri.toString()),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    val noAppToOpenFolder = stringResource(R.string.no_app_to_open_folder)
    
    // Confirmation Dialog State - must be declared before LaunchedEffect that uses it
    var confirmationDialogState by remember { mutableStateOf<SettingsEffect.ShowConfirmationDialog?>(null) }
    
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
                        FirebaseCrashlytics.getInstance().recordException(anfe)
                        Toast.makeText(
                            context,
                            noAppToOpenFolder,
                            Toast.LENGTH_SHORT
                        ).show()
                        TimberLogger.logE(
                            tag,
                            "[Comiqueta][Settings] No activity found to handle folder URI: ${effect.folderUri}",
                            anfe
                        )
                    }
                }

                is SettingsEffect.NavigateToCategoriesScreen -> {
                    navigationViewModel?.navigateToCategories()
                }

                is SettingsEffect.ShowConfirmationDialog -> {
                    confirmationDialogState = effect
                }
            }
        }
    }

    // Display Confirmation Dialog
    confirmationDialogState?.let { dialog ->
        AlertDialog(
            onDismissRequest = { confirmationDialogState = null },
            title = { Text(dialog.title) },
            text = { Text(dialog.message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmationDialogState = null
                        settingsViewModel.processIntent(dialog.confirmIntent)
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmationDialogState = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
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
                .padding(horizontal = ComiquetaTheme.dimen.paddingLarge.scaled()),
        ) {
            permissionsSection(
                uiState = uiState,
                onIntent = onIntent,
                getPermissionFriendlyName = { getPermissionFriendlyNameSettings(it) },
                getPermissionDescription = { getPermissionDescriptionSettings(it) },
                getPermissionRationale = { getPermissionRationaleSettings(it) }
            )

            monitoredFoldersSection(
                uiState = uiState,
                onIntent = onIntent
            )

            item { Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerMedium.scaled())) }

            viewerSettingsSection(
                uiState = uiState,
                onIntent = onIntent
            )

            manageCategoriesSection(
                onIntent = onIntent
            )

            dataManagementSection(
                onIntent = onIntent
            )

            privacySection(
                onIntent = onIntent
            )

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

private fun LazyListScope.permissionsSection(
    uiState: SettingsUIState,
    onIntent: ((SettingsIntent) -> Unit)?,
    getPermissionFriendlyName: @Composable (String) -> String,
    getPermissionDescription: @Composable (String) -> String,
    getPermissionRationale: @Composable (String) -> String
) {
    item { // Permissions Section Title
        Text(
            text = stringResource(R.string.settings_section_permissions_title),
            style = ComiquetaTheme.typography.typography.titleLarge,
            modifier = Modifier.padding(top = ComiquetaTheme.dimen.paddingLarge.scaled(), bottom = ComiquetaTheme.dimen.paddingSmall.scaled())
        )
    }
    if (uiState.permissionDisplayStatuses.isEmpty()) {
        item {
            Text(
                stringResource(R.string.settings_permissions_none_required),
                modifier = Modifier.padding(vertical = ComiquetaTheme.dimen.paddingSmall.scaled()),
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
                    onOpenSettingsClick = { onIntent?.invoke(SettingsIntent.OpenAppSettingsClicked) },
                    friendlyName = getPermissionFriendlyName(permission),
                    description = getPermissionDescription(permission),
                    rationale = getPermissionRationale(permission)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = ComiquetaTheme.dimen.paddingSmall.scaled()))
            }
        }
        if (uiState.permissionDisplayStatuses.keys.any { it == Manifest.permission.READ_EXTERNAL_STORAGE }) {
            item {
                Text(
                    stringResource(R.string.settings_permission_read_external_storage_rationale_extended),
                    style = ComiquetaTheme.typography.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = ComiquetaTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        top = ComiquetaTheme.dimen.paddingSmall.scaled(),
                        bottom = ComiquetaTheme.dimen.paddingLarge.scaled()
                    )
                )
            }
        }
    }
}

private fun LazyListScope.monitoredFoldersSection(
    uiState: SettingsUIState,
    onIntent: ((SettingsIntent) -> Unit)?
) {
    item { // Monitored Folders Section Title
        Text(
            text = stringResource(R.string.settings_section_monitored_folders_title),
            style = ComiquetaTheme.typography.typography.titleLarge,
            modifier = Modifier.padding(top = ComiquetaTheme.dimen.paddingLarge.scaled(), bottom = ComiquetaTheme.dimen.paddingSmall.scaled())
        )
    }
    // Button to add new folder
    item {
        Button(
            onClick = { onIntent?.invoke(SettingsIntent.AddFolderClicked) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_add_folder_button))
        }
        Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerSmall.scaled()))
    }

    if (uiState.comicsFolders.isEmpty()) {
        item {
            Text(
                stringResource(R.string.settings_monitored_folders_empty),
                modifier = Modifier.padding(vertical = ComiquetaTheme.dimen.paddingSmall.scaled()),
                textAlign = TextAlign.Center
            )
        }
    } else {
        items(
            uiState.comicsFolders, // Directly use the list
            key = { folderUri -> folderUri.toString() }
        ) { folderUri ->
            ComicsFolderUriItem(
                folderUri = folderUri,
                onIntent = onIntent
            )
            HorizontalDivider()
        }
    }
}

private fun LazyListScope.viewerSettingsSection(
    uiState: SettingsUIState,
    onIntent: ((SettingsIntent) -> Unit)?
) {
    item {
        Text(
            text = stringResource(R.string.settings_section_viewer_title),
            style = ComiquetaTheme.typography.typography.titleLarge,
            modifier = Modifier.padding(top = ComiquetaTheme.dimen.paddingLarge.scaled(), bottom = ComiquetaTheme.dimen.paddingSmall.scaled())
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_viewer_preload_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_viewer_preload_description, MAX_PRELOAD_PAGES)) },
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
                                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.settings_viewer_preload_decrease_desc))
                            }
                            Text(
                                text = uiState.viewerPagesToPreloadAhead.toString(),
                                style = ComiquetaTheme.typography.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = ComiquetaTheme.dimen.paddingSmall.scaled())
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
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.settings_viewer_preload_increase_desc))
                            }
                        }
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = ComiquetaTheme.dimen.paddingMedium.scaled()),
                    thickness = 0.5.dp,
                    color = ComiquetaTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_webtoon_mode_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_webtoon_mode_desc)) },
                    trailingContent = {
                        Switch(
                            checked = uiState.isWebtoonMode,
                            onCheckedChange = { onIntent?.invoke(SettingsIntent.UpdateWebtoonMode(it)) }
                        )
                    },
                    modifier = Modifier.clickable { onIntent?.invoke(SettingsIntent.UpdateWebtoonMode(!uiState.isWebtoonMode)) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = ComiquetaTheme.dimen.paddingMedium.scaled()),
                    thickness = 0.5.dp,
                    color = ComiquetaTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_double_page_mode_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_double_page_mode_desc)) },
                    trailingContent = {
                        Switch(
                            checked = uiState.isDoublePageView,
                            onCheckedChange = { onIntent?.invoke(SettingsIntent.UpdateDoublePageView(it)) }
                        )
                    },
                    modifier = Modifier.clickable { onIntent?.invoke(SettingsIntent.UpdateDoublePageView(!uiState.isDoublePageView)) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = ComiquetaTheme.dimen.paddingMedium.scaled()),
                    thickness = 0.5.dp,
                    color = ComiquetaTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_page_flip_sound_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_page_flip_sound_desc)) },
                    trailingContent = {
                        Switch(
                            checked = uiState.isPageFlipSoundEnabled,
                            onCheckedChange = { onIntent?.invoke(SettingsIntent.UpdatePageFlipSoundEnabled(it)) }
                        )
                    },
                    modifier = Modifier.clickable { onIntent?.invoke(SettingsIntent.UpdatePageFlipSoundEnabled(!uiState.isPageFlipSoundEnabled)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerMedium.scaled()))
    }
}

private fun LazyListScope.manageCategoriesSection(
    onIntent: ((SettingsIntent) -> Unit)?
) {
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
        Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerMedium.scaled()))
    }
}

private fun LazyListScope.dataManagementSection(
    onIntent: ((SettingsIntent) -> Unit)?
) {
    item {
        Text(
            text = stringResource(R.string.settings_section_data_management_title),
            style = ComiquetaTheme.typography.typography.titleLarge,
            modifier = Modifier.padding(top = ComiquetaTheme.dimen.paddingLarge.scaled(), bottom = ComiquetaTheme.dimen.paddingSmall.scaled())
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_data_clear_db_title)) },
                supportingContent = { Text(stringResource(R.string.settings_data_clear_db_desc)) },
                modifier = Modifier.clickable { onIntent?.invoke(SettingsIntent.ClearLocalDatabaseClicked) }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_data_rescan_folders_title)) },
                supportingContent = { Text(stringResource(R.string.settings_data_rescan_folders_desc)) },
                modifier = Modifier.clickable { onIntent?.invoke(SettingsIntent.RescanComicFoldersClicked) }
            )
        }
        Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerMedium.scaled()))
    }
}

private fun LazyListScope.privacySection(
    onIntent: ((SettingsIntent) -> Unit)?
) {
    item {
        val context = LocalContext.current
        val activity = context as? Activity
        val errorLoadingPrivacyTemplate = stringResource(R.string.settings_error_loading_privacy_settings)
        val errorCouldNotOpenPrivacy = stringResource(R.string.settings_error_could_not_open_privacy_settings)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    activity?.let { currentActivity ->
                        TimberLogger.logD(tag, "[Comiqueta][Settings] Showing privacy options form.")
                        UserMessagingPlatform.showPrivacyOptionsForm(currentActivity) { formError ->
                            if (formError != null) {
                                TimberLogger.logE(
                                    tag,
                                    "[Comiqueta][Settings] Error showing privacy options form: ${formError.message}",
                                    Exception("${formError.errorCode}-${formError.message}")
                                )
                                Toast.makeText(
                                    currentActivity,
                                    errorLoadingPrivacyTemplate.format(formError.errorCode, formError.message),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } ?: run {
                        TimberLogger.logW(
                            tag,
                            "[Comiqueta][Settings] Activity context not available for showing privacy options form."
                        )
                        Toast.makeText(
                            context,
                            errorCouldNotOpenPrivacy,
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
        Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.spacerMedium.scaled()))
    }
}

@Composable
fun PermissionItem(
    permission: String,
    status: PermissionDisplayStatus,
    onRequestPermissionClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    friendlyName: String,
    description: String,
    rationale: String
) {
    val isEffectivelyPermanentlyDenied = !status.isGranted && !status.shouldShowRationale

    Column(modifier = Modifier.padding(vertical = ComiquetaTheme.dimen.paddingSmall.scaled())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = ComiquetaTheme.dimen.paddingSmall.scaled())
            ) {
                Text(
                    text = friendlyName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp.scaled()
                )
                Text(
                    text = description,
                    fontSize = 12.sp.scaled(),
                    color = ComiquetaTheme.colorScheme.onSurfaceVariant
                )
            }

            if (status.isGranted) {
                Text(
                    stringResource(R.string.settings_permission_status_granted),
                    color = ComiquetaTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = ComiquetaTheme.dimen.paddingSmall.scaled())
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
                text = rationale,
                fontSize = 12.sp.scaled(),
                color = ComiquetaTheme.colorScheme.tertiary,
                modifier = Modifier.padding(
                    top = ComiquetaTheme.dimen.paddingExtraSmall.scaled(),
                    start = ComiquetaTheme.dimen.paddingSmall.scaled(),
                    end = ComiquetaTheme.dimen.paddingSmall.scaled()
                )
            )
        } else if (isEffectivelyPermanentlyDenied) {
            Text(
                text = stringResource(R.string.settings_permission_denied_permanently_message),
                fontSize = 12.sp.scaled(),
                color = ComiquetaTheme.colorScheme.error,
                modifier = Modifier.padding(
                    top = ComiquetaTheme.dimen.paddingExtraSmall.scaled(),
                    start = ComiquetaTheme.dimen.paddingSmall.scaled(),
                    end = ComiquetaTheme.dimen.paddingSmall.scaled()
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
    // Attempt to decode the path, but gracefully handle if it's already decoded or malformed
    val decodedPath = remember(path) {
        try {
            Uri.decode(path)
        } catch (e: IllegalArgumentException) {
            TimberLogger.logW(tag, "[Comiqueta][Settings] Failed to decode path: $path", e)
            path // Fallback to the original path if decoding fails
        }
    }


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onIntent?.invoke(SettingsIntent.OpenFolder(folderUri))
            }
            .padding(vertical = ComiquetaTheme.dimen.paddingMedium.scaled()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = decodedPath,
            modifier = Modifier
                .weight(1f)
                .padding(end = ComiquetaTheme.dimen.paddingSmall.scaled()),
            overflow = TextOverflow.Ellipsis,
            maxLines = 2, // Allow up to 2 lines for longer paths
            style = ComiquetaTheme.typography.typography.bodyMedium
        )
        IconButton(onClick = { onIntent?.invoke(SettingsIntent.RemoveFolderClicked(folderUri)) }) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.settings_remove_folder_action_desc),
                tint = ComiquetaTheme.colorScheme.error
            )
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.fromParts("package", context.packageName, null)
    context.startActivity(intent)
}


// --- Previews ---
@Preview(name = "SettingsScreenContent · With Data · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SettingsScreenContent · With Data · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SettingsScreenContentWithDataPreview() {
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
                viewerPagesToPreloadAhead = 1
            )
        )
    }
}

@Preview(name = "SettingsScreenContent · With Data · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenContentWithDataDarkPreview() {
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
                viewerPagesToPreloadAhead = 1
            )
        )
    }
}

@Preview(name = "SettingsScreenContent · Empty · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SettingsScreenContent · Empty · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SettingsScreenContentEmptyPreview() {
    ComiquetaThemeContent {
        SettingsScreenContent(
            uiState = SettingsUIState(
                isLoading = false,
                permissionDisplayStatuses = emptyMap(),
                comicsFolders = emptyList(),
                viewerPagesToPreloadAhead = 0
            )
        )
    }
}

@Preview(name = "SettingsScreenContent · Empty · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenContentEmptyDarkPreview() {
    ComiquetaThemeContent {
        SettingsScreenContent(
            uiState = SettingsUIState(
                isLoading = false,
                permissionDisplayStatuses = emptyMap(),
                comicsFolders = emptyList(),
                viewerPagesToPreloadAhead = 0
            )
        )
    }
}

@Preview(name = "SettingsScreenContent · Loading · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SettingsScreenContent · Loading · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SettingsScreenContentLoadingPreview() {
    ComiquetaThemeContent {
        SettingsScreenContent(
            uiState = SettingsUIState(
                isLoading = true,
                permissionDisplayStatuses = emptyMap(),
                comicsFolders = emptyList(),
                viewerPagesToPreloadAhead = 1
            )
        )
    }
}

@Preview(name = "SettingsScreenContent · Loading · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenContentLoadingDarkPreview() {
    ComiquetaThemeContent {
        SettingsScreenContent(
            uiState = SettingsUIState(
                isLoading = true,
                permissionDisplayStatuses = emptyMap(),
                comicsFolders = emptyList(),
                viewerPagesToPreloadAhead = 1
            )
        )
    }
}
