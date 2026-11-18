package com.pingo.core.model.fatura

import com.pingo.core.model.Instrumento
import com.pingo.core.model.TipoInstrumento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime

class GeradorDeParcelasTest {
    private val agora = Instant.parse("2026-11-18T12:00:00Z")

    private fun cartao(fechamento: Int? = 10, vencimento: Int? = 17) = Instrumento(
        publicId = "card-1",
        contaPublicId = "conta-1",
        tipo = TipoInstrumento.CREDITO,
        apelido = "Cartão",
        fechamentoDia = fechamento,
        vencimentoDia = vencimento,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    @Test
    fun `valor total com resto distribui centavos extras nas primeiras parcelas e soma exato`() {
        val parcelas = GeradorDeParcelas.planejar(
            valorBaseCentavos = 10_000L,
            totalParcelas = 3,
            modo = ModoParcelamento.VALOR_TOTAL,
            dataHoraInicial = ZonedDateTime.of(2026, 5, 15, 14, 0, 0, 0, CalendarioDeFatura.ZONA_SAO_PAULO).toInstant(),
            instrumento = cartao(10, 17),
        )

        assertEquals(listOf(3334L, 3333L, 3333L), parcelas.map { it.valorCentavos })
        assertEquals(10_000L, parcelas.sumOf { it.valorCentavos })
        assertEquals(10_000L, parcelas[0].valorTotalCompraCentavos)
        assertNull(parcelas[1].valorTotalCompraCentavos)
        assertNull(parcelas[2].valorTotalCompraCentavos)
    }

    @Test
    fun `valor por parcela calcula total na primeira parcela e mantem valor igual em todas`() {
        val parcelas = GeradorDeParcelas.planejar(
            valorBaseCentavos = 29_990L,
            totalParcelas = 6,
            modo = ModoParcelamento.VALOR_PARCELA,
            dataHoraInicial = ZonedDateTime.of(2026, 5, 15, 14, 0, 0, 0, CalendarioDeFatura.ZONA_SAO_PAULO).toInstant(),
            instrumento = cartao(10, 17),
        )

        assertEquals(6, parcelas.size)
        assertEquals(List(6) { 29_990L }, parcelas.map { it.valorCentavos })
        assertEquals(179_940L, parcelas[0].valorTotalCompraCentavos)
        assertEquals((1..6).toList(), parcelas.map { it.numero })
        assertEquals(List(6) { 6 }, parcelas.map { it.totalParcelas })
    }

    @Test
    fun `compra retroativa de 6 meses em 10x cai em 10 ciclos mensais consecutivos`() {
        val inst = cartao(fechamento = 10, vencimento = 17)
        val dataCompra = ZonedDateTime.of(2026, 5, 15, 14, 30, 0, 0, CalendarioDeFatura.ZONA_SAO_PAULO).toInstant()

        val parcelas = GeradorDeParcelas.planejar(
            valorBaseCentavos = 300_000L,
            totalParcelas = 10,
            modo = ModoParcelamento.VALOR_TOTAL,
            dataHoraInicial = dataCompra,
            instrumento = inst,
        )

        val competencias = parcelas.map { parcela ->
            val ciclo = CalendarioDeFatura.cicloParaCompra(inst, parcela.dataHora) as ResultadoCalculoCiclo.CicloCalculado
            YearMonth.from(ciclo.ciclo.competencia)
        }
        val esperadas = (0 until 10).map { YearMonth.of(2026, 6).plusMonths(it.toLong()) }
        assertEquals(esperadas, competencias)
    }

    @Test
    fun `compra com fechamento no fim do mes nao colide nem pula fevereiro`() {
        val inst = cartao(fechamento = 30, vencimento = 5)
        val compras = listOf(
            ZonedDateTime.of(2026, 1, 29, 10, 0, 0, 0, CalendarioDeFatura.ZONA_SAO_PAULO).toInstant(),
            ZonedDateTime.of(2026, 2, 28, 10, 0, 0, 0, CalendarioDeFatura.ZONA_SAO_PAULO).toInstant(),
        )

        compras.forEach { dataCompra ->
            val parcelas = GeradorDeParcelas.planejar(
                valorBaseCentavos = 12_000L,
                totalParcelas = 4,
                modo = ModoParcelamento.VALOR_TOTAL,
                dataHoraInicial = dataCompra,
                instrumento = inst,
            )
            val competencias = parcelas.map { parcela ->
                val ciclo = CalendarioDeFatura.cicloParaCompra(inst, parcela.dataHora) as ResultadoCalculoCiclo.CicloCalculado
                YearMonth.from(ciclo.ciclo.competencia)
            }
            val inicial = competencias.first()
            assertEquals((0L..3L).map { inicial.plusMonths(it) }, competencias)
        }
    }

    @Test
    fun `status dos ciclos de compra 6 meses atras marca anteriores como fechadas e atual como aberta`() {
        val inst = cartao(fechamento = 10, vencimento = 17)
        val hoje = LocalDate.of(2026, 11, 18)
        val compra = ZonedDateTime.of(2026, 5, 15, 10, 0, 0, 0, CalendarioDeFatura.ZONA_SAO_PAULO).toInstant()

        val parcelas = GeradorDeParcelas.planejar(
            valorBaseCentavos = 100_000L,
            totalParcelas = 10,
            modo = ModoParcelamento.VALOR_TOTAL,
            dataHoraInicial = compra,
            instrumento = inst,
        )
        val fechamentos = parcelas.map { parcela ->
            (CalendarioDeFatura.cicloParaCompra(inst, parcela.dataHora) as ResultadoCalculoCiclo.CicloCalculado).ciclo.dataFechamento
        }
        val fechadas = fechamentos.count { !hoje.isBefore(it) }
        val abertasOuFuturas = fechamentos.count { hoje.isBefore(it) }

        assertEquals(6, fechadas)
        assertEquals(4, abertasOuFuturas)
    }
}
