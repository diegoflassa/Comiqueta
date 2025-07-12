@file:Suppress("unused", "DEPRECATION")

package dev.diegoflassa.comiqueta.core.data.extensions

import android.net.Uri

fun List<Uri>.toStringList(): List<String> {
    return map { it.toString() }
}