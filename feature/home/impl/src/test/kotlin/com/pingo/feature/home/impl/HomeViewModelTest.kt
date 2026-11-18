package com.pingo.feature.home.impl

import com.pingo.core.datastore.MotivoParada
import com.pingo.core.model.Categoria
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.organizacao.EstadoBlocoFinanceiro
import com.pingo.core.model.organizacao.FaturaAbertaInicio
import com.pingo.core.model.organizacao.SaldoVoucherInicio
import com.pingo.core.notifications.DiagnosticoDaCaptura
import com.pingo.core.notifications.SituacaoDaCaptura
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

class HomeViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    private val relogio: Clock = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `saldo do mes soma receitas menos despesas sem transferencias`() {
        val viewModel = criarViewModel(
            lancamentos = listOf(
                lancamentoDeTeste("receita", 620000, NOVEMBRO, tipo = TipoLancamento.RECEITA),
                lancamentoDeTeste("despesa", 371810, NOVEMBRO_DESPESA),
                lancamentoDeTeste("transferencia", 50000, NOVEMBRO, tipo = TipoLancamento.TRANSFERENCIA),
            ),
        )

        val state = viewModel.uiState.value

        assertEquals(248190, state.saldoCentavos)
        assertEquals(620000, state.receitasCentavos)
        assertEquals(371810, state.despesasCentavos)
        assertEquals(YearMonth.of(2026, 11), state.mes)
    }

    @Test
    fun `seletor de mes recalcula o saldo e respeita os limites`() {
        val viewModel = criarViewModel(
            lancamentos = listOf(
                lancamentoDeTeste("setembro", 1500, SETEMBRO),
                lancamentoDeTeste("outubro", 4290, OUTUBRO),
                lancamentoDeTeste("novembro", 1000, NOVEMBRO),
            ),
        )

        assertTrue(viewModel.uiState.value.podeVoltarMes)
        assertFalse(viewModel.uiState.value.podeAvancarMes)

        viewModel.mesAnterior()

        assertEquals(YearMonth.of(2026, 10), viewModel.uiState.value.mes)
        assertEquals(-4290, viewModel.uiState.value.saldoCentavos)
        assertEquals(4290, viewModel.uiState.value.despesasCentavos)
        assertTrue(viewModel.uiState.value.podeVoltarMes)
        assertTrue(viewModel.uiState.value.podeAvancarMes)

        viewModel.mesAnterior()

        assertEquals(YearMonth.of(2026, 9), viewModel.uiState.value.mes)
        assertEquals(-1500, viewModel.uiState.value.saldoCentavos)
        assertFalse(viewModel.uiState.value.podeVoltarMes)

        viewModel.mesAnterior()

        assertEquals(YearMonth.of(2026, 9), viewModel.uiState.value.mes)

        viewModel.mesProximo()

        assertEquals(YearMonth.of(2026, 10), viewModel.uiState.value.mes)
    }

    @Test
    fun `pendentes incluem lancamento sem instrumento de qualquer mes`() {
        val viewModel = criarViewModel(
            lancamentos = listOf(
                lancamentoDeTeste(
                    "nulo",
                    1000,
                    NOVEMBRO,
                    status = StatusLancamento.PENDENTE,
                    instrumentoPublicId = null,
                    categoriaPublicId = null,
                ),
                lancamentoDeTeste("outubro", 2000, OUTUBRO, status = StatusLancamento.PENDENTE),
                lancamentoDeTeste("confirmado", 3000, NOVEMBRO),
                lancamentoDeTeste("excluido", 4000, NOVEMBRO, status = StatusLancamento.PENDENTE, excluidoEm = NOVEMBRO),
            ),
        )

        assertEquals(2, viewModel.uiState.value.pendentes)
    }

    @Test
    fun `top tres categorias de despesa do mes`() {
        val categorias = listOf(
            categoriaDeTeste("alimentacao", "Alimentação"),
            categoriaDeTeste("mercado", "Mercado"),
            categoriaDeTeste("transporte", "Transporte"),
            categoriaDeTeste("lazer", "Lazer"),
        )
        val viewModel = criarViewModel(
            categorias = categorias,
            lancamentos = listOf(
                lancamentoDeTeste("a", 4000, NOVEMBRO, categoriaPublicId = "alimentacao"),
                lancamentoDeTeste("m", 3000, NOVEMBRO, categoriaPublicId = "mercado"),
                lancamentoDeTeste("t", 2000, NOVEMBRO, categoriaPublicId = "transporte"),
                lancamentoDeTeste("l", 1000, NOVEMBRO, categoriaPublicId = "lazer"),
                lancamentoDeTeste("transferencia", 5000, NOVEMBRO, tipo = TipoLancamento.TRANSFERENCIA),
            ),
        )

        val topo = viewModel.uiState.value.topCategorias

        assertEquals(listOf("Alimentação", "Mercado", "Transporte"), topo.map { it.nome })
        assertEquals(listOf(40, 30, 20), topo.map { it.percentual })
        assertEquals(4000, topo.first().valorCentavos)
    }

    @Test
    fun `ultimos cinco lancamentos incluem pendente sem instrumento`() {
        val viewModel = criarViewModel(
            lancamentos = listOf(
                lancamentoDeTeste("t1", 100, INSTANTE_1),
                lancamentoDeTeste("t2", 200, INSTANTE_2),
                lancamentoDeTeste("t3", 300, INSTANTE_3),
                lancamentoDeTeste("t4", 400, INSTANTE_4),
                lancamentoDeTeste("t5", 500, INSTANTE_5),
                lancamentoDeTeste(
                    "pendente-nulo",
                    600,
                    INSTANTE_6,
                    status = StatusLancamento.PENDENTE,
                    instrumentoPublicId = null,
                    categoriaPublicId = null,
                ),
                lancamentoDeTeste("excluido", 700, INSTANTE_6, excluidoEm = INSTANTE_6),
            ),
        )

        val ultimos = viewModel.uiState.value.ultimos

        assertEquals(5, ultimos.size)
        assertEquals("pendente-nulo", ultimos.first().lancamento.publicId)
        assertEquals(null, ultimos.first().instrumento)
        assertEquals("t2", ultimos.last().lancamento.publicId)
    }

    @Test
    fun `sem lancamentos a inicio fica vazia`() {
        val vazia = criarViewModel()
        assertTrue(vazia.uiState.value.vazia)
        assertFalse(vazia.uiState.value.carregando)

        val comLancamento = criarViewModel(lancamentos = listOf(lancamentoDeTeste("a", 1000, NOVEMBRO)))
        assertFalse(comLancamento.uiState.value.vazia)
    }

    @Test
    fun `desconexao com acesso e leitura recente aparece na inicio`() {
        val saude = FakeSaudeDaCaptura(
            diagnostico = DiagnosticoDaCaptura(
                situacao = SituacaoDaCaptura.PARADA,
                motivo = MotivoParada.DESCONECTADO,
                ultimaLeituraEm = Instant.parse("2026-11-10T11:50:00Z"),
            ),
        )

        val viewModel = criarViewModel(saude = saude)

        assertTrue(viewModel.uiState.value.capturaAutomatica)
        assertTrue(viewModel.uiState.value.capturaParada)
        assertEquals(SituacaoDaCaptura.PARADA, viewModel.uiState.value.situacao)
        assertEquals(MotivoParada.DESCONECTADO, viewModel.uiState.value.motivo)
        assertFalse(viewModel.uiState.value.semLeituraHa48h)
    }

    @Test
    fun `sem acesso a captura aparece parada`() {
        val viewModel = criarViewModel(acesso = FakeAcessoNotificacoes(concedido = false))

        assertFalse(viewModel.uiState.value.capturaAutomatica)
        assertTrue(viewModel.uiState.value.capturaParada)
        assertEquals(SituacaoDaCaptura.PARADA, viewModel.uiState.value.situacao)
        assertEquals(MotivoParada.PERMISSAO_REVOGADA, viewModel.uiState.value.motivo)
        assertFalse(viewModel.uiState.value.semLeituraHa48h)
    }

    @Test
    fun `atualizar reflete a concessao do acesso`() {
        val acesso = FakeAcessoNotificacoes(concedido = false)
        val viewModel = criarViewModel(acesso = acesso)

        acesso.concedido = true
        viewModel.atualizarCaptura()

        assertTrue(viewModel.uiState.value.capturaAutomatica)
        assertFalse(viewModel.uiState.value.capturaParada)
    }

    @Test
    fun `sem leitura ha quarenta e oito horas aparece captura parada`() {
        val saude = FakeSaudeDaCaptura(
            diagnostico = DiagnosticoDaCaptura(
                situacao = SituacaoDaCaptura.SEM_LEITURA,
                semLeituraDesde = Instant.now().minus(Duration.ofHours(49)),
            ),
        )

        val viewModel = criarViewModel(saude = saude)

        assertTrue(viewModel.uiState.value.capturaParada)
        assertTrue(viewModel.uiState.value.semLeituraHa48h)
        assertEquals(SituacaoDaCaptura.SEM_LEITURA, viewModel.uiState.value.situacao)
        assertEquals(null, viewModel.uiState.value.motivo)
    }

    @Test
    fun `leitura recente volta a esconder o aviso`() {
        val saude = FakeSaudeDaCaptura(
            diagnostico = DiagnosticoDaCaptura(
                situacao = SituacaoDaCaptura.SEM_LEITURA,
                semLeituraDesde = Instant.now().minus(Duration.ofHours(49)),
            ),
        )
        val viewModel = criarViewModel(saude = saude)

        saude.diagnostico.value = DiagnosticoDaCaptura(situacao = SituacaoDaCaptura.ATIVA)
        viewModel.atualizarCaptura()

        assertFalse(viewModel.uiState.value.semLeituraHa48h)
        assertFalse(viewModel.uiState.value.capturaParada)
    }

    @Test
    fun `estado parado com silencio de quarenta e oito horas aparece na inicio`() {
        val saude = FakeSaudeDaCaptura(
            diagnostico = DiagnosticoDaCaptura(
                situacao = SituacaoDaCaptura.PARADA,
                semLeituraDesde = Instant.now().minus(Duration.ofHours(49)),
            ),
        )

        val viewModel = criarViewModel(saude = saude)

        assertTrue(viewModel.uiState.value.semLeituraHa48h)
        assertTrue(viewModel.uiState.value.capturaParada)
        assertEquals(SituacaoDaCaptura.PARADA, viewModel.uiState.value.situacao)
        assertEquals(null, viewModel.uiState.value.motivo)
    }

    @Test
    fun `falta de instrumento nao significa captura parada`() {
        val viewModel = criarViewModel(
            lancamentos = listOf(
                lancamentoDeTeste(
                    "nulo",
                    1000,
                    NOVEMBRO,
                    status = StatusLancamento.PENDENTE,
                    instrumentoPublicId = null,
                ),
            ),
            instrumentos = emptyList(),
        )

        assertEquals(1, viewModel.uiState.value.pendentes)
        assertTrue(viewModel.uiState.value.capturaAutomatica)
        assertFalse(viewModel.uiState.value.capturaParada)
    }

    @Test
    fun `saudacao segue o horario do relogio`() {
        val manha = criarViewModel(clock = Clock.fixed(Instant.parse("2026-11-10T09:00:00Z"), ZoneOffset.UTC))
        val tarde = criarViewModel(clock = Clock.fixed(Instant.parse("2026-11-10T14:00:00Z"), ZoneOffset.UTC))
        val noite = criarViewModel(clock = Clock.fixed(Instant.parse("2026-11-10T21:00:00Z"), ZoneOffset.UTC))
        val madrugada = criarViewModel(clock = Clock.fixed(Instant.parse("2026-11-10T03:00:00Z"), ZoneOffset.UTC))

        assertEquals("Bom dia,", manha.uiState.value.saudacao)
        assertEquals("Boa tarde,", tarde.uiState.value.saudacao)
        assertEquals("Boa noite,", noite.uiState.value.saudacao)
        assertEquals("Boa noite,", madrugada.uiState.value.saudacao)
    }

    @Test
    fun `faturas abertas e saldos de voucher populam o estado sem exigir lancamentos`() {
        val fatura = faturaAbertaDeTeste()
        val voucher = saldoVoucherDeTeste()
        val projecao = FakeInicioFinanceiroProjection(
            faturas = EstadoBlocoFinanceiro.Sucesso(listOf(fatura)),
            vouchers = EstadoBlocoFinanceiro.Sucesso(listOf(voucher)),
        )

        val viewModel = criarViewModel(projecao = projecao)
        val state = viewModel.uiState.value

        assertEquals(EstadoBlocoFinanceiro.Sucesso(listOf(fatura)), state.faturasAbertas)
        assertEquals(EstadoBlocoFinanceiro.Sucesso(listOf(voucher)), state.saldosVoucher)
        assertFalse(state.vazia)
    }

    @Test
    fun `erro em faturas nao afeta saldos de voucher nem lancamentos`() {
        val voucher = saldoVoucherDeTeste()
        val projecao = FakeInicioFinanceiroProjection(
            faturas = EstadoBlocoFinanceiro.Erro,
            vouchers = EstadoBlocoFinanceiro.Sucesso(listOf(voucher)),
        )

        val viewModel = criarViewModel(
            lancamentos = listOf(lancamentoDeTeste("receita", 50000, NOVEMBRO, tipo = TipoLancamento.RECEITA)),
            projecao = projecao,
        )
        val state = viewModel.uiState.value

        assertEquals(EstadoBlocoFinanceiro.Erro, state.faturasAbertas)
        assertEquals(EstadoBlocoFinanceiro.Sucesso(listOf(voucher)), state.saldosVoucher)
        assertEquals(50000, state.saldoCentavos)
        assertFalse(state.vazia)
    }

    @Test
    fun `erro em vouchers nao afeta faturas abertas nem lancamentos`() {
        val fatura = faturaAbertaDeTeste()
        val projecao = FakeInicioFinanceiroProjection(
            faturas = EstadoBlocoFinanceiro.Sucesso(listOf(fatura)),
            vouchers = EstadoBlocoFinanceiro.Erro,
        )

        val viewModel = criarViewModel(
            lancamentos = listOf(lancamentoDeTeste("despesa", 12000, NOVEMBRO)),
            projecao = projecao,
        )
        val state = viewModel.uiState.value

        assertEquals(EstadoBlocoFinanceiro.Sucesso(listOf(fatura)), state.faturasAbertas)
        assertEquals(EstadoBlocoFinanceiro.Erro, state.saldosVoucher)
        assertEquals(-12000, state.saldoCentavos)
        assertFalse(state.vazia)
    }

    @Test
    fun `excecao no flow de faturas isola erro sem interromper saldos de voucher`() {
        val voucher = saldoVoucherDeTeste()
        val projecao = FakeInicioFinanceiroProjection(
            vouchers = EstadoBlocoFinanceiro.Sucesso(listOf(voucher)),
            fluxoFaturasCustom = flow { throw IllegalStateException("falha simulada") },
        )

        val viewModel = criarViewModel(projecao = projecao)
        val state = viewModel.uiState.value

        assertEquals(EstadoBlocoFinanceiro.Erro, state.faturasAbertas)
        assertEquals(EstadoBlocoFinanceiro.Sucesso(listOf(voucher)), state.saldosVoucher)
        assertFalse(state.vazia)
    }

    @Test
    fun `construtor secundario sem projecao mantem blocos vazios`() {
        val viewModel = HomeViewModel(
            acessoNotificacoes = FakeAcessoNotificacoes(concedido = true),
            saudeDaCaptura = FakeSaudeDaCaptura(),
            lancamentoRepository = FakeLancamentoRepository(emptyList()),
            contaRepository = FakeContaRepository(listOf(contaDeTeste())),
            instrumentoRepository = FakeInstrumentoRepository(listOf(instrumentoDeTeste())),
            categoriaRepository = FakeCategoriaRepository(emptyList()),
            clock = relogio,
        )

        val state = viewModel.uiState.value

        assertEquals(EstadoBlocoFinanceiro.Sucesso(emptyList<FaturaAbertaInicio>()), state.faturasAbertas)
        assertEquals(EstadoBlocoFinanceiro.Sucesso(emptyList<SaldoVoucherInicio>()), state.saldosVoucher)
        assertTrue(state.vazia)
    }

    @Suppress("LongParameterList")
    private fun criarViewModel(
        lancamentos: List<Lancamento> = emptyList(),
        categorias: List<Categoria> = emptyList(),
        instrumentos: List<Instrumento> = listOf(instrumentoDeTeste()),
        acesso: FakeAcessoNotificacoes = FakeAcessoNotificacoes(concedido = true),
        saude: FakeSaudeDaCaptura = FakeSaudeDaCaptura(),
        projecao: FakeInicioFinanceiroProjection = FakeInicioFinanceiroProjection(),
        clock: Clock = relogio,
    ): HomeViewModel = HomeViewModel(
        acessoNotificacoes = acesso,
        saudeDaCaptura = saude,
        lancamentoRepository = FakeLancamentoRepository(lancamentos),
        contaRepository = FakeContaRepository(listOf(contaDeTeste())),
        instrumentoRepository = FakeInstrumentoRepository(instrumentos),
        categoriaRepository = FakeCategoriaRepository(categorias),
        inicioFinanceiroProjection = projecao,
        clock = clock,
    )

    private companion object {
        val NOVEMBRO: Instant = Instant.parse("2026-11-05T12:00:00Z")
        val NOVEMBRO_DESPESA: Instant = Instant.parse("2026-11-06T12:00:00Z")
        val OUTUBRO: Instant = Instant.parse("2026-10-05T12:00:00Z")
        val SETEMBRO: Instant = Instant.parse("2026-09-05T12:00:00Z")
        val INSTANTE_1: Instant = Instant.parse("2026-11-01T10:00:00Z")
        val INSTANTE_2: Instant = Instant.parse("2026-11-01T11:00:00Z")
        val INSTANTE_3: Instant = Instant.parse("2026-11-01T12:00:00Z")
        val INSTANTE_4: Instant = Instant.parse("2026-11-01T13:00:00Z")
        val INSTANTE_5: Instant = Instant.parse("2026-11-01T14:00:00Z")
        val INSTANTE_6: Instant = Instant.parse("2026-11-01T15:00:00Z")
    }
}
