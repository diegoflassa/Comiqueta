package dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/**
 * Represents a polygon defined by a list of vertices.
 *
 * @property vertices The list of [Offset] points that define the vertices of the polygon.
 */
internal data class Polygon(val vertices: List<Offset>) {

    private val size: Int = vertices.size

    /**
     * Translates the polygon by a given [Offset].
     *
     * @param offset The [Offset] by which to translate each vertex of the polygon.
     * @return A new [Polygon] instance with all vertices translated by the given offset.
     */
    fun translate(offset: Offset): Polygon =
        Polygon(vertices.map { it + offset })

    /**
     * Offsets the polygon edges by a given value along their normals.
     * This effectively creates an inset or outset version of the polygon.
     *
     * @param value The distance by which to offset the polygon. Positive values create an outset,
     *              negative values create an inset.
     * @return A new [Polygon] instance with its edges offset by the specified value.
     */
    fun offset(value: Float): Polygon {
        val edgeNormals = List(size) {
            val edge = vertices[index(it + 1)] - vertices[index(it)]
            Offset(edge.y, -edge.x).normalized()
        }

        val vertexNormals = List(size) {
            (edgeNormals[index(it - 1)] + edgeNormals[index(it)]).normalized()
        }

        return Polygon(
            vertices.mapIndexed { idx, vertex ->
                vertex + vertexNormals[idx] * value
            }
        )
    }

    /**
     * Converts the polygon to a [Path] object.
     * The path is closed by connecting the last vertex to the first.
     *
     * @return A [Path] representation of the polygon.
     */
    fun toPath(): Path {
        return Path().apply {
            vertices.forEachIndexed { idx, vertex ->
                if (idx == 0) {
                    moveTo(vertex.x, vertex.y)
                } else {
                    lineTo(vertex.x, vertex.y)
                }
            }
            // Implicitly closed by Path logic if fill is used, or explicitly close if stroke needs it.
            // For general utility, an explicit closePath() might be desired if the path is not always filled.
            // However, typical polygon rendering often assumes closure.
        }
    }

    private val index: (i: Int) -> Int = { i ->
        ((i % size) + size) % size
    }
}

private fun Offset.normalized(): Offset {
    val distance = getDistance()
    return if (distance != 0f) this / distance else this
}
