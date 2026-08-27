package dev.diegoflassa.comiqueta.core.data.timber

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * The `release` tree (`LOGGING_RULES.md` §8.6).
 *
 * §8.6 defines the release gate as *"drops `logD`/`logV` and forwards the rest"*, and lists what has to
 * survive: every failure branch, every external boundary outcome, library integrity and the boot gates.
 * `INFO` and `WARN` carry most of that, so they are forwarded here rather than dropped.
 *
 * Two things this tree deliberately does **not** do:
 *  - **It does not redact.** §8.3 puts that at the call site, where the value is still typed
 *    ([LogRedaction]). A tree only sees a finished string and would have to guess.
 *  - **It does not call back into `Timber`.** The previous version did, as
 *    `Timber.log(priority, tag, message, t)` — which binds to `log(priority, message, vararg args)`,
 *    so the **tag** was passed as the message and the real message and throwable became format
 *    arguments that no specifier consumed. Every release-build error therefore logged nothing but its
 *    own tag: no text, no stack trace. It also re-entered this tree once before Timber dropped the
 *    now-empty message.
 */
class CrashReportingTree : Timber.Tree() {

    /**
     * Skips the two developer-chatter levels before Timber builds the message. Anything at or above
     * `INFO` proceeds to [log].
     */
    public override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.INFO

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val label = tag ?: DEFAULT_TAG

        // Straight to the platform, never back through Timber: this tree is planted in the forest,
        // so a Timber.log() here re-enters this same method, and the overload it resolves to drops
        // everything except the tag.
        if (priority == Log.ASSERT) {
            Log.wtf(label, message)
        } else {
            Log.println(priority, label, message)
        }

        val crashlytics = FirebaseCrashlytics.getInstance()

        // Breadcrumbs. Crashlytics keeps a bounded ring of these and attaches them to whatever
        // crashes next, which is what turns a stack trace into a sequence of events.
        crashlytics.log("${priorityLabel(priority)}/$label: $message")

        // A throwable that was handled still explains a user-visible failure, so it is reported as a
        // non-fatal rather than left as a breadcrumb nobody aggregates.
        if (t != null && priority >= Log.ERROR) {
            crashlytics.recordException(t)
        }
    }

    private fun priorityLabel(priority: Int): String = when (priority) {
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        else -> priority.toString()
    }

    private companion object {
        const val DEFAULT_TAG = "Comiqueta"
    }
}
