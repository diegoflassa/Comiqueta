package dev.diegoflassa.comiqueta.viewer.ui.viewer

import android.net.Uri

/**
 * Represents the complete state of the Settings screen.
 * @param comicsFolders List of monitored comic folders.
 * @param isLoading True if initial data is being loaded.
 */
data class ViewerUIState(
    val comicsFolders: List<Uri> = emptyList(),
    val isLoading: Boolean = true
)