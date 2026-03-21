package dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip

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
 * A [Modifier] that enables drag gestures for page curling based on defined start and end target areas.
 * A page curl (forward or backward) is initiated if a drag starts within a specified "start" rectangle.
 * The page turn is considered successful if the drag ends within a corresponding "end" rectangle.
 *
 * @param dragInteraction The [PageCurlConfig.StartEndDragInteraction] configuration, defining
 *                        start and end target rectangles for forward and backward drags, and pointer behavior.
 * @param state The [PageCurlState.InternalState] managing the current animation and edge positions.
 * @param enabledForward True if forward page curling via drag is enabled, false otherwise.
 * @param enabledBackward True if backward page curling via drag is enabled, false otherwise.
 * @param scope The [CoroutineScope] for launching coroutines, e.g., for state resets or animations.
 * @param onChange A lambda function invoked when a drag gesture successfully completes a page turn.
 *                 It receives an integer indicating the direction: `+1` for forward, `-1` for backward.
 * @return A [Modifier] that processes drag gestures for page curling based on start/end areas.
 */
internal fun Modifier.dragStartEnd(
    dragInteraction: PageCurlConfig.StartEndDragInteraction,
    state: PageCurlState.InternalState,
    enabledForward: Boolean,
    enabledBackward: Boolean,
    dragThreshold: Float,
    scope: CoroutineScope,
    onChange: (Int) -> Unit,
): Modifier = this.composed {
    val isEnabledForward = rememberUpdatedState(enabledForward)
    val isEnabledBackward = rememberUpdatedState(enabledBackward)

    pointerInput(state) {
        val forwardStartRect by lazy { dragInteraction.forward.start.multiply(size) }
        val forwardEndRect by lazy { dragInteraction.forward.end.multiply(size) }
        val backwardStartRect by lazy { dragInteraction.backward.start.multiply(size) }
        val backwardEndRect by lazy { dragInteraction.backward.end.multiply(size) }

        val forwardConfig = DragConfig(
            edge = state.forward,
            start = state.rightEdge,
            end = state.leftEdge,
            isEnabled = { isEnabledForward.value },
            isDragSucceed = { start, end ->
                 val displacement = start.x - end.x
                 val thresholdPx = size.width * dragThreshold
                 forwardEndRect.contains(end) || displacement > thresholdPx
            },
            onChange = { onChange(+1) }
        )
        val backwardConfig = DragConfig(
            edge = state.backward,
            start = state.leftEdge,
            end = state.rightEdge,
            isEnabled = { isEnabledBackward.value },
            isDragSucceed = { start, end ->
                val displacement = end.x - start.x
                val thresholdPx = size.width * dragThreshold
                backwardEndRect.contains(end) || displacement > thresholdPx
            },
            onChange = { onChange(-1) }
        )

        detectCurlGestures(
            scope = scope,
            newEdgeCreator = when (dragInteraction.pointerBehavior) {
                PointerBehavior.Default -> NewEdgeCreator.Default()
                PointerBehavior.PageEdge -> NewEdgeCreator.PageEdge()
            },
            getConfig = { start, _ ->
                val config = if (forwardStartRect.contains(start)) {
                    forwardConfig
                } else if (backwardStartRect.contains(start)) {
                    backwardConfig
                } else {
                    null
                }

                val direction = if (config == forwardConfig) "forward" else if (config == backwardConfig) "backward" else "none"

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
