package dev.diegoflassa.comiqueta.core.data.timber

import android.util.Log
import timber.log.Timber

/** A tree which logs important information for crash reporting.  */
class CrashReportingTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.ERROR || priority == Log.ASSERT) {
            Timber.log(priority, tag, message, t)
        } else {
            return
        }
    }
}
