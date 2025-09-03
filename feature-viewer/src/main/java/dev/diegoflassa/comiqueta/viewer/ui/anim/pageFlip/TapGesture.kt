package dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.config.PageCurlConfig
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.utils.multiply
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A [Modifier] that handles tap gestures for page navigation (forward/backward) in a page curl animation.
 * It allows defining specific tap targets for forward and backward navigation, and also supports a custom
 * tap handler.
 *
 * @param config The [PageCurlConfig] containing tap interaction settings, including enabled states,
 *               tap targets, and custom tap callbacks.
 * @param scope The [CoroutineScope] in which to launch the navigation actions.
 * @param onTapForward A suspend function to be invoked when a tap occurs in the forward navigation target area.
 * @param onTapBackward A suspend function to be invoked when a tap occurs in the backward navigation target area.
 * @return A [Modifier] that listens for tap gestures and triggers the appropriate navigation actions.
 */
internal fun Modifier.tapGesture(
    config: PageCurlConfig,
    scope: CoroutineScope,
    onTapForward: suspend () -> Unit,
    onTapBackward: suspend () -> Unit,
): Modifier = pointerInput(config) {
    val tapInteraction = config.tapInteraction as? PageCurlConfig.TargetTapInteraction ?: return@pointerInput

    awaitEachGesture {
        val down = awaitFirstDown().also { it.consume() }
        val up = waitForUpOrCancellation() ?: return@awaitEachGesture

        // Check if it was a real tap (not a drag)
        if ((down.position - up.position).getDistance() > viewConfiguration.touchSlop) {
            return@awaitEachGesture
        }

        // Handle custom tap first if enabled
        if (config.tapCustomEnabled && config.onCustomTap(this, size, up.position)) {
            return@awaitEachGesture
        }

        // Handle forward tap
        if (config.tapForwardEnabled && tapInteraction.forward.target.multiply(size).contains(up.position)) {
            scope.launch {
                onTapForward()
            }
            return@awaitEachGesture
        }

        // Handle backward tap
        if (config.tapBackwardEnabled && tapInteraction.backward.target.multiply(size).contains(up.position)) {
            scope.launch {
                onTapBackward()
            }
            return@awaitEachGesture
        }
    }
}
