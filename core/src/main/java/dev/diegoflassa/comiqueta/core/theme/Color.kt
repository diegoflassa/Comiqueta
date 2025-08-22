@file:Suppress("unused", "UnusedReceiverParameter")

package dev.diegoflassa.comiqueta.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

object ComiquetaLightColors {
    val primaryLight = Color(0xFFBCBCBC)//OK
    val onPrimaryLight = Color(0xFFFFFFFF)//OK
    val primaryContainerLight = Color(0xFFFFFFFF)//OK
    val onPrimaryContainerLight = Color(0xFF231F20)//OK
    val secondaryLight = Color(0xFF00FF00)
    val onSecondaryLight = Color(0xFF0000FF)
    val secondaryContainerLight = Color(0xFFFF0000)
    val onSecondaryContainerLight = Color(0xFF00FF00)
    val tertiaryLight = Color(0xFF1E1E1E)//OK
    val onTertiaryLight = Color(0xFFFFFFFF)//OK
    val tertiaryContainerLight = Color(0xFF00FF00)
    val onTertiaryContainerLight = Color(0xFF0000FF)
    val errorLight = Color(0xFFBA1A1A)//OK
    val onErrorLight = Color(0xFFFFFFFF)//OK
    val errorContainerLight = Color(0xFFFFDAD6)//OK
    val onErrorContainerLight = Color(0xFF93000A)//OK
    val backgroundLight = Color(0xFF231F20)//OK
    val onBackgroundLight = Color(0xFFFFFFFF)//OK
    val surfaceLight = Color(0xFFFCF8F8)//OK
    val onSurfaceLight = Color(0xFF1C1B1B)//OK
    val surfaceVariantLight = Color(0xFFFFFFFF)//OK
    val onSurfaceVariantLight = Color(0xFF444748)//OK
    val outlineLight = Color(0xFFFF0000)
    val outlineVariantLight = Color(0xFFC4C7C7)//OK
    val scrimLight = Color(0xFF0000FF)
    val inverseSurfaceLight = Color(0xFFFF0000)
    val inverseOnSurfaceLight = Color(0xFF00FF00)
    val inversePrimaryLight = Color(0xFF0000FF)
    val surfaceDimLight = Color(0xFFFF0000)
    val surfaceBrightLight = Color(0xFF00FF00)
    val surfaceContainerLowestLight = Color(0xFF0000FF)
    val surfaceContainerLowLight = Color(0xFFFF0000)
    val surfaceContainerLight = Color(0xFFF1EDEC)//OK
    val surfaceContainerHighLight = Color(0xFF0000FF)
    val surfaceContainerHighestLight = Color(0xFFE5E2E1)//OK

    //Custom colors
    val settingIconTintLight = Color(0xFF231F20)

    //Tab colors
    val tabSelectedTextLight = Color(0xFFBCBCBC)
    val tabUnselectedTextLight = Color(0xFFFFFFFF)

    //BottomAppBar colors
    val bottomAppBarSelectedIconLight = Color(0xFF333333)
    val bottomAppBarUnselectedIconLight = Color(0xFFBCBCBC)
    val bottomAppBarSelectedTextLight = Color(0xFF333333)
    val bottomAppBarUnselectedTextLight = Color(0xFFBCBCBC)

    //Header colors
    val headerSelectedIconLight = Color(0xFFBCBCBC)
    val headerUnselectedIconLight = Color(0xFFFFFFFF)

    //ComicsRow colors
    val trackBarTrackColorLight = Color(0xFFF2F2F2)
    val trackBarThumbColorLight = Color(0xFF333333)

    //ComicListItem colors
    val comicListItemTitleTextColorLight = Color(0xFF000000)
    val comicListItemSubtitleTextColorLight = Color(0xFFB1B1B1)
    val comicListItemIconColorLight = Color(0xFF333333)
}

object ComiquetaDarkColors {
    val primaryDark = Color(0xFF231F20)//Ok
    val onPrimaryDark = Color(0xFFFFFFFF)//OK
    val primaryContainerDark = Color(0xFF231F20)//OK
    val onPrimaryContainerDark = Color(0xFFFFFFFF)//OK
    val secondaryDark = Color(0xFF00FF00)
    val onSecondaryDark = Color(0xFF0000FF)
    val secondaryContainerDark = Color(0xFFFF0000)
    val onSecondaryContainerDark = Color(0xFF00FF00)
    val tertiaryDark = Color(0xFFFFFFFF)//Ok
    val onTertiaryDark = Color(0xFF303030)//Ok
    val tertiaryContainerDark = Color(0xFF00FF00)
    val onTertiaryContainerDark = Color(0xFF0000FF)
    val errorDark = Color(0xFFFFB4AB)//Ok
    val onErrorDark = Color(0xFF690005)//Ok
    val errorContainerDark = Color(0xFF93000A)//Ok
    val onErrorContainerDark = Color(0xFFFFDAD6)//Ok
    val backgroundDark = Color(0xFFFFFFFF)//Ok
    val onBackgroundDark = Color(0xFF333333)//OK
    val surfaceDark = Color(0xFF231F20)//Ok
    val onSurfaceDark = Color(0xFFF8F8F8)//Ok
    val surfaceVariantDark = Color(0xFFFFFFFF)//Ok
    val onSurfaceVariantDark = Color(0xFF333333)//Ok
    val outlineDark = Color(0xFF00FF00)
    val outlineVariantDark = Color(0xFF444748)//OK
    val scrimDark = Color(0xFFFF0000)
    val inverseSurfaceDark = Color(0xFF00FF00)
    val inverseOnSurfaceDark = Color(0xFF0000FF)
    val inversePrimaryDark = Color(0xFFFF0000)
    val surfaceDimDark = Color(0xFF00FF00)
    val surfaceBrightDark = Color(0xFF0000FF)
    val surfaceContainerLowestDark = Color(0xFFFF0000)
    val surfaceContainerLowDark = Color(0xFF00FF00)
    val surfaceContainerDark = Color(0xFF201F1F)//OK
    val surfaceContainerHighDark = Color(0xFFFF0000)
    val surfaceContainerHighestDark = Color(0xFFFFFFFF)//OK

    //Custom colors
    val settingIconTintDark = Color(0xFFFFFFFF)

    //Tab colors
    val tabSelectedTextDark = Color(0xFF333333)
    val tabUnselectedTextDark = Color(0xFFBCBCBC)

    //BottomAppBar colors
    val bottomAppBarSelectedIconDark = Color(0xFFFFFFFF)
    val bottomAppBarUnselectedIconDark = Color(0xFFBCBCBC)
    val bottomAppBarSelectedTextDark = Color(0xFFFFFFFF)
    val bottomAppBarUnselectedTextDark = Color(0xFFBCBCBC)

    //Header colors
    val headerSelectedIconDark = Color(0xFFBCBCBC)
    val headerUnselectedIconDark = Color(0xFF333333)

    //ComicsRow colors
    val trackBarTrackColorDark = Color(0xFFF2F2F2)
    val trackBarThumbColorDark = Color(0xFF333333)

    //ComicListItem colors
    val comicListItemTitleTextColorDark = Color(0xFF000000)
    val comicListItemSubtitleTextColorDark = Color(0xFFB1B1B1)
    val comicListItemIconColorDark = Color(0xFF333333)
}

@Composable
fun getOutlinedTextFieldDefaultsColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFFF8F8F8),
        unfocusedBorderColor = Color(0xFFF8F8F8),
        focusedContainerColor = Color(0xFFF8F8F8),
        unfocusedContainerColor = Color(0xFFF8F8F8),
        cursorColor = Color(0xFF333333),
        focusedTextColor = Color(0xFF333333),
        unfocusedTextColor = Color(0xFF333333)
    )
}

internal val comiquetaLightColorScheme = lightColorScheme(
    primary = ComiquetaLightColors.primaryLight,
    onPrimary = ComiquetaLightColors.onPrimaryLight,
    primaryContainer = ComiquetaLightColors.primaryContainerLight,
    onPrimaryContainer = ComiquetaLightColors.onPrimaryContainerLight,
    secondary = ComiquetaLightColors.secondaryLight,
    onSecondary = ComiquetaLightColors.onSecondaryLight,
    secondaryContainer = ComiquetaLightColors.secondaryContainerLight,
    onSecondaryContainer = ComiquetaLightColors.onSecondaryContainerLight,
    tertiary = ComiquetaLightColors.tertiaryLight,
    onTertiary = ComiquetaLightColors.onTertiaryLight,
    tertiaryContainer = ComiquetaLightColors.tertiaryContainerLight,
    onTertiaryContainer = ComiquetaLightColors.onTertiaryContainerLight,
    error = ComiquetaLightColors.errorLight,
    onError = ComiquetaLightColors.onErrorLight,
    errorContainer = ComiquetaLightColors.errorContainerLight,
    onErrorContainer = ComiquetaLightColors.onErrorContainerLight,
    background = ComiquetaLightColors.backgroundLight,
    onBackground = ComiquetaLightColors.onBackgroundLight,
    surface = ComiquetaLightColors.surfaceLight,
    onSurface = ComiquetaLightColors.onSurfaceLight,
    surfaceVariant = ComiquetaLightColors.surfaceVariantLight,
    onSurfaceVariant = ComiquetaLightColors.onSurfaceVariantLight,
    outline = ComiquetaLightColors.outlineLight,
    outlineVariant = ComiquetaLightColors.outlineVariantLight,
    scrim = ComiquetaLightColors.scrimLight,
    inverseSurface = ComiquetaLightColors.inverseSurfaceLight,
    inverseOnSurface = ComiquetaLightColors.inverseOnSurfaceLight,
    inversePrimary = ComiquetaLightColors.inversePrimaryLight,
    surfaceDim = ComiquetaLightColors.surfaceDimLight,
    surfaceBright = ComiquetaLightColors.surfaceBrightLight,
    surfaceContainerLowest = ComiquetaLightColors.surfaceContainerLowestLight,
    surfaceContainerLow = ComiquetaLightColors.surfaceContainerLowLight,
    surfaceContainer = ComiquetaLightColors.surfaceContainerLight,
    surfaceContainerHigh = ComiquetaLightColors.surfaceContainerHighLight,
    surfaceContainerHighest = ComiquetaLightColors.surfaceContainerHighestLight,
)

internal val comiquetaDarkColorScheme = darkColorScheme(
    primary = ComiquetaDarkColors.primaryDark,
    onPrimary = ComiquetaDarkColors.onPrimaryDark,
    primaryContainer = ComiquetaDarkColors.primaryContainerDark,
    onPrimaryContainer = ComiquetaDarkColors.onPrimaryContainerDark,
    secondary = ComiquetaDarkColors.secondaryDark,
    onSecondary = ComiquetaDarkColors.onSecondaryDark,
    secondaryContainer = ComiquetaDarkColors.secondaryContainerDark,
    onSecondaryContainer = ComiquetaDarkColors.onSecondaryContainerDark,
    tertiary = ComiquetaDarkColors.tertiaryDark,
    onTertiary = ComiquetaDarkColors.onTertiaryDark,
    tertiaryContainer = ComiquetaDarkColors.tertiaryContainerDark,
    onTertiaryContainer = ComiquetaDarkColors.onTertiaryContainerDark,
    error = ComiquetaDarkColors.errorDark,
    onError = ComiquetaDarkColors.onErrorDark,
    errorContainer = ComiquetaDarkColors.errorContainerDark,
    onErrorContainer = ComiquetaDarkColors.onErrorContainerDark,
    background = ComiquetaDarkColors.backgroundDark,
    onBackground = ComiquetaDarkColors.onBackgroundDark,
    surface = ComiquetaDarkColors.surfaceDark,
    onSurface = ComiquetaDarkColors.onSurfaceDark,
    surfaceVariant = ComiquetaDarkColors.surfaceVariantDark,
    onSurfaceVariant = ComiquetaDarkColors.onSurfaceVariantDark,
    outline = ComiquetaDarkColors.outlineDark,
    outlineVariant = ComiquetaDarkColors.outlineVariantDark,
    scrim = ComiquetaDarkColors.scrimDark,
    inverseSurface = ComiquetaDarkColors.inverseSurfaceDark,
    inverseOnSurface = ComiquetaDarkColors.inverseOnSurfaceDark,
    inversePrimary = ComiquetaDarkColors.inversePrimaryDark,
    surfaceDim = ComiquetaDarkColors.surfaceDimDark,
    surfaceBright = ComiquetaDarkColors.surfaceBrightDark,
    surfaceContainerLowest = ComiquetaDarkColors.surfaceContainerLowestDark,
    surfaceContainerLow = ComiquetaDarkColors.surfaceContainerLowDark,
    surfaceContainer = ComiquetaDarkColors.surfaceContainerDark,
    surfaceContainerHigh = ComiquetaDarkColors.surfaceContainerHighDark,
    surfaceContainerHighest = ComiquetaDarkColors.surfaceContainerHighestDark,
)

@Composable
@ReadOnlyComposable
fun getComiquetaColorScheme(
    darkTheme: Boolean,
    //dynamicColor: Boolean = true
): ColorScheme {
    return when {
        //dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        //    val context = LocalContext.current
        //    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        //}

        darkTheme -> comiquetaDarkColorScheme
        else -> comiquetaLightColorScheme
    }
}

val ColorScheme.transparent: Color
    @Composable
    @ReadOnlyComposable
    get() {
        return Color(0x00000000)
    }

val ColorScheme.settingIconTint: Color
    @Composable
    @ReadOnlyComposable
    get() {
        return if (this.primary == comiquetaLightColorScheme.primary) {
            ComiquetaLightColors.settingIconTintLight
        } else {
            ComiquetaDarkColors.settingIconTintDark
        }
    }

//Tab colors

val ColorScheme.tabSelectedText: Color
    @Composable
    @ReadOnlyComposable
    get() {
        return if (this.primary == comiquetaLightColorScheme.primary) {
            ComiquetaLightColors.tabSelectedTextLight
        } else {
            ComiquetaDarkColors.tabSelectedTextDark
        }
    }

val ColorScheme.tabUnselectedText: Color
    @Composable
    @ReadOnlyComposable
    get() {
        return if (this.primary == comiquetaLightColorScheme.primary) {
            ComiquetaLightColors.tabUnselectedTextLight
        } else {
            ComiquetaDarkColors.tabUnselectedTextDark
        }
    }

//BottomAppBar colors

val ColorScheme.bottomAppBarSelectedIcon: Color
    @Composable
    @ReadOnlyComposable
    get() {
        return if (this.primary == comiquetaLightColorScheme.primary) {
            ComiquetaLightColors.bottomAppBarSelectedIconLight
        } else {
            ComiquetaDarkColors.bottomAppBarSelectedIconDark
        }
    }

val ColorScheme.bottomAppBarUnselectedIcon: Color
    @Composable
    @ReadOnlyComposable
    get() {
        return if (this.primary == comiquetaLightColorScheme.primary) {
            ComiquetaLightColors.bottomAppBarUnselectedIconLight
        } else {
            ComiquetaDarkColors.bottomAppBarUnselectedIconDark
        }
    }

val ColorScheme.bottomAppBarSelectedText: Color
    @Composable
    @ReadOnlyComposable
    get() {
        return if (this.primary == comiquetaLightColorScheme.primary) {
            ComiquetaLightColors.bottomAppBarSelectedTextLight
        } else {
            ComiquetaDarkColors.bottomAppBarSelectedTextDark
        }
    }

val ColorScheme.bottomAppBarUnselectedText: Color
    @Composable
    @ReadOnlyComposable
    get() {
        return if (this.primary == comiquetaLightColorScheme.primary) {
            ComiquetaLightColors.bottomAppBarUnselectedTextLight
        } else {
            ComiquetaDarkColors.bottomAppBarUnselectedTextDark
        }
    }

//Header colors

val ColorScheme.headerSelectedIcon: Color
    @Composable
    @ReadOnlyComposable
    get() {
        return if (this.primary == comiquetaLightColorScheme.primary) {
            ComiquetaLightColors.headerSelectedIconLight
        } else {
            ComiquetaDarkColors.headerSelectedIconDark
        }
    }

val ColorScheme.headerUnselectedIcon: Color
    @Composable
    @ReadOnlyComposable
    get() {
        return if (this.primary == comiquetaLightColorScheme.primary) {
            ComiquetaLightColors.headerUnselectedIconLight
        } else {
            ComiquetaDarkColors.headerUnselectedIconDark
        }
    }

//TrackBar colors

val ColorScheme.trackBarTrackColor: Color
    @Composable
    @ReadOnlyComposable
    get() {
        return if (this.primary == comiquetaLightColorScheme.primary) {
            ComiquetaLightColors.trackBarTrackColorLight
        } else {
            ComiquetaDarkColors.trackBarTrackColorDark
        }
    }

val ColorScheme.trackBarThumbColor: Color
    @Composable
    @ReadOnlyComposable
    get() {
        return if (this.primary == comiquetaLightColorScheme.primary) {
            ComiquetaLightColors.trackBarThumbColorLight
        } else {
            ComiquetaDarkColors.trackBarThumbColorDark
        }
    }

//ComicListItem colors

val ColorScheme.comicListItemTitleTextColor: Color
    @Composable
    @ReadOnlyComposable
    get() {
        return if (this.primary == comiquetaLightColorScheme.primary) {
            ComiquetaLightColors.comicListItemTitleTextColorLight
        } else {
            ComiquetaDarkColors.comicListItemTitleTextColorDark
        }
    }

val ColorScheme.comicListItemSubtitleTextColor: Color
    @Composable
    @ReadOnlyComposable
    get() {
        return if (this.primary == comiquetaLightColorScheme.primary) {
            ComiquetaLightColors.comicListItemSubtitleTextColorLight
        } else {
            ComiquetaDarkColors.comicListItemSubtitleTextColorDark
        }
    }

val ColorScheme.comicListItemIconColor: Color
    @Composable
    @ReadOnlyComposable
    get() {
        return if (this.primary == comiquetaLightColorScheme.primary) {
            ComiquetaLightColors.comicListItemIconColorLight
        } else {
            ComiquetaDarkColors.comicListItemIconColorDark
        }
    }
