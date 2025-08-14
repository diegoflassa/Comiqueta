package dev.diegoflassa.comiqueta.core.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Suppress("unused")
object ComiquetaTheme {
    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme

    val baselineTypography: Typography
        @Composable @ReadOnlyComposable get() = LocalBaselineTypography.current

    val typography: ComiquetaTypography
        @Composable @ReadOnlyComposable get() = LocalComiquetaTypography.current

    val shapes: ComiquetaShapes
        @Composable @ReadOnlyComposable get() = LocalComiquetaShapes.current

    val dimen: ComiquetaDimen
        @Composable @ReadOnlyComposable get() = LocalComiquetaDimen.current
}

@Composable
fun ComiquetaThemeContent(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    //dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val typography: Typography = LocalComiquetaTypography.current.typography
    val colorScheme = when {
        //dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        //    val context = LocalContext.current
        //    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        //}

        darkTheme -> comiquetaDarkColorScheme
        else -> comiquetaLightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            WindowCompat.setDecorFitsSystemWindows(window, false)

            if (Build.VERSION.SDK_INT < 35) {
                @Suppress("DEPRECATION")
                window.statusBarColor = colorScheme.primary.toArgb()
            }

            insetsController.isAppearanceLightStatusBars = !darkTheme

            if (Build.VERSION.SDK_INT < 35) {
                @Suppress("DEPRECATION")
                window.navigationBarColor = colorScheme.surface.toArgb()
            }
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
