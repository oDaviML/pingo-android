package com.pingo.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CorpusFixturesTest {
    @Test
    fun `carrega 95, 27 e 90 entradas dos tres arquivos do corpus`() {
        val porArquivo = CorpusFixtures.entradas().groupingBy { it.arquivo }.eachCount()

        assertEquals(
            mapOf("pingo-corpus.txt" to 95, "pingo-corpus-iasmin.txt" to 27, "pingo-corpus-kaua.txt" to 90),
            porArquivo,
        )
    }

    @Test
    fun `cada entrada traz timestamp, app e pacote do cabecalho`() {
        val timestamp = Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}""")

        CorpusFixtures.entradas().forEach { entrada ->
            assertTrue("timestamp de ${entrada.arquivo}#${entrada.ordem}", timestamp.matches(entrada.timestamp))
            assertTrue("app de ${entrada.arquivo}#${entrada.ordem}", entrada.app.isNotBlank())
            assertTrue("pacote de ${entrada.arquivo}#${entrada.ordem}", entrada.pacote.contains('.'))
        }
    }

    @Test
    fun `preserva a continuacao de campos multilinha dentro da mesma notificacao`() {
        val multilinhas = CorpusFixtures.entradas().filter { it.texto.contains('\n') }

        assertEquals(2, multilinhas.size)
        assertTrue(multilinhas.all { it.texto.lines().size > 1 })
    }
}
