package dev.diegoflassa.comiqueta.viewer.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface IViewerViewModel {
    val uiState: StateFlow<ViewerUIState>
    val effect: Flow<ViewerEffect>

    fun reduce(intent: ViewerIntent)
}
