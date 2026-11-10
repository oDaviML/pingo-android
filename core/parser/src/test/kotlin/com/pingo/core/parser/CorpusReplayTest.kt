package com.pingo.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CorpusReplayTest {
    private val entradas = CorpusFixtures.entradas()
    private val expectativas = CorpusFixtures.expectativas().associateBy { it.arquivo to it.ordem }
    private val desfechos: List<DesfechoReplay> by lazy {
        ReplayCorpus(DefaultParserEngine(), CorpusFixtures.regrasV1()).executar(entradas)
    }
    private val transacoes: List<DesfechoReplay> by lazy { desfechos.filter { it.resultado is ResultadoParse.Transacao } }

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

    @Test
    fun `data e hora embutidas saem so nas compras aprovadas do Itau e no Pix Automatico da Inter`() {
        val divergentes = transacoes.filter { it.transacao().dataHoraTexto != dataHoraDoTextoOriginal(it.entrada) }
        val comData = transacoes.map { it.transacao() }.filter { it.dataHoraTexto != null }

        assertEquals(emptyList<String>(), divergentes.map { "${it.entrada.arquivo}#${it.entrada.ordem}" })
        assertTrue(comData.isNotEmpty())
        assertTrue(comData.all { it.padraoId in padroesComDataEmbutida })
    }

    @Test
    fun `transacionais e ruido batem com a tabela de expectativas`() {
        val divergentes = desfechos.filter { divergeDaExpectativa(it) }

        assertEquals(emptyList<String>(), divergentes.map { "${it.entrada.arquivo}#${it.entrada.ordem}" })
    }

    @Test
    fun `negadas e ruido viram IGNORADA e fora da v1 vira NAO_ENTENDIDA`() {
        val porClasse = desfechos.groupBy { expectativaDe(it.entrada).esperado }

        assertTrue(porClasse.getValue(ClasseEsperada.NEGADA).all { it.resultado == ResultadoParse.Ignorada })
        assertTrue(porClasse.getValue(ClasseEsperada.RUIDO).all { it.resultado == ResultadoParse.Ignorada })
        assertTrue(porClasse.getValue(ClasseEsperada.FORA_V1).all { it.resultado == ResultadoParse.NaoEntendida })
    }

    @Test
    fun `transacoes por instituicao e tipo batem com a tabela de expectativas`() {
        val esperadas = expectativas.values
            .filter { it.esperado == ClasseEsperada.TRANSACIONAL }
            .groupingBy { it.instituicao to it.tipo }
            .eachCount()
        val obtidas = transacoes.groupingBy { it.instituicao?.name to it.transacao().tipo.name }.eachCount()

        assertEquals(esperadas, obtidas)
    }

    @Test
    fun `ruido e negadas por instituicao batem com a tabela e viram IGNORADA`() {
        val esperadas = expectativas.values
            .filter { it.esperado == ClasseEsperada.RUIDO || it.esperado == ClasseEsperada.NEGADA }
            .groupingBy { it.instituicao }
            .eachCount()
        val ignoradas = desfechos.filter { it.resultado == ResultadoParse.Ignorada }.groupingBy { it.instituicao?.name }.eachCount()

        assertEquals(esperadas, ignoradas)
    }

    @Test
    fun `valor de cada transacional e o primeiro valor monetario do texto original`() {
        val divergentes = transacoes.filter { it.transacao().valorCentavos != primeiroValorDoTexto(it.entrada) }

        assertEquals(emptyList<String>(), divergentes.map { "${it.entrada.arquivo}#${it.entrada.ordem}" })
    }

    @Test
    fun `transacoes sem estabelecimento sao so depositos e transferencias sem remetente`() {
        val semEstabelecimento = transacoes.map { it.transacao() }.filter { it.estabelecimento == null }

        assertEquals(
            mapOf(TipoLancamentoPadrao.DEPOSITO to 2, TipoLancamentoPadrao.TRANSFERENCIA_RECEBIDA to 6),
            semEstabelecimento.groupingBy { it.tipo }.eachCount(),
        )
    }

    private fun DesfechoReplay.transacao(): TransacaoExtraida = (resultado as ResultadoParse.Transacao).dados

    private fun dataHoraDoTextoOriginal(entrada: EntradaCorpus): String? {
        val texto = (entrada.titulo + "\n" + entrada.texto).replace('\u00A0', ' ')
        return dataHoraNoTexto.find(texto)?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } }
    }

    private fun primeiroValorDoTexto(entrada: EntradaCorpus): Long? {
        val texto = (entrada.titulo + "\n" + entrada.texto).replace('\u00A0', ' ')
        return valorNoTexto.find(texto)?.groupValues?.get(1)?.let(::centavosDe)
    }

    private fun centavosDe(numero: String): Long {
        val decimal = if (',' in numero) numero.replace(".", "").replace(',', '.') else numero
        return BigDecimal(decimal).movePointRight(2).longValueExact()
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

private val valorNoTexto = Regex("""(?:R\$|USD)\s*(\d[\d.,]*\d|\d)""")
private val dataHoraNoTexto = Regex("""no dia (\d{2}/\d{2} às \d{2}:\d{2})|feito à .+? em (\d{2}/\d{2}/\d{4})""")
private val padroesComDataEmbutida = setOf("itau-compra-aprovada", "itau-compra-aprovada-usd", "inter-pix-automatico")
