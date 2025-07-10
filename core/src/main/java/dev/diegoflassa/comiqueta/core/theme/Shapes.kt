package dev.diegoflassa.comiqueta.core.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

val LocalComiquetaShapes = staticCompositionLocalOf { ComiquetaShapes() }

data class ComiquetaShapes(
    val bottomBarShape: Shape = BottomBarShape(),
)

private class BottomBarShape() : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()

        // Define the original dimensions from which the SVG path data was designed.
        // This is crucial for correctly scaling the path to the current 'size'.
        val originalDesignWidth = 360f
        val originalDesignHeight = 55.7f // Based on the maximum Y coordinate in the path data

        // Calculate scaling factors for X and Y directions.
        // This ensures the path scales proportionally to fit the provided 'size'.
        val scaleX = size.width / originalDesignWidth
        val scaleY = size.height / originalDesignHeight

        // Apply the SVG path commands, scaling each coordinate.
        // M0,0.02
        path.moveTo(0f * scaleX, 0.02f * scaleY)

        // H131.79
        path.lineTo(131.79f * scaleX, 0.02f * scaleY)

        // C140.18,-0.08 147.96,-0.39 149.52,14.88
        path.cubicTo(
            140.18f * scaleX, -0.08f * scaleY, // Control point 1
            147.96f * scaleX, -0.39f * scaleY, // Control point 2
            149.52f * scaleX, 14.88f * scaleY  // End point
        )

        // C154.08,47.7 201.98,51.73 211,15.92
        path.cubicTo(
            154.08f * scaleX, 47.7f * scaleY,   // Control point 1
            201.98f * scaleX, 51.73f * scaleY,  // Control point 2
            211f * scaleX, 15.92f * scaleY      // End point
        )

        // C214.45,2.22 212.56,-0.08 228.84,0.02
        path.cubicTo(
            214.45f * scaleX, 2.22f * scaleY,   // Control point 1
            212.56f * scaleX, -0.08f * scaleY,  // Control point 2
            228.84f * scaleX, 0.02f * scaleY    // End point
        )

        // H360
        path.lineTo(360f * scaleX, 0.02f * scaleY)

        // V55.7
        path.lineTo(360f * scaleX, 55.7f * scaleY)

        // H0
        path.lineTo(0f * scaleX, 55.7f * scaleY)

        // V0.02
        path.lineTo(0f * scaleX, 0.02f * scaleY)

        // Z (Close the path)
        path.close()

        return Outline.Generic(path)
    }
}
