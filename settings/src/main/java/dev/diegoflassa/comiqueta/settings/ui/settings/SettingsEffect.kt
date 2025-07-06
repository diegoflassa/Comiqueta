package dev.diegoflassa.comiqueta.settings.ui.settings

/**
 * Represents one-time side effects triggered by the ViewModel, to be handled by the View.
 */
sealed interface SettingsEffect {
    data class LaunchPermissionRequest(val permissionsToRequest: List<String>) : SettingsEffect
    data object NavigateToAppSettingsScreen : SettingsEffect
    data class ShowToast(val message: String) : SettingsEffect
    data object LaunchFolderPicker : SettingsEffect
}