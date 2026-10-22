package com.pingo.spike.capture

import java.io.File

class CorpusStore(directory: File) {
    private val file = File(directory, CORPUS_FILE)

    init {
        directory.mkdirs()
    }

    fun append(entry: CorpusEntry) {
        synchronized(FILE_LOCK) {
            file.appendText(CorpusCodec.encode(entry) + "\n")
        }
    }

    fun readAll(): List<CorpusEntry> = synchronized(FILE_LOCK) {
        if (file.exists()) {
            file.readLines().mapNotNull(CorpusCodec::decode).sortedByDescending { it.capturedAt }
        } else {
            emptyList()
        }
    }

    fun clear() {
        synchronized(FILE_LOCK) {
            file.delete()
        }
    }

    fun exportReadable(target: File): File {
        target.parentFile?.mkdirs()
        target.writeText(readAll().joinToString(separator = "\n\n", transform = CorpusFormatter::describe))
        return target
    }

    companion object {
        const val DIRECTORY = "corpus"
        private const val CORPUS_FILE = "corpus.tsv"
        private val FILE_LOCK = Any()
    }
}
