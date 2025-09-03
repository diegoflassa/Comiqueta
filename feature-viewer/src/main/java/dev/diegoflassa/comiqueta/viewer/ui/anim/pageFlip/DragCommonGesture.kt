@file:Suppress("MatchingDeclarationName")

package dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.utils.rotate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI

/**
 * Configuration data class for a specific drag-to-curl operation (either forward or backward).
 *
 * @property edge The [Animatable] [Edge] that is being manipulated by the drag.
 * @property start The starting [Edge] position (e.g., right edge for forward, left edge for backward).
 * @property end The target [Edge] position if the drag successfully completes a page turn.
 * @property isEnabled A lambda that returns true if this drag configuration is currently active and enabled.
 * @property isDragSucceed A lambda that determines if a drag gesture is considered successful based on its start and end [Offset]s.
 * @property onChange A lambda to be invoked when the drag successfully completes a page turn.
 */
internal data class DragConfig(
    val edge: Animatable<Edge, AnimationVector4D>,
    val start: Edge,
    val end: Edge,
    val isEnabled: () -> Boolean,
    val isDragSucceed: (startOffset: Offset, endOffset: Offset) -> Boolean,
    val onChange: () -> Unit,
)

/**
 * Detects drag gestures specifically for page curling.
 * It uses a [VelocityTracker] for fling support and determines the drag configuration dynamically.
 * Manages the animation of the curling [Edge] based on drag movements and fling velocity.
 *
 * @param scope The [CoroutineScope] for launching animations and other suspending operations.
 * @param newEdgeCreator The [NewEdgeCreator] strategy to determine the shape of the curling edge during a drag.
 * @param getConfig A lambda function that, given the start and current drag [Offset]s, returns the relevant [DragConfig]
 *                  for the current gesture, or null if the gesture should not initiate a curl.
 */
internal suspend fun PointerInputScope.detectCurlGestures(
    scope: CoroutineScope,
    newEdgeCreator: NewEdgeCreator,
    getConfig: (startOffset: Offset, currentOffset: Offset) -> DragConfig?,
) {
    // Use velocity tracker to support flings
    val velocityTracker = VelocityTracker()

    var config: DragConfig? = null
    var startOffset: Offset = Offset.Zero

    detectCustomDragGestures(
        onDragStart = { start, end ->
            startOffset = start
            config = getConfig(start, end)
            config != null
        },
        onDragEnd = { endOffset, complete ->
            config?.apply {
                val velocity = velocityTracker.calculateVelocity()
                val decay = splineBasedDecay<Offset>(this@detectCurlGestures)
                val flingEndOffset = decay.calculateTargetValue(
                    Offset.VectorConverter,
                    endOffset,
                    Offset(velocity.x, velocity.y)
                ).let {
                    Offset(
                        it.x.coerceIn(0f, size.width.toFloat() - 1f),
                        it.y.coerceIn(0f, size.height.toFloat() - 1f)
                    )
                }

                scope.launch {
                    if (complete && isDragSucceed(startOffset, flingEndOffset)) {
                        try {
                            edge.animateTo(end)
                        } finally {
                            onChange()
                            edge.snapTo(start)
                        }
                    } else {
                        try {
                            edge.animateTo(start)
                        } finally {
                            edge.snapTo(start)
                        }
                    }
                }
            }
        },
        onDrag = { change, _ ->
            config?.apply {
                if (!isEnabled()) {
                    throw CancellationException("Drag gesture was disabled during operation")
                }

                velocityTracker.addPosition(System.currentTimeMillis(), change.position)

                scope.launch {
                    val target = newEdgeCreator.createNew(size, startOffset, change.position)
                    edge.animateTo(target)
                }
            }
        }
    )
}

/**
 * A low-level custom drag gesture detector.
 * It awaits a down pointer, then waits for the touch slop to be exceeded before starting a drag.
 * It calls [onDragStart] when the drag begins, [onDrag] for each drag event, and [onDragEnd]
 * when the drag finishes.
 *
 * @param onDragStart Lambda invoked when a drag gesture is initiated. It receives the start and current pointer [Offset]s.
 *                    Returns `true` if the drag should proceed, `false` to ignore the drag.
 * @param onDragEnd Lambda invoked when the drag gesture concludes. It receives the final pointer [Offset]
 *                  and a boolean `completed` which is true if the drag completed successfully (not cancelled).
 * @param onDrag Lambda invoked for each drag movement. It receives the [PointerInputChange] and the drag amount [Offset].
 */
internal suspend fun PointerInputScope.detectCustomDragGestures(
    onDragStart: (startOffset: Offset, currentOffset: Offset) -> Boolean,
    onDragEnd: (endOffset: Offset, completed: Boolean) -> Unit,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var drag: PointerInputChange?
        var overSlop = Offset.Zero
        do {
            drag = awaitTouchSlopOrCancellation(down.id) { change, over ->
                change.consume()
                overSlop = over
            }
        } while (drag != null && !drag.isConsumed)
        if (drag != null) {
            if (!onDragStart.invoke(down.position, drag.position)) {
                return@awaitEachGesture
            }
            onDrag(drag, overSlop) // Initial drag event after slop
            val completed = drag(drag.id) {
                // drag = it // Not needed as 'it' is the current PointerInputChange
                onDrag(it, it.positionChange())
                it.consume()
            }
            onDragEnd(drag.position, completed)
        }
    }
}

/**
 * Abstract class defining a strategy for creating a new [Edge] shape during a drag gesture.
 * This determines how the curling page's edge responds to the user's finger movement.
 */
internal abstract class NewEdgeCreator {

    /**
     * Creates a new [Edge] based on the drag's start and current positions.
     *
     * @param size The [IntSize] of the composable area.
     * @param startOffset The [Offset] where the drag started.
     * @param currentOffset The current [Offset] of the drag pointer.
     * @return The calculated new [Edge] representing the curl.
     */
    abstract fun createNew(size: IntSize, startOffset: Offset, currentOffset: Offset): Edge

    /**
     * Calculates the primary vector (from right-edge-touch-y to current drag point)
     * and its perpendicular rotated vector, which form the basis for the curl edge.
     *
     * @param size The [IntSize] of the composable area.
     * @param startOffset The [Offset] where the drag started (specifically its y-component is used for the anchor).
     * @param currentOffset The current [Offset] of the drag pointer.
     * @return A [Pair] containing the primary vector and the rotated perpendicular vector.
     */
    protected fun createVectors(size: IntSize, startOffset: Offset, currentOffset: Offset): Pair<Offset, Offset> {
        // Vector from the point on the right edge (at startOffset.y) to the current drag position
        val vector = Offset(size.width.toFloat(), startOffset.y) - currentOffset
        // Vector perpendicular to the primary vector, defining the curl line's orientation
        val rotatedVector = vector.rotate(PI.toFloat() / 2f) // Corrected PI.toFloat() / 2
        return vector to rotatedVector
    }

    /**
     * Default [NewEdgeCreator] strategy.
     * The curl edge is created perpendicular to the line connecting the initial touch Y-coordinate on the page edge
     * and the current pointer position.
     */
    class Default : NewEdgeCreator() {
        /**
         * Creates a new [Edge] where the edge line is perpendicular to the vector formed from
         * a point on the page's far edge (aligned with the drag start's Y) to the current drag position.
         * The curl edge is centered on the [currentOffset].
         */
        override fun createNew(size: IntSize, startOffset: Offset, currentOffset: Offset): Edge {
            val vectors = createVectors(size, startOffset, currentOffset)
            // The edge is formed by moving along the rotated vector in both directions from currentOffset
            return Edge(currentOffset - vectors.second, currentOffset + vectors.second)
        }
    }

    /**
     * [NewEdgeCreator] strategy that attempts to keep the curl originating from the page's actual edge.
     * The curl edge is defined relative to the page's far edge and the current pointer position.
     */
    class PageEdge : NewEdgeCreator() {
        /**
         * Creates a new [Edge] that is oriented based on the drag vector but shifted
         * so that it appears to emanate more directly from the physical edge of the page being curled.
         * It uses the primary vector to adjust the center of the curl.
         */
        override fun createNew(size: IntSize, startOffset: Offset, currentOffset: Offset): Edge {
            val (vector, rotatedVector) = createVectors(size, startOffset, currentOffset)
            // The edge is formed similarly to Default, but offset along the primary vector
            // to simulate the curl peeling from the page's edge.
            return Edge(currentOffset - rotatedVector + vector / 2f, currentOffset + rotatedVector + vector / 2f)
        }
    }
}
