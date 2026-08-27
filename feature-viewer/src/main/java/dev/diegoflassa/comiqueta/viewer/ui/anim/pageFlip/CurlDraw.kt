package dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.unit.dp
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.utils.Polygon
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.utils.lineLineIntersection
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.utils.rotate
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.config.PageCurlConfig
import java.lang.Float.max
import kotlin.math.atan2

/**
 * A [Modifier] that draws a page curl effect based on two anchor points [posA] and [posB].
 * It clips the original content to simulate the uncurled part of the page and draws a
 * mirrored and rotated representation for the curled back-page, including a shadow.
 *
 * @param config The [PageCurlConfig] to customize the appearance and behavior of the curl.
 * @param posA The top anchor point of the curl line.
 * @param posB The bottom anchor point of the curl line.
 * @return A [Modifier] that applies the draw transformations for the curl effect.
 */
internal fun Modifier.drawCurl(
    config: PageCurlConfig,
    posA: Offset,
    posB: Offset,
): Modifier = drawWithCache {
    // Fast-check if curl is in left most position (gesture is fully completed)
    // In such case do not bother and draw nothing
    if (posA == size.toRect().topLeft && posB == size.toRect().bottomLeft) {
        return@drawWithCache drawNothing()
    }

    // Fast-check if curl is in right most position (gesture is not yet started)
    // In such case do not bother and draw the full content
    if (posA == size.toRect().topRight && posB == size.toRect().bottomRight) {
        return@drawWithCache drawOnlyContent()
    }

    // Find the intersection of the curl line ([posA, posB]) and top and bottom sides, so that we may clip and mirror
    // content correctly
    val topIntersection = lineLineIntersection(
        Offset(0f, 0f), Offset(size.width, 0f),
        posA, posB
    )
    val bottomIntersection = lineLineIntersection(
        Offset(0f, size.height), Offset(size.width, size.height),
        posA, posB
    )

    // Should not really happen, but in case there is not intersection (curl line is horizontal), just draw the full
    // content instead
    if (topIntersection == null || bottomIntersection == null) {
        return@drawWithCache drawOnlyContent()
    }

    // Limit x coordinates of both intersections to be at least 0, so that page do not look like teared from the book
    val topCurlOffset = Offset(max(0f, topIntersection.x), topIntersection.y)
    val bottomCurlOffset = Offset(max(0f, bottomIntersection.x), bottomIntersection.y)

    // That is the easy part, prepare a lambda to draw the content clipped by the curl line
    val drawClippedContent = prepareClippedContent(topCurlOffset, bottomCurlOffset)
    // That is the tricky part, prepare a lambda to draw the back-page with the shadow
    val drawCurl = prepareCurl(config, topCurlOffset, bottomCurlOffset)

    onDrawWithContent {
        drawClippedContent()
        drawCurl()
    }
}

/**
 * Helper function to create a [DrawResult] that simply draws the original content.
 * Used when the curl effect is not active or in edge cases.
 */
private fun CacheDrawScope.drawOnlyContent(): DrawResult =
    onDrawWithContent {
        drawContent()
    }

/**
 * Helper function to create a [DrawResult] that draws nothing.
 * Used when the page is fully curled (e.g., at the leftmost position).
 */
private fun CacheDrawScope.drawNothing(): DrawResult =
    onDrawWithContent {
        /* Empty */
    }

/**
 * Prepares a drawing lambda to render the main content clipped by the curl line.
 * The clipping path is a quadrilateral formed by the left edge of the content area
 * and the intersection points of the curl line with the top and bottom edges.
 *
 * @param topCurlOffset The intersection point of the curl line with the top edge of the content area.
 * @param bottomCurlOffset The intersection point of the curl line with the bottom edge of the content area.
 * @return A lambda of type `ContentDrawScope.() -> Unit` that, when invoked, draws the clipped content.
 */
private fun CacheDrawScope.prepareClippedContent(
    topCurlOffset: Offset,
    bottomCurlOffset: Offset,
): ContentDrawScope.() -> Unit {
    // Make a quadrilateral from the left side to the intersection points
    val path = Path()
    path.lineTo(topCurlOffset.x, topCurlOffset.y)
    path.lineTo(bottomCurlOffset.x, bottomCurlOffset.y)
    path.lineTo(0f, size.height)
    return result@{
        // Draw a content clipped by the constructed path
        clipPath(path) {
            this@result.drawContent()
        }
    }
}

/**
 * Prepares a drawing lambda for the curled back-page.
 * This involves creating a polygon for the curled portion, calculating the rotation angle,
 * preparing the shadow, and then drawing the mirrored content with an overlay.
 *
 * @param config The [PageCurlConfig] for styling and behavior.
 * @param topCurlOffset The top intersection point of the curl line.
 * @param bottomCurlOffset The bottom intersection point of the curl line.
 * @return A lambda of type `ContentDrawScope.() -> Unit` that draws the complete back-page curl effect.
 */
private fun CacheDrawScope.prepareCurl(
    config: PageCurlConfig,
    topCurlOffset: Offset,
    bottomCurlOffset: Offset,
): ContentDrawScope.() -> Unit {
    // Build a quadrilateral of the part of the page which should be mirrored as the back-page
    // In all cases polygon should have 4 points, even when back-page is only a small "corner" (with 3 points) due to
    // the shadow rendering, otherwise it will create a visual artifact when switching between 3 and 4 points polygon
    val polygon = Polygon(
        sequence {
            // Find the intersection of the curl line and right side
            // If intersection is found adds to the polygon points list
            suspend fun SequenceScope<Offset>.yieldEndSideInterception() {
                val offset = lineLineIntersection(
                    topCurlOffset, bottomCurlOffset,
                    Offset(size.width, 0f), Offset(size.width, size.height)
                ) ?: return
                yield(offset)
                yield(offset)
            }

            // In case top intersection lays in the bounds of the page curl, take 2 points from the top side, otherwise
            // take the interception with a right side
            if (topCurlOffset.x < size.width) {
                yield(topCurlOffset)
                yield(Offset(size.width, topCurlOffset.y))
            } else {
                yieldEndSideInterception()
            }

            // In case bottom intersection lays in the bounds of the page curl, take 2 points from the bottom side,
            // otherwise take the interception with a right side
            if (bottomCurlOffset.x < size.width) {
                yield(Offset(size.width, size.height))
                yield(bottomCurlOffset)
            } else {
                yieldEndSideInterception()
            }
        }.toList()
    )

    // Calculate the angle in radians between X axis and the curl line, this is used to rotate mirrored content to the
    // right position of the curled back-page
    val lineVector = topCurlOffset - bottomCurlOffset
    val angle = Math.PI.toFloat() - atan2(lineVector.y, lineVector.x) * 2

    // Prepare a lambda to draw the shadow of the back-page
    val drawShadow = prepareShadow(config, polygon, angle)

    return result@{
        withTransform({
            // Mirror in X axis the drawing as back-page should be mirrored
            scale(-1f, 1f, pivot = bottomCurlOffset)
            // Rotate the drawing according to the curl line
            rotateRad(angle, pivot = bottomCurlOffset)
        }) {
            // Draw shadow first
            this@result.drawShadow()

            // And finally draw the back-page with an overlay with alpha
            clipPath(polygon.toPath()) {
                this@result.drawContent()

                val overlayAlpha = 1f - config.backPageContentAlpha
                drawRect(config.backPageColor.copy(alpha = overlayAlpha))
            }
        }
    }
}

/**
 * Prepares a drawing lambda for rendering the shadow of the curled back-page.
 * If shadow is disabled in [config], it returns a no-op lambda.
 *
 * @param config The [PageCurlConfig] containing shadow parameters.
 * @param polygon The [Polygon] defining the shape of the back-page curl, used to cast the shadow.
 * @param angle The angle of the curl, used to offset the shadow correctly.
 * @return A lambda of type `ContentDrawScope.() -> Unit` that draws the shadow.
 */
private fun CacheDrawScope.prepareShadow(
    config: PageCurlConfig,
    polygon: Polygon,
    angle: Float
): ContentDrawScope.() -> Unit {
    // Quick exit if no shadow is requested
    if (config.shadowAlpha == 0f || config.shadowRadius == 0.dp) {
        return { /* No shadow is requested */ }
    }

    // Prepare shadow parameters
    val radius = config.shadowRadius.toPx()
    val shadowColor = config.shadowColor.copy(alpha = config.shadowAlpha).toArgb()
    val transparent = config.shadowColor.copy(alpha = 0f).toArgb()
    val shadowOffset = Offset(-config.shadowOffset.x.toPx(), config.shadowOffset.y.toPx())
        .rotate(2 * Math.PI.toFloat() - angle)

    // Prepare shadow paint with a shadow layer
    val paint = Paint().apply {
        val frameworkPaint = nativePaint
        frameworkPaint.color = transparent
        frameworkPaint.setShadowLayer(
            config.shadowRadius.toPx(),
            shadowOffset.x,
            shadowOffset.y,
            shadowColor
        )
    }

    // Hardware acceleration supports setShadowLayer() only on API 28 and above, thus to support previous API versions
    // draw a shadow to the bitmap instead
    return prepareShadowApi28(radius, paint, polygon)
}

/**
 * Prepares a drawing lambda to render the shadow using a [Paint] object with a shadow layer.
 * This method is specifically for API 28+ where hardware acceleration for shadow layers is robust.
 *
 * @param radius The blur radius of the shadow.
 * @param paint The [Paint] object pre-configured with the shadow layer.
 * @param polygon The [Polygon] shape (offset for the shadow) to draw with the shadow paint.
 * @return A lambda of type `ContentDrawScope.() -> Unit` that draws the shadow path.
 */
private fun prepareShadowApi28(
    radius: Float,
    paint: Paint,
    polygon: Polygon,
): ContentDrawScope.() -> Unit = {
    drawIntoCanvas {
        it.nativeCanvas.drawPath(
            polygon
                .offset(radius).toPath()
                .asAndroidPath(),
            paint.nativePaint
        )
    }
}
