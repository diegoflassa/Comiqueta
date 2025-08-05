@file:Suppress("unused", "DEPRECATION")

package dev.diegoflassa.comiqueta.core.data.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun Int.toDp(): Dp {
    val density = LocalDensity.current
    return with(density) {
        toDp()
    }
}
