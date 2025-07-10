@file:Suppress("unused")

package dev.diegoflassa.comiqueta.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.math.sqrt

/**
 * Singleton to hold the reference resolution for scaling.
 */
private object ResolucaoDeReferencia {
    var valor = Resolucao(360, 640)
}

fun setResolucaoReferencia(resolucao: Resolucao) {
    ResolucaoDeReferencia.valor = resolucao
}

fun setResolucaoReferencia(largura: Int, altura: Int) {
    ResolucaoDeReferencia.valor = Resolucao(largura, altura)
}

fun getResolucaoReferencia(): Resolucao = ResolucaoDeReferencia.valor

/**
 * Provide the current resolution for the platform.
 * You should implement this on each target platform (e.g., Android, iOS, Web, Desktop).
 */
@Composable
fun getResolucaoAtual(): Resolucao {
    val config = LocalConfiguration.current
    return Resolucao(config.screenWidthDp, config.screenHeightDp)
}

/**
 * Resolution scale factor between reference and target.
 */
fun calcularEscala(referencia: Resolucao, alvo: Resolucao): Float {
    val porcentagemLargura = alvo.largura.toFloat() / referencia.largura
    val porcentagemAltura = alvo.altura.toFloat() / referencia.altura
    return sqrt(porcentagemLargura * porcentagemAltura)
}

fun calcularEscala(alvo: Resolucao): Float {
    val ref = ResolucaoDeReferencia.valor
    val porcentagemLargura = alvo.largura.toFloat() / ref.largura
    val porcentagemAltura = alvo.altura.toFloat() / ref.altura
    return sqrt(porcentagemLargura * porcentagemAltura)
}
