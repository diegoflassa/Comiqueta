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


private class BottomBarShapeFillWidth() : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()

        val originalDesignWidth = 360f
        val originalDesignHeight = 55.7f

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

private class BottomBarShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()

        // Original design height - this remains the reference for vertical scaling
        val originalDesignHeight = 55.7f

        // --- Define new proportions for the width ---
        // Length of the straight "wing" section on each side of the cutout, in original design units.
        // Adjust this value to control how much extends beyond the cutout.
        val sideWingDesignLength = 10f

        // Original X-coordinates defining the start and end of the cutout curves
        val originalCutoutStartX = 131.79f
        val originalCutoutEndX = 228.84f
        val cutoutDesignWidth = originalCutoutEndX - originalCutoutStartX // Should be 97.05f

        // New total design width for the shape's path
        val newOriginalDesignWidth = sideWingDesignLength + cutoutDesignWidth + sideWingDesignLength

        // Calculate scaling factors
        val scaleX = size.width / newOriginalDesignWidth
        val scaleY = size.height / originalDesignHeight

        // --- Apply the SVG path commands, adjusting X coordinates for the new proportions ---

        // M0,0.02
        path.moveTo(0f * scaleX, 0.02f * scaleY)

        // Line to the start of the cutout (left wing)
        path.lineTo(sideWingDesignLength * scaleX, 0.02f * scaleY)

        // Helper function to remap original X coordinates of the cutout to the new design
        val transformOriginalCutoutX = { originalX: Float ->
            (originalX - originalCutoutStartX) + sideWingDesignLength
        }

        // C1: First curve of the cutout
        path.cubicTo(
            transformOriginalCutoutX(140.18f) * scaleX, -0.08f * scaleY,
            transformOriginalCutoutX(147.96f) * scaleX, -0.39f * scaleY,
            transformOriginalCutoutX(149.52f) * scaleX, 14.88f * scaleY
        )

        // C2: Main dip of the cutout
        path.cubicTo(
            transformOriginalCutoutX(154.08f) * scaleX, 47.7f * scaleY,
            transformOriginalCutoutX(201.98f) * scaleX, 51.73f * scaleY,
            transformOriginalCutoutX(211f) * scaleX, 15.92f * scaleY
        )

        // C3: Second curve, returning to the top edge
        // The end X of this curve is (sideWingDesignLength + cutoutDesignWidth)
        path.cubicTo(
            transformOriginalCutoutX(214.45f) * scaleX, 2.22f * scaleY,
            transformOriginalCutoutX(212.56f) * scaleX, -0.08f * scaleY,
            (sideWingDesignLength + cutoutDesignWidth) * scaleX, 0.02f * scaleY
        )

        // Line to the end of the shape (right wing)
        path.lineTo(newOriginalDesignWidth * scaleX, 0.02f * scaleY)

        // V originalDesignHeight (scaled)
        path.lineTo(newOriginalDesignWidth * scaleX, originalDesignHeight * scaleY)

        // H0 (scaled)
        path.lineTo(0f * scaleX, originalDesignHeight * scaleY)

        // V0.02 (scaled)
        path.lineTo(0f * scaleX, 0.02f * scaleY)

        // Z (Close the path)
        path.close()

        return Outline.Generic(path)
    }
}

