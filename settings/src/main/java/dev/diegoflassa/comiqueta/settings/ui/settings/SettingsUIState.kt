package dev.diegoflassa.comiqueta.settings.ui.settings

/**
 * Represents the complete state of the Settings screen.
 * @param comicsFolders List of monitored comic folders.
 * @param permissionDisplayStatuses Map of permission strings to their current display status.
 * @param isLoading True if initial data is being loaded.
 */
data class SettingsUIState(
    val comicsFolders: List<ComicsFolderEntity> = emptyList(),
    val permissionDisplayStatuses: Map<String, PermissionDisplayStatus> = emptyMap(),
    val isLoading: Boolean = true
)