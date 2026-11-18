package com.pingo

import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.SessionState
import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.assinatura.AgendadorDeAvisosOpcional
import com.pingo.core.data.voucher.MaterializarRecargasVencidasUseCase
import com.pingo.core.notifications.IniciadorDeObservacaoDePendencias
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class InicializacaoDoApp internal constructor(
    private val observador: Provider<IniciadorDeObservacaoDePendencias>,
    private val agendadorDeSync: AgendadorDeSync,
    private val aoAutenticar: (suspend () -> Unit)?,
    private val aoDeslogar: (suspend () -> Unit)?,
    private val authRepository: Provider<AuthRepository>?,
    private val dispatchers: AppDispatchers,
) {
    @Inject
    constructor(
        observador: Provider<IniciadorDeObservacaoDePendencias>,
        agendadorDeSync: AgendadorDeSync,
        materializarRecargasVencidas: Provider<MaterializarRecargasVencidasUseCase>,
        agendadorDeAvisos: Provider<AgendadorDeAvisosOpcional>,
        authRepository: Provider<AuthRepository>,
        dispatchers: AppDispatchers,
    ) : this(
        observador = observador,
        agendadorDeSync = agendadorDeSync,
        aoAutenticar = {
            executarComSeguranca { materializarRecargasVencidas.get().invoke() }
            executarComSeguranca { agendadorDeAvisos.get().reconciliarTodas() }
        },
        aoDeslogar = {
            executarComSeguranca { agendadorDeAvisos.get().cancelarTodos() }
        },
        authRepository = authRepository,
        dispatchers = dispatchers,
    )

    constructor(
        observador: Provider<IniciadorDeObservacaoDePendencias>,
        agendadorDeSync: AgendadorDeSync,
        dispatchers: AppDispatchers,
    ) : this(
        observador = observador,
        agendadorDeSync = agendadorDeSync,
        aoAutenticar = null,
        aoDeslogar = null,
        authRepository = null,
        dispatchers = dispatchers,
    )

    private val escopo = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val iniciado = AtomicBoolean(false)

    fun iniciar() {
        if (!iniciado.compareAndSet(false, true)) {
            return
        }

        escopo.launch {
            observador.get().iniciar()
            agendadorDeSync.agendar()
            observarSessao()
        }
    }

    private suspend fun observarSessao() {
        val repositorio = authRepository ?: return
        repositorio.get().session.collect { sessao ->
            when (sessao) {
                SessionState.LoggedIn -> aoAutenticar?.invoke()
                SessionState.LoggedOut -> aoDeslogar?.invoke()
                SessionState.Loading -> Unit
            }
        }
    }

    private companion object {
        suspend fun executarComSeguranca(bloco: suspend () -> Unit) {
            runCatching { bloco() }.onFailure { erro ->
                if (erro is CancellationException) {
                    throw erro
                }
            }
        }
    }
}
