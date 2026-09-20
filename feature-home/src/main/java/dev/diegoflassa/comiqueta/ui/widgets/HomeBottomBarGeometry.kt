package dev.diegoflassa.comiqueta.ui.widgets

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class HomeBottomBarGeometry(
    val scale: Float,
    val width: Dp,
    val bodyHeight: Dp,
    val circleDiameter: Dp,
    val centerX: Dp,
    val centerYFromBodyTop: Dp,
    val protrusion: Dp,
    val occupiedHeight: Dp,
    val cutoutWidth: Dp,
    val navIconSize: Dp,
    val actionIconSize: Dp,
) {
    companion object {
        const val DESIGN_WIDTH = 360f
        const val DESIGN_BODY_HEIGHT = 56f
        const val DESIGN_CIRCLE_DIAMETER = 52f
        const val DESIGN_CENTER_Y = 12f
        const val DESIGN_PROTRUSION = 14f
        const val DESIGN_CUTOUT_START_X = 131.79f
        const val DESIGN_CUTOUT_END_X = 228.84f
        const val DESIGN_NAV_ICON = 20f
        const val DESIGN_ACTION_ICON = 22f

        fun fromWidth(width: Dp): HomeBottomBarGeometry {
            val s = width.value / DESIGN_WIDTH
            return HomeBottomBarGeometry(
                scale = s,
                width = width,
                bodyHeight = (DESIGN_BODY_HEIGHT * s).dp,
                circleDiameter = (DESIGN_CIRCLE_DIAMETER * s).dp,
                centerX = width / 2f,
                centerYFromBodyTop = (DESIGN_CENTER_Y * s).dp,
                protrusion = (DESIGN_PROTRUSION * s).dp,
                occupiedHeight = ((DESIGN_BODY_HEIGHT + DESIGN_PROTRUSION) * s).dp,
                cutoutWidth = ((DESIGN_CUTOUT_END_X - DESIGN_CUTOUT_START_X) * s).dp,
                navIconSize = (DESIGN_NAV_ICON * s).dp,
                actionIconSize = (DESIGN_ACTION_ICON * s).dp,
            )
        }
    }
}
