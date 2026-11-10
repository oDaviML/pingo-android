package com.pingo.feature.onboarding.impl

import com.pingo.core.data.ContaRepository
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.NovaConta
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertTrue(viewModel.uiState.value.escolhas.isEmpty())
        assertFalse(viewModel.uiState.value.podeContinuar)
    }

    @Test
    fun `escolher um banco cria a conta com o tipo e o instrumento padrao`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))

        val escolha = viewModel.uiState.value.escolhas.single()
        assertEquals(InstituicaoFinanceira.NUBANK, escolha.instituicao)
        assertEquals("Nubank", escolha.apelido)
        assertEquals(TipoConta.CORRENTE, escolha.tipoConta)
        assertEquals(listOf(TipoInstrumento.CREDITO), escolha.instrumentos.map { it.tipo })
        assertEquals("Cartão de crédito", escolha.instrumentos.single().apelido)
        assertTrue(viewModel.uiState.value.podeContinuar)
    }

    @Test
    fun `desmarcar um banco o remove da lista`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))

        assertTrue(viewModel.uiState.value.escolhas.isEmpty())
    }

    @Test
    fun `caju cria conta de beneficio com voucher`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.CAJU))

        val escolha = viewModel.uiState.value.escolhas.single()
        assertEquals(TipoConta.BENEFICIO, escolha.tipoConta)
        assertEquals(listOf(TipoInstrumento.VOUCHER), escolha.instrumentos.map { it.tipo })
    }

    @Test
    fun `alterar apelido e tipo da conta mexe so no banco escolhido`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.INTER))

        viewModel.onEvent(OnboardingContasEvent.AlterarApelidoConta(InstituicaoFinanceira.NUBANK, "Roxinho"))
        viewModel.onEvent(OnboardingContasEvent.AlterarTipoConta(InstituicaoFinanceira.NUBANK, TipoConta.BENEFICIO))

        val (nubank, inter) = viewModel.uiState.value.escolhas
        assertEquals("Roxinho", nubank.apelido)
        assertEquals(TipoConta.BENEFICIO, nubank.tipoConta)
        assertEquals("Inter", inter.apelido)
        assertEquals(TipoConta.CORRENTE, inter.tipoConta)
    }

    @Test
    fun `marcar outro tipo de instrumento adiciona um instrumento com apelido padrao`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        viewModel.onEvent(OnboardingContasEvent.AlternarInstrumento(InstituicaoFinanceira.NUBANK, TipoInstrumento.DEBITO))

        val instrumentos = viewModel.uiState.value.escolhas.single().instrumentos
        assertEquals(listOf(TipoInstrumento.CREDITO, TipoInstrumento.DEBITO), instrumentos.map { it.tipo })
        assertEquals("Cartão de débito", instrumentos.last().apelido)
    }

    @Test
    fun `desmarcar um tipo de instrumento mantem os outros`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        viewModel.onEvent(OnboardingContasEvent.AlternarInstrumento(InstituicaoFinanceira.NUBANK, TipoInstrumento.DEBITO))
        viewModel.onEvent(OnboardingContasEvent.AlternarInstrumento(InstituicaoFinanceira.NUBANK, TipoInstrumento.CREDITO))

        val instrumentos = viewModel.uiState.value.escolhas.single().instrumentos
        assertEquals(listOf(TipoInstrumento.DEBITO), instrumentos.map { it.tipo })
    }

    @Test
    fun `nao desmarca o ultimo tipo de instrumento do banco`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        viewModel.onEvent(OnboardingContasEvent.AlternarInstrumento(InstituicaoFinanceira.NUBANK, TipoInstrumento.CREDITO))

        val instrumentos = viewModel.uiState.value.escolhas.single().instrumentos
        assertEquals(listOf(TipoInstrumento.CREDITO), instrumentos.map { it.tipo })
        assertTrue(viewModel.uiState.value.podeContinuar)
    }

    @Test
    fun `ultimos digitos aceitam so numeros e guardam no maximo quatro`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))

        viewModel.onEvent(OnboardingContasEvent.AlterarUltimosDigitos(InstituicaoFinanceira.NUBANK, TipoInstrumento.CREDITO, "12a34x9"))

        assertEquals("1234", viewModel.uiState.value.escolhas.single().instrumentos.single().ultimosDigitos)
    }

    @Test
    fun `apelido em branco ou ultimos digitos incompletos impedem continuar`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))

        viewModel.onEvent(OnboardingContasEvent.AlterarApelidoConta(InstituicaoFinanceira.NUBANK, "   "))
        assertFalse(viewModel.uiState.value.podeContinuar)

        viewModel.onEvent(OnboardingContasEvent.AlterarApelidoConta(InstituicaoFinanceira.NUBANK, "Nubank"))
        viewModel.onEvent(OnboardingContasEvent.AlterarUltimosDigitos(InstituicaoFinanceira.NUBANK, TipoInstrumento.CREDITO, "12"))
        assertFalse(viewModel.uiState.value.podeContinuar)

        viewModel.onEvent(OnboardingContasEvent.AlterarUltimosDigitos(InstituicaoFinanceira.NUBANK, TipoInstrumento.CREDITO, ""))
        assertTrue(viewModel.uiState.value.podeContinuar)
    }

    @Test
    fun `continuar cria as contas com instrumentos e conclui o passo`() {
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.NUBANK))
        viewModel.onEvent(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.ITAU))
        viewModel.onEvent(OnboardingContasEvent.AlterarUltimosDigitos(InstituicaoFinanceira.NUBANK, TipoInstrumento.CREDITO, "1084"))

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
        viewModel.onEvent(OnboardingContasEvent.Continuar)

        viewModel.onEvent(OnboardingContasEvent.Continuar)
        contaRepository.gate?.complete(Unit)

        assertEquals(1, contaRepository.criadas.size)
        assertTrue(viewModel.uiState.value.concluido)
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

        override suspend fun salvar(conta: Conta) = Unit

        override suspend fun salvarTodas(contas: List<Conta>) = Unit

        override suspend fun excluir(publicId: String, quando: Instant) = Unit
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
