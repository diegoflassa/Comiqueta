package dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.utils

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

/**
 * Rotates an [Offset] point around the origin (0,0) by a given angle.
 *
 * @param angle The angle of rotation in radians.
 * @return A new [Offset] representing the rotated point.
 */
internal fun Offset.rotate(angle: Float): Offset {
    val sin = sin(angle)
    val cos = cos(angle)
    return Offset(x * cos - y * sin, x * sin + y * cos)
}

/**
 * Calculates the intersection point of two lines, each defined by two [Offset] points.
 *
 * Line 1 is defined by points [line1a] and [line1b].
 * Line 2 is defined by points [line2a] and [line2b].
 *
 * @param line1a The first point of the first line segment.
 * @param line1b The second point of the first line segment.
 * @param line2a The first point of the second line segment.
 * @param line2b The second point of the second line segment.
 * @return An [Offset] representing the intersection point, or `null` if the lines are parallel
 *         or coincident (denominator is zero).
 */
internal fun lineLineIntersection(
    line1a: Offset,
    line1b: Offset,
    line2a: Offset,
    line2b: Offset,
): Offset? {
    val denominator = (line1a.x - line1b.x) * (line2a.y - line2b.y) - (line1a.y - line1b.y) * (line2a.x - line2b.x)
    if (denominator == 0f) return null

    val x1 = (line1a.x * line1b.y - line1a.y * line1b.x) * (line2a.x - line2b.x)
    val x2 = (line1a.x - line1b.x) * (line2a.x * line2b.y - line2a.y * line2b.x)
    val x = (x1 - x2) / denominator

    val y1 = (line1a.x * line1b.y - line1a.y * line1b.x) * (line2a.y - line2b.y)
    val y2 = (line1a.y - line1b.y) * (line2a.x * line2b.y - line2a.y * line2b.x)
    val y = (y1 - y2) / denominator
    return Offset(x, y)
}
