package dev.diegoflassa.comiqueta.core.data.timber

/**
 * Splits log messages that would not survive a single logcat write.
 *
 * Android's logging pipeline caps one entry at roughly 4 KB **counted in bytes** and discards the
 * remainder without saying so. A stack-trace dump, a SAF tree listing or a decode manifest therefore
 * arrives silently half-written — the precise failure `LOGGING_RULES.md` §8.2 exists to prevent, since
 * a truncated log is indistinguishable from a log that never mentioned the thing you are looking for.
 *
 * Timber's own `DebugTree` already splits long messages, but not in a way that helps here: it counts
 * **characters**, so 3 000 accented or CJK characters still overflow the byte cap, and the pieces it
 * emits carry no scenario filter — `logcat | grep "\[Comiqueta]\[Viewer]"` returns the first fragment
 * and silently hides the rest. This splitter counts bytes and repeats the filter on every piece, so a
 * grep on the filter still returns the whole message (§8.1).
 */
internal object LogChunker {

    /**
     * Byte budget for one emitted entry. Deliberately under the ~4 KB platform cap: the tag, the
     * priority and the process/thread preamble are all counted against the same limit and are not
     * visible from here.
     */
    private const val MAX_ENTRY_BYTES = 3_500

    /**
     * Reserved for the `[part 12/34] ` marker. Sized for a three-digit count on both sides, which at
     * this budget corresponds to a message of about 3 MB — far past anything worth emitting.
     */
    private const val MARKER_BYTES = 20

    /** The leading `[Comiqueta][Viewer][DECODE]` run, if the message carries one (§8.1). */
    private val LEADING_FILTERS = Regex("""^(?:\[[^\[\]]+])+""")

    /**
     * Returns the pieces to emit, in order. A message that already fits comes back as a single
     * element and is left completely untouched — no marker, no rewriting — so the common case is
     * byte-identical to not calling this at all.
     */
    fun split(message: String): List<String> {
        if (message.utf8Size() <= MAX_ENTRY_BYTES) return listOf(message)

        val filters = LEADING_FILTERS.find(message)?.value.orEmpty()
        // Drop the single space that separated the filters from the text: each piece re-adds one
        // after its own marker, and without this the first piece would carry two.
        val body = message.substring(filters.length).removePrefix(" ")

        val budget = MAX_ENTRY_BYTES - filters.utf8Size() - MARKER_BYTES
        // A filter run long enough to swallow the whole budget would loop forever below. Emitting the
        // message untouched keeps the old truncating behaviour, which is bad but bounded.
        if (budget <= 0) return listOf(message)

        val pieces = body.chunkByUtf8Budget(budget)
        return pieces.mapIndexed { index, piece ->
            "$filters[part ${index + 1}/${pieces.size}] $piece"
        }
    }

    private fun String.utf8Size(): Int = toByteArray(Charsets.UTF_8).size

    /**
     * Splits on whole code points, never on a UTF-16 surrogate pair: cutting one in half produces a
     * lone surrogate that the logcat encoder turns into a replacement character, corrupting the very
     * payload we split in order to preserve.
     */
    private fun String.chunkByUtf8Budget(budget: Int): List<String> {
        val pieces = mutableListOf<String>()
        val current = StringBuilder()
        var currentBytes = 0
        var index = 0

        while (index < length) {
            val codePoint = codePointAt(index)
            val charCount = Character.charCount(codePoint)
            val byteCount = utf8ByteCount(codePoint)

            if (currentBytes + byteCount > budget && current.isNotEmpty()) {
                pieces += current.toString()
                current.setLength(0)
                currentBytes = 0
            }

            current.appendRange(this, index, index + charCount)
            currentBytes += byteCount
            index += charCount
        }

        if (current.isNotEmpty()) pieces += current.toString()
        return pieces
    }

    private fun utf8ByteCount(codePoint: Int): Int = when {
        codePoint < 0x80 -> 1
        codePoint < 0x800 -> 2
        codePoint < 0x10000 -> 3
        else -> 4
    }
}
