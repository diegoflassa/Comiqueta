package dev.diegoflassa.comiqueta.core.data.timber

import android.content.Context
import dev.diegoflassa.comiqueta.core.data.extensions.modoDebugHabilitado
import timber.log.Timber

object TimberManager {

    /**
     * Plants the tree for this build and arms call-site redaction in the same step.
     *
     * The two belong together: the tree decides which levels survive (`LOGGING_RULES.md` §8.6) and
     * [LogRedaction] decides what those surviving lines are allowed to say (§8.3). Arming one without
     * the other produces either a mute release build or a talkative one that leaks — so there is
     * exactly one call site for both, and it runs before anything else logs.
     */
    fun inicializar(context: Context) {
        val debugBuild = context.modoDebugHabilitado()
        LogRedaction.configure(isDebugBuild = debugBuild)
        if (debugBuild) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
    }
}
