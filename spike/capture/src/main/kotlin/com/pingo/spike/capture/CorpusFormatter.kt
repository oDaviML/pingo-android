package com.pingo.spike.capture

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CorpusFormatter {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun time(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(formatter)

    fun describe(entry: CorpusEntry): String = buildString {
        appendLine("${time(entry.capturedAt)} · ${KnownApps.displayName(entry.packageName)} (${entry.packageName})")
        appendLine("Título: ${entry.title}")
        appendLine("Texto: ${entry.text}")
        append("BigText: ${entry.bigText}")
    }
}
