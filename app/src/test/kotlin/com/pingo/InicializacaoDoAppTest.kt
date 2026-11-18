package com.pingo

import android.app.Activity
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.SessionState
import com.pingo.core.common.AppDispatchers
import com.pingo.core.notifications.IniciadorDeObservacaoDePendencias
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class InicializacaoDoAppTest {
    @Test
    fun `init resolves observer and schedules sync on io without eager resolution`() = runTest {
        val harness = Harness(testScheduler)

        harness.inicializacao.iniciar()

        assertEquals(0, harness.resolvidos)
        assertEquals(0, harness.iniciados)
        assertEquals(0, harness.agendados)

        advanceUntilIdle()

        assertTrue(harness.despachadoNoIo)
        assertEquals(1, harness.resolvidos)
        assertEquals(1, harness.iniciados)
        assertEquals(1, harness.agendados)
    }

    @Test
    fun `init twice keeps a single observation and a single sync schedule`() = runTest {
        val harness = Harness(testScheduler)

        harness.inicializacao.iniciar()
        harness.inicializacao.iniciar()
        advanceUntilIdle()

        assertEquals(1, harness.resolvidos)
        assertEquals(1, harness.iniciados)
        assertEquals(1, harness.agendados)
    }

    @Test
    fun `sessao autenticada materializa e reconcilia e logout cancela avisos`() = runTest {
        val harness = Harness(testScheduler)

        harness.inicializacaoComSessao.iniciar()
        advanceUntilIdle()

        assertEquals(0, harness.autenticacoes)
        assertEquals(0, harness.deslogamentos)

        harness.sessao.value = SessionState.LoggedIn
        advanceUntilIdle()

        assertEquals(1, harness.autenticacoes)
        assertEquals(0, harness.deslogamentos)

        harness.sessao.value = SessionState.LoggedOut
        advanceUntilIdle()

        assertEquals(1, harness.autenticacoes)
        assertEquals(1, harness.deslogamentos)

        harness.sessao.value = SessionState.LoggedIn
        advanceUntilIdle()

        assertEquals(2, harness.autenticacoes)
        assertEquals(1, harness.deslogamentos)
    }

    private class Harness(scheduler: kotlinx.coroutines.test.TestCoroutineScheduler) {
        val dispatcherDeTeste = StandardTestDispatcher(scheduler)
        var despachadoNoIo = false
        var resolvidos = 0
        var iniciados = 0
        var agendados = 0
        var autenticacoes = 0
        var deslogamentos = 0
        val sessao = MutableStateFlow(SessionState.Loading)

        private val dispatchers = object : AppDispatchers {
            override val io = object : CoroutineDispatcher() {
                override fun dispatch(context: CoroutineContext, block: Runnable) {
                    despachadoNoIo = true
                    dispatcherDeTeste.dispatch(context, block)
                }
            }
            override val default = dispatcherDeTeste
            override val main = dispatcherDeTeste
        }

        private val authRepository = object : AuthRepository {
            override val session: StateFlow<SessionState> = sessao

            override suspend fun loginWithGoogle(activity: Activity): LoginResult = LoginResult.Cancelled

            override suspend fun logout() {
                sessao.value = SessionState.LoggedOut
            }

            override suspend fun accessToken(forceRefresh: Boolean): String? = null
        }

        val inicializacao = InicializacaoDoApp(
            observador = Provider {
                resolvidos++
                IniciadorDeObservacaoDePendencias { iniciados++ }
            },
            agendadorDeSync = AgendadorDeSync { agendados++ },
            dispatchers = dispatchers,
        )

        val inicializacaoComSessao = InicializacaoDoApp(
            observador = Provider {
                resolvidos++
                IniciadorDeObservacaoDePendencias { iniciados++ }
            },
            agendadorDeSync = AgendadorDeSync { agendados++ },
            aoAutenticar = { autenticacoes++ },
            aoDeslogar = { deslogamentos++ },
            authRepository = Provider { authRepository },
            dispatchers = dispatchers,
        )
    }
}
