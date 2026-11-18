package com.pingo.core.auth

import android.app.Activity
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class GerenciadorDeBloqueioDoAppTest {
    @Test
    fun `inicia desbloqueado quando biometria estiver desligada`() = runTest(UnconfinedTestDispatcher()) {
        val prefs = FakePreferences(biometria = false)
        val auth = FakeAuthRepository(SessionState.LoggedIn)
        val clock = RelogioMutavel(INSTANTE_INICIAL)
        val gerenciador = GerenciadorDeBloqueioDoApp(prefs, auth, clock, backgroundScope)

        advanceUntilIdle()

        assertFalse("estado=${gerenciador.estado.value}", gerenciador.bloqueado.value)
        assertEquals(EstadoBloqueio.Desbloqueado, gerenciador.estado.value)
    }

    @Test
    fun `inicia bloqueado na abertura fria com biometria ativa e sessao logada`() = runTest(UnconfinedTestDispatcher()) {
        val prefs = FakePreferences(biometria = true)
        val auth = FakeAuthRepository(SessionState.LoggedIn)
        val clock = RelogioMutavel(INSTANTE_INICIAL)
        val gerenciador = GerenciadorDeBloqueioDoApp(prefs, auth, clock, backgroundScope)

        advanceUntilIdle()

        assertTrue(gerenciador.bloqueado.value)
        assertEquals(EstadoBloqueio.Bloqueado, gerenciador.estado.value)
    }

    @Test
    fun `nao bloqueia quando usuario ainda esta deslogado`() = runTest(UnconfinedTestDispatcher()) {
        val prefs = FakePreferences(biometria = true)
        val auth = FakeAuthRepository(SessionState.LoggedOut)
        val clock = RelogioMutavel(INSTANTE_INICIAL)
        val gerenciador = GerenciadorDeBloqueioDoApp(prefs, auth, clock, backgroundScope)

        advanceUntilIdle()

        assertFalse(gerenciador.bloqueado.value)
    }

    @Test
    fun `nao bloqueia imediatamente apos login novo mesmo com biometria ativa`() = runTest(UnconfinedTestDispatcher()) {
        val prefs = FakePreferences(biometria = true)
        val auth = FakeAuthRepository(SessionState.LoggedOut)
        val clock = RelogioMutavel(INSTANTE_INICIAL)
        val gerenciador = GerenciadorDeBloqueioDoApp(prefs, auth, clock, backgroundScope)
        advanceUntilIdle()

        auth.sessao.value = SessionState.LoggedIn
        advanceUntilIdle()

        assertFalse(gerenciador.bloqueado.value)
    }

    @Test
    fun `bloqueia ao voltar do segundo plano apos atingir tempo limite`() = runTest(UnconfinedTestDispatcher()) {
        val prefs = FakePreferences(biometria = true, tempo = 300L)
        val auth = FakeAuthRepository(SessionState.LoggedIn)
        val clock = RelogioMutavel(INSTANTE_INICIAL)
        val gerenciador = GerenciadorDeBloqueioDoApp(prefs, auth, clock, backgroundScope)
        advanceUntilIdle()
        gerenciador.marcarDesbloqueado()

        gerenciador.aoIrParaSegundoPlano()
        clock.avancarSegundos(299L)
        gerenciador.aoVoltarParaPrimeiroPlano()
        assertFalse(gerenciador.bloqueado.value)

        gerenciador.aoIrParaSegundoPlano()
        clock.avancarSegundos(300L)
        gerenciador.aoVoltarParaPrimeiroPlano()
        assertTrue(gerenciador.bloqueado.value)
    }

    @Test
    fun `tempo zero bloqueia imediatamente ao retornar do segundo plano`() = runTest(UnconfinedTestDispatcher()) {
        val prefs = FakePreferences(biometria = true, tempo = 0L)
        val auth = FakeAuthRepository(SessionState.LoggedIn)
        val clock = RelogioMutavel(INSTANTE_INICIAL)
        val gerenciador = GerenciadorDeBloqueioDoApp(prefs, auth, clock, backgroundScope)
        advanceUntilIdle()
        gerenciador.marcarDesbloqueado()

        gerenciador.aoIrParaSegundoPlano()
        gerenciador.aoVoltarParaPrimeiroPlano()

        assertTrue(gerenciador.bloqueado.value)
    }

    @Test
    fun `tempo negativo nunca bloqueia ao retornar do segundo plano`() = runTest(UnconfinedTestDispatcher()) {
        val prefs = FakePreferences(biometria = true, tempo = -1L)
        val auth = FakeAuthRepository(SessionState.LoggedIn)
        val clock = RelogioMutavel(INSTANTE_INICIAL)
        val gerenciador = GerenciadorDeBloqueioDoApp(prefs, auth, clock, backgroundScope)
        advanceUntilIdle()
        gerenciador.marcarDesbloqueado()

        gerenciador.aoIrParaSegundoPlano()
        clock.avancarSegundos(86_400L)
        gerenciador.aoVoltarParaPrimeiroPlano()

        assertFalse(gerenciador.bloqueado.value)
    }

    @Test
    fun `ignora segundo plano enquanto prompt biometrico esta ativo`() = runTest(UnconfinedTestDispatcher()) {
        val prefs = FakePreferences(biometria = true, tempo = 0L)
        val auth = FakeAuthRepository(SessionState.LoggedIn)
        val clock = RelogioMutavel(INSTANTE_INICIAL)
        val gerenciador = GerenciadorDeBloqueioDoApp(prefs, auth, clock, backgroundScope)
        advanceUntilIdle()
        gerenciador.marcarDesbloqueado()

        gerenciador.marcarAutenticacaoEmAndamento(true)
        gerenciador.aoIrParaSegundoPlano()
        gerenciador.marcarAutenticacaoEmAndamento(false)
        gerenciador.aoVoltarParaPrimeiroPlano()

        assertFalse(gerenciador.bloqueado.value)
    }

    @Test
    fun `desbloqueia quando usuario faz logout`() = runTest(UnconfinedTestDispatcher()) {
        val prefs = FakePreferences(biometria = true)
        val auth = FakeAuthRepository(SessionState.LoggedIn)
        val clock = RelogioMutavel(INSTANTE_INICIAL)
        val gerenciador = GerenciadorDeBloqueioDoApp(prefs, auth, clock, backgroundScope)
        advanceUntilIdle()
        assertTrue(gerenciador.bloqueado.value)

        auth.sessao.value = SessionState.LoggedOut
        advanceUntilIdle()

        assertFalse(gerenciador.bloqueado.value)
    }

    private class RelogioMutavel(private var agora: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = agora

        fun avancarSegundos(segundos: Long) {
            agora = agora.plusSeconds(segundos)
        }
    }

    private class FakeAuthRepository(inicial: SessionState) : AuthRepository {
        val sessao = MutableStateFlow(inicial)
        override val session: StateFlow<SessionState> = sessao

        override suspend fun loginWithGoogle(activity: Activity): LoginResult = LoginResult.Cancelled

        override suspend fun logout() {
            sessao.value = SessionState.LoggedOut
        }

        override suspend fun accessToken(forceRefresh: Boolean): String? = null
    }

    private class FakePreferences(biometria: Boolean, tempo: Long = 300L) : UserPreferencesDataSource {
        private val biometriaFlow = MutableStateFlow(biometria)
        private val tempoFlow = MutableStateFlow(tempo)

        override val biometriaAtiva: Flow<Boolean> = biometriaFlow
        override val tempoBloqueioSegundos: Flow<Long> = tempoFlow
        override val syncStatus: Flow<SyncStatus> = MutableStateFlow(SyncStatus.IDLE)

        override suspend fun setBiometriaAtiva(ativo: Boolean) {
            biometriaFlow.value = ativo
        }

        override suspend fun setTempoBloqueioSegundos(segundos: Long) {
            tempoFlow.value = segundos
        }

        override suspend fun setSyncStatus(status: SyncStatus) = Unit

        override suspend fun pullCursor(): String? = null

        override suspend fun setPullCursor(cursor: String?) = Unit

        override suspend fun pushCursor(): Instant? = null

        override suspend fun setPushCursor(instant: Instant) = Unit

        override suspend fun lastSyncAt(): Instant? = null

        override suspend fun setLastSyncAt(instant: Instant) = Unit

        override suspend fun deviceId(): String = "dev-1"

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

    private companion object {
        val INSTANTE_INICIAL: Instant = Instant.parse("2026-11-18T12:00:00Z")
    }
}
