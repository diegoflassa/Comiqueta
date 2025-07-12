package dev.diegoflassa.comiqueta.settings.ui.settings

import android.app.Activity
import android.net.Uri

/**
 * Represents user actions or events that can modify the state or trigger effects.
 */
sealed interface SettingsIntent {
    data object LoadInitialData : SettingsIntent
    data class RefreshPermissionStatuses(val activity: Activity) : SettingsIntent
    data class RequestPermission(val permission: String) : SettingsIntent
    data class PermissionResults(val results: Map<String, Boolean>) : SettingsIntent
    data class RemoveFolderClicked(val folderUri: Uri) : SettingsIntent
    data object OpenAppSettingsClicked : SettingsIntent
    data object RequestAddFolder : SettingsIntent
    data class FolderSelected(val uri: Uri) : SettingsIntent
    data class OpenFolder(val uri: Uri) : SettingsIntent
    data object NavigateToCategoriesClicked : SettingsIntent // Added this line
}