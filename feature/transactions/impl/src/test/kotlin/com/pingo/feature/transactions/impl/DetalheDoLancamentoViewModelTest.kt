package com.pingo.feature.transactions.impl

import androidx.lifecycle.SavedStateHandle
import com.pingo.core.auth.StoredTokens
import com.pingo.core.auth.TokenStore
import com.pingo.core.auth.UsuarioAutenticado
import com.pingo.core.data.categoria.CategoriaAprendida
import com.pingo.core.data.detalhe.CampoDoDetalhe
import com.pingo.core.data.detalhe.EditarLancamento
import com.pingo.core.data.detalhe.MarcarTransferencia
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.testing.TestDispatcherRule
import com.pingo.feature.transactions.api.TransactionsRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class DetalheDoLancamentoViewModelTest {
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

    private val capturado = lancamentoDeTeste(
        publicId = "capturado-padaria",
        dataHora = agora.minus(Duration.ofHours(2)),
        valorCentavos = 4290,
        instrumentoPublicId = instrumentoNubank.publicId,
        categoriaPublicId = alimentacao.publicId,
        estabelecimento = "Padaria do Zé",
        instituicaoOrigem = InstituicaoFinanceira.NUBANK,
        textoBruto = "Padaria do Zé R$ 42,90 aprovado no Nubank",
    )

    @Test
    fun `abre com os campos editaveis e a origem capturada`() = runTest {
        val cenario = criarViewModel(lancamentos = listOf(capturado))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertFalse(estado.carregando)
        assertFalse(estado.naoEncontrado)
        assertFalse(estado.ehTransferencia)
        assertEquals(TipoDoLancamentoManual.DESPESA, estado.tipo)
        assertEquals(4290L, estado.valorCentavos)
        assertEquals(instrumentoNubank.publicId, estado.instrumento?.publicId)
        assertEquals(alimentacao.publicId, estado.categoria?.publicId)
        assertEquals("Padaria do Zé", estado.estabelecimento)
        assertEquals(hoje, estado.hoje)
        assertEquals(listOf("Alimentação", "Mercado"), estado.categorias.map { it.nome })

        val origem = estado.origem as OrigemDoLancamentoUi.Capturado
        assertEquals(InstituicaoFinanceira.NUBANK, origem.instituicao)
        assertEquals("Padaria do Zé R$ 42,90 aprovado no Nubank", origem.textoBruto)
        assertEquals(capturado.criadoEm, origem.lidoEm)
        assertNull(estado.par)
    }

    @Test
    fun `lancamento manual abre com a origem manual`() = runTest {
        val manual = lancamentoDeTeste(
            publicId = "manual-mercado",
            dataHora = agora.minus(Duration.ofDays(1)),
            valorCentavos = 7500,
            origem = OrigemLancamento.MANUAL,
            instrumentoPublicId = instrumentoInter.publicId,
            estabelecimento = "Mercado",
        )

        val cenario = criarViewModel(publicId = manual.publicId, lancamentos = listOf(manual))
        advanceUntilIdle()

        val origem = cenario.viewModel.uiState.value.origem as OrigemDoLancamentoUi.Manual
        assertEquals(manual.criadoEm, origem.registradoEm)
    }

    @Test
    fun `restauracao sem texto bruto mostra indisponibilidade sem inventar origem`() = runTest {
        val restaurado = lancamentoDeTeste(
            publicId = "restaurado",
            dataHora = agora.minus(Duration.ofDays(3)),
            valorCentavos = 1890,
            instrumentoPublicId = instrumentoNubank.publicId,
            instituicaoOrigem = null,
            textoBruto = null,
        )

        val cenario = criarViewModel(publicId = restaurado.publicId, lancamentos = listOf(restaurado))
        advanceUntilIdle()

        val origem = cenario.viewModel.uiState.value.origem as OrigemDoLancamentoUi.Capturado
        assertNull(origem.instituicao)
        assertNull(origem.textoBruto)
    }

    @Test
    fun `captura pendente sem instrumento nao autoescolhe cartao`() = runTest {
        val pendente = lancamentoDeTeste(
            publicId = "pendente",
            dataHora = agora.minus(Duration.ofMinutes(30)),
            valorCentavos = 1290,
            status = StatusLancamento.PENDENTE,
            instrumentoPublicId = null,
            instituicaoOrigem = InstituicaoFinanceira.INTER,
            motivoPendencia = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name,
        )

        val cenario = criarViewModel(publicId = pendente.publicId, lancamentos = listOf(pendente))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertNull(estado.instrumento)
        assertTrue(estado.erros.isEmpty())
        assertTrue(estado.instrumentos.isNotEmpty())
    }

    @Test
    fun `salvar captura pendente sem instrumento mantem a pendencia sem inventar cartao`() = runTest {
        val pendente = lancamentoDeTeste(
            publicId = "pendente",
            dataHora = agora.minus(Duration.ofMinutes(30)),
            valorCentavos = 1290,
            status = StatusLancamento.PENDENTE,
            instrumentoPublicId = null,
            instituicaoOrigem = InstituicaoFinanceira.INTER,
            motivoPendencia = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name,
        )

        val cenario = criarViewModel(publicId = pendente.publicId, lancamentos = listOf(pendente))
        advanceUntilIdle()

        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.Salvar)
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertTrue(estado.salvo)
        assertNull(estado.instrumento)

        val persistido = cenario.lancamentos.buscar(pendente.publicId)
        assertNull(persistido?.instrumentoPublicId)
        assertEquals(StatusLancamento.PENDENTE, persistido?.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, persistido?.motivoPendencia)
    }

    @Test
    fun `escolher o instrumento resolve a pendencia e salva confirmado`() = runTest {
        val pendente = lancamentoDeTeste(
            publicId = "pendente",
            dataHora = agora.minus(Duration.ofMinutes(30)),
            valorCentavos = 1290,
            status = StatusLancamento.PENDENTE,
            instrumentoPublicId = null,
            categoriaPublicId = alimentacao.publicId,
            instituicaoOrigem = InstituicaoFinanceira.INTER,
            motivoPendencia = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name,
        )

        val cenario = criarViewModel(publicId = pendente.publicId, lancamentos = listOf(pendente))
        advanceUntilIdle()

        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.AbrirPainel(PainelDoDetalhe.INSTRUMENTO))
        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.SelecionarInstrumento(instrumentoInter.publicId))
        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.Salvar)
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertTrue(estado.erros.isEmpty())
        assertTrue(estado.salvo)
        assertEquals(instrumentoInter.publicId, estado.instrumento?.publicId)

        val persistido = cenario.lancamentos.buscar(pendente.publicId)
        assertNotNull(persistido)
        assertEquals(instrumentoInter.publicId, persistido?.instrumentoPublicId)
        assertEquals(StatusLancamento.CONFIRMADO, persistido?.status)
        assertNull(persistido?.motivoPendencia)
    }

    @Test
    fun `editar valor e estabelecimento persiste e reflete na lista`() = runTest {
        val cenario = criarViewModel(lancamentos = listOf(capturado))
        advanceUntilIdle()

        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.ApagarPressionado)
        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.DigitoPressionado(5))
        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.EstabelecimentoAlterado("Padaria do Zé Centro"))
        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.AbrirPainel(PainelDoDetalhe.CATEGORIA))
        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.SelecionarCategoria(mercado.publicId))
        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.Salvar)
        advanceUntilIdle()

        val daLista = cenario.lancamentos.observeLancamentos().first().single { it.publicId == capturado.publicId }
        assertEquals(4295L, daLista.valorCentavos)
        assertEquals("Padaria do Zé Centro", daLista.estabelecimento)
        assertEquals(mercado.publicId, daLista.categoriaPublicId)
        assertTrue(cenario.viewModel.uiState.value.salvo)
    }

    @Test
    fun `valor zerado impede salvar`() = runTest {
        val cenario = criarViewModel(lancamentos = listOf(capturado))
        advanceUntilIdle()

        repeat(4) { cenario.viewModel.onEvent(DetalheDoLancamentoEvent.ApagarPressionado) }
        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.Salvar)
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertEquals(0L, estado.valorCentavos)
        assertEquals(setOf(CampoDoDetalhe.VALOR), estado.erros)
        assertFalse(estado.salvo)
        assertEquals(4290L, cenario.lancamentos.buscar(capturado.publicId)?.valorCentavos)
    }

    @Test
    fun `aplicar categoria a todos grava a regra do estabelecimento`() = runTest {
        val cenario = criarViewModel(lancamentos = listOf(capturado))
        advanceUntilIdle()

        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.AplicarATodosAlterado(true))
        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.AbrirPainel(PainelDoDetalhe.CATEGORIA))
        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.SelecionarCategoria(mercado.publicId))
        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.Salvar)
        advanceUntilIdle()

        val regra = cenario.regras.buscarPorEstabelecimento("padaria do zé")
        assertNotNull(regra)
        assertEquals(mercado.publicId, regra?.categoriaPublicId)
        assertTrue(cenario.viewModel.uiState.value.salvo)
    }

    @Test
    fun `excluir exige confirmacao e remove o lancamento da lista`() = runTest {
        val cenario = criarViewModel(lancamentos = listOf(capturado))
        advanceUntilIdle()

        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.SolicitarExclusao)
        advanceUntilIdle()

        assertTrue(cenario.viewModel.uiState.value.dialogoDeExclusao)
        assertNull(cenario.lancamentos.buscar(capturado.publicId)?.excluidoEm)

        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.ConfirmarExclusao)
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertFalse(estado.dialogoDeExclusao)
        assertTrue(estado.excluido)
        assertEquals(agora, cenario.lancamentos.buscar(capturado.publicId)?.excluidoEm)
        assertTrue(cenario.lancamentos.observeLancamentos().first().none { it.publicId == capturado.publicId })
    }

    @Test
    fun `cancelar a exclusao mantem o lancamento`() = runTest {
        val cenario = criarViewModel(lancamentos = listOf(capturado))
        advanceUntilIdle()

        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.SolicitarExclusao)
        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.CancelarExclusao)
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertFalse(estado.dialogoDeExclusao)
        assertFalse(estado.excluido)
        assertNull(cenario.lancamentos.buscar(capturado.publicId)?.excluidoEm)
    }

    @Test
    fun `marcar transferencia sem instrumento resolvido avisa e nao cria par`() = runTest {
        val pendente = lancamentoDeTeste(
            publicId = "pendente",
            dataHora = agora.minus(Duration.ofMinutes(30)),
            valorCentavos = 1290,
            status = StatusLancamento.PENDENTE,
            instrumentoPublicId = null,
            instituicaoOrigem = InstituicaoFinanceira.INTER,
            motivoPendencia = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name,
        )

        val cenario = criarViewModel(publicId = pendente.publicId, lancamentos = listOf(pendente))
        advanceUntilIdle()

        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.SolicitarTransferencia)
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertEquals(MensagemDoDetalhe.INSTRUMENTO_PARA_TRANSFERENCIA, estado.mensagem)
        assertNull(estado.painel)
        assertEquals(1, cenario.lancamentos.observeLancamentos().first().size)
    }

    @Test
    fun `marcar transferencia vincula as duas contas sem duplicar`() = runTest {
        val cenario = criarViewModel(lancamentos = listOf(capturado))
        advanceUntilIdle()

        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.SolicitarTransferencia)
        advanceUntilIdle()

        assertEquals(PainelDoDetalhe.MARCAR_TRANSFERENCIA, cenario.viewModel.uiState.value.painel)
        assertEquals(listOf(instrumentoInter.publicId), cenario.viewModel.uiState.value.destinosDaTransferencia.map { it.publicId })

        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.SelecionarDestino(instrumentoInter.publicId))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertTrue(estado.ehTransferencia)
        assertEquals(TipoDoLancamentoManual.TRANSFERENCIA, estado.tipo)
        assertEquals(instrumentoInter.publicId, estado.destino?.publicId)
        assertNotNull(estado.par)

        val comVinculo = cenario.lancamentos.observeLancamentos().first().filter { it.transferenciaId != null }
        assertEquals(2, comVinculo.size)
        assertEquals(1, comVinculo.mapNotNull { it.transferenciaId }.distinct().size)
        assertEquals(
            setOf(instrumentoNubank.publicId, instrumentoInter.publicId),
            comVinculo.mapNotNull { it.instrumentoPublicId }.toSet(),
        )
    }

    @Test
    fun `desfazer transferencia remove o vinculo dos dois lados`() = runTest {
        val saida = lancamentoDeTeste(
            publicId = "saida",
            dataHora = capturado.dataHora,
            valorCentavos = 5000,
            tipo = TipoLancamento.TRANSFERENCIA,
            instrumentoPublicId = instrumentoNubank.publicId,
            categoriaPublicId = alimentacao.publicId,
            estabelecimento = null,
            transferenciaId = "tr-1",
        )
        val entrada = lancamentoDeTeste(
            publicId = "entrada",
            dataHora = capturado.dataHora,
            valorCentavos = 5000,
            tipo = TipoLancamento.TRANSFERENCIA,
            instrumentoPublicId = instrumentoInter.publicId,
            estabelecimento = null,
            transferenciaId = "tr-1",
        )

        val cenario = criarViewModel(publicId = saida.publicId, lancamentos = listOf(saida, entrada))
        advanceUntilIdle()

        assertTrue(cenario.viewModel.uiState.value.ehTransferencia)
        assertEquals("entrada", cenario.viewModel.uiState.value.par?.publicId)

        cenario.viewModel.onEvent(DetalheDoLancamentoEvent.DesfazerTransferencia)
        advanceUntilIdle()

        assertNull(cenario.lancamentos.buscar(saida.publicId)?.transferenciaId)
        assertNull(cenario.lancamentos.buscar(entrada.publicId)?.transferenciaId)
        assertFalse(cenario.viewModel.uiState.value.ehTransferencia)
        assertNull(cenario.viewModel.uiState.value.par)
        assertEquals(2, cenario.lancamentos.observeLancamentos().first().size)
    }

    @Test
    fun `lancamento inexistente abre como nao encontrado`() = runTest {
        val cenario = criarViewModel(publicId = "nao-existe", lancamentos = listOf(capturado))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertTrue(estado.naoEncontrado)
        assertFalse(estado.carregando)
        assertFalse(estado.podeSalvar)
    }

    private fun criarViewModel(
        publicId: String = capturado.publicId,
        contas: List<Conta> = listOf(contaNubank, contaInter),
        instrumentos: List<Instrumento> = listOf(instrumentoNubank, instrumentoInter),
        categorias: List<Categoria> = listOf(alimentacao, mercado, salario),
        lancamentos: List<Lancamento> = emptyList(),
    ): Cenario {
        val repositorioLancamentos = FakeLancamentoRepository(lancamentos)
        val repositorioInstrumentos = FakeInstrumentoRepository(instrumentos)
        val repositorioCategorias = FakeCategoriaRepository(categorias)
        val repositorioRegras = FakeRegraCategoriaRepository()
        val transacao = FakeTransacaoLocal()
        val usuario = UsuarioAutenticado(FakeTokenStore(tokensDoUsuario("auth0|lucas")))
        val aprendida = CategoriaAprendida(repositorioCategorias, repositorioRegras, usuario)

        val viewModel = DetalheDoLancamentoViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TransactionsRoute.ARG_PUBLIC_ID to publicId)),
            lancamentoRepository = repositorioLancamentos,
            contaRepository = FakeContaRepository(contas),
            instrumentoRepository = repositorioInstrumentos,
            categoriaRepository = repositorioCategorias,
            editarLancamento = EditarLancamento(
                lancamentoRepository = repositorioLancamentos,
                instrumentoRepository = repositorioInstrumentos,
                categoriaRepository = repositorioCategorias,
                categoriaAprendida = aprendida,
                transacaoLocal = transacao,
                clock = clock,
            ),
            marcarTransferencia = MarcarTransferencia(
                lancamentoRepository = repositorioLancamentos,
                instrumentoRepository = repositorioInstrumentos,
                categoriaRepository = repositorioCategorias,
                transacaoLocal = transacao,
                clock = clock,
            ),
            clock = clock,
        )

        return Cenario(viewModel, repositorioLancamentos, repositorioRegras)
    }

    private fun categoria(publicId: String, nome: String, tipo: TipoCategoria): Categoria = Categoria(
        publicId = publicId,
        nome = nome,
        tipo = tipo,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private data class Cenario(
        val viewModel: DetalheDoLancamentoViewModel,
        val lancamentos: FakeLancamentoRepository,
        val regras: FakeRegraCategoriaRepository,
    )

    private class FakeTokenStore(private var tokens: StoredTokens?) : TokenStore {
        override suspend fun read(): StoredTokens? = tokens

        override suspend fun write(tokens: StoredTokens) {
            this.tokens = tokens
        }

        override suspend fun clear() {
            tokens = null
        }
    }

    private fun tokensDoUsuario(sub: String): StoredTokens =
        StoredTokens(accessToken = jwtComPayload("""{"sub":"$sub"}"""), refreshToken = null, expiresAtEpochMillis = Long.MAX_VALUE)

    private fun jwtComPayload(payload: String): String {
        val codificador = Base64.getUrlEncoder().withoutPadding()
        val cabecalho = codificador.encodeToString("""{"alg":"RS256"}""".toByteArray())

        return listOf(cabecalho, codificador.encodeToString(payload.toByteArray()), "assinatura").joinToString(".")
    }
}
