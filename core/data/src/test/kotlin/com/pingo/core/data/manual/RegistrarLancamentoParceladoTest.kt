package com.pingo.core.data.manual

import com.pingo.core.data.ConfiguracaoCartaoRepository
import com.pingo.core.model.Instrumento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.fatura.CalendarioDeFatura
import com.pingo.core.model.fatura.ModoParcelamento
import com.pingo.core.model.fatura.ResultadoCalculoCiclo
import com.pingo.core.model.fatura.VigenciaDeCartao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.YearMonth

class RegistrarLancamentoParceladoTest {
    @Test
    fun `compra parcelada em valor total divide centavos com soma exata e vincula parcelas`() = runBlocking {
        val ambiente = AmbienteManual()
        val cartao = ambiente.instrumentos.criar("Nubank")

        val resultado = ambiente.registrar.registrar(
            NovoLancamentoManual(
                tipo = TipoLancamento.DESPESA,
                valorCentavos = 10000L,
                instrumentoPublicId = cartao.publicId,
                dataHora = AGORA_MANUAL,
                estabelecimento = "Loja TV",
                totalParcelas = 3,
                modoParcelamento = ModoParcelamento.VALOR_TOTAL,
            ),
        )

        val parcelas = (resultado as ResultadoLancamentoManual.Salvo).lancamentos
        assertEquals(3, parcelas.size)
        assertEquals(listOf(3334L, 3333L, 3333L), parcelas.map { it.valorCentavos })
        assertEquals(listOf(1, 2, 3), parcelas.map { it.parcelaNumero })
        assertEquals(listOf(3, 3, 3), parcelas.map { it.totalParcelas })
        val paiId = parcelas.first().publicId
        assertNull(parcelas.first().compraPaiPublicId)
        assertEquals(10000L, parcelas.first().valorTotalCompraCentavos)
        parcelas.drop(1).forEach { parcela ->
            assertEquals(paiId, parcela.compraPaiPublicId)
            assertNull(parcela.valorTotalCompraCentavos)
        }
        assertEquals(3, ambiente.lancamentos.todos().size)
        assertEquals(1, ambiente.transacao.execucoes)
    }

    @Test
    fun `compra parcelada por valor da parcela replica valor e grava total na primeira`() = runBlocking {
        val ambiente = AmbienteManual()
        val cartao = ambiente.instrumentos.criar("Inter")

        val resultado = ambiente.registrar.registrar(
            NovoLancamentoManual(
                tipo = TipoLancamento.DESPESA,
                valorCentavos = 9990L,
                instrumentoPublicId = cartao.publicId,
                dataHora = AGORA_MANUAL,
                estabelecimento = "Celular",
                totalParcelas = 6,
                modoParcelamento = ModoParcelamento.VALOR_PARCELA,
            ),
        )

        val parcelas = (resultado as ResultadoLancamentoManual.Salvo).lancamentos
        assertEquals(6, parcelas.size)
        assertEquals(List(6) { 9990L }, parcelas.map { it.valorCentavos })
        assertEquals(59940L, parcelas.first().valorTotalCompraCentavos)
        parcelas.drop(1).forEach { assertNull(it.valorTotalCompraCentavos) }
    }

    @Test
    fun `compra parcelada retroativa de 6 meses usa vigencias do cartao para posicionar ciclos`() = runBlocking {
        val lancamentos = FakeLancamentosManual()
        val instrumentos = FakeInstrumentosManual()
        val cartao = cartaoCredito(fechamentoDia = 10, vencimentoDia = 17)
        instrumentos.salvar(cartao)
        val registrar = RegistrarLancamentoManual(
            lancamentoRepository = lancamentos,
            instrumentoRepository = instrumentos,
            configuracaoCartaoRepository = VigenciasFixas(emptyList()),
            transacaoLocal = FakeTransacaoLocal(),
            clock = Clock.fixed(AGORA_MANUAL, FUSO_MANUAL),
        )

        val compra = NovoLancamentoManual(
            tipo = TipoLancamento.DESPESA,
            valorCentavos = 100000L,
            instrumentoPublicId = cartao.publicId,
            dataHora = Instant.parse("2026-05-05T14:00:00Z"),
            estabelecimento = "TV Sala",
            totalParcelas = 10,
        )
        val parcelas = (registrar.registrar(compra) as ResultadoLancamentoManual.Salvo).lancamentos

        assertEquals(10, parcelas.size)
        val ciclos = parcelas.map {
            val calculado = CalendarioDeFatura.cicloParaCompra(cartao, it.dataHora) as ResultadoCalculoCiclo.CicloCalculado
            YearMonth.from(calculado.ciclo.competencia).toString()
        }
        val esperados = listOf(
            "2026-05", "2026-06", "2026-07", "2026-08", "2026-09",
            "2026-10", "2026-11", "2026-12", "2027-01", "2027-02",
        )
        assertEquals(esperados, ciclos)
    }

    private fun cartaoCredito(fechamentoDia: Int, vencimentoDia: Int): Instrumento = Instrumento(
        publicId = "cartao-1",
        contaPublicId = "conta-1",
        tipo = TipoInstrumento.CREDITO,
        apelido = "Cartão",
        fechamentoDia = fechamentoDia,
        vencimentoDia = vencimentoDia,
        criadoEm = AGORA_MANUAL,
        atualizadoEm = AGORA_MANUAL,
    )

    @Test
    fun `parcelamento fora de credito ou receita eh recusado`() = runBlocking {
        val ambiente = AmbienteManual()
        val debito = Instrumento(
            publicId = "debito-1",
            contaPublicId = "conta-1",
            tipo = TipoInstrumento.DEBITO,
            apelido = "Débito",
            criadoEm = AGORA_MANUAL,
            atualizadoEm = AGORA_MANUAL,
        )
        val credito = ambiente.instrumentos.criar("Crédito")
        ambiente.instrumentos.salvar(debito)

        val noDebito = ambiente.registrar.registrar(
            NovoLancamentoManual(
                tipo = TipoLancamento.DESPESA,
                valorCentavos = 10000L,
                instrumentoPublicId = debito.publicId,
                dataHora = AGORA_MANUAL,
                totalParcelas = 3,
            ),
        )
        val naReceita = ambiente.registrar.registrar(
            NovoLancamentoManual(
                tipo = TipoLancamento.RECEITA,
                valorCentavos = 10000L,
                instrumentoPublicId = credito.publicId,
                dataHora = AGORA_MANUAL,
                totalParcelas = 2,
            ),
        )

        assertEquals(ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.PARCELAS)), noDebito)
        assertEquals(ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.PARCELAS)), naReceita)
    }

    @Test
    fun `parcelamento com quantidade invalida ou valor menor que numero de parcelas eh recusado`() = runBlocking {
        val ambiente = AmbienteManual()
        val cartao = ambiente.instrumentos.criar("Crédito")

        val cemParcelas = ambiente.registrar.registrar(
            NovoLancamentoManual(
                tipo = TipoLancamento.DESPESA,
                valorCentavos = 100000L,
                instrumentoPublicId = cartao.publicId,
                dataHora = AGORA_MANUAL,
                totalParcelas = 100,
            ),
        )
        val centavosInsuficientes = ambiente.registrar.registrar(
            NovoLancamentoManual(
                tipo = TipoLancamento.DESPESA,
                valorCentavos = 5L,
                instrumentoPublicId = cartao.publicId,
                dataHora = AGORA_MANUAL,
                totalParcelas = 10,
                modoParcelamento = ModoParcelamento.VALOR_TOTAL,
            ),
        )

        assertEquals(ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.PARCELAS)), cemParcelas)
        assertEquals(ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.VALOR)), centavosInsuficientes)
    }

    @Test
    fun `duplicata capturada marca apenas a primeira parcela como pendente`() = runBlocking {
        val ambiente = AmbienteManual()
        val cartao = ambiente.instrumentos.criar("Crédito")
        val capturado = com.pingo.core.model.Lancamento(
            publicId = "cap-1",
            instrumentoPublicId = cartao.publicId,
            valorCentavos = 5000L,
            dataHora = AGORA_MANUAL,
            tipo = TipoLancamento.DESPESA,
            estabelecimento = "Loja",
            status = StatusLancamento.CONFIRMADO,
            origem = com.pingo.core.model.OrigemLancamento.NOTIFICACAO,
            confianca = 1.0,
            criadoEm = AGORA_MANUAL,
            atualizadoEm = AGORA_MANUAL,
        )
        ambiente.lancamentos.salvar(capturado)

        val resultado = ambiente.registrar.registrar(
            NovoLancamentoManual(
                tipo = TipoLancamento.DESPESA,
                valorCentavos = 15000L,
                instrumentoPublicId = cartao.publicId,
                dataHora = AGORA_MANUAL,
                totalParcelas = 3,
                modoParcelamento = ModoParcelamento.VALOR_TOTAL,
            ),
        )

        val parcelas = (resultado as ResultadoLancamentoManual.Salvo).lancamentos
        assertEquals(StatusLancamento.PENDENTE, parcelas.first().status)
        assertNotNull(parcelas.first().motivoPendencia)
        assertEquals(MotivoPendencia.POSSIVEL_DUPLICATA.name, parcelas.first().motivoPendencia)
        parcelas.drop(1).forEach { parcela ->
            assertEquals(StatusLancamento.CONFIRMADO, parcela.status)
            assertNull(parcela.motivoPendencia)
        }
    }

    private class VigenciasFixas(private val vigencias: List<VigenciaDeCartao>) : ConfiguracaoCartaoRepository {
        override fun observePorInstrumento(instrumentoPublicId: String): Flow<List<VigenciaDeCartao>> = emptyFlow()

        override suspend fun listarPorInstrumento(instrumentoPublicId: String): List<VigenciaDeCartao> = vigencias

        override suspend fun listarAtivas(): List<VigenciaDeCartao> = vigencias
    }
}
