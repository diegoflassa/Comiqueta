package dev.diegoflassa.comiqueta.core.ui.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.diegoflassa.comiqueta.core.ui.Resolution
import dev.diegoflassa.comiqueta.core.ui.calculateScale
import dev.diegoflassa.comiqueta.core.ui.getCurrentResolution

/**
 * Scales [TextUnit] (e.g., Sp) using the current screen resolution against the default reference resolution.
 */
@Composable
fun TextUnit.scaled(): TextUnit {
    if (value == 0f) {
        return 0.sp
    }
    val escala = calculateScale(getCurrentResolution())
    return (value * escala).sp
}

/**
 * Scales [TextUnit] (e.g., Sp) using the given reference resolution.
 */
@Composable
fun TextUnit.scaled(referencia: Resolution): TextUnit {
    if (value == 0f) {
        return 0.sp
    }
    val escala = calculateScale(referencia, getCurrentResolution())
    return (value * escala).sp
}
