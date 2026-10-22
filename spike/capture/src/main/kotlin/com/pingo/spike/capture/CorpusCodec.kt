package com.pingo.spike.capture

import java.nio.charset.StandardCharsets
import java.util.Base64

object CorpusCodec {
    private const val SEPARATOR = "\t"
    private const val FIELD_PREFIX = "b"
    private const val FIELD_COUNT = 6
    private const val CAPTURED_AT = 0
    private const val POSTED_AT = 1
    private const val PACKAGE_NAME = 2
    private const val TITLE = 3
    private const val TEXT = 4
    private const val BIG_TEXT = 5

    fun encode(entry: CorpusEntry): String = listOf(
        entry.capturedAt.toString(),
        entry.postedAt.toString(),
        encodeField(entry.packageName),
        encodeField(entry.title),
        encodeField(entry.text),
        encodeField(entry.bigText),
    ).joinToString(SEPARATOR)

    fun decode(line: String): CorpusEntry? = runCatching {
        val fields = line.split(SEPARATOR)
        check(fields.size == FIELD_COUNT)
        CorpusEntry(
            capturedAt = fields[CAPTURED_AT].toLong(),
            postedAt = fields[POSTED_AT].toLong(),
            packageName = decodeField(fields[PACKAGE_NAME]),
            title = decodeField(fields[TITLE]),
            text = decodeField(fields[TEXT]),
            bigText = decodeField(fields[BIG_TEXT]),
        )
    }.getOrNull()

    private fun encodeField(value: String): String =
        FIELD_PREFIX + Base64.getEncoder().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeField(value: String): String {
        check(value.startsWith(FIELD_PREFIX))
        val decoded = Base64.getDecoder().decode(value.substring(FIELD_PREFIX.length))
        return String(decoded, StandardCharsets.UTF_8)
    }
}
