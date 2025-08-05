package dev.diegoflassa.comiqueta.core.ui.extensions

import java.util.Stack

fun <T> Stack<T>.copy(): Stack<T> {
    val newStack = Stack<T>()
    if (this.isNotEmpty()) {
        newStack.addAll(this)
    }
    return newStack
}
