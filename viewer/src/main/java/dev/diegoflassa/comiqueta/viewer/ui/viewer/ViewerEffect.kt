package dev.diegoflassa.comiqueta.viewer.ui.viewer

/**
 * Represents one-time side effects triggered by the ViewerViewModel,
 * to be handled by the View (e.g., showing a toast message).
 */
sealed interface ViewerEffect {
    /**
     * Effect to show a toast message.
     * @param message The text message to display in the toast.
     */
    data class ShowToast(val message: String) : ViewerEffect
}
