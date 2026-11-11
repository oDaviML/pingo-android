package com.pingo.feature.review.impl

import androidx.lifecycle.ViewModel
import com.pingo.core.auth.UsuarioAutenticado
import com.pingo.core.data.categoria.CategoriaAprendida
import com.pingo.core.data.detalhe.EditarLancamento
import com.pingo.core.data.detalhe.MarcarTransferencia
import com.pingo.core.data.revisao.ConfirmarPendencias
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.testing.TestDispatcherRule
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
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val agora: Instant = Instant.parse("2026-11-10T15:30:00Z")
    private val clock = Clock.fixed(agora, ZoneOffset.UTC)

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

    private val padaria = pendenciaDeTeste(
        publicId = "padaria",
        dataHora = agora.minus(Duration.ofHours(2)),
        instrumentoPublicId = instrumentoNubank.publicId,
        categoriaPublicId = alimentacao.publicId,
        estabelecimento = "Padaria do Zé",
        instituicaoOrigem = InstituicaoFinanceira.NUBANK,
        textoBruto = "Compra aprovada R$ 42,90 PADARIA DO ZE",
        motivoPendencia = MotivoPendencia.BAIXA_CONFIANCA.name,
        confianca = 0.62,
    )
    private val semInstrumento = pendenciaDeTeste(
        publicId = "sem-instrumento",
        dataHora = agora.minus(Duration.ofHours(1)),
        valorCentavos = 1990,
        instrumentoPublicId = null,
        estabelecimento = "ZPT*LOJA2231",
        motivoPendencia = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name,
        textoBruto = null,
    )
    private val duplicata = pendenciaDeTeste(
        publicId = "duplicata",
        dataHora = agora.minus(Duration.ofHours(3)),
        valorCentavos = 8000,
        instrumentoPublicId = instrumentoNubank.publicId,
        estabelecimento = "Mercado do Bairro",
        motivoPendencia = MotivoPendencia.POSSIVEL_DUPLICATA.name,
        textoBruto = "Pix enviado R$ 80,00 para Mercado do Bairro",
    )
    private val confirmado = pendenciaDeTeste(
        publicId = "confirmado",
        dataHora = agora.minus(Duration.ofHours(4)),
        instrumentoPublicId = instrumentoNubank.publicId,
        status = StatusLancamento.CONFIRMADO,
        textoBruto = "Compra aprovada R$ 10,00",
    )

    @Test
    fun `lista somente pendentes com motivo trecho horario e vinculo`() = runTest {
        val cenario = criarViewModel(listOf(padaria, semInstrumento, duplicata, confirmado))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertFalse(estado.carregando)
        assertFalse(estado.vazia)
        assertEquals(3, estado.quantidade)
        assertEquals(listOf("sem-instrumento", "padaria", "duplicata"), estado.pendentes.map { it.publicId })

        val padariaUi = estado.pendentes.first { it.publicId == "padaria" }
        assertEquals(MotivoDaRevisao.BAIXA_CONFIANCA, padariaUi.motivo)
        assertEquals("Baixa confiança · 62%", RotulosDaRevisao.motivo(padariaUi))
        assertEquals("Padaria do Zé", padariaUi.estabelecimento)
        assertEquals(4290L, padariaUi.valorCentavos)
        assertEquals("Nubank · crédito", padariaUi.vinculo)
        assertEquals("hoje, 13:30", padariaUi.horario)
        assertEquals("Compra aprovada R$ 42,90 PADARIA DO ZE", padariaUi.textoInterpretado)
        assertEquals("Alimentação", padariaUi.categoriaNome)
        assertTrue(padariaUi.podeConfirmar)

        val semInstrumentoUi = estado.pendentes.first { it.publicId == "sem-instrumento" }
        assertEquals(MotivoDaRevisao.INSTRUMENTO_NAO_ENCONTRADO, semInstrumentoUi.motivo)
        assertNull(semInstrumentoUi.textoInterpretado)
        assertFalse(semInstrumentoUi.podeConfirmar)
        assertEquals("Instrumento não identificado", semInstrumentoUi.vinculo)

        val duplicataUi = estado.pendentes.first { it.publicId == "duplicata" }
        assertEquals(MotivoDaRevisao.POSSIVEL_DUPLICATA, duplicataUi.motivo)
        assertEquals("Possível duplicata", RotulosDaRevisao.motivo(duplicataUi))
    }

    @Test
    fun `mapeia estabelecimento novo e moeda estrangeira para os rotulos do contrato`() = runTest {
        val novo = pendenciaDeTeste(
            publicId = "novo",
            dataHora = agora,
            motivoPendencia = MotivoPendencia.CATEGORIA_DESCONHECIDA.name,
            estabelecimento = "ZPT*LOJA2231",
        )
        val estrangeira = pendenciaDeTeste(
            publicId = "dolar",
            dataHora = agora.minusSeconds(60),
            motivoPendencia = MotivoPendencia.MOEDA_ESTRANGEIRA.name,
            estabelecimento = "Amazon US",
        )
        val cenario = criarViewModel(listOf(novo, estrangeira))
        advanceUntilIdle()

        val itens = cenario.viewModel.uiState.value.pendentes
        assertEquals("Estabelecimento novo", RotulosDaRevisao.motivo(itens.first { it.publicId == "novo" }))
        assertEquals("Moeda estrangeira", RotulosDaRevisao.motivo(itens.first { it.publicId == "dolar" }))
    }

    @Test
    fun `trunca o trecho interpretado longo`() = runTest {
        val texto = "Compra aprovada " + "x".repeat(200)
        val longo = pendenciaDeTeste(publicId = "longo", dataHora = agora, textoBruto = texto, instrumentoPublicId = null)
        val cenario = criarViewModel(listOf(longo))
        advanceUntilIdle()

        val trecho = cenario.viewModel.uiState.value.pendentes.single().textoInterpretado
        assertNotNull(trecho)
        assertEquals(161, trecho?.length)
        assertTrue(trecho?.endsWith("…") == true)
    }

    @Test
    fun `confirmar pendente elegivel remove da lista`() = runTest {
        val cenario = criarViewModel(listOf(padaria, semInstrumento))
        advanceUntilIdle()

        cenario.viewModel.onEvent(ReviewEvent.Confirmar("padaria"))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertEquals(listOf("sem-instrumento"), estado.pendentes.map { it.publicId })
        assertEquals(StatusLancamento.CONFIRMADO, cenario.lancamentos.atual("padaria")?.status)
        assertNull(cenario.lancamentos.atual("padaria")?.motivoPendencia)
        assertNull(estado.mensagem)
    }

    @Test
    fun `confirmar pendente sem instrumento mantem na lista e orienta`() = runTest {
        val cenario = criarViewModel(listOf(padaria, semInstrumento))
        advanceUntilIdle()

        cenario.viewModel.onEvent(ReviewEvent.Confirmar("sem-instrumento"))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertTrue(estado.pendentes.any { it.publicId == "sem-instrumento" })
        assertEquals(StatusLancamento.PENDENTE, cenario.lancamentos.atual("sem-instrumento")?.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, cenario.lancamentos.atual("sem-instrumento")?.motivoPendencia)
        assertEquals(MensagemDaRevisao.InstrumentoNecessario, estado.mensagem)
    }

    @Test
    fun `descartar remove o pendente nulo sem afetar os demais`() = runTest {
        val cenario = criarViewModel(listOf(padaria, semInstrumento))
        advanceUntilIdle()

        cenario.viewModel.onEvent(ReviewEvent.Descartar("sem-instrumento"))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertEquals(listOf("padaria"), estado.pendentes.map { it.publicId })
        assertNotNull(cenario.lancamentos.atual("sem-instrumento")?.excluidoEm)
        assertNull(cenario.lancamentos.atual("padaria")?.excluidoEm)
    }

    @Test
    fun `confirmar todos em lote misto confirma elegiveis e mantem irresolvidos`() = runTest {
        val cenario = criarViewModel(listOf(padaria, semInstrumento, duplicata))
        advanceUntilIdle()

        cenario.viewModel.onEvent(ReviewEvent.ConfirmarTodos)
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertEquals(listOf("sem-instrumento"), estado.pendentes.map { it.publicId })
        assertEquals(StatusLancamento.CONFIRMADO, cenario.lancamentos.atual("padaria")?.status)
        assertEquals(StatusLancamento.CONFIRMADO, cenario.lancamentos.atual("duplicata")?.status)
        assertEquals(StatusLancamento.PENDENTE, cenario.lancamentos.atual("sem-instrumento")?.status)
        assertEquals(MensagemDaRevisao.LoteComPendentes(1), estado.mensagem)
    }

    @Test
    fun `confirmar todos com todos elegiveis esvazia a lista`() = runTest {
        val cenario = criarViewModel(listOf(padaria, duplicata))
        advanceUntilIdle()

        cenario.viewModel.onEvent(ReviewEvent.ConfirmarTodos)
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertTrue(estado.vazia)
        assertNull(estado.mensagem)
        assertEquals(StatusLancamento.CONFIRMADO, cenario.lancamentos.atual("padaria")?.status)
        assertEquals(StatusLancamento.CONFIRMADO, cenario.lancamentos.atual("duplicata")?.status)
    }

    @Test
    fun `confirmar todos somente de irresolvidos nao confirma nenhum`() = runTest {
        val cenario = criarViewModel(listOf(semInstrumento))
        advanceUntilIdle()

        cenario.viewModel.onEvent(ReviewEvent.ConfirmarTodos)
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertEquals(1, estado.quantidade)
        assertEquals(StatusLancamento.PENDENTE, cenario.lancamentos.atual("sem-instrumento")?.status)
        assertEquals(MensagemDaRevisao.InstrumentoNecessario, estado.mensagem)
    }

    @Test
    fun `trocar categoria com sempre usar aplica e grava a regra`() = runTest {
        val cenario = criarViewModel(listOf(padaria))
        advanceUntilIdle()

        cenario.viewModel.onEvent(ReviewEvent.AbrirTrocaDeCategoria("padaria"))
        advanceUntilIdle()
        val painel = cenario.viewModel.uiState.value.painelDeCategoria
        assertNotNull(painel)
        assertEquals(listOf("Alimentação", "Mercado"), painel?.opcoes?.map { it.nome })

        cenario.viewModel.onEvent(ReviewEvent.SelecionarCategoria(mercado.publicId))
        cenario.viewModel.onEvent(ReviewEvent.AlternarAplicarATodos)
        cenario.viewModel.onEvent(ReviewEvent.AplicarCategoria)
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertNull(estado.painelDeCategoria)
        val persistido = cenario.lancamentos.atual("padaria")
        assertEquals(mercado.publicId, persistido?.categoriaPublicId)
        assertEquals(StatusLancamento.CONFIRMADO, persistido?.status)
        assertNull(persistido?.motivoPendencia)
        assertTrue(estado.pendentes.isEmpty())

        val regra = cenario.regras.todas().single()
        assertEquals("padaria do zé", regra.estabelecimentoNormalizado)
        assertEquals(mercado.publicId, regra.categoriaPublicId)
    }

    @Test
    fun `trocar categoria sem sempre usar nao grava a regra`() = runTest {
        val cenario = criarViewModel(listOf(padaria))
        advanceUntilIdle()

        cenario.viewModel.onEvent(ReviewEvent.AbrirTrocaDeCategoria("padaria"))
        cenario.viewModel.onEvent(ReviewEvent.SelecionarCategoria(mercado.publicId))
        cenario.viewModel.onEvent(ReviewEvent.AplicarCategoria)
        advanceUntilIdle()

        assertEquals(mercado.publicId, cenario.lancamentos.atual("padaria")?.categoriaPublicId)
        assertTrue(cenario.regras.todas().isEmpty())
    }

    @Test
    fun `trocar categoria de pendente sem instrumento mantem pendente`() = runTest {
        val cenario = criarViewModel(listOf(semInstrumento))
        advanceUntilIdle()

        cenario.viewModel.onEvent(ReviewEvent.AbrirTrocaDeCategoria("sem-instrumento"))
        cenario.viewModel.onEvent(ReviewEvent.SelecionarCategoria(mercado.publicId))
        cenario.viewModel.onEvent(ReviewEvent.AplicarCategoria)
        advanceUntilIdle()

        val persistido = cenario.lancamentos.atual("sem-instrumento")
        assertEquals(mercado.publicId, persistido?.categoriaPublicId)
        assertEquals(StatusLancamento.PENDENTE, persistido?.status)
        assertEquals(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name, persistido?.motivoPendencia)
    }

    @Test
    fun `troca de categoria oferece apenas categorias do tipo do lancamento`() = runTest {
        val receita = pendenciaDeTeste(
            publicId = "receita",
            dataHora = agora,
            tipo = TipoLancamento.RECEITA,
            motivoPendencia = MotivoPendencia.CATEGORIA_DESCONHECIDA.name,
        )
        val cenario = criarViewModel(listOf(receita))
        advanceUntilIdle()

        cenario.viewModel.onEvent(ReviewEvent.AbrirTrocaDeCategoria("receita"))
        advanceUntilIdle()

        assertEquals(listOf("Salário"), cenario.viewModel.uiState.value.painelDeCategoria?.opcoes?.map { it.nome })
    }

    @Test
    fun `marcar transferencia resolve o pendente e cria o par`() = runTest {
        val ambigua = pendenciaDeTeste(
            publicId = "ambigua",
            dataHora = agora.minus(Duration.ofMinutes(5)),
            valorCentavos = 15000,
            instrumentoPublicId = instrumentoNubank.publicId,
            estabelecimento = "Conta própria",
            motivoPendencia = MotivoPendencia.TRANSFERENCIA_AMBIGUA.name,
            textoBruto = "Pix enviado R$ 150,00",
        )
        val cenario = criarViewModel(listOf(ambigua))
        advanceUntilIdle()

        cenario.viewModel.onEvent(ReviewEvent.AbrirMarcarTransferencia("ambigua"))
        advanceUntilIdle()
        val painel = cenario.viewModel.uiState.value.painelDeTransferencia
        assertNotNull(painel)
        assertEquals(listOf(instrumentoInter.publicId), painel?.opcoes?.map { it.publicId })

        cenario.viewModel.onEvent(ReviewEvent.SelecionarDestino(instrumentoInter.publicId))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertNull(estado.painelDeTransferencia)
        assertTrue(estado.pendentes.isEmpty())
        val saida = cenario.lancamentos.atual("ambigua")
        assertEquals(TipoLancamento.TRANSFERENCIA, saida?.tipo)
        assertEquals(StatusLancamento.CONFIRMADO, saida?.status)
        assertNotNull(saida?.transferenciaId)
        val par = cenario.lancamentos.todos().firstOrNull { it.publicId != "ambigua" }
        assertEquals(saida?.transferenciaId, par?.transferenciaId)
        assertEquals(instrumentoInter.publicId, par?.instrumentoPublicId)
    }

    @Test
    fun `marcar transferencia de pendente sem instrumento mantem pendente e orienta`() = runTest {
        val cenario = criarViewModel(listOf(semInstrumento))
        advanceUntilIdle()

        cenario.viewModel.onEvent(ReviewEvent.AbrirMarcarTransferencia("sem-instrumento"))
        advanceUntilIdle()
        cenario.viewModel.onEvent(ReviewEvent.SelecionarDestino(instrumentoInter.publicId))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertTrue(estado.pendentes.any { it.publicId == "sem-instrumento" })
        assertEquals(MensagemDaRevisao.TransferenciaBloqueada, estado.mensagem)
        val persistido = cenario.lancamentos.atual("sem-instrumento")
        assertEquals(StatusLancamento.PENDENTE, persistido?.status)
        assertNull(persistido?.transferenciaId)
        assertTrue(cenario.lancamentos.todos().size == 1)
    }

    @Test
    fun `dispensar mensagem limpa o aviso`() = runTest {
        val cenario = criarViewModel(listOf(semInstrumento))
        advanceUntilIdle()

        cenario.viewModel.onEvent(ReviewEvent.ConfirmarTodos)
        advanceUntilIdle()
        assertNotNull(cenario.viewModel.uiState.value.mensagem)

        cenario.viewModel.onEvent(ReviewEvent.DispensarMensagem)
        advanceUntilIdle()

        assertNull(cenario.viewModel.uiState.value.mensagem)
    }

    @Test
    fun `estado vazio quando nao ha pendentes`() = runTest {
        val cenario = criarViewModel(listOf(confirmado))
        advanceUntilIdle()

        val estado = cenario.viewModel.uiState.value
        assertTrue(estado.vazia)
        assertEquals(0, estado.quantidade)
        assertFalse(estado.podeConfirmarTodos)
    }

    private fun criarViewModel(lancamentos: List<Lancamento>): Cenario {
        val repositorioLancamentos = FakeLancamentoRepository(lancamentos)
        val repositorioInstrumentos = FakeInstrumentoRepository(listOf(instrumentoNubank, instrumentoInter))
        val repositorioCategorias = FakeCategoriaRepository(listOf(alimentacao, mercado, salario))
        val repositorioRegras = FakeRegraCategoriaRepository()
        val transacao = FakeTransacaoLocal()
        val aprendida = CategoriaAprendida(
            categoriaRepository = repositorioCategorias,
            regraRepository = repositorioRegras,
            usuario = UsuarioAutenticado(FakeTokenStore(tokensDoUsuario("auth0|lucas"))),
        )

        val viewModel = ReviewViewModel(
            lancamentoRepository = repositorioLancamentos,
            contaRepository = FakeContaRepository(listOf(contaNubank, contaInter)),
            instrumentoRepository = repositorioInstrumentos,
            categoriaRepository = repositorioCategorias,
            confirmarPendencias = ConfirmarPendencias(repositorioLancamentos, repositorioInstrumentos, clock),
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
        val viewModel: ReviewViewModel,
        val lancamentos: FakeLancamentoRepository,
        val regras: FakeRegraCategoriaRepository,
    )
}
