package dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

/**
 * Scales the coordinates of a [Rect] by the dimensions of an [IntSize].
 * This is typically used when the [Rect] contains normalized coordinates (e.g., from 0.0 to 1.0)
 * and needs to be scaled to an absolute size in pixels.
 *
 * @param size The [IntSize] containing the width and height to scale the rectangle by.
 *             The [Rect.left] and [Rect.right] will be multiplied by `size.width`.
 *             The [Rect.top] and [Rect.bottom] will be multiplied by `size.height`.
 * @return A new [Rect] with its coordinates scaled by the given [IntSize].
 */
internal fun Rect.multiply(size: IntSize): Rect {
    return Rect(
        topLeft = Offset(size.width * left, size.height * top),
        bottomRight = Offset(size.width * right, size.height * bottom),
    )
}

