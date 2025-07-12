package dev.diegoflassa.comiqueta.viewer.ui.viewer

import android.app.Activity
import android.net.Uri

/**
 * Represents user actions or events that can modify the state or trigger effects.
 */
sealed interface ViewerIntent {
    data object LoadInitialData : ViewerIntent
    data class RefreshPermissionStatuses(val activity: Activity) : ViewerIntent
    data class RequestPermission(val permission: String) : ViewerIntent
    data class PermissionResults(val results: Map<String, Boolean>) : ViewerIntent
    data class RemoveFolderClicked(val folderUri: Uri) : ViewerIntent
    data object OpenAppViewerClicked : ViewerIntent
    data object RequestAddFolder : ViewerIntent
    data class FolderSelected(val uri: Uri) : ViewerIntent
}