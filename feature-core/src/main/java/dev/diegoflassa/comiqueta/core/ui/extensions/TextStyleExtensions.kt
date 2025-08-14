@file:Suppress("unused")

package dev.diegoflassa.comiqueta.core.ui.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import dev.diegoflassa.comiqueta.core.ui.Resolution

@Composable
fun TextStyle.scaled(): TextStyle = copy(
    fontSize = fontSize.scaled(),
    lineHeight = lineHeight.scaled(),
    letterSpacing = letterSpacing.scaled()
)

@Composable
fun TextStyle.scaled(referencia: Resolution): TextStyle = copy(
    fontSize = fontSize.scaled(referencia),
    lineHeight = lineHeight.scaled(referencia),
    letterSpacing = letterSpacing.scaled(referencia)
)
