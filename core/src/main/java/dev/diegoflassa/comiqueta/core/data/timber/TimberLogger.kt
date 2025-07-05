package dev.diegoflassa.comiqueta.core.data.timber

import android.util.Log
import timber.log.Timber

@Suppress("unused")
object TimberLogger {

    fun logD(tag: String? = null, mensagem: String, throwable: Throwable? = null) {
        if (tag.isNullOrEmpty()) {
            Timber.d(throwable, "%s", mensagem)
        } else {
            Timber.tag(tag).log(Log.DEBUG, throwable, "%s", mensagem)
        }
    }

    fun logI(tag: String? = null, mensagem: String, throwable: Throwable? = null) {
        if (tag.isNullOrEmpty()) {
            Timber.i(throwable, "%s", mensagem) // Corrected to Timber.i
        } else {
            Timber.tag(tag).log(Log.INFO, throwable, "%s", mensagem)
        }
    }

    fun logW(tag: String? = null, mensagem: String, throwable: Throwable? = null) {
        if (tag.isNullOrEmpty()) {
            Timber.w(throwable, "%s", mensagem)
        } else {
            Timber.tag(tag).log(Log.WARN, throwable, "%s", mensagem)
        }
    }

    fun logE(tag: String? = null, mensagem: String, throwable: Throwable? = null) {
        if (tag.isNullOrEmpty()) {
            Timber.e(throwable, "%s", mensagem)
        } else {
            Timber.tag(tag).log(Log.ERROR, throwable, "%s", mensagem)
        }
    }

    fun logA(tag: String? = null, mensagem: String, throwable: Throwable? = null) {
        if (tag.isNullOrEmpty()) {
            Timber.wtf(throwable, "%s", mensagem)
        } else {
            Timber.tag(tag).log(Log.ASSERT, throwable, "%s", mensagem)
        }
    }
}
