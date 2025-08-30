package dev.diegoflassa.comiqueta.ui

import android.net.Uri
import dev.diegoflassa.comiqueta.core.data.preferences.PreferencesKeys
import dev.diegoflassa.comiqueta.data.model.PermissionDisplayStatus

/**
 * Represents the complete state of the Settings screen.
 * @param comicsFolders List of monitored comic folders.
 * @param permissionDisplayStatuses Map of permission strings to their current display status.
 * @param isLoading True if initial data is being loaded.
 * @param viewerPagesToPreloadAhead Number of pages to preload in the comic viewer.
 */
data class SettingsUIState(
    val comicsFolders: List<Uri> = emptyList(),
    val permissionDisplayStatuses: Map<String, PermissionDisplayStatus> = emptyMap(),
    val isLoading: Boolean = true,
    val viewerPagesToPreloadAhead: Int = PreferencesKeys.DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD
)
