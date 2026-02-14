package dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.config.PageCurlConfig
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.utils.multiply
import kotlinx.coroutines.CoroutineScope
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import kotlinx.coroutines.launch

private const val TAG = "TapGesture"

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
        // Use Initial pass to see events before Zoom logic consumes them
        val down = awaitFirstDown(pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial, requireUnconsumed = false)
        TimberLogger.logI(TAG, "[PageNavFix] Down detected at ${down.position} (Initial pass)")
        
        var up: androidx.compose.ui.input.pointer.PointerInputChange? = null
        try {
            // Wait for up or cancellation using Initial pass manually
            while (true) {
                val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null) {
                    // Pointer lost (cancelled?)
                    TimberLogger.logI(TAG, "[PageNavFix] Pointer lost")
                    return@awaitEachGesture
                }
                
                if (!change.pressed) {
                    up = change
                    break
                }
                
                if (change.isConsumed) {
                     // Even if consumed (by Zoom), we continue to track it for Tap purposes
                     // But if it's consumed as a DRAG, we might want to respect that?
                     // However, Zoom consumes on "wobble". We want to ignore wobble.
                }
            }
        } catch (e: Exception) {
            TimberLogger.logI(TAG, "[PageNavFix] Exception waiting for up: $e")
            return@awaitEachGesture
        }

        if (up == null) {
            TimberLogger.logI(TAG, "[PageNavFix] Gesture cancelled or no up event")
            return@awaitEachGesture
        }
        
        // Consume the UP event if we handle it appropriately?
        // If we consume UP in Initial, Zoom logic won't see UP.
        // But Zoom logic consumes Move.
        // If we consume Up, it's fine.

        // Check if it was a real tap (not a drag)
        // Increased tolerance for "wobble" especially when zoomed
        if ((down.position - up.position).getDistance() > viewConfiguration.touchSlop * 6) {
            TimberLogger.logI(TAG, "[PageNavFix] Ignored as drag: distance > touchSlop * 6")
            return@awaitEachGesture
        }

        // Handle custom tap first if enabled
        if (config.tapCustomEnabled && config.onCustomTap(this, size, up.position)) {
            TimberLogger.logI(TAG, "[PageNavFix] Custom tap handled")
            up.consume() // Consume UP to prevent others from handling it
            return@awaitEachGesture
        }

        // Handle forward tap
        if (config.tapForwardEnabled && tapInteraction.forward.target.multiply(size).contains(up.position)) {
            scope.launch {
                onTapForward()
            }
            TimberLogger.logI(TAG, "[PageNavFix] Forward tap detected")
            up.consume()
            return@awaitEachGesture
        }

        // Handle backward tap
        if (config.tapBackwardEnabled && tapInteraction.backward.target.multiply(size).contains(up.position)) {
            scope.launch {
                onTapBackward()
            }
            TimberLogger.logI(TAG, "[PageNavFix] Backward tap detected")
            up.consume()
            return@awaitEachGesture
        }
        TimberLogger.logI(TAG, "[PageNavFix] Tap ignored: no matching target")
    }
}

