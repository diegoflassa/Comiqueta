package dev.diegoflassa.comiqueta.core.data.timber

import android.content.Context
import dev.diegoflassa.comiqueta.core.data.extensions.modoDebugHabilitado
import timber.log.Timber

object TimberManager {
    fun inicializar(context: Context) {
        if (context.modoDebugHabilitado()) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
    }
}
