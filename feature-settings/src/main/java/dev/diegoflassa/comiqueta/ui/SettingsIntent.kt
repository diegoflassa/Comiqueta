package dev.diegoflassa.comiqueta.ui

import android.app.Activity
import android.net.Uri
import dev.diegoflassa.comiqueta.data.model.PermissionDisplayStatus

sealed interface SettingsIntent {
    data object LoadInitialData : SettingsIntent
    data class RefreshPermissionStatuses(val activity: Activity) : SettingsIntent
    data class RequestPermission(val permission: String) : SettingsIntent
    data class PermissionResults(
        val results: Map<String, PermissionDisplayStatus>,
        val activity: Activity
    ) : SettingsIntent

    data class RemoveFolderClicked(val folderUri: Uri) : SettingsIntent
    data object OpenAppSettingsClicked : SettingsIntent
    data object RequestAddFolder : SettingsIntent
    data class FolderSelected(val uri: Uri) : SettingsIntent
    data class OpenFolder(val uri: Uri) : SettingsIntent
    data object NavigateToCategoriesClicked : SettingsIntent
    data class UpdateViewerPagesToPreloadAhead(val count: Int) : SettingsIntent // Added
}
