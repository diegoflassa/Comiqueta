@file:Suppress("unused")

package dev.diegoflassa.comiqueta.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import dev.diegoflassa.comiqueta.core.data.extensions.toDp
import kotlin.math.sqrt

/**
 * Singleton to hold the reference resolution for scaling.
 */
private object ReferenceResolution {
    var valor = Resolution(360, 640)
}

fun setReferenceResolution(resolution: Resolution) {
    ReferenceResolution.valor = resolution
}

fun setReferenceResolution(largura: Int, altura: Int) {
    ReferenceResolution.valor = Resolution(largura, altura)
}

fun getReferenceResolution(): Resolution = ReferenceResolution.valor

/**
 * Provide the current resolution for the platform.
 * You should implement this on each target platform (e.g., Android, iOS, Web, Desktop).
 */
@Composable
fun getCurrentResolution(): Resolution {
    val containerSize = LocalWindowInfo.current.containerSize
    return Resolution(containerSize.width.toDp().value.toInt(), containerSize.height.toDp().value.toInt())
}

/**
 * Resolution scale factor between reference and target.
 */
fun calculateScale(referencia: Resolution, alvo: Resolution): Float {
    val porcentagemLargura = alvo.largura.toFloat() / referencia.largura
    val porcentagemAltura = alvo.altura.toFloat() / referencia.altura
    return sqrt(porcentagemLargura * porcentagemAltura)
}

fun calculateScale(alvo: Resolution): Float {
    val ref = ReferenceResolution.valor
    val porcentagemLargura = alvo.largura.toFloat() / ref.largura
    val porcentagemAltura = alvo.altura.toFloat() / ref.altura
    return sqrt(porcentagemLargura * porcentagemAltura)
}
