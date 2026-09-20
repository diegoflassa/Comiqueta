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


private class BottomBarShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val s = size.width / 360f

        path.moveTo(0f * s, 0.02f * s)
        path.lineTo(131.79f * s, 0.02f * s)
        path.cubicTo(
            140.18f * s, -0.08f * s,
            147.96f * s, -0.39f * s,
            149.52f * s, 14.88f * s
        )
        path.cubicTo(
            154.08f * s, 47.7f * s,
            201.98f * s, 51.73f * s,
            211f * s, 15.92f * s
        )
        path.cubicTo(
            214.45f * s, 2.22f * s,
            212.56f * s, -0.08f * s,
            228.84f * s, 0.02f * s
        )
        path.lineTo(360f * s, 0.02f * s)
        path.lineTo(360f * s, 56f * s)
        path.lineTo(0f * s, 56f * s)
        path.lineTo(0f * s, 0.02f * s)
        path.close()

        return Outline.Generic(path)
    }
}

