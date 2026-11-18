package com.pingo.feature.accounts.impl

import com.pingo.core.data.fatura.ResultadoCompletarDatasCartao
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.fatura.CicloDeFaturaDetalhado
import com.pingo.core.model.fatura.FaturasDoCartao
import com.pingo.core.model.fatura.TipoCicloFatura
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class FaturasViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    private val projection = FakeContasFinanceiroProjection()
    private val categorias = FakeCategoriaRepository()
    private val clock: Clock = Clock.fixed(INSTANTE, ZoneOffset.UTC)

    @Test
    fun `instrumentoId vazio marca estado nao encontrado`() {
        val viewModel = criarViewModel(instrumentoId = "")

        assertFalse(viewModel.uiState.value.carregando)
        assertTrue(viewModel.uiState.value.naoEncontrada)
    }

    @Test
    fun `cartao inexistente na projecao marca nao encontrado`() {
        projection.faturasPorInstrumento.value = emptyMap()
        val viewModel = criarViewModel(instrumentoId = "inexistente")

        assertFalse(viewModel.uiState.value.carregando)
        assertTrue(viewModel.uiState.value.naoEncontrada)
    }

    @Test
    fun `carrega ciclos aberta proximas e fechadas com selecao por aba e categorias`() {
        val categoriaAlimentacao = Categoria(
            publicId = "cat-alim",
            nome = "Alimentação",
            tipo = TipoCategoria.DESPESA,
            criadoEm = INSTANTE,
            atualizadoEm = INSTANTE,
        )
        categorias.categorias.value = listOf(categoriaAlimentacao)
        val lancAberta = lancamento("lanc-1", 4290L, categoriaPublicId = "cat-alim")
        val cicloAberta = ciclo(LocalDate.of(2026, 11, 1), TipoCicloFatura.ABERTA, StatusFatura.ABERTA, 4290L, listOf(lancAberta))
        val cicloProx1 = ciclo(LocalDate.of(2026, 12, 1), TipoCicloFatura.PROXIMA, StatusFatura.ABERTA, 10000L, emptyList())
        val cicloProx2 = ciclo(LocalDate.of(2027, 1, 1), TipoCicloFatura.PROXIMA, StatusFatura.ABERTA, 10000L, emptyList())
        val cicloFechada1 = ciclo(LocalDate.of(2026, 10, 1), TipoCicloFatura.FECHADA, StatusFatura.FECHADA, 64210L, emptyList())
        val cicloFechada2 = ciclo(LocalDate.of(2026, 9, 1), TipoCicloFatura.FECHADA, StatusFatura.PAGA, 50000L, emptyList())

        projection.faturasPorInstrumento.value = mapOf(
            "inst-1" to FaturasDoCartao(
                instrumento = cartao("inst-1", fechamento = 3, vencimento = 10),
                conta = conta("conta-1", "Nubank"),
                configuracaoIncompleta = false,
                ciclos = listOf(cicloProx2, cicloAberta, cicloFechada2, cicloProx1, cicloFechada1),
            ),
        )

        val viewModel = criarViewModel("inst-1")

        assertEquals("Nubank · Cartão", viewModel.uiState.value.tituloCartao)
        assertEquals(AbaFaturas.ABERTA, viewModel.uiState.value.abaSelecionada)
        assertEquals(cicloAberta.competencia, viewModel.uiState.value.cicloSelecionado?.competencia)
        assertEquals("Alimentação", viewModel.uiState.value.lancamentosDoCicloSelecionado.single().categoria?.nome)

        viewModel.onEvent(FaturasEvent.SelecionarAba(AbaFaturas.PROXIMAS))
        assertEquals(LocalDate.of(2026, 12, 1), viewModel.uiState.value.cicloSelecionado?.competencia)
        viewModel.onEvent(FaturasEvent.SelecionarCiclo(LocalDate.of(2027, 1, 1)))
        assertEquals(LocalDate.of(2027, 1, 1), viewModel.uiState.value.cicloSelecionado?.competencia)

        viewModel.onEvent(FaturasEvent.SelecionarAba(AbaFaturas.FECHADAS))
        assertEquals(LocalDate.of(2026, 10, 1), viewModel.uiState.value.cicloSelecionado?.competencia)
        viewModel.onEvent(FaturasEvent.SelecionarCiclo(LocalDate.of(2026, 9, 1)))
        assertEquals(LocalDate.of(2026, 9, 1), viewModel.uiState.value.cicloSelecionado?.competencia)
    }

    @Test
    fun `configuracao incompleta abre modal e salva datas do cartao`() {
        var salvo: Triple<String, Int, Int>? = null
        val inst = cartao("inst-1", fechamento = null, vencimento = null)
        projection.faturasPorInstrumento.value = mapOf(
            "inst-1" to FaturasDoCartao(
                instrumento = inst,
                conta = conta("conta-1", "Inter"),
                configuracaoIncompleta = true,
                ciclos = emptyList(),
            ),
        )

        val viewModel = criarViewModel("inst-1") { id, fechamento, vencimento ->
            salvo = Triple(id, fechamento, vencimento)
            ResultadoCompletarDatasCartao.Sucesso(inst.copy(fechamentoDia = fechamento, vencimentoDia = vencimento))
        }

        assertTrue(viewModel.uiState.value.configuracaoIncompleta)
        viewModel.onEvent(FaturasEvent.AbrirCompletarDatas)
        viewModel.onEvent(FaturasEvent.AlterarFechamentoModal("5"))
        viewModel.onEvent(FaturasEvent.AlterarVencimentoModal("12"))
        viewModel.onEvent(FaturasEvent.SalvarDatasCartao)

        assertEquals(Triple("inst-1", 5, 12), salvo)
        assertNull(viewModel.uiState.value.modalCompletarDatas)
    }

    @Test
    fun `falha de leitura mostra erro e tentarDeNovo recupera`() {
        projection.falhaAoObservarFaturas = IllegalStateException("erro")
        val viewModel = criarViewModel("inst-1")

        assertTrue(viewModel.uiState.value.falha)
        assertFalse(viewModel.uiState.value.carregando)

        projection.falhaAoObservarFaturas = null
        projection.faturasPorInstrumento.value = mapOf(
            "inst-1" to FaturasDoCartao(
                instrumento = cartao("inst-1", 5, 12),
                conta = conta("conta-1", "Nubank"),
                configuracaoIncompleta = false,
                ciclos = listOf(ciclo(LocalDate.of(2026, 11, 1), TipoCicloFatura.ABERTA, StatusFatura.ABERTA, 0L, emptyList())),
            ),
        )
        viewModel.onEvent(FaturasEvent.TentarDeNovo)

        assertFalse(viewModel.uiState.value.falha)
        assertEquals(0L, viewModel.uiState.value.cicloSelecionado?.totalCentavos)
        assertTrue(viewModel.uiState.value.lancamentosDoCicloSelecionado.isEmpty())
    }

    private fun criarViewModel(
        instrumentoId: String,
        completarDatas: suspend (String, Int, Int) -> ResultadoCompletarDatasCartao = { _, _, _ ->
            ResultadoCompletarDatasCartao.NaoEncontrado
        },
    ): FaturasViewModel = FaturasViewModel(
        instrumentoPublicId = instrumentoId,
        contasFinanceiroProjection = projection,
        categoriaRepository = categorias,
        completarDatas = completarDatas,
        clock = clock,
    )

    private fun conta(publicId: String, apelido: String): Conta = Conta(
        publicId = publicId,
        instituicao = InstituicaoFinanceira.NUBANK,
        apelido = apelido,
        tipo = TipoConta.CORRENTE,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
    )

    private fun cartao(publicId: String, fechamento: Int?, vencimento: Int?): Instrumento = Instrumento(
        publicId = publicId,
        contaPublicId = "conta-1",
        tipo = TipoInstrumento.CREDITO,
        apelido = "Cartão",
        ultimosDigitos = "4321",
        fechamentoDia = fechamento,
        vencimentoDia = vencimento,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
    )

    private fun ciclo(
        competencia: LocalDate,
        tipoCiclo: TipoCicloFatura,
        status: StatusFatura,
        totalCentavos: Long,
        lancamentos: List<Lancamento>,
    ): CicloDeFaturaDetalhado = CicloDeFaturaDetalhado(
        faturaPublicId = "fat-$competencia",
        instrumentoPublicId = "inst-1",
        competencia = competencia,
        dataFechamento = competencia.withDayOfMonth(5),
        dataVencimento = competencia.withDayOfMonth(12),
        totalCentavos = totalCentavos,
        status = status,
        tipoCiclo = tipoCiclo,
        diasParaVencimento = 10L,
        vencida = false,
        lancamentos = lancamentos,
    )

    private fun lancamento(publicId: String, valorCentavos: Long, categoriaPublicId: String?): Lancamento = Lancamento(
        publicId = publicId,
        instrumentoPublicId = "inst-1",
        categoriaPublicId = categoriaPublicId,
        valorCentavos = valorCentavos,
        dataHora = INSTANTE,
        tipo = TipoLancamento.DESPESA,
        estabelecimento = "Padaria",
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.NOTIFICACAO,
        confianca = 1.0,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
    )

    private companion object {
        val INSTANTE: Instant = Instant.parse("2026-11-10T12:00:00Z")
    }
}
