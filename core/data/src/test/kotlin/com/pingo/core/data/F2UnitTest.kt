package com.pingo.core.data

import com.pingo.core.data.captura.FakeContas
import com.pingo.core.data.captura.FakeInstrumentos
import com.pingo.core.data.captura.FakeLancamentos
import com.pingo.core.data.captura.MutableClock
import com.pingo.core.data.impl.FakeInstrumentoDao
import com.pingo.core.data.impl.InicioFinanceiroProjectionImpl
import com.pingo.core.data.projecao.ContasFinanceiroProjection
import com.pingo.core.data.revisao.ConfirmarPendencias
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.fatura.FaturasDoCartao
import com.pingo.core.model.organizacao.ContaComIndicadores
import com.pingo.core.model.organizacao.EstadoBlocoFinanceiro
import com.pingo.core.model.organizacao.IndicadorDeInstrumento
import com.pingo.core.model.organizacao.ResumoContasFinanceiro
import com.pingo.core.model.voucher.DetalheVoucher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class F2UnitTest {
    private val agora: Instant = Instant.parse("2026-11-10T15:00:00Z")
    private val clock = MutableClock(agora, ZoneId.of("America/Sao_Paulo"))

    @Test
    fun `confirmar pendencias nao aprova divergencia ou beneficio ambiguo em lote`() = runTest {
        val contas = FakeContas()
        val instrumentos = FakeInstrumentos(contas)
        val lancamentos = FakeLancamentos(instrumentos)
        val confirmar = ConfirmarPendencias(lancamentos, instrumentos, clock)

        val inst = instrumentos.criar(InstituicaoFinanceira.CAJU)
        val divergente = pendenteBase(inst.publicId, "lanc-div", MotivoPendencia.DIVERGENCIA_RECARGA.name)
        val ambiguo = pendenteBase(inst.publicId, "lanc-amb", MotivoPendencia.BENEFICIO_AMBIGUO.name)
        val normal = pendenteBase(inst.publicId, "lanc-ok", MotivoPendencia.BAIXA_CONFIANCA.name)
            .copy(tipo = TipoLancamento.DESPESA)
        lancamentos.salvarTodos(listOf(divergente, ambiguo, normal))

        val resultado = confirmar.confirmarTodos()

        assertEquals(1, resultado.confirmados)
        assertEquals(2, resultado.mantidos)
        assertEquals(StatusLancamento.PENDENTE, lancamentos.buscar("lanc-div")?.status)
        assertEquals(StatusLancamento.PENDENTE, lancamentos.buscar("lanc-amb")?.status)
        assertEquals(StatusLancamento.CONFIRMADO, lancamentos.buscar("lanc-ok")?.status)
    }

    @Test
    fun `bloco de faturas com falha nao derruba bloco de vouchers`() = runTest {
        val fakeDao = FakeInstrumentoDao()
        val saudavel = projecaoComVoucher(42000L)
        val comFalha = object : ContasFinanceiroProjection by saudavel {
            override fun observeContas(): Flow<ResumoContasFinanceiro> = flow {
                throw IllegalStateException("falha simulada")
            }
        }
        val inicioFalha = InicioFinanceiroProjectionImpl(comFalha, fakeDao)
        val inicioSaudavel = InicioFinanceiroProjectionImpl(saudavel, fakeDao)

        val estadosFaturas = inicioFalha.observeFaturasAbertas().toList()
        val estadosVouchers = inicioSaudavel.observeSaldosVoucher().toList()

        assertEquals(EstadoBlocoFinanceiro.Erro, estadosFaturas.last())
        val sucesso = estadosVouchers.last() as EstadoBlocoFinanceiro.Sucesso
        assertEquals(42000L, sucesso.dados.single().saldoCentavos)
    }

    @Test
    fun `falha de instrumento dao afeta apenas bloco de faturas no inicio`() = runTest {
        val projection = InicioFinanceiroProjectionImpl(
            projecaoComVoucher(35000L),
            InstrumentoDaoComFalha(),
        )

        val estadosFaturas = projection.observeFaturasAbertas().toList()
        val estadosVouchers = projection.observeSaldosVoucher().toList()

        assertTrue(estadosFaturas.first() is EstadoBlocoFinanceiro.Carregando)
        assertEquals(EstadoBlocoFinanceiro.Erro, estadosFaturas.last())
        assertTrue(estadosVouchers.last() is EstadoBlocoFinanceiro.Sucesso)
    }

    private fun pendenteBase(instId: String, id: String, motivo: String) = Lancamento(
        publicId = id,
        instrumentoPublicId = instId,
        valorCentavos = 50000L,
        dataHora = agora,
        tipo = TipoLancamento.RECEITA,
        estabelecimento = "Recarga Caju",
        status = StatusLancamento.PENDENTE,
        origem = OrigemLancamento.NOTIFICACAO,
        confianca = 1.0,
        motivoPendencia = motivo,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private fun projecaoComVoucher(saldoCentavos: Long): ContasFinanceiroProjection {
        val conta = Conta("conta-caju", InstituicaoFinanceira.CAJU, "Caju", TipoConta.BENEFICIO, criadoEm = agora, atualizadoEm = agora)
        val inst = Instrumento(
            publicId = "inst-va",
            contaPublicId = conta.publicId,
            tipo = TipoInstrumento.VOUCHER,
            apelido = "VA",
            beneficio = BeneficioVoucher.VA,
            saldoCentavos = 50000L,
            criadoEm = agora,
            atualizadoEm = agora,
        )
        val resumo = ResumoContasFinanceiro(
            contasAtivas = listOf(
                ContaComIndicadores(
                    conta = conta,
                    instrumentos = listOf(IndicadorDeInstrumento.Voucher(inst, BeneficioVoucher.VA, saldoCentavos, null)),
                ),
            ),
            contasArquivadas = emptyList(),
        )
        return object : ContasFinanceiroProjection {
            override fun observeContas(): Flow<ResumoContasFinanceiro> = flowOf(resumo)

            override fun observeFaturasDoCartao(instrumentoPublicId: String): Flow<FaturasDoCartao?> = flowOf(null)

            override fun observeDetalheVoucher(instrumentoPublicId: String): Flow<DetalheVoucher?> = flowOf(null)
        }
    }

    private class InstrumentoDaoComFalha : com.pingo.core.database.dao.InstrumentoDao by FakeInstrumentoDao() {
        override fun observeAtivos(): Flow<List<InstrumentoEntity>> = flow {
            throw IllegalStateException("erro ao ler cartoes")
        }
    }
}
