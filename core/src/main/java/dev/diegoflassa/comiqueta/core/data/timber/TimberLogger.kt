package dev.diegoflassa.comiqueta.core.data.timber

import android.util.Log
import timber.log.Timber

/**
 * The only sanctioned way to log (`LOGGING_RULES.md` §8). Raw `Timber.*` and `android.util.Log` are
 * forbidden outside this file and [CrashReportingTree].
 *
 * Every message passes through [LogChunker], so a line too long for one logcat entry is emitted as
 * several pieces that each repeat the scenario filter instead of being silently cut in half. Sensitive
 * values are redacted by the *caller* via [LogRedaction] — see §8.3 for why that decision cannot live
 * down here.
 */
@Suppress("unused")
object TimberLogger {

    fun logD(tag: String? = null, mensagem: String, throwable: Throwable? = null) =
        emit(Log.DEBUG, tag, mensagem, throwable)

    fun logI(tag: String? = null, mensagem: String, throwable: Throwable? = null) =
        emit(Log.INFO, tag, mensagem, throwable)

    fun logW(tag: String? = null, mensagem: String, throwable: Throwable? = null) =
        emit(Log.WARN, tag, mensagem, throwable)

    fun logE(tag: String? = null, mensagem: String, throwable: Throwable? = null) =
        emit(Log.ERROR, tag, mensagem, throwable)

    fun logA(tag: String? = null, mensagem: String, throwable: Throwable? = null) =
        emit(Log.ASSERT, tag, mensagem, throwable)

    private fun emit(priority: Int, tag: String?, mensagem: String, throwable: Throwable?) {
        val pieces = LogChunker.split(mensagem)
        pieces.forEachIndexed { index, piece ->
            // The throwable rides on the last piece only. Timber renders the whole stack trace onto
            // every message it is given, so attaching it to each piece would repeat the trace N times
            // and re-create the oversized entry the split just prevented.
            val attached = throwable.takeIf { index == pieces.lastIndex }
            // "%s" keeps the piece a literal argument: a message containing a stray % is a payload
            // here, not a format string, and formatting it would throw on the log call.
            if (tag.isNullOrEmpty()) {
                Timber.log(priority, attached, "%s", piece)
            } else {
                Timber.tag(tag).log(priority, attached, "%s", piece)
            }
        }
    }
}
