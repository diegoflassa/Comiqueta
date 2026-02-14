package dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip

import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger

import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.config.PageCurlConfig
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.config.PageCurlConfig.DragInteraction.PointerBehavior
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.utils.multiply
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A [Modifier] that enables drag gestures for page curling based on defined target areas.
 * This gesture handler initiates a page curl (forward or backward) when a drag starts within
 * specified rectangular target zones and moves in the appropriate direction.
 *
 * @param dragInteraction The [PageCurlConfig.GestureDragInteraction] configuration, defining target
 *                        rectangles for forward and backward drag initiation, and pointer behavior.
 * @param state The [PageCurlState.InternalState] managing the current animation and edge positions.
 * @param enabledForward True if forward page curling via drag is enabled, false otherwise.
 * @param enabledBackward True if backward page curling via drag is enabled, false otherwise.
 * @param scope The [CoroutineScope] for launching coroutines, e.g., for state resets or animations.
 * @param onChange A lambda function invoked when a drag gesture successfully completes a page turn.
 *                 It receives an integer indicating the direction: `+1` for forward, `-1` for backward.
 * @return A [Modifier] that processes drag gestures for page curling.
 */
internal fun Modifier.dragGesture(
    dragInteraction: PageCurlConfig.GestureDragInteraction,
    state: PageCurlState.InternalState,
    enabledForward: Boolean,
    enabledBackward: Boolean,
    dragThreshold: Float,
    scope: CoroutineScope,
    onChange: (Int) -> Unit
): Modifier = this.composed {
    val isEnabledForward = rememberUpdatedState(enabledForward)
    val isEnabledBackward = rememberUpdatedState(enabledBackward)

    pointerInput(state) {
        val forwardTargetRect by lazy { dragInteraction.forward.target.multiply(size) }
        val backwardTargetRect by lazy { dragInteraction.backward.target.multiply(size) }

        val forwardConfig = DragConfig(
            edge = state.forward,
            start = state.rightEdge,
            end = state.leftEdge,
            isEnabled = { isEnabledForward.value },
            isDragSucceed = { start, end ->
                val displacement = start.x - end.x
                val thresholdPx = size.width * dragThreshold
                // Success if passed threshold OR (moved somewhat AND fling says success per original logic)
                // We keep the original logic "end < start" implied?
                // The original logic was: end.x < start.x
                // That effectively meant "Any movement left".
                // We likely want: (displacement > threshold) OR (original_velocity_based_check_if_we_kept_it)
                // But wait, the original logic in DragGesture was JUST "end.x < start.x".
                // So now we require threshold?
                // User said "The animation should 'auto complete' when this threshold is reached!"
                // So: if displacement > threshold -> Success.
                // What if displacement < threshold?
                // Should it depend on velocity?
                // The `end` passed here is `flingEndOffset`.
                // If I fling, `flingEndOffset` will be far. displacement will be large.
                // Fix: Allow ANY forward movement (that exceeded slop) to succeed if it's > 10px
                // displacement > thresholdPx || displacement > 10f
                val success = displacement > thresholdPx || displacement > 10f
                TimberLogger.logI("DragGesture", "[PageNavFix] Forward drag: displacement=$displacement, threshold=$thresholdPx. Success: $success")
                success
            },
            onChange = { onChange(+1) }
        )
        val backwardConfig = DragConfig(
            edge = state.backward,
            start = state.leftEdge,
            end = state.rightEdge,
            isEnabled = { isEnabledBackward.value },
            isDragSucceed = { start, end ->
                val displacement = end.x - start.x // Backward: Swipe Right
                val thresholdPx = size.width * dragThreshold
                // Fix: Allow ANY backward movement (that exceeded slop) to succeed if it's > 10px
                val success = displacement > thresholdPx || displacement > 10f
                TimberLogger.logI("DragGesture", "[PageNavFix] Backward drag: displacement=$displacement, threshold=$thresholdPx. Success: $success")
                success
            },
            onChange = { onChange(-1) }
        )

        detectCurlGestures(
            scope = scope,
            newEdgeCreator = when (dragInteraction.pointerBehavior) {
                PointerBehavior.Default -> NewEdgeCreator.Default()
                PointerBehavior.PageEdge -> NewEdgeCreator.PageEdge()
            },
            getConfig = { start, end ->
                val config = if (forwardTargetRect.contains(start) && end.x < start.x) {
                    forwardConfig
                } else if (backwardTargetRect.contains(start) && end.x > start.x) {
                    backwardConfig
                } else {
                    null
                }

                if (config != null) {
                    scope.launch {
                        state.animateJob?.cancel()
                        state.reset()
                    }
                }

                config
            },
        )
    }
}
