package com.pingo.core.model.assinatura

import com.pingo.core.model.Assinatura
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class DetectorDeAssinaturasTest {
    private fun instanteSaoPaulo(ano: Int, mes: Int, dia: Int, hora: Int = 12): Instant = LocalDateTime.of(ano, mes, dia, hora, 0)
        .atZone(DetectorDeAssinaturas.ZONA_SAO_PAULO)
        .toInstant()

    private fun despesa(
        publicId: String,
        instrumentoPublicId: String? = "card-1",
        estabelecimento: String? = "Netflix",
        valorCentavos: Long = 5_590L,
        dataHora: Instant,
        tipo: TipoLancamento = TipoLancamento.DESPESA,
        status: StatusLancamento = StatusLancamento.CONFIRMADO,
    ) = Lancamento(
        publicId = publicId,
        instrumentoPublicId = instrumentoPublicId,
        estabelecimento = estabelecimento,
        valorCentavos = valorCentavos,
        dataHora = dataHora,
        tipo = tipo,
        status = status,
        origem = OrigemLancamento.NOTIFICACAO,
        confianca = 1.0,
        criadoEm = dataHora,
        atualizadoEm = dataHora,
    )

    @Test
    fun `normalizador de assinatura padroniza caixa acentos e espacos repetidos`() {
        assertEquals("cafe central", NormalizadorDeAssinatura.normalizar("CAFÉ   Central"))
        assertEquals("cafe central", NormalizadorDeAssinatura.normalizar("  cafe\u00A0\u00A0central  "))
        assertEquals("acai da praca", NormalizadorDeAssinatura.normalizar("Açaí   da Praça"))
        assertNull(NormalizadorDeAssinatura.normalizar("   "))
        assertNull(NormalizadorDeAssinatura.normalizar(null))
    }

    @Test
    fun `mesmo estabelecimento em instrumentos distintos gera sugestoes separadas sem fusao`() {
        val d1 = instanteSaoPaulo(2026, 3, 1)
        val d2 = instanteSaoPaulo(2026, 3, 31)
        val itens = listOf(
            despesa("c1-1", "card-1", "CAFÉ   Central", 3_990L, d1),
            despesa("c1-2", "card-1", "cafe central", 3_990L, d2),
            despesa("c2-1", "card-2", "Cafe Central", 2_000L, d1),
            despesa("c2-2", "card-2", "CAFÉ Central", 2_000L, d2),
        )

        val sugestoes = DetectorDeAssinaturas.detectarSugestoes(itens)
        assertEquals(2, sugestoes.size)
        assertEquals(setOf("card-1", "card-2"), sugestoes.map { it.chave.instrumentoPublicId }.toSet())
    }

    @Test
    fun `intervalo inclusivo de 28 a 35 dias e rejeicao de 27 e 36 dias`() {
        val base = LocalDate.of(2026, 3, 1)
        val instBase = instanteSaoPaulo(2026, 3, 1)

        assertTrue(
            DetectorDeAssinaturas.detectarSugestoes(
                listOf(
                    despesa("1", dataHora = instBase),
                    despesa("2", dataHora = instanteSaoPaulo(2026, 3, 28)),
                ),
            ).isEmpty(),
        )

        assertEquals(
            1,
            DetectorDeAssinaturas.detectarSugestoes(
                listOf(
                    despesa("1", dataHora = instBase),
                    despesa("2", dataHora = base.plusDays(28).atTime(12, 0).atZone(DetectorDeAssinaturas.ZONA_SAO_PAULO).toInstant()),
                ),
            ).size,
        )

        assertEquals(
            1,
            DetectorDeAssinaturas.detectarSugestoes(
                listOf(
                    despesa("1", dataHora = instBase),
                    despesa("2", dataHora = base.plusDays(35).atTime(12, 0).atZone(DetectorDeAssinaturas.ZONA_SAO_PAULO).toInstant()),
                ),
            ).size,
        )

        assertTrue(
            DetectorDeAssinaturas.detectarSugestoes(
                listOf(
                    despesa("1", dataHora = instBase),
                    despesa("2", dataHora = base.plusDays(36).atTime(12, 0).atZone(DetectorDeAssinaturas.ZONA_SAO_PAULO).toInstant()),
                ),
            ).isEmpty(),
        )
    }

    @Test
    fun `faixa de valor de 90 a 110 por cento do anterior eh direcional e exata em centavos`() {
        assertFalse(DetectorDeAssinaturas.valorNaFaixa(10_000L, 8_999L))
        assertTrue(DetectorDeAssinaturas.valorNaFaixa(10_000L, 9_000L))
        assertTrue(DetectorDeAssinaturas.valorNaFaixa(10_000L, 11_000L))
        assertFalse(DetectorDeAssinaturas.valorNaFaixa(10_000L, 11_001L))

        assertTrue(DetectorDeAssinaturas.valorNaFaixa(11_000L, 9_900L))
        assertFalse(DetectorDeAssinaturas.valorNaFaixa(9_900L, 11_000L))
    }

    @Test
    fun `ignora ocorrencia unica pendente receita transferencia ajuste excluido e sem instrumento`() {
        val d1 = instanteSaoPaulo(2026, 3, 1)
        val d2 = instanteSaoPaulo(2026, 3, 31)
        val itens = listOf(
            despesa("unica", "card-1", "Unica", 1_000L, d1),
            despesa("pend-1", "card-1", "Pend", 1_000L, d1),
            despesa("pend-2", "card-1", "Pend", 1_000L, d2, status = StatusLancamento.PENDENTE),
            despesa("rec-1", "card-1", "Rec", 1_000L, d1, tipo = TipoLancamento.RECEITA),
            despesa("rec-2", "card-1", "Rec", 1_000L, d2, tipo = TipoLancamento.RECEITA),
            despesa("tr-1", "card-1", "Transf", 1_000L, d1, tipo = TipoLancamento.TRANSFERENCIA),
            despesa("tr-2", "card-1", "Transf", 1_000L, d2, tipo = TipoLancamento.TRANSFERENCIA),
            despesa("aj-1", "card-1", "Ajuste", 1_000L, d1, tipo = TipoLancamento.AJUSTE),
            despesa("aj-2", "card-1", "Ajuste", 1_000L, d2, tipo = TipoLancamento.AJUSTE),
            despesa("ex-1", "card-1", "Excluida", 1_000L, d1),
            despesa("ex-2", "card-1", "Excluida", 1_000L, d2).copy(excluidoEm = d2),
            despesa("sem-1", null, "SemInst", 1_000L, d1),
            despesa("sem-2", null, "SemInst", 1_000L, d2),
        )

        assertTrue(DetectorDeAssinaturas.detectarSugestoes(itens).isEmpty())
    }

    @Test
    fun `proxima cobranca usa calendario mensal com ajuste de fim de mes e virada de ano`() {
        assertEquals(LocalDate.of(2025, 2, 28), DetectorDeAssinaturas.calcularProximaCobranca(LocalDate.of(2025, 1, 31)))
        assertEquals(LocalDate.of(2024, 2, 29), DetectorDeAssinaturas.calcularProximaCobranca(LocalDate.of(2024, 1, 31)))
        assertEquals(LocalDate.of(2026, 4, 30), DetectorDeAssinaturas.calcularProximaCobranca(LocalDate.of(2026, 3, 31)))
        assertEquals(LocalDate.of(2026, 1, 15), DetectorDeAssinaturas.calcularProximaCobranca(LocalDate.of(2025, 12, 15)))
    }

    @Test
    fun `chaves ignoradas total mensal e reconhecimento de cobranca confirmada preservam legado`() {
        val d1 = instanteSaoPaulo(2026, 3, 1)
        val d2 = instanteSaoPaulo(2026, 3, 31)
        val chaveRecusada = ChaveAssinatura("card-1", "spotify")
        val itens = listOf(
            despesa("s1", "card-1", "Spotify", 2_190L, d1),
            despesa("s2", "card-1", "Spotify", 2_190L, d2),
        )
        assertTrue(DetectorDeAssinaturas.detectarSugestoes(itens, setOf(chaveRecusada)).isEmpty())

        val agora = Instant.parse("2026-04-01T12:00:00Z")
        val confirmada1 = Assinatura(
            publicId = "a1",
            estabelecimento = "Netflix",
            estabelecimentoNormalizado = "netflix",
            valorReferenciaCentavos = 3_990L,
            ultimaCobranca = LocalDate.of(2026, 3, 31),
            proximaCobrancaEstimada = LocalDate.of(2026, 4, 30),
            status = StatusAssinatura.CONFIRMADA,
            criadoEm = agora,
            atualizadoEm = agora,
        )
        val confirmada2 = confirmada1.copy(publicId = "a2", valorReferenciaCentavos = 2_000L)
        val sugerida = confirmada1.copy(publicId = "a3", status = StatusAssinatura.SUGERIDA, valorReferenciaCentavos = 9_900L)
        val recusada = confirmada1.copy(publicId = "a4", status = StatusAssinatura.RECUSADA, valorReferenciaCentavos = 9_900L)

        assertEquals(
            5_990L,
            DetectorDeAssinaturas.totalMensalConfirmadasCentavos(listOf(confirmada1, confirmada2, sugerida, recusada)),
        )

        val novaCobranca = despesa("s3", "card-1", "NETFLIX", 3_990L, instanteSaoPaulo(2026, 4, 30))
        assertTrue(DetectorDeAssinaturas.correspondeACobrancaConfirmada(novaCobranca, confirmada1, "card-1"))
        assertFalse(DetectorDeAssinaturas.correspondeACobrancaConfirmada(novaCobranca, confirmada1, "card-2"))
        assertFalse(DetectorDeAssinaturas.correspondeACobrancaConfirmada(novaCobranca, confirmada1, null))
    }

    @Test
    fun `estorno eh ignorado na elegibilidade deteccao de sugestoes e correspondencia confirmada`() {
        val d1 = instanteSaoPaulo(2026, 3, 1)
        val d2 = instanteSaoPaulo(2026, 3, 31)
        val d3 = instanteSaoPaulo(2026, 4, 30)
        val estorno1 = despesa("e-1", "card-1", "Netflix", 3_990L, d1).copy(estornoDePublicId = "orig-1")
        val estorno2 = despesa("e-2", "card-1", "Netflix", 3_990L, d2).copy(estornoDePublicId = "orig-2")
        val cobranca = despesa("c-1", "card-1", "Netflix", 3_990L, d1)

        assertFalse(DetectorDeAssinaturas.elegivelParaDeteccao(estorno1))
        assertTrue(DetectorDeAssinaturas.detectarSugestoes(listOf(cobranca, estorno2)).isEmpty())
        assertTrue(DetectorDeAssinaturas.detectarSugestoes(listOf(estorno1, estorno2)).isEmpty())

        val confirmada = Assinatura(
            publicId = "a1",
            estabelecimento = "Netflix",
            estabelecimentoNormalizado = "netflix",
            valorReferenciaCentavos = 3_990L,
            ultimaCobranca = LocalDate.of(2026, 3, 31),
            proximaCobrancaEstimada = LocalDate.of(2026, 4, 30),
            status = StatusAssinatura.CONFIRMADA,
            criadoEm = d1,
            atualizadoEm = d1,
        )
        val estorno3 = despesa("e-3", "card-1", "Netflix", 3_990L, d3).copy(estornoDePublicId = "orig-3")
        assertFalse(DetectorDeAssinaturas.correspondeACobrancaConfirmada(estorno3, confirmada, "card-1"))
    }

    @Test
    fun `compra parcelada nao eh sugerida como assinatura`() {
        val d1 = instanteSaoPaulo(2026, 3, 15)
        val d2 = instanteSaoPaulo(2026, 4, 15)
        val d3 = instanteSaoPaulo(2026, 5, 15)
        val parcela1 = despesa("p-1", "card-1", "Casas Bahia", 30_000L, d1).copy(
            parcelaNumero = 1,
            totalParcelas = 6,
            valorTotalCompraCentavos = 180_000L,
        )
        val parcela2 = despesa("p-2", "card-1", "Casas Bahia", 30_000L, d2).copy(
            compraPaiPublicId = "p-1",
            parcelaNumero = 2,
            totalParcelas = 6,
        )
        val parcela3 = despesa("p-3", "card-1", "Casas Bahia", 30_000L, d3).copy(
            compraPaiPublicId = "p-1",
            parcelaNumero = 3,
            totalParcelas = 6,
        )

        assertFalse(DetectorDeAssinaturas.elegivelParaDeteccao(parcela1))
        assertFalse(DetectorDeAssinaturas.elegivelParaDeteccao(parcela2))
        assertTrue(DetectorDeAssinaturas.detectarSugestoes(listOf(parcela1, parcela2, parcela3)).isEmpty())
    }

    @Test
    fun `lancamento unico na categoria assinaturas gera sugestao por categoria e ignora parcelado`() {
        val d1 = instanteSaoPaulo(2026, 11, 5)
        val netflix = despesa("sub-1", "card-1", "Netflix", 4_490L, d1).copy(categoriaPublicId = "cat-assinaturas")
        val parcelado = despesa("parc-1", "card-1", "Curso", 10_000L, d1).copy(
            categoriaPublicId = "cat-assinaturas",
            parcelaNumero = 1,
            totalParcelas = 4,
        )

        val sugestoes = DetectorDeAssinaturas.detectarSugestoes(
            lancamentos = listOf(netflix, parcelado),
            categoriasAssinaturaIds = setOf("cat-assinaturas"),
        )

        assertEquals(1, sugestoes.size)
        val sugestao = sugestoes.single()
        assertEquals(ChaveAssinatura("card-1", "netflix"), sugestao.chave)
        assertEquals(4_490L, sugestao.valorReferenciaCentavos)
        assertTrue(sugestao.porCategoria)
        assertEquals(listOf("sub-1"), sugestao.lancamentosQualificadoresPublicIds)
    }
}
