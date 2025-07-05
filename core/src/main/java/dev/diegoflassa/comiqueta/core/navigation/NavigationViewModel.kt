package dev.diegoflassa.comiqueta.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(NavigationUIState()) // Uses NavigationUIState
    val state: StateFlow<NavigationUIState> = _state.asStateFlow() // Uses NavigationUIState

    private val _effect = MutableSharedFlow<NavigationEffect>()
    val effect: SharedFlow<NavigationEffect> = _effect.asSharedFlow()

    /**
     * Processes the given navigation intent and updates the state accordingly.
     * This is the central logic for handling all navigation actions.
     *
     * @param intent The navigation intent to process.
     */
    fun processIntent(intent: NavigationIntent) {
        _state.update { currentState -> // currentState is of type NavigationUIState
            when (intent) {
                is NavigationIntent.NavigateTo -> {
                    // Avoid adding duplicate screen if it's already at the top
                    if (currentState.backStack.lastOrNull() != intent.screen) {
                        currentState.copy(backStack = currentState.backStack + intent.screen)
                    } else {
                        // Optional: Notify the user if they are already on the target screen.
                        viewModelScope.launch { _effect.emit(NavigationEffect.ShowToast("Already on this screen")) }
                        currentState // No change
                    }
                }
                is NavigationIntent.GoToHome -> {
                    // Navigate to Home.
                    // Current logic adds Home to the stack if not already at the top.
                    // Consider if a true "clear stack and go home" is needed, e.g.,
                    // currentState.copy(backStack = listOf(Screen.Home))
                    if (currentState.backStack.lastOrNull() != Screen.Home) {
                        currentState.copy(backStack = currentState.backStack + Screen.Home)
                    } else {
                        currentState // No change
                    }
                }
                is NavigationIntent.GoBack -> {
                    // Go back if there's more than one screen in the stack
                    if (currentState.backStack.size > 1) {
                        currentState.copy(backStack = currentState.backStack.dropLast(1))
                    } else {
                        // Emit an effect because we can't go back further
                        viewModelScope.launch {
                            _effect.emit(NavigationEffect.ShowToast("Cannot go back further"))
                        }
                        currentState // Cannot go back from the initial screen, no state change
                    }
                }
                is NavigationIntent.ReplaceAll -> {
                    // Replace the entire back stack with the new screen
                    currentState.copy(backStack = listOf(intent.screen))
                }
            }
        }
    }

    // --- Convenience functions that dispatch intents ---
    // These maintain a similar API to the previous version for ease of use from the UI,
    // but now delegate to the MVI pattern's intent processing.

    /**
     * Navigates to a new screen by sending a [NavigationIntent.NavigateTo].
     * Adds the screen to the top of the back stack.
     *
     * @param screen The destination screen to navigate to.
     */
    fun navigateTo(screen: Screen) {
        processIntent(NavigationIntent.NavigateTo(screen))
    }

    /**
     * Goes to the Home screen by sending a [NavigationIntent.GoToHome].
     * (Current implementation adds Home to stack if not already at top).
     */
    fun goToHome() {
        processIntent(NavigationIntent.GoToHome)
    }

    /**
     * Goes back to the previous screen by sending a [NavigationIntent.GoBack].
     * Removes the current screen from the top of the back stack.
     * If already at the root, an effect is emitted.
     */
    fun goBack() {
        processIntent(NavigationIntent.GoBack)
    }

    /**
     * Clears the entire back stack and navigates to the given screen
     * by sending a [NavigationIntent.ReplaceAll].
     *
     * @param screen The screen to become the new single entry in the back stack.
     */
    fun replaceAll(screen: Screen) {
        processIntent(NavigationIntent.ReplaceAll(screen))
    }
}
