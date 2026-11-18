package com.pingo.feature.transactions.impl

import androidx.lifecycle.SavedStateHandle
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.testing.TestDispatcherRule
import com.pingo.feature.transactions.api.TransactionsRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val agora = Instant.parse("2026-11-10T15:30:00Z")
    private val clock = Clock.fixed(agora, ZoneOffset.UTC)

    private val contaNubank = Conta(
        publicId = "conta-nubank",
        instituicao = InstituicaoFinanceira.NUBANK,
        apelido = "Nubank",
        tipo = TipoConta.CORRENTE,
        criadoEm = agora,
        atualizadoEm = agora,
    )
    private val contaItau = Conta(
        publicId = "conta-itau",
        instituicao = InstituicaoFinanceira.ITAU,
        apelido = "Itaú",
        tipo = TipoConta.CORRENTE,
        criadoEm = agora,
        atualizadoEm = agora,
    )
    private val instrumentoNubank = Instrumento(
        publicId = "inst-nubank",
        contaPublicId = contaNubank.publicId,
        tipo = TipoInstrumento.CREDITO,
        apelido = "Cartão de crédito",
        criadoEm = agora,
        atualizadoEm = agora,
    )
    private val instrumentoItau = Instrumento(
        publicId = "inst-itau",
        contaPublicId = contaItau.publicId,
        tipo = TipoInstrumento.DEBITO,
        apelido = "Cartão de débito",
        criadoEm = agora,
        atualizadoEm = agora,
    )
    private val alimentacao = categoria("cat-alimentacao", "Alimentação", TipoCategoria.DESPESA)
    private val mercado = categoria("cat-mercado", "Mercado", TipoCategoria.DESPESA)
    private val salario = categoria("cat-salario", "Salário", TipoCategoria.RECEITA)

    private val padaria = lancamentoDeTeste(
        publicId = "l1",
        dataHora = Instant.parse("2026-11-10T14:32:00Z"),
        valorCentavos = 4290,
        estabelecimento = "Padaria do Zé",
        instrumentoPublicId = instrumentoNubank.publicId,
        categoriaPublicId = alimentacao.publicId,
    )
    private val semInstrumento = lancamentoDeTeste(
        publicId = "l2",
        dataHora = Instant.parse("2026-11-10T11:05:00Z"),
        valorCentavos = 1890,
        status = StatusLancamento.PENDENTE,
        estabelecimento = "Loja Sem Cartão",
        instrumentoPublicId = null,
        instituicaoOrigem = InstituicaoFinanceira.NUBANK,
    )
    private val transferencia = lancamentoDeTeste(
        publicId = "l4",
        dataHora = Instant.parse("2026-11-10T08:02:00Z"),
        valorCentavos = 50000,
        tipo = TipoLancamento.TRANSFERENCIA,
        estabelecimento = "Transferência entre contas",
        instrumentoPublicId = instrumentoNubank.publicId,
    )
    private val receita = lancamentoDeTeste(
        publicId = "l3",
        dataHora = Instant.parse("2026-11-09T19:20:00Z"),
        valorCentavos = 15000,
        tipo = TipoLancamento.RECEITA,
        estabelecimento = "Salário",
        instrumentoPublicId = instrumentoItau.publicId,
        categoriaPublicId = salario.publicId,
    )
    private val manual = lancamentoDeTeste(
        publicId = "l5",
        dataHora = Instant.parse("2026-11-08T10:00:00Z"),
        valorCentavos = 1000,
        origem = OrigemLancamento.MANUAL,
        estabelecimento = "Padaria do Zé",
        instrumentoPublicId = instrumentoItau.publicId,
        categoriaPublicId = alimentacao.publicId,
    )

    @Test
    fun `agrupa por dia com total do dia e inclui pendente sem instrumento`() = runTest {
        val viewModel = criarViewModel()
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertFalse(estado.carregando)
        assertEquals(3, estado.dias.size)
        val primeiroDia = estado.dias.first()
        assertEquals(LocalDate.of(2026, 11, 10), primeiroDia.data)
        assertEquals("HOJE", primeiroDia.rotulo)
        assertEquals(-6180, primeiroDia.totalCentavos)
        assertEquals(3, primeiroDia.itens.size)
        assertNull(primeiroDia.itens.single { it.lancamento.publicId == "l2" }.instrumento)
        assertNull(primeiroDia.itens.single { it.lancamento.publicId == "l2" }.conta)
        assertNotNull(primeiroDia.itens.single { it.lancamento.publicId == "l1" }.categoria)
        assertEquals(5, estado.quantidadeDeLancamentos)
    }

    @Test
    fun `total do dia ignora transferencias`() = runTest {
        val viewModel = criarViewModel(lancamentos = listOf(transferencia, padaria))
        advanceUntilIdle()

        val dia = viewModel.uiState.value.dias.single()
        assertEquals(-4290, dia.totalCentavos)
    }

    @Test
    fun `filtro de vinculo nao atribui pendente sem instrumento a conta ou cartao`() = runTest {
        val viewModel = criarViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TransactionsEvent.SelecionarVinculo(VinculoFiltro.Conta(contaNubank.publicId)))
        advanceUntilIdle()

        val porConta = viewModel.uiState.value
        assertEquals(setOf("l1", "l4"), porConta.dias.flatMap { dia -> dia.itens.map { it.lancamento.publicId } }.toSet())

        viewModel.onEvent(TransactionsEvent.SelecionarVinculo(VinculoFiltro.Instrumento(instrumentoNubank.publicId)))
        advanceUntilIdle()

        val porInstrumento = viewModel.uiState.value
        assertEquals(setOf("l1", "l4"), porInstrumento.dias.flatMap { dia -> dia.itens.map { it.lancamento.publicId } }.toSet())

        viewModel.onEvent(TransactionsEvent.SelecionarVinculo(null))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.dias.any { dia -> dia.itens.any { it.lancamento.publicId == "l2" } })
    }

    @Test
    fun `filtros de tipo categoria e status combinam com a busca`() = runTest {
        val viewModel = criarViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TransactionsEvent.SelecionarTipo(TipoLancamento.DESPESA))
        viewModel.onEvent(TransactionsEvent.SelecionarCategoria(alimentacao.publicId))
        viewModel.onEvent(TransactionsEvent.SelecionarStatus(StatusFiltro.CONFIRMADO))
        viewModel.onEvent(TransactionsEvent.BuscaAlterada("padaria"))
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(2, estado.quantidadeDeLancamentos)
        assertEquals(setOf("l1", "l5"), estado.dias.flatMap { dia -> dia.itens.map { it.lancamento.publicId } }.toSet())
        assertEquals(3, estado.filtrosAtivos)
        assertNull(estado.estadoVazio)
    }

    @Test
    fun `busca por estabelecimento ignora acentos e caixa`() = runTest {
        val viewModel = criarViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TransactionsEvent.BuscaAlterada("PADARIA DO ZE"))
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(setOf("l1", "l5"), estado.dias.flatMap { dia -> dia.itens.map { it.lancamento.publicId } }.toSet())
    }

    @Test
    fun `filtro de mes sem dados mostra nada com esses filtros e limpar restaura`() = runTest {
        val viewModel = criarViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TransactionsEvent.SelecionarMes(YearMonth.of(2026, 10)))
        advanceUntilIdle()

        val vazio = viewModel.uiState.value
        assertEquals(EstadoVazioDosLancamentos.NADA_COM_OS_FILTROS, vazio.estadoVazio)
        assertEquals(0, vazio.quantidadeDeLancamentos)

        viewModel.onEvent(TransactionsEvent.LimparFiltros)
        advanceUntilIdle()

        val restaurado = viewModel.uiState.value
        assertNull(restaurado.estadoVazio)
        assertNull(restaurado.filtros.mes)
        assertEquals(5, restaurado.quantidadeDeLancamentos)
    }

    @Test
    fun `limpar filtros tambem limpa a busca`() = runTest {
        val viewModel = criarViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TransactionsEvent.BuscaAlterada("inexistente"))
        advanceUntilIdle()
        assertEquals(EstadoVazioDosLancamentos.NADA_COM_OS_FILTROS, viewModel.uiState.value.estadoVazio)

        viewModel.onEvent(TransactionsEvent.LimparFiltros)
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.busca)
        assertEquals(5, viewModel.uiState.value.quantidadeDeLancamentos)
    }

    @Test
    fun `excluir marca o lancamento e desfazer restaura`() = runTest {
        val repositorio = FakeLancamentoRepository(listaCompleta())
        val viewModel = criarViewModel(repositorio = repositorio)
        advanceUntilIdle()

        viewModel.onEvent(TransactionsEvent.Excluir("l1"))
        advanceUntilIdle()

        val excluido = viewModel.uiState.value
        assertNotNull(excluido.mensagemDeExclusao)
        assertEquals(4, excluido.quantidadeDeLancamentos)
        assertNotNull(repositorio.atual("l1")?.excluidoEm)

        viewModel.onEvent(TransactionsEvent.DesfazerExclusao)
        advanceUntilIdle()

        val restaurado = viewModel.uiState.value
        assertNull(restaurado.mensagemDeExclusao)
        assertEquals(5, restaurado.quantidadeDeLancamentos)
        assertNull(repositorio.atual("l1")?.excluidoEm)
    }

    @Test
    fun `troca de categoria mostra apenas categorias do tipo e salva a escolha`() = runTest {
        val repositorio = FakeLancamentoRepository(listaCompleta())
        val viewModel = criarViewModel(repositorio = repositorio)
        advanceUntilIdle()

        viewModel.onEvent(TransactionsEvent.AbrirTrocaCategoria("l1"))
        advanceUntilIdle()

        val troca = viewModel.uiState.value.trocaDeCategoria
        assertNotNull(troca)
        assertEquals("Padaria do Zé", troca?.estabelecimento)
        assertEquals(setOf(alimentacao.publicId, mercado.publicId), troca?.opcoes?.map { it.publicId }?.toSet())

        viewModel.onEvent(TransactionsEvent.TrocarCategoria("l1", mercado.publicId))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.trocaDeCategoria)
        assertEquals(mercado.publicId, repositorio.atual("l1")?.categoriaPublicId)
    }

    @Test
    fun `opcoes de vinculo trazem contas e instrumentos do passo sete`() = runTest {
        val viewModel = criarViewModel()
        advanceUntilIdle()

        val opcoes = viewModel.uiState.value.opcoesDeVinculo
        assertEquals(
            listOf("Nubank", "Nubank crédito", "Itaú", "Itaú débito"),
            opcoes.map { it.rotulo },
        )
        assertEquals(
            listOf("conta-conta-nubank", "instrumento-inst-nubank", "conta-conta-itau", "instrumento-inst-itau"),
            opcoes.map { it.chave },
        )
    }

    @Test
    fun `mes inicial usa o mes mais recente com dados quando o atual esta vazio`() = runTest {
        val janeiro = Clock.fixed(Instant.parse("2027-01-15T12:00:00Z"), ZoneOffset.UTC)
        val viewModel = criarViewModel(clock = janeiro)
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(YearMonth.of(2026, 11), estado.filtros.mes)
        assertEquals(5, estado.quantidadeDeLancamentos)
    }

    @Test
    fun `mes inicial se ajusta quando os dados chegam depois da primeira emissao`() = runTest {
        val janeiro = Clock.fixed(Instant.parse("2027-01-15T12:00:00Z"), ZoneOffset.UTC)
        val repositorio = FakeLancamentoRepository()
        val viewModel = criarViewModel(repositorio = repositorio, clock = janeiro)
        advanceUntilIdle()

        assertEquals(YearMonth.of(2027, 1), viewModel.uiState.value.filtros.mes)

        repositorio.publicar(listaCompleta())
        advanceUntilIdle()

        assertEquals(YearMonth.of(2026, 11), viewModel.uiState.value.filtros.mes)
        assertEquals(5, viewModel.uiState.value.quantidadeDeLancamentos)
    }

    @Test
    fun `mes escolhido pelo usuario nao e sobrescrito por dados novos`() = runTest {
        val janeiro = Clock.fixed(Instant.parse("2027-01-15T12:00:00Z"), ZoneOffset.UTC)
        val repositorio = FakeLancamentoRepository(listaCompleta())
        val viewModel = criarViewModel(repositorio = repositorio, clock = janeiro)
        advanceUntilIdle()

        viewModel.onEvent(TransactionsEvent.SelecionarMes(YearMonth.of(2026, 10)))
        advanceUntilIdle()

        repositorio.publicar(listaCompleta())
        advanceUntilIdle()

        assertEquals(YearMonth.of(2026, 10), viewModel.uiState.value.filtros.mes)
    }

    @Test
    fun `base vazia mostra nenhum lancamento ainda`() = runTest {
        val viewModel = criarViewModel(lancamentos = emptyList())
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(EstadoVazioDosLancamentos.SEM_LANCAMENTOS, estado.estadoVazio)
        assertEquals(YearMonth.of(2026, 11), estado.filtros.mes)
    }

    @Test
    fun `drill down via savedStateHandle inicializa filtros de categoria mes despesa confirmada e preserva mes`() = runTest {
        val extras = listOf(
            lancamentoOutubro("l-out", StatusLancamento.CONFIRMADO, alimentacao.publicId, "Restaurante Outubro", 3500),
            lancamentoOutubro("l-out-pend", StatusLancamento.PENDENTE, alimentacao.publicId, "Pendente Outubro", 2500),
            lancamentoOutubro("l-out-merc", StatusLancamento.CONFIRMADO, mercado.publicId, "Mercado Outubro", 8000),
        )
        val janeiroClock = Clock.fixed(Instant.parse("2027-01-15T12:00:00Z"), ZoneOffset.UTC)
        val savedStateHandle = SavedStateHandle(
            mapOf(
                TransactionsRoute.ARG_CATEGORIA_ID to alimentacao.publicId,
                TransactionsRoute.ARG_MES to "2026-10",
            ),
        )
        val viewModel = criarViewModel(
            lancamentos = listaCompleta() + extras,
            clock = janeiroClock,
            savedStateHandle = savedStateHandle,
        )
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(YearMonth.of(2026, 10), estado.filtros.mes)
        assertEquals(alimentacao.publicId, estado.filtros.categoriaPublicId)
        assertEquals(StatusFiltro.CONFIRMADO, estado.filtros.status)
        assertEquals(TipoLancamento.DESPESA, estado.filtros.tipo)
        assertEquals(3, estado.filtrosAtivos)
        assertEquals(1, estado.quantidadeDeLancamentos)
        assertEquals("l-out", estado.dias.single().itens.single().lancamento.publicId)

        viewModel.onEvent(TransactionsEvent.LimparFiltros)
        advanceUntilIdle()

        val limpo = viewModel.uiState.value
        assertEquals(FiltrosDeLancamentos(), limpo.filtros)
        assertEquals(0, limpo.filtrosAtivos)
        assertEquals(8, limpo.quantidadeDeLancamentos)
    }

    private fun lancamentoOutubro(
        publicId: String,
        status: StatusLancamento,
        categoriaId: String,
        estabelecimento: String,
        valorCentavos: Long,
    ): Lancamento = lancamentoDeTeste(
        publicId = publicId,
        dataHora = Instant.parse("2026-10-12T12:00:00Z"),
        valorCentavos = valorCentavos,
        status = status,
        estabelecimento = estabelecimento,
        instrumentoPublicId = instrumentoNubank.publicId,
        categoriaPublicId = categoriaId,
    )

    @Test
    fun `navegacao normal sem argumentos nao pre aplica filtros`() = runTest {
        val viewModel = criarViewModel(savedStateHandle = SavedStateHandle())
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertNull(estado.filtros.tipo)
        assertNull(estado.filtros.categoriaPublicId)
        assertNull(estado.filtros.status)
        assertNull(estado.filtros.vinculo)
        assertEquals(0, estado.filtrosAtivos)
    }

    @Test
    fun `drill down com clock utc inclui lancamento na virada do mes em sao paulo`() = runTest {
        val viradaSp = lancamentoDeTeste(
            publicId = "l-virada-sp",
            dataHora = Instant.parse("2026-11-01T01:30:00Z"),
            valorCentavos = 4500,
            estabelecimento = "Jantar Virada",
            instrumentoPublicId = instrumentoNubank.publicId,
            categoriaPublicId = alimentacao.publicId,
        )
        val utcClock = Clock.fixed(Instant.parse("2026-11-01T01:30:00Z"), ZoneOffset.UTC)
        val savedStateHandle = SavedStateHandle(
            mapOf(
                TransactionsRoute.ARG_CATEGORIA_ID to alimentacao.publicId,
                TransactionsRoute.ARG_MES to "2026-10",
            ),
        )
        val viewModel = criarViewModel(
            lancamentos = listOf(viradaSp),
            clock = utcClock,
            savedStateHandle = savedStateHandle,
        )
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(YearMonth.of(2026, 10), estado.filtros.mes)
        assertEquals(1, estado.quantidadeDeLancamentos)
        assertEquals("l-virada-sp", estado.dias.single().itens.single().lancamento.publicId)
    }

    @Test
    fun `exportar csv usa exatamente os lancamentos filtrados ativos e limpa ao consumir`() = runTest {
        val viewModel = criarViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TransactionsEvent.SelecionarTipo(TipoLancamento.DESPESA))
        viewModel.onEvent(TransactionsEvent.BuscaAlterada("padaria"))
        advanceUntilIdle()

        viewModel.onEvent(TransactionsEvent.ExportarCsv)
        advanceUntilIdle()

        val pronta = viewModel.uiState.value.exportacaoCsvPronta
        assertNotNull(pronta)
        assertTrue(pronta!!.conteudoCsv.contains("Padaria do Zé"))
        assertFalse(pronta.conteudoCsv.contains("Salário"))
        assertFalse(pronta.conteudoCsv.contains("Loja Sem Cartão"))
        assertNull(viewModel.uiState.value.avisoExportacaoCsv)

        viewModel.onEvent(TransactionsEvent.ConsumirExportacaoCsv)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.exportacaoCsvPronta)
    }

    @Test
    fun `exportar csv sem lancamentos no filtro atual exibe aviso claro`() = runTest {
        val viewModel = criarViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TransactionsEvent.BuscaAlterada("sem-resultado"))
        advanceUntilIdle()

        viewModel.onEvent(TransactionsEvent.ExportarCsv)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.exportacaoCsvPronta)
        assertEquals(ExportadorCsvDeLancamentos.MENSAGEM_VAZIO, viewModel.uiState.value.avisoExportacaoCsv)

        viewModel.onEvent(TransactionsEvent.DispensarAvisoExportacao)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.avisoExportacaoCsv)
    }

    private fun criarViewModel(
        lancamentos: List<Lancamento> = listaCompleta(),
        repositorio: FakeLancamentoRepository = FakeLancamentoRepository(lancamentos),
        clock: Clock = this.clock,
        savedStateHandle: SavedStateHandle? = null,
    ): TransactionsViewModel = if (savedStateHandle == null) {
        TransactionsViewModel(
            lancamentoRepository = repositorio,
            contaRepository = FakeContaRepository(listOf(contaNubank, contaItau)),
            instrumentoRepository = FakeInstrumentoRepository(listOf(instrumentoNubank, instrumentoItau)),
            categoriaRepository = FakeCategoriaRepository(listOf(alimentacao, mercado, salario)),
            clock = clock,
        )
    } else {
        TransactionsViewModel(
            savedStateHandle = savedStateHandle,
            lancamentoRepository = repositorio,
            contaRepository = FakeContaRepository(listOf(contaNubank, contaItau)),
            instrumentoRepository = FakeInstrumentoRepository(listOf(instrumentoNubank, instrumentoItau)),
            categoriaRepository = FakeCategoriaRepository(listOf(alimentacao, mercado, salario)),
            clock = clock,
        )
    }

    private fun listaCompleta(): List<Lancamento> = listOf(padaria, semInstrumento, transferencia, receita, manual)

    private fun categoria(publicId: String, nome: String, tipo: TipoCategoria): Categoria = Categoria(
        publicId = publicId,
        nome = nome,
        tipo = tipo,
        criadoEm = agora,
        atualizadoEm = agora,
    )
}
