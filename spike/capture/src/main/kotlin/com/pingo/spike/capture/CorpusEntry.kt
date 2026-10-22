package com.pingo.spike.capture

data class CorpusEntry(
    val packageName: String,
    val postedAt: Long,
    val capturedAt: Long,
    val title: String,
    val text: String,
    val bigText: String,
)
