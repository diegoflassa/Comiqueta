package dev.diegoflassa.comiqueta.core.data.timber

import android.net.Uri

/**
 * Call-site redaction for log messages (`LOGGING_RULES.md` §8.3).
 *
 * §8.3 puts the decision **at the call site**, not in a Timber tree: a tree sees a finished string and
 * cannot tell a library path from a comic title, so anything it removed it would have to remove by
 * pattern-matching — which fails open on the first shape nobody predicted. These helpers are called
 * where the value is still typed and its meaning is still known.
 *
 * The other half of §8.3 is that **redaction is not deletion**. Every helper returns a placeholder
 * that keeps the length and, where it is not itself identifying, the shape — so a release capture can
 * still answer *was this field present, was it plausible, did it change between two runs* without ever
 * carrying the value. `release` is the only variant that redacts; `debug` runs on a developer machine
 * and keeps full fidelity.
 */
object LogRedaction {

    /**
     * Defaults to redacting. An un-configured path is then a path that over-redacts, which costs
     * detail; the opposite default would leak a user's real name out of a library path the first time
     * someone logged before [configure] ran.
     */
    @Volatile
    private var redacting: Boolean = true

    /** Called once from `MyApplication.onCreate`, before anything else logs. */
    fun configure(isDebugBuild: Boolean) {
        redacting = !isDebugBuild
    }

    /** True when the current build must redact — `release`. Exposed so a call site can branch. */
    val isRedacting: Boolean get() = redacting

    /**
     * A SAF URI. Keeps the scheme and authority — which provider answered is a diagnostic fact and
     * identifies nobody — and replaces the document id, which routinely embeds the user's real name
     * via a folder like `/storage/emulated/0/Users/Diego Lassa/Quadrinhos`.
     */
    fun uri(value: Uri?): String {
        if (value == null) return "null"
        if (!redacting) return value.toString()
        val prefix = "${value.scheme ?: "?"}://${value.authority ?: "?"}"
        val rest = value.toString().removePrefix(prefix)
        return "$prefix/[REDACTED len=${rest.length}]"
    }

    /** A filesystem path. Keeps only its depth and length; every segment can name a person. */
    fun path(value: String?): String {
        if (value == null) return "null"
        if (!redacting) return value
        val depth = value.count { it == '/' || it == '\\' }
        return "[REDACTED path depth=$depth len=${value.length}]"
    }

    /**
     * A file name. Keeps the extension, because `format=CBZ` is what distinguishes a decoder bug from
     * a corrupt archive, and drops the stem, which is the part that carries a title or a person.
     */
    fun fileName(value: String?): String {
        if (value == null) return "null"
        if (!redacting) return value
        val extension = value.substringAfterLast('.', missingDelimiterValue = "")
        val suffix = if (extension.isEmpty()) "" else " ext=$extension"
        return "[REDACTED len=${value.length}$suffix]"
    }

    /** Any other free-text value with no safe shape to keep — a title, a folder label, an account. */
    fun text(value: String?): String {
        if (value == null) return "null"
        return if (redacting) "[REDACTED len=${value.length}]" else value
    }
}
