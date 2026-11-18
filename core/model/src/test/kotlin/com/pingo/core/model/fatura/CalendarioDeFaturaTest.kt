package com.pingo.core.model.fatura

import com.pingo.core.model.Fatura
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.TimeZone

class CalendarioDeFaturaTest {
    private val agoraFixo: Instant = Instant.parse("2026-01-01T12:00:00Z")

    private fun instanteSaoPaulo(ano: Int, mes: Int, dia: Int, hora: Int = 12, minuto: Int = 0, segundo: Int = 0): Instant =
        LocalDateTime.of(ano, mes, dia, hora, minuto, segundo)
            .atZone(CalendarioDeFatura.ZONA_SAO_PAULO)
            .toInstant()

    private fun cartaoCredito(publicId: String = "card-1", fechamentoDia: Int? = 10, vencimentoDia: Int? = 17) = Instrumento(
        publicId = publicId,
        contaPublicId = "conta-1",
        tipo = TipoInstrumento.CREDITO,
        apelido = "Cartao Teste",
        fechamentoDia = fechamentoDia,
        vencimentoDia = vencimentoDia,
        criadoEm = agoraFixo,
        atualizadoEm = agoraFixo,
    )

    private fun lancamento(
        publicId: String,
        valorCentavos: Long,
        tipo: TipoLancamento = TipoLancamento.DESPESA,
        status: StatusLancamento = StatusLancamento.CONFIRMADO,
        faturaPublicId: String? = "fat-1",
        excluidoEm: Instant? = null,
        estornoDePublicId: String? = null,
    ) = Lancamento(
        publicId = publicId,
        instrumentoPublicId = "card-1",
        faturaPublicId = faturaPublicId,
        estornoDePublicId = estornoDePublicId,
        valorCentavos = valorCentavos,
        dataHora = agoraFixo,
        tipo = tipo,
        status = status,
        origem = OrigemLancamento.MANUAL,
        confianca = 1.0,
        criadoEm = agoraFixo,
        atualizadoEm = agoraFixo,
        excluidoEm = excluidoEm,
    )

    @Test
    fun `compra um segundo antes da meia noite do fechamento fica no ciclo atual e na meia noite vai para o proximo`() {
        val antes = instanteSaoPaulo(2026, 5, 9, 23, 59, 59)
        val noDia = instanteSaoPaulo(2026, 5, 10, 0, 0, 0)

        val cicloAntes = CalendarioDeFatura.cicloParaInstante(antes, 10, 17) as ResultadoCalculoCiclo.CicloCalculado
        val cicloNoDia = CalendarioDeFatura.cicloParaInstante(noDia, 10, 17) as ResultadoCalculoCiclo.CicloCalculado

        assertEquals(LocalDate.of(2026, 5, 1), cicloAntes.ciclo.competencia)
        assertEquals(LocalDate.of(2026, 5, 10), cicloAntes.ciclo.dataFechamento)
        assertEquals(LocalDate.of(2026, 5, 17), cicloAntes.ciclo.dataVencimento)

        assertEquals(LocalDate.of(2026, 6, 1), cicloNoDia.ciclo.competencia)
        assertEquals(LocalDate.of(2026, 6, 10), cicloNoDia.ciclo.dataFechamento)
        assertEquals(LocalDate.of(2026, 6, 17), cicloNoDia.ciclo.dataVencimento)
    }

    @Test
    fun `calculo usa America Sao Paulo mesmo com fuso padrao do aparelho diferente`() {
        val fusoOriginal = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            val instante = instanteSaoPaulo(2026, 5, 9, 23, 30, 0)
            val resultado = CalendarioDeFatura.cicloParaInstante(instante, 10, 17) as ResultadoCalculoCiclo.CicloCalculado
            assertEquals(LocalDate.of(2026, 5, 10), resultado.ciclo.dataFechamento)
        } finally {
            TimeZone.setDefault(fusoOriginal)
        }
    }

    @Test
    fun `dias 29 30 e 31 ajustam ao ultimo dia de fevereiro comum bissexto e virada de ano`() {
        val antesFevComum = CalendarioDeFatura.cicloParaInstante(instanteSaoPaulo(2026, 2, 27, 23, 59, 59), 31, 5)
            as ResultadoCalculoCiclo.CicloCalculado
        assertEquals(LocalDate.of(2026, 2, 28), antesFevComum.ciclo.dataFechamento)
        assertEquals(LocalDate.of(2026, 3, 5), antesFevComum.ciclo.dataVencimento)

        val noDiaFevComum = CalendarioDeFatura.cicloParaInstante(instanteSaoPaulo(2026, 2, 28, 0, 0, 0), 31, 5)
            as ResultadoCalculoCiclo.CicloCalculado
        assertEquals(LocalDate.of(2026, 3, 31), noDiaFevComum.ciclo.dataFechamento)
        assertEquals(LocalDate.of(2026, 4, 5), noDiaFevComum.ciclo.dataVencimento)

        val antesFevBissexto = CalendarioDeFatura.cicloParaInstante(instanteSaoPaulo(2024, 2, 28, 23, 59, 59), 29, 30)
            as ResultadoCalculoCiclo.CicloCalculado
        assertEquals(LocalDate.of(2024, 2, 29), antesFevBissexto.ciclo.dataFechamento)
        assertEquals(LocalDate.of(2024, 3, 30), antesFevBissexto.ciclo.dataVencimento)

        val viradaDeAno = CalendarioDeFatura.cicloParaInstante(instanteSaoPaulo(2025, 12, 31, 10, 0, 0), 31, 10)
            as ResultadoCalculoCiclo.CicloCalculado
        assertEquals(LocalDate.of(2026, 1, 1), viradaDeAno.ciclo.competencia)
        assertEquals(LocalDate.of(2026, 1, 31), viradaDeAno.ciclo.dataFechamento)
        assertEquals(LocalDate.of(2026, 2, 10), viradaDeAno.ciclo.dataVencimento)
    }

    @Test
    fun `vencimento eh sempre a primeira ocorrencia estritamente posterior ao fechamento`() {
        assertEquals(LocalDate.of(2026, 6, 10), CalendarioDeFatura.calcularVencimento(LocalDate.of(2026, 5, 10), 10))
        assertEquals(LocalDate.of(2026, 6, 5), CalendarioDeFatura.calcularVencimento(LocalDate.of(2026, 5, 20), 5))
        assertEquals(LocalDate.of(2026, 3, 31), CalendarioDeFatura.calcularVencimento(LocalDate.of(2026, 2, 28), 31))
        assertEquals(LocalDate.of(2026, 5, 11), CalendarioDeFatura.calcularVencimento(LocalDate.of(2026, 5, 10), 11))
    }

    @Test
    fun `vigencia preserva ciclos historicos em captura tardia e aplica nova configuracao no futuro`() {
        val cartao = cartaoCredito(fechamentoDia = 20, vencimentoDia = 28)
        val tCriacao = instanteSaoPaulo(2026, 1, 1, 0, 0, 0)
        val vigencias = listOf(
            VigenciaDeCartao("vig-2", "card-1", instanteSaoPaulo(2026, 3, 15, 12, 0, 0), 20, 28),
            VigenciaDeCartao("vig-1", "card-1", tCriacao, 10, 17),
        )
        val faturaHistorica = Fatura(
            publicId = "fat-mar-2026",
            instrumentoPublicId = "card-1",
            competencia = LocalDate.of(2026, 3, 1),
            dataFechamento = LocalDate.of(2026, 3, 10),
            dataVencimento = LocalDate.of(2026, 3, 17),
            totalCentavos = 15_000L,
            status = StatusFatura.FECHADA,
            criadoEm = tCriacao,
            atualizadoEm = tCriacao,
        )

        val resolucaoAtrasada = CalendarioDeFatura.resolverFaturaDaCompra(
            cartao,
            instanteSaoPaulo(2026, 3, 5, 14, 0, 0),
            vigencias,
            listOf(faturaHistorica),
        ) as ResultadoResolucaoFatura.FaturaExistente
        assertEquals("fat-mar-2026", resolucaoAtrasada.fatura.publicId)
        assertEquals("vig-1", resolucaoAtrasada.vigenciaPublicId)
        assertEquals(LocalDate.of(2026, 3, 10), resolucaoAtrasada.fatura.dataFechamento)

        val resolucaoFutura = CalendarioDeFatura.resolverFaturaDaCompra(
            cartao,
            instanteSaoPaulo(2026, 4, 15, 10, 0, 0),
            vigencias,
            listOf(faturaHistorica),
        ) as ResultadoResolucaoFatura.NovoCiclo
        assertEquals("vig-2", resolucaoFutura.vigenciaPublicId)
        assertEquals(LocalDate.of(2026, 4, 20), resolucaoFutura.ciclo.dataFechamento)
        assertEquals(LocalDate.of(2026, 4, 28), resolucaoFutura.ciclo.dataVencimento)
    }

    @Test
    fun `cartao legado sem datas retorna ConfiguracaoIncompleta mesmo se editado depois para periodo anterior`() {
        val cartaoLegado = cartaoCredito(fechamentoDia = null, vencimentoDia = null)
        assertEquals(
            ResultadoCalculoCiclo.ConfiguracaoIncompleta,
            CalendarioDeFatura.cicloParaCompra(cartaoLegado, agoraFixo),
        )

        val tLegado = instanteSaoPaulo(2026, 1, 1, 0, 0, 0)
        val tComplemento = instanteSaoPaulo(2026, 4, 1, 0, 0, 0)
        val cartaoComplementado = cartaoCredito(fechamentoDia = 10, vencimentoDia = 17)
        val vigencias = listOf(
            VigenciaDeCartao("v-legado", "card-1", tLegado, null, null),
            VigenciaDeCartao("v-novo", "card-1", tComplemento, 10, 17),
        )

        assertEquals(
            ResultadoCalculoCiclo.ConfiguracaoIncompleta,
            CalendarioDeFatura.cicloParaCompra(cartaoComplementado, instanteSaoPaulo(2026, 2, 10), vigencias),
        )
        assertTrue(
            CalendarioDeFatura.cicloParaCompra(cartaoComplementado, instanteSaoPaulo(2026, 4, 5), vigencias)
                is ResultadoCalculoCiclo.CicloCalculado,
        )
    }

    @Test
    fun `pagamento em transferencia pendente ajuste receita e excluido nao entram no total da fatura`() {
        val itens = listOf(
            lancamento("l-compra-1", 10_000L, TipoLancamento.DESPESA),
            lancamento("l-compra-2", 5_500L, TipoLancamento.DESPESA),
            lancamento("l-pagamento", 10_000L, TipoLancamento.TRANSFERENCIA),
            lancamento("l-pendente", 9_900L, TipoLancamento.DESPESA, status = StatusLancamento.PENDENTE),
            lancamento("l-receita", 2_000L, TipoLancamento.RECEITA),
            lancamento("l-ajuste", 1_000L, TipoLancamento.AJUSTE),
            lancamento("l-excluido", 3_000L, TipoLancamento.DESPESA, excluidoEm = agoraFixo),
            lancamento("l-outra-fat", 7_000L, TipoLancamento.DESPESA, faturaPublicId = "fat-2"),
        )

        assertFalse(CalendarioDeFatura.entraNaFatura(itens[2]))
        assertTrue(CalendarioDeFatura.ehPagamentoOuTransferencia(itens[2]))
        assertEquals(15_500L, CalendarioDeFatura.calcularTotalFaturaCentavos(itens, "fat-1"))
    }

    @Test
    fun `status do ciclo transita corretamente entre aberta fechada e paga`() {
        val fechamento = LocalDate.of(2026, 5, 10)
        assertEquals(StatusFatura.ABERTA, CalendarioDeFatura.statusDoCiclo(fechamento, LocalDate.of(2026, 5, 9)))
        assertEquals(StatusFatura.FECHADA, CalendarioDeFatura.statusDoCiclo(fechamento, LocalDate.of(2026, 5, 10)))
        assertEquals(StatusFatura.PAGA, CalendarioDeFatura.statusDoCiclo(fechamento, LocalDate.of(2026, 5, 10), paga = true))
        assertEquals(LocalDate.of(2026, 2, 28), CalendarioDeFatura.diaAjustadoNoMes(YearMonth.of(2026, 2), 31))
    }

    @Test
    fun `estorno abate total da fatura com valor positivo ou negativo e limita total minimo em zero`() {
        val compra = lancamento("l-1", 34_990L)
        val compra2 = lancamento("l-2", 4_290L)
        val estornoPositivo = lancamento("l-3", 15_000L, estornoDePublicId = "l-1")
        val estornoNegativo = lancamento("l-4", -15_000L, estornoDePublicId = "l-1")
        val estornoMaiorQueCompras = lancamento("l-5", 50_000L, estornoDePublicId = "l-1")

        assertEquals(
            24_280L,
            CalendarioDeFatura.calcularTotalFaturaCentavos(listOf(compra, compra2, estornoPositivo), "fat-1"),
        )
        assertEquals(
            24_280L,
            CalendarioDeFatura.calcularTotalFaturaCentavos(listOf(compra, compra2, estornoNegativo), "fat-1"),
        )
        assertEquals(
            0L,
            CalendarioDeFatura.calcularTotalFaturaCentavos(listOf(compra, estornoMaiorQueCompras), "fat-1"),
        )
    }
}
