package dev.diegoflassa.comiqueta.ui.home

import java.util.concurrent.atomic.AtomicBoolean

internal class HomeAddFolderGuard {
    private val inFlight = AtomicBoolean(false)

    fun claim(): Boolean = inFlight.compareAndSet(false, true)

    fun release() {
        inFlight.compareAndSet(true, false)
    }
}
