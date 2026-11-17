package com.pingo.feature.accounts.impl

import androidx.lifecycle.SavedStateHandle
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.testing.TestDispatcherRule
import com.pingo.core.ui.contas.EventoDoFormularioDeConta
import com.pingo.feature.accounts.api.AccountsRoute
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ContaFormViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    private val contas = FakeContaRepository()
    private val instrumentos = FakeInstrumentoRepository()

    @Test
    fun `nova conta comeca com o formulario padrao e sem poder salvar credito incompleto`() {
        val viewModel = criarViewModel()

        assertFalse(viewModel.uiState.value.carregando)
        assertFalse(viewModel.uiState.value.edicao)
        assertEquals(InstituicaoFinanceira.NUBANK, viewModel.uiState.value.formulario.instituicao)
        assertFalse(viewModel.uiState.value.podeSalvar)

        contarCreditoCompleto(viewModel)
        assertTrue(viewModel.uiState.value.podeSalvar)
    }

    @Test
    fun `salvar conta valida cria uma unica conta com todos os instrumentos`() {
        val viewModel = criarViewModel()
        contarCreditoCompleto(viewModel)
        val chaveDoSegundo = adicionarCredito(viewModel)

        viewModel.onEvent(
            ContaFormEvent.Formulario(EventoDoFormularioDeConta.AlterarFechamento(chaveDoSegundo, "20")),
        )
        viewModel.onEvent(
            ContaFormEvent.Formulario(EventoDoFormularioDeConta.AlterarVencimento(chaveDoSegundo, "28")),
        )
        viewModel.onEvent(ContaFormEvent.Salvar)

        val criada = contas.criadas.single().single()
        assertEquals(InstituicaoFinanceira.NUBANK, criada.instituicao)
        assertEquals(listOf(TipoInstrumento.CREDITO, TipoInstrumento.CREDITO), criada.instrumentos.map { it.tipo })
        assertEquals(listOf(5, 20), criada.instrumentos.map { it.fechamentoDia })
        assertEquals(listOf(12, 28), criada.instrumentos.map { it.vencimentoDia })
        assertTrue(viewModel.uiState.value.concluido)
        assertFalse(viewModel.uiState.value.salvando)
    }

    @Test
    fun `instituicao outra salva conta manual com instrumento de debito`() {
        val viewModel = criarViewModel()

        viewModel.onEvent(
            ContaFormEvent.Formulario(EventoDoFormularioDeConta.AlterarInstituicao(InstituicaoFinanceira.OUTRA)),
        )
        viewModel.onEvent(ContaFormEvent.Formulario(EventoDoFormularioDeConta.AlterarApelidoConta("Conta manual")))
        viewModel.onEvent(ContaFormEvent.Salvar)

        val criada = contas.criadas.single().single()
        assertEquals(InstituicaoFinanceira.OUTRA, criada.instituicao)
        assertEquals("Conta manual", criada.apelido)
        assertEquals(TipoInstrumento.DEBITO, criada.instrumentos.single().tipo)
        assertTrue(viewModel.uiState.value.concluido)
    }

    @Test
    fun `salvar pendente bloqueia toques repetidos e grava uma vez`() {
        val viewModel = criarViewModel()
        contarCreditoCompleto(viewModel)
        contas.gateDeSalvamento = CompletableDeferred()

        viewModel.onEvent(ContaFormEvent.Salvar)
        viewModel.onEvent(ContaFormEvent.Salvar)

        assertTrue(viewModel.uiState.value.salvando)
        contas.gateDeSalvamento?.complete(Unit)

        assertEquals(1, contas.criadas.size)
        assertTrue(viewModel.uiState.value.concluido)
    }

    @Test
    fun `falha ao salvar mostra erro e nova tentativa conclui uma vez`() {
        val viewModel = criarViewModel()
        contarCreditoCompleto(viewModel)
        contas.falhaAoSalvar = IOException("disco cheio")

        viewModel.onEvent(ContaFormEvent.Salvar)

        assertTrue(viewModel.uiState.value.falhouAoSalvar)
        assertFalse(viewModel.uiState.value.concluido)
        assertTrue(contas.criadas.isEmpty())

        contas.falhaAoSalvar = null
        viewModel.onEvent(ContaFormEvent.Salvar)

        assertFalse(viewModel.uiState.value.falhouAoSalvar)
        assertTrue(viewModel.uiState.value.concluido)
        assertEquals(1, contas.criadas.size)
    }

    @Test
    fun `salvar formulario invalido nao grava`() {
        val viewModel = criarViewModel()

        viewModel.onEvent(ContaFormEvent.Salvar)

        assertTrue(contas.criadas.isEmpty())
        assertFalse(viewModel.uiState.value.concluido)
    }

    @Test
    fun `edicao carrega conta e instrumentos preservando identificadores`() {
        semearContaEditavel()
        val viewModel = criarViewModel("conta-1")

        assertFalse(viewModel.uiState.value.carregando)
        assertTrue(viewModel.uiState.value.edicao)
        assertEquals("conta-1", viewModel.uiState.value.formulario.publicId)
        assertEquals("instrumento-1", viewModel.uiState.value.formulario.instrumentos.single().publicId)
        assertTrue(viewModel.uiState.value.podeSalvar)
    }

    @Test
    fun `edicao preserva ids e adiciona novo instrumento sem substituir o existente`() {
        semearContaEditavel()
        val viewModel = criarViewModel("conta-1")

        viewModel.onEvent(
            ContaFormEvent.Formulario(EventoDoFormularioDeConta.AlterarApelidoConta("Nubank pessoal")),
        )
        viewModel.onEvent(
            ContaFormEvent.Formulario(EventoDoFormularioDeConta.AlterarUltimosDigitos("instrumento-1", "4321")),
        )
        viewModel.onEvent(
            ContaFormEvent.Formulario(EventoDoFormularioDeConta.AdicionarInstrumento(TipoInstrumento.DEBITO)),
        )
        viewModel.onEvent(ContaFormEvent.Salvar)

        val salva = contas.salvas.single()
        assertEquals("conta-1", salva.publicId)
        assertEquals("Nubank pessoal", salva.apelido)
        assertEquals("instrumento-1", salva.instrumentos[0].publicId)
        assertEquals("4321", salva.instrumentos[0].ultimosDigitos)
        assertNull(salva.instrumentos[1].publicId)
        assertEquals(TipoInstrumento.DEBITO, salva.instrumentos[1].tipo)
        assertTrue(contas.criadas.isEmpty())
        assertTrue(viewModel.uiState.value.concluido)
    }

    @Test
    fun `edicao de voucher preserva beneficio saldo e recarga`() {
        semearContaEditavel(instrumentoVoucher = true)
        val viewModel = criarViewModel("conta-1")
        val chave = viewModel.uiState.value.formulario.instrumentos.single().chave

        viewModel.onEvent(ContaFormEvent.Formulario(EventoDoFormularioDeConta.AlterarSaldoInicial(chave, "500,00")))
        viewModel.onEvent(ContaFormEvent.Salvar)

        val salva = contas.salvas.single().instrumentos.single()
        assertEquals("instrumento-1", salva.publicId)
        assertEquals(BeneficioVoucher.VR, salva.beneficio)
        assertEquals(50000L, salva.saldoCentavos)
        assertEquals(5, salva.diaRecarga)
        assertEquals(30000L, salva.valorRecargaCentavos)
    }

    @Test
    fun `falha na edicao nao sinaliza conclusao e mantem o formulario`() {
        semearContaEditavel()
        val viewModel = criarViewModel("conta-1")
        contas.falhaAoSalvar = IOException("sem espaço")

        viewModel.onEvent(ContaFormEvent.Salvar)

        assertTrue(viewModel.uiState.value.falhouAoSalvar)
        assertFalse(viewModel.uiState.value.concluido)
        assertEquals("instrumento-1", viewModel.uiState.value.formulario.instrumentos.single().publicId)
    }

    @Test
    fun `falha ao carregar edicao permite tentar de novo`() {
        val viewModel = criarViewModel("conta-1")

        assertTrue(viewModel.uiState.value.falhaDeLeitura)

        semearContaEditavel()
        viewModel.onEvent(ContaFormEvent.TentarDeNovo)

        assertFalse(viewModel.uiState.value.falhaDeLeitura)
        assertEquals("conta-1", viewModel.uiState.value.formulario.publicId)
    }

    @Test
    fun `dois creditos usam chaves distintas no formulario`() {
        val viewModel = criarViewModel()

        val chaveDoSegundo = adicionarCredito(viewModel)
        val chaves = viewModel.uiState.value.formulario.instrumentos.map { it.chave }

        assertEquals(2, chaves.size)
        assertNotEquals(chaves[0], chaves[1])
        assertEquals(chaveDoSegundo, chaves[1])
    }

    @Test
    fun `arquivar instrumento isolado afeta apenas ele e permite reativar`() {
        semearContaEditavel()
        val irmao = instrumentos.porConta.single().copy(publicId = "instrumento-2", apelido = "Débito", ultimosDigitos = null)
        instrumentos.porConta = instrumentos.porConta + irmao
        instrumentos.instrumentos.value = instrumentos.porConta
        val viewModel = criarViewModel("conta-1")

        viewModel.onEvent(ContaFormEvent.SolicitarArquivamentoDoInstrumento("instrumento-1"))
        assertEquals(DialogoDeArquivamento.Instrumento("instrumento-1", "Cartão"), viewModel.uiState.value.dialogo)
        viewModel.onEvent(ContaFormEvent.ConfirmarArquivamento)

        assertEquals(listOf("instrumento-1"), instrumentos.arquivados)
        assertTrue(contas.arquivadas.isEmpty())
        assertFalse(viewModel.uiState.value.contaArquivada)
        assertEquals(
            listOf(true, false),
            viewModel.uiState.value.formulario.instrumentos.map { it.arquivado },
        )

        val reaberto = criarViewModel("conta-1")
        assertEquals(listOf(true, false), reaberto.uiState.value.formulario.instrumentos.map { it.arquivado })

        reaberto.onEvent(ContaFormEvent.ReativarInstrumento("instrumento-1"))

        assertEquals(listOf("instrumento-1"), instrumentos.reativados)
        assertEquals(listOf(false, false), reaberto.uiState.value.formulario.instrumentos.map { it.arquivado })
        assertEquals("instrumento-1", reaberto.uiState.value.formulario.instrumentos.first().publicId)
    }

    @Test
    fun `cancelar confirmacao de arquivamento nao altera nada`() {
        semearContaEditavel()
        val viewModel = criarViewModel("conta-1")

        viewModel.onEvent(ContaFormEvent.SolicitarArquivamentoDaConta)
        assertEquals(DialogoDeArquivamento.Conta, viewModel.uiState.value.dialogo)

        viewModel.onEvent(ContaFormEvent.CancelarArquivamento)

        assertNull(viewModel.uiState.value.dialogo)
        assertTrue(contas.arquivadas.isEmpty())
        assertFalse(viewModel.uiState.value.contaArquivada)
        assertFalse(viewModel.uiState.value.concluido)
    }

    @Test
    fun `confirmar arquivamento da conta conclui e reativar volta ao estado ativo`() {
        semearContaEditavel()
        val viewModel = criarViewModel("conta-1")

        viewModel.onEvent(ContaFormEvent.SolicitarArquivamentoDaConta)
        viewModel.onEvent(ContaFormEvent.ConfirmarArquivamento)

        assertEquals(listOf("conta-1"), contas.arquivadas)
        assertTrue(viewModel.uiState.value.contaArquivada)
        assertTrue(viewModel.uiState.value.concluido)

        val reaberto = criarViewModel("conta-1")
        assertTrue(reaberto.uiState.value.contaArquivada)
        assertFalse(reaberto.uiState.value.podeSalvar)

        reaberto.onEvent(ContaFormEvent.ReativarConta)

        assertEquals(listOf("conta-1"), contas.reativadas)
        assertFalse(reaberto.uiState.value.contaArquivada)
        assertEquals("instrumento-1", reaberto.uiState.value.formulario.instrumentos.single().publicId)
    }

    @Test
    fun `falha ao arquivar mantem a conta ativa e sinaliza erro`() {
        semearContaEditavel()
        val viewModel = criarViewModel("conta-1")
        contas.falhaAoArquivar = IOException("banco indisponível")

        viewModel.onEvent(ContaFormEvent.SolicitarArquivamentoDaConta)
        viewModel.onEvent(ContaFormEvent.ConfirmarArquivamento)

        assertTrue(viewModel.uiState.value.falhouAoArquivar)
        assertFalse(viewModel.uiState.value.contaArquivada)
        assertFalse(viewModel.uiState.value.concluido)
        assertNull(viewModel.uiState.value.dialogo)
    }

    private fun criarViewModel(contaPublicId: String? = null): ContaFormViewModel {
        val argumentos = if (contaPublicId == null) emptyMap() else mapOf(AccountsRoute.ARG_CONTA_ID to contaPublicId)

        return ContaFormViewModel(contas, instrumentos, CLOCK, SavedStateHandle(argumentos))
    }

    private fun contarCreditoCompleto(viewModel: ContaFormViewModel) {
        val chave = viewModel.uiState.value.formulario.instrumentos.single().chave
        viewModel.onEvent(ContaFormEvent.Formulario(EventoDoFormularioDeConta.AlterarFechamento(chave, "5")))
        viewModel.onEvent(ContaFormEvent.Formulario(EventoDoFormularioDeConta.AlterarVencimento(chave, "12")))
    }

    private fun adicionarCredito(viewModel: ContaFormViewModel): String {
        viewModel.onEvent(
            ContaFormEvent.Formulario(EventoDoFormularioDeConta.AdicionarInstrumento(TipoInstrumento.CREDITO)),
        )

        return viewModel.uiState.value.formulario.instrumentos.last().chave
    }

    private fun semearContaEditavel(instrumentoVoucher: Boolean = false) {
        contas.contaPorId = Conta(
            publicId = "conta-1",
            instituicao = InstituicaoFinanceira.NUBANK,
            apelido = "Nubank",
            tipo = TipoConta.CORRENTE,
            criadoEm = INSTANTE,
            atualizadoEm = INSTANTE,
        )
        instrumentos.porConta = listOf(
            Instrumento(
                publicId = "instrumento-1",
                contaPublicId = "conta-1",
                tipo = if (instrumentoVoucher) TipoInstrumento.VOUCHER else TipoInstrumento.CREDITO,
                apelido = if (instrumentoVoucher) "VR" else "Cartão",
                ultimosDigitos = if (instrumentoVoucher) null else "1234",
                fechamentoDia = if (instrumentoVoucher) null else 5,
                vencimentoDia = if (instrumentoVoucher) null else 12,
                beneficio = if (instrumentoVoucher) BeneficioVoucher.VR else null,
                saldoCentavos = if (instrumentoVoucher) 25000L else null,
                diaRecarga = if (instrumentoVoucher) 5 else null,
                valorRecargaCentavos = if (instrumentoVoucher) 30000L else null,
                criadoEm = INSTANTE,
                atualizadoEm = INSTANTE,
            ),
        )
        contas.contas.value = listOf(contas.contaPorId!!)
        instrumentos.instrumentos.value = instrumentos.porConta
    }

    private companion object {
        val INSTANTE: Instant = Instant.parse("2026-11-10T12:00:00Z")
        val CLOCK: Clock = Clock.fixed(INSTANTE, ZoneOffset.UTC)
    }
}
