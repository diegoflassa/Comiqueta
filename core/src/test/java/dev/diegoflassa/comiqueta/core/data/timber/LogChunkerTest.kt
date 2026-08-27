package dev.diegoflassa.comiqueta.core.data.timber

import android.util.Log
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pins the two defects `LogChunker` and [CrashReportingTree] were written to fix (CORE_RULES §12).
 *
 * 1. A message over the logcat byte cap lost its tail silently, and any pieces that did survive were
 *    unfindable because only the first carried the `[Comiqueta][…]` filter.
 * 2. The release tree dropped `logI`/`logW`, which `LOGGING_RULES.md` §8.6 lists as mandatory-surviving.
 */
class LogChunkerTest {

    private val filter = "[Comiqueta][Viewer]"

    private fun String.utf8Size() = toByteArray(Charsets.UTF_8).size

    @Test
    fun `a message that fits is returned untouched`() {
        val message = "$filter scan finished comics=812"

        val pieces = LogChunker.split(message)

        assertThat(pieces).containsExactly(message)
    }

    @Test
    fun `an oversized message is split rather than truncated`() {
        val message = "$filter " + "a".repeat(10_000)

        val pieces = LogChunker.split(message)

        assertThat(pieces.size).isGreaterThan(1)
        // Nothing may be lost: the payload has to survive reassembly exactly.
        val rejoined = pieces.joinToString("") { it.substringAfter("] ", missingDelimiterValue = it) }
        assertThat(rejoined).isEqualTo("a".repeat(10_000))
    }

    @Test
    fun `every piece carries the filter so a grep finds the whole message`() {
        val message = "$filter " + "b".repeat(10_000)

        val pieces = LogChunker.split(message)

        assertThat(pieces).isNotEmpty()
        pieces.forEach { assertThat(it).startsWith(filter) }
    }

    @Test
    fun `every piece fits inside one logcat entry, counted in bytes not characters`() {
        // Accented text is the case Timber's own character-based splitter gets wrong: 4 000 of these
        // are 8 000 bytes, so a character budget would wave them through and the kernel would cut them.
        val message = "$filter " + "á".repeat(4_000)

        val pieces = LogChunker.split(message)

        assertThat(pieces.size).isGreaterThan(1)
        pieces.forEach { assertThat(it.utf8Size()).isAtMost(3_500) }
    }

    @Test
    fun `a surrogate pair is never split down the middle`() {
        val emoji = "😀" // U+1F600, one code point across two UTF-16 units
        val message = "$filter " + emoji.repeat(2_000)

        val pieces = LogChunker.split(message)

        assertThat(pieces.size).isGreaterThan(1)
        pieces.forEach { piece ->
            // A piece cut through a pair would end high or start low; either corrupts on encode.
            assertThat(piece.last().isHighSurrogate()).isFalse()
            assertThat(piece.first().isLowSurrogate()).isFalse()
        }
    }

    @Test
    fun `an untagged message still splits, just without a filter to repeat`() {
        val pieces = LogChunker.split("c".repeat(10_000))

        assertThat(pieces.size).isGreaterThan(1)
        pieces.forEach { assertThat(it).startsWith("[part ") }
    }

    @Test
    fun `the release tree keeps the levels section 8_6 calls mandatory-surviving`() {
        val tree = CrashReportingTree()

        // §8.6: "a release tree that drops logD/logV and forwards the rest". Before the fix this
        // tree forwarded only ERROR and ASSERT, so every milestone and every warning was invisible
        // in the field.
        assertThat(tree.isLoggable(null, Log.VERBOSE)).isFalse()
        assertThat(tree.isLoggable(null, Log.DEBUG)).isFalse()
        assertThat(tree.isLoggable(null, Log.INFO)).isTrue()
        assertThat(tree.isLoggable(null, Log.WARN)).isTrue()
        assertThat(tree.isLoggable(null, Log.ERROR)).isTrue()
        assertThat(tree.isLoggable(null, Log.ASSERT)).isTrue()
    }
}
