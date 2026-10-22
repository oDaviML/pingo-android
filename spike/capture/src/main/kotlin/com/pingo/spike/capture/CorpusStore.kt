package com.pingo.spike.capture

import java.io.File

class CorpusStore(directory: File) {
    private val file = File(directory, CORPUS_FILE)

    init {
        directory.mkdirs()
    }

    @Synchronized
    fun append(entry: CorpusEntry) {
        file.appendText(CorpusCodec.encode(entry) + "\n")
    }

    @Synchronized
    fun readAll(): List<CorpusEntry> = if (file.exists()) {
        file.readLines().mapNotNull(CorpusCodec::decode).sortedByDescending { it.capturedAt }
    } else {
        emptyList()
    }

    @Synchronized
    fun clear() {
        file.delete()
    }

    @Synchronized
    fun exportReadable(target: File): File {
        target.parentFile?.mkdirs()
        target.writeText(readAll().joinToString(separator = "\n\n", transform = CorpusFormatter::describe))
        return target
    }

    companion object {
        const val DIRECTORY = "corpus"
        private const val CORPUS_FILE = "corpus.tsv"
    }
}
