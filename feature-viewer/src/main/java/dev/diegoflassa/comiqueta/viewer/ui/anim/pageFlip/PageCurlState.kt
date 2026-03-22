package dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Constraints
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.config.PageCurlConfig
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.config.rememberPageCurlConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Remembers and returns an instance of [PageCurlState].
 * The state is remembered across recompositions and saved during configuration changes.
 *
 * @param initialCurrent The initial page index to be displayed.
 * @return A remembered [PageCurlState] instance.
 */
@Composable
fun rememberPageCurlState(
    initialCurrent: Int = 0,
): PageCurlState =
    rememberSaveable(
        initialCurrent,
        saver = Saver(
            save = { it.current },
            restore = { PageCurlState(initialCurrent = it) }
        )
    ) {
        PageCurlState(
            initialCurrent = initialCurrent,
        )
    }

/**
 * Remembers and returns an instance of [PageCurlState].
 * The state is remembered across recompositions and saved during configuration changes.
 *
 * @param initialCurrent The initial page index to be displayed.
 * @param config The configuration for PageCurl. **Deprecated**: Specify 'config' in the PageCurl composable itself.
 * @return A remembered [PageCurlState] instance.
 * @see PageCurlState
 */
@Composable
@Deprecated(
    message = "Specify 'config' as 'config' in PageCurl composable.",
    level = DeprecationLevel.ERROR,
)
@Suppress("UnusedPrivateMember")
fun rememberPageCurlState(
    initialCurrent: Int = 0,
    config: PageCurlConfig,
): PageCurlState =
    rememberSaveable(
        initialCurrent,
        saver = Saver(
            save = { it.current },
            restore = { PageCurlState(initialCurrent = it) }
        )
    ) {
        PageCurlState(
            initialCurrent = initialCurrent,
        )
    }

/**
 * Remembers and returns an instance of [PageCurlState].
 * The state is remembered across recompositions and saved during configuration changes.
 *
 * @param max The total number of pages. **Deprecated**: Specify 'max' as 'count' in the PageCurl composable.
 * @param initialCurrent The initial page index to be displayed.
 * @param config The configuration for PageCurl. **Deprecated**: Specify 'config' in the PageCurl composable itself.
 * @return A remembered [PageCurlState] instance.
 * @see PageCurlState
 */
@Composable
@Deprecated(
    message = "Specify 'max' as 'count' in PageCurl composable and 'config' as 'config' in PageCurl composable.",
    level = DeprecationLevel.ERROR,
)
@Suppress("UnusedPrivateMember")
fun rememberPageCurlState(
    max: Int,
    initialCurrent: Int = 0,
    config: PageCurlConfig = rememberPageCurlConfig()
): PageCurlState =
    rememberSaveable(
        max, initialCurrent,
        saver = Saver(
            save = { it.current },
            restore = {
                PageCurlState(
                    initialCurrent = it,
                    initialMax = max,
                )
            }
        )
    ) {
        PageCurlState(
            initialCurrent = initialCurrent,
            initialMax = max,
        )
    }

/**
 * Manages the state for a PageCurl composable, including the current page, animation progress,
 * and interactions for page turning.
 *
 * @param initialMax The initial total number of pages. This is typically set up later via [setup].
 * @param initialCurrent The initial page index that is currently visible or being interacted with.
 */
class PageCurlState(
    initialMax: Int = 0,
    initialCurrent: Int = 0,
) {
    /**
     * The currently displayed page index. This state is observable and mutable internally.
     */
    var current: Int by mutableIntStateOf(initialCurrent)
        internal set

    /**
     * The progress of the current page turn animation.
     * - `0.0` means no animation is active or the page is flat.
     * - Values from `0.0` to `1.0` indicate a forward page turn in progress.
     * - Values from `0.0` to `-1.0` indicate a backward page turn in progress.
     */
    val progress: Float get() = internalState?.progress ?: 0f

    /**
     * The total number of pages available. Set via the [setup] method.
     */
    internal var max: Int = initialMax
        private set

    /**
     * The internal state holder, containing animation values and constraint information.
     * Null until [setup] is called.
     */
    internal var internalState: InternalState? by mutableStateOf(null)
        private set

    /**
     * Initializes or updates the state based on the total page count and layout constraints.
     * This should be called when the PageCurl composable is laid out or when its parameters change.
     *
     * @param count The total number of pages.
     * @param constraints The layout constraints of the PageCurl composable.
     */
    internal fun setup(count: Int, constraints: Constraints) {
        max = count
        if (count in 1..current) {
            current = (count - 1)
        } else if (count == 0) {
            current = 0
        }

        if (internalState?.constraints == constraints) {
            return
        }

        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        val left = Edge(Offset(0f, 0f), Offset(0f, maxHeightPx))
        val right = Edge(Offset(maxWidthPx, 0f), Offset(maxWidthPx, maxHeightPx))

        val forward = Animatable(right, Edge.VectorConverter, Edge.VisibilityThreshold)
        val backward = Animatable(left, Edge.VectorConverter, Edge.VisibilityThreshold)

        internalState = InternalState(constraints, left, right, forward, backward)
    }

    /**
     * Instantly snaps the PageCurl to the specified page index without animation.
     * Any ongoing animation will be cancelled.
     *
     * @param value The target page index to snap to. Will be coerced within the valid page range `[0, max - 1]`.
     */
    suspend fun snapTo(value: Int) {
        current = if (max > 0) value.coerceIn(0, max - 1) else 0
        internalState?.reset()
    }

    /**
     * Animates to the next page.
     * If a custom animation [block] is provided, it will be used for the animation.
     * Otherwise, a default animation is applied.
     *
     * @param block An optional suspend lambda to define a custom animation for the forward page turn.
     *              It receives an [Animatable] for the [Edge] and the [Size] of the composable.
     */
    suspend fun next(block: suspend Animatable<Edge, AnimationVector4D>.(Size) -> Unit = DefaultNext) {
        internalState?.animateTo(
            target = { current + 1 },
            animate = { size -> this.forward.block(size) }
        )
    }

    /**
     * Animates to the previous page.
     * If a custom animation [block] is provided, it will be used for the animation.
     * Otherwise, a default animation is applied.
     *
     * @param block An optional suspend lambda to define a custom animation for the backward page turn.
     *              It receives an [Animatable] for the [Edge] and the [Size] of the composable.
     */
    suspend fun prev(block: suspend Animatable<Edge, AnimationVector4D>.(Size) -> Unit = DefaultPrev) {
        internalState?.animateTo(
            target = { current - 1 },
            animate = { size -> this.backward.block(size) }
        )
    }

    /**
     * Internal state holder for animations and layout-dependent values.
     *
     * @property constraints The layout constraints of the PageCurl composable.
     * @property leftEdge The [Edge] representing the leftmost boundary.
     * @property rightEdge The [Edge] representing the rightmost boundary.
     * @property forward The [Animatable] [Edge] used for forward page curl animations.
     * @property backward The [Animatable] [Edge] used for backward page curl animations.
     */
    internal inner class InternalState(
        val constraints: Constraints,
        val leftEdge: Edge,
        val rightEdge: Edge,
        val forward: Animatable<Edge, AnimationVector4D>,
        val backward: Animatable<Edge, AnimationVector4D>,
    ) {

        /**
         * The [Job] for the current ongoing page turn animation, if any.
         */
        var animateJob: Job? = null

        /**
         * The current animation progress, derived from the positions of the [forward] and [backward] edges.
         * See [PageCurlState.progress] for detailed value interpretation.
         */
        val progress: Float by derivedStateOf {
            if (forward.value != rightEdge) {
                1f - forward.value.centerX / constraints.maxWidth.toFloat().coerceAtLeast(1f)
            } else if (backward.value != leftEdge) {
                -backward.value.centerX / constraints.maxWidth.toFloat().coerceAtLeast(1f)
            } else {
                0f
            }
        }

        /**
         * Resets both [forward] and [backward] animatable edges to their default positions (right and left respectively)
         * without animation.
         */
        suspend fun reset() {
            forward.snapTo(rightEdge)
            backward.snapTo(leftEdge)
        }

        /**
         * Initiates an animation to a target page.
         * It cancels any existing animation, resets the edges, and then runs the provided [animate] block.
         * Finally, it snaps to the target page index.
         *
         * @param target A lambda returning the target page index.
         * @param animate A suspend lambda that defines the animation logic for an [Edge].
         */
        suspend fun animateTo(
            target: () -> Int,
            animate: suspend InternalState.(Size) -> Unit
        ) {
            animateJob?.cancel()

            val targetIndex = target()
            
            if (max == 0 && targetIndex == 0) { // Allow animation to 0 if count is 0
                // No specific handling needed here, proceed to animation
            } else if (targetIndex !in 0..max) {
                return // Invalid target
            }

            coroutineScope {
                animateJob = launch {
                    try {
                        reset()
                        animate(Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()))
                    } finally {
                        withContext(NonCancellable) {
                            snapTo(target())
                        }
                    }
                }
            }
        }
    }
}

/**
 * Represents a vertical edge defined by a top and a bottom [Offset].
 * This is used to define the boundaries of a page and the animated curling edge.
 *
 * @property top The top [Offset] of the edge.
 * @property bottom The bottom [Offset] of the edge.
 */
data class Edge(val top: Offset, val bottom: Offset) {

    /**
     * The x-coordinate of the center of the edge.
     */
    internal val centerX: Float = (top.x + bottom.x) * 0.5f

    internal companion object {
        /**
         * A [TwoWayConverter] for animating [Edge] objects using [AnimationVector4D].
         * It converts an [Edge] to a vector of (top.x, top.y, bottom.x, bottom.y) and back.
         */
        val VectorConverter: TwoWayConverter<Edge, AnimationVector4D> =
            TwoWayConverter(
                convertToVector = { AnimationVector4D(it.top.x, it.top.y, it.bottom.x, it.bottom.y) },
                convertFromVector = { Edge(Offset(it.v1, it.v2), Offset(it.v3, it.v4)) }
            )

        /**
         * The visibility threshold for [Edge] animations.
         */
        val VisibilityThreshold: Edge =
            Edge(Offset.VisibilityThreshold, Offset.VisibilityThreshold)
    }
}

/**
 * Default animation block for turning to the next page.
 * Animates the edge from right to left.
 */
private val DefaultNext: suspend Animatable<Edge, AnimationVector4D>.(Size) -> Unit = { size ->
    animateTo(
        targetValue = size.startEdgePosition,
        animationSpec = keyframes {
            durationMillis = DefaultAnimDuration
            size.endEdgePosition at 0
            size.middleEdgePosition at DefaultMidPointDuration
        }
    )
}

/**
 * Default animation block for turning to the previous page.
 * Animates the edge from left to right.
 */
private val DefaultPrev: suspend Animatable<Edge, AnimationVector4D>.(Size) -> Unit = { size ->
    animateTo(
        targetValue = size.endEdgePosition,
        animationSpec = keyframes {
            durationMillis = DefaultAnimDuration
            size.startEdgePosition at 0
            size.middleEdgePosition at DefaultAnimDuration - DefaultMidPointDuration
        }
    )
}

private const val DefaultAnimDuration: Int = 450
private const val DefaultMidPointDuration: Int = 150

/** Represents the starting edge position (typically left side) for an animation within a given [Size]. */
private val Size.startEdgePosition: Edge
    get() = Edge(Offset(0f, 0f), Offset(0f, height))

/** Represents a middle edge position (e.g., page folded halfway) for an animation within a given [Size]. */
private val Size.middleEdgePosition: Edge
    get() = Edge(Offset(width / 2f, 0f), Offset(width / 2f, height)) // Simplified middle edge

/** Represents the ending edge position (typically right side) for an animation within a given [Size]. */
private val Size.endEdgePosition: Edge
    get() = Edge(Offset(width, 0f), Offset(width, height))
