package com.pingo.feature.review.impl

import com.pingo.core.auth.UsuarioAutenticado
import com.pingo.core.data.categoria.CategoriaAprendida
import com.pingo.core.data.detalhe.EditarLancamento
import com.pingo.core.data.detalhe.MarcarTransferencia
import com.pingo.core.data.reconciliacao.ReconciliadorFinanceiro
import com.pingo.core.data.revisao.ConfirmarPendencias
import com.pingo.core.data.voucher.ResolverRecargaVoucherUseCase
import com.pingo.core.database.entity.RecargaProgramadaVoucherEntity
import com.pingo.core.model.BeneficioVoucher
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
import com.pingo.core.model.voucher.MotivoRevisaoRecarga
import com.pingo.core.model.voucher.StatusRecargaProgramada
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewVoucherViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val agora: Instant = Instant.parse("2026-11-10T15:30:00Z")
    private val clock = Clock.fixed(agora, ZoneOffset.UTC)

    private val contaCaju = Conta(
        publicId = "conta-caju",
        instituicao = InstituicaoFinanceira.CAJU,
        apelido = "Caju",
        tipo = TipoConta.BENEFICIO,
        criadoEm = agora,
        atualizadoEm = agora,
    )
    private val voucherVr = Instrumento(
        publicId = "inst-vr",
        contaPublicId = contaCaju.publicId,
        tipo = TipoInstrumento.VOUCHER,
        apelido = "Refeição",
        ultimosDigitos = "1111",
        beneficio = BeneficioVoucher.VR,
        saldoCentavos = 52000,
        criadoEm = agora,
        atualizadoEm = agora,
    )
    private val voucherVa = Instrumento(
        publicId = "inst-va",
        contaPublicId = contaCaju.publicId,
        tipo = TipoInstrumento.VOUCHER,
        apelido = "Alimentação",
        ultimosDigitos = "2222",
        beneficio = BeneficioVoucher.VA,
        saldoCentavos = 34000,
        criadoEm = agora,
        atualizadoEm = agora,
    )
    private val beneficioCategoria = Categoria(
        publicId = "cat-beneficio",
        nome = "Benefício",
        tipo = TipoCategoria.RECEITA,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    @Test
    fun `beneficio ambiguo bloqueia confirmacao cega e resolve como extra sem previsao`() = runTest {
        val ambiguo = lancamentoVoucher("rec-ambiguo", null, null, MotivoPendencia.BENEFICIO_AMBIGUO, 75000)
        val cenario = criarCenario(lancamentos = listOf(ambiguo))
        advanceUntilIdle()

        val item = cenario.viewModel.uiState.value.pendentes.single()
        assertEquals(MotivoDaRevisao.BENEFICIO_AMBIGUO, item.motivo)
        assertFalse(item.podeConfirmar)

        cenario.viewModel.onEvent(ReviewEvent.ConfirmarTodos)
        advanceUntilIdle()
        val msg = cenario.viewModel.uiState.value.mensagem as? MensagemDaRevisao.VoucherPendenteManual
        assertEquals(1, msg?.restantes)
        assertTrue(msg?.apenasVouchers == true)

        cenario.viewModel.onEvent(ReviewEvent.AbrirResolucaoVoucher("rec-ambiguo"))
        advanceUntilIdle()
        val painel = cenario.viewModel.uiState.value.painelDeVoucher
        assertEquals(2, painel?.vouchersElegiveis?.size)
        assertFalse(painel?.podeConfirmar == true)

        cenario.viewModel.onEvent(ReviewEvent.SelecionarVoucher(voucherVr.publicId))
        advanceUntilIdle()
        assertTrue(cenario.viewModel.uiState.value.painelDeVoucher?.podeConfirmar == true)

        cenario.viewModel.onEvent(ReviewEvent.ConfirmarResolucaoVoucher)
        advanceUntilIdle()

        val salvo = cenario.lancamentos.atual("rec-ambiguo")
        assertEquals(StatusLancamento.CONFIRMADO, salvo?.status)
        assertEquals(voucherVr.publicId, salvo?.instrumentoPublicId)
        assertEquals(BeneficioVoucher.VR, salvo?.beneficio)
    }

    @Test
    fun `beneficio ambiguo com previsao exige escolha e substitui ocorrencia selecionada`() = runTest {
        val ambiguo = lancamentoVoucher(
            publicId = "rec-ambiguo-prev",
            instrumentoPublicId = null,
            beneficio = null,
            motivo = MotivoPendencia.BENEFICIO_AMBIGUO,
            valorCentavos = 60000,
        )
        val recarga = recargaEntity(
            publicId = "prog-va-1",
            instrumentoPublicId = voucherVa.publicId,
            beneficio = BeneficioVoucher.VA,
            dataPrevista = LocalDate.of(2026, 11, 10),
            valorPrevistoCentavos = 60000,
        )
        val cenario = criarCenario(lancamentos = listOf(ambiguo), recargas = listOf(recarga))
        advanceUntilIdle()

        cenario.viewModel.onEvent(ReviewEvent.AbrirResolucaoVoucher("rec-ambiguo-prev"))
        advanceUntilIdle()
        cenario.viewModel.onEvent(ReviewEvent.SelecionarVoucher(voucherVa.publicId))
        advanceUntilIdle()

        val aposVoucher = cenario.viewModel.uiState.value.painelDeVoucher
        assertTrue(aposVoucher?.exigeEscolhaDeDecisao == true)
        assertFalse(aposVoucher?.podeConfirmar == true)

        cenario.viewModel.onEvent(ReviewEvent.SelecionarDecisaoVoucher(TipoDecisaoVoucher.SUBSTITUIR_PREVISAO))
        advanceUntilIdle()
        assertTrue(cenario.viewModel.uiState.value.painelDeVoucher?.podeConfirmar == true)

        cenario.viewModel.onEvent(ReviewEvent.ConfirmarResolucaoVoucher)
        advanceUntilIdle()

        assertEquals(StatusLancamento.CONFIRMADO, cenario.lancamentos.atual("rec-ambiguo-prev")?.status)
        val recargaAtual = cenario.recargas.atual("prog-va-1")
        assertEquals(StatusRecargaProgramada.SUBSTITUIDA, recargaAtual?.status)
        assertEquals("rec-ambiguo-prev", recargaAtual?.lancamentoPublicId)
    }

    @Test
    fun `divergencia recarga destaca ocorrencia mais proxima e permite trocar ocorrencia ou registrar extra`() = runTest {
        val divergente = lancamentoVoucher(
            "rec-divergente",
            voucherVr.publicId,
            BeneficioVoucher.VR,
            MotivoPendencia.DIVERGENCIA_RECARGA,
            85000,
        )
        val recargaDistante = recargaEntity("prog-distante", voucherVr.publicId, BeneficioVoucher.VR, LocalDate.of(2026, 11, 1), 50000)
        val recargaProxima = recargaEntity("prog-proxima", voucherVr.publicId, BeneficioVoucher.VR, LocalDate.of(2026, 11, 9), 55000)
        val cenario = criarCenario(lancamentos = listOf(divergente), recargas = listOf(recargaDistante, recargaProxima))
        advanceUntilIdle()

        cenario.viewModel.onEvent(ReviewEvent.AbrirResolucaoVoucher("rec-divergente"))
        advanceUntilIdle()

        val painel = cenario.viewModel.uiState.value.painelDeVoucher
        assertEquals(MotivoRevisaoRecarga.DIVERGENCIA_RECARGA, painel?.motivo)
        assertEquals("prog-proxima", painel?.ocorrenciaSelecionadaPublicId)
        assertFalse(painel?.podeConfirmar == true)

        cenario.viewModel.onEvent(ReviewEvent.SelecionarOcorrenciaVoucher("prog-distante"))
        advanceUntilIdle()
        assertEquals("prog-distante", cenario.viewModel.uiState.value.painelDeVoucher?.ocorrenciaSelecionadaPublicId)
        assertEquals(TipoDecisaoVoucher.SUBSTITUIR_PREVISAO, cenario.viewModel.uiState.value.painelDeVoucher?.decisaoSelecionada)

        cenario.viewModel.onEvent(ReviewEvent.SelecionarDecisaoVoucher(TipoDecisaoVoucher.REGISTRAR_EXTRA))
        advanceUntilIdle()
        cenario.viewModel.onEvent(ReviewEvent.ConfirmarResolucaoVoucher)
        advanceUntilIdle()

        assertEquals(StatusLancamento.CONFIRMADO, cenario.lancamentos.atual("rec-divergente")?.status)
        assertEquals(StatusRecargaProgramada.PENDENTE, cenario.recargas.atual("prog-distante")?.status)
        assertEquals(StatusRecargaProgramada.PENDENTE, cenario.recargas.atual("prog-proxima")?.status)
    }

    @Test
    fun `descartar continua funcionando para pendencia de voucher`() = runTest {
        val ambiguo = lancamentoVoucher(
            publicId = "rec-descartar",
            instrumentoPublicId = null,
            beneficio = null,
            motivo = MotivoPendencia.BENEFICIO_AMBIGUO,
            valorCentavos = 40000,
        )
        val cenario = criarCenario(lancamentos = listOf(ambiguo))
        advanceUntilIdle()

        cenario.viewModel.onEvent(ReviewEvent.Descartar("rec-descartar"))
        advanceUntilIdle()

        assertNotNull(cenario.lancamentos.atual("rec-descartar")?.excluidoEm)
        assertTrue(cenario.viewModel.uiState.value.vazia)
    }

    private fun criarCenario(lancamentos: List<Lancamento>, recargas: List<RecargaProgramadaVoucherEntity> = emptyList()): Cenario {
        val repoLancamentos = FakeLancamentoRepository(lancamentos)
        val repoInstrumentos = FakeInstrumentoRepository(listOf(voucherVr, voucherVa))
        val repoCategorias = FakeCategoriaRepository(listOf(beneficioCategoria))
        val repoRegras = FakeRegraCategoriaRepository()
        val transacao = FakeTransacaoLocal()
        val recargaDao = FakeRecargaProgramadaVoucherDao(recargas)
        val resolver = ResolverRecargaVoucherUseCase(
            transacao,
            FakeLancamentoDao(repoLancamentos),
            FakeInstrumentoDao(repoInstrumentos),
            recargaDao,
            FakeCategoriaDao(repoCategorias),
            ReconciliadorFinanceiro.NoOp,
            clock,
        )
        val aprendida = CategoriaAprendida(repoCategorias, repoRegras, UsuarioAutenticado(FakeTokenStore(null)))
        val viewModel = ReviewViewModel(
            repoLancamentos,
            FakeContaRepository(listOf(contaCaju)),
            repoInstrumentos,
            repoCategorias,
            ConfirmarPendencias(repoLancamentos, repoInstrumentos, clock),
            EditarLancamento(repoLancamentos, repoInstrumentos, repoCategorias, aprendida, transacao, clock),
            MarcarTransferencia(repoLancamentos, repoInstrumentos, transacao, clock),
            resolver,
            clock,
        )
        return Cenario(viewModel, repoLancamentos, recargaDao)
    }

    private fun lancamentoVoucher(
        publicId: String,
        instrumentoPublicId: String?,
        beneficio: BeneficioVoucher?,
        motivo: MotivoPendencia,
        valorCentavos: Long,
    ): Lancamento = Lancamento(
        publicId = publicId,
        instrumentoPublicId = instrumentoPublicId,
        categoriaPublicId = null,
        valorCentavos = valorCentavos,
        dataHora = agora,
        tipo = TipoLancamento.RECEITA,
        estabelecimento = "Recarga Caju",
        status = StatusLancamento.PENDENTE,
        origem = OrigemLancamento.NOTIFICACAO,
        instituicaoOrigem = InstituicaoFinanceira.CAJU,
        beneficio = beneficio,
        confianca = 0.7,
        motivoPendencia = motivo.name,
        criadoEm = agora,
        atualizadoEm = agora,
        textoBruto = "Você recebeu uma recarga no Caju",
    )

    private fun recargaEntity(
        publicId: String,
        instrumentoPublicId: String,
        beneficio: BeneficioVoucher,
        dataPrevista: LocalDate,
        valorPrevistoCentavos: Long,
    ): RecargaProgramadaVoucherEntity = RecargaProgramadaVoucherEntity(
        publicId = publicId,
        instrumentoPublicId = instrumentoPublicId,
        beneficio = beneficio,
        competenciaMes = dataPrevista.withDayOfMonth(1),
        dataPrevista = dataPrevista,
        valorPrevistoCentavos = valorPrevistoCentavos,
        status = StatusRecargaProgramada.PENDENTE,
        lancamentoPublicId = null,
        criadoEm = agora,
        atualizadoEm = agora,
        excluidoEm = null,
    )

    private data class Cenario(
        val viewModel: ReviewViewModel,
        val lancamentos: FakeLancamentoRepository,
        val recargas: FakeRecargaProgramadaVoucherDao,
    )
}
