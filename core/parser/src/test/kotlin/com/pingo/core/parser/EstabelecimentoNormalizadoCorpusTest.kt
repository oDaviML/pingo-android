package com.pingo.core.parser

import com.pingo.core.model.EstabelecimentoNormalizado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EstabelecimentoNormalizadoCorpusTest {
    private val estabelecimentos: List<String> by lazy {
        ReplayCorpus(DefaultParserEngine(), CorpusFixtures.regrasV1())
            .executar(CorpusFixtures.entradas())
            .mapNotNull { (it.resultado as? ResultadoParse.Transacao)?.dados?.estabelecimento }
    }

    @Test
    fun `variantes de DL UberRides do corpus viram a mesma chave normalizada`() {
        val chaves = estabelecimentos
            .filter { it.contains("UberRides") }
            .mapNotNull(EstabelecimentoNormalizado::de)
            .toSet()

        assertEquals(setOf("dl*uberrides"), chaves)
    }

    @Test
    fun `toda chave normalizada do corpus e idempotente e sem maiusculas, espacos sobrando ou sufixo numerico`() {
        val chaves = estabelecimentos.mapNotNull(EstabelecimentoNormalizado::de)

        assertTrue(chaves.isNotEmpty())
        assertEquals(emptyList<String>(), chaves.filterNot { EstabelecimentoNormalizado.de(it) == it })
        assertEquals(emptyList<String>(), chaves.filter { it != it.lowercase() || it.contains("  ") || it != it.trim() })
        assertEquals(emptyList<String>(), chaves.filter { Regex("\\*[0-9]+$").containsMatchIn(it) })
    }

    @Test
    fun `estabelecimentos transacionais do corpus nunca ficam sem chave`() {
        assertEquals(emptyList<String>(), estabelecimentos.filter { EstabelecimentoNormalizado.de(it) == null })
    }
}
