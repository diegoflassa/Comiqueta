package dev.diegoflassa.comiqueta.viewer.ui.viewer

/**
 * Represents one-time side effects triggered by the ViewModel, to be handled by the View.
 */
sealed interface ViewerEffect {
    data class LaunchPermissionRequest(val permissionsToRequest: List<String>) : ViewerEffect
    data object NavigateToAppViewerScreen : ViewerEffect
    data class ShowToast(val message: String) : ViewerEffect
    data object LaunchFolderPicker : ViewerEffect
}