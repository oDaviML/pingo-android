package com.pingo.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class CorpusReplayTest {
    private val entradas = CorpusFixtures.entradas()
    private val expectativas = CorpusFixtures.expectativas().associateBy { it.arquivo to it.ordem }
    private val desfechos: List<DesfechoReplay> by lazy {
        ReplayCorpus(DefaultParserEngine(), CorpusFixtures.regrasFixture()).executar(entradas)
    }

    @Test
    fun `tabela de expectativas tem a contagem esperada por classe`() {
        val porClasse = expectativas.values.groupingBy { it.esperado }.eachCount()

        assertEquals(
            mapOf(
                ClasseEsperada.TRANSACIONAL to 80,
                ClasseEsperada.RUIDO to 75,
                ClasseEsperada.VAZIO to 31,
                ClasseEsperada.FORA_V1 to 10,
                ClasseEsperada.NEGADA to 9,
                ClasseEsperada.NAO_BANCARIO to 7,
            ),
            porClasse,
        )
    }

    @Test
    fun `executa as 212 entradas do corpus sem excecao`() {
        assertEquals(212, entradas.size)
        assertEquals(212, desfechos.size)
    }

    @Test
    fun `cada entrada do corpus tem linha correspondente na tabela de expectativas`() {
        entradas.forEach { entrada ->
            val expectativa = expectativaDe(entrada)

            assertEquals("pacote de ${entrada.arquivo}#${entrada.ordem}", expectativa.pacote, entrada.pacote)
            assertEquals("timestamp de ${entrada.arquivo}#${entrada.ordem}", expectativa.timestamp, entrada.timestamp)
        }
    }

    @Test
    fun `entradas vazias sao descartadas antes dos contadores e sao exatamente as linhas VAZIO`() {
        val vazias = desfechos.filter { it.entrada.vazia }

        assertEquals(31, vazias.size)
        assertEquals(chavesDe(ClasseEsperada.VAZIO), chavesDe(vazias))
        assertTrue(vazias.all { it.resultado == ResultadoParse.Descartada })
    }

    @Test
    fun `notificacoes de apps nao bancarios ficam fora dos contadores bancarios`() {
        val naoBancarias = desfechos.filter { it.instituicao == null }

        assertEquals(7, naoBancarias.size)
        assertEquals(chavesDe(ClasseEsperada.NAO_BANCARIO), chavesDe(naoBancarias))
        assertTrue(naoBancarias.all { it.classe == ClasseDesfecho.NAO_BANCARIA })
    }

    @Test
    fun `cada entrada bancaria nao vazia e contada como transacao, ignorada ou nao entendida`() {
        val contadas = desfechos.filter { it.instituicao != null && !it.entrada.vazia }

        assertEquals(174, contadas.size)
        assertTrue(contadas.all { it.classe in setOf(ClasseDesfecho.TRANSACAO, ClasseDesfecho.IGNORADA, ClasseDesfecho.NAO_ENTENDIDA) })
    }

    @Test
    fun `compras negadas, ruido e entradas fora da v1 nao geram lancamento`() {
        val proibidas = desfechos.filter {
            expectativaDe(it.entrada).esperado in setOf(ClasseEsperada.NEGADA, ClasseEsperada.FORA_V1, ClasseEsperada.RUIDO)
        }

        assertEquals(94, proibidas.size)
        assertTrue(proibidas.none { it.resultado is ResultadoParse.Transacao })
    }

    @Test
    fun `transacionais nunca viram ignoradas ou descartadas`() {
        val transacionais = desfechos.filter { expectativaDe(it.entrada).esperado == ClasseEsperada.TRANSACIONAL }

        assertEquals(80, transacionais.size)
        assertTrue(transacionais.all { it.resultado is ResultadoParse.Transacao || it.resultado == ResultadoParse.NaoEntendida })
    }

    @Test
    fun `debitos da Inter sem titulo extraem valor, estabelecimento e direcao so do texto`() {
        val debitos = desfechos.filter { it.entrada.arquivo == "pingo-corpus-kaua.txt" && it.entrada.ordem in setOf(58, 59) }
        val dados = debitos.mapNotNull { (it.resultado as? ResultadoParse.Transacao)?.dados }

        assertTrue(debitos.all { it.entrada.titulo.isEmpty() })
        assertEquals(2, dados.size)
        assertEquals(setOf(3750L, 3290L), dados.map { it.valorCentavos }.toSet())
        assertTrue(
            dados.all {
                it.estabelecimento == "DL*99 99" && it.direcao == DirecaoPadrao.DESPESA && it.tipo == TipoLancamentoPadrao.COMPRA
            },
        )
    }

    @Ignore("VAL-PARSER-001 e VAL-PARSER-003 dependem das regras v1 (STR-177)")
    @Test
    fun `transacionais e ruido batem com a tabela de expectativas`() {
        val divergentes = desfechos.filter { divergeDaExpectativa(it) }

        assertEquals(emptyList<String>(), divergentes.map { "${it.entrada.arquivo}#${it.entrada.ordem}" })
    }

    private fun divergeDaExpectativa(desfecho: DesfechoReplay): Boolean {
        val expectativa = expectativaDe(desfecho.entrada)
        val resultado = desfecho.resultado
        return when (expectativa.esperado) {
            ClasseEsperada.TRANSACIONAL -> (resultado as? ResultadoParse.Transacao)?.dados?.tipo?.name != expectativa.tipo
            ClasseEsperada.RUIDO -> resultado != ResultadoParse.Ignorada
            else -> false
        }
    }

    private fun expectativaDe(entrada: EntradaCorpus): ExpectativaCorpus =
        requireNotNull(expectativas[entrada.arquivo to entrada.ordem]) { "sem expectativa para ${entrada.arquivo}#${entrada.ordem}" }

    private fun chavesDe(classe: ClasseEsperada): Set<Pair<String, Int>> =
        expectativas.values.filter { it.esperado == classe }.map { it.arquivo to it.ordem }.toSet()

    private fun chavesDe(desfechos: List<DesfechoReplay>): Set<Pair<String, Int>> =
        desfechos.map { it.entrada.arquivo to it.entrada.ordem }.toSet()
}
