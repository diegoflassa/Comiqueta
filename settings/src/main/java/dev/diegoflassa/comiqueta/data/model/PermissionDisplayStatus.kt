package dev.diegoflassa.comiqueta.data.model

/**
 * Represents the display status of an OS permission.
 *
 * @param isGranted True if the permission is currently granted.
 * @param shouldShowRationale True if a rationale should be shown to the user before requesting again.
 */
data class PermissionDisplayStatus(
    val isGranted: Boolean,
    val shouldShowRationale: Boolean
)
