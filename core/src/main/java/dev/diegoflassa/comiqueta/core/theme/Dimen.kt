package dev.diegoflassa.comiqueta.core.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalComiquetaDimen = staticCompositionLocalOf { ComiquetaDimen() }

data class ComiquetaDimen(
    val fabDiameter: Dp = 48.dp,
    val bottomBarHeight: Dp = 50.dp,
    val bottomAppBarIconSize: Dp = 19.dp
)
