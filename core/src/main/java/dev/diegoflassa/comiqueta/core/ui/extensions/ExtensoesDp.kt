@file:Suppress("unused")

package dev.diegoflassa.comiqueta.core.ui.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.diegoflassa.comiqueta.core.ui.Resolucao
import dev.diegoflassa.comiqueta.core.ui.calcularEscala
import dev.diegoflassa.comiqueta.core.ui.getResolucaoAtual

/**
 * Dp scaled using the current resolution and optionally a custom reference.
 */
@Composable
fun Dp.scaled(): Dp {
    val escala = calcularEscala(getResolucaoAtual())
    return (value * escala).dp
}

@Composable
fun Dp.scaled(referencia: Resolucao): Dp {
    val escala = calcularEscala(referencia, getResolucaoAtual())
    return (value * escala).dp
}
