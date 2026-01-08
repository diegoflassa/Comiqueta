package dev.diegoflassa.comiqueta.core.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalComiquetaDimen = staticCompositionLocalOf { ComiquetaDimen() }

data class ComiquetaDimen(
    val noPadding: Dp = 0.dp,
    val appBarVerticalPadding: Dp = 17.dp,
    val appBarHorizontalPadding: Dp = 17.dp,
    val searchTopPadding: Dp = 23.dp,
    val searchBottomPadding: Dp = 30.dp,
    val searchHorizontalPadding: Dp = 20.dp,
    val tabHorizontalPadding: Dp = 20.dp,
    val tabIndicatorHeight: Dp = 3.dp,
    val inputHeight: Dp = 40.dp,
    val iconSettings: Dp = 24.dp,
    val iconSize: Dp = 18.dp,
    val fabDiameter: Dp = 45.dp,
    val fabIconSize: Dp = 22.dp,
    val bottomBarHeight: Dp = 50.dp,
    val bottomAppBarIconSize: Dp = 20.dp,
    val topAppBarHeight: Dp = 62.dp,
    val paddingExtraSmall: Dp = 4.dp,
    val paddingSmall: Dp = 8.dp,
    val paddingMedium: Dp = 12.dp,
    val paddingLarge: Dp = 16.dp,
    val paddingExtraLarge: Dp = 32.dp,
    val spacerSmall: Dp = 8.dp,
    val spacerMedium: Dp = 16.dp,
    val fabOffset: Dp = 17.dp,
    val searchRoundedCorner: Dp = 8.dp,
    val bannerHeight: Dp = 50.dp
)
