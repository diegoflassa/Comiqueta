package dev.diegoflassa.comiqueta.viewer.ui

/**
 * Represents one-time side effects triggered by the ViewerViewModel,
 * to be handled by the View (e.g., showing an error message).
 */
sealed interface ViewerEffect {
    /**
     * Effect to show an error message, likely as a Toast or Snackbar.
     * @param message The error message to display.
     */
    data class ShowError(val message: String) : ViewerEffect
}
