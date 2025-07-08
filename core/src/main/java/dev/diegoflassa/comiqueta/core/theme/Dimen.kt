package dev.diegoflassa.comiqueta.core.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalDimen = staticCompositionLocalOf { Dimen() }

data class Dimen(
    val bottomAppBarIconSize: Dp = 19.dp
)
