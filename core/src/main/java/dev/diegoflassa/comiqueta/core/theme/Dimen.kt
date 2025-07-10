package dev.diegoflassa.comiqueta.core.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalComiquetaDimen = staticCompositionLocalOf { ComiquetaDimen() }

data class ComiquetaDimen(
    val bottomAppBarIconSize: Dp = 19.dp
)
