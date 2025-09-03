@file:Suppress("ComplexMethod", "LongParameterList", "LongMethod")

package dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.config

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Creates and remembers a [PageCurlConfig] instance with customizable properties for the page curl effect.
 * This configuration is saved and restored across recompositions and configuration changes.
 *
 * @param backPageColor Color of the back-page. Defaults to [Color.White].
 *                      Typically set to the content background color.
 * @param backPageContentAlpha Alpha defining how content is "seen through" the back-page (0.0 to 1.0).
 *                             Defaults to `0.1f`.
 * @param shadowColor Color of the shadow. Defaults to [Color.Black].
 *                    Typically set to the inverted color of the content background. Alpha is handled by [shadowAlpha].
 * @param shadowAlpha Alpha of the [shadowColor]. Defaults to `0.2f`.
 * @param shadowRadius Defines the blur radius of the shadow. Defaults to `15.dp`.
 * @param shadowOffset Defines how the shadow is shifted from the page. Defaults to `DpOffset((-5).dp, 0.dp)`.
 * @param dragForwardEnabled True if forward drag interaction is enabled. Defaults to `true`.
 * @param dragBackwardEnabled True if backward drag interaction is enabled. Defaults to `true`.
 * @param tapForwardEnabled True if forward tap interaction is enabled. Defaults to `true`.
 * @param tapBackwardEnabled True if backward tap interaction is enabled. Defaults to `true`.
 * @param tapCustomEnabled True if custom tap interaction via [onCustomTap] is enabled. Defaults to `true`.
 * @param dragInteraction The drag interaction strategy. Defaults to [PageCurlConfig.StartEndDragInteraction].
 * @param tapInteraction The tap interaction strategy. Defaults to [PageCurlConfig.TargetTapInteraction].
 * @param onCustomTap Lambda to handle custom tap events. Receives [Density] scope, [IntSize] of the PageCurl area,
 *                    and the tap [Offset]. Returns `true` if the tap is handled, `false` otherwise.
 *                    Defaults to a no-op that returns `false`.
 * @return A remembered [PageCurlConfig] instance.
 */
@Composable
fun rememberPageCurlConfig(
    backPageColor: Color = Color.White,
    backPageContentAlpha: Float = 0.1f,
    shadowColor: Color = Color.Black,
    shadowAlpha: Float = 0.2f,
    shadowRadius: Dp = 15.dp,
    shadowOffset: DpOffset = DpOffset((-5).dp, 0.dp),
    dragForwardEnabled: Boolean = true,
    dragBackwardEnabled: Boolean = true,
    tapForwardEnabled: Boolean = true,
    tapBackwardEnabled: Boolean = true,
    tapCustomEnabled: Boolean = true,
    dragInteraction: PageCurlConfig.DragInteraction = PageCurlConfig.StartEndDragInteraction(),
    tapInteraction: PageCurlConfig.TapInteraction = PageCurlConfig.TargetTapInteraction(),
    onCustomTap: Density.(IntSize, Offset) -> Boolean = { _, _ -> false },
): PageCurlConfig =
    rememberSaveable(
        saver = listSaver(
            save = {
                fun Rect.forSave(): List<Any> =
                    listOf(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)

                fun PageCurlConfig.DragInteraction.getRectList(): List<Rect> =
                    when (this) {
                        is PageCurlConfig.GestureDragInteraction ->
                            listOf(forward.target, backward.target)

                        is PageCurlConfig.StartEndDragInteraction ->
                            listOf(forward.start, forward.end, backward.start, backward.end)
                    }

                fun PageCurlConfig.TapInteraction.getRectList(): List<Rect> =
                    when (this) {
                        is PageCurlConfig.TargetTapInteraction ->
                            listOf(forward.target, backward.target)
                    }

                fun PageCurlConfig.DragInteraction.forSave(): List<Any> =
                    listOf(this::class.java.name, pointerBehavior.name) + getRectList().flatMap(Rect::forSave)

                fun PageCurlConfig.TapInteraction.forSave(): List<Any> =
                    listOf(this::class.java.name) + getRectList().flatMap(Rect::forSave)

                listOf(
                    it.backPageColor.value.toLong(), // Save as Long to preserve ARGB
                    it.backPageContentAlpha,
                    it.shadowColor.value.toLong(), // Save as Long
                    it.shadowAlpha,
                    it.shadowRadius.value,
                    it.shadowOffset.x.value,
                    it.shadowOffset.y.value,
                    it.dragForwardEnabled,
                    it.dragBackwardEnabled,
                    it.tapForwardEnabled,
                    it.tapBackwardEnabled,
                    it.tapCustomEnabled,
                    *it.dragInteraction.forSave().toTypedArray(),
                    *it.tapInteraction.forSave().toTypedArray(),
                )
            },
            restore = {
                val iterator = it.iterator()
                fun Iterator<Any>.nextRect(): Rect =
                    Rect(next() as Float, next() as Float, next() as Float, next() as Float)

                PageCurlConfig(
                    Color(iterator.next() as Long), // Restore from Long
                    iterator.next() as Float,
                    Color(iterator.next() as Long), // Restore from Long
                    iterator.next() as Float,
                    Dp(iterator.next() as Float),
                    DpOffset(Dp(iterator.next() as Float), Dp(iterator.next() as Float)),
                    iterator.next() as Boolean,
                    iterator.next() as Boolean,
                    iterator.next() as Boolean,
                    iterator.next() as Boolean,
                    iterator.next() as Boolean,
                    when (val dragInteractionClassName = iterator.next() as String) {
                        PageCurlConfig.GestureDragInteraction::class.java.name -> {
                            PageCurlConfig.GestureDragInteraction(
                                PageCurlConfig.DragInteraction.PointerBehavior.valueOf(iterator.next() as String),
                                PageCurlConfig.GestureDragInteraction.Config(iterator.nextRect()),
                                PageCurlConfig.GestureDragInteraction.Config(iterator.nextRect()),
                            )
                        }

                        PageCurlConfig.StartEndDragInteraction::class.java.name -> {
                            PageCurlConfig.StartEndDragInteraction(
                                PageCurlConfig.DragInteraction.PointerBehavior.valueOf(iterator.next() as String),
                                PageCurlConfig.StartEndDragInteraction.Config(iterator.nextRect(), iterator.nextRect()),
                                PageCurlConfig.StartEndDragInteraction.Config(iterator.nextRect(), iterator.nextRect()),
                            )
                        }

                        else -> error("Unable to restore PageCurlConfig.DragInteraction: Unknown class name $dragInteractionClassName")
                    },
                    when (val tapInteractionClassName = iterator.next() as String) {
                        PageCurlConfig.TargetTapInteraction::class.java.name -> {
                            PageCurlConfig.TargetTapInteraction(
                                PageCurlConfig.TargetTapInteraction.Config(iterator.nextRect()),
                                PageCurlConfig.TargetTapInteraction.Config(iterator.nextRect()),
                            )
                        }

                        else -> error("Unable to restore PageCurlConfig.TapInteraction: Unknown class name $tapInteractionClassName")
                    },
                    onCustomTap
                )
            }
        )
    ) {
        PageCurlConfig(
            backPageColor = backPageColor,
            backPageContentAlpha = backPageContentAlpha,
            shadowColor = shadowColor,
            shadowAlpha = shadowAlpha,
            shadowRadius = shadowRadius,
            shadowOffset = shadowOffset,
            dragForwardEnabled = dragForwardEnabled,
            dragBackwardEnabled = dragBackwardEnabled,
            tapForwardEnabled = tapForwardEnabled,
            tapBackwardEnabled = tapBackwardEnabled,
            tapCustomEnabled = tapCustomEnabled,
            dragInteraction = dragInteraction,
            tapInteraction = tapInteraction,
            onCustomTap = onCustomTap
        )
    }

/**
 * Configuration class for the PageCurl composable, defining its appearance and interaction behavior.
 * All properties are mutable states, allowing dynamic updates to the configuration.
 *
 * @property backPageColor Color of the back-page. Typically set to the content background color.
 * @property backPageContentAlpha Alpha defining how content is "seen through" the back-page (0.0 to 1.0).
 * @property shadowColor Color of the shadow. Alpha is handled by [shadowAlpha].
 * @property shadowAlpha Alpha of the [shadowColor].
 * @property shadowRadius Defines the blur radius of the shadow.
 * @property shadowOffset Defines how the shadow is shifted from the page.
 * @property dragForwardEnabled True if forward drag interaction is enabled.
 * @property dragBackwardEnabled True if backward drag interaction is enabled.
 * @property tapForwardEnabled True if forward tap interaction is enabled.
 * @property tapBackwardEnabled True if backward tap interaction is enabled.
 * @property tapCustomEnabled True if custom tap interaction via [onCustomTap] is enabled.
 * @property dragInteraction The drag interaction strategy.
 * @property tapInteraction The tap interaction strategy.
 * @property onCustomTap Lambda to handle custom tap events. Receives [Density] scope, [IntSize] of the PageCurl area,
 *                       and the tap [Offset]. Returns `true` if the tap is handled, `false` otherwise.
 */
class PageCurlConfig(
    backPageColor: Color,
    backPageContentAlpha: Float,
    shadowColor: Color,
    shadowAlpha: Float,
    shadowRadius: Dp,
    shadowOffset: DpOffset,
    dragForwardEnabled: Boolean,
    dragBackwardEnabled: Boolean,
    tapForwardEnabled: Boolean,
    tapBackwardEnabled: Boolean,
    tapCustomEnabled: Boolean,
    dragInteraction: DragInteraction,
    tapInteraction: TapInteraction,
    val onCustomTap: Density.(IntSize, Offset) -> Boolean,
) {
    /** Color of the back-page. In majority of use-cases it should be set to the content background color. */
    var backPageColor: Color by mutableStateOf(backPageColor)

    /** Alpha which defines how content is "seen through" the back-page. From 0 (nothing is visible) to 1 (everything is visible). */
    var backPageContentAlpha: Float by mutableFloatStateOf(backPageContentAlpha)

    /** Color of the shadow. In majority of use-cases it should be set to the inverted color to the content background color. Should be a solid color, see [shadowAlpha] to adjust opacity. */
    var shadowColor: Color by mutableStateOf(shadowColor)

    /** Alpha of the [shadowColor]. */
    var shadowAlpha: Float by mutableFloatStateOf(shadowAlpha)

    /** Defines how big the shadow is. */
    var shadowRadius: Dp by mutableStateOf(shadowRadius)

    /** Defines how shadow is shifted from the page. A little shift may add more realism. */
    var shadowOffset: DpOffset by mutableStateOf(shadowOffset)

    /** True if forward drag interaction is enabled or not. */
    var dragForwardEnabled: Boolean by mutableStateOf(dragForwardEnabled)

    /** True if backward drag interaction is enabled or not. */
    var dragBackwardEnabled: Boolean by mutableStateOf(dragBackwardEnabled)

    /** True if forward tap interaction is enabled or not. */
    var tapForwardEnabled: Boolean by mutableStateOf(tapForwardEnabled)

    /** True if backward tap interaction is enabled or not. */
    var tapBackwardEnabled: Boolean by mutableStateOf(tapBackwardEnabled)

    /** True if custom tap interaction is enabled or not, see [onCustomTap]. */
    var tapCustomEnabled: Boolean by mutableStateOf(tapCustomEnabled)

    /** The drag interaction setting. */
    var dragInteraction: DragInteraction by mutableStateOf(dragInteraction)

    /** The tap interaction setting. */
    var tapInteraction: TapInteraction by mutableStateOf(tapInteraction)

    /**
     * Defines the strategy for handling drag gestures to initiate page curls.
     */
    sealed interface DragInteraction {

        /**
         * Specifies how the pointer's movement influences the curl animation.
         */
        val pointerBehavior: PointerBehavior

        /**
         * Enumerates available pointer behaviors for drag interactions.
         */
        enum class PointerBehavior {
            /**
             * The default behavior: the curl line (dividing back page and next page front) is anchored to the user's finger.
             * Dragging to the left edge fully reveals the next page.
             */
            Default,

            /**
             * Page-edge behavior: the right edge of the current (curling) page is anchored to the user's finger.
             * Dragging to the left edge reveals half of the next page.
             */
            PageEdge;
        }
    }

    /**
     * Drag interaction based on defined start and end target areas.
     * A curl initiates if a drag starts in the [Config.start] [Rect] and successfully completes if it ends in the [Config.end] [Rect].
     *
     * @property pointerBehavior The [DragInteraction.PointerBehavior] for this interaction.
     * @property forward Configuration for forward drag (e.g., turning to the next page).
     * @property backward Configuration for backward drag (e.g., turning to the previous page).
     */
    data class StartEndDragInteraction(
        override val pointerBehavior: DragInteraction.PointerBehavior = DragInteraction.PointerBehavior.Default,
        val forward: Config = Config(start = rightHalf(), end = leftHalf()),
        val backward: Config = Config(start = leftHalf(), end = rightHalf())
    ) : DragInteraction {

        /**
         * Configuration for a specific drag direction (forward or backward) in [StartEndDragInteraction].
         *
         * @property start Defines a [Rect] (relative coordinates 0.0-1.0) where the drag must start.
         * @property end Defines a [Rect] (relative coordinates 0.0-1.0) where the drag must end for success.
         */
        data class Config(val start: Rect, val end: Rect)
    }

    /**
     * Drag interaction based on the initial direction of the drag gesture within a target area.
     * A curl initiates if a drag starts within the [Config.target] [Rect].
     *
     * @property pointerBehavior The [DragInteraction.PointerBehavior] for this interaction.
     * @property forward Configuration for forward drag.
     * @property backward Configuration for backward drag.
     */
    data class GestureDragInteraction(
        override val pointerBehavior: DragInteraction.PointerBehavior = DragInteraction.PointerBehavior.Default,
        val forward: Config = Config(target = full()),
        val backward: Config = Config(target = full()),
    ) : DragInteraction {

        /**
         * Configuration for a specific drag direction (forward or backward) in [GestureDragInteraction].
         *
         * @property target Defines a [Rect] (relative coordinates 0.0-1.0) where the drag interaction is captured.
         */
        data class Config(val target: Rect)
    }

    /**
     * Defines the strategy for handling tap gestures to initiate page turns.
     */
    sealed interface TapInteraction

    /**
     * Tap interaction based on specific target areas within the PageCurl composable.
     * A tap within a [Config.target] [Rect] initiates a page turn.
     *
     * @property forward Configuration for forward tap (e.g., turning to the next page).
     * @property backward Configuration for backward tap (e.g., turning to the previous page).
     */
    data class TargetTapInteraction(
        val forward: Config = Config(target = rightHalf()),
        val backward: Config = Config(target = leftHalf())
    ) : TapInteraction {

        /**
         * Configuration for a specific tap direction (forward or backward) in [TargetTapInteraction].
         *
         * @property target Defines a [Rect] (relative coordinates 0.0-1.0) where a tap is captured for this action.
         */
        data class Config(val target: Rect)
    }
}

/**
 * Returns a [Rect] covering the full area (0.0, 0.0) to (1.0, 1.0).
 * Used for defining interaction targets relative to the PageCurl composable size.
 */
private fun full(): Rect = Rect(0.0f, 0.0f, 1.0f, 1.0f)

/**
 * Returns a [Rect] covering the left half of an area (0.0, 0.0) to (0.5, 1.0).
 * Used for defining interaction targets relative to the PageCurl composable size.
 */
private fun leftHalf(): Rect = Rect(0.0f, 0.0f, 0.5f, 1.0f)

/**
 * Returns a [Rect] covering the right half of an area (0.5, 0.0) to (1.0, 1.0).
 * Used for defining interaction targets relative to the PageCurl composable size.
 */
private fun rightHalf(): Rect = Rect(0.5f, 0.0f, 1.0f, 1.0f)
