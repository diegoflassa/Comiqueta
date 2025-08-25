@file:Suppress("unused")

package dev.diegoflassa.comiqueta.core.ui.extensions

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.unit.Dp

fun Modifier.containsWidthModifier(): Boolean = this.foldIn(false) { acc, element ->
    acc ||
        when (element) {
            is LayoutModifier -> {
                element == Modifier.wrapContentWidth() ||
                    element == Modifier.fillMaxWidth() ||
                    element == Modifier.width(
                        Dp.Unspecified
                    )
            }

            else -> false
        }
}

fun Modifier.containsHeightModifier(): Boolean = this.foldIn(false) { acc, element ->
    acc ||
        when (element) {
            is LayoutModifier -> {
                element == Modifier.wrapContentHeight() ||
                    element == Modifier.fillMaxHeight() ||
                    element == Modifier.height(
                        Dp.Unspecified
                    )
            }

            else -> false
        }
}

fun Modifier.containsSizeModifier(): Boolean = this.foldIn(false) { acc, element ->
    acc ||
        when (element) {
            is LayoutModifier -> {
                element == Modifier.wrapContentSize() ||
                    element == Modifier.fillMaxSize() ||
                    element == Modifier.size(
                        Dp.Unspecified
                    )
            }

            else -> false
        }
}

fun Modifier.containsWrapContentWidth(): Boolean = this.foldIn(false) { acc, element ->
    acc || (element == Modifier.wrapContentWidth())
}

fun Modifier.containsWrapContentHeight(): Boolean = this.foldIn(false) { acc, element ->
    acc || (element == Modifier.wrapContentHeight())
}

fun Modifier.containsWrapContentSize(): Boolean = this.foldIn(false) { acc, element ->
    acc || (element == Modifier.wrapContentSize())
}

fun Modifier.containsFillMaxWidth(): Boolean = this.foldIn(false) { acc, element ->
    acc || (element == Modifier.fillMaxWidth())
}

fun Modifier.hasWidthBeenSet(): Boolean = this.foldIn(false) { acc, element ->
    acc ||
        when (element) {
            is LayoutModifier -> {
                element == Modifier.width(Dp.Unspecified)
            }

            else -> false
        }
}

fun Modifier.hasHeightBeenSet(): Boolean = this.foldIn(false) { acc, element ->
    acc ||
        when (element) {
            is LayoutModifier -> {
                element == Modifier.height(Dp.Unspecified)
            }

            else -> false
        }
}

fun Modifier.hasSizeBeenSet(): Boolean = this.foldIn(false) { acc, element ->
    acc ||
        when (element) {
            is LayoutModifier -> {
                element == Modifier.size(Dp.Unspecified)
            }

            else -> false
        }
}
