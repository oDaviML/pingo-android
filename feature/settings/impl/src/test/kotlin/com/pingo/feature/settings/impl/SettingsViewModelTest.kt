package com.pingo.feature.settings.impl

import com.pingo.core.data.AccountRepository
import com.pingo.core.data.assinatura.AgendadorDeAvisosDeAssinatura
import com.pingo.core.data.assinatura.AgendadorDeAvisosOpcional
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.Optional

class SettingsViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    private val accountRepository = FakeAccountRepository()
    private val preferences = FakePreferences()
    private val agendador = FakeAgendador()

    private val viewModel = SettingsViewModel(accountRepository)

    @Test
    fun `aviso de assinaturas comeca desligado por padrao`() = runTest {
        val vm = criarViewModel()

        assertFalse(vm.uiState.value.avisoAssinaturasAtivo)
        assertFalse(vm.uiState.value.permissaoNotificacaoNegada)
    }

    @Test
    fun `ligar e desligar aviso de assinaturas persiste e reconcilia ou cancela`() = runTest {
        val vm = criarViewModel()

        vm.alternarAvisoAssinaturas(true)

        assertTrue(vm.uiState.value.avisoAssinaturasAtivo)
        assertTrue(preferences.avisoAssinaturasAtivo.first())
        assertFalse(vm.uiState.value.permissaoNotificacaoNegada)
        assertEquals(1, agendador.reconciliacoes)

        vm.alternarAvisoAssinaturas(false)

        assertFalse(vm.uiState.value.avisoAssinaturasAtivo)
        assertFalse(preferences.avisoAssinaturasAtivo.first())
        assertEquals(1, agendador.cancelamentosTotais)
    }

    @Test
    fun `negar permissao de notificacao mantem aviso desligado e sinaliza bloqueio`() = runTest {
        val vm = criarViewModel()

        vm.aoNegarPermissaoDeNotificacao()

        assertFalse(vm.uiState.value.avisoAssinaturasAtivo)
        assertFalse(preferences.avisoAssinaturasAtivo.first())
        assertTrue(vm.uiState.value.permissaoNotificacaoNegada)
        assertEquals(0, agendador.reconciliacoes)
        assertEquals(1, agendador.cancelamentosTotais)
    }

    @Test
    fun `sincronizar permissao revogada desativa aviso e limpa ao conceder`() = runTest {
        val vm = criarViewModel()
        vm.alternarAvisoAssinaturas(true)

        vm.sincronizarPermissaoDeNotificacao(concedida = false)

        assertFalse(vm.uiState.value.avisoAssinaturasAtivo)
        assertTrue(vm.uiState.value.permissaoNotificacaoNegada)
        assertFalse(preferences.avisoAssinaturasAtivo.first())
        assertEquals(1, agendador.cancelamentosTotais)

        vm.sincronizarPermissaoDeNotificacao(concedida = true)

        assertFalse(vm.uiState.value.permissaoNotificacaoNegada)
    }

    @Test
    fun `exclusao confirmada conclui e sinaliza conta apagada`() = runTest {
        viewModel.showDeleteDialog()
        viewModel.deleteAccount()

        assertTrue(viewModel.uiState.value.accountDeleted)
        assertFalse(viewModel.uiState.value.showDeleteDialog)
    }

    @Test
    fun `falha na exclusao mantem o dialogo e permite tentar de novo`() = runTest {
        accountRepository.failure = IllegalStateException("sem rede")
        viewModel.showDeleteDialog()
        viewModel.deleteAccount()

        assertTrue(viewModel.uiState.value.deleteFailed)
        assertTrue(viewModel.uiState.value.showDeleteDialog)
        assertFalse(viewModel.uiState.value.accountDeleted)

        accountRepository.failure = null
        viewModel.deleteAccount()

        assertTrue(viewModel.uiState.value.accountDeleted)
    }

    @Test
    fun `cancelar fecha o dialogo sem excluir`() = runTest {
        viewModel.showDeleteDialog()
        viewModel.dismissDeleteDialog()

        assertFalse(viewModel.uiState.value.showDeleteDialog)
        assertFalse(viewModel.uiState.value.accountDeleted)
    }

    private fun criarViewModel(): SettingsViewModel = SettingsViewModel(
        accountRepository = accountRepository,
        preferences = preferences,
        agendadorDeAvisos = AgendadorDeAvisosOpcional(Optional.of(agendador)),
    )

    private class FakeAccountRepository : AccountRepository {
        var failure: Throwable? = null

        override suspend fun recordConsent(version: String) = Unit

        override suspend fun deleteAccount() {
            failure?.let { throw it }
        }
    }

    private class FakeAgendador : AgendadorDeAvisosDeAssinatura {
        var reconciliacoes = 0
        var cancelamentosTotais = 0

        override suspend fun reconciliarTodas() {
            reconciliacoes++
        }

        override suspend fun cancelarParaAssinatura(assinaturaPublicId: String) = Unit

        override suspend fun cancelarTodos() {
            cancelamentosTotais++
        }
    }

    private class FakePreferences : UserPreferencesDataSource {
        private val aviso = MutableStateFlow(false)

        override val syncStatus: Flow<SyncStatus> = flowOf(SyncStatus.IDLE)
        override val avisoAssinaturasAtivo: Flow<Boolean> = aviso

        override suspend fun setAvisoAssinaturasAtivo(ativo: Boolean) {
            aviso.value = ativo
        }

        override suspend fun setSyncStatus(status: SyncStatus) = Unit

        override suspend fun pullCursor(): String? = null

        override suspend fun setPullCursor(cursor: String?) = Unit

        override suspend fun pushCursor(): Instant? = null

        override suspend fun setPushCursor(instant: Instant) = Unit

        override suspend fun lastSyncAt(): Instant? = null

        override suspend fun setLastSyncAt(instant: Instant) = Unit

        override suspend fun deviceId(): String = "dev"

        override suspend fun activatedDeviceId(): String? = null

        override suspend fun setActivatedDeviceId(deviceId: String) = Unit

        override suspend fun consentVersion(): String? = null

        override suspend fun consentAt(): Instant? = null

        override suspend fun setConsent(version: String, instant: Instant) = Unit

        override suspend fun contasConfiguradas(): Boolean = false

        override suspend fun setContasConfiguradas(configuradas: Boolean) = Unit

        override suspend fun contasOnboardingPendentes(): List<String> = emptyList()

        override suspend fun setContasOnboardingPendentes(publicIds: List<String>) = Unit

        override suspend fun clear() = Unit
    }
}
