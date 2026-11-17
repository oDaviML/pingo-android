package com.pingo.feature.onboarding.impl

import com.pingo.core.data.CadastroSalvo
import com.pingo.core.data.ContaRepository
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Conta
import com.pingo.core.model.ContaEditada
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.NovaConta
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.testing.TestDispatcherRule
import com.pingo.core.ui.contas.ContaFormulario
import com.pingo.core.ui.contas.EventoDoFormularioDeConta
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant

class OnboardingContasViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    private val contaRepository = FakeContaRepository()
    private val preferences = FakeUserPreferences()

    private val viewModel = OnboardingContasViewModel(contaRepository, preferences)

    @Test
    fun `comeca sem bancos escolhidos e sem poder continuar`() {
        assertTrue(viewModel.uiState.value.contas.isEmpty())
        assertFalse(viewModel.uiState.value.podeContinuar)
    }

    @Test
    fun `escolher um banco cria a conta com o tipo e o instrumento padrao`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))

        val conta = contaDe(InstituicaoFinanceira.NUBANK)
        assertEquals("Nubank", conta.apelido)
        assertEquals(TipoConta.CORRENTE, conta.tipoConta)
        assertEquals(listOf(TipoInstrumento.CREDITO), conta.instrumentos.map { it.tipo })
        assertEquals("Cartão de crédito", conta.instrumentos.single().apelido)
        assertFalse(viewModel.uiState.value.podeContinuar)

        preencherCredito(InstituicaoFinanceira.NUBANK)
        assertTrue(viewModel.uiState.value.podeContinuar)
    }

    @Test
    fun `credito sem fechamento ou vencimento impede continuar`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarFechamento(chaveDe(InstituicaoFinanceira.NUBANK), "5"))
        assertFalse(viewModel.uiState.value.podeContinuar)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarVencimento(chaveDe(InstituicaoFinanceira.NUBANK), "32"))
        assertFalse(viewModel.uiState.value.podeContinuar)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarVencimento(chaveDe(InstituicaoFinanceira.NUBANK), "31"))
        assertTrue(viewModel.uiState.value.podeContinuar)
    }

    @Test
    fun `voucher exige beneficio e saldo inicial`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.CAJU))

        assertFalse(viewModel.uiState.value.podeContinuar)

        alterar(
            InstituicaoFinanceira.CAJU,
            EventoDoFormularioDeConta.AlterarBeneficio(chaveDe(InstituicaoFinanceira.CAJU), BeneficioVoucher.VR),
        )
        assertFalse(viewModel.uiState.value.podeContinuar)

        alterar(InstituicaoFinanceira.CAJU, EventoDoFormularioDeConta.AlterarSaldoInicial(chaveDe(InstituicaoFinanceira.CAJU), "250,00"))
        assertTrue(viewModel.uiState.value.podeContinuar)

        val instrumento = contaDe(InstituicaoFinanceira.CAJU).instrumentos.single()
        assertEquals(BeneficioVoucher.VR, instrumento.beneficio)
        assertEquals(25000L, instrumento.paraNovoInstrumento().saldoCentavos)
    }

    @Test
    fun `desmarcar um banco o remove da lista`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))

        assertTrue(viewModel.uiState.value.contas.isEmpty())
    }

    @Test
    fun `caju cria conta de beneficio com voucher`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.CAJU))

        val conta = contaDe(InstituicaoFinanceira.CAJU)
        assertEquals(TipoConta.BENEFICIO, conta.tipoConta)
        assertEquals(listOf(TipoInstrumento.VOUCHER), conta.instrumentos.map { it.tipo })
    }

    @Test
    fun `alterar apelido e tipo da conta mexe so no banco escolhido`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.INTER))

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarApelidoConta("Roxinho"))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarTipoConta(TipoConta.BENEFICIO))

        assertEquals("Roxinho", contaDe(InstituicaoFinanceira.NUBANK).apelido)
        assertEquals(TipoConta.BENEFICIO, contaDe(InstituicaoFinanceira.NUBANK).tipoConta)
        assertEquals("Inter", contaDe(InstituicaoFinanceira.INTER).apelido)
        assertEquals(TipoConta.CORRENTE, contaDe(InstituicaoFinanceira.INTER).tipoConta)
    }

    @Test
    fun `adiciona dois instrumentos do mesmo tipo com dados independentes`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AdicionarInstrumento(TipoInstrumento.CREDITO))

        val instrumentos = contaDe(InstituicaoFinanceira.NUBANK).instrumentos
        assertEquals(listOf(TipoInstrumento.CREDITO, TipoInstrumento.CREDITO), instrumentos.map { it.tipo })
        assertNotEquals(instrumentos[0].chave, instrumentos[1].chave)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarApelidoInstrumento(instrumentos[1].chave, "Cartão dois"))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarFechamento(instrumentos[1].chave, "20"))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarVencimento(instrumentos[1].chave, "28"))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarFechamento(instrumentos[0].chave, "5"))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarVencimento(instrumentos[0].chave, "12"))

        val atualizados = contaDe(InstituicaoFinanceira.NUBANK).instrumentos
        assertEquals("Cartão de crédito", atualizados[0].apelido)
        assertEquals("5", atualizados[0].fechamentoDia)
        assertEquals("Cartão dois", atualizados[1].apelido)
        assertEquals("20", atualizados[1].fechamentoDia)
        assertTrue(viewModel.uiState.value.podeContinuar)
    }

    @Test
    fun `marcar outro tipo de instrumento adiciona um instrumento com apelido padrao`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AdicionarInstrumento(TipoInstrumento.DEBITO))

        val instrumentos = contaDe(InstituicaoFinanceira.NUBANK).instrumentos
        assertEquals(listOf(TipoInstrumento.CREDITO, TipoInstrumento.DEBITO), instrumentos.map { it.tipo })
        assertEquals("Cartão de débito", instrumentos.last().apelido)
    }

    @Test
    fun `remover um instrumento mantem os outros`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AdicionarInstrumento(TipoInstrumento.DEBITO))
        val chaveDoCredito = chaveDe(InstituicaoFinanceira.NUBANK, indice = 0)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.RemoverInstrumento(chaveDoCredito))

        assertEquals(listOf(TipoInstrumento.DEBITO), contaDe(InstituicaoFinanceira.NUBANK).instrumentos.map { it.tipo })
    }

    @Test
    fun `nao remove o ultimo instrumento do banco`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        preencherCredito(InstituicaoFinanceira.NUBANK)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.RemoverInstrumento(chaveDe(InstituicaoFinanceira.NUBANK)))

        assertEquals(listOf(TipoInstrumento.CREDITO), contaDe(InstituicaoFinanceira.NUBANK).instrumentos.map { it.tipo })
        assertTrue(viewModel.uiState.value.podeContinuar)
    }

    @Test
    fun `ultimos digitos aceitam so numeros e guardam no maximo quatro`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))

        alterar(
            InstituicaoFinanceira.NUBANK,
            EventoDoFormularioDeConta.AlterarUltimosDigitos(chaveDe(InstituicaoFinanceira.NUBANK), "12a34x9"),
        )

        assertEquals("1234", contaDe(InstituicaoFinanceira.NUBANK).instrumentos.single().ultimosDigitos)
    }

    @Test
    fun `apelido em branco ou ultimos digitos incompletos impedem continuar`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        preencherCredito(InstituicaoFinanceira.NUBANK)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarApelidoConta("   "))
        assertFalse(viewModel.uiState.value.podeContinuar)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarApelidoConta("Nubank"))
        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarUltimosDigitos(chaveDe(InstituicaoFinanceira.NUBANK), "12"))
        assertFalse(viewModel.uiState.value.podeContinuar)

        alterar(InstituicaoFinanceira.NUBANK, EventoDoFormularioDeConta.AlterarUltimosDigitos(chaveDe(InstituicaoFinanceira.NUBANK), ""))
        assertTrue(viewModel.uiState.value.podeContinuar)
    }

    @Test
    fun `continuar cria as contas com instrumentos e conclui o passo`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.ITAU))
        alterar(
            InstituicaoFinanceira.NUBANK,
            EventoDoFormularioDeConta.AlterarUltimosDigitos(chaveDe(InstituicaoFinanceira.NUBANK), "1084"),
        )
        preencherCredito(InstituicaoFinanceira.NUBANK)

        viewModel.onEvent(OnboardingContasEvent.Continuar)

        val criadas = contaRepository.criadas.single()
        assertEquals(listOf(InstituicaoFinanceira.NUBANK, InstituicaoFinanceira.ITAU), criadas.map { it.instituicao })
        assertEquals("1084", criadas.first().instrumentos.single().ultimosDigitos)
        assertEquals(listOf(TipoInstrumento.DEBITO), criadas.last().instrumentos.map { it.tipo })
        assertTrue(preferences.configuradas)
        assertTrue(viewModel.uiState.value.concluido)
    }

    @Test
    fun `continuar sem bancos nao cria contas`() {
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        assertTrue(contaRepository.criadas.isEmpty())
        assertFalse(preferences.configuradas)
        assertFalse(viewModel.uiState.value.concluido)
    }

    @Test
    fun `pular conclui o passo sem criar contas`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))

        viewModel.onEvent(OnboardingContasEvent.Pular)

        assertTrue(contaRepository.criadas.isEmpty())
        assertTrue(preferences.configuradas)
        assertTrue(viewModel.uiState.value.concluido)
    }

    @Test
    fun `falha ao criar contas mostra erro e permite tentar de novo`() {
        contaRepository.failure = IllegalStateException("disco cheio")
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        preencherCredito(InstituicaoFinanceira.NUBANK)

        viewModel.onEvent(OnboardingContasEvent.Continuar)

        assertEquals(SalvamentoUiState.Error, viewModel.uiState.value.salvamento)
        assertFalse(viewModel.uiState.value.concluido)
        assertFalse(preferences.configuradas)

        contaRepository.failure = null
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        assertEquals(SalvamentoUiState.Idle, viewModel.uiState.value.salvamento)
        assertTrue(viewModel.uiState.value.concluido)
        assertEquals(1, contaRepository.criadas.size)
    }

    @Test
    fun `falha ao marcar o passo nao duplica as contas ao tentar de novo`() {
        preferences.failure = IOException("sem espaco")
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        preencherCredito(InstituicaoFinanceira.NUBANK)

        viewModel.onEvent(OnboardingContasEvent.Continuar)

        assertEquals(SalvamentoUiState.Error, viewModel.uiState.value.salvamento)
        assertFalse(viewModel.uiState.value.concluido)

        preferences.failure = null
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        assertEquals(1, contaRepository.criadas.size)
        assertTrue(preferences.configuradas)
        assertTrue(viewModel.uiState.value.concluido)
    }

    @Test
    fun `continuar durante o salvamento nao grava as contas de novo`() {
        contaRepository.gate = CompletableDeferred()
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        preencherCredito(InstituicaoFinanceira.NUBANK)
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        viewModel.onEvent(OnboardingContasEvent.Continuar)
        contaRepository.gate?.complete(Unit)

        assertEquals(1, contaRepository.criadas.size)
        assertTrue(viewModel.uiState.value.concluido)
    }

    private fun contaDe(instituicao: InstituicaoFinanceira): ContaFormulario =
        viewModel.uiState.value.contas.first { it.instituicao == instituicao }

    private fun chaveDe(instituicao: InstituicaoFinanceira, indice: Int = 0): String = contaDe(instituicao).instrumentos[indice].chave

    private fun alterar(instituicao: InstituicaoFinanceira, evento: EventoDoFormularioDeConta) {
        viewModel.onEvent(OnboardingContasEvent.AlterarFormulario(instituicao, evento))
    }

    private fun preencherCredito(instituicao: InstituicaoFinanceira) {
        alterar(instituicao, EventoDoFormularioDeConta.AlterarFechamento(chaveDe(instituicao), "5"))
        alterar(instituicao, EventoDoFormularioDeConta.AlterarVencimento(chaveDe(instituicao), "12"))
    }

    private class FakeContaRepository : ContaRepository {
        val criadas = mutableListOf<List<NovaConta>>()
        var failure: Throwable? = null
        var gate: CompletableDeferred<Unit>? = null

        override fun observeContas(): Flow<List<Conta>> = flowOf(emptyList())

        override suspend fun buscar(publicId: String): Conta? = null

        override suspend fun criarComInstrumentos(contas: List<NovaConta>, agora: Instant) {
            gate?.await()
            failure?.let { throw it }
            criadas.add(contas)
        }

        override suspend fun salvarCadastro(edicao: ContaEditada, agora: Instant): CadastroSalvo = error("nao usado")

        override suspend fun salvar(conta: Conta) = Unit

        override suspend fun salvarTodas(contas: List<Conta>) = Unit

        override suspend fun excluir(publicId: String, quando: Instant) = Unit

        override suspend fun arquivar(publicId: String, quando: Instant) = Unit

        override suspend fun reativar(publicId: String, quando: Instant) = Unit
    }

    private class FakeUserPreferences : UserPreferencesDataSource {
        var configuradas = false
        var failure: Throwable? = null

        override val syncStatus: Flow<SyncStatus> = flowOf(SyncStatus.IDLE)

        override suspend fun setSyncStatus(status: SyncStatus) = Unit

        override suspend fun pullCursor(): String? = null

        override suspend fun setPullCursor(cursor: String?) = Unit

        override suspend fun pushCursor(): Instant? = null

        override suspend fun setPushCursor(instant: Instant) = Unit

        override suspend fun lastSyncAt(): Instant? = null

        override suspend fun setLastSyncAt(instant: Instant) = Unit

        override suspend fun deviceId(): String = "dispositivo-teste"

        override suspend fun activatedDeviceId(): String? = null

        override suspend fun setActivatedDeviceId(deviceId: String) = Unit

        override suspend fun consentVersion(): String? = null

        override suspend fun consentAt(): Instant? = null

        override suspend fun setConsent(version: String, instant: Instant) = Unit

        override suspend fun contasConfiguradas(): Boolean = configuradas

        override suspend fun setContasConfiguradas(configuradas: Boolean) {
            failure?.let { throw it }
            this.configuradas = configuradas
        }

        override suspend fun clear() = Unit
    }
}
