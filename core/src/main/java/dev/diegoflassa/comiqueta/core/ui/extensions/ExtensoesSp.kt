@file:Suppress("unused")

package dev.diegoflassa.comiqueta.core.ui.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.diegoflassa.comiqueta.core.ui.Resolucao
import dev.diegoflassa.comiqueta.core.ui.calcularEscala
import dev.diegoflassa.comiqueta.core.ui.getResolucaoAtual

/**
 * Scales [TextUnit] (e.g., Sp) using the current screen resolution against the default reference resolution.
 */
@Composable
fun TextUnit.scaled(): TextUnit {
    val escala = calcularEscala(getResolucaoAtual())
    return (value * escala).sp
}

/**
 * Scales [TextUnit] (e.g., Sp) using the given reference resolution.
 */
@Composable
fun TextUnit.scaled(referencia: Resolucao): TextUnit {
    val escala = calcularEscala(referencia, getResolucaoAtual())
    return (value * escala).sp
}
