package com.pingo.feature.transactions.impl

import com.pingo.core.data.manual.CampoDeLancamentoManual
import com.pingo.core.data.manual.RegistrarLancamentoManual
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.RegraCategoria
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.fatura.ModoParcelamento
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class NovoLancamentoViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val agora = Instant.parse("2026-11-10T15:30:00Z")
    private val clock = Clock.fixed(agora, ZoneOffset.UTC)
    private val hoje = LocalDate.of(2026, 11, 10)

    private val contaNubank = Conta(
        publicId = "conta-nubank",
        instituicao = InstituicaoFinanceira.NUBANK,
        apelido = "Nubank",
        tipo = TipoConta.CORRENTE,
        criadoEm = agora,
        atualizadoEm = agora,
    )
    private val contaInter = Conta(
        publicId = "conta-inter",
        instituicao = InstituicaoFinanceira.INTER,
        apelido = "Inter",
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
    private val instrumentoInter = Instrumento(
        publicId = "inst-inter",
        contaPublicId = contaInter.publicId,
        tipo = TipoInstrumento.DEBITO,
        apelido = "Cartão de débito",
        criadoEm = agora,
        atualizadoEm = agora,
    )
    private val alimentacao = categoria("cat-alimentacao", "Alimentação", TipoCategoria.DESPESA)
    private val mercado = categoria("cat-mercado", "Mercado", TipoCategoria.DESPESA)
    private val salario = categoria("cat-salario", "Salário", TipoCategoria.RECEITA)
    private val capturadoRecente = lancamento(
        publicId = "capturado-recente",
        dataHora = agora.minus(Duration.ofHours(1)),
        valorCentavos = 1000,
        instrumentoPublicId = instrumentoNubank.publicId,
    )
    private val capturadoDuplicado = lancamento(
        publicId = "capturado-duplicado",
        dataHora = agora.minus(Duration.ofHours(1)),
        valorCentavos = 4290,
        instrumentoPublicId = instrumentoNubank.publicId,
    )

    @Test
    fun `abre com despesa valor zerado ultimo instrumento e data de hoje`() = runTest {
        val cenario = criarViewModel(lancamentos = listOf(capturadoRecente))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertFalse(estado.carregando)
        assertEquals(TipoDoLancamentoManual.DESPESA, estado.tipo)
        assertEquals(0L, estado.valorCentavos)
        assertEquals("Nubank · crédito", estado.instrumento?.rotulo)
        assertEquals(hoje, estado.data)
        assertEquals(hoje, estado.hoje)
        assertEquals(listOf("Alimentação", "Mercado"), estado.categorias.map { it.nome })
        assertNull(estado.categoria)
        assertFalse(estado.categoriaSugerida)
        assertFalse(estado.semInstrumentos)
    }

    @Test
    fun `unico instrumento cadastrado vira default mesmo sem historico`() = runTest {
        val cenario = criarViewModel(instrumentos = listOf(instrumentoInter), contas = listOf(contaInter))
        advanceUntilIdle()

        assertEquals("inst-inter", cenario.viewModel.uiState.value.instrumento?.publicId)
    }

    @Test
    fun `captura sem instrumento resolvido nao vira default`() = runTest {
        val semInstrumento = lancamento(
            publicId = "capturado-sem-instrumento",
            dataHora = agora.minus(Duration.ofMinutes(30)),
            valorCentavos = 1890,
            instrumentoPublicId = null,
        )

        val cenario = criarViewModel(lancamentos = listOf(capturadoRecente, semInstrumento))
        advanceUntilIdle()

        assertEquals(instrumentoNubank.publicId, cenario.viewModel.uiState.value.instrumento?.publicId)
    }

    @Test
    fun `sem instrumentos nao inventa default e sinaliza ausencia de opcoes`() = runTest {
        val cenario = criarViewModel(instrumentos = emptyList(), contas = emptyList())
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertNull(estado.instrumento)
        assertTrue(estado.semInstrumentos)
        assertTrue(estado.instrumentos.isEmpty())
    }

    @Test
    fun `digitos formam centavos e apagar remove o ultimo`() = runTest {
        val cenario = criarViewModel()

        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(4))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(2))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(9))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(0))
        advanceUntilIdle()

        assertEquals(4290, cenario.viewModel.uiState.value.valorCentavos)

        cenario.viewModel.onEvent(NovoLancamentoEvent.ApagarPressionado)
        advanceUntilIdle()

        assertEquals(429, cenario.viewModel.uiState.value.valorCentavos)
    }

    @Test
    fun `estabelecimento digitado sugere a categoria da regra aprendida`() = runTest {
        val cenario = criarViewModel(regras = listOf(regraDe("padaria do zé", alimentacao)))

        cenario.viewModel.onEvent(NovoLancamentoEvent.EstabelecimentoAlterado("Padaria do Zé"))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertEquals("cat-alimentacao", estado.categoria?.publicId)
        assertTrue(estado.categoriaSugerida)
    }

    @Test
    fun `categoria escolhida pelo usuario nao e sobrescrita pela sugestao`() = runTest {
        val cenario = criarViewModel(regras = listOf(regraDe("padaria do zé", alimentacao)))

        cenario.viewModel.onEvent(NovoLancamentoEvent.EstabelecimentoAlterado("Padaria do Zé"))
        advanceUntilIdle()
        cenario.viewModel.onEvent(NovoLancamentoEvent.AbrirPainel(PainelDoNovoLancamento.CATEGORIA))
        cenario.viewModel.onEvent(NovoLancamentoEvent.SelecionarCategoria(mercado.publicId))
        cenario.viewModel.onEvent(NovoLancamentoEvent.EstabelecimentoAlterado("Padaria do Zé 2"))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertEquals("cat-mercado", estado.categoria?.publicId)
        assertFalse(estado.categoriaSugerida)
    }

    @Test
    fun `sugestao de despesa nao vale para receita`() = runTest {
        val cenario = criarViewModel(regras = listOf(regraDe("padaria do zé", alimentacao)))

        cenario.viewModel.onEvent(NovoLancamentoEvent.EstabelecimentoAlterado("Padaria do Zé"))
        cenario.viewModel.onEvent(NovoLancamentoEvent.TipoSelecionado(TipoDoLancamentoManual.RECEITA))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertNull(estado.categoria)
        assertEquals(listOf("Salário"), estado.categorias.map { it.nome })
    }

    @Test
    fun `trocar para transferencia esvazia categorias e mantem instrumentos`() = runTest {
        val cenario = criarViewModel(lancamentos = listOf(capturadoRecente))

        cenario.viewModel.onEvent(NovoLancamentoEvent.TipoSelecionado(TipoDoLancamentoManual.TRANSFERENCIA))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertTrue(estado.categorias.isEmpty())
        assertEquals(2, estado.instrumentos.size)
        assertEquals(instrumentoNubank.publicId, estado.instrumento?.publicId)
    }

    @Test
    fun `salvar com defaults grava despesa manual confirmada com data de hoje`() = runTest {
        val cenario = criarViewModel(lancamentos = listOf(capturadoRecente))
        advanceUntilIdle()

        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(4))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(2))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(9))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(0))
        cenario.viewModel.onEvent(NovoLancamentoEvent.Salvar)
        advanceUntilIdle()

        val salvo = cenario.lancamentos.observeLancamentos().first().single {
            it.valorCentavos == 4290L &&
                it.origem == OrigemLancamento.MANUAL
        }
        assertEquals(StatusLancamento.CONFIRMADO, salvo.status)
        assertEquals(TipoLancamento.DESPESA, salvo.tipo)
        assertEquals(instrumentoNubank.publicId, salvo.instrumentoPublicId)
        assertEquals(hoje, salvo.dataHora.atZone(ZoneOffset.UTC).toLocalDate())
        assertNull(salvo.motivoPendencia)
        assertTrue(cenario.viewModel.uiState.value.salvo)
        assertFalse(cenario.viewModel.uiState.value.salvando)
    }

    @Test
    fun `salvar sem valor mostra erro no valor e nao grava`() = runTest {
        val cenario = criarViewModel(lancamentos = listOf(capturadoRecente))
        advanceUntilIdle()

        cenario.viewModel.onEvent(NovoLancamentoEvent.Salvar)
        advanceUntilIdle()

        assertEquals(setOf(CampoDeLancamentoManual.VALOR), cenario.viewModel.uiState.value.erros)
        assertEquals(1, cenario.lancamentos.observeLancamentos().first().size)
        assertFalse(cenario.viewModel.uiState.value.salvo)
    }

    @Test
    fun `salvar sem instrumento escolhido mostra erro no instrumento`() = runTest {
        val cenario = criarViewModel(instrumentos = listOf(instrumentoNubank, instrumentoInter))
        advanceUntilIdle()

        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(1))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(0))
        cenario.viewModel.onEvent(NovoLancamentoEvent.Salvar)
        advanceUntilIdle()

        assertEquals(setOf(CampoDeLancamentoManual.INSTRUMENTO), cenario.viewModel.uiState.value.erros)
        assertTrue(cenario.lancamentos.observeLancamentos().first().isEmpty())
    }

    @Test
    fun `sem instrumentos salvamento fica bloqueado mesmo com valor valido`() = runTest {
        val cenario = criarViewModel(instrumentos = emptyList(), contas = emptyList())
        advanceUntilIdle()

        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(1))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(0))
        cenario.viewModel.onEvent(NovoLancamentoEvent.Salvar)
        advanceUntilIdle()

        assertEquals(setOf(CampoDeLancamentoManual.INSTRUMENTO), cenario.viewModel.uiState.value.erros)
        assertTrue(cenario.viewModel.uiState.value.semInstrumentos)
        assertTrue(cenario.lancamentos.observeLancamentos().first().isEmpty())
        assertTrue(cenario.instrumentos.observeInstrumentos().first().isEmpty())
    }

    @Test
    fun `transferencia com origem igual ao destino mostra erro e nao grava`() = runTest {
        val cenario = criarViewModel(lancamentos = listOf(capturadoRecente))
        advanceUntilIdle()

        cenario.viewModel.onEvent(NovoLancamentoEvent.TipoSelecionado(TipoDoLancamentoManual.TRANSFERENCIA))
        advanceUntilIdle()
        val origem = cenario.viewModel.uiState.value.instrumento
        cenario.viewModel.onEvent(NovoLancamentoEvent.AbrirPainel(PainelDoNovoLancamento.DESTINO))
        cenario.viewModel.onEvent(NovoLancamentoEvent.SelecionarInstrumento(origem!!.publicId))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(5))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(0))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(0))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(0))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(0))
        cenario.viewModel.onEvent(NovoLancamentoEvent.Salvar)
        advanceUntilIdle()

        assertEquals(setOf(CampoDeLancamentoManual.DESTINO), cenario.viewModel.uiState.value.erros)
        assertTrue(cenario.lancamentos.observeLancamentos().first().none { it.origem == OrigemLancamento.MANUAL })
    }

    @Test
    fun `transferencia valida grava o par marcado com o mesmo identificador`() = runTest {
        val cenario = criarViewModel(lancamentos = listOf(capturadoRecente))
        advanceUntilIdle()

        cenario.viewModel.onEvent(NovoLancamentoEvent.TipoSelecionado(TipoDoLancamentoManual.TRANSFERENCIA))
        advanceUntilIdle()
        cenario.viewModel.onEvent(NovoLancamentoEvent.AbrirPainel(PainelDoNovoLancamento.DESTINO))
        cenario.viewModel.onEvent(NovoLancamentoEvent.SelecionarInstrumento(instrumentoInter.publicId))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(5))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(0))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(0))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(0))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(0))
        cenario.viewModel.onEvent(NovoLancamentoEvent.Salvar)
        advanceUntilIdle()

        val par = cenario.lancamentos.observeLancamentos().first().filter { it.origem == OrigemLancamento.MANUAL }
        assertEquals(2, par.size)
        assertEquals(1, par.map { it.transferenciaId }.distinct().size)
        assertEquals(
            setOf(instrumentoNubank.publicId, instrumentoInter.publicId),
            par.map { it.instrumentoPublicId }.toSet(),
        )
        par.forEach { lancamento ->
            assertEquals(TipoLancamento.TRANSFERENCIA, lancamento.tipo)
            assertEquals(50000, lancamento.valorCentavos)
            assertEquals(StatusLancamento.CONFIRMADO, lancamento.status)
        }
        assertTrue(cenario.transacao.execucoes >= 1)
    }

    @Test
    fun `duplicata de capturado recente grava pendente possivel duplicata`() = runTest {
        val cenario = criarViewModel(lancamentos = listOf(capturadoDuplicado))
        advanceUntilIdle()

        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(4))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(2))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(9))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(0))
        cenario.viewModel.onEvent(NovoLancamentoEvent.Salvar)
        advanceUntilIdle()

        val manual = cenario.lancamentos.observeLancamentos().first().single { it.origem == OrigemLancamento.MANUAL }
        assertEquals(StatusLancamento.PENDENTE, manual.status)
        assertEquals(MotivoPendencia.POSSIVEL_DUPLICATA.name, manual.motivoPendencia)
        assertEquals(StatusLancamento.CONFIRMADO, cenario.lancamentos.buscar(capturadoDuplicado.publicId)?.status)
        assertEquals(capturadoDuplicado, cenario.lancamentos.buscar(capturadoDuplicado.publicId))
    }

    @Test
    fun `data escolhida no calendario entra no lancamento`() = runTest {
        val cenario = criarViewModel(lancamentos = listOf(capturadoRecente))
        advanceUntilIdle()

        cenario.viewModel.onEvent(NovoLancamentoEvent.AbrirCalendario)
        cenario.viewModel.onEvent(NovoLancamentoEvent.SelecionarData(LocalDate.of(2026, 11, 8)))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(1))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(0))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(0))
        cenario.viewModel.onEvent(NovoLancamentoEvent.Salvar)
        advanceUntilIdle()

        val manual = cenario.lancamentos.observeLancamentos().first().single { it.origem == OrigemLancamento.MANUAL }
        assertEquals(LocalDate.of(2026, 11, 8), manual.dataHora.atZone(ZoneOffset.UTC).toLocalDate())
        assertFalse(cenario.viewModel.uiState.value.calendarioVisivel)
    }

    @Test
    fun `seletores ignoram instrumento arquivado e instrumento de conta arquivada`() = runTest {
        val contaArquivada = contaInter.copy(arquivada = true)
        val instrumentoArquivado = instrumentoNubank.copy(
            publicId = "inst-arquivado",
            apelido = "Cartão antigo",
            arquivado = true,
        )
        val cenario = criarViewModel(
            contas = listOf(contaNubank, contaArquivada),
            instrumentos = listOf(instrumentoNubank, instrumentoInter, instrumentoArquivado),
            lancamentos = listOf(capturadoRecente),
        )
        advanceUntilIdle()

        assertEquals(listOf("inst-nubank"), cenario.viewModel.uiState.value.instrumentos.map { it.publicId })
        assertEquals("inst-nubank", cenario.viewModel.uiState.value.instrumento?.publicId)
    }

    @Test
    fun `segundo toque em salvar nao duplica o lancamento`() = runTest {
        val cenario = criarViewModel(lancamentos = listOf(capturadoRecente))
        advanceUntilIdle()

        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(7))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(5))
        cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(0))
        cenario.viewModel.onEvent(NovoLancamentoEvent.Salvar)
        advanceUntilIdle()
        cenario.viewModel.onEvent(NovoLancamentoEvent.Salvar)
        advanceUntilIdle()

        assertEquals(1, cenario.lancamentos.observeLancamentos().first().count { it.origem == OrigemLancamento.MANUAL })
    }

    @Test
    fun `compra parcelada retroativa no credito salva todas as parcelas e reseta se mudar para debito`() = runTest {
        val cenario = criarViewModel()
        advanceUntilIdle()

        cenario.viewModel.onEvent(NovoLancamentoEvent.SelecionarInstrumento("inst-nubank"))
        cenario.viewModel.onEvent(NovoLancamentoEvent.SelecionarParcelas(6))
        cenario.viewModel.onEvent(NovoLancamentoEvent.SelecionarModoParcelamento(ModoParcelamento.VALOR_PARCELA))
        cenario.viewModel.onEvent(NovoLancamentoEvent.SelecionarData(LocalDate.of(2026, 5, 10)))
        listOf(1, 5, 0, 0, 0).forEach { cenario.viewModel.onEvent(NovoLancamentoEvent.DigitoPressionado(it)) }
        advanceUntilIdle()

        assertEquals(6, cenario.viewModel.uiState.value.totalParcelas)
        assertEquals(15000L, cenario.viewModel.uiState.value.valorParcelaPreviewCentavos)
        assertEquals(90000L, cenario.viewModel.uiState.value.valorTotalPreviewCentavos)

        cenario.viewModel.onEvent(NovoLancamentoEvent.Salvar)
        advanceUntilIdle()

        val salvas = cenario.lancamentos.observeLancamentos().first().sortedBy { it.parcelaNumero }
        assertEquals(6, salvas.size)
        assertEquals(List(6) { 15000L }, salvas.map { it.valorCentavos })
        assertEquals(90000L, salvas.first().valorTotalCompraCentavos)
        assertEquals(LocalDate.of(2026, 5, 10), salvas.first().dataHora.atZone(clock.zone).toLocalDate())
        assertEquals(LocalDate.of(2026, 10, 10), salvas.last().dataHora.atZone(clock.zone).toLocalDate())
    }

    @Test
    fun `mudar instrumento para debito ou tipo para receita zera parcelamento`() = runTest {
        val cenario = criarViewModel()
        advanceUntilIdle()

        cenario.viewModel.onEvent(NovoLancamentoEvent.SelecionarInstrumento("inst-nubank"))
        cenario.viewModel.onEvent(NovoLancamentoEvent.SelecionarParcelas(5))
        advanceUntilIdle()
        assertEquals(5, cenario.viewModel.uiState.value.totalParcelas)

        cenario.viewModel.onEvent(NovoLancamentoEvent.SelecionarInstrumento("inst-inter"))
        advanceUntilIdle()
        assertEquals(1, cenario.viewModel.uiState.value.totalParcelas)
        assertFalse(cenario.viewModel.uiState.value.podeParcelar)

        cenario.viewModel.onEvent(NovoLancamentoEvent.SelecionarInstrumento("inst-nubank"))
        cenario.viewModel.onEvent(NovoLancamentoEvent.SelecionarParcelas(4))
        cenario.viewModel.onEvent(NovoLancamentoEvent.TipoSelecionado(TipoDoLancamentoManual.RECEITA))
        advanceUntilIdle()
        assertEquals(1, cenario.viewModel.uiState.value.totalParcelas)
    }

    @Test
    fun `sugere categoria automaticamente por padrao quando nao ha regra do usuario`() = runTest {
        val cenario = criarViewModel(
            categorias = listOf(alimentacao, mercado, salario),
            regras = emptyList(),
        )
        advanceUntilIdle()

        cenario.viewModel.onEvent(NovoLancamentoEvent.EstabelecimentoAlterado("IFOOD PEDIDO 998"))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertEquals(alimentacao.publicId, estado.categoria?.publicId)
        assertTrue(estado.categoriaSugerida)
    }

    private fun criarViewModel(
        contas: List<Conta> = listOf(contaNubank, contaInter),
        instrumentos: List<Instrumento> = listOf(instrumentoNubank, instrumentoInter),
        categorias: List<Categoria> = listOf(alimentacao, mercado, salario),
        lancamentos: List<Lancamento> = emptyList(),
        regras: List<RegraCategoria> = emptyList(),
    ): Cenario {
        val repositorioLancamentos = FakeLancamentoRepository(lancamentos)
        val repositorioInstrumentos = FakeInstrumentoRepository(instrumentos)
        val transacao = FakeTransacaoLocal()
        val viewModel = NovoLancamentoViewModel(
            contaRepository = FakeContaRepository(contas),
            instrumentoRepository = repositorioInstrumentos,
            categoriaRepository = FakeCategoriaRepository(categorias),
            regraCategoriaRepository = FakeRegraCategoriaRepository(regras),
            registrarLancamentoManual = RegistrarLancamentoManual(
                lancamentoRepository = repositorioLancamentos,
                instrumentoRepository = repositorioInstrumentos,
                transacaoLocal = transacao,
                clock = clock,
            ),
            clock = clock,
        )

        return Cenario(viewModel, repositorioLancamentos, repositorioInstrumentos, transacao)
    }

    private fun categoria(publicId: String, nome: String, tipo: TipoCategoria): Categoria = Categoria(
        publicId = publicId,
        nome = nome,
        tipo = tipo,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private fun regraDe(estabelecimento: String, categoria: Categoria): RegraCategoria = RegraCategoria(
        publicId = "regra-$estabelecimento",
        categoriaPublicId = categoria.publicId,
        estabelecimentoNormalizado = estabelecimento,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private fun lancamento(publicId: String, dataHora: Instant, valorCentavos: Long, instrumentoPublicId: String?): Lancamento = Lancamento(
        publicId = publicId,
        instrumentoPublicId = instrumentoPublicId,
        valorCentavos = valorCentavos,
        dataHora = dataHora,
        tipo = TipoLancamento.DESPESA,
        estabelecimento = "Estabelecimento",
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.NOTIFICACAO,
        confianca = 1.0,
        criadoEm = dataHora,
        atualizadoEm = dataHora,
    )

    private data class Cenario(
        val viewModel: NovoLancamentoViewModel,
        val lancamentos: FakeLancamentoRepository,
        val instrumentos: FakeInstrumentoRepository,
        val transacao: FakeTransacaoLocal,
    )
}
